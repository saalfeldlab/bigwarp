package bigwarp;

import java.io.IOException;

import bdv.export.ProgressWriterConsole;
import bdv.gui.BigWarpViewerOptions;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import bigwarp.BigWarp.BigWarpData;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class SourceTest
{

	public static void main( String[] args ) throws IOException, SpimDataException
	{
		new ImageJ();

		ImagePlus imp = IJ.openImage( "http://imagej.nih.gov/ij/images/mri-stack.zip" );

		BdvStackSource< UnsignedByteType > bdv = BdvFunctions
				.show( ImageJFunctions.wrapByte( imp ), "mri-stack" );
		Source< UnsignedByteType > src = bdv.getSources().get( 0 ).getSpimSource();

		BigWarpData< UnsignedByteType > datasrc = BigWarpInit.createBigWarpData( new Source[] { src }, new Source[] { src }, new String[] { "mvg", "tgt" } );
		BigWarp< UnsignedByteType > bw = new BigWarp<>( datasrc, "bw", BigWarpViewerOptions.options(), new ProgressWriterConsole() );

	}
}
