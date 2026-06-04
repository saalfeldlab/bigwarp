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
package org.janelia.utility.geom;

import net.imglib2.RealPoint;

public class Sphere 
{
	private RealPoint center;
	private double[] centerArr;
	private double radius;
	
	public Sphere( double[] center, double radius )
	{
		this.centerArr = center;
		this.radius = radius;
		this.center = RealPoint.wrap( center );
	}
	
	public Sphere( final RealPoint center, double radius )
	{
		this( center.positionAsDoubleArray(), radius );
	}

	public RealPoint getCenter()
	{
		return center;
	}

	public double[] getCenterArray()
	{
		return centerArr;
	}

	public double getRadius()
	{
		return radius;
	}

	public void setRadius( final double radius )
	{
		this.radius = radius;
	}

	public void setCenter( final double[] center )
	{
		System.arraycopy( center, 0, centerArr, 0, centerArr.length );
	}
	
	/**
	 * Is a point inside this sphere.
	 * Returns true if the distance from the given point to the center is less than or equal to
	 * this sphere's radius.
	 * 
	 * @param p the point
	 * @return true if the point inside this sphere.
	 */
	public boolean isInside( double[] p )
	{
		final double r2 = radius * radius;
		return GeomUtils.squaredDistance( centerArr, p ) <= r2;
	}

}
