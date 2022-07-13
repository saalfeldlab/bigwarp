package org.janelia.utility.geom;

import java.util.Arrays;
import java.util.List;

import bigwarp.landmarks.LandmarkTableModel;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

public class GeomUtils
{
	public static double squaredDistance( final RealLocalizable position1, final RealLocalizable position2 )
	{
		double dist = 0;
		final int n = position1.numDimensions();
		for ( int d = 0; d < n; ++d )
		{
			final double diff = position2.getDoublePosition( d ) - position1.getDoublePosition( d );
			dist += diff * diff;
		}
		return dist;
	}
	
	public static double squaredDistance( final double[] position1, final double[] position2 )
	{
		double dist = 0;
		final int n = position1.length;
		for ( int d = 0; d < n; ++d )
		{
			final double diff = position2[ d ] - position1[ d ];
			dist += diff * diff;
		}
		return dist;
	}
	
	public final static void scale( final RealPoint p, final double scale )
	{
		for( int i = 0; i < p.numDimensions(); i++ )
			p.setPosition( p.getDoublePosition( i ) * scale, i );
	}
	
	/**
	 * Finds the parameters of the smallest hypersphere containing the points.
	 *
	 * @param pts
	 *            a list of points
	 * @return a pair containing the center and the squared distance
	 */
	public static Pair< RealPoint, Double > smallestEnclosingSpherePts( List< RealPoint > pts )
	{
		int nd = pts.get( 0 ).numDimensions();
		RealPoint p = new RealPoint( nd );
		RealPoint q = new RealPoint( nd );
		double maxSqrDist = Double.POSITIVE_INFINITY;
		// find pair of points with the largest distance
		for ( int i = 0; i < pts.size(); i++ )
			for ( int j = i + 1; j < pts.size(); j++ )
			{
				final double d = squaredDistance( pts.get( i ), pts.get( j ) );
				if ( d > maxSqrDist )
				{
					maxSqrDist = d;
					p.setPosition( pts.get( i ) );
					q.setPosition( pts.get( j ) );
				}
			}

		final RealPoint center = new RealPoint( p.numDimensions() );
		for ( int d = 0; d < p.numDimensions(); d++ )
			center.setPosition( 0.5 * p.getDoublePosition( d ) + 0.5 * q.getDoublePosition( d ), d );

		return new ValuePair< RealPoint, Double >( center, maxSqrDist );
	}
	
	public static Pair< RealPoint, Double > smallestEnclosingSphere( LandmarkTableModel ltm )
	{
		int nd = ltm.getNumdims();
		RealPoint p = new RealPoint( nd );
		RealPoint q = new RealPoint( nd );
		
		final double[] tmpA = new double[ nd ];
		final double[] tmpB = new double[ nd ];

		double maxSqrDist = -1;
		int N = ltm.getRowCount();
		// find pair of points with the largest distance
		for ( int i = 0; i < N; i++ )
			for ( int j = i + 1; j < N; j++ )
			{
				if( !ltm.isActive( i ) || !ltm.isActive( j ))
				{
					continue;
				}
				
				for( int d = 0; d < nd; d++)
				{
					tmpA[d] = ltm.getFixedPoint( i )[d];
					tmpB[d] = ltm.getFixedPoint( j )[d];
				}

				final double d = squaredDistance( tmpA, tmpB );
//				System.out.println( "tmpA: " + Arrays.toString( tmpA ));
//				System.out.println( "tmpB: " + Arrays.toString( tmpB ));
				
				if ( d > maxSqrDist )
				{
					maxSqrDist = d;
					p.setPosition( tmpA );
					q.setPosition( tmpB );
				}
			}
		
//		System.out.println( "p : " + p );
//		System.out.println( "q : " + q );

		final RealPoint center = new RealPoint( p.numDimensions() );
		for ( int d = 0; d < p.numDimensions(); d++ )
			center.setPosition( 0.5 * p.getDoublePosition( d ) + 0.5 * q.getDoublePosition( d ), d );

		return new ValuePair< RealPoint, Double >( center, maxSqrDist / 4 );
	}

}
