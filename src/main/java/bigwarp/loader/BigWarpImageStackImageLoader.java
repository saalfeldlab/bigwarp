package bigwarp.loader;

import ij.ImagePlus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

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
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

public abstract class BigWarpImageStackImageLoader< T extends NumericType< T > & NativeType< T >, A extends ArrayDataAccess< A > > implements BasicImgLoader, TypedBasicImgLoader< T >
{
	public static BigWarpImageStackImageLoader< UnsignedByteType, ByteArray > createUnsignedByteInstance( final ImagePlus imp, int[] ids )
	{
		return new BigWarpImageStackImageLoader< UnsignedByteType, ByteArray >( new UnsignedByteType(), imp, array -> new ByteArray( ( byte[] ) array ), ids )
		{

			@Override
			protected void linkType( final PlanarImg< UnsignedByteType, ByteArray > img )
			{
				img.setLinkedType( new UnsignedByteType( img ) );
			}
		};
	}

	public static BigWarpImageStackImageLoader< UnsignedShortType, ShortArray > createUnsignedShortInstance( final ImagePlus imp, int[] ids )
	{
		return new BigWarpImageStackImageLoader< UnsignedShortType, ShortArray >( new UnsignedShortType(), imp, array -> new ShortArray( ( short[] ) array ), ids )
		{

			@Override
			protected void linkType( final PlanarImg< UnsignedShortType, ShortArray > img )
			{
				img.setLinkedType( new UnsignedShortType( img ) );
			}
		};
	}

	public static BigWarpImageStackImageLoader< FloatType, FloatArray > createFloatInstance( final ImagePlus imp, int[] ids )
	{
		return new BigWarpImageStackImageLoader< FloatType, FloatArray >( new FloatType(), imp, array -> new FloatArray( ( float[] ) array ), ids )
		{

			@Override
			protected void linkType( final PlanarImg< FloatType, FloatArray > img )
			{
				img.setLinkedType( new FloatType( img ) );
			}
		};
	}

	public static BigWarpImageStackImageLoader< ARGBType, IntArray > createARGBInstance( final ImagePlus imp, int[] ids )
	{
		return new BigWarpImageStackImageLoader< ARGBType, IntArray >( new ARGBType(), imp, array -> new IntArray( ( int[] ) array ), ids )
		{

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

	private final Function< Object, A > wrapPixels;

	public BigWarpImageStackImageLoader( final T type, final ImagePlus imp,  final Function< Object, A > wrapPixels, int[] setupIds )
	{
		this.type = type;
		this.imp = imp;
		this.dim = new long[] { imp.getWidth(), imp.getHeight(), imp.getNSlices() };
		final int numSetups = imp.getNChannels();
		setupImgLoaders = new HashMap< Integer, SetupImgLoader >();
		this.wrapPixels = wrapPixels;
		for ( int i = 0; i < numSetups; ++i )
			setupImgLoaders.put( setupIds[ i ], new SetupImgLoader( setupIds[ i ] ) );
	}

	protected abstract void linkType( PlanarImg< T, A > img );

	@Override
	public SetupImgLoader getSetupImgLoader( final int setupId )
	{
		return setupImgLoaders.get( setupId );
	}
	
	public class SetupImgLoader implements BasicSetupImgLoader< T >
	{
		private final int setupId;

		public SetupImgLoader( final int setupId )
		{
			this.setupId = setupId;
		}

		@Override
		public RandomAccessibleInterval< T > getImage( final int timepointId, final ImgLoaderHint... hints )
		{
			final int channel = setupId + 1;
			final int frame = timepointId + 1;
			final ArrayList< A > slices = new ArrayList<>();
			for ( int slice = 1; slice <= dim[ 2 ]; ++slice )
				slices.add( wrapPixels.apply( imp.getStack().getPixels( imp.getStackIndex( channel, slice, frame ) ) ) );
			final PlanarImg< T, A > img = new PlanarImg<>( slices, dim, type.getEntitiesPerPixel() );
			@SuppressWarnings( "unchecked" )
			final NativeTypeFactory< T, ? super A > typeFactory = ( NativeTypeFactory< T, ? super A > ) type.getNativeTypeFactory();
			img.setLinkedType( typeFactory.createLinkedType( img ) );
			return img;
		}

		@Override
		public T getImageType()
		{
			return type;
		}
	}
}
