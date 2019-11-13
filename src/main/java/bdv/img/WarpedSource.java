package bdv.img;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.render.DefaultMipmapOrdering;
import bdv.viewer.render.MipmapOrdering;
import bigwarp.BigWarpExporter;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Wrapped2DTransformAs3D;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class WarpedSource < T > implements Source< T >, MipmapOrdering
{

	public static < T > SourceAndConverter< T > wrap( final SourceAndConverter< T > wrap, final String name, int ndims )
	{
		return new SourceAndConverter< T >(
				new WarpedSource< T >( wrap.getSpimSource(), name ),
				wrap.getConverter(),
				wrap.asVolatile() == null ? null : wrap( wrap.asVolatile(), name, ndims ) );
	}

	/**
	 * The wrapped {@link Source}.
	 */
	private final Source< T > source;

	private final String name;

	/**
	 * This is either the {@link #source} itself, if it implements
	 * {@link MipmapOrdering}, or a {@link DefaultMipmapOrdering}.
	 */
	private final MipmapOrdering sourceMipmapOrdering;

	private InvertibleRealTransform xfm;

	private boolean isTransformed;

	private Interval warpedInterval;

	public WarpedSource( final Source< T > source, final String name )
	{
		this.source = source;
		this.name = name;
		this.isTransformed = false;

		this.xfm = null;

		sourceMipmapOrdering = MipmapOrdering.class.isInstance( source ) ?
				( MipmapOrdering ) source : new DefaultMipmapOrdering( source );
	}

	@Override
	public boolean isPresent( final int t )
	{
		return source.isPresent( t );
	}
	
	public synchronized void updateTransform( InvertibleRealTransform xfm )
	{
		this.xfm = xfm;

		// re-estimate interval of warped source
		final Interval wrappedInterval = source.getSource( 0, 0 );
		final InvertibleRealTransform invxfm = xfm.inverse().copy();
		warpedInterval = BigWarpExporter.estimateBounds( invxfm, wrappedInterval );
	}
	
	public void setIsTransformed( boolean isTransformed )
	{
		this.isTransformed = isTransformed;
	}
	
	public boolean isTransformed( )
	{
		return isTransformed;
	}

	public Source< T > getWrappedSource()
	{
		return source;
	}

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		return Views.interval(
				Views.raster( getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR ) ),
				boundingInterval( t, level ));
	}

	public Interval boundingInterval( final int t, final int level )
	{
		if( isTransformed && warpedInterval != null)
			return warpedInterval;
		else
			return source.getSource( t, level );
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		final RealRandomAccessible< T > sourceRealAccessible = source.getInterpolatedSource( t, level, method );

		if( isTransformed )
		{
			final AffineTransform3D transform = new AffineTransform3D();
			source.getSourceTransform( t, level, transform );
			final RealRandomAccessible< T > srcRaTransformed = RealViews.affineReal( source.getInterpolatedSource( t, level, method ), transform );

			if( xfm == null )
				return srcRaTransformed;
			else
				return new RealTransformRealRandomAccessible< T, RealTransform >( srcRaTransformed, xfm );
		}
		else
		{
			return sourceRealAccessible;
		}
	}

	@Override
	public void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		if( isTransformed )
			transform.identity();
		else
			source.getSourceTransform( t, level, transform );

//		System.out.println( "WarpedSource getSourceTransform: " + transform );
	}

	public InvertibleRealTransform getTransform()
	{
		return xfm;
	}

	@Override
	public T getType()
	{
		return source.getType();
	}

	@Override
	public String getName()
	{
		return source.getName() + "_" + name;
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return source.getVoxelDimensions();
	}

	@Override
	public int getNumMipmapLevels()
	{
		return source.getNumMipmapLevels();
	}

	@Override
	public synchronized MipmapHints getMipmapHints( final AffineTransform3D screenTransform, final int timepoint, final int previousTimepoint )
	{
		return sourceMipmapOrdering.getMipmapHints( screenTransform, timepoint, previousTimepoint );
	}
}
