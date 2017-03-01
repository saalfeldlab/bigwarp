package bigwarp;

import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.janelia.utility.parse.ParseUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import net.imglib2.FinalDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ConstantUtils;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.generic.sequence.TypedBasicImgLoader;
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
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.loader.ImagePlusLoader;

public class BigWarpBatchTransformFOV
{
	private transient JCommander jCommander;

	@Parameter(names = {"--image", "-i"}, description = "Input image file" )
	private String imageFilePath;

	@Parameter(names = {"--landmarks", "-l"}, description = "Input landmarks file" )
	private String landmarkFilePath;

	@Parameter(names = {"--output", "-o"}, description = "Output image file" )
	private String outputFilePath;

	@Parameter(names = {"--dimension", "-d"}, description = "Output dimension", 
			converter = ParseUtils.LongArrayConverter.class )
	private long[] dims;

	@Parameter(names = {"--threads", "-j"}, description = "Number of threads" )
	private int nThreads = 1;

	@Parameter(names = {"--spacing", "-s"}, description = "Voxel spacing, e.g. \"0.5,0.5,2.0\"", 
			converter = ParseUtils.DoubleArrayConverter.class )
	private double[] spacing = new double[]{ 1.0 };

	@Parameter(names = {"--offset", "-f"}, description = "Offset, e.g. \"5.0,5.0,-1.0\"", 
			converter = ParseUtils.DoubleArrayConverter.class )
	private double[] offset = new double[]{ 0.0 };

	@Parameter(names = {"--help", "-h"}, help = true)
	private boolean help;

	private long[] dimsFull;
	private double[] spacingFull;
	private double[] offsetFull;

	@Parameter(names = {"--interpolation", "-p"}, description = "Interpolation Type {NLINEAR,NEARESTNEIGHBOR}" )
	private String interpType = "NLINEAR";

	public static void main( String[] args ) throws IOException
	{
		BigWarpBatchTransformFOV alg = parseCommandLineArgs( args );
		alg.process();
	}

	private void initCommander()
	{
		jCommander = new JCommander( this );
		jCommander.setProgramName( "input parser" ); 
	}

	public static BigWarpBatchTransformFOV parseCommandLineArgs( final String[] args )
	{
		BigWarpBatchTransformFOV alg = new BigWarpBatchTransformFOV();
		alg.initCommander();
		try 
		{
			alg.jCommander.parse( args );
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}

		if( alg.help )
		{
			alg.jCommander.usage();
			return alg;
		}

		int nd = alg.dims.length;
		if( alg.offset.length < 3 )
		{
			alg.offsetFull = fill( alg.offset, nd, 0.0 );
		}
		else
		{
			alg.offsetFull = alg.offset;
		}

		if( alg.spacing.length < 3 )
		{
			alg.spacingFull = fill( alg.spacing, nd, 1.0 );
		}
		else
		{
			alg.spacingFull  = alg.spacing;
		}

		if( alg.dims.length == 1 )
		{
			alg.dimsFull = new long[ 3 ];
			Arrays.fill( alg.dimsFull, alg.dims[ 0 ] );
		}
		else if( alg.dims.length == 2 )
		{
			alg.dimsFull = new long[ 3 ];
			System.arraycopy( alg.dims, 0, alg.dimsFull, 0, 2 );
			alg.dimsFull[ 2 ] = 1;
		}
		else if( alg.dims.length == 3 )
		{
			alg.dimsFull = alg.dims;
		}

		return alg;
	}

	public void process() throws IOException
	{
		if( help )
			return;

		long startTime = System.currentTimeMillis();
		int nd = dims.length;
		if( nd != 3 && nd != 2 )
		{
			System.err.println( "For 2D or 3D use only" );
			return;
		}

		LandmarkTableModel ltm = new LandmarkTableModel( nd );
		ltm.load( new File( landmarkFilePath ));
		ThinPlateR2LogRSplineKernelTransform xfm = ltm.getTransform();

		ImagePlus impP = IJ.openImage( imageFilePath );

		String[] names = new String[]{ impP.getTitle(), "target_interval" };

		/* Load the first source */
		final ImagePlusLoader loaderP = new ImagePlusLoader( impP );
		final AbstractSpimData< ? >[] spimDataP = loaderP.loadAll( 0 );
		int numMovingChannels = loaderP.numChannels();

		final AbstractSpimData< ? >[] spimDataQ = new AbstractSpimData[]{ createSpimData() };
		BigWarpData data = BigWarpInit.createBigWarpData( spimDataP, spimDataQ, names );

		Interpolation interpolation = Interpolation.valueOf( interpType );
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

		System.out.println( "exporting ");
		ImagePlus ipout = exporter.exportMovingImagePlus( false, nThreads );
		System.out.println( "saving");
		IJ.save( ipout, outputFilePath );

		long endTime = System.currentTimeMillis();
		System.out.println( "total time: " + (endTime - startTime) + " ms");
		System.exit( 0 );
	}

	public final SpimDataMinimal createSpimData()
	{
		int numSetups = 1;
		int numTimepoints = 1;
		int[] ids = new int[]{ 349812342 };
		final File basePath = new File( "." );

		double pw = spacingFull[ 0 ];
		double ph = spacingFull[ 1 ];
		double pd = spacingFull[ 2 ];

		double ox = offsetFull[ 0 ] / spacingFull[ 0 ];
		double oy = offsetFull[ 1 ] / spacingFull[ 1 ];
		double oz = offsetFull[ 2 ] / spacingFull[ 2 ];

		String punit = "px";

		final FinalVoxelDimensions voxelSize = new FinalVoxelDimensions( punit, pw, ph, pd );
		final long w = dimsFull[ 0 ];
		final long h = dimsFull[ 1 ];
		final long d = dimsFull[ 2 ];
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
		sourceTransform.set( pw, 0, 0, ox, 0, ph, 0, oy, 0, 0, pd, oz );
		final ArrayList< ViewRegistration > registrations = new ArrayList< ViewRegistration >();
		for ( int t = 0; t < numTimepoints; ++t )
			for ( int s = 0; s < numSetups; ++s )
				registrations.add( new ViewRegistration( t, ids[ s ], sourceTransform ) );

		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( new TimePoints( timepoints ), setups, 
				new DummyImageLoader< FloatType >( new FloatType(), this ), null );

		SpimDataMinimal spimData = new SpimDataMinimal( basePath, seq, new ViewRegistrations( registrations ) );
		if ( WrapBasicImgLoader.wrapImgLoaderIfNecessary( spimData ) )
			System.err.println( "WARNING:\nOpening <SpimData> dataset that is not suited for interactive browsing.\nConsider resaving as HDF5 for better performance." );

		return spimData;
	}

	public static double[] fill( double[] in, int ndim, double zVal )
	{
		double[] out = new double[ 3 ];
		if( in.length == 1 && ndim == 2 )
		{
			Arrays.fill( out, in[ 0 ] );
			out[ 2 ] = zVal;
		}
		else if( in.length == 2 && ndim == 2 )
		{
			System.arraycopy( in, 0, out, 0, 2 );
			out[ 2 ] = zVal;
		}

		return out;
	}

	public static class DummyImageLoader< T > implements TypedBasicImgLoader< T >
	{
		private final T type;
		private final long[] dim;

		public DummyImageLoader( final T type, final BigWarpBatchTransformFOV info )
		{
			this.type = type;
			this.dim = info.dims;
		}

		public DummyImageLoader( final T type, final long[] dims )
		{
			this.type = type;
			this.dim = dims;
		}

		@Override
		public BasicSetupImgLoader< T > getSetupImgLoader( int setupId )
		{
			return new BasicSetupImgLoader< T >()
			{
				@Override
				public RandomAccessibleInterval< T > getImage( int timepointId,
						ImgLoaderHint... hints )
				{
					return ConstantUtils.constantRandomAccessibleInterval( type,
							dim.length, new FinalInterval( dim ) );
				}

				@Override
				public T getImageType()
				{
					return type;
				}
			};
		}
	}
}
