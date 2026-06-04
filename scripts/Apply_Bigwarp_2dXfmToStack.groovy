#@ File (label="Landmark file") landmarksPath
#@ ImagePlus (label="Moving image") movingIp
#@ ImagePlus (label="Target image") targetIp
#@ String (label="Transform type", choices={"Thin Plate Spline", "Affine", "Similarity", "Rotation", "Translation" }) transformType
#@ String (label="Interpolation", choices={"Linear", "Nearest Neighbor"}) interpType
#@ Integer (label="Number of threads", min=1, max=64, value=1) nThreads
#@ UIService ui


int nd = 2;
ltm = new LandmarkTableModel( nd );
try
{
	ltm.load( landmarksPath );
} catch ( IOException e ) {
	ui.showMessage("Could not load landmarks from:\n" +  landmarksPath)
	e.printStackTrace();
	return;
}

// apply transformation to every slice / frame
sz = movingIp.getImageStackSize();
sliceResults = new ImageProcessor[sz];
for( int i = 0; i < sz; i++) {
	sliceIp = new ImagePlus("slice"+i, movingIp.getStack().getProcessor(i+1));
	sliceIp.setCalibration(movingIp.getCalibration());
	sliceResults[i] = transform(sliceIp).getProcessor();
}


// combine slices and show
nx = sliceResults[0].getWidth();
ny = sliceResults[0].getHeight();
stack = new ImageStack(nx, ny);
for( int i = 0; i < sz; i++) {
	stack.addSlice(sliceResults[i]);
}
result = new ImagePlus(movingIp.getTitle() + "-transformed", stack);
result.setDimensions(movingIp.getNChannels(), movingIp.getNSlices(), movingIp.getNFrames());
result.show();


// Applies transform to one slice
def transform( ImagePlus mvg ) {

	bwData = BigWarpInit.createBigWarpDataFromImages(mvg, targetIp);
	bwData.wrapMovingSources();

	bboxEst = new BoundingBoxEstimation(BoundingBoxEstimation.Method.CORNERS);
	emptyWriteOpts = new ApplyBigwarpPlugin.WriteDestinationOptions( "", "", null, null );

	Interpolation interp = Interpolation.NLINEAR;
	if (interpType.equals("Nearest Neighbor"))
		interp = Interpolation.NEARESTNEIGHBOR;

	invXfm = new BigWarpTransform( ltm, transformType ).getTransformation();
	show = false;
	isVirtual = false;

	warpedIpList = ApplyBigwarpPlugin.apply(
			bwData, ltm, invXfm, transformType, "Target", "", bboxEst,
			"Target", null, null, null,
			interp, isVirtual, nThreads, true, emptyWriteOpts, show);

	return warpedIpList.get(0);
}

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import bdv.ij.ApplyBigwarpPlugin;
import bdv.ij.ApplyBigwarpPlugin.WriteDestinationOptions;
import bdv.viewer.Interpolation;
import bigwarp.BigWarpData;
import bigwarp.BigWarpInit;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import net.imglib2.realtransform.BoundingBoxEstimation;
import net.imglib2.realtransform.InvertibleRealTransform;
