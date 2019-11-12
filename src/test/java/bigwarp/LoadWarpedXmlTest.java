package bigwarp;

import bdv.gui.TransformTypeSelectDialog;
import bdv.ij.BigWarpBdvCommand;
import bdv.util.BdvFunctions;
import ij.IJ;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;

import java.io.File;

public class LoadWarpedXmlTest
{
	public static void main( String[] args ) throws SpimDataException
	{
		final SpimData warpedSpimData = new XmlIoSpimData().load( Class.class.getResource( "../../mri-stack.bigWarp.xml" ).getFile()  );
		BdvFunctions.show( warpedSpimData );
	}
}