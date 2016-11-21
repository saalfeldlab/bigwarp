package bigwarp;

import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import bdv.img.WarpedSource;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp.BigWarpData;
import bigwarp.landmarks.LandmarkTableModel;

public class BigWarpBatchTransform
{

	public static void main( String[] args ) throws IOException
	{
		LandmarkTableModel ltm = new LandmarkTableModel( Integer.parseInt( args[ 0 ] ) );
		ltm.load( new File( args[ 1 ] ) );

		ThinPlateR2LogRSplineKernelTransform xfm = ltm.getTransform();

		String srcName = args[ 2 ];
		String template = args[ 3 ];
		String dstName = args[ 4 ];

		ImagePlus impP = IJ.openImage( srcName );
		ImagePlus impQ = IJ.openImage( template );

		BigWarpData data = BigWarpInit.createBigWarpDataFromImages( impP, impQ );

		Interpolation interpolation = Interpolation.NLINEAR;
		int[] movingSourceIndexList = new int[]{ 0 };
		int[] targetSourceIndexList = new int[]{ 1 };
		ArrayList< SourceAndConverter< ? >> sourcesxfm = BigWarp.wrapSourcesAsTransformed(
				data.sources, 
				ltm.getNumdims(),
				movingSourceIndexList );

		((WarpedSource< ? >) (sourcesxfm.get( 0 ).getSpimSource())).updateTransform( xfm );
		((WarpedSource< ? >) (sourcesxfm.get( 0 ).getSpimSource())).setIsTransformed( true );

		BigWarpExporter< ? > exporter;
		Object baseType = sourcesxfm.get( movingSourceIndexList[ 0 ] ).getSpimSource().getType();
		if ( ByteType.class.isInstance( baseType ) )
			exporter = new BigWarpRealExporter< ByteType >( sourcesxfm,
					movingSourceIndexList, targetSourceIndexList, interpolation,
					(ByteType) baseType );
		else if ( UnsignedByteType.class.isInstance( baseType ) )
			exporter = new BigWarpRealExporter< UnsignedByteType >( sourcesxfm,
					movingSourceIndexList, targetSourceIndexList, interpolation,
					(UnsignedByteType) baseType );
		else if ( IntType.class.isInstance( baseType ) )
			exporter = new BigWarpRealExporter< IntType >( sourcesxfm, movingSourceIndexList,
					targetSourceIndexList, interpolation, (IntType) baseType );
		else if ( UnsignedShortType.class.isInstance( baseType ) )
			exporter = new BigWarpRealExporter< UnsignedShortType >( sourcesxfm,
					movingSourceIndexList, targetSourceIndexList, interpolation,
					(UnsignedShortType) baseType );
		else if ( FloatType.class.isInstance( baseType ) )
			exporter = new BigWarpRealExporter< FloatType >( sourcesxfm,
					movingSourceIndexList, targetSourceIndexList, interpolation,
					(FloatType) baseType );
		else if ( DoubleType.class.isInstance( baseType ) )
			exporter = new BigWarpRealExporter< DoubleType >( sourcesxfm,
					movingSourceIndexList, targetSourceIndexList, interpolation,
					(DoubleType) baseType );
		else if ( ARGBType.class.isInstance( baseType ) )
			exporter = new BigWarpARGBExporter( sourcesxfm,
					movingSourceIndexList, targetSourceIndexList );
		else
		{
			System.err.println( "Can't export type " + baseType.getClass() );
			exporter = null;
		}

		ImagePlus ipout = exporter.exportMovingImagePlus( false );

		IJ.save( ipout, dstName );

	}

}
