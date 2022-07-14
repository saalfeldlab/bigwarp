package org.janelia.utility.geom;

import java.util.List;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

public class GeomUtils {
	
	/**
	 * Finds the parameters of the smallest hypersphere containing the points.
	 * 
	 * @param pts a list of points
	 * @return a pair containing the center and the squared distance
	 */
	public static Pair<RealPoint,Double> smallestEnclosingSphere( List<RealPoint> pts )
	{
		RealPoint p = null;
		RealPoint q = null;
		double maxSqrDist = Double.POSITIVE_INFINITY;
		// find pair of points with the largest distance
		for( int i = 0; i < pts.size(); i++)
			for( int j = i+1; j < pts.size(); j++) {
				double d = squaredDistance( pts.get( i ), pts.get( j ));
				if( d < maxSqrDist )
				{
					maxSqrDist = d;
					p = pts.get( i );
					q = pts.get( j );
				}
			}
		
		final RealPoint center = new RealPoint( p.numDimensions());
		for( int d = 0; d < p.numDimensions(); d++ )
		{
			center.setPosition( 
					0.5 * p.getDoublePosition(d) + 0.5 * q.getDoublePosition(d),
					d );
		}
		return new ValuePair<RealPoint, Double>(center, maxSqrDist);
	}

	final public static void scale( final RealPoint p, final double scale )
	{
		for( int i = 0; i < p.numDimensions(); i++ )
			p.setPosition( p.getDoublePosition( i ) * scale , i);
	}

	final public static double squaredDistance( final RealLocalizable position1, final RealLocalizable position2 )
	{
		double dist = 0;

		final int n = position1.numDimensions();
		for ( int d = 0; d < n; ++d )
		{
			final double pos = position2.getDoublePosition( d ) - position1.getDoublePosition( d );

			dist += pos * pos;
		}

		return dist;
	}	

	final public static double squaredDistance( final double[] position1, final double[] position2 )
	{
		double dist = 0;

		final int n = position1.length;
		for ( int d = 0; d < n; ++d )
		{
			final double pos = position2[d] - position1[d];
			dist += pos * pos;
		}

		return dist;
	}

}
