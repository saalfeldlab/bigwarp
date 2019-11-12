package bigwarp;

import bdv.ij.BigWarpBdvCommand;

import java.io.File;

public class BigWarpBdvCommandTest
{
	public static void main( String[] args )
	{
		final BigWarpBdvCommand command = new BigWarpBdvCommand();
		command.fixedImageXml = new File( Class.class.getResource( "../../mri-stack.xml" ).getFile() );
		command.movingImageXml = new File( Class.class.getResource( "../../mri-stack-translated.xml" ).getFile() );
		command.run() ;
	}
}