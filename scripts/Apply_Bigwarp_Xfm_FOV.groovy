// @File(label="Landmark file") landmarksPath
// @ImagePlus(label="Moving image file") movingIp
// @String(label="Specification type", choices={"pixel units", "physical units"}) fovType
// @Float(label="Resolution x") resx
// @Float(label="Resolution y") resy
// @Float(label="Resolution z") resz
// @Float(label="Width x") fovx
// @Float(label="Height y") fovy
// @Float(label="Depth z") fovz
// @Float(label="Offset x") offsetx
// @Float(label="Offset y") offsety
// @Float(label="Offset z") offsetz
// @String(label="Interpolation", choices={"Linear", "Nearest Neighbor"}) interpType
// @Integer(label="Number of threads", min=1, max=64, value=1) nThreads
// @Boolean(label="Virtual stack?") isVirtual

import java.io.File;
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

fovspec = "Specified (" + fovType + ")"


Interpolation interp = Interpolation.NLINEAR;
if( interpType.equals( "Nearest Neighbor" ))
	interp = Interpolation.NEARESTNEIGHBOR;


ImagePlus warpedIp = ApplyBigwarpPlugin.apply( 
		movingIp, movingIp, ltm,
		fovspec, "", "Specified",
		([ resx, resy, resz ] as double[]),
		([ fovx, fovy, fovz ] as double[]),
		([ offsetx, offsety, offsetz ] as double[]),
		interp, isVirtual, nThreads );

warpedIp.show();
