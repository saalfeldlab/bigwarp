package net.imglib2.realtransform;

import java.util.Arrays;

import org.janelia.utility.geom.GeomUtils;

import net.imglib2.util.LinAlgHelpers;

public class SimilarityTransformInterpolator2D implements AffineInterpolator
{
	private final double thtDiff;

	private final AffineTransform2D tform;

	private final double[] c;

	private final double[] pDiff;

//	private final double sStart;

	private final double sDiff;

	public SimilarityTransformInterpolator2D( final AffineTransform2D transform, final double[] c )
	{
		tform = new AffineTransform2D();

		final AffineTransform2D transformEnd = new AffineTransform2D();
		transformEnd.set( transform );
		transformEnd.translate( -c[0], -c[1] );

		double[] params = GeomUtils.scalesAngle( transformEnd );
		thtDiff = params[ 2 ];
//		double s = ( params[ 0 ] + params[ 1 ] ) / 2.0 ; 
//		sDiff = s - 1.0;

		sDiff = ( params[ 0 ] + params[ 1 ] ) / 2.0 - 1.0; 

		// translation needed to from reconstructed to target transformation
		this.c = new double[ 2 ];
		System.arraycopy( c, 0, this.c, 0, 2 );

		pDiff = new double[ 2 ];
		final double[] tmp = new double[ 2 ];
		final AffineTransform2D t1 = get( 1 );
		t1.apply( c, tmp );
		transform.apply( c, pDiff );
		LinAlgHelpers.subtract( pDiff, tmp, pDiff );
	}

	public AffineTransform2D get( final double t )
	{
		// make identity
		tform.set( 1.0, 0.0, 0.0, 0.0, 1.0, 0.0 );

		// the rotation
		tform.rotate( t * thtDiff );

		// the scale
		tform.scale( 1.0 + t * sDiff );

		// the translation
		final double[] pCurrent = new double[ 2 ];
		final double[] pTgt = new double[ 2 ];
		tform.apply( c, pCurrent );

		LinAlgHelpers.scale( pDiff, t, pTgt );
		LinAlgHelpers.add( c, pTgt, pTgt );
		LinAlgHelpers.subtract( pTgt, pCurrent, pTgt );
		tform.translate( pTgt );

		return tform;
	}

	public static void main( String[] args )
	{
		final double tht = Math.PI - 0.1;
		final double scale = 2.2;
		final double[] c = new double[]{ -10.0, 20.0  };
		final AffineTransform2D t = GeomUtils.centeredSimilarity( tht, scale, c );
		System.out.println( t );

		double[] ps = GeomUtils.scalesAngle( t, c );
		System.out.println( Arrays.toString( ps ));
		double sx = ps[0];
		double sy = ps[1];
		double thtEst = ps[2];
		double sEst = 0.5 * ( sx + sy );
		
		
		System.out.println( "\ntht: " + tht + " vs " + thtEst );
		System.out.println( "scl: " + sEst + " vs " + scale + "\n" );

		AffineTransform2D tEst = GeomUtils.centeredSimilarity( thtEst, sEst, c );
		System.out.println( tEst );
		

		final SimilarityTransformInterpolator2D interp = new SimilarityTransformInterpolator2D( t, c );
////		System.out.println( interp );
//		System.out.println( interp.get( 0 ) );

		System.out.println( t );
		System.out.println( interp.get( 1 ) ); 
	}
	
}