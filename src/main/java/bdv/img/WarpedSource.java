/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2022 Howard Hughes Medical Institute.
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
package bdv.img;

import java.util.function.Supplier;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.render.DefaultMipmapOrdering;
import bdv.viewer.render.MipmapOrdering;
import bigwarp.BigWarpExporter;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.BoundingBoxEstimation;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
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

	private RealTransform xfm;

	private boolean isTransformed;
	
	private final Supplier< Boolean > boundingBoxCullingSupplier;

	private BoundingBoxEstimation bboxEst;

	public WarpedSource( final Source< T > source, final String name )
	{
		this( source, name, null );
	}

	public WarpedSource( final Source< T > source, final String name,
			final Supplier< Boolean > doBoundingBoxCulling )
	{
		this.source = source;
		this.name = name;
		this.isTransformed = false;
		this.boundingBoxCullingSupplier = doBoundingBoxCulling;
		bboxEst = new BoundingBoxEstimation( BoundingBoxEstimation.Method.FACES, 5 );

		this.xfm = null;

		sourceMipmapOrdering = MipmapOrdering.class.isInstance( source ) ?
				( MipmapOrdering ) source : new DefaultMipmapOrdering( source );
	}

	@Override
	public boolean isPresent( final int t )
	{
		return source.isPresent( t );
	}
	
	@Override
	public boolean doBoundingBoxCulling()
	{
		if( boundingBoxCullingSupplier != null )
			return boundingBoxCullingSupplier.get();
		else
			return ( !isTransformed ) && ( source.doBoundingBoxCulling() );
	}

	public void updateTransform( RealTransform xfm )
	{
		this.xfm = xfm;
	}
	
	public void setIsTransformed( boolean isTransformed )
	{
		this.isTransformed = isTransformed;
	}
	
	public void setBoundingBoxEstimator( final BoundingBoxEstimation bboxEst )
	{
		this.bboxEst = bboxEst;
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
		if( isTransformed )
		{
			return Views.interval(
					Views.raster( getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR ) ),
					estimateBoundingInterval( t, level ));
		}
		return source.getSource( t, level );
	}

	private Interval estimateBoundingInterval( final int t, final int level )
	{
		if( xfm == null )
		{
			return source.getSource( t, level );
		}
		else
		{
			// getSource can be called by multiple threads, so need ensure application of 
			// the transform is thread safe here by copying
			return bboxEst.estimatePixelInterval( xfm.copy(), source.getSource( t, level ) );
		}
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{

//		RealRandomAccessible<T> realSrc = source.getInterpolatedSource( t, level, method );
//		if( isTransformed && xfm != null )
//		{
//			final AffineTransform3D transform = new AffineTransform3D();
//			source.getSourceTransform( t, level, transform );
//
//			RealTransformSequence totalTransform = new RealTransformSequence();
////			totalTransform.add( transform );
////			totalTransform.add( xfm );
////			totalTransform.add( transform.inverse() );
//
//			totalTransform.add( transform.inverse() );
//			totalTransform.add( xfm );
//			totalTransform.add( transform );
//
//			return new RealTransformRealRandomAccessible< T, RealTransform >( realSrc, xfm );
//		}
//		else
//		{
//			return realSrc;
//		}

		final RealRandomAccessible< T > sourceRealAccessible = source.getInterpolatedSource( t, level, method );
		if( isTransformed )
		{
			final AffineTransform3D transform = new AffineTransform3D();
			source.getSourceTransform( t, level, transform );
			final RealRandomAccessible< T > srcRaTransformed = RealViews.affineReal( source.getInterpolatedSource( t, level, method ), transform );

			if( xfm == null )
				return srcRaTransformed;
			else
				return new RealTransformRealRandomAccessible< T, RealTransform >( srcRaTransformed, xfm);
		}
		else
		{
			return sourceRealAccessible;
		}
	}

	@Override
	public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		if( isTransformed )
			transform.identity();
		else
			source.getSourceTransform( t, level, transform );
	}

	public RealTransform getTransform()
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
