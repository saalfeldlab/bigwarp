package bigwarp;

import bdv.gui.TransformTypeSelectDialog;
import bdv.ij.BigWarpBdvCommand;
import bdv.util.BdvFunctions;
import ij.IJ;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;

import java.io.File;

public class ExportTransformAsXmlTest
{
	public static void main( String[] args ) throws SpimDataException
	{
		final BigWarpBdvCommand command = new BigWarpBdvCommand();
		command.fixedImageXml = new File( Class.class.getResource( "../../mri-stack.xml" ).getFile() );
		command.movingImageXml = new File( Class.class.getResource( "../../mri-stack.xml" ).getFile() );
		command.run() ;

		final BigWarp bw = command.bw;

		bw.loadLandmarks( Class.class.getResource( "../../mri-stack-landmarks.csv" ).getFile() );

		bw.setTransformType( TransformTypeSelectDialog.AFFINE );

		new BigWarp.SolveThread( bw ).start(); IJ.wait( 100 );

		final File warpedXml = bw.saveMovingImageXml();

		final SpimData warpedSpimData = new XmlIoSpimData().load( warpedXml.getAbsolutePath() );

		BdvFunctions.show( warpedSpimData );
	}
}