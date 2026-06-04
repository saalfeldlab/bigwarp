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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.ConstantUtils;

public class InterpolatedTransformTest
{
	
	@Test
	public void simpleTest()
	{
		final double EPS = 1e-9;

		final Translation2D a = new Translation2D( new double[]{ 1, 0 });
		final Translation2D b = new Translation2D( new double[]{ 2, 0 });
		
		final RealRandomAccessible< DoubleType > l1 = ConstantUtils.constantRealRandomAccessible( new DoubleType(0.1), 2 );
		final RealRandomAccessible< DoubleType > l5 = ConstantUtils.constantRealRandomAccessible( new DoubleType(0.5), 2 );
		final RealRandomAccessible< DoubleType > l9 = ConstantUtils.constantRealRandomAccessible( new DoubleType(0.9), 2 );

		final SpatiallyInterpolatedRealTransform<?> t1 = new SpatiallyInterpolatedRealTransform<>( a, b, l1 );
		final SpatiallyInterpolatedRealTransform<?> t5 = new SpatiallyInterpolatedRealTransform<>( a, b, l5 );
		final SpatiallyInterpolatedRealTransform<?> t9 = new SpatiallyInterpolatedRealTransform<>( a, b, l9 );
		
		RealPoint src = new RealPoint( 2 );
		RealPoint dst = new RealPoint( 2 );
		
		t1.apply( src, dst );
		assertEquals( "lambda 0.1", 1.9, dst.getDoublePosition( 0 ), EPS );

		t5.apply( src, dst );
		assertEquals( "lambda 0.5", 1.5, dst.getDoublePosition( 0 ), EPS );

		t9.apply( src, dst );
		assertEquals( "lambda 0.9", 1.1, dst.getDoublePosition( 0 ), EPS );

	}

}
