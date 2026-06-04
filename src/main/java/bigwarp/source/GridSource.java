/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2025 Howard Hughes Medical Institute.
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
package bigwarp.source;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bigwarp.BigWarpData;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.BoundingBoxEstimation;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class GridSource< T extends RealType< T >> implements Source< T >
{
	
	public enum GRID_TYPE { MOD, LINE };
	
	protected final String name;
	
	protected BigWarpData<?> sourceData;

	protected AffineTransform3D sourceTransform;
	
	protected final Interval interval;

	protected final GridRealRandomAccessibleRealInterval<T> gridImg;
	
	protected T type;
	
	public GridSource( String name, BigWarpData< ? > data, T t, RealTransform warp  )
	{
		this( name, t, getInterval( data ), getSourceTransform( data ), warp );
	}

	public GridSource( String name, T t, Interval interval, AffineTransform3D sourceTransform,  RealTransform warp  )
	{
		this.name = name;
		this.type = t.copy();
		this.interval = interval;
		// always identity
		this.sourceTransform = new AffineTransform3D();
		
		gridImg = new GridRealRandomAccessibleRealInterval<T>( interval, t, warp );
	}
	
	private static AffineTransform3D getSourceTransform( BigWarpData<?> data )
	{
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		data.sources.get( 0 ).getSpimSource().getSourceTransform( 0, 0, sourceTransform );
		return sourceTransform;
	}

	private static Interval getInterval( BigWarpData<?> data )
	{
//		return new FinalInterval( data.sources.get( data.targetSourceIndices[ 0 ] ).getSpimSource().getSource( 0, 0 ));
//		return new FinalInterval( data.sources.get( data.movingSourceIndices[ 0 ] ).getSpimSource().getSource( 0, 0 ));
		BoundingBoxEstimation bbe = new BoundingBoxEstimation();
		AffineTransform3D affine = new AffineTransform3D();
		data.getTargetSource( 0 ).getSpimSource().getSourceTransform( 0, 0, affine );
		return bbe.estimatePixelInterval(  affine, data.getTargetSource( 0 ).getSpimSource().getSource( 0, 0 ) );
	}

	public void setGridSpacing( double spacing )
	{
		gridImg.ra.setGridSpacing( spacing );
	}
	
	public void setGridWidth( double width )
	{
		gridImg.ra.setGridWidth( width );
	}

	public void setWarp( RealTransform warp )
	{
		gridImg.ra.warp = warp;
	}
	
	@Override
	public boolean isPresent( int t )
	{
		return ( t == 0 );
	}
	
	public void setMethod( GRID_TYPE method )
	{
		gridImg.ra.setMethod( method );
	}

	@Override
	public RandomAccessibleInterval<T> getSource( int t, int level ) 
	{
		return Views.interval( Views.raster( 
				getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR ) ), 
				interval );
	}

	@Override
	public RealRandomAccessible<T> getInterpolatedSource( int t, int level, Interpolation method ) 
	{
		return gridImg;
	}

	@Override
	public void getSourceTransform( int t, int level, AffineTransform3D transform )
	{
//		sourceData.sources.get( 0 ).getSpimSource().getSourceTransform( t, level, transform );
		transform.set( sourceTransform );
	}

	@Override
	public T getType()
	{
		return type;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
//		return sourceData.sources.get( sourceData.targetSourceIndices[ 0 ] ).getSpimSource().getVoxelDimensions();
		return sourceData.getTargetSource( 0 ).getSpimSource().getVoxelDimensions();
	}

	@Override
	public int getNumMipmapLevels() 
	{
		return 1;
	}
	
}
