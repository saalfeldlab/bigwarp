// @ImagePlus( label = "Target image" ) tgtImg
// @ImagePlus( label = "Moving image" ) mvgImg
// @File(label="Landmark file") landmarksPath
// @String( choices={"Moving to target", "Target to moving"}, style="radioButtonHorizontal") inverseOrForward

import ij.*;
import ij.gui.*;
import ij.process.*;

import java.util.*;
import java.io.IOException;

import bigwarp.landmarks.LandmarkTableModel;

nd = 2
ltm = new LandmarkTableModel( nd );
try {
	ltm.load( landmarksPath );
} catch ( IOException e ) {
	e.printStackTrace();
	return;
}

// duplicate variable for clarity of code
mvgToTarget = inverseOrForward.equals("Moving to target")
needInverseTransform = mvgToTarget;

if( mvgToTarget ) {
	srcImg = mvgImg;
	dstImg = tgtImg;
}
else {
	srcImg = tgtImg;
	dstImg = mvgImg;
}

// convert the roi to a densely spaced polygon
floatPolygon = srcImg.getRoi().getInterpolatedPolygon()

// transform all points of the polygon
transform = ltm.getTransform();
result = new double[ 2 ];

N = floatPolygon.npoints;
xpointsWarped = new float[ N ]
ypointsWarped = new float[ N ]

// set tolerance in case we need to do an iterative inverse
// make sure its 40% the smaller pixel spacing
src_rx = srcImg.getCalibration().pixelWidth;
src_ry = srcImg.getCalibration().pixelHeight;

tgt_rx = dstImg.getCalibration().pixelWidth;
tgt_ry = dstImg.getCalibration().pixelHeight;

tolerance = 0.4 * tgt_rx;
maxIters = 200;
if( tgt_ry < tgt_rx )
	tolerance = 0.4 * tgt_ry;


for( int i = 0; i < N; i++ )
{
	// pixel to physical space
	pt = [ src_rx * floatPolygon.xpoints[ i ], src_ry * floatPolygon.ypoints[ i ]] as double[]

	if( needInverseTransform )
	{
		err = transform.inverseTol( pt, result, tolerance, maxIters )
	}
	else
	{
		transform.apply( pt, result )
	}

	// physical to pixel space
	xpointsWarped[ i ] = result[ 0 ] / tgt_rx
	ypointsWarped[ i ] = result[ 1 ] / tgt_ry
}

polygonWarped =  new PolygonRoi( new FloatPolygon( xpointsWarped, ypointsWarped ), Roi.POLYGON );
dstImg.setRoi( polygonWarped )