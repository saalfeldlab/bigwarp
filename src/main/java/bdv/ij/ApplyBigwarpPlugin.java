package bdv.ij;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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

/**
 * 
 * Apply a bigwarp transform to a 2d or 3d ImagePlus
 *
 */
public class ApplyBigwarpPlugin implements PlugIn
{
	public static void main( String[] args ) throws IOException
	{
		new ImageJ();
		new ApplyBigwarpPlugin().run( "" );
	}

	public static ImagePlus apply(
			final ImagePlus movingIp,
			final ImagePlus targetIp,
			final LandmarkTableModel landmarks,
			final Interpolation interp,
			final int nThreads )
	{
		int numChannels = movingIp.getNChannels();
		BigWarpData bwData = BigWarpInit.createBigWarpDataFromImages( movingIp, targetIp );

		BigWarpExporter< ? > exporter = null;
		ArrayList< SourceAndConverter< ? > > sources = bwData.sources;
		int[] movingSourceIndexList = bwData.movingSourceIndices;
		int[] targetSourceIndexList = bwData.targetSourceIndices;
		VoxelDimensions voxdim = sources.get( targetSourceIndexList[ 0 ] ).getSpimSource().getVoxelDimensions();

		ArrayList< SourceAndConverter< ? >> sourcesxfm = BigWarp.wrapSourcesAsTransformed(
				sources, 
				landmarks.getNumdims(),
				movingSourceIndexList );

		ThinPlateR2LogRSplineKernelTransform xfm = landmarks.getTransform();

		for ( int i = 0; i < numChannels; i++ )
		{
			InverseRealTransform irXfm = new InverseRealTransform( new TpsTransformWrapper( 3, xfm ) );
			((WarpedSource< ? >) (sourcesxfm.get( i ).getSpimSource())).updateTransform( irXfm );
			((WarpedSource< ? >) (sourcesxfm.get( i ).getSpimSource())).setIsTransformed( true );
		}

		if ( BigWarpRealExporter.isTypeListFullyConsistent( sources, movingSourceIndexList ) )
		{
			Object baseType = sourcesxfm.get( movingSourceIndexList[ 0 ] ).getSpimSource().getType();
			if ( ByteType.class.isInstance( baseType ) )
				exporter = new BigWarpRealExporter< ByteType >( sourcesxfm, movingSourceIndexList, targetSourceIndexList, interp, ( ByteType ) baseType );
			else if ( UnsignedByteType.class.isInstance( baseType ) )
				exporter = new BigWarpRealExporter< UnsignedByteType >( sourcesxfm, movingSourceIndexList, targetSourceIndexList, interp, ( UnsignedByteType ) baseType );
			else if ( IntType.class.isInstance( baseType ) )
				exporter = new BigWarpRealExporter< IntType >( sourcesxfm, movingSourceIndexList, targetSourceIndexList, interp, ( IntType ) baseType );
			else if ( UnsignedShortType.class.isInstance( baseType ) )
				exporter = new BigWarpRealExporter< UnsignedShortType >( sourcesxfm, movingSourceIndexList, targetSourceIndexList, interp, ( UnsignedShortType ) baseType );
			else if ( FloatType.class.isInstance( baseType ) )
				exporter = new BigWarpRealExporter< FloatType >( sourcesxfm, movingSourceIndexList, targetSourceIndexList, interp, ( FloatType ) baseType );
			else if ( DoubleType.class.isInstance( baseType ) )
				exporter = new BigWarpRealExporter< DoubleType >( sourcesxfm, movingSourceIndexList, targetSourceIndexList, interp, ( DoubleType ) baseType );
			else if ( ARGBType.class.isInstance( baseType ) )
				exporter = new BigWarpARGBExporter( sourcesxfm, movingSourceIndexList, targetSourceIndexList );
			else
			{
				System.err.println( "Can't export type " + baseType.getClass() );
				exporter = null;
				return null;
			}
		}

		ImagePlus warpedIp = exporter.exportMovingImagePlus( false, nThreads );

		// Note: need to get number of channels and frames from moving image
		// but get the number of slices form the target
		warpedIp.setDimensions( movingIp.getNChannels(), targetIp.getNSlices(),
				movingIp.getNFrames() );

		warpedIp.getCalibration().pixelWidth = 1.0 / voxdim.dimension( 0 );
		warpedIp.getCalibration().pixelHeight = 1.0 / voxdim.dimension( 1 );
		warpedIp.getCalibration().pixelDepth = voxdim.dimension( 2 );
		warpedIp.getCalibration().setUnit( voxdim.unit() );
		warpedIp.setTitle( movingIp.getTitle() + "_bigwarped" );

		return warpedIp;
	}

	@Override
	public void run( String arg )
	{
		if ( IJ.versionLessThan( "1.40" ) )
			return;

		final GenericDialog gd = new GenericDialog( "Apply Big Warp transform" );
		gd.addMessage( "File Selection:" );
		gd.addStringField( "landmarks_image_file", "" );
		gd.addStringField( "moving_image_file", "" );
		gd.addStringField( "target_space_file", "" );
		gd.addChoice( "interpolation", new String[]{ "Nearest Neighbor", "Linear" }, "Linear" );
		gd.addNumericField( "threads", 1, 0 );
		gd.showDialog();

		if ( gd.wasCanceled() )
			return;

		String landmarksPath = gd.getNextString();
		String movingPath = gd.getNextString();
		String targetPath = gd.getNextString();
		String interpType = gd.getNextChoice();

		ImagePlus movingIp = IJ.openImage( movingPath );
		ImagePlus targetIp = movingIp;

		if ( !targetPath.isEmpty() )
			targetIp = IJ.openImage( targetPath );

		int nd = 2;
		if ( movingIp.getNSlices() > 1 )
			nd = 3;

		LandmarkTableModel ltm = new LandmarkTableModel( nd );
		try
		{
			ltm.load( new File( landmarksPath ) );
		} catch ( IOException e )
		{
			e.printStackTrace();
			return;
		}

		Interpolation interp = Interpolation.NLINEAR;
		if( interpType.equals( "Nearest Neighbor" ))
			interp = Interpolation.NEARESTNEIGHBOR;

		int nThreads = (int)gd.getNextNumber();
		System.out.println( nThreads );

		ImagePlus warpedIp = apply( movingIp, targetIp, ltm, interp, nThreads );
		warpedIp.show();
	}

}