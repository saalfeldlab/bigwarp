package bigwarp;

import java.io.File;
import java.io.IOException;

import bdv.export.ProgressWriterConsole;
import bdv.gui.BigWarpViewerOptions;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import bigwarp.BigWarp.BigWarpData;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
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

		TransformedSource< UnsignedByteType > tsrc = new TransformedSource<>( src );
		AffineTransform3D affine = new AffineTransform3D();
		affine.set( 
				0.2, 0.0, 0.0, 0.0,
				0.0, 0.2, 0.0, 0.0,
				0.0, 0.0, 0.4, 0.0 );
		tsrc.setIncrementalTransform( affine );


		BigWarpData< UnsignedByteType > datasrc = BigWarpInit.createBigWarpData( new Source[] { tsrc }, new Source[] { tsrc }, new String[] { "mvg", "tgt" } );
		BigWarp< UnsignedByteType > bw = new BigWarp<>( datasrc, "bw", BigWarpViewerOptions.options(), new ProgressWriterConsole() );
		bw.getLandmarkPanel().getTableModel().load( new File( "src/test/resources/mr_landmarks_p2p2p4-111.csv" ));
	}
}
