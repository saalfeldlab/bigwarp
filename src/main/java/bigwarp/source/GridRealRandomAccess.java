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
package bigwarp.source;

import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import bigwarp.source.GridSource.GRID_TYPE;
import net.imglib2.AbstractRealLocalizable;
import net.imglib2.FinalInterval;
import net.imglib2.Localizable;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccess;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;


public class GridRealRandomAccess< T extends RealType<T>> extends AbstractRealLocalizable implements RealRandomAccess< T >
{

	protected RealTransform warp;

	private T value;
	private GRID_TYPE method = GRID_TYPE.MOD;

	private double gridSpacing = 20;
	private double gridWidth = 2.0;
	private double gridHalfWidth = gridWidth / 2.0;
	
	private boolean is2d = false;
	
	protected GridRealRandomAccess( double[] dimensions )
	{
		this( dimensions, null, null );
	}

	protected GridRealRandomAccess( double[] dimensions, T value, RealTransform warp )
	{
		this( dimensions, value, warp, GRID_TYPE.MOD );
	}

	protected GridRealRandomAccess( double[] dimensions, T value, RealTransform warp, GRID_TYPE method )
	{
		super( dimensions.length );
		this.value = value;
		if( warp != null )
			this.warp = warp.copy();

		this.method = method;
		is2d = ( dimensions.length == 2 || dimensions[2] == 0 );
	}
	
	
	public static void main( String[] args )
	{
		AffineTransform3D transform = new AffineTransform3D();
		transform.set(
				1.1, -0.2, 0.1, 0,
				0.3, 0.9, -0.1, 0,
				-0.1, -0.2, 1.2, 0);
		
		FinalInterval itvl = Intervals.createMinMax( 0,0,0, 255, 255, 255 );
		AffineTransform3D srcXfm = new AffineTransform3D();
		GridSource src = new GridSource( "grid", new UnsignedByteType(), itvl, srcXfm, null );
		src.setMethod( GridSource.GRID_TYPE.LINE );

		BdvStackSource bdv = BdvFunctions.show( src );

//		GridRealRandomAccess<DoubleType> ra = new GridRealRandomAccess<DoubleType>( new double[]{ 100.0, 100.0 }, new DoubleType(), transform );
//		ra.setMethod( GRID_TYPE.LINE );
//		ra.setGridWidth( 4 );
//
//		for( double x = 2.1; x < 5.0; x+= 1.0 ) for( double y = 0.0; y < 5.0; y+= 0.5 )
//		{
//			ra.setPosition( new double[]{x,y});
//			System.out.println("("+x+","+y+") : " + ra.get() );
//		}
	}
	
	public void setGridSpacing( double spacing )
	{
		this.gridSpacing = spacing;
	}
	
	public void setGridWidth( double width )
	{
		this.gridWidth = width;
		gridHalfWidth = width / 2.0 ;
	}

	public void setMethod( GRID_TYPE method )
	{
		this.method = method;
	}

	@Override
	public T get() 
	{
		double[] mypt = new double[ this.numDimensions() ];
		this.localize( mypt );
		
		switch( method )
		{
		case LINE:
			return getLine( mypt );
		default:
			return getMod( mypt );
		}
	}

	private T getLine( double[] pt )
	{
		double[] warpRes;
		if( warp != null )
		{
			warpRes = new double[ warp.numTargetDimensions() ];
			warp.apply( pt, warpRes );
		}
		else
		{
			warpRes = pt;
		}
		int nd = warpRes.length;
		if( is2d )
			nd = 2;
		
		double val = -1.0;
		int i = 0;
		for( int d = 0; d < nd; d++ )
		{
			double tmp = warpRes[ d ] % gridSpacing;
			// make sure tmp is positive
			if( tmp < 0.0 ){ tmp *= -1; }

			if( tmp <= gridHalfWidth )
			{
				tmp = gridHalfWidth - tmp;
			}
			else if( tmp >= (gridSpacing - gridHalfWidth ) )
			{
				tmp = tmp - gridSpacing + gridHalfWidth;
			}else
			{
				tmp = 0.0;
			}
			
			if( tmp > val ){
				val = tmp;
//				val += tmp;
//				i++;
			}
			
		}

		T out = value.copy();
		if( val < gridWidth )
		{
			// we want out to have a peak value of 255
			// Note: val takes a max value of gridHalfWidth
			out.setReal( val * ( 255.0 / gridHalfWidth) );
		}else
			out.setZero();

		return out;
	}

	private T getMod( double[] pt )
	{
		double[] warpRes;
		if( warp != null )
		{
			warpRes = new double[ warp.numTargetDimensions() ];
			warp.apply( pt, warpRes );
		}
		else
		{
			warpRes = pt;
		}

		double val = 0.0;
		for( int d = 0; d < warpRes.length; d++ )
		{
			double tmp = warpRes[ d ] % gridSpacing;
			if( tmp < 0 ) tmp *= -1;

			val += tmp;
		}
		T out = value.copy();
		out.setReal( val );
		return out;
	}

	private boolean withinRad( double[] pt1, double[] pt2, double rad )
	{
		double radSquared = rad * rad;
		double distSquared = 0.0;
		for( int d = 0; d < pt2.length; d++ )
			distSquared += ( pt1[ d ] - pt2[ d ] ) * ( pt1[ d ] - pt2[ d ] );
		
		return distSquared  < radSquared;
	}

	@Override
	public RealRandomAccess<T> copyRealRandomAccess()
	{
		return copy();
	}
	
	public RealRandomAccess<T> copy() 
	{
		if( warp == null )
		{
			GridRealRandomAccess< T > ra =  new GridRealRandomAccess< T >( new double[ position.length ], value.copy(), 
					null, this.method  );
			ra.gridSpacing = this.gridSpacing;
			ra.gridWidth = this.gridWidth;
			ra.gridHalfWidth = this.gridHalfWidth;
			return ra;
		}
		else
		{
			GridRealRandomAccess< T > ra = new GridRealRandomAccess< T >( new double[ position.length ], value.copy(), 
					warp, this.method  );
			ra.gridSpacing = this.gridSpacing;
			ra.gridWidth = this.gridWidth;
			ra.gridHalfWidth = this.gridHalfWidth;
			return ra;
		}

	}

	public RealRandomAccess<T> copyRandomAccess() 
	{
		return copy();
	}

	@Override
	public void fwd( final int d )
	{
		++position[ d ];
	}

	@Override
	public void bck( final int d )
	{
		--position[ d ];
	}

	@Override
	public void move( final int distance, final int d )
	{
		position[ d ] += distance;
	}

	@Override
	public void move( final long distance, final int d )
	{
		position[ d ] += distance;
	}

	@Override
	public void move( final Localizable localizable )
	{
		for ( int d = 0; d < n; ++d )
		{
			final int distance = localizable.getIntPosition( d );
			position[ d ] += distance;
		}
	}

	@Override
	public void move( final int[] distance )
	{
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] += distance[ d ];
		}
	}

	@Override
	public void move( final long[] distance )
	{
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] += distance[ d ];
		}
	}

	@Override
	public void setPosition( final Localizable localizable )
	{
		localizable.localize( position );
	}

	@Override
	public void setPosition( final int[] pos )
	{
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] = pos[ d ];
		}
	}

	@Override
	public void setPosition( final long[] pos )
	{
		for ( int d = 0; d < n; ++d )
		{
			final int p = ( int ) pos[ d ];
			position[ d ] = p;
		}
	}

	@Override
	public void setPosition( final int pos, final int d )
	{
		position[ d ] = pos;
	}

	@Override
	public void setPosition( final long pos, final int d )
	{
		position[ d ] = ( int ) pos;
	}

	@Override
	public void move(float distance, int d) {
		position[ d ] += distance;
	}

	@Override
	public void move(double distance, int d) {
		position[ d ] += distance;
	}

	@Override
	public void move(RealLocalizable localizable) {
		for ( int d = 0; d < n; ++d )
		{
			final double distance = localizable.getDoublePosition( d );
			position[ d ] += distance;
		}
	}

	@Override
	public void move(float[] distance) {
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] += distance[ d ];
		}
	}

	@Override
	public void move(double[] distance) {
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] += distance[ d ];
		}
	}

	@Override
	public void setPosition(RealLocalizable localizable) {
		for ( int d = 0; d < n; ++d )
		{
			final double pos = localizable.getDoublePosition( d );
			position[ d ] = pos;
		}
	}

	@Override
	public void setPosition(float[] p) {
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] = p[ d ];
		}
	}

	@Override
	public void setPosition(double[] p) {
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] = p[ d ];
		}	
	}

	@Override
	public void setPosition(float p, int d) {
		position[ d ] = p;
	}

	@Override
	public void setPosition(double p, int d) {
		position[ d ] = p;
	}

}
