package bdv.ij;

import java.io.IOException;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import bdv.gui.BigWarpInitDialog;
import ij.IJ;
import net.imagej.ImageJ;

@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>Big Warp Command" )
public class BigWarpCommand implements Command
{
	@Override
	public void run()
	{
		BigWarpInitDialog.createAndShow();
	}
	
	public static void main( String[] args ) throws IOException
	{
		ImageJ ij2 = new ImageJ();
		ij2.ui().showUI();

		Object im1 = ij2.io().open( "/home/john/tmp/mri-stack.tif" );
		Object im2 = ij2.io().open( "/home/john/tmp/t1-head.tif" );

		ij2.ui().show( im1 );
		ij2.ui().show( im2 );
	}

}
