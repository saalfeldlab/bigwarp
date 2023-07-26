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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;
import org.janelia.saalfeldlab.n5.blosc.BloscCompression;
import org.janelia.saalfeldlab.n5.ij.N5Exporter;
import org.janelia.saalfeldlab.n5.imglib2.N5DisplacementField;
import org.janelia.saalfeldlab.n5.universe.N5Factory;

import bdv.gui.TransformTypeSelectDialog;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarpExporter;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.img.imageplus.FloatImagePlus;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.Scale2D;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import net.imglib2.view.composite.CompositeIntervalView;
import net.imglib2.view.composite.GenericComposite;

/**
 * ImageJ plugin to convert the thin plate spline to a deformation field.
 *
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public class BigWarpToDeformationFieldPlugIn implements PlugIn
{
	public static final String[] compressionOptions = new String[] {
				N5Exporter.RAW_COMPRESSION,
				N5Exporter.GZIP_COMPRESSION,
				N5Exporter.LZ4_COMPRESSION,
				N5Exporter.XZ_COMPRESSION,
				N5Exporter.BLOSC_COMPRESSION };

	public static void main( final String[] args )
	{
		new ImageJ();
//		IJ.run("Boats (356K)");
		final ImagePlus imp = IJ.openImage( "/groups/saalfeld/home/bogovicj/tmp/mri-stack.tif" );
//		ImagePlus imp = IJ.openImage( "/groups/saalfeld/home/bogovicj/tmp/mri-stack_p2p2p4.tif" );

//		WindowManager.getActiveWindow();
		imp.show();

		new BigWarpToDeformationFieldPlugIn().run( null );
	}

	public <T> void runFromBigWarpInstance(
			final LandmarkTableModel landmarkModel,
			final List<SourceAndConverter<T>> sources,
			final int[] targetSourceIndexList )
	{
		final ImageJ ij = IJ.getInstance();
		if ( ij == null )
			return;


		final DeformationFieldExportParameters params = DeformationFieldExportParameters.fromDialog( false, false );
		if( params == null )
			return;

		final RandomAccessibleInterval< ? > tgtInterval = sources.get( targetSourceIndexList[ 0 ] ).getSpimSource().getSource( 0, 0 );

		final int ndims = landmarkModel.getNumdims();
		long[] dims;
		if ( ndims <= 2 )
		{
			dims = new long[ 3 ];
			dims[ 0 ] = tgtInterval.dimension( 0 );
			dims[ 1 ] = tgtInterval.dimension( 1 );
			dims[ 2 ] = 2;
		}
		else
		{
			dims = new long[ 4 ];
			dims[ 0 ] = tgtInterval.dimension( 0 );
			dims[ 1 ] = tgtInterval.dimension( 1 );
			dims[ 2 ] = 3;
			dims[ 3 ] = tgtInterval.dimension( 2 );
		}

		double[] spacing = new double[ 3 ];
		final VoxelDimensions voxelDim = sources.get( targetSourceIndexList[ 0 ] ).getSpimSource().getVoxelDimensions();
		voxelDim.dimensions( spacing );

		if( params.spacing != null )
			spacing = params.spacing;

		if( params.size != null )
			dims = params.size;

		if( params.n5Base.isEmpty() )
		{
			toImagePlus( landmarkModel, params.ignoreAffine, dims, spacing, params.nThreads );
		}
		else
		{
			try
			{
				writeN5( params.n5Base, landmarkModel, dims, spacing, params.blockSize, params.compression, params.nThreads );
			}
			catch ( final N5Exception e )
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run( final String arg )
	{
		if ( IJ.versionLessThan( "1.40" ) )
			return;

		final DeformationFieldExportParameters params = DeformationFieldExportParameters.fromDialog( true, true );
		final int nd = params.size.length;

		// load
		final LandmarkTableModel ltm = new LandmarkTableModel( nd );
		try
		{
			ltm.load( new File( params.landmarkPath ) );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
			return;
		}

		if( params.n5Base.isEmpty() )
		{
			toImagePlus( ltm, params.ignoreAffine, params.size, params.spacing, params.nThreads );
		}
		else
		{
			try
			{
				writeN5( params.n5Base, params.n5Dataset, ltm, params.size, params.spacing, params.blockSize, params.compression, params.nThreads );
			}
			catch ( final N5Exception e )
			{
				e.printStackTrace();
			}
		}
	}

	public static ImagePlus toImagePlus(
			final LandmarkTableModel ltm,
			final boolean ignoreAffine,
			final long[] dims,
			final double[] spacing,
			final int nThreads )
	{
		final BigWarpTransform bwXfm = new BigWarpTransform( ltm, TransformTypeSelectDialog.TPS );
		final RealTransform tps = getTpsAffineToggle( bwXfm, ignoreAffine );

		AffineGet pixelToPhysical = null;
		if( spacing.length == 2)
		{
			pixelToPhysical = new Scale2D( spacing );
		}
		else if( spacing.length == 3)
		{
			pixelToPhysical = new Scale3D( spacing );
		}
		else
		{
			return null;
		}

		final FloatImagePlus< FloatType > dfield = convertToDeformationField( dims, tps, pixelToPhysical, nThreads );

		String title = "bigwarp dfield";
		if ( ignoreAffine )
			title += " (no affine)";

		final ImagePlus dfieldIp = dfield.getImagePlus();
		dfieldIp.setTitle( title );

		dfieldIp.getCalibration().pixelWidth = spacing[ 0 ];
		dfieldIp.getCalibration().pixelHeight = spacing[ 1 ];

		if( spacing.length > 2 )
			dfieldIp.getCalibration().pixelDepth = spacing[ 2 ];

		dfieldIp.show();
		return dfieldIp;
	}

	public static void writeN5(
			final String n5BasePath,
			final LandmarkTableModel ltm,
			final long[] dims,
			final double[] spacing,
			final int[] spatialBlockSize,
			final Compression compression,
			final int nThreads )
	{
		writeN5( n5BasePath, "dfield", ltm, dims, spacing, spatialBlockSize, compression, nThreads );
	}

	public static void writeN5( final String n5BasePath, final String n5Dataset,
			final LandmarkTableModel ltm,
			final long[] dims,
			final double[] spacing,
			final int[] spatialBlockSize,
			final Compression compression,
			final int nThreads )
	{
		final BigWarpTransform bwXfm = new BigWarpTransform( ltm, TransformTypeSelectDialog.TPS );
		final RealTransform tpsTotal = getTpsAffineToggle( bwXfm, false );

		AffineGet pixelToPhysical = null;
		if( spacing.length == 2 )
			pixelToPhysical = new Scale2D( spacing );
		else if( spacing.length == 3 )
			pixelToPhysical = new Scale3D( spacing );

		final FloatImagePlus< FloatType > dfieldRaw = convertToDeformationField(
				dims, tpsTotal, pixelToPhysical, nThreads );

		// this works for both 2d and 3d, it turn out
		final RandomAccessibleInterval< FloatType > dfield =
					Views.permute(
						Views.permute( dfieldRaw,
								0, 2 ),
						1, 2 );

		final int[] blockSize = new int[ spatialBlockSize.length + 1 ];
		blockSize[ 0 ] = spatialBlockSize.length;
		for( int i = 0; i < spatialBlockSize.length; i++ )
		{
			blockSize[ i + 1 ] = spatialBlockSize[ i ];
		}

		final N5Writer n5 = new N5Factory().openWriter( n5BasePath );
		N5DisplacementField.save( n5, n5Dataset, null, dfield, spacing, blockSize, compression );
		n5.close();
	}

	private static RealTransform getTpsAffineToggle( final BigWarpTransform bwXfm, final boolean ignoreAffine )
	{
		if( ignoreAffine )
		{
			final ThinPlateR2LogRSplineKernelTransform tps = bwXfm.getTpsBase();
			return new ThinplateSplineTransform(
						new ThinPlateR2LogRSplineKernelTransform( tps.getSourceLandmarks(), null, null, tps.getKnotWeights() ));
		}
		else
			return bwXfm.getTransformation();
	}

	public static AffineGet toAffine( final ThinPlateR2LogRSplineKernelTransform tps )
	{
		final double[] affineFlat = toFlatAffine( tps );
		if( affineFlat.length == 6 )
		{
			final AffineTransform2D affine = new AffineTransform2D();
			affine.set( affineFlat );
			return affine;
		}
		else if( affineFlat.length == 12 )
		{
			final AffineTransform3D affine = new AffineTransform3D();
			affine.set( affineFlat );
			return affine;
		}
		else
			return null;
	}

	public static double[] toFlatAffine( final ThinPlateR2LogRSplineKernelTransform tps )
	{
		// move this method somewhere more central?

		final double[][] tpsAffine = tps.getAffine();
		final double[] translation = tps.getTranslation();

		double[] affine;
		if( tps.getNumDims() == 2)
		{
			affine = new double[ 6 ];

			affine[ 0 ] = 1 + tpsAffine[ 0 ][ 0 ];
			affine[ 1 ] = tpsAffine[ 0 ][ 1 ];
			affine[ 2 ] = translation[ 0 ];

			affine[ 3 ] = tpsAffine[ 1 ][ 0 ];
			affine[ 4 ] = 1 + tpsAffine[ 1 ][ 1 ];
			affine[ 5 ] = translation[ 1 ];
		}
		else
		{
			affine = new double[ 12 ];

			affine[ 0 ] = 1 + tpsAffine[ 0 ][ 0 ];
			affine[ 1 ] = tpsAffine[ 0 ][ 1 ];
			affine[ 2 ] = tpsAffine[ 0 ][ 2 ];
			affine[ 3 ] = translation[ 0 ];

			affine[ 4 ] = tpsAffine[ 1 ][ 0 ];
			affine[ 5 ] = 1 + tpsAffine[ 1 ][ 1 ];
			affine[ 6 ] = tpsAffine[ 1 ][ 2 ];
			affine[ 7 ] = translation[ 1 ];

			affine[ 8 ] = tpsAffine[ 2 ][ 0 ];
			affine[ 9 ] = tpsAffine[ 2 ][ 1 ];
			affine[ 10 ] = 1 + tpsAffine[ 2 ][ 2 ];
			affine[ 11 ] = translation[ 2 ];
		}
		return affine;
	}

	public static long[] dimensionsFromImagePlus( final ImagePlus ref_imp )
	{
		long[] dims;
		if( ref_imp.getNSlices() < 2 )
		{
			dims = new long[ 3 ];
			dims[ 0 ] = ref_imp.getWidth();
			dims[ 1 ] = ref_imp.getHeight();
			dims[ 2 ] = 2;
		}
		else
		{
			dims = new long[ 4 ];
			dims[ 0 ] = ref_imp.getWidth();
			dims[ 1 ] = ref_imp.getHeight();
			dims[ 2 ] = 3;
			dims[ 3 ] = ref_imp.getNSlices();
		}
		return dims;
	}

	public static FloatImagePlus< FloatType > convertToDeformationField(
			final long[] dims,
			final RealTransform transform,
			final AffineGet pixToPhysical,
			final int nThreads)
	{
		final FloatImagePlus< FloatType > deformationField = ImagePlusImgs.floats( dims );

		RandomAccessibleInterval<FloatType> dfieldPermuted = deformationField;
		if( dims.length == 4 )
			dfieldPermuted = Views.permute( deformationField, 2, 3 );

		if( nThreads <= 1 )
			fromRealTransform( transform, pixToPhysical, dfieldPermuted );
		else
			fromRealTransform( transform, pixToPhysical, dfieldPermuted, nThreads );

		return deformationField;
	}

	public static boolean areTransformsTheSame( final RealTransform xfm1, final RealTransform xfm2, final Interval itvl, final double EPS )
	{

		final double[] pArray = new double[ 3 ];
		final double[] qArray = new double[ 3 ];
		final RealPoint p = RealPoint.wrap( pArray );
		final RealPoint q = RealPoint.wrap( qArray );

		final IntervalIterator c = new IntervalIterator( itvl );
		while ( c.hasNext() )
		{
			c.fwd();
			xfm1.apply( c, p );
			xfm2.apply( c, q );

			for( int d = 0; d < itvl.numDimensions(); d++ )
				if( Math.abs( p.getDoublePosition( d ) - q.getDoublePosition( d )) > EPS )
					return false;
		}
		return true;
	}

	/**
	 * Converts a {@link RealTransform} into a deformation field.
	 *
	 * Writes the result into the passed {@link RandomAccessibleInterval}. If
	 * the transform has N source dimensions, then the deformation field must
	 * have at least N+1 dimensions where the last dimensions of of length at
	 * least N.
	 *
	 * A DeformationField creating with the resulting {@link RandomAccessibleInterval}
	 * will give the same results as the transform inside its Interval.
	 *
     * @param <T> the type of the deformation field
	 * @param transform
	 *            the {@link RealTransform} to convert
	 * @param pixelToPhysical
	 * 			  the transform from coordinates in the {@link RandomAccessibleInterval} to
	 * 		      physical units
	 * @param deformationField
	 *            the {@link RandomAccessibleInterval} into which the
	 *            displacement field will be written
	 */
	public static < T extends RealType< T > > void fromRealTransform(
			final RealTransform transform,
			final AffineGet pixelToPhysical,
			final RandomAccessibleInterval< T > deformationField )
	{
		assert deformationField.numDimensions() == ( transform.numSourceDimensions() + 1 );
		assert deformationField.dimension( deformationField.numDimensions() - 1 ) >= transform.numSourceDimensions();

		final int N = transform.numSourceDimensions();
		final RealPoint p = new RealPoint( transform.numTargetDimensions() );
		final RealPoint q = new RealPoint( transform.numTargetDimensions() );

		final CompositeIntervalView< T, ? extends GenericComposite< T > > col = Views.collapse( deformationField );
		final Cursor< ? extends GenericComposite< T > > c = Views.flatIterable( col ).cursor();
		while ( c.hasNext() )
		{
			final GenericComposite< T > displacementVector = c.next();

			// transform the location of the cursor
			// and store the displacement
			pixelToPhysical.apply( c, p );
			transform.apply( p, q );

			for ( int i = 0; i < N; i++ )
				displacementVector.get( i ).setReal( q.getDoublePosition( i ) - p.getDoublePosition( i ) );
		}
	}

	/**
	 * Converts a {@link RealTransform} into a deformation field.
	 *
	 * Writes the result into the passed {@link RandomAccessibleInterval}. If
	 * the transform has N source dimensions, then the deformation field must
	 * have at least N+1 dimensions where the last dimensions of of length at
	 * least N.
	 *
	 * A DeformationField creating with the resulting {@link RandomAccessibleInterval}
	 * will give the same results as the transform inside its Interval.
	 *
     * @param <T> the type of the deformation field
	 * @param transform
	 *            the {@link RealTransform} to convert
	 * @param pixelToPhysical
	 * 			  the transform from coordinates in the {@link RandomAccessibleInterval} to
	 * 		      physical units
	 * @param deformationField
	 *            the {@link RandomAccessibleInterval} into which the
	 *            displacement field will be written
	 * @param nThreads
	 *            the number of threads
	 */
	public static < T extends RealType< T > > void fromRealTransform( final RealTransform transform,
			final AffineGet pixelToPhysical,
			final RandomAccessibleInterval< T > deformationField,
			final int nThreads)
	{
		assert deformationField.numDimensions() == ( transform.numSourceDimensions() + 1 );
		assert deformationField.dimension( deformationField.numDimensions() - 1 ) >= transform.numSourceDimensions();

		final int ndims = transform.numSourceDimensions();
		final long[] splitPoints = new long[ nThreads + 1 ];
		long N;
		final int dim2split;
		if( ndims == 2 )
		{
			N = deformationField.dimension( 1 );
			dim2split = 1;
		}
		else
		{
			N = deformationField.dimension( 2 );
			dim2split = 2;
		}

		final long del = ( long )( N / nThreads );
		splitPoints[ 0 ] = 0;
		splitPoints[ nThreads ] = deformationField.dimension( dim2split );
		for( int i = 1; i < nThreads; i++ )
		{
			splitPoints[ i ] = splitPoints[ i - 1 ] + del;
		}

		final ExecutorService threadPool = Executors.newFixedThreadPool( nThreads );
		final LinkedList<Callable<Boolean>> jobs = new LinkedList<Callable<Boolean>>();

		for( int i = 0; i < nThreads; i++ )
		{
			final long start = splitPoints[ i ];
			final long end   = splitPoints[ i+1 ];

			final RealTransform transformCopy = transform.copy();
			final RealTransform toPhysicalCopy = pixelToPhysical.copy();

			jobs.add( new Callable<Boolean>()
			{
				@Override
				public Boolean call()
				{
					try
					{
						final RealPoint p = new RealPoint( transform.numTargetDimensions() );
						final RealPoint q = new RealPoint( transform.numTargetDimensions() );

						final FinalInterval subItvl = BigWarpExporter.getSubInterval( deformationField, dim2split, start, end );
						final CompositeIntervalView< T, ? extends GenericComposite< T > > col = Views.collapse( deformationField );
						final IntervalView< ? extends GenericComposite< T > > subTgt = Views.interval( col, subItvl );

						final Cursor< ? extends GenericComposite< T > > c = Views.flatIterable( subTgt ).cursor();
						while ( c.hasNext() )
						{
							final GenericComposite< T > displacementVector = c.next();

							// transform the location of the cursor
							// and store the displacement
							toPhysicalCopy.apply( c, p );
							transformCopy.apply( p, q );

							for ( int i = 0; i < ndims; i++ )
								displacementVector.get( i ).setReal( q.getDoublePosition( i ) - p.getDoublePosition( i ) );
						}
						return true;
					}
					catch( final Exception e )
					{
						e.printStackTrace();
					}
					return false;
				}
			});
		}
		try
		{
			final List< Future< Boolean > > futures = threadPool.invokeAll( jobs );
			for( final Future<Boolean> f : futures )
					f.get();

			threadPool.shutdown(); // wait for all jobs to finish
		}
		catch ( final InterruptedException e1 )
		{
			e1.printStackTrace();
		}
		catch ( final ExecutionException e )
		{
			e.printStackTrace();
		}
	}

	private static Compression getCompression( final String compressionArg )
	{
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

	/**
	 * A helper that stores the parameters for export
	 * and can prompt the user for these parameters.
	 *
	 */
	private static class DeformationFieldExportParameters
	{
		public final String landmarkPath;
		public final boolean ignoreAffine;
		public final int nThreads;

		public final long[] size;
		public final double[] spacing;

		public final String n5Base;
		public final String n5Dataset;
		public final Compression compression;
		public final int[] blockSize;

		public DeformationFieldExportParameters(
				final String landmarkPath,
				final boolean ignoreAffine,
				final int nThreads,
				final long[] size,
				final double[] spacing,
				final String n5Base,
				final String n5Dataset,
				final int[] blockSize,
				final Compression compression )
		{
			this.landmarkPath = landmarkPath;
			this.ignoreAffine = ignoreAffine;
			this.nThreads = nThreads;

			this.size = size;
			this.spacing = spacing;

			this.n5Base = n5Base;
			this.n5Dataset = n5Dataset;
			this.blockSize  = blockSize;
			this.compression = compression;
		}

		public static DeformationFieldExportParameters fromDialog(
				final boolean promptLandmarks,
				final boolean promptReference )
		{
			final GenericDialogPlus gd = new GenericDialogPlus( "BigWarp to Deformation" );
			gd.addMessage( "Deformation field export:" );
			if( promptLandmarks )
			{
				gd.addFileField( "landmarks_image_file", "" );
			}

			gd.addCheckbox( "Ignore affine part", false );
			gd.addNumericField( "threads", 1, 0 );
			gd.addMessage( "Size and spacing" );

			final int[] ids = WindowManager.getIDList();
			if( promptReference )
			{
				final String[] titles = new String[ ids.length + 1 ];
				for ( int i = 0; i < ids.length; ++i )
				{
					titles[ i ] = ( WindowManager.getImage( ids[ i ] ) ).getTitle();
				}
				titles[ ids.length ] = "None";

				final String current = WindowManager.getCurrentImage().getTitle();
				gd.addChoice( "reference_image", titles, current );
			}
			gd.addStringField( "output size", "");
			gd.addStringField( "output spacing", "");

			gd.addMessage( "Leave n5 path empty to export as ImagePlus" );
			gd.addDirectoryOrFileField( "n5 root path", "" );
			gd.addStringField( "n5 dataset", "");
			gd.addStringField( "n5 block size", "32,32,32");
			gd.addChoice( "n5 compression", compressionOptions, N5Exporter.GZIP_COMPRESSION );
			gd.showDialog();

			if ( gd.wasCanceled() )
				return null;

			String landmarkPath = null;
			if( promptLandmarks )
				landmarkPath = gd.getNextString();

			final boolean ignoreAffine = gd.getNextBoolean();
			final int nThreads = ( int ) gd.getNextNumber();

			ImagePlus ref_imp = null;
			if( promptReference )
			{
				final int idx = ids[ gd.getNextChoiceIndex() ] ;
				if( idx < ids.length )
					ref_imp = WindowManager.getImage( idx );
			}

			final String sizeString = gd.getNextString();
			final String spacingString = gd.getNextString();

			final String n5Base = gd.getNextString();
			final String n5Dataset = gd.getNextString();
			final String n5BlockSizeString = gd.getNextString();
			final String n5CompressionString = gd.getNextChoice();

			final Compression compression = getCompression( n5CompressionString );
			final int[] blockSize = n5BlockSizeString.isEmpty() ? null :
				Arrays.stream( n5BlockSizeString.split( "," ) ).mapToInt( Integer::parseInt ).toArray();

			final long[] size;
			final double[] spacing;
			if( ref_imp == null )
			{
				if( !sizeString.isEmpty())
					size = Arrays.stream( sizeString.split( "," ) ).mapToLong( Long::parseLong ).toArray();
				else
					size = null;

				if( !spacingString.isEmpty() )
					spacing = Arrays.stream( spacingString.split( "," ) ).mapToDouble( Double::parseDouble ).toArray();
				else
					spacing = null;
			}
			else
			{
				int nd = 2;
				if ( ref_imp.getNSlices() > 1 )
					nd = 3;

				// account for physical units of reference image
				spacing = new double[ nd ];
				spacing[ 0 ] = ref_imp.getCalibration().pixelWidth;
				spacing[ 1 ] = ref_imp.getCalibration().pixelHeight;

				if ( nd > 2 )
					spacing[ 2 ] = ref_imp.getCalibration().pixelDepth;

				size = BigWarpToDeformationFieldPlugIn.dimensionsFromImagePlus( ref_imp );
			}

			return new DeformationFieldExportParameters(
					landmarkPath,
					ignoreAffine,
					nThreads,
					size,
					spacing,
					n5Base,
					n5Dataset,
					blockSize,
					compression );
		}
	}

}

