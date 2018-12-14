// @File(label="Landmark file") landmarksPath
// @ImagePlus(label="Target image") targetIp
// @Boolean(label="Output as virtual?") isVirtual
// @Integer(label="Number of threads", min=1, max=64, value=4) nThreads

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

res = ApplyBigwarpPlugin.getResolution( null, targetIp, ApplyBigwarpPlugin.TARGET, null )

outputInterval = ApplyBigwarpPlugin.getPixelInterval( targetIp, targetIp, ltm, ApplyBigwarpPlugin.TARGET, null, null, null, res )
println( outputInterval )

tps = new ThinplateSplineTransform( ltm.getTransform() );
img = JacobianDeterminantRandomAccess.createJacobianDeterminant( new FloatType(), tps );
imgres = ApplyBigwarpPlugin.transfomResolution( img, res );

ipi = ImagePlusImgs.floats( Intervals.dimensionsAsLongArray( outputInterval ));
BigWarpExporter.copyToImageStack( Views.raster( imgres ), ipi, 4 );

jip = ipi.getImagePlus();

jip.getCalibration().pixelWidth = res[ 0 ]
jip.getCalibration().pixelHeight = res[ 1 ]
jip.getCalibration().pixelDepth = res[ 2 ]
jip.setDimensions( 1, (int)outputInterval.dimension( 2 ), 1 );
jip.show();
