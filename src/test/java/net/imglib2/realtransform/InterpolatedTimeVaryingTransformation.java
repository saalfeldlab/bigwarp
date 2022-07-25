package net.imglib2.realtransform;

import bdv.viewer.animate.AbstractTransformAnimator;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.ConstantUtils;

public class InterpolatedTimeVaryingTransformation< T extends AbstractTransformAnimator > implements TimeVaryingTransformation
{
	private RealTransform a;
	private RealTransform b;

	public InterpolatedTimeVaryingTransformation( RealTransform a, RealTransform b )
	{
		this.a = a;
		this.b = b;
	}

	public RealTransform get( double t )
	{
		return new SpatiallyInterpolatedRealTransform<>( a, b, ConstantUtils.constantRealRandomAccessible( new DoubleType( t ), a.numSourceDimensions() ) );
	}

}
