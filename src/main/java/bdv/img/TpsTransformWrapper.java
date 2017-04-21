package bdv.img;

import java.io.File;
import java.io.Serializable;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.realtransform.InvertibleRealTransform;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;

public class TpsTransformWrapper implements InvertibleRealTransform, Serializable 
{
	private static final long serialVersionUID = -4960535139988240362L;

	protected int ndims;
	
	protected ThinPlateR2LogRSplineKernelTransform tps;

	private int invMaxIters = 3000;

	protected double invTolerance = 0.05;

	/**
	 * Initialize as identity transform
	 * 
	 * @param ndims number of dimensions this tps acts on
	 */
	public TpsTransformWrapper( int ndims ){
		this.ndims = ndims;
	}
	
	public TpsTransformWrapper( int ndims, final ThinPlateR2LogRSplineKernelTransform tps )
	{
		this( ndims );
		setTps( tps );
	}
	
	public void setTps( final ThinPlateR2LogRSplineKernelTransform tps )
	{
		assert( tps.getNumDims() == 2 || tps.getNumDims() == 3 );

		this.tps = tps;
	}

	public ThinPlateR2LogRSplineKernelTransform getTps()
	{
		return tps;
	}

	@Override
	public int numSourceDimensions()
	{
		return ndims;
	}

	@Override
	public int numTargetDimensions()
	{
		return ndims;
	}

	public void setInvMaxIters(int invMaxIters)
	{
		this.invMaxIters = invMaxIters;
	}

	public void setInvTolerance( double tolerance )
	{
		this.invTolerance = tolerance;
	}

	@Override
	public void apply( final double[] source, final double[] target )
	{ 
		double[] guess = tps.initialGuessAtInverse( source );
		double error = tps.inverseTol( source, guess, invTolerance, invMaxIters );
//		System.out.println( "error: " + error );
		System.arraycopy( guess, 0, target, 0, target.length );
	}

	@Override
	public void apply( final float[] source, final float[] target ){}

	@Override
	public void apply( final RealLocalizable source, final RealPositionable target )
	{
		double[] srcd = new double[ source.numDimensions() ];
		double[] tgtd = new double[ target.numDimensions() ];

		source.localize( srcd );
		apply( srcd, tgtd );

		for( int i = 0; i < target.numDimensions(); i++ )
			target.setPosition( tgtd[ i ], i );
	}

	@Override
	public void applyInverse( final double[] source, final double[] target )
	{
		if ( tps == null )
		{
			for ( int d = 0; d < target.length; ++d )
				source[ d ] = target[ d ];

			return;
		}

		double[] pt = new double[ tps.getNumDims() ];
		for ( int d = 0; d < tps.getNumDims(); ++d )
			pt[ d ] = target[ d ];

		double[] ptxfm = tps.apply( pt );

		for ( int d = 0; d < tps.getNumDims(); ++d )
			source[ d ] = ptxfm[ d ];
	}

	@Override
	public void applyInverse( final float[] source, final float[] target )
	{
		if ( tps == null )
		{
			for ( int d = 0; d < target.length; ++d )
				source[ d ] = target[ d ];

			return;
		}

		double[] pt = new double[ tps.getNumDims() ];
		for ( int d = 0; d < tps.getNumDims(); ++d )
			pt[ d ] = target[ d ];

		double[] ptxfm = tps.apply( pt );

		for ( int d = 0; d < tps.getNumDims(); ++d )
			source[ d ] = (float) ptxfm[ d ];
	}

	@Override
	public void applyInverse( final RealPositionable source, final RealLocalizable target )
	{
		if( tps == null )
		{
			for ( int d = 0; d < target.numDimensions(); ++d )
				source.setPosition( target.getDoublePosition( d ), d );
				
			return;
		}
		
		double[] pt = new double[ tps.getNumDims() ];
		for ( int d = 0; d < tps.getNumDims(); ++d )
			pt[ d ] = target.getDoublePosition( d );
		
		double[] ptxfm = tps.apply( pt );
		
		for ( int d = 0; d < tps.getNumDims(); ++d )
			source.setPosition( ptxfm[ d ], d);
		
	}

	@Override
	public InvertibleRealTransform inverse()
	{
		return null;
	}

	@Override
	public InvertibleRealTransform copy()
	{
		if( tps == null )
			return new TpsTransformWrapper( this.ndims );
		else
			return new TpsTransformWrapper( this.ndims, this.tps );
	}

	public void write( File f ){}
	
	public void read( File f ){}
}
