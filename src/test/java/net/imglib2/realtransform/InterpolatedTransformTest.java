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
