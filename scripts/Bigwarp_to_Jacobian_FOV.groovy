// @File(label="Landmark file") landmarksPath
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
// @Integer(label="Number of threads", min=1, max=64, value=1) nThreads
// @Boolean(label="Virtual stack?") isVirtual

import java.io.File;
import java.io.IOException;

import net.imglib2.view.Views;
import net.imglib2.util.Intervals;
import net.imglib2.img.imageplus.FloatImagePlus;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.realtransform.ThinplateSplineTransform;

import bdv.ij.ApplyBigwarpPlugin;
import bigwarp.BigWarp.BigWarpData;
import bigwarp.BigWarpExporter;
import bigwarp.source.JacobianRandomAccess;
import bigwarp.source.JacobianDeterminantRandomAccess;
import bigwarp.landmarks.LandmarkTableModel;
import ij.ImagePlus;

/* 
 * Add this at some point
 * @Boolean(label="Jacobian determinant", value=true) doJacDeterminant 
 */

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

resolutionSpec = ApplyBigwarpPlugin.SPECIFIED;
fovspec = "Specified (" + fovType + ")"
println( fovspec )

tps = new ThinplateSplineTransform( ltm.getTransform() );

res = [resx, resy, resz] as double[]
offIn = [offsetx, offsety, offsetz] as double[]
fovIn = [fovx, fovy, fovz] as double[]


outputInterval = ApplyBigwarpPlugin.getPixelInterval( null, ltm, fovspec, null, fovIn, offIn, res );
offset = ApplyBigwarpPlugin.getPixelOffset( fovspec, offIn, res, outputInterval );
println( 'res: ' + res )


img = JacobianDeterminantRandomAccess.createJacobianDeterminant( new FloatType(), tps );

ipi = ImagePlusImgs.floats( Intervals.dimensionsAsLongArray( outputInterval ));
BigWarpExporter.copyToImageStack( Views.raster( img ), ipi, 4 );

jip = ipi.getImagePlus();

jip.getCalibration().pixelWidth = res[ 0 ]
jip.getCalibration().pixelHeight = res[ 1 ]
jip.getCalibration().pixelDepth = res[ 2 ]
jip.setDimensions( 1, (int)outputInterval.dimension( 2 ), 1 );
jip.show();

