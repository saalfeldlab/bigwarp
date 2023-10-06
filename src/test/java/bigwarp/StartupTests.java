package bigwarp;

import java.io.IOException;

import bdv.gui.BigWarpInitDialog;
import ij.ImageJ;

public class StartupTests {

	public static void main( final String[] args ) throws IOException
	{
//		ImageJ ij2 = new ImageJ();
//		ij2.ui().showUI();

		final ImageJ ij = new ImageJ();
//
//		IJ.openImage( "/groups/saalfeld/home/bogovicj/tmp/boatsBlur.tif" ).show();
//		IJ.openImage( "/groups/saalfeld/home/bogovicj/tmp/boats.tif" ).show();
//
//		IJ.openImage( "/home/john/tmp/boats.tif" ).show();
//		IJ.openImage( "/home/john/tmp/boatsBlur.tif" ).show();

//		IJ.createImage("", ImageJPrefix, DEFAULT_OUTER_PAD, DEFAULT_MID_PAD, DEFAULT_BUTTON_PAD)
//		IJ.createImage("a", 32, 32, 8, 8).show();

//		IJ.openImage( "/home/john/tmp/mri-stack.tif" ).show();
//		IJ.openImage( "/home/john/tmp/t1-head.tif" ).show();

//		new ImageJ();
//		IJ.openImage( "/home/john/tmp/mri-stack.tif" ).show();
//		String macroOptions = "images=imagej://mri-stack.tif,imagej://mri-stack.tif moving=true,false transforms=,";
//		runMacro( macroOptions );

//		IJ.openImage( "/home/john/tmp/t1-head.tif" ).show();
//		IJ.openImage( "/home/john/tmp/mri-stack.tif" ).show();

		final String proj = "/home/john/Desktop/bw-boats.json";
//		final String proj = "/home/john/Desktop/bw-boats-affine2d.json";
//		final String proj = "/home/john/Desktop/bw-boats-tlation.json";
//		final String proj = "/home/john/Desktop/bw-boats-tlationImported-fineTune.json";

//		final String proj = "/home/john/Desktop/bw-boats-affine2d-imported.json";
//		final String proj = "/home/john/Desktop/bigwarp-nomask-project.json";
//		final String proj = "/home/john/Desktop/bigwarp-affine2d.json";
//		final String proj = "/home/john/Desktop/bigwarp-vmask-project.json";
//		final String proj = "/home/john/Desktop/bigwarp-mask-project.json";
//		final String proj = "/home/john/Desktop/bigwarp-mask51k-project.json";
//		final String proj = "/home/john/Desktop/bigwarp-maskn5a-project.json";
//		final String proj = "/home/john/Desktop/bigwarp-project.json";
//		final String proj = "/home/john/Desktop/t1-bigwarp-project.json";
//		final String proj = "/home/john/Desktop/inf-bw.json";

//		final String proj = "/home/john/projects/bigwarp/projects/bw_jrc18Down.json";
//		final String proj = "/home/john/projects/bigwarp/projects/bw_jrc18Down-vmask.json";
//		final String proj = "/home/john/Desktop/bw-jrc18-rot.json";
//		final String proj = "/home/john/Desktop/bw-jrc18-rot-maskTune.json";

		final String boats = "/home/john/tmp/boats.tif";
		final String jrc18 = "/home/john/Documents/teaching/emboBioImage2023_registration/sampleImages/jrc18_down.nrrd";
//		final String jrc18Df2 = "/home/john/projects/bigwarp/projects/bw_jrc18Down_tforms.n5?#coordinateTransformations[3]";
//		final String jrc18Df5 = "/home/john/projects/bigwarp/projects/bw_jrc18Down_tforms.n5?#coordinateTransformations[5]";
//		final String jrc18DfTgt = "/home/john/projects/bigwarp/projects/bw_jrc18Down_tforms.n5?#coordinateTransformations[0]";

		final String boatsTlationDf = "/home/john/projects/bigwarp/projects/boats-chain.zarr?/#coordinateTransformations[1]";
		final String boatsAffine = "/home/john/projects/bigwarp/projects/boats-chain.zarr?/#coordinateTransformations[2]";

//		final String jrc18DfMvgWrp = "/home/john/projects/bigwarp/projects/bw_jrc18Down_tforms.n5?#coordinateTransformations[2]";
//		final String jrc18DfTgt = "/home/john/projects/bigwarp/projects/jrc18.zarr?/#coordinateTransformations[0]";

		final String jrc18RotDfTgt = "/home/john/projects/bigwarp/projects/jrc18-rot.zarr?/#coordinateTransformations[0]";
		final String jrc18RotTotSeq = "/home/john/projects/bigwarp/projects/jrc18-rot.zarr?/#coordinateTransformations[1]";
		final String jrc18RotPartial = "/home/john/projects/bigwarp/projects/jrc18-rot.zarr?/#coordinateTransformations[2]";
		final String jrc18RotTotSeqRev = "/home/john/projects/bigwarp/projects/jrc18-rot.zarr?/#coordinateTransformations[3]";

//		final String jrc18DfAffine = "/home/john/projects/bigwarp/projects/jrc18.zarr?/#coordinateTransformations[3]";

//		runBigWarp(null, new String[] {boats, boats}, new String[] {"true", "false" }, new String[]{});
//		runBigWarp(proj, new String[] {boats, boats}, new String[] {"true", "false" }, null);
//		runBigWarp(null, new String[] {boats, boats}, new String[] {"true", "false" }, new String[]{"/home/john/Desktop/tforms.n5?aff2d#coordinateTransformations[0]", null});
//		runBigWarp(null, new String[] {boats, boats}, new String[] {"true", "false" }, new String[]{"/home/john/projects/bigwarp/projects/boats-chain.zarr?/#coordinateTransformations[0]", null});
//		runBigWarp(null, new String[] {boats, boats}, new String[] {"true", "false" }, new String[]{boatsAffine, null});
//		runBigWarp(null, new String[] {boats}, new String[] {"true"}, new String[]{boatsAffine});

//		runBigWarp(null, new String[] {boats, boats}, new String[] {"true", "false" }, new String[]{"/home/john/projects/bigwarp/projects/boats.n5?/#coordinateTransformations[0]", null});
//		runBigWarp(null, new String[] {boats, boats}, new String[] {"true", "false" }, new String[]{"/home/john/projects/bigwarp/projects/boats.zarr?/#coordinateTransformations[0]", null});
//		runBigWarp(null, new String[] {boats, boats}, new String[] {"true", "false" }, new String[]{"/home/john/projects/bigwarp/projects/boats.zarr?/#coordinateTransformations[2]", null});

//		runBigWarp(null, new String[] {jrc18, jrc18}, new String[] {"true", "false" }, new String[]{ jrc18RotTotSeqRev, null });

		BigWarpInitDialog.runBigWarp(proj, new String[] {}, new String[] {}, null);

//		BigWarpInitDialog.createAndShow();


		// below this are from BigWarpCommand

//			String images = Macro.getValue(options, "images", "");
//			String moving = Macro.getValue(options, "moving", "");
//			String transforms = Macro.getValue(options, "transforms", "");
//			System.out.println( images );
//			System.out.println( moving );
//			System.out.println( transforms );

//			final ImageJ ij2 = new ImageJ();
//			ij2.ui().showUI();

//			final Object im1 = ij2.io().open( "/home/john/tmp/mri-stack.tif" );
//			final Object im2 = ij2.io().open( "/home/john/tmp/t1-head.tif" );
////			Object im1 = ij2.io().open( "/groups/saalfeld/home/bogovicj/tmp/mri-stack.tif" );
////			Object im2 = ij2.io().open( "/groups/saalfeld/home/bogovicj/tmp/t1-head.tif" );
//			ij2.ui().show( im1 );
//			ij2.ui().show( im2 );
//			final Object im1 = ij2.io().open( "/home/john/tmp/boats.tif" );
//			ij2.ui().show( im1 );
//			String args = "images=[a, b, c], isMoving=[true, true, false], transforms=[,,]";
//			String imagesList = null;
//			String isMovingList = null;
//			String transformsList = null;

	}
}
