package net.imglib2.realtransform;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;

public class Wrapped2DTransformAs3D implements InvertibleRealTransform
{
	public InvertibleRealTransform transform;
	
	public double[] src2;

	public double[] tgt2;

	public Wrapped2DTransformAs3D( final InvertibleRealTransform transform )
	{
		this.transform = transform;
		src2 = new double[ 2 ];
		tgt2 = new double[ 2 ];
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
		System.arraycopy( source, 0, src2, 0, 2 );
		transform.apply( src2, tgt2 );
		System.arraycopy( tgt2, 0, target, 0, 2 );
		target[ 2 ] = source[ 2 ];
	}

	@Override
	public void apply( RealLocalizable source, RealPositionable target )
	{
		localizeProject( source, src2 );
		transform.apply( src2, tgt2 );
		positionProject( tgt2, target );
		target.setPosition( source.getDoublePosition( 2 ), 2 );
	}

	@Override
	public void applyInverse( double[] source, double[] target )
	{
		System.arraycopy( target, 0, tgt2, 0, 2 );
		transform.applyInverse( source, target );
		System.arraycopy( source, 0, src2, 0, 2 );
		source[ 2 ] = target[ 2 ];
	}

	@Override
	public void applyInverse( RealPositionable source, RealLocalizable target )
	{
		localizeProject( target, tgt2 );
		transform.applyInverse( src2, tgt2 );
		positionProject( src2, source );
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
	
	public static void localizeProject( final RealLocalizable src, final double[] dest )
	{
		int n = Math.min( src.numDimensions(), dest.length );
		for( int i = 0; i < n; i++ )
			dest[ i ] = src.getDoublePosition( i );
	}

	public static void positionProject( final double[] src, final RealPositionable dest )
	{
		int n = Math.min( dest.numDimensions(), src.length );
		for( int i = 0; i < n; i++ )
			dest.setPosition( src[ i ], i );
	}

}