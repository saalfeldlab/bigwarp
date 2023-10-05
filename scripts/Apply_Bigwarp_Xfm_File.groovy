#@ File (label="Landmark file") landmarksPath
#@ File (label="Moving image file") movingPath
#@ File (label="Result image file") resultPath
#@ Double (label="x size") sizeX
#@ Double (label="y size") sizeY
#@ Double (label="z size") sizeZ
#@ Double (label="x spacing") resX
#@ Double (label="y spacing") resY
#@ Double (label="z spacing") resZ
#@ String (label="Transform type", choices={"Thin Plate Spline", "Affine", "Similarity", "Rotation", "Translation" }) transformType
#@ String (label="Interpolation", choices={"Linear", "Nearest Neighbor"}) interpType
#@ Integer (label="Number of threads", min=1, max=64, value=1) nThreads

import java.io.File;
import java.io.IOException;
import bdv.ij.ApplyBigwarpPlugin;
import bdv.viewer.Interpolation;
import bigwarp.landmarks.LandmarkTableModel;
import net.imglib2.realtransform.BoundingBoxEstimation;
import ij.IJ;
import ij.ImagePlus;

movingIp = IJ.openImage( movingPath.getAbsolutePath() );

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

res = [ resX, resY, resZ ] as double[];
fov = [ sizeX, sizeY, sizeZ ] as double[];

bboxEst = new BoundingBoxEstimation();

Interpolation interp = Interpolation.NLINEAR;
if( interpType.equals( "Nearest Neighbor" ))
	interp = Interpolation.NEARESTNEIGHBOR;

emptyWriteOpts = new ApplyBigwarpPlugin.WriteDestinationOptions( "", "", null, null );
warpedIpList = ApplyBigwarpPlugin.apply(
		movingIp, movingIp, ltm, transformType,
		ApplyBigwarpPlugin.SPECIFIED_PIXEL, "", bboxEst, ApplyBigwarpPlugin.SPECIFIED,
		res, fov, [0,0,0] as double[],
		interp, false, nThreads, true, emptyWriteOpts );


IJ.save( warpedIpList.get(0), resultPath.getAbsolutePath() );
