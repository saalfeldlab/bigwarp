package bigwarp;

import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bigwarp.BigWarp.BigWarpData;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/*
 * See
 * https://github.com/bigdataviewer/bigdataviewer-playground/issues/249
 */
public class Sources2Dtests {

	public static void main(String[] args) throws SpimDataException {
		run();
	}

	public static <T extends NativeType<T> & RealType<T>> void run() throws SpimDataException
	{
		Source<T> mvg = loadSource("https://imagej.nih.gov/ij/images/boats.gif",  20);
		Source<T> tgt = loadSource("https://imagej.nih.gov/ij/images/boats.gif", -20);

		BigWarpData<T> bwdata = BigWarpInit.initData();
		BigWarpInit.add(bwdata, mvg, 0, 0, true);
		BigWarpInit.add(bwdata, tgt, 1, 0, false);
		bwdata.wrapUp();

		BigWarp bw = new BigWarp(bwdata, "bw test", null);
	}

	public static <T extends NativeType<T> & RealType<T>> Source<T> loadSource( String path, double zOffset )
	{
		ImagePlus imp = IJ.openImage(path);
		RandomAccessibleInterval<T> img = ImageJFunctions.wrap(imp);
		if( img.numDimensions() == 2 )
			img = Views.addDimension(img, 0, 0);

		AffineTransform3D xfm = new AffineTransform3D();
		xfm.translate(0, 0, zOffset);

		return new RandomAccessibleIntervalSource<>(img, Util.getTypeFromInterval(img), xfm, imp.getTitle());
	}

}
