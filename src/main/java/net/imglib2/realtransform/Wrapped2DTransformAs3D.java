package net.imglib2.realtransform;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;

public class Wrapped2DTransformAs3D implements InvertibleRealTransform
{
	public InvertibleRealTransform transform;

	public Wrapped2DTransformAs3D( final InvertibleRealTransform transform )
	{
		this.transform = transform;
	}

	public InvertibleRealTransform getTransform()
	{
		return transform;
	}

	@Override
	public int numSourceDimensions()
	{
		return 3;
	}

	@Override
	public int numTargetDimensions()
	{
		return 3;
	}

	@Override
	public void apply( double[] source, double[] target )
	{
		transform.apply( source, target );
		target[ 2 ] = source[ 2 ];
	}

	@Override
	public void apply( RealLocalizable source, RealPositionable target )
	{
		transform.apply( source, target );
		target.setPosition( source.getDoublePosition( 2 ), 2 );
	}

	@Override
	public void applyInverse( double[] source, double[] target )
	{
		transform.applyInverse( source, target );
		source[ 2 ] = target[ 2 ];
	}

	@Override
	public void applyInverse( RealPositionable source, RealLocalizable target )
	{
		transform.applyInverse( source, target );
		source.setPosition( target.getDoublePosition( 2 ), 2 );
	}

	public InvertibleRealTransform copy()
	{
		return new Wrapped2DTransformAs3D( transform.copy() );
	}

	@Override
	public InvertibleRealTransform inverse()
	{
		return new Wrapped2DTransformAs3D( transform.inverse() );
	}

}