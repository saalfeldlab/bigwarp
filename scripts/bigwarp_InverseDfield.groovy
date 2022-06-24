#@ UIService ui
#@ Dataset movingImage
#@ File landmarks
#@ Double (label="Inverse tolerance", value=0.1 ) invTolerance
#@ Integer (label="Inverse maximum iterations", value=200) invMaxIters
#@ Integer (label="Number of threads", min=1, max=64, value=1) nThreads


// setup - output resolution and size
nd = movingImage.numDimensions();
dims = movingImage.dimensionsAsLongArray();
if( nd == 2){
    pixelToPhysical = new Scale2D( (0..1).collect{ movingImage.averageScale(it) }  as double[] );
    dfieldDims = new long[]{ dims[0], dims[1], 2};
}
else if( nd == 3){
    pixelToPhysical = new Scale3D( (0..2).collect{ movingImage.averageScale(it) }  as double[] );
    dfieldDims = new long[]{ dims[0], dims[1], 3, dims[2]};
}
else
{
	ui.showDialog( "image must be 2d or 3d" );
	return;
}

// load landmarks
ltm = new LandmarkTableModel( nd );
ltm.load( landmarks );

// build transform and set inverse options
transform = new BigWarpTransform( ltm, TransformTypeSelectDialog.TPS ).getTransformation();
transform.getOptimzer().setMaxIters( invMaxIters );
transform.getOptimzer().setTolerance( invTolerance );

// create displacement field
dfieldIp = BigWarpToDeformationFieldPlugIn.convertToDeformationField(
	dfieldDims,
	transform.inverse(),
	pixelToPhysical,
	nThreads );

dfieldIp.getImagePlus().show();


import net.imglib2.realtransform.Scale2D;
import net.imglib2.realtransform.Scale3D;
import bdv.ij.BigWarpToDeformationFieldPlugIn;
import bdv.gui.TransformTypeSelectDialog;
import bigwarp.transforms.BigWarpTransform;
import bigwarp.landmarks.LandmarkTableModel;