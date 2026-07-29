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
package bigwarp.source;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import bigwarp.source.JacobianDeterminantRandomAccess.JacobianDeterminantRandomAccessibleInterval;
import net.imglib2.FinalInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.inverse.RealTransformFiniteDerivatives;
import net.imglib2.type.numeric.real.DoubleType;

public class JacobianDeterminantRandomAccessTest
{
	private static final double EPS = 1e-6;

	/**
	 * For an affine transform the Jacobian is constant and equal to the linear
	 * part, so the Jacobian determinant should be the determinant of that linear
	 * part at every point (the translation is irrelevant).
	 * <p>
	 * {@link AffineTransform3D} is not a {@code DifferentiableRealTransform}, so
	 * it is wrapped with {@link RealTransformFiniteDerivatives}; finite
	 * differences are exact for a linear map.
	 */
	@Test
	public void testDiagonalAffine()
	{
		final AffineTransform3D affine = new AffineTransform3D();
		affine.set(
				2.0, 0.0, 0.0, 5.0,
				0.0, 3.0, 0.0, -2.0,
				0.0, 0.0, 4.0, 7.0 );

		final double expectedDet = 2.0 * 3.0 * 4.0; // 24, independent of translation

		final RealRandomAccess< DoubleType > ra = jacobianDeterminantAccess( affine, new FinalInterval( 8, 8, 8 ) );

		// affine => constant jacobian determinant everywhere
		final double[][] pts = { { 0, 0, 0 }, { 1.5, 2.0, 3.25 }, { 7, 7, 7 }, { 3.3, 0.1, 5.9 } };
		for ( final double[] p : pts )
		{
			ra.setPosition( p );
			assertEquals( "jacobian determinant at " + Arrays.toString( p ),
					expectedDet, ra.get().getRealDouble(), EPS );
		}
	}

	/**
	 * Same idea but with shear terms, so the determinant is not simply a product
	 * of the diagonal scales.
	 */
	@Test
	public void testShearedAffine()
	{
		final AffineTransform3D affine = new AffineTransform3D();
		affine.set(
				1.0, 0.5, 0.0, 0.0,
				0.0, 2.0, 0.0, 0.0,
				0.3, 0.0, 3.0, 0.0 );

		final double expectedDet = det3(
				1.0, 0.5, 0.0,
				0.0, 2.0, 0.0,
				0.3, 0.0, 3.0 ); // = 6.0

		final RealRandomAccess< DoubleType > ra = jacobianDeterminantAccess( affine, new FinalInterval( 4, 4, 4 ) );
		ra.setPosition( new double[] { 2, 2, 2 } );
		assertEquals( expectedDet, ra.get().getRealDouble(), EPS );
	}

	private static RealRandomAccess< DoubleType > jacobianDeterminantAccess(
			final AffineTransform3D affine, final FinalInterval interval )
	{
		final RealTransformFiniteDerivatives diff = new RealTransformFiniteDerivatives( affine );
		final JacobianDeterminantRandomAccessibleInterval< DoubleType > jdet =
				new JacobianDeterminantRandomAccessibleInterval<>( interval, new DoubleType(), diff );
		return jdet.realRandomAccess();
	}

	private static double det3(
			final double a, final double b, final double c,
			final double d, final double e, final double f,
			final double g, final double h, final double i )
	{
		return a * ( e * i - f * h ) - b * ( d * i - f * g ) + c * ( d * h - e * g );
	}
}
