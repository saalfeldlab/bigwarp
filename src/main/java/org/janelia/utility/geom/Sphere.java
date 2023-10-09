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
