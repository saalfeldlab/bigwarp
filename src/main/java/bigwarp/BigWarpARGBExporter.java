package bigwarp;

import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;

import java.util.ArrayList;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.Views;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;

public class BigWarpARGBExporter extends BigWarpExporter<ARGBType>
{

	private Interpolation interp;

	public BigWarpARGBExporter(
			final ArrayList< SourceAndConverter< ? >> sources,
			final int[] movingSourceIndexList,
			final int[] targetSourceIndexList,
			final Interpolation interp )
	{
		super( sources, movingSourceIndexList, targetSourceIndexList, interp );
	}

	/**
	 * Returns true if moving image sources are all of the same type.
	 * 
	 * @param sources the sources
	 * @param movingSourceIndexList list of indexes for moving sources
	 * @return true if all moving sources are of the same type
	 */
	public static boolean isTypeListFullyConsistent( ArrayList< SourceAndConverter< ? >> sources, int[] movingSourceIndexList )
	{
		for ( int i = 0; i < movingSourceIndexList.length; i++ )
		{
			int idx = movingSourceIndexList[ i ];
			Object type = sources.get( idx ).getSpimSource().getType();

			if ( !type.getClass().equals( ARGBType.class ) )
				return false;
		}
		return true;
	}
	
	public ImagePlus export()
	{
		ArrayList< RandomAccessibleInterval< ARGBType > > raiList = new ArrayList< RandomAccessibleInterval< ARGBType > >(); 
		ARGBType t = null;
		
		buildTotalRenderTransform();
		
		int numChannels = movingSourceIndexList.length;
		VoxelDimensions voxdim = new FinalVoxelDimensions( "um",
				resolutionTransform.get( 0, 0 ),
				resolutionTransform.get( 1, 1 ),
				resolutionTransform.get( 2, 2 ));

		for ( int i = 0; i < numChannels; i++ )
		{
			int movingSourceIndex = movingSourceIndexList[ i ];

			RealRandomAccessible< ARGBType > convertedSource;
			convertedSource = ( RealRandomAccessible< ARGBType > ) sources.get( movingSourceIndex ).getSpimSource().getInterpolatedSource( 0, 0, interp );
			
			final RealRandomAccessible< ARGBType > raiRaw = ( RealRandomAccessible< ARGBType > )sources.get( movingSourceIndex ).getSpimSource().getInterpolatedSource( 0, 0, interp );


			// apply the transformations
//			AffineTransform3D tmpAffine = new AffineTransform3D();
			final AffineRandomAccessible< ARGBType, AffineGet > rai = RealViews.affine( 
					raiRaw, pixelRenderToPhysical.inverse() );
			
			raiList.add( Views.interval( Views.raster( rai ), outputInterval ) );
		}
		
		RandomAccessibleInterval< ARGBType > raiStack = Views.stack( raiList );
		
		ImagePlus ip = null;
		if ( isVirtual )
		{
			ip = ImageJFunctions.wrap( raiStack, "warped_moving_image" );
		}
		else if( nThreads == 1 )
		{
			ip = copyToImageStack( raiStack, raiStack );
		}
		else
		{
			System.out.println( "render with " + nThreads + " threads.");
			final ImagePlusImgFactory< ARGBType > factory = new ImagePlusImgFactory< ARGBType >( new ARGBType() );

			if ( outputInterval.numDimensions() == 3 )
			{
				// A bit of hacking to make slices the 4th dimension and
				// channels the 3rd since that's how ImagePlusImgFactory does it
				final long[] dimensions = new long[ 4 ];
				dimensions[ 0 ] = outputInterval.dimension( 0 );	// x
				dimensions[ 1 ] = outputInterval.dimension( 1 );	// y
				dimensions[ 2 ] = numChannels; 					// c
				dimensions[ 3 ] = outputInterval.dimension( 2 ); 	// z 
				FinalInterval destIntervalPerm = new FinalInterval( dimensions );
				RandomAccessibleInterval< ARGBType > img = copyToImageStack( 
						raiStack,
						destIntervalPerm, factory, nThreads );
				ip = ((ImagePlusImg<ARGBType,?>)img).getImagePlus();
			}
			else if ( outputInterval.numDimensions() == 2 )
			{
				final long[] dimensions = new long[ 4 ];
				dimensions[ 0 ] = outputInterval.dimension( 0 );	// x
				dimensions[ 1 ] = outputInterval.dimension( 1 );	// y
				dimensions[ 2 ] = numChannels; 					// c
				dimensions[ 3 ] = 1; 							// z 
				FinalInterval destIntervalPerm = new FinalInterval( dimensions );
				RandomAccessibleInterval< ARGBType > img = copyToImageStack( 
						Views.addDimension( Views.extendMirrorDouble( raiStack )),
						destIntervalPerm, factory, nThreads );
				ip = ((ImagePlusImg<ARGBType,?>)img).getImagePlus();
			}
		}

		ip.getCalibration().pixelWidth = voxdim.dimension( 0 );
		ip.getCalibration().pixelHeight = voxdim.dimension( 1 );
		ip.getCalibration().pixelDepth = voxdim.dimension( 2 );
		ip.getCalibration().setUnit( voxdim.unit() );
		
		if( offsetTransform != null )
		{
			ip.getCalibration().xOrigin = offsetTransform.get( 0, 0 );
			ip.getCalibration().yOrigin = offsetTransform.get( 1, 1 );
			ip.getCalibration().zOrigin = offsetTransform.get( 2, 2 );
		}
		
		ip.setTitle( sources.get( movingSourceIndexList[ 0 ]).getSpimSource().getName() );

		return ip;
	}

	public static ImagePlus copyToImageStack( final RandomAccessible< ARGBType > rai, final Interval itvl )
	{
		// A bit of hacking to make slices the 4th dimension and channels the 3rd
		// since that's how ImagePlusImgFactory does it
		MixedTransformView< ARGBType > raip = Views.permute( rai, 2, 3 );

		final long[] dimensions = new long[ itvl.numDimensions() ];
		for( int d = 0; d < itvl.numDimensions(); d++ )
		{
			if( d == 2 )
				dimensions[ d ] = itvl.dimension( 3 );
			else if( d == 3 )
				dimensions[ d ] = itvl.dimension( 2 );
			else
				dimensions[ d ] = itvl.dimension( d );
		}

		// create the image plus image
		final ImagePlusImgFactory< ARGBType > factory = new ImagePlusImgFactory< ARGBType >( new ARGBType() );
		final ImagePlusImg< ARGBType, ? > target = factory.create( dimensions );

		long[] dims = new long[ target.numDimensions() ];
		target.dimensions( dims );

		double k = 0;
		long N = 1;
		for ( int i = 0; i < itvl.numDimensions(); i++ )
			N *= dimensions[ i ];

		final net.imglib2.Cursor< ARGBType > c = target.cursor();

		final RandomAccess< ARGBType > ra = raip.randomAccess();
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
