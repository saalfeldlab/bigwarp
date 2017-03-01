package bigwarp;

import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import bdv.img.TpsTransformWrapper;
import bdv.img.WarpedSource;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp.BigWarpData;
import bigwarp.BigWarpBatchTransformFOV.DummyImageLoader;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.loader.ImagePlusLoader;

public class BigWarpBatchTransform
{

	public static void main( String[] args ) throws IOException, FormatException
	{
		LandmarkTableModel ltm = new LandmarkTableModel( Integer.parseInt( args[ 0 ] ) );
		ltm.load( new File( args[ 1 ] ) );

		ThinPlateR2LogRSplineKernelTransform xfm = ltm.getTransform();

		String srcName = args[ 2 ];
		String template = args[ 3 ];
		String dstName = args[ 4 ];

		ImagePlus impP = IJ.openImage( srcName );

		// read image properties from the header
		ImageReader reader = new ImageReader();
		reader.setId( template );
		Hashtable< String, Object > gmeta = reader.getGlobalMetadata();
		System.out.println( gmeta );

		String[] names = new String[]{ impP.getTitle(), "target_interval" };

		/* Load the first source */
		final ImagePlusLoader loaderP = new ImagePlusLoader( impP );
		final AbstractSpimData< ? >[] spimDataP = loaderP.loadAll( 0 );
		int numMovingChannels = loaderP.numChannels();

		final AbstractSpimData< ? >[] spimDataQ = new AbstractSpimData[]{ createSpimData( gmeta ) };
		BigWarpData data = BigWarpInit.createBigWarpData( spimDataP, spimDataQ, names );

		Interpolation interpolation = Interpolation.NLINEAR;
		int[] movingSourceIndexList = new int[]{ 0 };
		int[] targetSourceIndexList = new int[]{ 1 };
		ArrayList< SourceAndConverter< ? >> sourcesxfm = BigWarp.wrapSourcesAsTransformed(
				data.sources, 
				ltm.getNumdims(),
				movingSourceIndexList );

		InverseRealTransform irXfm = new InverseRealTransform( new TpsTransformWrapper( xfm.getNumDims(), xfm )); 
		((WarpedSource< ? >) (sourcesxfm.get( 0 ).getSpimSource())).updateTransform( irXfm );
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

	public static final SpimDataMinimal createSpimData( Hashtable< String, Object > gmeta )
	{
		// get relevant metadata
		double pw = 1.0;
		double ph = 1.0;
		double pd = 1.0;

		if( gmeta.keySet().contains( "XResolution" ))
			pw = ((Double)gmeta.get("XResolution")).doubleValue();

		if( gmeta.keySet().contains( "YResolution" ))
			ph = ((Double)gmeta.get("YResolution")).doubleValue();

		if( gmeta.keySet().contains( "Spacing" ))
			pd = ((Double)gmeta.get("Spacing")).doubleValue();

		int numSetups = 1;
		int numTimepoints = 1;
		int[] ids = new int[]{ 349812342 };
		final File basePath = new File( "." );

		String punit = "px";
		final FinalVoxelDimensions voxelSize = new FinalVoxelDimensions( punit, pw, ph, pd );
		final long w = ((Long)gmeta.get("ImageWidth")).longValue();
		final long h = ((Long)gmeta.get("ImageLength")).longValue();
		final long d = Long.parseLong( (String)gmeta.get("images") );

		long[] dims = new long[]{ w, h, d };
		final FinalDimensions size = new FinalDimensions( new long[] { w, h, d } );

		// create setups from channels
		final HashMap< Integer, BasicViewSetup > setups = new HashMap< Integer, BasicViewSetup >( numSetups );
		for ( int s = 0; s < numSetups; ++s )
		{
			final BasicViewSetup setup = new BasicViewSetup( ids[ s ], String.format( "channel %d", ids[ s ] + 1 ), size, voxelSize );
			setup.setAttribute( new Channel( ids[ s ] + 1 ) );
			setups.put( ids[ s ], setup );
		}

		// create timepoints
		final ArrayList< TimePoint > timepoints = new ArrayList< TimePoint >( numTimepoints );
		for ( int t = 0; t < numTimepoints; ++t )
			timepoints.add( new TimePoint( t ) );

		// create ViewRegistrations from the images calibration
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceTransform.set( pw, 0, 0, 0, 0, ph, 0, 0, 0, 0, pd, 0 );
		final ArrayList< ViewRegistration > registrations = new ArrayList< ViewRegistration >();
		for ( int t = 0; t < numTimepoints; ++t )
			for ( int s = 0; s < numSetups; ++s )
				registrations.add( new ViewRegistration( t, ids[ s ], sourceTransform ) );

		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( new TimePoints( timepoints ), setups, 
				new DummyImageLoader< FloatType >( new FloatType(), dims ), null );

		SpimDataMinimal spimData = new SpimDataMinimal( basePath, seq, new ViewRegistrations( registrations ) );
		if ( WrapBasicImgLoader.wrapImgLoaderIfNecessary( spimData ) )
			System.err.println( "WARNING:\nOpening <SpimData> dataset that is not suited for interactive browsing.\nConsider resaving as HDF5 for better performance." );

		return spimData;
	}

}
