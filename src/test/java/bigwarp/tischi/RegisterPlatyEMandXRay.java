package bigwarp.tischi;

import bdv.ij.BigWarpBdvCommand;

import java.io.File;

public class RegisterPlatyEMandXRay
{
	public static void main( String[] args )
	{
		final BigWarpBdvCommand command = new BigWarpBdvCommand();
		command.fixedImageXml = new File( "/Volumes/cba/exchange/maxim/ver2/2sources/Platy-88_01_tomo-transformed.xml" );
		command.movingImageXml = new File( "/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data/rawdata/xray-6dpf-1-whole-raw.xml" );
		command.run() ;
	}
}