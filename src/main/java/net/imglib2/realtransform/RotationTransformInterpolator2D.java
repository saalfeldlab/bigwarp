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
package net.imglib2.realtransform;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.janelia.utility.geom.GeomUtils;

import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.source.PlateauSphericalMaskRealRandomAccessible;
import bigwarp.source.PlateauSphericalMaskRealRandomAccessible.FalloffShape;
import bigwarp.transforms.BigWarpTransform;
import bigwarp.transforms.MaskedSimRotTransformSolver;
import bigwarp.transforms.TpsTransformSolver;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.MaskedSimilarityTransform.Interpolators;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.LinAlgHelpers;

public class RotationTransformInterpolator2D implements AffineInterpolator
{
	private final double thtDiff;

	private final AffineTransform2D tform;

	private final double[] c;

	private final double[] pDiff;

	public RotationTransformInterpolator2D( final AffineTransform2D transform, final double[] c )
	{
		tform = new AffineTransform2D();

		final AffineTransform2D transformEnd = new AffineTransform2D();
		transformEnd.set( transform );
		transformEnd.translate( -c[0], -c[1] );

		thtDiff = GeomUtils.scalesAngle( transformEnd )[ 2 ];

		// translation needed to from reconstructed to target transformation
		this.c = new double[ 2 ];
		System.arraycopy( c, 0, this.c, 0, 2 );

		pDiff = new double[ 2 ];
		double[] tmp = new double[ 2 ];
		AffineTransform2D t1 = get( 1 );
		t1.apply( c, tmp );
		transform.apply( c, pDiff );
		LinAlgHelpers.subtract( pDiff, tmp, pDiff );
	}

	public AffineTransform2D get( final double t )
	{
		// make identity
		tform.set( 1.0, 0.0, 0.0, 0.0, 1.0, 0.0 );

		// the rotation
		final double thtCurrent = t * thtDiff;
		tform.rotate( thtCurrent );

		// the translation
		final double[] pCurrent = new double[ 2 ];
		final double[] pTgt = new double[ 2 ];
		tform.apply( c, pCurrent );

		LinAlgHelpers.scale( pDiff, t, pTgt );
		LinAlgHelpers.add( pTgt, c, pTgt );
		LinAlgHelpers.subtract( pTgt, pCurrent, pTgt );
		tform.translate( pTgt );

		return tform;
	}
	
	public static void main( String[] args ) throws IOException
	{
//		final AffineTransform2D t = new AffineTransform2D();	
//		t.rotate( 0.2 );
//		t.translate( 10, 20 );
//		System.out.println( t );

//		test90deg();
		testReal2d();
	}

	public static void testReal2d() throws IOException
	{
		final LandmarkTableModel ltm = LandmarkTableModel.loadFromCsv( new File("/home/john/tmp/boats-bigrotation.csv"), false );

		final RealPoint c = new RealPoint( 0.0, 0.0 );
		final PlateauSphericalMaskRealRandomAccessible lambda = new PlateauSphericalMaskRealRandomAccessible( c );
		lambda.setFalloffShape( FalloffShape.COSINE );
		lambda.setSquaredRadius( 28111.13100490283 );
		lambda.setSquaredSigma( 24251.31739665425 );
		lambda.setCenter( new double[] { 380.26234219457774, 284.5915704375881 } );

		final RealPoint p = new RealPoint( 0.0, 0.0 );
		final RealPoint q = new RealPoint( 0.0, 0.0 );
		
		final TpsTransformSolver baseSolver = new TpsTransformSolver();
		
		final MaskedSimRotTransformSolver solver = new MaskedSimRotTransformSolver<>( 2, baseSolver, lambda, c.positionAsDoubleArray(), Interpolators.ROTATION );
		WrappedIterativeInvertibleRealTransform xfm = solver.solve( ltm );

		int idx = 0;
		
		final int nd = ltm.getNumdims();
		final RealPoint x = new RealPoint( nd );
		final RealPoint y = new RealPoint( nd );

		x.setPosition( Arrays.stream( ltm.getFixedPoint( idx ) ).mapToDouble( Double::doubleValue ).toArray() );
		xfm.apply( x, y );
		System.out.println("est pt : " + y );

		final double[] ytrue = Arrays.stream( ltm.getMovingPoint( idx ) ).mapToDouble( Double::doubleValue ).toArray();
		System.out.println( "tru pt :" +  new RealPoint( ytrue ));

	}
	
	public static <T extends RealType<T>> void transformLandmarkPoint( final LandmarkTableModel ltm, final int idx,
			final BigWarpTransform bwTform, RealRandomAccessible< T > lambda )
	{
		final int nd = ltm.getNumdims();
		final RealPoint x = new RealPoint( nd );
		final RealPoint y = new RealPoint( nd );

		x.setPosition( Arrays.stream( ltm.getFixedPoint( idx ) ).mapToDouble( Double::doubleValue ).toArray() );

		if( lambda != null )
			System.out.println( "l(x): " + lambda.getAt( x ) );

		final InvertibleRealTransform xfm = bwTform.getTransformation(false);

		xfm.apply( x, y );
		System.out.println( y );

		final double[] ytrue = Arrays.stream( ltm.getMovingPoint( idx ) ).mapToDouble( Double::doubleValue ).toArray();
		System.out.println( "true pt :" +  Arrays.toString( ytrue ));
	}
	
	
	public static void test90deg()
	{
		final LandmarkTableModel ltm = new LandmarkTableModel(2);
		ltm.add( new double[] { 0.0, 0.0 }, new double[] { 0.0, 0.0 } );
		ltm.add( new double[] { -1.0, 0.0 }, new double[] { 0.0, 1.0 } );
		ltm.add( new double[] { 0.0, 1.0 }, new double[] { 1.0, 0.0 } );
		ltm.add( new double[] { 1.0, 0.0 }, new double[] { 0.0, -1.0 } );
		ltm.add( new double[] { 0.0, -1.0 }, new double[] { -1.0, 0.0 } );
		
		final RealPoint c = new RealPoint( 0.0, 0.0 );
		final PlateauSphericalMaskRealRandomAccessible lambda = new PlateauSphericalMaskRealRandomAccessible( c );
		lambda.setFalloffShape( FalloffShape.COSINE );
		lambda.setRadius( 1.0 );
		lambda.setSigma( 1.0 );

		final RealPoint p = new RealPoint( 0.0, 0.0 );
		final RealPoint q = new RealPoint( 0.0, 0.0 );
		
		final TpsTransformSolver baseSolver = new TpsTransformSolver();
		final MaskedSimRotTransformSolver solver = new MaskedSimRotTransformSolver<>( 2, baseSolver, lambda, c.positionAsDoubleArray(), Interpolators.ROTATION );
		
		WrappedIterativeInvertibleRealTransform xfm = solver.solve( ltm );
		xfm.apply( p, q );
		System.out.println( p + " > " + q );

		System.out.println( "" );
		p.setPosition( 2.0, 0 );;
		xfm.apply( p, q );
		System.out.println( p + " > " + q );

		System.out.println( "" );
		p.setPosition( 1.0, 0 );
		xfm.apply( p, q );
		System.out.println( p + " > " + q );

	}

	
}
