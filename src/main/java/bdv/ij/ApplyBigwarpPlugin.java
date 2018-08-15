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
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;

/**
 * 
 * Apply a bigwarp transform to a 2d or 3d ImagePlus
 *
 */
public class ApplyBigwarpPlugin implements PlugIn
{
	public static final String TARGET = "Target";
	public static final String MOVING = "Moving";
	public static final String MOVING_WARPED = "Moving (warped)";
	public static final String SPECIFIED = "Specified";
	
	public static void main( String[] args ) throws IOException
	{
		new ImageJ();
		new ApplyBigwarpPlugin().run( "" );
	}
	
	
	public static boolean validateInput(
			final ImagePlus movingIp,
			final ImagePlus targetIp,
			final LandmarkTableModel landmarks,
			final String fieldOfViewOption,
			final String resolutionOption,
			final double[] resolutionSpec,
			final double[] fovSpec,
			final double[] offsetSpec,
			final Interpolation interp,
			final boolean isVirtual,
			final int nThreads )
	{
		// TODO finish this

		// there's no target image
		if( targetIp == movingIp )
		{
			if( fieldOfViewOption.equals( TARGET ) )
			{
				return false;
			}

			if( resolutionOption.equals( TARGET ) )
			{
				return false;
			}
		}
		return true;
	}
	
	public static double[] getResolution(
			final ImagePlus movingIp,
			final ImagePlus targetIp,
			final String resolutionOption,
			final double[] resolutionSpec )
	{
		
		if( resolutionOption.equals( TARGET ))
		{
			if( ( targetIp == null ) )
			{
				System.err.println("Requested target resolution but target image is missing.");
				return null;
			}

			double[] res = new double[ 3 ];
			res[ 0 ] = targetIp.getCalibration().pixelWidth;
			res[ 1 ] = targetIp.getCalibration().pixelHeight;
			res[ 2 ] = targetIp.getCalibration().pixelDepth;
			return res;
		}
		else if( resolutionOption.equals( MOVING ))
		{
			if( ( movingIp == null ) )
			{
				System.err.println("Requested moving resolution but moving image is missing.");
				return null;
			}
			
			double[] res = new double[ 3 ];
			res[ 0 ] = movingIp.getCalibration().pixelWidth;
			res[ 1 ] = movingIp.getCalibration().pixelHeight;
			res[ 2 ] = movingIp.getCalibration().pixelDepth;
			return res;
		}
		else if( resolutionOption.equals( SPECIFIED ))
		{
			if( ( resolutionSpec == null ) )
			{
				System.err.println("Requested moving resolution but moving image is missing.");
				return null;
			}
			
			double[] res = new double[ 3 ];
			System.arraycopy( resolutionSpec, 0, res, 0, resolutionSpec.length );
			return res;
		}
		System.err.println("Invalid resolution option: " + resolutionOption );
		return null;
	}
	
	public static Interval getPixelInterval(
			final ImagePlus movingIp,
			final ImagePlus targetIp,
			final LandmarkTableModel landmarks,
			final String fieldOfViewOption,
			final double[] fovSpec,
			final double[] offsetSpec,
			final double[] outputResolution )
	{
		if( fieldOfViewOption.equals( TARGET ))
		{
			if ( (targetIp == null) )
			{
				System.err.println("Requested target fov but target image is missing.");
				return null;
			}

			return new FinalInterval( (long)targetIp.getWidth(), (long)targetIp.getHeight(), (long) targetIp.getNSlices() );
		}
		else if( fieldOfViewOption.equals( MOVING_WARPED ))
		{
			ThinPlateR2LogRSplineKernelTransform tps = landmarks.getTransform();

			AffineTransform movingPixelToPhysical = new AffineTransform( tps.getNumDims() );
			movingPixelToPhysical.set( movingIp.getCalibration().pixelWidth, 0, 0 );
			movingPixelToPhysical.set( movingIp.getCalibration().pixelHeight, 1, 1 );
			if( tps.getNumDims() > 2 )
				movingPixelToPhysical.set( movingIp.getCalibration().pixelDepth, 2, 2 );

			AffineTransform outputResolution2Pixel = new AffineTransform( tps.getNumDims() );
			outputResolution2Pixel.set( outputResolution[ 0 ], 0, 0 );
			outputResolution2Pixel.set( outputResolution[ 1 ], 1, 1  );
			if( tps.getNumDims() > 2 )
				outputResolution2Pixel.set( outputResolution[ 2 ], 2, 2  );
			
			RealTransformSequence seq = new RealTransformSequence();
			seq.add( movingPixelToPhysical );
			seq.add( new TpsTransformWrapper( tps.getNumDims(), tps ) );
			seq.add( outputResolution2Pixel.inverse() );
			
			FinalInterval interval = new FinalInterval( (long)movingIp.getWidth(), 
					(long)movingIp.getHeight(), (long)movingIp.getNSlices() );

			return BigWarpExporter.estimateBounds( seq, interval );
		}
		else if( fieldOfViewOption.equals( SPECIFIED ) )
		{
			if( fovSpec.length == 2 )
			{
				return new FinalInterval( 
						(long)Math.ceil( fovSpec[ 0 ] ),
						(long)Math.ceil( fovSpec[ 1 ] ));
			}
			else if( fovSpec.length == 3 )
			{
				return new FinalInterval( 
						(long)Math.ceil( fovSpec[ 0 ] ),
						(long)Math.ceil( fovSpec[ 1 ] ),
						(long)Math.ceil( fovSpec[ 2 ] ));
			}
			else
			{
				System.out.println("Invalid fov spec, length : " + fovSpec.length );
			}
		}
		System.err.println("Invalid field of view option: ( " + fieldOfViewOption + " )" );
		return null;
	}

	public static double[] getOffset( 
			final String fieldOfViewOption,
			final double[] offsetSpec, 
			final Interval outputInterval ) 
	{
		double[] offset = new double[ 3 ];
		if( fieldOfViewOption.equals( SPECIFIED ) )
		{
			System.arraycopy( offsetSpec, 0, offset, 0, offset.length );
			return offset;
		}

		for( int d = 0; d < outputInterval.numDimensions(); d++ )
		{
			offset[ d ] = outputInterval.realMin( d );
		}

		return offset;
	}
	
	public static ImagePlus apply(
			final ImagePlus movingIp,
			final ImagePlus targetIp,
			final LandmarkTableModel landmarks,
			final String fieldOfViewOption,
			final String resolutionOption,
			final double[] resolutionSpec,
			final double[] fovSpec,
			final double[] offsetSpec,
			final Interpolation interp,
			final boolean isVirtual,
			final int nThreads )
	{		
		if( !validateInput( movingIp, targetIp, landmarks,
				fieldOfViewOption, resolutionOption, resolutionSpec,
				fovSpec, offsetSpec, interp, isVirtual, nThreads ) )
			return null;

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
				exporter = new BigWarpARGBExporter( sourcesxfm, movingSourceIndexList, targetSourceIndexList, interp );
			else
			{
				System.err.println( "Can't export type " + baseType.getClass() );
				exporter = null;
				return null;
			}
		}
		
		double[] res = getResolution( movingIp, targetIp, resolutionOption, resolutionSpec );
		Interval outputInterval = getPixelInterval( movingIp, targetIp, landmarks, fieldOfViewOption, 
				fovSpec, offsetSpec, res );
		double[] offset = getOffset(fieldOfViewOption, offsetSpec, outputInterval );

		System.out.println( "output interval : " + Util.printInterval( outputInterval ));

		exporter.setRenderResolution( res );
		exporter.setInterval( outputInterval );
		exporter.setOffset( offset );
		exporter.setVirtual( isVirtual );
		exporter.setNumThreads( nThreads );
		
		ImagePlus warpedIp = exporter.export();

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
		gd.addStringField( "landmarks_image_file", "/groups/saalfeld/home/bogovicj/tmp/mri-stack_p2p2p4_landmarks_2.csv" );
		gd.addStringField( "moving_image_file", "/groups/saalfeld/home/bogovicj/tmp/mri-stack_p2p2p4.tif" );
		gd.addStringField( "target_space_file", "" );
		
		gd.addMessage( "Field of view and resolution:" );
		gd.addChoice( "Field of view", 
				new String[]{ TARGET, MOVING_WARPED, "Points", SPECIFIED },
				MOVING_WARPED );

		gd.addChoice( "Resolution", 
				new String[]{ TARGET, MOVING, SPECIFIED },
				MOVING );
		
		gd.addMessage( "Resolution in physical units");
		gd.addNumericField( "x", 1.0, 4 );
		gd.addNumericField( "y", 1.0, 4 );
		gd.addNumericField( "z", 1.0, 4 );
		
		gd.addMessage( "Offset in physical units");
		gd.addNumericField( "x", 0.0, 4 );
		gd.addNumericField( "y", 0.0, 4 );
		gd.addNumericField( "z", 0.0, 4 );
		
		gd.addMessage( "Field of view (pixels)");
		gd.addNumericField( "x", -1, 0 );
		gd.addNumericField( "y", -1, 0 );
		gd.addNumericField( "z", -1, 0 );
		
		gd.addMessage( "Output options");
		gd.addChoice( "Interpolation", new String[]{ "Nearest Neighbor", "Linear" }, "Linear" );
		gd.addCheckbox( "virtual?", false );
		gd.addNumericField( "threads", 1, 0 );

		gd.showDialog();

		if ( gd.wasCanceled() )
			return;

		String landmarksPath = gd.getNextString();
		String movingPath = gd.getNextString();
		String targetPath = gd.getNextString();
		
		String fovOption = gd.getNextChoice();
		String resOption = gd.getNextChoice();
		
		double[] resolutions = new double[ 3 ];
		resolutions[ 0 ] = gd.getNextNumber();
		resolutions[ 1 ] = gd.getNextNumber();
		resolutions[ 2 ] = gd.getNextNumber();
		
		double[] offset = new double[ 3 ];
		offset[ 0 ] = gd.getNextNumber();
		offset[ 1 ] = gd.getNextNumber();
		offset[ 2 ] = gd.getNextNumber();
		
		double[] fov = new double[ 3 ];
		fov[ 0 ] = gd.getNextNumber();
		fov[ 1 ] = gd.getNextNumber();
		fov[ 2 ] = gd.getNextNumber();

		String interpType = gd.getNextChoice();
		boolean isVirtual = gd.getNextBoolean();
		int nThreads = (int)gd.getNextNumber();

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
		
		System.out.println( nThreads );

		ImagePlus warpedIp = apply( movingIp, targetIp, ltm,
				fovOption, resOption,
				resolutions, fov, offset,
				interp, isVirtual, nThreads );

		warpedIp.show();
	}

}
