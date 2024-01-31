package org.janelia.utility.geom;

import java.util.List;

/**
 * Estimates the smallest sphere that bounds given points.
 * <p>
 * Ritter, Jack (1990), "An efficient bounding sphere", in Glassner, Andrew S. (ed.), Graphics Gems, San Diego, CA, US: Academic Press Professional, Inc., pp. 301–303,
 *
 * @author John Bogovic
 *
 */
public class BoundingSphereRitter
{

	/**
	 * Returns an estimate of the smallest {@link Sphere} that contains the input points.
	 *
	 * @param points a collection of points
	 * @return the bounding sphere
	 */
	public static Sphere boundingSphere( final List<double[]> points )
	{
		if( points == null || points.isEmpty() )
			return null;

		final int nd = points.get( 0 ).length;
		final double[] x = new double[ nd ];
		final double[] y = new double[ nd ];
		double maxSqrDist = -1;
		// find pair of points with the largest distance
		for ( int i = 0; i < points.size(); i++ )
			for ( int j = i + 1; j < points.size(); j++ )
			{
				final double d = GeomUtils.squaredDistance( points.get( i ), points.get( j ) );
				if ( d > maxSqrDist )
				{
					maxSqrDist = d;
					System.arraycopy( points.get( i ), 0, x, 0, nd );
					System.arraycopy( points.get( j ), 0, y, 0, nd );
				}
			}

		final double[] center = new double[ nd ];
		for ( int d = 0; d < nd; d++ )
			center[d] = 0.5 * ( x[d] + y[d] );

		final Sphere sph = new Sphere( center, Math.sqrt( maxSqrDist ) / 2 );
		boolean allCovered = false;
		int k = 0;
		while( !allCovered && k < 5)
		{
			allCovered = false;
			for( final double[] p : points )
			{
				allCovered |= updateSphere( sph, p );
			}
			k++;
		}
		return sph;
	}

	/**
	 * return the point in point farthest from p
	 *
	 * @param points a list of points
	 * @param p a base point
	 * @return the point farthest from p
	 */
	public static double[] furthestFrom( final List<double[]> points, double[] p )
	{
		double maxSqrDist = -1;
		int maxIndex = -1;
		for ( int i = 0; i < points.size(); i++ )
		{
			final double dist = GeomUtils.squaredDistance( p, points.get( i ) );
			if ( dist > maxSqrDist )
			{
				maxSqrDist = dist;
				maxIndex = i;
			}
		}
		return points.get( maxIndex );
	}

	/**
	 * Changes the value of the input sphere such that it contains the given point p.
	 * Returns true if the sphere was modified.
	 *
	 * @param sphere the sphere
	 * @param p the point coordinates
	 * @return true if the sphere was changed
	 */
	public static boolean updateSphere( final Sphere sphere, final double[] p )
	{
		final double sqrDist = GeomUtils.squaredDistance( sphere.getCenterArray(), p );
		final double r = sphere.getRadius();
		if ( sqrDist <= r*r )
		{
			return false;
		} else
		{
//			System.out.println( "update sphere" );
			final double halfDist = 0.5 * ( Math.sqrt( sqrDist ) - sphere.getRadius() );
			final double[] c = sphere.getCenterArray();
			final double[] v = new double[ c.length ];
			double mag = 0;
			for( int i = 0; i < c.length; i++ )
			{
				v[i] = ( p[i] - c[i]);
				mag += v[i] * v[i];
			}
			for( int i = 0; i < c.length; i++ )
			{
				c[i] += v[i] * halfDist / Math.sqrt( mag );
			}

			sphere.setRadius( sphere.getRadius() + halfDist );
			return true;
		}
	}

}
