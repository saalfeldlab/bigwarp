#@ File (label="Landmark file") landmarksPath
#@ ImagePlus (label="Moving image") movingIp
#@ ImagePlus (label="Target image") targetIp
#@ String (label="Interpolation", choices={"Linear", "Nearest Neighbor"}) interpType
#@ Boolean (label="Output as virtual?") isVirtual
#@ Integer (label="Number of threads", min=1, max=64, value=1) nThreads


import java.io.IOException;
import bdv.ij.ApplyBigwarpPlugin;
import bdv.viewer.Interpolation;
import bigwarp.landmarks.LandmarkTableModel;
import ij.ImagePlus;

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

ImagePlus warpedIp = ApplyBigwarpPlugin.apply( 
		movingIp, targetIp, ltm,
		"Target", "", "Target",
		null, null, null,
		interp, isVirtual, nThreads );

warpedIp.show();
