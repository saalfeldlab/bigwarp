package bigwarp;

import java.util.ArrayList;
import java.util.List;

import bdv.export.ProgressWriter;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.ConverterSetups;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class BigWarpRealExporter< T extends RealType< T > & NativeType< T >  > extends BigWarpExporter<T>
{

	final private T baseType;

	public BigWarpRealExporter(
			final List< SourceAndConverter< T >> sources,
			final List< ConverterSetup > convSetups,
			final int[] movingSourceIndexList,
			final int[] targetSourceIndexList,
			final Interpolation interp,
			final T baseType,
			final boolean needConversion,
			final ProgressWriter progress )
	{
		super( sources, convSetups, movingSourceIndexList, targetSourceIndexList, interp, progress );
		this.baseType = baseType;
	}

	public BigWarpRealExporter(
			final List< SourceAndConverter< T >> sources,
			final List< ConverterSetup > convSetups,
			final int[] movingSourceIndexList,
			final int[] targetSourceIndexList,
			final Interpolation interp,
			final T baseType,
			final ProgressWriter progress )
	{
		this( sources, convSetups, movingSourceIndexList, targetSourceIndexList, interp, baseType, false, progress );
	}

	/**
	 * Returns true if moving image sources are all of the same type.
	 * 
	 * @param sources the sources
	 * @param <T> the type
	 * @param movingSourceIndexList list of indexes for moving sources
	 * @return true if all moving sources are of the same type
	 */
	public static <T> boolean isTypeListFullyConsistent( List< SourceAndConverter< T >> sources, int[] movingSourceIndexList )
	{
		Object baseType = sources.get( movingSourceIndexList[ 0 ] ).getSpimSource().getType();

		for ( int i = 1; i < movingSourceIndexList.length; i++ )
		{
			int idx = movingSourceIndexList[ i ];
			Object type = sources.get( idx ).getSpimSource().getType();

			if ( !baseType.getClass().equals( type.getClass() ) )
				return false;
		}

		return true;
	}
	
	@Override
	public RandomAccessibleInterval< T > exportRai()
	{
		ArrayList< RandomAccessibleInterval< T > > raiList = new ArrayList< RandomAccessibleInterval< T > >(); 
		
		buildTotalRenderTransform();
		//System.out.println( "pixelRenderToPhysical : " + pixelRenderToPhysical );
		
		int numChannels = movingSourceIndexList.length;
		VoxelDimensions voxdim = new FinalVoxelDimensions( unit,
				resolutionTransform.get( 0, 0 ),
				resolutionTransform.get( 1, 1 ),
				resolutionTransform.get( 2, 2 ));

		for ( int i = 0; i < numChannels; i++ )
		{
			final int movingSourceIndex = movingSourceIndexList[ i ];
			final RealRandomAccessible< T > raiRaw = ( RealRandomAccessible< T > )sources.get( movingSourceIndex ).getSpimSource().getInterpolatedSource( 0, 0, interp );

			// apply the transformations
			final AffineRandomAccessible< T, AffineGet > rai = RealViews.affine( 
					raiRaw, pixelRenderToPhysical.inverse() );
			
			raiList.add( Views.interval( Views.raster( rai ), outputInterval ) );
		}
		RandomAccessibleInterval< T > raiStack = Views.stack( raiList );

		return raiStack;
	}
	
	@Override
	public boolean isRGB()
	{
		return false;
	}

	@SuppressWarnings("unchecked")
	public ImagePlus export()
	{
		int numChannels = movingSourceIndexList.length;
		RandomAccessibleInterval< T > raiStack = exportRai();
		
		VoxelDimensions voxdim = new FinalVoxelDimensions( unit,
				resolutionTransform.get( 0, 0 ),
				resolutionTransform.get( 1, 1 ),
				resolutionTransform.get( 2, 2 ));

		ImagePlus ip = null;
		if ( isVirtual )
		{
			ip = ImageJFunctions.wrap(
					Views.translateInverse( raiStack, Intervals.minAsLongArray( raiStack )),
					"warped_moving_image" );

			ip.setDimensions( numChannels, (int)raiStack.dimension( 2 ), 1 );
		}
		else if( nThreads == 1 )
		{
			ip = copyToImageStack( raiStack, raiStack, progress );
		}
		else
		{
			System.out.println( "render with " + nThreads + " threads.");
			final ImagePlusImgFactory< T > factory = new ImagePlusImgFactory< T >( baseType );

			if ( outputInterval.numDimensions() == 3 )
			{
				// A bit of hacking to make slices the 4th dimension and
				// channels the 3rd since that's how ImagePlusImgFactory does it
				final long[] dimensions = new long[ 4 ];
				dimensions[ 0 ] = outputInterval.dimension( 0 );	// x
				dimensions[ 1 ] = outputInterval.dimension( 1 );	// y
				dimensions[ 2 ] = numChannels; 						// c
				dimensions[ 3 ] = outputInterval.dimension( 2 ); 	// z 
				FinalInterval destIntervalPerm = new FinalInterval( dimensions );
				RandomAccessibleInterval< T > img = copyToImageStack( 
						raiStack,
						destIntervalPerm, factory, nThreads );
				ip = ((ImagePlusImg<T,?>)img).getImagePlus();
			}
			else if ( outputInterval.numDimensions() == 2 )
			{
				final long[] dimensions = new long[ 4 ];
				dimensions[ 0 ] = outputInterval.dimension( 0 );	// x
				dimensions[ 1 ] = outputInterval.dimension( 1 );	// y
				dimensions[ 2 ] = numChannels; 						// c
				dimensions[ 3 ] = 1; 								// z 
				FinalInterval destIntervalPerm = new FinalInterval( dimensions );
				RandomAccessibleInterval< T > img = copyToImageStack( 
						Views.addDimension( Views.extendMirrorDouble( raiStack )),
						destIntervalPerm, factory, nThreads );
				ip = ((ImagePlusImg<T,?>)img).getImagePlus();
			}
		}

		ip.getCalibration().pixelWidth = voxdim.dimension( 0 );
		ip.getCalibration().pixelHeight = voxdim.dimension( 1 );
		ip.getCalibration().pixelDepth = voxdim.dimension( 2 );
		ip.getCalibration().setUnit( voxdim.unit() );
		
		if( offsetTransform != null )
		{
			ip.getCalibration().xOrigin = offsetTransform.get( 0, 3 );
			ip.getCalibration().yOrigin = offsetTransform.get( 1, 3 );
			ip.getCalibration().zOrigin = offsetTransform.get( 2, 3 );
		}
		
		ip.setTitle( sources.get( movingSourceIndexList[ 0 ]).getSpimSource().getName() + nameSuffix );

		return ip;
	}

	public static < T extends NumericType< T > & NativeType< T > > ImagePlus copyToImageStack( final RandomAccessible< T > rai, final Interval itvl,
			ProgressWriter progress )
	{
		// A bit of hacking to make slices the 4th dimension and channels the 3rd
		// since that's how ImagePlusImgFactory does it
		RandomAccessible< T > raip;
		if ( rai.numDimensions() > 3 )
			raip = Views.permute( rai, 2, 3 );
		else
			raip = rai;

		final long[] dimensions = new long[ itvl.numDimensions() ];
		for( int d = 0; d < itvl.numDimensions(); d++ )
		{
			if ( d == 2 && itvl.numDimensions() > 3 )
				dimensions[ d ] = itvl.dimension( 3 );
			else if ( d == 3 && itvl.numDimensions() > 3 )
				dimensions[ d ] = itvl.dimension( 2 );
			else
				dimensions[ d ] = itvl.dimension( d );
		}

		// create the image plus image
		final T t = rai.randomAccess().get();
		final ImagePlusImgFactory< T > factory = new ImagePlusImgFactory< T >( t );
		final ImagePlusImg< T, ? > target = factory.create( dimensions );

		long[] dims = new long[ target.numDimensions() ];
		target.dimensions( dims );

		long N = Intervals.numElements(itvl);
		final Cursor< T > c = target.cursor();
		final RandomAccess< T > ra = raip.randomAccess();
		double k = 0;
		while ( c.hasNext() )
		{
			c.fwd();
			ra.setPosition( c );
			c.get().set( ra.get() );

			if ( k % 10000 == 0 )
				progress.setProgress( k / N );

			k++;
		}

		progress.setProgress( 1.0 );
		try
		{
			return target.getImagePlus();
		}
		catch ( final ImgLibException e )
		{
			e.printStackTrace();
		}

		return null;
	}

	public static < T extends NumericType< T > & NativeType< T > > ImagePlus copyToImageStack( final RealRandomAccessible< T > rai, final Interval itvl )
	{
		final long[] dimensions = new long[ itvl.numDimensions() ];
		itvl.dimensions( dimensions );

		// create the image plus image
		final T t = rai.realRandomAccess().get();
		final ImagePlusImgFactory< T > factory = new ImagePlusImgFactory< T >( t );
		final ImagePlusImg< T, ? > target = factory.create( itvl );

		double k = 0;
		final long N = dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ];

		final Cursor< T > c = target.cursor();
		final RealRandomAccess< T > ra = rai.realRandomAccess();
		while ( c.hasNext() )
		{
			c.fwd();
			ra.setPosition( c );
			c.get().set( ra.get() );

			if ( k % 10000 == 0 )
			{
				IJ.showProgress( k / N );
			}
			k++;
		}

		IJ.showProgress( 1.1 );
		try
		{
			return target.getImagePlus();
		}
		catch ( final ImgLibException e )
		{
			e.printStackTrace();
		}

		return null;
	}

}
