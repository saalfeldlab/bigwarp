package bigwarp;

import bdv.ij.BigWarpBdvCommand;

import java.io.File;

public class BdvUITest
{
	public static void main( String[] args )
	{
		final BigWarpBdvCommand command = new BigWarpBdvCommand();
		command.fixedImageFile = new File( Class.class.getResource( "../../mri-stack.xml" ).getFile() );
		command.movingImageFile = new File( Class.class.getResource( "../../mri-stack.xml" ).getFile() );
		command.run() ;
	}
}