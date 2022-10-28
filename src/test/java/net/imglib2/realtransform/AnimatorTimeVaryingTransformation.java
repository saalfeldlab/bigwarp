package net.imglib2.realtransform;

import bdv.viewer.animate.AbstractTransformAnimator;

public class AnimatorTimeVaryingTransformation< T extends AbstractTransformAnimator > implements TimeVaryingTransformation
{
	private T animator;

	public AnimatorTimeVaryingTransformation( T animator )
	{
		this.animator = animator;
	}
	
	public RealTransform get( double t )
	{
		return animator.get( t );
	}

}
