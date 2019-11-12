package bigwarp;

import bdv.gui.TransformTypeSelectDialog;
import bdv.ij.BigWarpBdvCommand;
import ij.IJ;
import net.imglib2.realtransform.InvertibleRealTransform;

import java.io.File;

public class ExportTransformAsXmlTest
{
	public static void main( String[] args )
	{
		final BigWarpBdvCommand command = new BigWarpBdvCommand();
		command.fixedImageFile = new File( Class.class.getResource( "../../mri-stack.xml" ).getFile() );
		command.movingImageFile = new File( Class.class.getResource( "../../mri-stack.xml" ).getFile() );
		command.run() ;

		final BigWarp bw = command.bw;

		bw.loadLandmarks( Class.class.getResource( "../../mri-stack-landmarks.csv" ).getFile() );

		bw.setTransformType( TransformTypeSelectDialog.ROTATION );

		new BigWarp.SolveThread( bw ).start(); IJ.wait( 100 );

		bw.saveMovingImageXml();

	}
}