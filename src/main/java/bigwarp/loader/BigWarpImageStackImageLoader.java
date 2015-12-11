package bigwarp.loader;

import ij.ImagePlus;

import java.util.ArrayList;
import java.util.HashMap;

import bdv.ij.export.imgloader.ImagePlusImgLoader.SetupImgLoader;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.generic.sequence.TypedBasicImgLoader;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

public abstract class BigWarpImageStackImageLoader< T extends NumericType< T > & NativeType< T >, A extends ArrayDataAccess< A > > implements BasicImgLoader, TypedBasicImgLoader< T >
{
	@SuppressWarnings( "unchecked" )
	public static BigWarpImageStackImageLoader< UnsignedByteType, ByteArray > createUnsignedByteInstance( final ImagePlus imp, int[] ids )
	{
		return new BigWarpImageStackImageLoader< UnsignedByteType, ByteArray >( new UnsignedByteType(), imp, ids )
		{
			@Override
			protected ByteArray wrapPixels( final Object array )
			{
				return new ByteArray( ( byte[] ) array );
			}

			@Override
			protected void linkType( final PlanarImg< UnsignedByteType, ByteArray > img )
			{
				img.setLinkedType( new UnsignedByteType( img ) );
			}
		};
	}

	@SuppressWarnings( "unchecked" )
	public static BigWarpImageStackImageLoader< UnsignedShortType, ShortArray > createUnsignedShortInstance( final ImagePlus imp, int[] ids )
	{
		return new BigWarpImageStackImageLoader< UnsignedShortType, ShortArray >( new UnsignedShortType(), imp, ids )
		{
			@Override
			protected ShortArray wrapPixels( final Object array )
			{
				return new ShortArray( ( short[] ) array );
			}

			@Override
			protected void linkType( final PlanarImg< UnsignedShortType, ShortArray > img )
			{
				img.setLinkedType( new UnsignedShortType( img ) );
			}
		};
	}

	@SuppressWarnings( "unchecked" )
	public static BigWarpImageStackImageLoader< FloatType, FloatArray > createFloatInstance( final ImagePlus imp, int[] ids )
	{
		return new BigWarpImageStackImageLoader< FloatType, FloatArray >( new FloatType(), imp, ids )
		{
			@Override
			protected FloatArray wrapPixels( final Object array )
			{
				return new FloatArray( ( float[] ) array );
			}

			@Override
			protected void linkType( final PlanarImg< FloatType, FloatArray > img )
			{
				img.setLinkedType( new FloatType( img ) );
			}
		};
	}

	@SuppressWarnings( "unchecked" )
	public static BigWarpImageStackImageLoader< ARGBType, IntArray > createARGBInstance( final ImagePlus imp, int[] ids )
	{
		return new BigWarpImageStackImageLoader< ARGBType, IntArray >( new ARGBType(), imp, ids )
		{
			@Override
			protected IntArray wrapPixels( final Object array )
			{
				return new IntArray( ( int[] ) array );
			}

			@Override
			protected void linkType( final PlanarImg< ARGBType, IntArray > img )
			{
				img.setLinkedType( new ARGBType( img ) );
			}
		};
	}

	private final T type;

	private final ImagePlus imp;

	private final long[] dim;

	private final HashMap< Integer, SetupImgLoader > setupImgLoaders;

	public BigWarpImageStackImageLoader( final T type, final ImagePlus imp, int[] setupIds )
	{
		this.type = type;
		this.imp = imp;
		this.dim = new long[] { imp.getWidth(), imp.getHeight(), imp.getNSlices() };
		final int numSetups = imp.getNChannels();
		setupImgLoaders = new HashMap< Integer, SetupImgLoader >();
		for ( int i = 0; i < numSetups; ++i )
			setupImgLoaders.put( setupIds[ i ], new SetupImgLoader( i ) );
	}

	public class SetupImgLoader implements BasicSetupImgLoader< T >
	{
		private final int channel;

		public SetupImgLoader( final int channel )
		{
			this.channel = channel;
		}

		@Override
		public RandomAccessibleInterval< T > getImage( final int timepointId, final ImgLoaderHint... hints )
		{
			return new PlanarImg< T, A >( dim, type.getEntitiesPerPixel() )
			{
				private PlanarImg< T, A > init()
				{
					final int frame = timepointId + 1;
					for ( int slice = 1; slice <= dim[ 2 ]; ++slice )
						mirror.set( slice - 1, wrapPixels( imp.getStack().getPixels( imp.getStackIndex( channel + 1, slice, frame ) ) ) );
					linkType( this );
					return this;
				}
			}.init();
		}

		@Override
		public T getImageType()
		{
			return type;
		}
	}

	protected abstract A wrapPixels( Object array );

	protected abstract void linkType( PlanarImg< T, A > img );

	@Override
	public SetupImgLoader getSetupImgLoader( final int setupId )
	{
		return setupImgLoaders.get( setupId );
	}
}
