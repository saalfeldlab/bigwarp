/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2022 Howard Hughes Medical Institute.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package bdv.ij;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;
import org.janelia.saalfeldlab.n5.blosc.BloscCompression;
import org.janelia.saalfeldlab.n5.ij.N5Exporter;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadataParser;

import bdv.export.ProgressWriter;
import bdv.ij.util.ProgressWriterIJ;
import bdv.img.WarpedSource;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import bigwarp.BigWarpData;
import bigwarp.BigWarpExporter;
import bigwarp.BigWarpInit;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.BoundingBoxEstimation;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 *
 * Apply a bigwarp transform to a 2d or 3d ImagePlus
 *
 */
public class ApplyBigwarpPlugin implements PlugIn
{
	public static final String TARGET = "Target";
	public static final String MOVING = "Moving";
	public static final String MOVING_WARPED = "Moving (warped)";
	public static final String UNION_TARGET_MOVING = "Union of Target and warped moving image";
	public static final String SPECIFIED = "Specified";
	public static final String SPECIFIED_PHYSICAL = "Specified (physical units)";
	public static final String SPECIFIED_PIXEL = "Specified (pixel units)";
	public static final String LANDMARK_POINTS = "Landmark points";
	public static final String LANDMARK_POINT_CUBE_PHYSICAL = "Landmark point cube (physical units)";
	public static final String LANDMARK_POINT_CUBE_PIXEL = "Landmark point cube (pixel units)";

	public static void main( final String[] args )
	{
		new ImageJ();
		new ApplyBigwarpPlugin().run( "" );
	}

	public static boolean validateInput(
			final ImagePlus movingIp,
			final ImagePlus targetIp,
			final LandmarkTableModel landmarks,
			final String fieldOfViewOption,
			final String resolutionOption,
			final double[] resolutionSpec,
			final double[] fovSpec,
			final double[] offsetSpec,
			final Interpolation interp,
			final boolean isVirtual,
			final int nThreads )
	{
		// there's no target image
		if( targetIp == movingIp )
		{
			if( fieldOfViewOption.equals( TARGET ) )
			{
				return false;
			}

			if( resolutionOption.equals( TARGET ) )
			{
				return false;
			}
		}
		return true;
	}

	public static double[] getResolution(
			final Source< ? > source,
			final String resolutionOption,
			final double[] resolutionSpec )
	{
		if( ( source == null ) )
		{
			System.err.println("Requested target resolution but target image is missing.");
			return null;
		}

		final double[] res = resolutionFromSource( source );

		/*
		 * Maybe the below will work in the future, but it does not right now
		 * ( April 2021) in part because theres no way to set the VoxelDimensions for a
		 * bdv.util.RandomAccessibleIntervalSource
		 */
//		VoxelDimensions voxdims = source.getVoxelDimensions();
//		if( voxdims == null )
//			Arrays.fill( res, 1.0 );
//		else
//			voxdims.dimensions( res );

		return res;
	}

	public static String getUnit( final BigWarpData<?> bwData,
			final String resolutionOption )
	{
		String unit = "pix";
		final boolean doTargetImagesExist = bwData.numTargetSources()  > 0;
		if( resolutionOption.equals( MOVING ) || resolutionOption.equals( MOVING_WARPED ) || !doTargetImagesExist )
		{
			final VoxelDimensions mvgVoxDims = bwData.getMovingSource( 0 ).getSpimSource().getVoxelDimensions();
			if( mvgVoxDims != null )
				unit = mvgVoxDims.unit();
		}
		else
		{
			// use target units even if
			final VoxelDimensions tgtVoxDims = bwData.getTargetSource( 0 ).getSpimSource().getVoxelDimensions();
			if( tgtVoxDims != null )
				unit = tgtVoxDims.unit();
		}
		return unit;
	}

	public static double[] getResolution(
			final BigWarpData<?> bwData,
			final String resolutionOption,
			final double[] resolutionSpec )
	{
		// TODO it may be necessary to generalize this to an arbitrary
		// moving or target index rather than grabbing the first

		if( resolutionOption.equals( TARGET ))
		{
			if( bwData.numTargetSources() <= 0 )
				return null;

			final Source< ? > spimSource = bwData.getTargetSource(0 ).getSpimSource();
			return getResolution( spimSource, resolutionOption, resolutionSpec );
		}
		else if( resolutionOption.equals( MOVING ) )
		{
			if( bwData.numTargetSources() <= 0 )
				return null;

			final Source< ? > spimSource = bwData.getMovingSource( 0 ).getSpimSource();
			return getResolution( spimSource, resolutionOption, resolutionSpec );
		}
		else if( resolutionOption.equals( SPECIFIED ))
		{
			if( ( resolutionSpec == null ) )
			{
				System.err.println("Requested moving resolution but moving image is missing.");
				return null;
			}

			final double[] res = new double[ 3 ];
			System.arraycopy( resolutionSpec, 0, res, 0, resolutionSpec.length );
			return res;
		}
		return null;
	}

	/**
	 * Returns the resolution of the output given input options
	 *
	 * @param movingIp the moving ImagePlus
	 * @param targetIp the target ImagePlus
	 * @param resolutionOption the resolution option
	 * @param resolutionSpec the resolution (if applicable)
	 * @return the output image resolution
	 */
	public static double[] getResolution(
			final ImagePlus movingIp,
			final ImagePlus targetIp,
			final String resolutionOption,
			final double[] resolutionSpec )
	{
		if( resolutionOption.equals( TARGET ))
		{
			if( ( targetIp == null ) )
			{
				System.err.println("Requested target resolution but target image is missing.");
				return null;
			}

			final double[] res = new double[ 3 ];
			res[ 0 ] = targetIp.getCalibration().pixelWidth;
			res[ 1 ] = targetIp.getCalibration().pixelHeight;
			res[ 2 ] = targetIp.getCalibration().pixelDepth;
			return res;
		}
		else if( resolutionOption.equals( MOVING ))
		{
			if( ( movingIp == null ) )
			{
				System.err.println("Requested moving resolution but moving image is missing.");
				return null;
			}

			final double[] res = new double[ 3 ];
			res[ 0 ] = movingIp.getCalibration().pixelWidth;
			res[ 1 ] = movingIp.getCalibration().pixelHeight;
			res[ 2 ] = movingIp.getCalibration().pixelDepth;
			return res;
		}
		else if( resolutionOption.equals( SPECIFIED ))
		{
			if( ( resolutionSpec == null ) )
			{
				System.err.println("Requested moving resolution but moving image is missing.");
				return null;
			}

			final double[] res = new double[ 3 ];
			System.arraycopy( resolutionSpec, 0, res, 0, resolutionSpec.length );
			return res;
		}
		System.err.println("Invalid resolution option: " + resolutionOption );
		return null;
	}

	public static List<Interval> getPixelInterval(
			final ImagePlus movingIp,
			final ImagePlus targetIp,
			final LandmarkTableModel landmarks,
			final InvertibleRealTransform transform,
			final String fieldOfViewOption,
			final String fieldOfViewPointFilter,
			final double[] fovSpec,
			final double[] offsetSpec,
			final double[] outputResolution) {

		final BoundingBoxEstimation bboxEst = new BoundingBoxEstimation();
		final BigWarpData<?> bwData = BigWarpInit.createBigWarpDataFromImages(movingIp, targetIp);
		return getPixelInterval(bwData, landmarks, transform, fieldOfViewOption,
				fieldOfViewPointFilter, bboxEst, fovSpec, offsetSpec, outputResolution);
	}

	public static List<Interval> getPixelInterval(
			final ImagePlus movingIp,
			final ImagePlus targetIp,
			final LandmarkTableModel landmarks,
			final InvertibleRealTransform transform,
			final String fieldOfViewOption,
			final String fieldOfViewPointFilter,
			final BoundingBoxEstimation bboxEst,
			final double[] fovSpec,
			final double[] offsetSpec,
			final double[] outputResolution) {

		final BigWarpData<?> bwData = BigWarpInit.createBigWarpDataFromImages(movingIp, targetIp);
		return getPixelInterval(bwData, landmarks, transform, fieldOfViewOption,
				fieldOfViewPointFilter, bboxEst, fovSpec, offsetSpec, outputResolution);
	}

	public static Interval getPixelInterval(
			final Source< ? > source,
			final LandmarkTableModel landmarks,
			final InvertibleRealTransform transform,
			final String fieldOfViewOption,
			final double[] outputResolution )
	{
		return getPixelInterval( source, landmarks, transform, fieldOfViewOption, new BoundingBoxEstimation(), outputResolution );
	}

	public static Interval getPixelInterval(
			final Source< ? > source,
			final LandmarkTableModel landmarks,
			final InvertibleRealTransform transform,
			final String fieldOfViewOption,
			final BoundingBoxEstimation bboxEst,
			final double[] outputResolution )
	{
		final RandomAccessibleInterval< ? > rai = source.getSource( 0, 0 );

		if( fieldOfViewOption.equals( TARGET ))
		{
			final double[] inputres = resolutionFromSource( source );

			final int N  = outputResolution.length <= rai.numDimensions() ? outputResolution.length : rai.numDimensions();
			final long[] max = new long[ rai.numDimensions() ];
			for( int d = 0; d < N; d++ )
			{
				max[ d ] = (long)Math.ceil( ( inputres[ d ] * rai.dimension( d )) / outputResolution[ d ]);
			}

			return new FinalInterval( max );
		}
		else if( fieldOfViewOption.equals( MOVING_WARPED ))
		{
			final FinalInterval interval = new FinalInterval(
					Intervals.minAsLongArray( rai ),
					Intervals.maxAsLongArray( rai ));

			if( transform == null )
				return interval;

			final double[] movingRes = resolutionFromSource( source );
			final int ndims = transform.numSourceDimensions();
			final AffineTransform movingPixelToPhysical = new AffineTransform( ndims );
			movingPixelToPhysical.set( movingRes[ 0 ], 0, 0 );
			movingPixelToPhysical.set( movingRes[ 1 ], 1, 1 );
			if( ndims > 2 )
				movingPixelToPhysical.set( movingRes[ 2 ], 2, 2 );

			final AffineTransform outputResolution2Pixel = new AffineTransform( ndims );
			outputResolution2Pixel.set( outputResolution[ 0 ], 0, 0 );
			outputResolution2Pixel.set( outputResolution[ 1 ], 1, 1  );
			if( ndims > 2 )
				outputResolution2Pixel.set( outputResolution[ 2 ], 2, 2  );

			final RealTransformSequence seq = new RealTransformSequence();
			seq.add( movingPixelToPhysical );
			seq.add( transform.inverse() );
			seq.add( outputResolution2Pixel.inverse() );

			return bboxEst.estimatePixelInterval(seq, interval);
			//			return BigWarpExporter.estimateBounds( seq, interval );
		}
		else if( fieldOfViewOption.equals( UNION_TARGET_MOVING ))
		{
			final Interval movingWarpedInterval = getPixelInterval( source, landmarks, transform, MOVING_WARPED, bboxEst, outputResolution );
			final Interval targetInterval = getPixelInterval( source, landmarks, transform, TARGET, bboxEst, outputResolution );
			return Intervals.union( movingWarpedInterval, targetInterval );
		}

		return null;
	}

	public static double[] resolutionFromSource( final Source< ? > src )
	{
		final double[] res = new double[ 3 ];
		final AffineTransform3D xfm = new AffineTransform3D();
		src.getSourceTransform( 0, 0, xfm );

		res[ 0 ] = xfm.get( 0, 0 );
		res[ 1 ] = xfm.get( 1, 1 );
		res[ 2 ] = xfm.get( 2, 2 );

		return res;
	}

	public static List<Interval> getPixelInterval(
			final BigWarpData<?> bwData,
			final LandmarkTableModel landmarks,
			final InvertibleRealTransform transform,
			final String fieldOfViewOption,
			final String fieldOfViewPointFilter,
			final double[] fovSpec,
			final double[] offsetSpec,
			final double[] outputResolution)
	{
		return getPixelInterval( bwData, landmarks, transform, fieldOfViewOption,
				fieldOfViewPointFilter, new BoundingBoxEstimation(),
				fovSpec, offsetSpec, outputResolution );
	}

	/**
	 * Returns the interval in pixels of the output given input options
	 *
	 * @param bwData
	 *            the BigWarpData
	 * @param landmarks
	 *            the landmarks
	 * @param fieldOfViewOption
	 *            the field of view option
	 * @param fieldOfViewPointFilter
	 *            the regexp for filtering landmarks points by name
	 * @param bboxEst
	 *            the bounding box estimator
	 * @param fovSpec
	 *            the field of view specification
	 * @param offsetSpec
	 *            the offset specification
	 * @param outputResolution
	 *            the resolution of the output image
	 * @return the output interval
	 */
	public static List<Interval> getPixelInterval(
			final BigWarpData<?> bwData,
			final LandmarkTableModel landmarks,
			final InvertibleRealTransform transform,
			final String fieldOfViewOption,
			final String fieldOfViewPointFilter,
			final BoundingBoxEstimation bboxEst,
			final double[] fovSpec,
			final double[] offsetSpec,
			final double[] outputResolution) {

		if (fieldOfViewOption.equals(TARGET)) {
			if (bwData.numTargetSources() <= 0) {
				System.err.println("Requested target fov but target image is missing.");
				return null;
			}

			return Stream.of(
					getPixelInterval(
							bwData.getTargetSource( 0 ).getSpimSource(),
							landmarks, transform, fieldOfViewOption, bboxEst, outputResolution))
					.collect(Collectors.toList());
		} else if (fieldOfViewOption.equals(MOVING_WARPED)) {
			return Stream.of(
					getPixelInterval(
							bwData.getMovingSource( 0 ).getSpimSource(),
							landmarks, transform, fieldOfViewOption, bboxEst, outputResolution))
					.collect(Collectors.toList());
		} else if (fieldOfViewOption.equals(UNION_TARGET_MOVING)) {
			return Stream.of(
					getPixelInterval(
							bwData.getMovingSource( 0 ).getSpimSource(),
							landmarks, transform, fieldOfViewOption, bboxEst, outputResolution))
					.collect(Collectors.toList());
		} else if (fieldOfViewOption.equals(SPECIFIED_PIXEL)) {
			if (fovSpec.length == 2) {
				final long[] min = new long[]{
						(long)Math.floor(offsetSpec[0]),
						(long)Math.floor(offsetSpec[1])};

				final long[] max = new long[]{
						(long)Math.ceil(offsetSpec[0] + fovSpec[0]),
						(long)Math.ceil(offsetSpec[0] + fovSpec[1])};

				final ArrayList<Interval> out = new ArrayList<>();
				out.add(new FinalInterval(min, max));
				return out;
			} else if (fovSpec.length == 3) {
				final long[] min = new long[]{
						(long)Math.floor(offsetSpec[0]),
						(long)Math.floor(offsetSpec[1]),
						(long)Math.floor(offsetSpec[2])};

				final long[] max = new long[]{
						(long)Math.ceil(offsetSpec[0] + fovSpec[0]),
						(long)Math.ceil(offsetSpec[1] + fovSpec[1]),
						(long)Math.ceil(offsetSpec[2] + fovSpec[2])};

				final ArrayList<Interval> out = new ArrayList<>();
				out.add(new FinalInterval(min, max));
				return out;
			} else {
				System.out.println("Invalid fov spec, length : " + fovSpec.length);
				return null;
			}
		} else if (fieldOfViewOption.equals(SPECIFIED_PHYSICAL)) {
			if (fovSpec.length == 2) {
				final long[] min = new long[]{
						(long)Math.floor(offsetSpec[0] / outputResolution[0]),
						(long)Math.floor(offsetSpec[1] / outputResolution[1])};

				final long[] max = new long[]{
						(long)Math.floor((offsetSpec[0] + fovSpec[0]) / outputResolution[0]),
						(long)Math.floor((offsetSpec[1] + fovSpec[1]) / outputResolution[1])};

				final ArrayList<Interval> out = new ArrayList<>();
				out.add(new FinalInterval(min, max));
				return out;
			} else if (fovSpec.length == 3) {
				final long[] min = new long[]{
						(long)Math.floor(offsetSpec[0] / outputResolution[0]),
						(long)Math.floor(offsetSpec[1] / outputResolution[1]),
						(long)Math.floor(offsetSpec[2] / outputResolution[2])};

				final long[] max = new long[]{
						(long)Math.floor((offsetSpec[0] + fovSpec[0]) / outputResolution[0]),
						(long)Math.floor((offsetSpec[0] + fovSpec[1]) / outputResolution[1]),
						(long)Math.floor((offsetSpec[2] + fovSpec[2]) / outputResolution[2])};

				final ArrayList<Interval> out = new ArrayList<>();
				out.add(new FinalInterval(min, max));
				return out;
			} else {
				System.out.println("Invalid fov spec, length : " + fovSpec.length);
				return null;
			}
		} else if (fieldOfViewOption.equals(LANDMARK_POINTS)) {
			final List<Double[]> matchedLandmarks = getMatchedPoints(landmarks, fieldOfViewPointFilter);

			final long[] min = new long[landmarks.getNumdims()];
			final long[] max = new long[landmarks.getNumdims()];

			Arrays.fill(min, Long.MAX_VALUE);
			Arrays.fill(max, Long.MIN_VALUE);

			int numPoints = 0;
			for (int i = 0; i < matchedLandmarks.size(); i++) {
				final Double[] pt = matchedLandmarks.get(i);
				for (int d = 0; d < pt.length; d++) {
					final long lo = (long)(Math.floor(pt[d] / outputResolution[d]));
					final long hi = (long)(Math.ceil(pt[d] / outputResolution[d]));

					if (lo < min[d])
						min[d] = lo;

					if (hi > max[d])
						max[d] = hi;

				}
				numPoints++;
			}

			System.out.println("Estimated field of view using " + numPoints + " landmarks.");

			// Make sure something naughty didn't happen
			for (int d = 0; d < min.length; d++) {
				if (min[d] == Long.MAX_VALUE) {
					System.err.println("Problem generating field of view from landmarks");
					return null;
				}

				if (max[d] == Long.MIN_VALUE) {
					System.err.println("Problem generating field of view from landmarks");
					return null;
				}
			}

			final ArrayList<Interval> out = new ArrayList<>();
			out.add(new FinalInterval(min, max));
			return out;
		} else if (fieldOfViewOption.equals(LANDMARK_POINT_CUBE_PHYSICAL)
				|| fieldOfViewOption.equals(LANDMARK_POINT_CUBE_PIXEL)) {
			final List<Double[]> matchedLandmarks = getMatchedPoints(landmarks, fieldOfViewPointFilter);
			if (matchedLandmarks.isEmpty()) {
				System.err.println("No matching point found");
				return null;
			}

			final int nd = landmarks.getNumdims();
			final ArrayList<Interval> out = new ArrayList<>();

			for (int i = 0; i < matchedLandmarks.size(); i++) {
				final Double[] pt = matchedLandmarks.get(i);
				final long[] min = new long[nd];
				final long[] max = new long[nd];
				if (fieldOfViewOption.equals(LANDMARK_POINT_CUBE_PHYSICAL)) {
					for (int d = 0; d < nd; d++) {
						min[d] = (long)Math.floor((pt[d] / outputResolution[d]) - (fovSpec[d] / outputResolution[d]));
						max[d] = (long)Math.ceil((pt[d] / outputResolution[d]) + (fovSpec[d] / outputResolution[d]));
					}
				} else {
					for (int d = 0; d < nd; d++) {
						min[d] = (long)Math.floor((pt[d] / outputResolution[d]) - (fovSpec[d])) + 1;
						max[d] = (long)Math.ceil((pt[d] / outputResolution[d]) + (fovSpec[d])) - 1;
					}
				}

				out.add(new FinalInterval(min, max));
			}
			return out;
		}

		System.err.println("Invalid field of view option: ( " + fieldOfViewOption + " )");
		return null;
	}

	public static void fillMatchedPointNames(
			final List<String> ptList,
			final LandmarkTableModel landmarks,
			final String fieldOfViewPointFilter )
	{
		Pattern r = null;
		if ( !fieldOfViewPointFilter.isEmpty() )
			r = Pattern.compile( fieldOfViewPointFilter );

		for ( int i = 0; i < landmarks.getRowCount(); i++ )
		{
			if ( r != null && !r.matcher( landmarks.getNames().get( i ) ).matches() )
			{
				continue;
			}

			final Double[] pt = landmarks.getFixedPoint( i );
			if( Double.isInfinite( pt[ 0 ].doubleValue()) )
			{
				continue;
			}

			ptList.add( landmarks.getNames().get( i ) );
		}
	}

	public static List<Double[]> getMatchedPoints(
			final LandmarkTableModel landmarks,
			final String fieldOfViewPointFilter )
	{
		final ArrayList<Double[]> ptList = new ArrayList<>();

		Pattern r = null;
		if ( !fieldOfViewPointFilter.isEmpty() )
			r = Pattern.compile( fieldOfViewPointFilter );

		for ( int i = 0; i < landmarks.getRowCount(); i++ )
		{
			if ( r != null && !r.matcher( landmarks.getNames().get( i ) ).matches() )
			{
				continue;
			}

			final Double[] pt = landmarks.getFixedPoint( i );
			if( Double.isInfinite( pt[ 0 ].doubleValue()) )
			{
				continue;
			}

			ptList.add( pt );

			System.out.println( "Using point with name : "
					+ landmarks.getNames().get( i ) );
		}

		return ptList;
	}

	/**
	 * Get the offset in pixels given the output resolution and interval
	 *
	 * @param fieldOfViewOption the field of view option
	 * @param offsetSpec the offset specification
	 * @param outputResolution the resolution of the output image
	 * @param outputInterval the output interval
	 * @return the offset
	 */
	public static double[] getPixelOffset(
			final String fieldOfViewOption,
			final double[] offsetSpec,
			final double[] outputResolution,
			final Interval outputInterval )
	{
		final double[] offset = new double[ 3 ];
		if( fieldOfViewOption.equals( SPECIFIED_PIXEL ) )
		{
			System.arraycopy( offsetSpec, 0, offset, 0, offset.length );
			return offset;
		}
		else if( fieldOfViewOption.equals( SPECIFIED_PHYSICAL ) )
		{
			for( int d = 0; d < outputInterval.numDimensions(); d++ )
			{
				offset[ d ] = offsetSpec[ d ] / outputResolution[ d ];
			}
			return offset;
		}
		else
		{
			for( int d = 0; d < outputInterval.numDimensions(); d++ )
			{
				offset[ d ] = outputInterval.realMin( d );
			}
			return offset;
		}
	}

	public static List<ImagePlus> apply(
			final ImagePlus movingIp,
			final ImagePlus targetIp,
			final LandmarkTableModel landmarks,
			final String tranformTypeOption,
			final String fieldOfViewOption,
			final String fieldOfViewPointFilter,
			final String resolutionOption,
			final double[] resolutionSpec,
			final double[] fovSpec,
			final double[] offsetSpec,
			final Interpolation interp,
			final boolean isVirtual,
			final boolean wait,
			final int nThreads) {

		final BoundingBoxEstimation bboxEst = new BoundingBoxEstimation(BoundingBoxEstimation.Method.CORNERS);
		return apply(movingIp, targetIp, landmarks, tranformTypeOption, fieldOfViewOption,
				fieldOfViewPointFilter, bboxEst,
				resolutionOption, resolutionSpec, fovSpec, offsetSpec,
				interp, isVirtual, nThreads, wait, null);
	}

	public static List<ImagePlus> apply(
			final ImagePlus movingIp,
			final ImagePlus targetIp,
			final LandmarkTableModel landmarks,
			final String tranformTypeOption,
			final String fieldOfViewOption,
			final String fieldOfViewPointFilter,
			final BoundingBoxEstimation bboxEst,
			final String resolutionOption,
			final double[] resolutionSpec,
			final double[] fovSpec,
			final double[] offsetSpec,
			final Interpolation interp,
			final boolean isVirtual,
			final boolean wait,
			final int nThreads) {

		return apply(movingIp, targetIp, landmarks, tranformTypeOption, fieldOfViewOption,
				fieldOfViewPointFilter, bboxEst,
				resolutionOption, resolutionSpec, fovSpec, offsetSpec,
				interp, isVirtual, nThreads, wait, null);
	}

	public static List<ImagePlus> apply(
			final ImagePlus movingIp,
			final ImagePlus targetIp,
			final LandmarkTableModel landmarks,
			final String tranformTypeOption,
			final String fieldOfViewOption,
			final String fieldOfViewPointFilter,
			final BoundingBoxEstimation bboxEst,
			final String resolutionOption,
			final double[] resolutionSpec,
			final double[] fovSpec,
			final double[] offsetSpec,
			final Interpolation interp,
			final boolean isVirtual,
			final int nThreads,
			final boolean wait,
			final WriteDestinationOptions writeOpts) {

		final BigWarpData<?> bwData = BigWarpInit.createBigWarpDataFromImages(movingIp, targetIp);
		return apply(bwData, landmarks, tranformTypeOption, fieldOfViewOption, fieldOfViewPointFilter, bboxEst,
				resolutionOption, resolutionSpec, fovSpec, offsetSpec,
				interp, isVirtual, nThreads, wait, writeOpts);
	}

	public static <T> List<ImagePlus> apply(
			final BigWarpData<T> bwData,
			final LandmarkTableModel landmarks,
			final String tranformTypeOption,
			final String fieldOfViewOption,
			final String fieldOfViewPointFilter,
			final String resolutionOption,
			final double[] resolutionSpec,
			final double[] fovSpec,
			final double[] offsetSpec,
			final Interpolation interp,
			final boolean isVirtual,
			final int nThreads,
			final boolean wait,
			final WriteDestinationOptions writeOpts) {

		final BoundingBoxEstimation bboxEst = new BoundingBoxEstimation(BoundingBoxEstimation.Method.CORNERS);
		return apply(bwData, landmarks, tranformTypeOption, fieldOfViewOption, fieldOfViewPointFilter, bboxEst,
				resolutionOption, resolutionSpec, fovSpec, offsetSpec, interp, isVirtual,
				nThreads, wait, writeOpts);
	}

	public static <T> List<ImagePlus> apply(
			final BigWarpData<T> bwData,
			final LandmarkTableModel landmarks,
			final String tranformTypeOption,
			final String fieldOfViewOption,
			final String fieldOfViewPointFilter,
			final BoundingBoxEstimation bboxEst,
			final String resolutionOption,
			final double[] resolutionSpec,
			final double[] fovSpec,
			final double[] offsetSpec,
			final Interpolation interp,
			final boolean isVirtual,
			final int nThreads,
			final boolean wait,
			final WriteDestinationOptions writeOpts) {

//		int numChannels = bwData.movingSourceIndexList.size();
		final int numChannels = bwData.numMovingSources();
//		final List< SourceAndConverter< T >> sourcesxfm = BigWarp.wrapSourcesAsTransformed(
//				bwData.sourceInfos,
//				landmarks.getNumdims(),
//				bwData );

		final InvertibleRealTransform invXfm = new BigWarpTransform( landmarks, tranformTypeOption ).getTransformation();
		for ( int i = 0; i < numChannels; i++ )
		{
			final SourceAndConverter< T > movingSource = bwData.getMovingSource( i );
//			final int originalIdx = bwData.sources.indexOf(movingSource);
			((WarpedSource<?>)(movingSource.getSpimSource())).updateTransform(invXfm);
			((WarpedSource<?>)(movingSource.getSpimSource())).setIsTransformed(true);
		}

		final ProgressWriter progressWriter = new ProgressWriterIJ();

		// Generate the properties needed to generate the transform from output pixel space
		// to physical space
		final double[] res = getResolution( bwData, resolutionOption, resolutionSpec );

		final List<Interval> outputIntervalList = getPixelInterval(bwData, landmarks, invXfm, fieldOfViewOption,
				fieldOfViewPointFilter, bboxEst, fovSpec, offsetSpec, res);

		final List<String> matchedPtNames = new ArrayList<>();
		if( outputIntervalList.size() > 1 )
			ApplyBigwarpPlugin.fillMatchedPointNames( matchedPtNames, landmarks, fieldOfViewPointFilter );

		final double[] offset = getPixelOffset( fieldOfViewOption, offsetSpec, res, outputIntervalList.get( 0 ) );

		if( writeOpts != null && writeOpts.n5Dataset != null && !writeOpts.n5Dataset.isEmpty())
		{
			final String unit = ApplyBigwarpPlugin.getUnit( bwData, resolutionOption );
			runN5Export( bwData, bwData.sources, fieldOfViewOption,
					outputIntervalList.get( 0 ), interp,
					offset, res, unit,
					progressWriter, writeOpts,
					Executors.newFixedThreadPool( nThreads )  );
			return null;
		}
		else
		{
			final boolean show;
			if( writeOpts == null || writeOpts.pathOrN5Root == null || writeOpts.pathOrN5Root.isEmpty())
				show = true;
			else
				show = false;

			return runExport( bwData, bwData.sources, fieldOfViewOption,
					outputIntervalList, matchedPtNames, interp,
					offset, res, isVirtual, nThreads,
					progressWriter, show, wait, writeOpts );
		}
	}

	public static <T> List<ImagePlus> runExport(
			final BigWarpData<T> data,
			final List<SourceAndConverter<T>> sources,
			final String fieldOfViewOption,
			final List<Interval> outputIntervalList,
			final List<String> matchedPtNames,
			final Interpolation interp,
			final double[] offsetIn,
			final double[] resolution,
			final boolean isVirtual,
			final int nThreads,
			final ProgressWriter progressWriter,
			final boolean show,
			final boolean wait,
			final WriteDestinationOptions writeOpts )
	{
		final ArrayList<ImagePlus> ipList = new ArrayList<>();

		int i = 0;
		for( final Interval outputInterval : outputIntervalList )
		{
			final double[] offset = ApplyBigwarpPlugin.getPixelOffset( fieldOfViewOption, offsetIn, resolution, outputIntervalList.get( i ) );

			// need to declare the exporter in the loop since the actual work
			// is done asynchronously, and changing variables in the loop would mess it up
			final BigWarpExporter<?> exporter = BigWarpExporter.getExporter( data, sources, interp, progressWriter );
			exporter.setOutputList( ipList );
			exporter.setRenderResolution( resolution );
			exporter.setOffset( offset );
			exporter.setVirtual( isVirtual );
			exporter.setNumThreads( nThreads );

			if( writeOpts!= null && writeOpts.pathOrN5Root != null )
				exporter.setExportPath( writeOpts.pathOrN5Root );

			exporter.setInterval( outputInterval );

			if( matchedPtNames.size() > 0 )
				exporter.setNameSuffix( matchedPtNames.get( i ));

			// never wait
			exporter.exportAsynch( wait, show );

			i++;
		}
		return ipList;
	}

	public static <S, T extends NativeType<T> & NumericType<T>> void runN5Export(
			final BigWarpData<S> data,
			final List< SourceAndConverter< S >> sources,
			final String fieldOfViewOption,
			final Interval outputInterval,
			final Interpolation interp,
			final double[] offset,
			final double[] resolution,
			final String unit,
			final ProgressWriter progressWriter,
			final WriteDestinationOptions writeOpts,
			final ExecutorService exec )
	{
		final int nd = BigWarp.detectNumDims( data.sources );

		// setup n5 parameters
		final String dataset = writeOpts.n5Dataset;
		final int[] blockSize = writeOpts.blockSize;
		final Compression compression = writeOpts.compression;
		if( dataset == null || dataset.isEmpty() )
		{
			return;
		}
		N5Writer n5;
		try
		{
			n5 = new N5Factory().openWriter( writeOpts.pathOrN5Root );
		}
		catch ( final RuntimeException e1 )
		{
			e1.printStackTrace();
			return;
		}

		// build metadata
		final String[] axes = nd == 2 ? new String[] { "y", "x" } :new String[]{ "z", "y", "x" } ;
		final String[] units = nd == 2 ? new String[]{ unit, unit } : new String[] { unit, unit, unit };
		final N5CosemMetadata metadata = new N5CosemMetadata( "", new N5CosemMetadata.CosemTransform( axes, resolution, offset, units ), null );
		final N5CosemMetadataParser parser = new N5CosemMetadataParser();

		// setup physical to pixel transform
		final AffineTransform3D resolutionTransform = new AffineTransform3D();
		resolutionTransform.set( resolution[ 0 ], 0, 0 );
		resolutionTransform.set( resolution[ 1 ], 1, 1 );

		if( resolution.length > 2 )
			resolutionTransform.set( resolution[ 2 ], 2, 2 );

		final AffineTransform3D offsetTransform = new AffineTransform3D();
		offsetTransform.set( offset[ 0 ], 0, 3 );
		offsetTransform.set( offset[ 1 ], 1, 3 );

		if( resolution.length > 2 )
			offsetTransform.set( offset[ 2 ], 2, 3 );

		final AffineTransform3D pixelRenderToPhysical = new AffineTransform3D();
		pixelRenderToPhysical.concatenate( resolutionTransform );
		pixelRenderToPhysical.concatenate( offsetTransform );

		// render and write
		final int N = data.numMovingSources();
		for ( int i = 0; i < N; i++ )
		{
			final SourceAndConverter< S > originalMovingSource = data.getMovingSource( i );
			final int movingSourceIndex = data.sources.indexOf( originalMovingSource );
			@SuppressWarnings( "unchecked" )
			final RealRandomAccessible< T > raiRaw = ( RealRandomAccessible< T > )sources.get( movingSourceIndex ).getSpimSource().getInterpolatedSource( 0, 0, interp );

			// to pixel space
			final AffineRandomAccessible< T, AffineGet > rai = RealViews.affine( raiRaw, pixelRenderToPhysical.inverse() );

			final IntervalView< T > img = Views.interval( Views.raster( rai ),
					Intervals.zeroMin( outputInterval ) );
			final String srcName = originalMovingSource.getSpimSource().getName();

			String destDataset = dataset;
			if( N >  1 )
				destDataset = dataset + String.format( "/%s", srcName.replace( " " , "_" ));

			RandomAccessibleInterval<T> imgToWrite;
			if( nd == 2 )
				imgToWrite = Views.hyperSlice( img, 2, 0 );
			else
				imgToWrite = img;

			try
			{
				N5Utils.save( imgToWrite, n5, destDataset, blockSize, compression, exec );
				if( parser != null && metadata != null )
					parser.writeMetadata( metadata, n5, destDataset );

				n5.close();
			}
			catch ( final Exception e )
			{
				e.printStackTrace();
			}

			progressWriter.setProgress( (i+1) / ((double)N) );
		}

		progressWriter.setProgress( 1.0 );
	}

	@Override
	public void run( final String arg )
	{
		if ( IJ.versionLessThan( "1.40" ) )
			return;

        // Find any open images
        final int[] ids = WindowManager.getIDList();
        final int N = ids == null ? 0 : ids.length;

        final String[] titles = new String[ N + 1 ];
        for ( int i = 0; i < N; ++i )
        {
            titles[ i ] = ( WindowManager.getImage( ids[ i ] ) ).getTitle();
        }
        titles[ N ] = "<None>";

		final GenericDialogPlus gd = new GenericDialogPlus( "Apply Big Warp transform" );
		gd.addMessage( "File Selection:" );
		gd.addFileField( "landmarks_image_file", "" );

		final ImagePlus currimg = WindowManager.getCurrentImage();
		String current = titles[ N ];
		if ( currimg != null )
		{
			current = currimg.getTitle();
		}

		gd.addChoice( "moving_image", titles, current );
		if ( titles.length > 1 )
			gd.addChoice( "target_image", titles, current.equals( titles[ 0 ] ) ? titles[ 1 ] : titles[ 0 ] );
		else
			gd.addChoice( "target_image", titles, titles[ 0 ] );

		gd.addMessage( "\nN5/Zarr/HDF5/BDV-XML" );
        gd.addDirectoryOrFileField( "Moving", "" );
        gd.addStringField( "Moving_dataset", "" );
        gd.addDirectoryOrFileField( "Target", "" );
        gd.addStringField( "Target_dataset", "" );

		gd.addChoice( "Transform type",
				new String[] {
					BigWarpTransform.TPS,
					BigWarpTransform.AFFINE,
					BigWarpTransform.SIMILARITY,
					BigWarpTransform.ROTATION,
					BigWarpTransform.TRANSLATION },
				BigWarpTransform.TPS);

		gd.addMessage( "Field of view and resolution:" );
		gd.addChoice( "Resolution",
				new String[]{ TARGET, MOVING, SPECIFIED },
				TARGET );

		gd.addChoice( "Field of view",
				new String[]{ TARGET, MOVING_WARPED, LANDMARK_POINTS, SPECIFIED_PIXEL, SPECIFIED_PHYSICAL },
				TARGET );

		gd.addChoice("Bounding box estimation",
				new String[]{
						BoundingBoxEstimation.Method.CORNERS.toString(),
						BoundingBoxEstimation.Method.FACES.toString(),
						BoundingBoxEstimation.Method.VOLUME.toString()},
			BoundingBoxEstimation.Method.FACES.toString());

		gd.addNumericField( "samples per dimension", 5, 0 );

		gd.addStringField( "point filter", "" );

		gd.addMessage( "Resolution");
		gd.addNumericField( "x", 1.0, 4 );
		gd.addNumericField( "y", 1.0, 4 );
		gd.addNumericField( "z", 1.0, 4 );

		gd.addMessage( "Offset");
		gd.addNumericField( "x", 0.0, 4 );
		gd.addNumericField( "y", 0.0, 4 );
		gd.addNumericField( "z", 0.0, 4 );

		gd.addMessage( "Field of view");
		gd.addNumericField( "x", -1, 0 );
		gd.addNumericField( "y", -1, 0 );
		gd.addNumericField( "z", -1, 0 );

		gd.addMessage( "Output options");
		gd.addChoice( "Interpolation", new String[]{ "Nearest Neighbor", "Linear" }, "Linear" );
		gd.addCheckbox( "virtual?", false );
		gd.addNumericField( "threads", 4, 0 );

		gd.addMessage( "Writing options (leave empty to opena new image window)" );
		gd.addDirectoryOrFileField( "File_or_n5_root", "" );
		gd.addStringField( "n5_dataset", "" );
		gd.addStringField( "n5_block_size", "32" );
		gd.addChoice( "n5_compression", new String[] {
				N5Exporter.GZIP_COMPRESSION,
				N5Exporter.RAW_COMPRESSION,
				N5Exporter.LZ4_COMPRESSION,
				N5Exporter.XZ_COMPRESSION,
				N5Exporter.BLOSC_COMPRESSION },
			N5Exporter.GZIP_COMPRESSION );

		gd.showDialog();

		if ( gd.wasCanceled() )
			return;

		final String landmarksPath = gd.getNextString();

		ImagePlus movingIp = null;
		ImagePlus targetIp = null;
		final int mvgImgIdx = gd.getNextChoiceIndex();
		final int tgtImgIdx = gd.getNextChoiceIndex();
		movingIp = mvgImgIdx < N ? WindowManager.getImage( ids[ mvgImgIdx ]) : null;
		targetIp = tgtImgIdx < N ? WindowManager.getImage( ids[ tgtImgIdx ]) : null;

		final String mvgRoot = gd.getNextString();
		final String mvgDataset = gd.getNextString();
		final String tgtRoot = gd.getNextString();
		final String tgtDataset = gd.getNextString();

		final String transformTypeOption = gd.getNextChoice();
		final String resOption = gd.getNextChoice();
		final String fovOption = gd.getNextChoice();

		final String bboxOption = gd.getNextChoice();
		final int bboxSamples = (int)gd.getNextNumber();

		final String fovPointFilter = gd.getNextString();

		final double[] resolutions = new double[ 3 ];
		resolutions[ 0 ] = gd.getNextNumber();
		resolutions[ 1 ] = gd.getNextNumber();
		resolutions[ 2 ] = gd.getNextNumber();

		final double[] offset = new double[ 3 ];
		offset[ 0 ] = gd.getNextNumber();
		offset[ 1 ] = gd.getNextNumber();
		offset[ 2 ] = gd.getNextNumber();

		final double[] fov = new double[ 3 ];
		fov[ 0 ] = gd.getNextNumber();
		fov[ 1 ] = gd.getNextNumber();
		fov[ 2 ] = gd.getNextNumber();

		final String interpType = gd.getNextChoice();
		final boolean isVirtual = gd.getNextBoolean();
		final int nThreads = (int)gd.getNextNumber();

		final String fileOrN5Root = gd.getNextString();
		final String n5Dataset = gd.getNextString();
		final String blockSizeString = gd.getNextString();
		final String compressionString = gd.getNextChoice();

		// load the image data
		final BigWarpData< ? > bigwarpdata = BigWarpInit.initData();
		int id = 0;
		if ( movingIp != null )
		{
			id += BigWarpInit.add( bigwarpdata, movingIp, id, 0, true );
		}
		else if( mvgDataset != null && !mvgDataset.isEmpty())
		{
			BigWarpInit.addToData( bigwarpdata, true, id, mvgRoot, mvgDataset );
			id++;
		}
		else if( mvgRoot != null && !mvgRoot.isEmpty() )
		{
			try
			{
				final ImagePlus movingFromFile = IJ.openImage( mvgRoot );
				id += BigWarpInit.add( bigwarpdata, movingFromFile, id, 0, true );
			}
			catch(final Exception e ) {
				IJ.showMessage( "could not read from file: " + mvgRoot );
				return;
			}
		}

		if ( targetIp != null )
		{
			id += BigWarpInit.add( bigwarpdata, targetIp, id, 0, false );
		}
		else if ( tgtDataset != null && !tgtDataset.isEmpty() )
		{
			BigWarpInit.addToData( bigwarpdata, false, id, tgtRoot, tgtDataset );
			id++;
		}
		else if( tgtRoot != null && !tgtRoot.isEmpty() )
		{
			try
			{
				final ImagePlus targetFromFile = IJ.openImage( tgtRoot );
				id += BigWarpInit.add( bigwarpdata, targetFromFile, id, 0, false );
			}
			catch(final Exception e ) {
				// we're allowed not to have a target image
			}

		}

		final int nd = BigWarp.detectNumDims( bigwarpdata.sources );
		BigWarp.wrapMovingSources(nd, bigwarpdata);

		final int[] blockSize = ApplyBigwarpPlugin.parseBlockSize( blockSizeString, nd );
		final Compression compression = ApplyBigwarpPlugin.getCompression( compressionString );
		final WriteDestinationOptions writeOpts = new ApplyBigwarpPlugin.WriteDestinationOptions( fileOrN5Root, n5Dataset,
				blockSize, compression );

		final LandmarkTableModel ltm = new LandmarkTableModel( nd );
		try
		{
			ltm.load( new File( landmarksPath ) );
		} catch ( final IOException e )
		{
			e.printStackTrace();
			return;
		}

		final Interpolation interp;
		if( interpType.equals( "Nearest Neighbor" ))
			interp = Interpolation.NEARESTNEIGHBOR;
		else
			interp = Interpolation.NLINEAR;

		final BoundingBoxEstimation bboxEst = new BoundingBoxEstimation(BoundingBoxEstimation.Method.valueOf(bboxOption), bboxSamples );

		final List<ImagePlus> warpedIpList = apply(bigwarpdata, ltm, transformTypeOption,
				fovOption, fovPointFilter, bboxEst,
				resOption, resolutions, fov, offset,
				interp, isVirtual, nThreads, false, writeOpts);
	}

	public static int[] parseBlockSize( final String blockSizeArg, final int nd )
	{
		if( blockSizeArg.isEmpty())
			return null;

		final int[] blockSize = new int[ nd ];
		final String[] blockArgList = blockSizeArg.split(",");
		int i = 0;
		while( i < blockArgList.length && i < nd )
		{
			blockSize[ i ] = Integer.parseInt( blockArgList[ i ] );
			i++;
		}
		final int N = blockArgList.length - 1;

		while( i < nd )
		{
			blockSize[ i ] = blockSize[ N ];
			i++;
		}
		return blockSize;
	}

	public static Compression getCompression( final String compressionArg ) {
		switch (compressionArg) {
		case N5Exporter.GZIP_COMPRESSION:
			return new GzipCompression();
		case N5Exporter.LZ4_COMPRESSION:
			return new Lz4Compression();
		case N5Exporter.XZ_COMPRESSION:
			return new XzCompression();
		case N5Exporter.RAW_COMPRESSION:
			return new RawCompression();
		case N5Exporter.BLOSC_COMPRESSION:
			return new BloscCompression();
		default:
			return new RawCompression();
		}
	}

	public static class WriteDestinationOptions
	{
		final public String pathOrN5Root;
		final public String n5Dataset;
		final public int[] blockSize;
		final public Compression compression;

		public WriteDestinationOptions( final String pathOrN5Root, final String n5Dataset,
				final int[] blockSize, final Compression compression )
		{
			this.pathOrN5Root = pathOrN5Root;
			this.n5Dataset = n5Dataset;
			this.blockSize = blockSize;
			this.compression = compression;
		}
	}

}
