package bdv.ij;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import bigwarp.BigWarpExporter;
import bigwarp.landmarks.LandmarkTableModel;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.img.imageplus.FloatImagePlus;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.DeformationFieldTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;
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
public class BigWarpToDeformationFieldPlugIn implements PlugIn
{

    private ImagePlus ref_imp;
    private ThinplateSplineTransform tps;
    private AffineTransform pixToPhysical;
    private int nThreads;

	public static void main( final String[] args )
	{
		new ImageJ();
		IJ.run("Boats (356K)");
//		ImagePlus imp = IJ.openImage( "/groups/saalfeld/home/bogovicj/tmp/mri-stack.tif" );
//		ImagePlus imp = IJ.openImage( "/groups/saalfeld/home/bogovicj/tmp/mri-stack_p2p2p4.tif" );
//		imp.show();
		
		WindowManager.getActiveWindow();
		new BigWarpToDeformationFieldPlugIn().run( null );
	}

	@Override
	public void run( final String arg )
	{
		if ( IJ.versionLessThan( "1.40" ) )
			return;

		final int[] ids = WindowManager.getIDList();
		if ( ids == null || ids.length < 1 )
		{
			IJ.showMessage( "You should have at least one image open." );
			return;
		}

		// Find any open images
		final String[] titles = new String[ ids.length ];
		for ( int i = 0; i < ids.length; ++i )
		{
			titles[ i ] = ( WindowManager.getImage( ids[ i ] ) ).getTitle();
		}

		// Build a dialog to choose how to export the deformation field
		final GenericDialog gd = new GenericDialog( "BigWarp to Deformation" );
		gd.addMessage( "Landmarks and Image Selection:" );
		gd.addStringField( "landmarks_image_file", "" );
		final String current = WindowManager.getCurrentImage().getTitle();
		gd.addChoice( "reference_image", titles, current );
		gd.addCheckbox( "Ignore affine part", false );
		gd.addNumericField( "threads", 1, 0 );
		gd.showDialog();

		if ( gd.wasCanceled() )
			return;

		String landmarksPath = gd.getNextString();
		ref_imp = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
		boolean ignoreAffine = gd.getNextBoolean();
		nThreads = ( int ) gd.getNextNumber();

		int nd = 2;
		if ( ref_imp.getNSlices() > 1 )
			nd = 3;

		// account for physical units of reference image
		pixToPhysical = new AffineTransform( nd );
		pixToPhysical.set( ref_imp.getCalibration().pixelWidth, 0, 0 );
		pixToPhysical.set( ref_imp.getCalibration().pixelHeight, 1, 1 );
		if ( nd > 2 )
			pixToPhysical.set( ref_imp.getCalibration().pixelDepth, 2, 2 );

		LandmarkTableModel ltm = new LandmarkTableModel( nd );
		try
		{
			ltm.load( new File( landmarksPath ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return;
		}

		ThinPlateR2LogRSplineKernelTransform tpsRaw = ltm.getTransform();
		ThinPlateR2LogRSplineKernelTransform tpsUseMe = tpsRaw;
		if ( ignoreAffine )
			tpsUseMe = new ThinPlateR2LogRSplineKernelTransform( 
					tpsRaw.getSourceLandmarks(), null, null, tpsRaw.getKnotWeights() );

		tps = new ThinplateSplineTransform( tpsUseMe );

		FloatImagePlus< FloatType > dfield = convertToDeformationField();

		String title = "bigwarp dfield";
		if ( ignoreAffine )
			title += " (no affine)";

		ImagePlus dfieldIp = dfield.getImagePlus();
		dfieldIp.setTitle( title );

		dfieldIp.getCalibration().pixelWidth = ref_imp.getCalibration().pixelWidth;
		dfieldIp.getCalibration().pixelHeight = ref_imp.getCalibration().pixelHeight;
		dfieldIp.getCalibration().pixelDepth = ref_imp.getCalibration().pixelDepth;
		dfieldIp.show();

	}

	public FloatImagePlus< FloatType > convertToDeformationField()
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

		FloatImagePlus< FloatType > deformationField = ImagePlusImgs.floats( dims );
		if( nThreads <= 1 )
			fromRealTransform( tps, pixToPhysical, Views.permute( deformationField, 2, 3 ));
		else
			fromRealTransform( tps, pixToPhysical, Views.permute( deformationField, 2, 3 ), 8 );
		

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
	public static < T extends RealType< T > > void fromRealTransform( final RealTransform transform, 
			final AffineTransform pixelToPhysical,
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
			final AffineTransform pixelToPhysical,
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

//						System.out.println( "subTgt size: " + Util.printInterval( subTgt ));
						
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
			threadPool.shutdown(); // wait for all jobs to finish
		}
		catch ( InterruptedException e1 )
		{
			e1.printStackTrace();
		}
	}

}

