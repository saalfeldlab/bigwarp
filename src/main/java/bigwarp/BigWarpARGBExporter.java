package bigwarp;

import ij.IJ;
import ij.ImagePlus;
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

public class BigWarpARGBExporter implements BigWarpExporter<ARGBType>
{
	final private ArrayList< SourceAndConverter< ? >> sources;

	final private int[] movingSourceIndexList;

	final private int[] targetSourceIndexList;

	private Interpolation interp;

	public BigWarpARGBExporter(
			final ArrayList< SourceAndConverter< ? >> sources,
			final int[] movingSourceIndexList,
			final int[] targetSourceIndexList )
	{
		this.sources = sources;
		this.movingSourceIndexList = movingSourceIndexList;
		this.targetSourceIndexList = targetSourceIndexList;
	}

	public void setInterp( Interpolation interp )
	{
		this.interp = interp;
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

	public ImagePlus exportMovingImagePlus( final boolean isVirtual )
	{
		return exportMovingImagePlus( isVirtual, 1 );
	}

	@SuppressWarnings( { "unchecked" } )
	public ImagePlus exportMovingImagePlus( final boolean isVirtual, int nThreads )
	{
		int numChannels = movingSourceIndexList.length;

		// TODO - 	this is questionable if the fixed source images are ever in different intervals
		// 			but outputting results to the space of the first one seems reasonable.
		final RandomAccessibleInterval< ? > destinterval= sources.get( targetSourceIndexList[ 0 ] ).getSpimSource().getSource( 0, 0 );
				
//		RandomAccessibleInterval< RealType< ? >> tmp = (RandomAccessibleInterval< RealType <? > > )sources.get( targetSourceIndexList[ 0 ] ).getSpimSource().getSource( 0, 0 );
		
		// go from physical space to fixed image space
		final AffineTransform3D fixedImgXfm = new AffineTransform3D();
		sources.get( targetSourceIndexList[ 0 ] ).getSpimSource().getSourceTransform( 0, 0, fixedImgXfm );
		final AffineTransform3D fixedXfmInv = fixedImgXfm.inverse(); // get to the pixel space of the fixed image
		VoxelDimensions voxdim = sources.get( targetSourceIndexList[ 0 ] ).getSpimSource().getVoxelDimensions();
		
		// TODO - 	require for now that all moving image types are the same.  
		// 		  	this is not too unreasonable, I think.		int numChannels = movingSourceIndexList.length;
		ArrayList< RandomAccessibleInterval< ARGBType > > raiList = new ArrayList< RandomAccessibleInterval< ARGBType > >(); 
		for ( int i = 0; i < numChannels; i++ )
		{
			int movingSourceIndex = movingSourceIndexList[ i ];
			final RealRandomAccessible< ARGBType > raiRaw = ( RealRandomAccessible< ARGBType > )sources.get( movingSourceIndex ).getSpimSource().getInterpolatedSource( 0, 0, interp );
			
			// go from moving to physical space
			final AffineTransform3D movingImgXfm = new AffineTransform3D();
			sources.get( movingSourceIndex ).getSpimSource().getSourceTransform( 0, 0, movingImgXfm );

			// apply the transformations
			final AffineRandomAccessible< ARGBType, AffineGet > rai = RealViews.affine( RealViews.affine( raiRaw, movingImgXfm ), fixedXfmInv );
			
			raiList.add( Views.interval( Views.raster( rai ), destinterval ) );
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
			System.out.println( "copy with " + nThreads );
			final ImagePlusImgFactory< ARGBType > factory = new ImagePlusImgFactory< ARGBType >();

			if ( destinterval.numDimensions() == 3 )
			{
				// A bit of hacking to make slices the 4th dimension and
				// channels the 3rd since that's how ImagePlusImgFactory does it

				final long[] dimensions = new long[ 4 ];
				dimensions[ 0 ] = destinterval.dimension( 0 );	// x
				dimensions[ 1 ] = destinterval.dimension( 1 );	// y
				dimensions[ 2 ] = numChannels; 					// c
				dimensions[ 3 ] = destinterval.dimension( 2 ); 	// z 
				FinalInterval destIntervalPerm = new FinalInterval( dimensions );

				RandomAccessibleInterval< ARGBType > img = BigWarpExporter.copyToImageStack( raiStack,
						destIntervalPerm, factory, nThreads );
				ip = ImageJFunctions.wrap( img, "bigwarped_image" );
			}
			else
			{
				RandomAccessibleInterval< ARGBType > img = BigWarpExporter.copyToImageStack( raiStack,
						destinterval, factory, nThreads );
				ip = ImageJFunctions.wrap( img, "bigwarped_image" );
			}
		}

		ip.getCalibration().pixelWidth = voxdim.dimension( 0 );
		ip.getCalibration().pixelHeight = voxdim.dimension( 1 );
		ip.getCalibration().pixelDepth = voxdim.dimension( 2 );
		ip.getCalibration().setUnit( voxdim.unit() );
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
		final ImagePlusImgFactory< ARGBType > factory = new ImagePlusImgFactory< ARGBType >();
		final ImagePlusImg< ARGBType, ? > target = factory.create( dimensions, new ARGBType() );

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

	public static < T extends NumericType< T > & NativeType< T > > ImagePlus copyToImageStack( final RealRandomAccessible< T > rai, final Interval itvl )
	{
		final long[] dimensions = new long[ itvl.numDimensions() ];
		itvl.dimensions( dimensions );

		// create the image plus image
		final T t = rai.realRandomAccess().get();
		final ImagePlusImgFactory< T > factory = new ImagePlusImgFactory< T >();
		final ImagePlusImg< T, ? > target = factory.create( itvl, t );

		double k = 0;
		final long N = dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ];

		final net.imglib2.Cursor< T > c = target.cursor();
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
