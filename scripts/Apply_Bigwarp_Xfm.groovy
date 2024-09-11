#@ File (label="Landmark file") landmarksPath
#@ File (label="Moving image file") movingPath
#@ File (label="Target image file") targetPath
#@ String (label="Transform type", choices={"Thin Plate Spline", "Affine", "Similarity", "Rotation", "Translation" }) transformType
#@ String (label="Interpolation", choices={"Linear", "Nearest Neighbor"}) interpType
#@ Integer (label="Number of threads", min=1, max=64, value=1) nThreads
#@ Boolean (label="Virtual stack?") isVirtual

import java.io.File;
import java.io.IOException;
import bdv.ij.ApplyBigwarpPlugin;
import bdv.viewer.Interpolation;
import bigwarp.BigWarpInit;
import bigwarp.landmarks.LandmarkTableModel;
import net.imglib2.realtransform.BoundingBoxEstimation;
import ij.IJ;
import ij.ImagePlus;


movingIp = IJ.openImage( movingPath.getAbsolutePath() );
targetIp = IJ.openImage( targetPath.getAbsolutePath() );

int nd = 2;
if ( movingIp.getNSlices() > 1 )
	nd = 3;

ltm = new LandmarkTableModel( nd );
try
{
	ltm.load( landmarksPath );
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

bwData = BigWarpInit.createBigWarpDataFromImages(movingIp, targetIp);
bwData.wrapMovingSources();
bboxEst = new BoundingBoxEstimation(BoundingBoxEstimation.Method.CORNERS);

warpedIpList =  ApplyBigwarpPlugin.apply(
		bwData, ltm, transformType, "Target", "", bboxEst,
		"Target", null, null, null,
		interp, isVirtual, nThreads, true, emptyWriteOpts);

warpedIpList.get(0).show();
