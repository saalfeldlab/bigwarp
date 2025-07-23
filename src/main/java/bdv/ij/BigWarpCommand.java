/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2025 Howard Hughes Medical Institute.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package bdv.ij;

import java.io.IOException;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import bdv.gui.BigWarpInitDialog;
import ij.Macro;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;

@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>Big Warp Command" )
public class BigWarpCommand implements Command, PlugIn
{
	private boolean initialRecorderState;

	@Parameter
	private DatasetService datasetService;

	public BigWarpCommand()
	{
		initialRecorderState = Recorder.record;
		Recorder.record = false;
	}

	@Override
	public void run( String args )
	{
		Recorder.record = initialRecorderState;
		final String macroOptions = Macro.getOptions();
		String options = args;
		if ( options == null || options.isEmpty() )
			options = macroOptions;

		final boolean isMacro = (options != null && !options.isEmpty());
		if ( isMacro )
			BigWarpInitDialog.runMacro( macroOptions );
		else
		{
			final BigWarpInitDialog dialog = BigWarpInitDialog.createAndShow( datasetService );
			// dialog sets recorder to its initial state on cancel or execution
			dialog.setInitialRecorderState( initialRecorderState );
		}
	}

	@Override
	public void run()
	{
		run(null);
	}

	public static void main( String[] a ) throws IOException
	{
//		String options = "images=mri-stack.tif,mri-stack.tif moving=true,false transforms=,";
//
//		String images = Macro.getValue(options, "images", "");
//		String moving = Macro.getValue(options, "moving", "");
//		String transforms = Macro.getValue(options, "transforms", "");
//
//		System.out.println( images );
//		System.out.println( moving );
//		System.out.println( transforms );

		final ImageJ ij2 = new ImageJ();
		ij2.ui().showUI();

//		final Object im1 = ij2.io().open( "/home/john/tmp/mri-stack.tif" );
//		final Object im2 = ij2.io().open( "/home/john/tmp/t1-head.tif" );
////
////		Object im1 = ij2.io().open( "/groups/saalfeld/home/bogovicj/tmp/mri-stack.tif" );
////		Object im2 = ij2.io().open( "/groups/saalfeld/home/bogovicj/tmp/t1-head.tif" );
////
//		ij2.ui().show( im1 );
//		ij2.ui().show( im2 );


		final Object im1 = ij2.io().open( "/home/john/tmp/boats.tif" );
		ij2.ui().show( im1 );


//		String args = "images=[a, b, c], isMoving=[true, true, false], transforms=[,,]";
//
//		String imagesList = null;
//		String isMovingList = null;
//		String transformsList = null;
	}

}
