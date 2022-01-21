#@ File (label="Landmark file") landmarksPath
#@ ImagePlus ( label = "Moving image" ) mvgImg
#@ ImagePlus ( label = "Fixed image" ) tgtImg
#@ String ( label="Direction", choices={"Moving to target", "Target to moving"}, style="radioButtonHorizontal") inverseOrForward
#@ Double (label="Inverse tolerance", value=0.1 ) invTolerance
#@ Integer (label="Inverse maximum iterations", value=200) invMaxIters

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
needInverse = mvgToTarget;

if( mvgToTarget ) {
	srcImg = mvgImg;
	dstImg = tgtImg;
}
else {
	srcImg = tgtImg;
	dstImg = mvgImg;
}

// convert the roi to a densely spaced polygon if necessary
roi = srcImg.getRoi()
if( roi.getType() == Roi.POINT ) {
	println('point roi')
	floatPolygon = roi.getFloatPolygon()
}
else{
	println('other roi')
	floatPolygon = roi.getInterpolatedPolygon()
}

N = floatPolygon.npoints
xpts = floatPolygon.xpoints
ypts = floatPolygon.ypoints

// transform all points of the polygon

bwTransform = new BigWarpTransform( ltm, transformType );
transform = bwTransform.getTransformation();
if( needInverse )
    transform = transform.inverse();

result = new double[ 2 ];

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
	pt = [ src_rx * xpts[ i ], src_ry * ypts[ i ]] as double[]

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

if( roi.getType() == Roi.POINT ) {
	roiWarped = new PointRoi(  xpointsWarped, ypointsWarped );
	roiWarped.setSize( roi.getSize() )
	roiWarped.setPointType( roi.getPointType() )
	roiWarped.setPointType( roi.getPointType() )
}
else{
	roiWarped =  new PolygonRoi( new FloatPolygon( xpointsWarped, ypointsWarped ), Roi.POLYGON );
}
dstImg.setRoi( roiWarped )
