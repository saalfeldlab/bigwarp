// @File(label="Landmark file") landmarksPath
// @ImagePlus(label="Moving image") movingIp
// @ImagePlus(label="Target image") targetIp
// @String(label="Interpolation", choices={"Linear", "Nearest Neighbor"}) interpType
// @Integer(label="Number of threads", min=1, max=64, value=1) nThreads

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import bdv.ij.ApplyBigwarpPlugin;
import bdv.img.TpsTransformWrapper;
import bdv.img.WarpedSource;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp.BigWarpData;
import bigwarp.BigWarp;
import bigwarp.BigWarpARGBExporter;
import bigwarp.BigWarpExporter;
import bigwarp.BigWarpInit;
import bigwarp.BigWarpRealExporter;
import bigwarp.landmarks.LandmarkTableModel;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

println landmarksPath

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

ImagePlus warpedIp = ApplyBigwarpPlugin.apply( movingIp, targetIp, ltm, interp, nThreads );
warpedIp.show();
