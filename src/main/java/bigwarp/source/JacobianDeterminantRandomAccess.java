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
package bigwarp.source;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import net.imglib2.AbstractRealInterval;
import net.imglib2.AbstractRealLocalizable;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessibleRealInterval;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.inverse.DifferentiableRealTransform;
import net.imglib2.type.numeric.RealType;

public class JacobianDeterminantRandomAccess< T extends RealType<T>> extends AbstractRealLocalizable implements RealRandomAccess< T >
{
	protected DifferentiableRealTransform transform;
	
	final private T value;
	final double[] warpRes;

	protected JacobianDeterminantRandomAccess( double[] dimensions )
	{
		this( dimensions, null, null );
	}
	
	protected JacobianDeterminantRandomAccess( final double[] dimensions, final T value, final DifferentiableRealTransform transform )
	{
		super( dimensions.length );
		setTransform( transform );
		this.value = value;
		warpRes = new double[ numDimensions() ]; 
	}
	
	public void setTransform( final DifferentiableRealTransform transform )
	{
		if( transform != null)
		{
			this.transform = transform.copy();
		}
	}

	@Override
	public T get() 
	{
		// copy value here so this is thread safe
		T out = value.copy();
		if( transform == null )
		{
			out.setZero();
			return out;
		}

		// compute the jacobian determinant at this point
		double[] x = new double[ numDimensions() ];
		localize( x );
		AffineTransform jacobian = transform.jacobian( x );
		DMatrixRMaj jacMtx = new DMatrixRMaj();
		jacMtx.data = jacobian.getRowPackedCopy(); 
		out.setReal( CommonOps_DDRM.det( jacMtx ) );

		return out;
	}

	public RealRandomAccess<T> copy() 
	{
		return new JacobianDeterminantRandomAccess< T >( new double[ position.length ], value.copy(), 
				transform );
	}

	public RealRandomAccess<T> copyRandomAccess() 
	{
		return copy();
	}
	
	@Override
	public RealRandomAccess<T> copyRealRandomAccess() 
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
	
	public static class JacobianDeterminantRandomAccessibleInterval<T extends RealType<T>> extends AbstractRealInterval implements RealRandomAccessibleRealInterval<T> 
	{
		protected final JacobianDeterminantRandomAccess< T > ra;

		public JacobianDeterminantRandomAccessibleInterval( Interval interval, T t, DifferentiableRealTransform warp )
		{
			super( interval );
			ra = new JacobianDeterminantRandomAccess< T >( new double[ interval.numDimensions() ], t, warp );
		}

		@Override
		public RealRandomAccess<T> realRandomAccess() {
			return ra.copy();
		}

		@Override
		public RealRandomAccess<T> realRandomAccess(RealInterval interval) {
			return realRandomAccess();
		}
		
		public void setTransform( final DifferentiableRealTransform transform )
		{
			ra.setTransform( transform );
		}
		
	}

}
