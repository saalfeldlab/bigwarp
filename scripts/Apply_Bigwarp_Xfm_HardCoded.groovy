import java.io.File;
import java.io.IOException;
import bdv.ij.ApplyBigwarpPlugin;
import bdv.viewer.Interpolation;
import bigwarp.landmarks.LandmarkTableModel;
import net.imglib2.realtransform.BoundingBoxEstimation;
import ij.IJ;
import ij.ImagePlus;

// Fixed parameters
transformType = "Thin Plate Spline";
interpType = "Linear";
nThreads = 4;

// Lists of inputs and output paths
// must all be the same length
movingPaths = [
	"/home/john/tmp/mri-stack.tif",
	"/home/john/tmp/boats.tif" ];

referencePaths = [
	"/home/john/tmp/mri-stack.tif",
	"/home/john/tmp/boats.tif" ];

landmarkPaths = [
	"/home/john/tmp/mri-stack-landmarks.csv",
	"/home/john/tmp/boats_landmarks.csv" ];

outputPaths = [
	"/home/john/tmp/mri-stack_transformed.tif",
	"/home/john/tmp/boats_transformed.tif" ];


// Loop over all input parameters and apply the transformation
( 0..<movingPaths.size() ).collect {

	println( "transforming " + movingPaths[it] );
	run( movingPaths[it], referencePaths[it], landmarkPaths[it], outputPaths[it] );
}

/**
 * This function reads the moving and reference images and landmarks at the given paths,
 * transforms the moving image, then writes the output image to the provided path.
 */
def run( moving, reference, landmarks, output ) {

	mvgIp = IJ.openImage( moving );
	refIp = IJ.openImage( reference );

	int nd = 2;
	if ( mvgIp.getNSlices() > 1 )
		nd = 3;

	ltm = new LandmarkTableModel( nd );
	try
	{
		ltm.load( new File( landmarks ));
	} catch ( IOException e )
	{
		e.printStackTrace();
		return;
	}

	Interpolation interp = Interpolation.NLINEAR;
	if( interpType.equals( "Nearest Neighbor" ))
		interp = Interpolation.NEARESTNEIGHBOR;

	bboxEst = new BoundingBoxEstimation();

	emptyWriteOpts = new ApplyBigwarpPlugin.WriteDestinationOptions( "", "", null, null );
	outputList = ApplyBigwarpPlugin.apply(
			mvgIp, refIp, ltm, transformType,
			"Target", "", bboxEst, "Target",
			null, null, null,
			interp, false, nThreads, true, emptyWriteOpts );

	IJ.save( outputList.get(0), output );
}