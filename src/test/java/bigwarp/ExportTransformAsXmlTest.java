package bigwarp;

import bdv.gui.TransformTypeSelectDialog;
import bdv.ij.BigWarpBdvCommand;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
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
		command.movingImageXml = new File( Class.class.getResource( "../../mri-stack-translated.xml" ).getFile() );
		command.run() ;

		final BigWarp bw = command.bw;

		bw.loadLandmarks( Class.class.getResource( "../../mri-stack-translation-landmarks.csv" ).getFile() );

		bw.setTransformType( TransformTypeSelectDialog.AFFINE );

		new BigWarp.SolveThread( bw ).start(); IJ.wait( 100 );

		final File warpedXml = bw.saveMovingImageXml( Class.class.getResource( "../../mri-stack-translated-bigWarp.xml" ).getFile() );

		showFixedAndWarpedMoving( warpedXml, command.fixedImageXml );
	}

	private static void showFixedAndWarpedMoving( File warpedXml, File fixedImageXml ) throws SpimDataException
	{
		final SpimData warpedSpimData = new XmlIoSpimData().load( warpedXml.getAbsolutePath() );
		final BdvStackSource< ? > bdvStackSource = BdvFunctions.show( warpedSpimData ).get( 0 );
		bdvStackSource.setDisplayRange( 0, 255 );

		final SpimData fixedSpimData = new XmlIoSpimData().load( fixedImageXml.getAbsolutePath() );
		final BdvStackSource< ? > bdvStackSource2 = BdvFunctions.show( fixedSpimData, BdvOptions.options().addTo( bdvStackSource.getBdvHandle() ) ).get( 0 );
		bdvStackSource2.setDisplayRange( 0, 255 );
	}
}