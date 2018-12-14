package bdv.ij;

import java.io.File;
import java.io.IOException;

import bigwarp.BigWarpExporter;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.source.JacobianDeterminantRandomAccess;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.FinalInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.FloatImagePlus;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformDimension;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.RandomAccessibleOnRealRandomAccessible;
import net.imglib2.view.Views;

/**
 * ImageJ plugin to convert the thin plate spline to a jacobian (determinant) field
 *
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public class BigWarpToJacobianFieldPlugIn implements PlugIn
{

    private ImagePlus ref_imp;
    private ThinplateSplineTransform tps;
    private AffineTransform pixToPhysical;
    private int nThreads;

	public static void main( final String[] args )
	{
		new ImageJ();
		//ImagePlus imp = IJ.openImage("/home/john/tmp/mri-stack.tif");
		ImagePlus imp = IJ.openImage("/groups/saalfeld/home/bogovicj/tmp/mri-stack.tif");

		//IJ.run("Boats (356K)");
//		ImagePlus imp = IJ.openImage( "/groups/saalfeld/home/bogovicj/tmp/mri-stack.tif" );
//		ImagePlus imp = IJ.openImage( "/groups/saalfeld/home/bogovicj/tmp/mri-stack_p2p2p4.tif" );

		imp.show();

		WindowManager.getActiveWindow();
		new BigWarpToJacobianFieldPlugIn().run( null );
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
		final GenericDialog gd = new GenericDialog( "BigWarp to Jacobian field" );
		gd.addMessage( "Landmarks and Image Selection:" );
		gd.addStringField( "landmarks_image_file", "" );

		final String current = WindowManager.getCurrentImage().getTitle();
		gd.addChoice( "reference_image", titles, current );
		gd.addCheckbox( "Display inverse jacobian?", false );
		gd.addCheckbox( "Is virtual?", false );
		gd.addNumericField( "threads", 1, 0 );
		gd.showDialog();

		if ( gd.wasCanceled() )
			return;

		String landmarksPath = gd.getNextString();
		ref_imp = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
		boolean isInverse = gd.getNextBoolean();
		boolean isVirtual = gd.getNextBoolean();
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

		tps = new ThinplateSplineTransform( tpsUseMe );

		long[] dims;
		if( ref_imp.getNSlices() < 2 )
		{
			dims = new long[ 2 ];
			dims[ 0 ] = ref_imp.getWidth();
			dims[ 1 ] = ref_imp.getHeight();
		} 
		else
		{
			dims = new long[ 3 ];
			dims[ 0 ] = ref_imp.getWidth();
			dims[ 1 ] = ref_imp.getHeight();
			dims[ 2 ] = ref_imp.getNSlices();
		}

		//FloatImagePlus< FloatType > deformationField = ImagePlusImgs.floats( dims );
		FinalInterval spatialinterval = new FinalInterval( dims );

//		SourceAndConverter<FloatType> jsource = BigWarp.createJacobianSource( tps, spatialinterval, "jacobian field");
//		RandomAccessibleInterval<FloatType> jimg = jsource.getSpimSource().getSource( 0, 0 );
//		ImagePlus jip = ImageJFunctions.wrap( Views.permute( jimg, 2, 3 ), "jacobian field" );

//		RandomAccessible< FloatType > jdimg = Views.raster( JacobianDeterminantRandomAccess.createJacobianDeterminant( new FloatType(), tps ));
//		//IntervalView< FloatType > jrai = Views.interval( jdimg, spatialinterval );
		
		RealRandomAccessible< FloatType > jdimg = JacobianDeterminantRandomAccess.createJacobianDeterminant( new FloatType(), tps );
		if( !isInverse )
			jdimg = jacobianDetToMovingSpace( jdimg, tps );

		ImagePlus jip = null;
		if( isVirtual )
			jip = ImageJFunctions.wrap( Views.interval( Views.raster( jdimg ), spatialinterval ), "jacobian field" );
		else
		{
			FloatImagePlus< FloatType > ipi = ImagePlusImgs.floats( dims );
			RandomAccessibleOnRealRandomAccessible< FloatType > jra = Views.raster( jdimg );

			BigWarpExporter.copyToImageStack( jra, ipi, nThreads );
			jip = ipi.getImagePlus();
		}

		jip.setTitle( "bigwarp jacobian" );
		jip.getCalibration().pixelWidth = ref_imp.getCalibration().pixelWidth;
		jip.getCalibration().pixelHeight = ref_imp.getCalibration().pixelHeight;
		jip.getCalibration().pixelDepth = ref_imp.getCalibration().pixelDepth;
	}
	
	/**
	 * 
	 * @param jacobian the jacobian tensor field
	 * @param invXfm the transform from target space to moving space 
	 * @return the transformed jacobian field
	 */
	public static <T extends RealType<T>> RealRandomAccessible< T > jacobianDetToMovingSpace(
			RealRandomAccessible<T> jacobian, final RealTransform invXfm )
	{
		System.out.println( "to moving space" );
		RealTransformRealRandomAccessible< T, RealTransform > jacXfm = 
				new RealTransformRealRandomAccessible<>( jacobian, invXfm );
		
		T t = jacobian.realRandomAccess().get().copy();
		RealRandomAccessible< T > out = Converters.convert( jacXfm, 
				new Converter<T,T>()
				{
					@Override
					public void convert( T input, T output )
					{
						output.setReal( 1 / input.getRealDouble() );
					}
				}, t );
		
		return out;
	}

	/**
	 * 
	 * @param jacobian the jacobian tensor field
	 * @param invXfm the transform from target space to moving space 
	 * @return the transformed jacobian field
	 */
	public static <T extends RealType<T>> RealRandomAccessible< T > jacobianToMovingSpace(
			RealRandomAccessible<T> jacobian, final RealTransform invXfm )
	{
		System.out.println( "to moving space" );
		RealTransformDimension invXfmUp = new RealTransformDimension( invXfm );
		RealTransformRealRandomAccessible< T, RealTransform > jacXfm = 
				new RealTransformRealRandomAccessible<>( jacobian, invXfmUp );
		
		T t = jacobian.realRandomAccess().get().copy();
		RealRandomAccessible< T > out = Converters.convert( jacXfm, 
				new Converter<T,T>()
				{
					@Override
					public void convert( T input, T output )
					{
						output.setReal( 1 / input.getRealDouble() );
					}
				}, t );
		
		return out;
	}
	
}

