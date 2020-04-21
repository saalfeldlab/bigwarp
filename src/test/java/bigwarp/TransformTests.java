package bigwarp;


import bdv.gui.BigWarpViewerOptions;
import bdv.gui.TransformTypeSelectDialog;
import bigwarp.BigWarp.BigWarpData;
import bigwarp.landmarks.LandmarkTableModel;
import ij.ImagePlus;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.FinalInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.ui.TransformListener;
import net.imglib2.view.Views;

public class TransformTests
{
	
	public static void main( String[] args ) throws SpimDataException
	{
		test3d( true );
		test3d( false );

		test2d( true );
		test2d( false );
	}

	public static void test2d( boolean testTps ) throws SpimDataException
	{
		FunctionRandomAccessible<UnsignedByteType> fimg = new FunctionRandomAccessible<>(
				2, 
				(l,v) -> { v.setOne(); },
				UnsignedByteType::new );

		ImagePlus imp = ImageJFunctions.wrap( Views.interval( fimg, new FinalInterval( 32, 32 ) ), "img" );

		//BigWarpViewerOptions opts = BigWarpViewerOptions.options( false, true );
		BigWarpViewerOptions opts = BigWarpViewerOptions.options( true );

		BigWarpData<?> data = BigWarpInit.createBigWarpDataFromImages( imp, imp );

		@SuppressWarnings({ "unchecked", "rawtypes" })
		BigWarp<?> bw = new BigWarp( data, "bigwarp", opts, null );

		LandmarkTableModel ltm = bw.getLandmarkPanel().getTableModel();

		ltm.add( new double[]{ 1, 1 }, true );
		ltm.setPoint( 0, false, new double[]{ 2, 3 }, null );

		ltm.add( new double[]{ 2, 1 }, true );
		ltm.setPoint( 1, false, new double[]{ 4, 3 }, null );

		ltm.add( new double[]{ 1, 2 }, true );
		ltm.setPoint( 2, false, new double[]{ 2, 6 }, null );

		ltm.add( new double[]{ 4, 3 }, true );
		ltm.setPoint( 3, false, new double[]{ 8, 9 }, null );

		TestTransformListener l = new TestTransformListener( bw );
		bw.addTransformListener( l );
	
		if( testTps )
			bw.restimateTransformation();
		else
			bw.setTransformType( TransformTypeSelectDialog.AFFINE.toString() );
		
		bw.closeAll();
	}

	public static void test3d( boolean testTps ) throws SpimDataException
	{
		FunctionRandomAccessible<UnsignedByteType> fimg = new FunctionRandomAccessible<>(
				3, 
				(l,v) -> { v.setOne(); },
				UnsignedByteType::new );

		ImagePlus imp = ImageJFunctions.wrap( Views.interval( fimg, new FinalInterval( 32, 32, 32 ) ), "img" );

		//BigWarpViewerOptions opts = BigWarpViewerOptions.options( false, true );
		BigWarpViewerOptions opts = BigWarpViewerOptions.options();

		BigWarpData<?> data = BigWarpInit.createBigWarpDataFromImages( imp, imp );

		@SuppressWarnings({ "unchecked", "rawtypes" })
		BigWarp<?> bw = new BigWarp( data, "bigwarp", opts, null );

		LandmarkTableModel ltm = bw.getLandmarkPanel().getTableModel();

		ltm.add( new double[]{ 1, 1, 1 }, true );
		ltm.setPoint( 0, false, new double[]{ 2, 2, 3 }, null );

		ltm.add( new double[]{ 1, 2, 1 }, true );
		ltm.setPoint( 1, false, new double[]{ 2, 4, 3 }, null );

		ltm.add( new double[]{ 2, 1, 2 }, true );
		ltm.setPoint( 2, false, new double[]{ 4, 2, 6 }, null );

		ltm.add( new double[]{ 4, 2, 3 }, true );
		ltm.setPoint( 3, false, new double[]{ 8, 4, 9 }, null );

		TestTransformListener l = new TestTransformListener( bw );
		bw.addTransformListener( l );
	
		if( testTps )
			bw.restimateTransformation();
		else
			bw.setTransformType( TransformTypeSelectDialog.AFFINE.toString() );

		bw.closeAll();
	}

	public static class TestTransformListener implements TransformListener<InvertibleRealTransform>
	{
		BigWarp<?> bw;

		public TestTransformListener( BigWarp<?> bw )
		{
			this.bw = bw;
		}

		@Override
		public void transformChanged( final InvertibleRealTransform transform )
		{
			System.out.println( "transform changed" );
			System.out.println( transform );
			System.out.println( bw.affine3d() );
			System.out.println( bw.affine() );

		}
	}

}