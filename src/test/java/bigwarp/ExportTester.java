/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2022 Howard Hughes Medical Institute.
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
package bigwarp;

import java.io.File;
import java.io.IOException;
import java.util.List;

import bdv.gui.TransformTypeSelectDialog;
import bdv.ij.ApplyBigwarpPlugin;
import bdv.viewer.Interpolation;
import bigwarp.landmarks.LandmarkTableModel;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

/**
 * Tough to make tests of exporter from scratch.  
 * This is a helper to get there, or at least part of the way there
 * by visually confirming the sanity of exporter results for certain sets of parameters.
 * 
 * @author John Bogovic
 *
 */
public class ExportTester
{

	public static void main( String[] args ) throws IOException
	{
		new ImageJ();

//		ImagePlus impm = IJ.openImage("/groups/saalfeld/home/bogovicj/tmp/mri-stack_p2p2p4.tif");
//		ImagePlus impt = IJ.openImage("/groups/saalfeld/home/bogovicj/tmp/mri-stack.tif");
		
		ImagePlus impm = IJ.openImage("http://imagej.nih.gov/ij/images/mri-stack.zip");
		ImagePlus impt = impm.duplicate();
		IJ.run(impm, "Properties...", "channels=1 slices=27 frames=1 unit=pixel pixel_width=0.2 pixel_height=0.2 voxel_depth=0.4");

//		ImagePlus impm = IJ.openImage("/groups/saalfeld/home/bogovicj/tmp/mri-stack_p2p2p4.tif");
//		ImagePlus impt = impm.duplicate();

		
		LandmarkTableModel landmarks = new LandmarkTableModel( 3 );
		landmarks.load( new File( "src/test/resources/mr_landmarks_p2p2p4-111.csv" ));
		
		/*******************************
		 * fov_res
		 *******************************/
		v_tgt_tgt( impm, impt, landmarks ); 

//		tgt_tgt( impm, impt, landmarks );
//		tgt_spc( impm, impt, landmarks );
//		tgt_mvg( impm, impt, landmarks );
//		
//		mvg_tgt( impm, impt, landmarks );
//		mvg_mvg( impm, impt, landmarks );
//		mvg_spc( impm, impt, landmarks );
//		
//		lmk_tgt( impm, impt, landmarks );
//		lmk_mvg( impm, impt, landmarks );
//		lmk_spc( impm, impt, landmarks );
//		
//		v_spc_spc( impm, impt, landmarks );
//		spc_spc( impm, impt, landmarks );
//		pspc_pspc( impm, impt, landmarks );
//		pix_spc( impm, impt, landmarks );
//	
//		tgt_lmpix( impm, impt, landmarks );
//		tgt_lmphy( impm, impt, landmarks );
	}
	
	public static void pix_spc( ImagePlus impm, ImagePlus impt, LandmarkTableModel landmarks )
	{
		String transformType = TransformTypeSelectDialog.TPS;
		String fieldOfViewOption = ApplyBigwarpPlugin.SPECIFIED_PIXEL;
		String fieldOfViewPointFilter = "";
		String resolutionOption = ApplyBigwarpPlugin.SPECIFIED;
		double[] resolutionSpec = new double[]{ 0.4, 0.4, 0.8 };
		double[] fovSpec = new double[]{ 125, 125, 13 };
		// test a case in which the offset spec here in pixels (output resolution) is the same
		// as the phyical offset specified in spc_spec
		double[] offsetSpec = new double[]{ 93 / 0.4 ,103 / 0.4, 7 / 0.8 }; 
		Interpolation interp = Interpolation.NLINEAR;
		boolean isVirtual = false;
		int nThreads = 4;

		List<ImagePlus> alist = ApplyBigwarpPlugin.apply(
			impm,
			impt,
			landmarks,
			transformType,
			fieldOfViewOption,
			fieldOfViewPointFilter,
			resolutionOption,
			resolutionSpec,
			fovSpec,
			offsetSpec,
			interp,
			isVirtual,
			true,
			nThreads );
	
		ImagePlus a = alist.get( 0 );
		a.setTitle( "PIX-MOVING" );
		a.show();
	}
	
	public static void v_spc_spc( ImagePlus impm, ImagePlus impt, LandmarkTableModel landmarks )
	{
		String transformType = TransformTypeSelectDialog.TPS;
		String fieldOfViewOption = ApplyBigwarpPlugin.SPECIFIED_PHYSICAL;
		String fieldOfViewPointFilter = "";
		String resolutionOption = ApplyBigwarpPlugin.SPECIFIED;
		double[] resolutionSpec = new double[]{ 0.4, 0.4, 0.8 };
		double[] fovSpec = new double[]{ 50, 50, 10};
		double[] offsetSpec = new double[]{ 93 ,103, 7};
		Interpolation interp = Interpolation.NLINEAR;
		boolean isVirtual = true;
		int nThreads = 4;

		List<ImagePlus> alist = ApplyBigwarpPlugin.apply(
			impm,
			impt,
			landmarks,
			transformType,
			fieldOfViewOption,
			fieldOfViewPointFilter,
			resolutionOption,
			resolutionSpec,
			fovSpec,
			offsetSpec,
			interp,
			isVirtual,
			true,
			nThreads );

		ImagePlus a = alist.get( 0 );
		a.setTitle( "VIRT-PHYS-MOVING" );
		a.show();
	}
	
	public static void spc_spc( ImagePlus impm, ImagePlus impt, LandmarkTableModel landmarks )
	{
		String transformType = TransformTypeSelectDialog.TPS;
		String fieldOfViewOption = ApplyBigwarpPlugin.SPECIFIED_PHYSICAL;
		String fieldOfViewPointFilter = "";
		String resolutionOption = ApplyBigwarpPlugin.SPECIFIED;
		double[] resolutionSpec = new double[]{ 0.5, 0.5, 0.5 };
		double[] fovSpec = new double[]{ 50, 50, 10};
		double[] offsetSpec = new double[]{ 93*0.5, 103*0.5, 7*0.5};
		Interpolation interp = Interpolation.NLINEAR;
		boolean isVirtual = false;
		int nThreads = 4;

		List<ImagePlus> alist = ApplyBigwarpPlugin.apply(
			impm,
			impt,
			landmarks,
			transformType,
			fieldOfViewOption,
			fieldOfViewPointFilter,
			resolutionOption,
			resolutionSpec,
			fovSpec,
			offsetSpec,
			interp,
			isVirtual,
			true,
			nThreads );

		ImagePlus a = alist.get( 0 );
		a.setTitle( "PHYS-MOVING" );
		a.show();
	}

	public static void pspc_pspc( ImagePlus impm, ImagePlus impt, LandmarkTableModel landmarks )
	{
		String transformType = TransformTypeSelectDialog.TPS;
		String fieldOfViewOption = ApplyBigwarpPlugin.SPECIFIED_PIXEL;
		String fieldOfViewPointFilter = "";
		String resolutionOption = ApplyBigwarpPlugin.SPECIFIED;
		double[] resolutionSpec = new double[]{ 0.4, 0.4, 0.8 };
		double[] fovSpec = new double[]{ 50, 50, 10 };
		double[] offsetSpec = new double[]{ 93 ,103, 7};
		Interpolation interp = Interpolation.NLINEAR;
		boolean isVirtual = false;
		int nThreads = 4;

		List<ImagePlus> alist = ApplyBigwarpPlugin.apply(
			impm,
			impt,
			landmarks,
			transformType,
			fieldOfViewOption,
			fieldOfViewPointFilter,
			resolutionOption,
			resolutionSpec,
			fovSpec,
			offsetSpec,
			interp,
			isVirtual,
			true,
			nThreads );

		ImagePlus a = alist.get( 0 );
		a.setTitle( "PHYS-MOVING" );
		a.show();
	}
	
	public static void lmk_mvg( ImagePlus impm, ImagePlus impt, LandmarkTableModel landmarks )
	{
		String transformType = TransformTypeSelectDialog.TPS;
		String fieldOfViewOption = ApplyBigwarpPlugin.LANDMARK_POINTS;
		String fieldOfViewPointFilter = "";
		String resolutionOption = ApplyBigwarpPlugin.MOVING;
		double[] resolutionSpec = new double[ 3 ];
		double[] fovSpec = new double[ 3 ];
		double[] offsetSpec = new double[ 3 ];
		Interpolation interp = Interpolation.NLINEAR;
		boolean isVirtual = false;
		int nThreads = 4;

		List<ImagePlus> alist = ApplyBigwarpPlugin.apply(
			impm,
			impt,
			landmarks,
			transformType,
			fieldOfViewOption,
			fieldOfViewPointFilter,
			resolutionOption,
			resolutionSpec,
			fovSpec,
			offsetSpec,
			interp,
			isVirtual,
			true,
			nThreads );

		ImagePlus a = alist.get( 0 );
		a.setTitle( "LANDMARK-MOVING" );
		a.show();
	}

	public static void lmk_tgt( ImagePlus impm, ImagePlus impt, LandmarkTableModel landmarks )
	{
		String transformType = TransformTypeSelectDialog.TPS;
		String fieldOfViewOption = ApplyBigwarpPlugin.LANDMARK_POINTS;
		String fieldOfViewPointFilter = "";
		String resolutionOption = ApplyBigwarpPlugin.TARGET;
		double[] resolutionSpec = new double[ 3 ];
		double[] fovSpec = new double[ 3 ];
		double[] offsetSpec = new double[ 3 ];
		Interpolation interp = Interpolation.NLINEAR;
		boolean isVirtual = false;
		int nThreads = 4;

		List<ImagePlus> alist = ApplyBigwarpPlugin.apply(
			impm,
			impt,
			landmarks,
			transformType,
			fieldOfViewOption,
			fieldOfViewPointFilter,
			resolutionOption,
			resolutionSpec,
			fovSpec,
			offsetSpec,
			interp,
			isVirtual,
			true,
			nThreads );

		ImagePlus a = alist.get( 0 );
		a.setTitle( "LANDMARK-TARGET" );
		a.show();
	}

	public static void lmk_spc( ImagePlus impm, ImagePlus impt, LandmarkTableModel landmarks )
	{
		String transformType = TransformTypeSelectDialog.TPS;
		String fieldOfViewOption = ApplyBigwarpPlugin.LANDMARK_POINTS;
		String fieldOfViewPointFilter = "";
		String resolutionOption = ApplyBigwarpPlugin.SPECIFIED;
		double[] resolutionSpec = new double[]{ 0.4, 0.4, 0.8 };
		double[] fovSpec = new double[ 3 ];
		double[] offsetSpec = new double[ 3 ];
		Interpolation interp = Interpolation.NLINEAR;
		boolean isVirtual = false;
		int nThreads = 4;

		List<ImagePlus> alist = ApplyBigwarpPlugin.apply(
			impm,
			impt,
			landmarks,
			transformType,
			fieldOfViewOption,
			fieldOfViewPointFilter,
			resolutionOption,
			resolutionSpec,
			fovSpec,
			offsetSpec,
			interp,
			isVirtual,
			true,
			nThreads );

		ImagePlus a = alist.get( 0 );
		a.setTitle( "LANDMARK-SPECD" );
		a.show();
	}
	
	
	public static void mvg_mvg( ImagePlus impm, ImagePlus impt, LandmarkTableModel landmarks )
	{
		String transformType = TransformTypeSelectDialog.TPS;
		String fieldOfViewOption = ApplyBigwarpPlugin.MOVING_WARPED;
		String fieldOfViewPointFilter = "";
		String resolutionOption = ApplyBigwarpPlugin.MOVING;
		double[] resolutionSpec = new double[ 3 ];
		double[] fovSpec = new double[ 3 ];
		double[] offsetSpec = new double[ 3 ];
		Interpolation interp = Interpolation.NLINEAR;
		boolean isVirtual = false;
		int nThreads = 4;

		List<ImagePlus> alist = ApplyBigwarpPlugin.apply(
			impm,
			impt,
			landmarks,
			transformType,
			fieldOfViewOption,
			fieldOfViewPointFilter,
			resolutionOption,
			resolutionSpec,
			fovSpec,
			offsetSpec,
			interp,
			isVirtual,
			true,
			nThreads );

		ImagePlus a = alist.get( 0 );
		a.setTitle( "MOVING-MOVING" );
		a.show();
	}
	
	public static void mvg_spc( ImagePlus impm, ImagePlus impt, LandmarkTableModel landmarks )
	{
		String transformType = TransformTypeSelectDialog.TPS;
		String fieldOfViewOption = ApplyBigwarpPlugin.MOVING_WARPED;
		String fieldOfViewPointFilter = "";
		String resolutionOption = ApplyBigwarpPlugin.SPECIFIED;
		double[] resolutionSpec = new double[]{ 0.4, 0.4, 0.8 };
		double[] fovSpec = new double[ 3 ];
		double[] offsetSpec = new double[ 3 ];
		Interpolation interp = Interpolation.NLINEAR;
		boolean isVirtual = false;
		int nThreads = 4;

		List<ImagePlus> alist = ApplyBigwarpPlugin.apply(
			impm,
			impt,
			landmarks,
			transformType,
			fieldOfViewOption,
			fieldOfViewPointFilter,
			resolutionOption,
			resolutionSpec,
			fovSpec,
			offsetSpec,
			interp,
			isVirtual,
			true,
			nThreads );

		ImagePlus a = alist.get( 0 );
		a.setTitle( "MOVING-SPECD" );
		a.show();
	}
	
	public static void mvg_tgt( ImagePlus impm, ImagePlus impt, LandmarkTableModel landmarks )
	{
		String transformType = TransformTypeSelectDialog.TPS;
		String fieldOfViewOption = ApplyBigwarpPlugin.MOVING_WARPED;
		String fieldOfViewPointFilter = "";
		String resolutionOption = ApplyBigwarpPlugin.TARGET;
		double[] resolutionSpec = new double[ 3 ];
		double[] fovSpec = new double[ 3 ];
		double[] offsetSpec = new double[ 3 ];
		Interpolation interp = Interpolation.NLINEAR;
		boolean isVirtual = false;
		int nThreads = 4;

		List<ImagePlus> alist = ApplyBigwarpPlugin.apply(
			impm,
			impt,
			landmarks,
			transformType,
			fieldOfViewOption,
			fieldOfViewPointFilter,
			resolutionOption,
			resolutionSpec,
			fovSpec,
			offsetSpec,
			interp,
			isVirtual,
			true,
			nThreads );

		ImagePlus a = alist.get( 0 );
		a.setTitle( "MOVING-TARGET" );
		a.show();
	}	
	
	public static void tgt_tgt( ImagePlus impm, ImagePlus impt, LandmarkTableModel landmarks )
	{
		String transformType = TransformTypeSelectDialog.TPS;
		String fieldOfViewOption = ApplyBigwarpPlugin.TARGET;
		String fieldOfViewPointFilter = "";
		String resolutionOption = ApplyBigwarpPlugin.TARGET;
		double[] resolutionSpec = new double[ 3 ];
		double[] fovSpec = new double[ 3 ];
		double[] offsetSpec = new double[ 3 ];
		Interpolation interp = Interpolation.NLINEAR;
		boolean isVirtual = false;
		int nThreads = 4;
		
		List<ImagePlus> alist = ApplyBigwarpPlugin.apply(
			impm,
			impt,
			landmarks,
			transformType,
			fieldOfViewOption,
			fieldOfViewPointFilter,
			resolutionOption,
			resolutionSpec,
			fovSpec,
			offsetSpec,
			interp,
			isVirtual,
			true,
			nThreads );

		ImagePlus a = alist.get( 0 );
		a.setTitle( "TARGET-TARGET" );
		a.show();
	}
	
	public static void v_tgt_tgt( ImagePlus impm, ImagePlus impt, LandmarkTableModel landmarks )
	{
		String transformType = TransformTypeSelectDialog.TPS;
		String fieldOfViewOption = ApplyBigwarpPlugin.TARGET;
		String fieldOfViewPointFilter = "";
		String resolutionOption = ApplyBigwarpPlugin.TARGET;
		double[] resolutionSpec = new double[ 3 ];
		double[] fovSpec = new double[ 3 ];
		double[] offsetSpec = new double[ 3 ];
		Interpolation interp = Interpolation.NLINEAR;
		boolean isVirtual = true;
		int nThreads = 4;
		
		List<ImagePlus> alist = ApplyBigwarpPlugin.apply(
			impm,
			impt,
			landmarks,
			transformType,
			fieldOfViewOption,
			fieldOfViewPointFilter,
			resolutionOption,
			resolutionSpec,
			fovSpec,
			offsetSpec,
			interp,
			isVirtual,
			true,
			nThreads );

		System.out.println( "alist size: " + alist.size());

		ImagePlus a = alist.get( 0 );
		a.setTitle( "VIRT-TARGET-TARGET" );
		a.show();
	}
	
	public static void tgt_spc( ImagePlus impm, ImagePlus impt, LandmarkTableModel landmarks )
	{
		String transformType = TransformTypeSelectDialog.TPS;
		String fieldOfViewOption = ApplyBigwarpPlugin.TARGET;
		String fieldOfViewPointFilter = "";
		String resolutionOption = ApplyBigwarpPlugin.SPECIFIED;
		double[] resolutionSpec = new double[]{ 0.4, 0.4, 0.8 };
		double[] fovSpec = new double[ 3 ];
		double[] offsetSpec = new double[ 3 ];
		Interpolation interp = Interpolation.NLINEAR;
		boolean isVirtual = false;
		int nThreads = 4;

		List<ImagePlus> alist = ApplyBigwarpPlugin.apply(
			impm,
			impt,
			landmarks,
			transformType,
			fieldOfViewOption,
			fieldOfViewPointFilter,
			resolutionOption,
			resolutionSpec,
			fovSpec,
			offsetSpec,
			interp,
			isVirtual,
			true,
			nThreads );

		ImagePlus a = alist.get( 0 );
		a.setTitle( "TARGET-SPECIFIED" );
		a.show();
	}

	public static void tgt_mvg( ImagePlus impm, ImagePlus impt, LandmarkTableModel landmarks )
	{
		String transformType = TransformTypeSelectDialog.TPS;
		String fieldOfViewOption = ApplyBigwarpPlugin.TARGET;
		String fieldOfViewPointFilter = "";
		String resolutionOption = ApplyBigwarpPlugin.MOVING;
		double[] resolutionSpec = new double[ 3 ];
		double[] fovSpec = new double[ 3 ];
		double[] offsetSpec = new double[ 3 ];
		Interpolation interp = Interpolation.NLINEAR;
		boolean isVirtual = false;
		int nThreads = 4;

		List<ImagePlus> alist = ApplyBigwarpPlugin.apply(
			impm,
			impt,
			landmarks,
			transformType,
			fieldOfViewOption,
			fieldOfViewPointFilter,
			resolutionOption,
			resolutionSpec,
			fovSpec,
			offsetSpec,
			interp,
			isVirtual,
			true,
			nThreads );

		ImagePlus a = alist.get( 0 );
		a.setTitle( "TARGET-MOVING" );
		a.show();
	}
		
	public static void tgt_lmpix( ImagePlus impm, ImagePlus impt, LandmarkTableModel landmarks )
	{
		String transformType = TransformTypeSelectDialog.TPS;
		String fieldOfViewOption = ApplyBigwarpPlugin.LANDMARK_POINT_CUBE_PIXEL;
		String fieldOfViewPointFilter = ".*5";
		String resolutionOption = ApplyBigwarpPlugin.TARGET;
		double[] resolutionSpec = new double[ 3 ];
		double[] fovSpec = new double[]{ 24, 24, 24 };
		double[] offsetSpec = new double[ 3 ];
		Interpolation interp = Interpolation.NLINEAR;
		boolean isVirtual = false;
		int nThreads = 4;

		List<ImagePlus> alist = ApplyBigwarpPlugin.apply(
			impm,
			impt,
			landmarks,
			transformType,
			fieldOfViewOption,
			fieldOfViewPointFilter,
			resolutionOption,
			resolutionSpec,
			fovSpec,
			offsetSpec,
			interp,
			isVirtual,
			true,
			nThreads );

		ImagePlus a = alist.get( 0 );
		a.setTitle( "TARGET-LMPIX" );
		a.show();
	}

	public static void tgt_lmphy( ImagePlus impm, ImagePlus impt, LandmarkTableModel landmarks )
	{
		String transformType = TransformTypeSelectDialog.TPS;
		String fieldOfViewOption = ApplyBigwarpPlugin.LANDMARK_POINT_CUBE_PHYSICAL;
		String fieldOfViewPointFilter = ".*5";
		String resolutionOption = ApplyBigwarpPlugin.TARGET;
		double[] resolutionSpec = new double[]{ 0.4, 0.4, 0.8 };
		double[] fovSpec = new double[]{5, 5, 5 };
		double[] offsetSpec = new double[ 3 ];
		Interpolation interp = Interpolation.NLINEAR;
		boolean isVirtual = false;
		int nThreads = 4;

		List<ImagePlus> alist = ApplyBigwarpPlugin.apply(
			impm,
			impt,
			landmarks,
			transformType,
			fieldOfViewOption,
			fieldOfViewPointFilter,
			resolutionOption,
			resolutionSpec,
			fovSpec,
			offsetSpec,
			interp,
			isVirtual,
			true,
			nThreads );

		ImagePlus a = alist.get( 0 );
		a.setTitle( "TARGET-LMPHYS" );
		a.show();
	}
	
}
