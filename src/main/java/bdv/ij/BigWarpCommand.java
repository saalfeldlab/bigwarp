package bdv.ij;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import bdv.gui.BigWarpInitDialog;
import net.imagej.ImageJ;

@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>Big Warp Command" )
public class BigWarpCommand implements Command
{
	@Override
	public void run()
	{
		BigWarpInitDialog.createAndShow();
	}
	
	public static void main( String[] args )
	{
		ImageJ ij2 = new ImageJ();
		ij2.ui().showUI();
	}

}
