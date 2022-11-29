package bdv.ij;

import java.io.IOException;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import bdv.gui.BigWarpInitDialog;
import ij.Macro;
import ij.plugin.PlugIn;
import net.imagej.DatasetService;

@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>Big Warp Command" )
public class BigWarpCommand implements Command, PlugIn
{

	@Parameter
	private DatasetService datasets;
	
	@Override
	public void run( String args )
	{
		System.out.println( "BigWarpCommand" );

		final String macroOptions = Macro.getOptions();
		String options = args;
		if ( options == null || options.isEmpty() )
			options = macroOptions;

		System.out.println( "macro options : " + macroOptions );
		final boolean isMacro = (options != null && !options.isEmpty());

		if ( isMacro )
			BigWarpInitDialog.runMacro( macroOptions );
		else
			BigWarpInitDialog.createAndShow( datasets );
	}

	@Override
	public void run()
	{
		run(null);
	}

	public static void main( String[] a ) throws IOException
	{
//		ImageJ ij2 = new ImageJ();
//		ij2.ui().showUI();
//
////		Object im1 = ij2.io().open( "/home/john/tmp/mri-stack.tif" );
////		Object im2 = ij2.io().open( "/home/john/tmp/t1-head.tif" );
//
//		Object im1 = ij2.io().open( "/groups/saalfeld/home/bogovicj/tmp/mri-stack.tif" );
//		Object im2 = ij2.io().open( "/groups/saalfeld/home/bogovicj/tmp/t1-head.tif" );
//
//		ij2.ui().show( im1 );
//		ij2.ui().show( im2 );


//		String args = "images=[a, b, c], isMoving=[true, true, false], transforms=[,,]";
//
//		String imagesList = null;
//		String isMovingList = null;
//		String transformsList = null;
	}

}
