/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2021 Howard Hughes Medical Institute.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package bigwarp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.janelia.utility.parse.ParseUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import bdv.export.ProgressWriter;
import bdv.export.ProgressWriterConsole;
import bdv.img.WarpedSource;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp.BigWarpData;
import bigwarp.BigWarpExporter.ExportThread;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.loader.ImagePlusLoader;
import ij.IJ;
import ij.ImagePlus;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import loci.formats.FormatException;
import loci.formats.in.TiffReader;
import loci.plugins.BF;
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
import net.imglib2.FinalDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ConstantUtils;

public class BigWarpBatchTransformFOV
{
	private transient JCommander jCommander;

	@Parameter(names = {"--image", "-i"}, description = "Input image file" )
	private String imageFilePath;

	@Parameter(names = {"--landmarks", "-l"}, description = "Input landmarks file" )
	private String landmarkFilePath;

	@Parameter(names = {"--output", "-o"}, description = "Output image file" )
	private String outputFilePath;

	@Parameter(names = {"--dimension", "-d"}, description = "Dimension of output image (overrides target image)", 
			converter = ParseUtils.LongArrayConverter.class )
	private long[] dims;

	@Parameter(names = {"--target", "-t"}, description = "Path to reference (target) image" )
	private String referenceImagePath;

	@Parameter(names = {"--threads", "-j"}, description = "Number of threads" )
	private int nThreads = 1;

	@Parameter(names = {"--out-spacing", "-s"}, description = "Output voxel spacing, e.g. \"0.5,0.5,2.0\" (overrides target image)", 
			converter = ParseUtils.DoubleArrayConverter.class )
	private double[] spacing;

	@Parameter(names = { "--in-spacing" }, description = "Input voxel spacing (overrides image metadata)", 
			converter = ParseUtils.DoubleArrayConverter.class )
	private double[] input_spacing;

	@Parameter(names = {"--offset", "-f"}, description = "Offset, e.g. \"5.0,5.0,-1.0\" (overrides target image)", 
			converter = ParseUtils.DoubleArrayConverter.class )
	private double[] offset;

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

		if ( alg.referenceImagePath != null && !alg.referenceImagePath.isEmpty() )
		{

			if ( alg.referenceImagePath.endsWith( "tif" ) || alg.referenceImagePath.endsWith( "tiff" ) || alg.referenceImagePath.endsWith( "TIF" ) || alg.referenceImagePath.endsWith( "TIFF" ) )
			{
				TiffReader reader = new TiffReader();
				try
				{
					reader.setId( alg.referenceImagePath );
					Hashtable< String, Object > meta = reader.getGlobalMetadata();
					alg.dimsFull = new long[ 3 ];
					alg.dimsFull[ 0 ] = reader.getSizeX();
					alg.dimsFull[ 1 ] = reader.getSizeY();
					alg.dimsFull[ 2 ] = reader.getSizeZ();

					alg.spacingFull = new double[ 3 ];
					alg.spacingFull[ 0 ] = 1 / ( double ) meta.get( "XResolution" );
					alg.spacingFull[ 1 ] = 1 / ( double ) meta.get( "YResolution" );
					alg.spacingFull[ 2 ] = ( double ) meta.get( "Spacing" );

					alg.offsetFull = new double[ 3 ];
					reader.close();

				}
				catch ( FormatException | IOException e )
				{
					alg.dimsFull = null;
					e.printStackTrace();
				}

			}
		}

		int nd = 3;
		if( alg.offset != null )
		{
			nd = alg.offset.length;
			if( alg.offset.length < 3 )
			{
				alg.offsetFull = fill( alg.offset, nd );
			}
			else
			{
				alg.offsetFull = alg.offset;
			}
		}

		if( alg.offsetFull == null )
		{
			alg.offsetFull = new double[ 3 ];
		}

		if( alg.spacing != null )
		{
			if( alg.spacing.length < 3 )
			{
				alg.spacingFull = fill( alg.spacing, nd );
			}
			else
			{
				alg.spacingFull  = alg.spacing;
			}
		}

		if( alg.spacingFull == null )
		{
			alg.spacingFull = new double[ 3 ];
			Arrays.fill( alg.spacingFull, 1.0 );
		}

		if( alg.dims != null )
		{
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
		}

		return alg;
	}

	public void process() throws IOException
	{
		if( help )
			return;

		long startTime = System.currentTimeMillis();
		int nd = dimsFull.length;
		if( nd != 3 && nd != 2 )
		{
			System.err.println( "For 2D or 3D use only" );
			return;
		}

		LandmarkTableModel ltm = new LandmarkTableModel( nd );
		ltm.load( new File( landmarkFilePath ));

		ImagePlus impP = null;
		try
		{
			impP = IJ.openImage( imageFilePath );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}

		if ( impP == null )
		{
			try
			{
				impP = BF.openImagePlus( imageFilePath )[ 0 ];
			}
			catch ( Exception e )
			{
				e.printStackTrace();
			}
		}

		if ( impP == null )
		{
			System.err.println( "FAILED TO READ IMAGE FROM: " + imageFilePath );
		}

		if( input_spacing != null )
		{
			System.out.println( "overriding input resolution: " + Arrays.toString( input_spacing ));
			impP.getCalibration().pixelWidth = input_spacing[ 0 ];
			impP.getCalibration().pixelHeight = input_spacing[ 1 ];
			impP.getCalibration().pixelDepth = input_spacing[ 2 ];
		}

		/* Load the first source */
		final ImagePlusLoader loaderP = new ImagePlusLoader( impP );
		final AbstractSpimData< ? >[] spimDataP = loaderP.loadAll( 0 );
		int numMovingChannels = loaderP.numChannels();

		final AbstractSpimData< ? >[] spimDataQ = new AbstractSpimData[]{ createSpimData() };

		final BigWarpExporter< ? > exporter = applyBigWarpHelper( spimDataP, spimDataQ, impP, ltm, Interpolation.valueOf( interpType ) );
		exporter.setNumThreads( nThreads );
		exporter.setVirtual( false );
		exporter.setInterval( new FinalInterval( dimsFull ));
		exporter.setRenderResolution( spacingFull );
		exporter.setOffset( offsetFull );
		exporter.setInterp( Interpolation.valueOf( interpType ));
		exporter.showResult( false );

		exporter.exportThread = new ExportThread( exporter, true );
		exporter.exportThread.run();
		final ImagePlus ipout = exporter.getResult();

		System.out.println( "saving" );
		IJ.save( ipout, outputFilePath );

		long endTime = System.currentTimeMillis();
		System.out.println( "total time: " + ( endTime - startTime ) + " ms" );
		System.exit( 0 );
	}

	public static < T > BigWarpExporter< T > applyBigWarpHelper( AbstractSpimData< ? >[] spimDataP, AbstractSpimData< ? >[] spimDataQ,
			ImagePlus impP, LandmarkTableModel ltm, Interpolation interpolation )
	{
		String[] names = generateNames( impP );
		BigWarpData data = BigWarpInit.createBigWarpData( spimDataP, spimDataQ, names );

		int numChannels = impP.getNChannels();
		int[] movingSourceIndexList = new int[ numChannels ];
		for ( int i = 0; i < numChannels; i++ )
		{
			movingSourceIndexList[ i ] = i;
		}
		
		int[] targetSourceIndexList = data.targetSourceIndices;
		
		@SuppressWarnings("unchecked")
		List< SourceAndConverter< T >> sourcesxfm = BigWarp.wrapSourcesAsTransformed(
				data.sources, 
				ltm.getNumdims(),
				data );

		ThinPlateR2LogRSplineKernelTransform xfm = ltm.getTransform();

		for ( int i = 0; i < numChannels; i++ )
		{
			WrappedIterativeInvertibleRealTransform irXfm = new WrappedIterativeInvertibleRealTransform<>( new ThinplateSplineTransform( xfm ));
			((WarpedSource< ? >) (sourcesxfm.get( i ).getSpimSource())).updateTransform( irXfm.copy() );
			((WarpedSource< ? >) (sourcesxfm.get( i ).getSpimSource())).setIsTransformed( true );
		}
		
		ProgressWriter progressWriter = new ProgressWriterConsole();

		
		BigWarpExporter< ? > exporter = null;
//		Object baseType = sourcesxfm.get( movingSourceIndexList[ 0 ] ).getSpimSource().getType();
//		if ( ByteType.class.isInstance( baseType ) )
//			exporter = new BigWarpRealExporter< ByteType >( sourcesxfm,
//					movingSourceIndexList, targetSourceIndexList, interpolation,
//					(ByteType) baseType, progressWriter );
//		else if ( UnsignedByteType.class.isInstance( baseType ) )
//			exporter = new BigWarpRealExporter< UnsignedByteType >( sourcesxfm,
//					movingSourceIndexList, targetSourceIndexList, interpolation,
//					(UnsignedByteType) baseType, progressWriter );
//		else if ( IntType.class.isInstance( baseType ) )
//			exporter = new BigWarpRealExporter< IntType >( sourcesxfm, movingSourceIndexList,
//					targetSourceIndexList, interpolation, (IntType) baseType, progressWriter );
//		else if ( UnsignedShortType.class.isInstance( baseType ) )
//			exporter = new BigWarpRealExporter< UnsignedShortType >( sourcesxfm,
//					movingSourceIndexList, targetSourceIndexList, interpolation,
//					(UnsignedShortType) baseType, progressWriter );
//		else if ( FloatType.class.isInstance( baseType ) )
//			exporter = new BigWarpRealExporter< FloatType >( sourcesxfm,
//					movingSourceIndexList, targetSourceIndexList, interpolation,
//					(FloatType) baseType, progressWriter );
//		else if ( DoubleType.class.isInstance( baseType ) )
//			exporter = new BigWarpRealExporter< DoubleType >( sourcesxfm,
//					movingSourceIndexList, targetSourceIndexList, interpolation,
//					(DoubleType) baseType, progressWriter );
//		else if ( ARGBType.class.isInstance( baseType ) )
//			exporter = new BigWarpARGBExporter( sourcesxfm,
//					movingSourceIndexList, targetSourceIndexList, interpolation, progressWriter );
//		else
//		{
//			System.err.println( "Can't export type " + baseType.getClass() );
//			exporter = null;
//		}
//		return exporter;
		return null;
	}


	public static String[] generateNames( ImagePlus imp )
	{
		String[] namesWithTarget = new String[ imp.getNChannels() + 1 ];
		String[] names = BigWarpInit.namesFromImagePlus(imp);
		
		for(int i = 0; i < names.length; i ++)
		{
			namesWithTarget[i] = names[i];
		}
		
		namesWithTarget[ imp.getNChannels() ] = "target_interval";
		return namesWithTarget;
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

	public static double[] fill( double[] in, int ndim )
	{
		double[] out = new double[ 3 ];
		if ( in.length == 1 )
		{
			Arrays.fill( out, in[ 0 ] );
		}
		else if ( in.length >= ndim )
		{
			System.arraycopy( in, 0, out, 0, 2 );
		}
		else
		{
			System.err.println( "Array length is less than dimensions!" );
			return null;
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
					return ConstantUtils.constantRandomAccessibleInterval( type, new FinalInterval( dim ) );
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
