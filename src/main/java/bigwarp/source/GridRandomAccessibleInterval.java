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

import net.imglib2.AbstractInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.RealType;

public class GridRandomAccessibleInterval<T extends RealType<T>> extends AbstractInterval implements RandomAccessibleInterval<T> 
{
	RealTransform warp;
	GridRandomAccess< T > ra;
	
	public GridRandomAccessibleInterval( Interval interval, T t, RealTransform warp )
	{
		super( interval );
		this.warp = warp;
		ra = new GridRandomAccess< T >( new long[ interval.numDimensions() ], t, warp );
	}

	@Override
	public RandomAccess<T> randomAccess() {
		return ra.copy();
	}

	@Override
	public RandomAccess<T> randomAccess( Interval interval) {
		return randomAccess();
	}

	@Override
	public T getType()
	{
		return ra.getType();
	}

//	public RandomAccessibleInterval<T> copy()
//	{
//		return new GridRandomAccessibleInterval<T>(
//					this, this.ra.value.copy(), 
//					warp );
//		
//
//	}
	
}
