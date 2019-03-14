package bigwarp.loader;

import java.util.ArrayList;
import java.util.HashMap;

import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.img.cache.CacheArrayLoader;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.virtualstack.VirtualStackVolatileARGBArrayLoader;
import bdv.img.virtualstack.VirtualStackVolatileByteArrayLoader;
import bdv.img.virtualstack.VirtualStackVolatileFloatArrayLoader;
import bdv.img.virtualstack.VirtualStackVolatileShortArrayLoader;
import ij.ImagePlus;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.generic.sequence.TypedBasicImgLoader;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileIntArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.type.volatiles.VolatileFloatType;
import net.imglib2.type.volatiles.VolatileUnsignedByteType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;

/**
 * ImageLoader backed by a ImagePlus. The ImagePlus may be virtual and in
 * contrast to the imglib2 wrappers, we do not try to load all slices into
 * memory. Instead slices are stored in {@link VolatileGlobalCellCache}.
 *
 * Use {@link #createFloatInstance(ImagePlus)},
 * {@link #createUnsignedByteInstance(ImagePlus)} or
 * {@link #createUnsignedShortInstance(ImagePlus)} depending on the ImagePlus
 * pixel type.
 *
 * When loading images ({@link #getSetupImgLoader(int)},
 * {@link BasicSetupImgLoader#getImage(int, ImgLoaderHint...)}) the provided
 * setup id is used as the channel index of the {@link ImagePlus}, the provided
 * timepoint id is used as the frame index of the {@link ImagePlus}.
 *
 * @param <T>
 *            (non-volatile) pixel type
 * @param <V>
 *            volatile pixel type
 * @param <A>
 *            volatile array access type
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 */
public abstract class BigWarpVirtualStackImageLoader< T extends NativeType< T >, V extends Volatile< T > & NativeType< V >, A extends VolatileAccess > implements ViewerImgLoader, TypedBasicImgLoader< T >
{
	public static BigWarpVirtualStackImageLoader< FloatType, VolatileFloatType, VolatileFloatArray > createFloatInstance( final ImagePlus imp, final int[] setupIds )
	{
		return new BigWarpVirtualStackImageLoader< FloatType, VolatileFloatType, VolatileFloatArray >( imp, new VirtualStackVolatileFloatArrayLoader( imp ), new FloatType(), new VolatileFloatType(), setupIds )
		{
			@Override
			protected void linkType( final CachedCellImg< FloatType, VolatileFloatArray > img )
			{
				img.setLinkedType( new FloatType( img ) );
			}

			@Override
			protected void linkVolatileType( final CachedCellImg< VolatileFloatType, VolatileFloatArray > img )
			{
				img.setLinkedType( new VolatileFloatType( img ) );
			}
		};
	}

	public static BigWarpVirtualStackImageLoader< UnsignedShortType, VolatileUnsignedShortType, VolatileShortArray > createUnsignedShortInstance( final ImagePlus imp, final int[] setupIds )
	{
		return new BigWarpVirtualStackImageLoader< UnsignedShortType, VolatileUnsignedShortType, VolatileShortArray >( imp, new VirtualStackVolatileShortArrayLoader( imp ), new UnsignedShortType(), new VolatileUnsignedShortType(), setupIds )
		{
			@Override
			protected void linkType( final CachedCellImg< UnsignedShortType, VolatileShortArray > img )
			{
				img.setLinkedType( new UnsignedShortType( img ) );
			}

			@Override
			protected void linkVolatileType( final CachedCellImg< VolatileUnsignedShortType, VolatileShortArray > img )
			{
				img.setLinkedType( new VolatileUnsignedShortType( img ) );
			}
		};
	}

	public static BigWarpVirtualStackImageLoader< UnsignedByteType, VolatileUnsignedByteType, VolatileByteArray > createUnsignedByteInstance( final ImagePlus imp, final int[] setupIds )
	{
		return new BigWarpVirtualStackImageLoader< UnsignedByteType, VolatileUnsignedByteType, VolatileByteArray >( imp, new VirtualStackVolatileByteArrayLoader( imp ), new UnsignedByteType(), new VolatileUnsignedByteType(), setupIds )
		{
			@Override
			protected void linkType( final CachedCellImg< UnsignedByteType, VolatileByteArray > img )
			{
				img.setLinkedType( new UnsignedByteType( img ) );
			}

			@Override
			protected void linkVolatileType( final CachedCellImg< VolatileUnsignedByteType, VolatileByteArray > img )
			{
				img.setLinkedType( new VolatileUnsignedByteType( img ) );
			}
		};
	}

	public static BigWarpVirtualStackImageLoader< ARGBType, VolatileARGBType, VolatileIntArray > createARGBInstance( final ImagePlus imp, final int[] setupIds )
	{
		return new BigWarpVirtualStackImageLoader< ARGBType, VolatileARGBType, VolatileIntArray >( imp, new VirtualStackVolatileARGBArrayLoader( imp ), new ARGBType(), new VolatileARGBType(), setupIds )
		{
			@Override
			protected void linkType( final CachedCellImg< ARGBType, VolatileIntArray > img )
			{
				img.setLinkedType( new ARGBType( img ) );
			}

			@Override
			protected void linkVolatileType( final CachedCellImg< VolatileARGBType, VolatileIntArray > img )
			{
				img.setLinkedType( new VolatileARGBType( img ) );
			}
		};
	}

	private static double[][] mipmapResolutions = new double[][] { { 1, 1, 1 } };

	private static AffineTransform3D[] mipmapTransforms = new AffineTransform3D[] { new AffineTransform3D() };

	private final CacheArrayLoader< A > loader;

	private final VolatileGlobalCellCache cache;

	private final long[] dimensions;

	private final int[] cellDimensions;

	private final HashMap< Integer, SetupImgLoader > setupImgLoaders;

	protected BigWarpVirtualStackImageLoader( final ImagePlus imp, final CacheArrayLoader< A > loader, final T type, final V volatileType, int[] setupIds )
	{
		this.loader = loader;
		dimensions = new long[] { imp.getWidth(), imp.getHeight(), imp.getNSlices() };
		cellDimensions = new int[] { imp.getWidth(), imp.getHeight(), 1 };
		final int numSetups = imp.getNChannels();
		cache = new VolatileGlobalCellCache( 1, 1 );
		setupImgLoaders = new HashMap<>();
		for ( int i = 0; i < numSetups; ++i )
			setupImgLoaders.put( setupIds[ i ], new SetupImgLoader( setupIds[ i ], type, volatileType ) );
	}

	protected abstract void linkType( CachedCellImg< T, A > img );

	protected abstract void linkVolatileType( CachedCellImg< V, A > img );

	@Override
	public VolatileGlobalCellCache getCacheControl()
	{
		return cache;
	}

	@Override
	public SetupImgLoader getSetupImgLoader( final int setupId )
	{
		return setupImgLoaders.get( setupId );
	}

	public class SetupImgLoader extends AbstractViewerSetupImgLoader< T, V >
	{
		private final int setupId;

		protected SetupImgLoader( final int setupId, final T type, final V volatileType )
		{
			super( type, volatileType );
			this.setupId = setupId;
		}

		@Override
		public RandomAccessibleInterval< V > getVolatileImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			return prepareCachedImage( timepointId, level, LoadingStrategy.BUDGETED, volatileType );
		}

		@Override
		public RandomAccessibleInterval< T > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			return prepareCachedImage( timepointId, level, LoadingStrategy.BLOCKING, type );
		}

		/**
		 * (Almost) create a {@link CachedCellImg} backed by the cache. The
		 * created image needs a
		 * {@link NativeImg#setLinkedType(net.imglib2.type.Type) linked type}
		 * before it can be used. The type should be either {@link ARGBType} and
		 * {@link VolatileARGBType}.
		 */
		protected < T extends NativeType< T > > AbstractCellImg< T, A, ?, ? > prepareCachedImage( final int timepointId, final int level, final LoadingStrategy loadingStrategy, final T type )
		{
			final int priority = 0;
			final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
			final CellGrid grid = new CellGrid( dimensions, cellDimensions );
			return cache.createImg( grid, timepointId, setupId, level, cacheHints, loader, type );
		}

		@Override
		public double[][] getMipmapResolutions()
		{
			return mipmapResolutions;
		}

		@Override
		public AffineTransform3D[] getMipmapTransforms()
		{
			return mipmapTransforms;
		}

		@Override
		public int numMipmapLevels()
		{
			return 1;
		}
	}
}
