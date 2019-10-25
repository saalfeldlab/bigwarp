package bdv.ij;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import bdv.export.ProgressWriter;
import bdv.gui.BigwarpLandmarkSelectionPanel;
import bdv.ij.util.ProgressWriterIJ;
import bdv.img.WarpedSource;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp.BigWarpData;
import bigwarp.BigWarp;
import bigwarp.BigWarpARGBExporter;
import bigwarp.BigWarpExporter;
import bigwarp.BigWarpInit;
import bigwarp.BigWarpRealExporter;
import bigwarp.landmarks.LandmarkTableModel;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.realtransform.Wrapped2DTransformAs3D;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.util.Intervals;

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

	public static void main( String[] args ) throws IOException
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
		// TODO finish this

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

		double[] res = new double[ 3 ];
		VoxelDimensions voxdims = source.getVoxelDimensions();
		if( voxdims == null )
			Arrays.fill( res, 1.0 );
		else
			voxdims.dimensions( res );

		return res;
	}

	public static String getUnit( final BigWarpData<?> bwData, 
			final String resolutionOption )
	{
		if( resolutionOption.equals( MOVING ))
		{
			return bwData.sources.get( bwData.movingSourceIndices[0] ).getSpimSource().getVoxelDimensions().unit();
		}
		else 
		{
			// use target units even if 
			return bwData.sources.get( bwData.targetSourceIndices[0] ).getSpimSource().getVoxelDimensions().unit();
		}
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
			if( bwData.targetSourceIndices.length <= 0 )
				return null;
			
			Source< ? > spimSource = bwData.sources.get( 
					bwData.targetSourceIndices[ 0 ]).getSpimSource();
			
			return getResolution( spimSource, resolutionOption, resolutionSpec );
		}
		else if( resolutionOption.equals( MOVING ))
		{
			if( bwData.targetSourceIndices.length <= 0 )
				return null;
			
			Source< ? > spimSource = bwData.sources.get( 
					bwData.movingSourceIndices[ 0 ]).getSpimSource();
			return getResolution( spimSource, resolutionOption, resolutionSpec );
		}
		else if( resolutionOption.equals( SPECIFIED ))
		{
			if( ( resolutionSpec == null ) )
			{
				System.err.println("Requested moving resolution but moving image is missing.");
				return null;
			}
			
			double[] res = new double[ 3 ];
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

			double[] res = new double[ 3 ];
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
			
			double[] res = new double[ 3 ];
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
			
			double[] res = new double[ 3 ];
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
			final String fieldOfViewOption,
			final String fieldOfViewPointFilter,
			final double[] fovSpec,
			final double[] offsetSpec,
			final double[] outputResolution )
	{
		BigWarpData<?> bwData = BigWarpInit.createBigWarpDataFromImages( movingIp, targetIp );
		return getPixelInterval( bwData, landmarks, fieldOfViewOption,
				fieldOfViewPointFilter, fovSpec, offsetSpec, outputResolution );
	}
	
	public static Interval getPixelInterval(
			final Source< ? > source,
			final LandmarkTableModel landmarks,
			final String fieldOfViewOption,
			final double[] outputResolution )
	{
		RandomAccessibleInterval< ? > rai = source.getSource( 0, 0 );
		

		if( fieldOfViewOption.equals( TARGET ))
		{
			double[] inputres = new double[ 3 ];
			VoxelDimensions voxdims = source.getVoxelDimensions();
			if( voxdims == null )
				Arrays.fill( inputres, 1.0 );
			else
				voxdims.dimensions( inputres );

			long[] max = new long[ rai.numDimensions() ];
			for( int d = 0; d < rai.numDimensions(); d++ )
			{
				max[ d ] = (long)Math.ceil( ( inputres[ d ] * rai.dimension( d )) / outputResolution[ d ]);
			}

			return new FinalInterval( max );
		}
		else if( fieldOfViewOption.equals( MOVING_WARPED ))
		{
			ThinPlateR2LogRSplineKernelTransform tps = landmarks.getTransform();
			double[] movingRes = new double[ 3 ];
			source.getVoxelDimensions().dimensions( movingRes );

			AffineTransform movingPixelToPhysical = new AffineTransform( tps.getNumDims() );
			movingPixelToPhysical.set( movingRes[ 0 ], 0, 0 );
			movingPixelToPhysical.set( movingRes[ 1 ], 1, 1 );
			if( tps.getNumDims() > 2 )
				movingPixelToPhysical.set( movingRes[ 2 ], 2, 2 );

			AffineTransform outputResolution2Pixel = new AffineTransform( tps.getNumDims() );
			outputResolution2Pixel.set( outputResolution[ 0 ], 0, 0 );
			outputResolution2Pixel.set( outputResolution[ 1 ], 1, 1  );
			if( tps.getNumDims() > 2 )
				outputResolution2Pixel.set( outputResolution[ 2 ], 2, 2  );
			
			RealTransformSequence seq = new RealTransformSequence();
			seq.add( movingPixelToPhysical );
			seq.add( new WrappedIterativeInvertibleRealTransform<>( new ThinplateSplineTransform( tps )).inverse() );
			seq.add( outputResolution2Pixel.inverse() );
			
			FinalInterval interval = new FinalInterval(
					Intervals.minAsLongArray( rai ),
					Intervals.maxAsLongArray( rai ));

			return BigWarpExporter.estimateBounds( seq, interval );
		}
		else if( fieldOfViewOption.equals( UNION_TARGET_MOVING ))
		{
			Interval movingWarpedInterval = getPixelInterval( source, landmarks, MOVING_WARPED, outputResolution );
			Interval targetInterval = getPixelInterval( source, landmarks, TARGET, outputResolution );
			return Intervals.union( movingWarpedInterval, targetInterval );
		}

		return null;
	}

	/**
	 * Returns the interval in pixels of the output given input options
	 * 
	 * @param bwData the BigWarpData
	 * @param landmarks the landmarks
	 * @param fieldOfViewOption the field of view option
     * @param fieldOfViewPointFilter the regexp for filtering landmarks points by name
	 * @param fovSpec the field of view specification
	 * @param offsetSpec the offset specification 
	 * @param outputResolution the resolution of the output image
	 * @return the output interval 
	 */
	public static List<Interval> getPixelInterval(
			final BigWarpData<?> bwData,
			final LandmarkTableModel landmarks,
			final String fieldOfViewOption,
			final String fieldOfViewPointFilter,
			final double[] fovSpec,
			final double[] offsetSpec,
			final double[] outputResolution )
	{
		if( fieldOfViewOption.equals( TARGET ))
		{
			if ( bwData.targetSourceIndices.length <= 0 )
			{
				System.err.println("Requested target fov but target image is missing.");
				return null;
			}

			return Stream.of( 
				getPixelInterval(
					bwData.sources.get( bwData.targetSourceIndices[ 0 ]).getSpimSource(),
					landmarks, fieldOfViewOption, outputResolution )
				).collect(Collectors.toList());
		}
		else if( fieldOfViewOption.equals( MOVING_WARPED ))
		{
			return Stream.of( 
				getPixelInterval(
					bwData.sources.get( bwData.movingSourceIndices[ 0 ]).getSpimSource(),
					landmarks, fieldOfViewOption, outputResolution )
				).collect(Collectors.toList());
		}
		else if( fieldOfViewOption.equals( UNION_TARGET_MOVING ))
		{

			return Stream.of(
				getPixelInterval(
					bwData.sources.get( bwData.movingSourceIndices[ 0 ]).getSpimSource(),
					landmarks, fieldOfViewOption, outputResolution )
				).collect(Collectors.toList());
		}
		else if( fieldOfViewOption.equals( SPECIFIED_PIXEL ) )
		{
			if( fovSpec.length == 2 )
			{
				long[] min = new long[]{ 
						(long)Math.floor( offsetSpec[ 0 ] ),
						(long)Math.floor( offsetSpec[ 1 ] ) };

				long[] max = new long[]{
					(long)Math.ceil( fovSpec[ 0 ] ),
					(long)Math.ceil( fovSpec[ 1 ] ) };
				
				ArrayList<Interval> out = new ArrayList<>();
				out.add( new FinalInterval( min, max ));
				return out;
			}
			else if( fovSpec.length == 3 )
			{
				long[] min = new long[]{ 
						(long)Math.floor( offsetSpec[ 0 ] ),
						(long)Math.floor( offsetSpec[ 1 ] ),
						(long)Math.floor( offsetSpec[ 2 ] ) };

				long[] max = new long[]{
						(long)Math.ceil( fovSpec[ 0 ] ),
						(long)Math.ceil( fovSpec[ 1 ] ),
						(long)Math.ceil( fovSpec[ 2 ] ) };

				ArrayList<Interval> out = new ArrayList<>();
				out.add( new FinalInterval( min, max ));
				return out;
			}
			else
			{
				System.out.println("Invalid fov spec, length : " + fovSpec.length );
				return null;
			}
		}
		else if( fieldOfViewOption.equals( SPECIFIED_PHYSICAL ) )
		{
			if( fovSpec.length == 2 )
			{
				long[] min = new long[]{ 
						(long)Math.floor( offsetSpec[ 0 ] / outputResolution[ 0 ]),
						(long)Math.floor( offsetSpec[ 1 ] / outputResolution[ 1 ]) };

				long[] max = new long[]{
					(long)Math.ceil( fovSpec[ 0 ] / outputResolution[ 0 ]),
					(long)Math.ceil( fovSpec[ 1 ] / outputResolution[ 1 ]) };

				ArrayList<Interval> out = new ArrayList<>();
				out.add( new FinalInterval( min, max ));
				return out;
			}
			else if( fovSpec.length == 3 )
			{
				long[] min = new long[]{ 
						(long)Math.floor( offsetSpec[ 0 ] / outputResolution[ 0 ]),
						(long)Math.floor( offsetSpec[ 1 ] / outputResolution[ 1 ]),
						(long)Math.floor( offsetSpec[ 2 ] / outputResolution[ 2 ]) };

				long[] max = new long[]{
						(long)Math.ceil( fovSpec[ 0 ] / outputResolution[ 0 ]),
						(long)Math.ceil( fovSpec[ 1 ] / outputResolution[ 1 ]),
						(long)Math.ceil( fovSpec[ 2 ] / outputResolution[ 2 ]) };

				ArrayList<Interval> out = new ArrayList<>();
				out.add( new FinalInterval( min, max ));
				return out;
			}
			else
			{
				System.out.println("Invalid fov spec, length : " + fovSpec.length );
				return null;
			}
		}
		else if( fieldOfViewOption.equals( LANDMARK_POINTS ) )
		{
			List< Double[] > matchedLandmarks = getMatchedPoints( landmarks, fieldOfViewPointFilter );

			long[] min = new long[ landmarks.getNumdims() ];
			long[] max = new long[ landmarks.getNumdims() ];

			Arrays.fill( min, Long.MAX_VALUE );
			Arrays.fill( max, Long.MIN_VALUE );

			int numPoints = 0;
			for ( int i = 0; i < matchedLandmarks.size(); i++ )
			{
				Double[] pt = matchedLandmarks.get( i );
				for ( int d = 0; d < pt.length; d++ )
				{
					long lo = (long) (Math.floor( pt[ d ] / outputResolution[ d ] ));
					long hi = (long) (Math.ceil( pt[ d ] / outputResolution[ d ] ));

					if ( lo < min[ d ] )
						min[ d ] = lo;

					if ( hi > max[ d ] )
						max[ d ] = hi;

				}
				numPoints++;
			}

			System.out.println( "Estimated field of view using " + numPoints + " landmarks." );

			// Make sure something naughty didn't happen
			for ( int d = 0; d < min.length; d++ )
			{
				if ( min[ d ] == Long.MAX_VALUE )
				{
					System.err.println( "Problem generating field of view from landmarks" );
					return null;
				}

				if ( max[ d ] == Long.MIN_VALUE )
				{
					System.err.println( "Problem generating field of view from landmarks" );
					return null;
				}
			}

			ArrayList<Interval> out = new ArrayList<>();
			out.add( new FinalInterval( min, max ));
			return out;
		}
		else if( fieldOfViewOption.equals( LANDMARK_POINT_CUBE_PHYSICAL ) 
		  || fieldOfViewOption.equals( LANDMARK_POINT_CUBE_PIXEL ) )
		{
			List< Double[] > matchedLandmarks = getMatchedPoints( landmarks, fieldOfViewPointFilter );
			if( matchedLandmarks.isEmpty() )
			{
				System.err.println( "No matching point found" );
				return null;
			}

			final int nd = landmarks.getNumdims();
			ArrayList<Interval> out = new ArrayList<>();

			for( int i = 0; i < matchedLandmarks.size(); i++ )
			{
				final Double[] pt = matchedLandmarks.get( i );
				long[] min = new long[ nd ];
				long[] max = new long[ nd ];
				if( fieldOfViewOption.equals( LANDMARK_POINT_CUBE_PHYSICAL ) )
				{
					for ( int d = 0; d < nd; d++ )
					{
						min[ d ] = ( long ) Math.floor( (pt[ d ] / outputResolution[ d ]) - ( fovSpec[ d ] / outputResolution[ d ] ) );
						max[ d ] = ( long ) Math.ceil( (pt[ d ] / outputResolution[ d ]) + ( fovSpec[ d ] / outputResolution[ d ] ) );
					}
				}
				else
				{
					for ( int d = 0; d < nd; d++ )
					{
						min[ d ] = ( long ) Math.floor( (pt[ d ] / outputResolution[ d ]) - ( fovSpec[ d ] ) ) + 1;
						max[ d ] = ( long ) Math.ceil( (pt[ d ] / outputResolution[ d ]) + ( fovSpec[ d ] ) ) - 1;
					}
				}

				out.add( new FinalInterval( min, max ) );
			}
			return out;
		}

		System.err.println("Invalid field of view option: ( " + fieldOfViewOption + " )" );
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

			Double[] pt = landmarks.getFixedPoint( i );
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
		ArrayList<Double[]> ptList = new ArrayList<>();
		
		Pattern r = null;
		if ( !fieldOfViewPointFilter.isEmpty() )
			r = Pattern.compile( fieldOfViewPointFilter );

		for ( int i = 0; i < landmarks.getRowCount(); i++ )
		{
			if ( r != null && !r.matcher( landmarks.getNames().get( i ) ).matches() )
			{
				continue;
			}
			
			Double[] pt = landmarks.getFixedPoint( i );
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
		double[] offset = new double[ 3 ];
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

		for( int d = 0; d < outputInterval.numDimensions(); d++ )
		{
			offset[ d ] = outputInterval.realMin( d );
		}

		return offset;
	}

	public static <T> List<ImagePlus> apply(
			final ImagePlus movingIp,
			final ImagePlus targetIp,
			final LandmarkTableModel landmarks,
			final String fieldOfViewOption,
			final String fieldOfViewPointFilter,
			final String resolutionOption,
			final double[] resolutionSpec,
			final double[] fovSpec,
			final double[] offsetSpec,
			final Interpolation interp,
			final boolean isVirtual,
			final int nThreads )
	{
		BigWarpData<?> bwData = BigWarpInit.createBigWarpDataFromImages( movingIp, targetIp );
		return apply( bwData, landmarks, fieldOfViewOption, fieldOfViewPointFilter,
				resolutionOption, resolutionSpec, fovSpec, offsetSpec, 
				interp, isVirtual, nThreads );
	}

	@SuppressWarnings( { "rawtypes" } )
	public static <T> List<ImagePlus> apply(
			final BigWarpData<T> bwData,
			final LandmarkTableModel landmarks,
			final String fieldOfViewOption,
			final String fieldOfViewPointFilter,
			final String resolutionOption,
			final double[] resolutionSpec,
			final double[] fovSpec,
			final double[] offsetSpec,
			final Interpolation interp,
			final boolean isVirtual,
			final int nThreads )
	{
		int numChannels = bwData.movingSourceIndices.length;
		List< SourceAndConverter<T> > sources = bwData.sources;
		int[] movingSourceIndexList = bwData.movingSourceIndices;
		List< SourceAndConverter< T >> sourcesxfm = BigWarp.wrapSourcesAsTransformed(
				sources, 
				landmarks.getNumdims(),
				movingSourceIndexList );

		ThinPlateR2LogRSplineKernelTransform xfm = landmarks.getTransform();
		InvertibleRealTransform invXfm = new WrappedIterativeInvertibleRealTransform<>( new ThinplateSplineTransform( xfm ) );

		boolean is2d = sources.get( movingSourceIndexList[ 0 ] ).getSpimSource().getSource( 0, 0 ).dimension( 2 ) < 2;
		if( is2d )
		{
			invXfm = new Wrapped2DTransformAs3D( invXfm );
		}

		for ( int i = 0; i < numChannels; i++ )
		{
			System.out.println( "transforming source " + movingSourceIndexList[ i ] );
			((WarpedSource< T >) (sourcesxfm.get( movingSourceIndexList[ i ]).getSpimSource())).updateTransform( invXfm );
			((WarpedSource< T >) (sourcesxfm.get( movingSourceIndexList[ i ]).getSpimSource())).setIsTransformed( true );
		}

		ProgressWriter progressWriter = new ProgressWriterIJ();

		// Generate the properties needed to generate the transform from output pixel space
		// to physical space
		double[] res = getResolution( bwData, resolutionOption, resolutionSpec );
		String unit = getUnit( bwData, resolutionOption );

		List<Interval> outputIntervalList = getPixelInterval( bwData, landmarks, fieldOfViewOption, 
				fieldOfViewPointFilter, fovSpec, offsetSpec, res );

		final List<String> matchedPtNames = new ArrayList<>();
		if( outputIntervalList.size() > 1 )
			ApplyBigwarpPlugin.fillMatchedPointNames( matchedPtNames, landmarks, fieldOfViewPointFilter );

		double[] offset = getPixelOffset( fieldOfViewOption, offsetSpec, res, outputIntervalList.get( 0 ) );
		
		return runExport( bwData, sourcesxfm, fieldOfViewOption,
				outputIntervalList, matchedPtNames, interp,
				offset, res, isVirtual, nThreads, 
				progressWriter, false );
	}

	public static <T> List<ImagePlus> runExport(
			final BigWarpData<T> data,
			final List< SourceAndConverter< T >> sources,
			final String fieldOfViewOption,
			final List<Interval> outputIntervalList,
			final List<String> matchedPtNames,
			final Interpolation interp,
			final double[] offsetIn,
			final double[] resolution,
			final boolean isVirtual,
			final int nThreads,
			final ProgressWriter progressWriter,
			final boolean show
			)
	{
		ArrayList<ImagePlus> ipList = new ArrayList<>();

		int i = 0;
		for( Interval outputInterval : outputIntervalList )
		{
			double[] offset = ApplyBigwarpPlugin.getPixelOffset( fieldOfViewOption, offsetIn, resolution, outputIntervalList.get( i ) );

			// need to declare the exporter in the loop since the actual work
			// is done asynchronously, and changing variables in the loop would mess it up
			BigWarpExporter< ? > exporter = BigWarpExporter.getExporter( data, sources, interp, progressWriter );
			exporter.setRenderResolution( resolution );
			exporter.setOffset( offset );
			exporter.setVirtual( isVirtual );
			exporter.setNumThreads( nThreads );

			//System.out.println( "interval: " + Util.printInterval( outputInterval ) );
			exporter.setInterval( outputInterval );

			if( matchedPtNames.size() > 0 )
				exporter.setNameSuffix( matchedPtNames.get( i ));

			ImagePlus ip = exporter.exportAsynch( true );
			ipList.add( ip );

			if( ip != null && show )
				ip.show();

			i++;
		}
		return ipList;
	}

	@Override
	public void run( String arg )
	{
		if ( IJ.versionLessThan( "1.40" ) )
			return;

		final GenericDialog gd = new GenericDialog( "Apply Big Warp transform" );
		gd.addMessage( "File Selection:" );
		gd.addStringField( "landmarks_image_file", "" );

		gd.addStringField( "moving_image_file", "" );
		gd.addStringField( "target_space_file", "" );

		gd.addMessage( "Field of view and resolution:" );
		gd.addChoice( "Resolution", 
				new String[]{ TARGET, MOVING, SPECIFIED },
				TARGET );

		gd.addChoice( "Field of view", 
				new String[]{ TARGET, MOVING_WARPED, LANDMARK_POINTS, SPECIFIED_PIXEL, SPECIFIED_PHYSICAL },
				TARGET );

//		gd.addStringField( "point filter", "BND.*" );
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

		gd.showDialog();

		if ( gd.wasCanceled() )
			return;

		String landmarksPath = gd.getNextString();
		String movingPath = gd.getNextString();
		String targetPath = gd.getNextString();
		
		String resOption = gd.getNextChoice();
		String fovOption = gd.getNextChoice();
		String fovPointFilter = gd.getNextString();
		
		double[] resolutions = new double[ 3 ];
		resolutions[ 0 ] = gd.getNextNumber();
		resolutions[ 1 ] = gd.getNextNumber();
		resolutions[ 2 ] = gd.getNextNumber();
		
		double[] offset = new double[ 3 ];
		offset[ 0 ] = gd.getNextNumber();
		offset[ 1 ] = gd.getNextNumber();
		offset[ 2 ] = gd.getNextNumber();
		
		double[] fov = new double[ 3 ];
		fov[ 0 ] = gd.getNextNumber();
		fov[ 1 ] = gd.getNextNumber();
		fov[ 2 ] = gd.getNextNumber();

		String interpType = gd.getNextChoice();
		boolean isVirtual = gd.getNextBoolean();
		int nThreads = (int)gd.getNextNumber();

		ImagePlus movingIp = IJ.openImage( movingPath );
		ImagePlus targetIp = movingIp;

		if ( !targetPath.isEmpty() )
			targetIp = IJ.openImage( targetPath );

		int nd = 2;
		if ( movingIp.getNSlices() > 1 )
			nd = 3;

		LandmarkTableModel ltm = new LandmarkTableModel( nd );
		try
		{
			ltm.load( new File( landmarksPath ) );
		} catch ( IOException e )
		{
			e.printStackTrace();
			return;
		}

		Interpolation interp = Interpolation.NLINEAR;
		if( interpType.equals( "Nearest Neighbor" ))
			interp = Interpolation.NEARESTNEIGHBOR;
		
		List<ImagePlus> warpedIpList = apply( movingIp, targetIp, ltm,
				fovOption, fovPointFilter, resOption,
				resolutions, fov, offset,
				interp, isVirtual, nThreads );

		for( ImagePlus warpedIp : warpedIpList )
			warpedIp.show();

	}

}
