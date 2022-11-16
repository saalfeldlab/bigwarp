package bigwarp;

import bdv.export.ProgressWriterConsole;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.realtransform.AffineTransform3D;

public class BigWarpTransformedSourcesTest
{

	public static void main( String[] args ) throws SpimDataException
	{
		BigWarpData< ? > data = createData();
		data.applyTransformations();

		final BigWarp<?> bw = new BigWarp<>( data, new ProgressWriterConsole());
		bw.loadLandmarks( "/home/john/tmp/bw_tformTest_landmarks_simple.csv" );
//		bw.loadLandmarks( "/groups/saalfeld/home/bogovicj/tmp/bw_tformTest_landmarks.csv" );

	}

	public static <T> BigWarpData<T> createData()
	{
		final ImagePlus mr = IJ.openImage("/home/john/tmp/mri-stack.tif");
		final ImagePlus t1 = IJ.openImage("/home/john/tmp/t1-head.tif");

//		final ImagePlus mr = IJ.openImage("/groups/saalfeld/home/bogovicj/tmp/mri-stack.tif");
//		final ImagePlus t1 = IJ.openImage("/groups/saalfeld/home/bogovicj/tmp/t1-head.tif");

		AffineTransform3D translation0 = new AffineTransform3D();
		translation0.translate( -50, -50, -10 );

		AffineTransform3D translation = new AffineTransform3D();
		translation.translate( -100, -150, -50 );

		int id = 0;
		final BigWarpData< T > data = BigWarpInit.initData();
		BigWarpInit.add( data, BigWarpInit.createSources( data, mr, id++, 0, true ), translation0 );
		BigWarpInit.add( data, BigWarpInit.createSources( data, t1, id++, 0, false ));
		BigWarpInit.add( data, BigWarpInit.createSources( data, t1, id++, 0, false ), translation);

		return data;
	}

}
