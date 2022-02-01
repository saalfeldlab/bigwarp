/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2021 Howard Hughes Medical Institute.
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
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;
import org.janelia.saalfeldlab.n5.blosc.BloscCompression;
import org.janelia.saalfeldlab.n5.ij.N5Exporter;
import org.janelia.saalfeldlab.n5.ij.N5Factory;
import org.janelia.saalfeldlab.n5.imglib2.N5DisplacementField;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarpExporter;
import bigwarp.landmarks.LandmarkTableModel;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
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
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.Scale2D;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
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
@Plugin(type= Command.class,
	menuPath = "Plugins>BigDataViewer>Big Warp to Displacement field"
)
public class BigWarpToDeformationFieldPlugIn implements Command
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
		IJ.run("Boats (356K)");
//		ImagePlus imp = IJ.openImage( "/groups/saalfeld/home/bogovicj/tmp/mri-stack.tif" );
//		ImagePlus imp = IJ.openImage( "/groups/saalfeld/home/bogovicj/tmp/mri-stack_p2p2p4.tif" );
//		imp.show();
		
		WindowManager.getActiveWindow();
		new BigWarpToDeformationFieldPlugIn().run();
	}

	public <T> void runFromBigWarpInstance(
			final LandmarkTableModel landmarkModel,
			final List<SourceAndConverter<T>> sources,
			final int[] targetSourceIndexList )
	{
		ImageJ ij = IJ.getInstance();
		if ( ij == null )
			return;


		final DeformationFieldExportParameters params = DeformationFieldExportParameters.fromDialog( false, false );
		final RandomAccessibleInterval< ? > tgtInterval = sources.get( targetSourceIndexList[ 0 ] ).getSpimSource().getSource( 0, 0 );

		int ndims = landmarkModel.getNumdims();
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
		VoxelDimensions voxelDim = sources.get( targetSourceIndexList[ 0 ] ).getSpimSource().getVoxelDimensions();
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
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run()
	{
		if ( IJ.versionLessThan( "1.40" ) )
			return;

		DeformationFieldExportParameters params = DeformationFieldExportParameters.fromDialog( true, true );
		int nd = params.size.length;

		// load
		LandmarkTableModel ltm = new LandmarkTableModel( nd );
		try
		{
			ltm.load( new File( params.landmarkPath ) );
		}
		catch ( IOException e )
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
			catch ( IOException e )
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
		ThinPlateR2LogRSplineKernelTransform tpsRaw = ltm.getTransform();
		ThinPlateR2LogRSplineKernelTransform tpsUseMe = tpsRaw;
		if ( ignoreAffine )
			tpsUseMe = new ThinPlateR2LogRSplineKernelTransform( tpsRaw.getSourceLandmarks(), null, null, tpsRaw.getKnotWeights() );

		ThinplateSplineTransform tps = new ThinplateSplineTransform( tpsUseMe );

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

		FloatImagePlus< FloatType > dfield = convertToDeformationField( dims, tps, pixelToPhysical, nThreads );

		String title = "bigwarp dfield";
		if ( ignoreAffine )
			title += " (no affine)";

		ImagePlus dfieldIp = dfield.getImagePlus();
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
			final int nThreads ) throws IOException 
	{
		writeN5( n5BasePath, "dfield", ltm, dims, spacing, spatialBlockSize, compression, nThreads );
	}

	public static void writeN5( final String n5BasePath, final String n5Dataset,
			final LandmarkTableModel ltm,
			final long[] dims,
			final double[] spacing,
			final int[] spatialBlockSize,
			final Compression compression,
			final int nThreads ) throws IOException 
	{
		final ThinPlateR2LogRSplineKernelTransform tpsRaw = ltm.getTransform();
		final AffineGet affine = toAffine( tpsRaw );

		/*
		 * "remove the affine" from the total transform
		 * by concatenating the inverse of the affine to be removed
		 */
		final ThinplateSplineTransform tpsTotal = new ThinplateSplineTransform( tpsRaw );
		final RealTransformSequence seq = new RealTransformSequence();
		seq.add( tpsTotal );
		seq.add( affine.inverse() );

		AffineGet pixelToPhysical = null;
		if( spacing.length == 2 )
			pixelToPhysical = new Scale2D( spacing );
		else if( spacing.length == 3 )
			pixelToPhysical = new Scale3D( spacing );

		FloatImagePlus< FloatType > dfieldRaw = convertToDeformationField(
				dims, seq, pixelToPhysical, nThreads );

		// this works for both 2d and 3d, it turn out
		RandomAccessibleInterval< FloatType > dfield =
					Views.permute(
						Views.permute( dfieldRaw,
								0, 2 ),
						1, 2 );

		int[] blockSize = new int[ spatialBlockSize.length + 1 ];
		blockSize[ 0 ] = spatialBlockSize.length;
		for( int i = 0; i < spatialBlockSize.length; i++ )
		{
			blockSize[ i + 1 ] = spatialBlockSize[ i ];
		}

		final N5Writer n5 = new N5Factory().openWriter( n5BasePath );
		N5DisplacementField.save( n5, n5Dataset, affine, dfield, spacing, blockSize, compression );
		N5DisplacementField.saveAffine( affine, n5, n5Dataset );
	}

	public static AffineGet toAffine( final ThinPlateR2LogRSplineKernelTransform tps )
	{
		double[] affineFlat = toFlatAffine( tps );
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
		FloatImagePlus< FloatType > deformationField = ImagePlusImgs.floats( dims );

		RandomAccessibleInterval<FloatType> dfieldPermuted = deformationField;
		if( dims.length == 4 )
			dfieldPermuted = Views.permute( deformationField, 2, 3 );

		if( nThreads <= 1 )
			fromRealTransform( transform, pixToPhysical, dfieldPermuted );
		else
			fromRealTransform( transform, pixToPhysical, dfieldPermuted, nThreads );
		
		return deformationField;
	}
	
	public static boolean areTransformsTheSame( RealTransform xfm1, RealTransform xfm2, Interval itvl, final double EPS )
	{

		double[] pArray = new double[ 3 ];
		double[] qArray = new double[ 3 ];
		RealPoint p = RealPoint.wrap( pArray );
		RealPoint q = RealPoint.wrap( qArray );

		IntervalIterator c = new IntervalIterator( itvl );
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

		int N = transform.numSourceDimensions();
		RealPoint p = new RealPoint( transform.numTargetDimensions() );
		RealPoint q = new RealPoint( transform.numTargetDimensions() );

		CompositeIntervalView< T, ? extends GenericComposite< T > > col = Views.collapse( deformationField );
		Cursor< ? extends GenericComposite< T > > c = Views.flatIterable( col ).cursor();
		while ( c.hasNext() )
		{
			GenericComposite< T > displacementVector = c.next();

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
			int nThreads)
	{
		assert deformationField.numDimensions() == ( transform.numSourceDimensions() + 1 );
		assert deformationField.dimension( deformationField.numDimensions() - 1 ) >= transform.numSourceDimensions();

		System.out.println( "NTHREADS: " + nThreads );
		System.out.println( "dfield size: " + Util.printInterval( deformationField ));
		
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

		long del = ( long )( N / nThreads ); 
		splitPoints[ 0 ] = 0;
		splitPoints[ nThreads ] = deformationField.dimension( dim2split );
		for( int i = 1; i < nThreads; i++ )
		{
			splitPoints[ i ] = splitPoints[ i - 1 ] + del;
		}

		ExecutorService threadPool = Executors.newFixedThreadPool( nThreads );
		LinkedList<Callable<Boolean>> jobs = new LinkedList<Callable<Boolean>>();
		
		for( int i = 0; i < nThreads; i++ )
		{
			final long start = splitPoints[ i ];
			final long end   = splitPoints[ i+1 ];

			final RealTransform transformCopy = transform.copy();
			final RealTransform toPhysicalCopy = pixelToPhysical.copy();
			
			jobs.add( new Callable<Boolean>()
			{
				public Boolean call()
				{
					try
					{
						RealPoint p = new RealPoint( transform.numTargetDimensions() );
						RealPoint q = new RealPoint( transform.numTargetDimensions() );

						final FinalInterval subItvl = BigWarpExporter.getSubInterval( deformationField, dim2split, start, end );
						CompositeIntervalView< T, ? extends GenericComposite< T > > col = Views.collapse( deformationField );
						final IntervalView< ? extends GenericComposite< T > > subTgt = Views.interval( col, subItvl );

						Cursor< ? extends GenericComposite< T > > c = Views.flatIterable( subTgt ).cursor();
						while ( c.hasNext() )
						{
							GenericComposite< T > displacementVector = c.next();

							// transform the location of the cursor
							// and store the displacement
							toPhysicalCopy.apply( c, p );
							transformCopy.apply( p, q );

							for ( int i = 0; i < ndims; i++ )
								displacementVector.get( i ).setReal( q.getDoublePosition( i ) - p.getDoublePosition( i ) ); 
						}
						return true;
					}
					catch( Exception e )
					{
						e.printStackTrace();
					}
					return false;
				}
			});
		}
		try
		{
			List< Future< Boolean > > futures = threadPool.invokeAll( jobs );
			for( Future<Boolean> f : futures )
					f.get();

			threadPool.shutdown(); // wait for all jobs to finish
		}
		catch ( InterruptedException e1 )
		{
			e1.printStackTrace();
		}
		catch ( ExecutionException e )
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

