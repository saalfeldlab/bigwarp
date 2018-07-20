package bdv.ij;

import java.io.File;
import java.io.IOException;

import bigwarp.landmarks.LandmarkTableModel;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import net.imglib2.Cursor;
import net.imglib2.Interval;
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

	public static void main( final String[] args )
	{
		new ImageJ();
		IJ.run("Boats (356K)");
		
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

		// Build a dialog to choose the moving and fixed images
		final GenericDialog gd = new GenericDialog( "Big Warp Setup" );
		gd.addMessage( "Landmarks and Image Selection:" );
		gd.addStringField( "landmarks_image_file", "" );
		final String current = WindowManager.getCurrentImage().getTitle();
		gd.addChoice( "reference_image", titles, current );
		//gd.addNumericField( "threads", 1, 0 );
		gd.showDialog();

        if (gd.wasCanceled()) return;

        String landmarksPath = gd.getNextString();
        ref_imp = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );

        int nd = 2;
		if ( ref_imp.getNSlices() > 1 )
			nd = 3;
		
		
		// account for physical units of reference image
		pixToPhysical = new AffineTransform( nd );
		pixToPhysical.set( ref_imp.getCalibration().pixelWidth, 0, 0 );
		pixToPhysical.set( ref_imp.getCalibration().pixelHeight, 1, 1 );
		if( nd > 2 )
			pixToPhysical.set( ref_imp.getCalibration().pixelDepth, 2, 2 );

		LandmarkTableModel ltm = new LandmarkTableModel( nd );
		try
		{
			ltm.load( new File( landmarksPath ) );
		} catch ( IOException e )
		{
			e.printStackTrace();
			return;
		}
		tps = new ThinplateSplineTransform( ltm.getTransform() );

		FloatImagePlus< FloatType > dfield = convertToDeformationField();
		DeformationFieldTransform< FloatType > dfieldXfm = new DeformationFieldTransform<>( 
				Views.permute( dfield, 2, 3 ));
		
		RealTransformSequence totalXfmDfield = new RealTransformSequence();
		totalXfmDfield.add( pixToPhysical );
		totalXfmDfield.add( dfieldXfm );
		
		RealTransformSequence totalXfmTps = new RealTransformSequence();
		totalXfmTps.add( pixToPhysical );
		totalXfmTps.add( tps );

//		boolean theSame = areTransformsTheSame( totalXfmTps, totalXfmDfield, Views.collapse( dfield ), 0.1 );
//		System.out.println( "are they the same? " + theSame );
		
		ImagePlus dfieldIp = dfield.getImagePlus();
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
		fromRealTransform( tps, pixToPhysical, Views.permute( deformationField, 2, 3 ) );

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

}
