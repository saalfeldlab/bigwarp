/*
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

package net.imglib2.iterator;

import net.imglib2.AbstractRealInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.Iterator;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.util.Util;

/**
 * Use this class to iterate a virtual {@link RealInterval} in flat order, that is:
 * row by row, plane by plane, cube by cube, ... at a particular spacing. 
 *
 * @author John Bogovic
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class RealIntervalIterator extends AbstractRealInterval implements Iterator, RealLocalizable
{

	final protected double[] step;

	final protected double[] location;

	/**
	 * Iterates an {@link Interval} of the given dimensions with <em>min</em>=
	 * 0<sup><em>n</em></sup>
	 *
	 * @param interval
	 * @param step
	 */
	public RealIntervalIterator( final RealInterval interval, final double[] step )
	{
		super( interval );
		this.step = step;
		this.location = new double[ interval.numDimensions() ];
		reset();
	}

	/**
	 * Iterates an {@link Interval} with given <em>min</em> and <em>max</em>.
	 *
	 * @param min
	 * @param max
	 */
	public RealIntervalIterator( final double[] min, final double[] max, final double[] steps )
	{
		this( new FinalRealInterval( min, max ), steps );
	}

	@Override
	public void reset()
	{
		realMin( location );
		location[ 0 ] -= step[ 0 ];
	}

	@Override
	public boolean hasNext()
	{
		for( int d = 0; d < numDimensions(); d++ )
			if( (location[ d ] + step[ d ]) <= realMax( d ))
			{
				return true;
			}

		return false;
	}

	@Override
	public String toString()
	{
		return Util.printCoordinates( this );
	}

	@Override
	public void localize(float[] position)
	{
		for( int d = 0; d < position.length; d++ )
			position[ d ] = (float)location[ d ];
	}

	@Override
	public void localize(double[] position)
	{
		System.arraycopy( location, 0, position, 0, position.length );
	}

	@Override
	public float getFloatPosition(int d)
	{
		return (float) location[ d ];
	}

	@Override
	public double getDoublePosition(int d)
	{
		return location[ d ];
	}

	@Override
	public void jumpFwd(long steps)
	{
		for( int i = 0; i < steps; i++ )
			fwd();
	}

	@Override
	public void fwd()
	{
		for( int d = 0; d < numDimensions(); d++ )
		{
			fwdDim( d );
			if( location[ d ] <= realMax(d))
				break;
			else
				location[ d ] = realMin( d );
		}
	}

	private void fwdDim( final int d )
	{
		location[ d ] += step[ d ];
	}

}
