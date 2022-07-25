package net.imglib2.realtransform;

import bdv.viewer.animate.SimilarityTransformAnimator;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealPositionable;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.type.numeric.RealType;

/**
 * Spatially-varying mask for a {@link RealTransform}.
 * <p>
 * Given a {@link RealRandomAccessible} "lambda", and a transformation "a", implements the transformation
 * lambda * a(x) + (1-lambda) * x for a point x.
 * 
 * @author John Bogovic
 *
 * @param <T> lambda's type
 */
public class MaskedSimilarityTransform<T extends RealType<T>> implements RealTransform {

	private final RealRandomAccessible<T> lambda;

	private RealRandomAccess<T> lambdaAccess;

	private final AffineTransform3D transform;

	private final SimilarityTransformInterpolator interpolator;

	private final double[] c;

	public MaskedSimilarityTransform(final AffineTransform3D transform, final RealRandomAccessible<T> lambda ) {
		this( transform, lambda, new double[3] );
	}

	public MaskedSimilarityTransform(final AffineTransform3D transform, final RealRandomAccessible<T> lambda, double[] c ) {

		assert ( transform.numSourceDimensions() == lambda.numDimensions() );
		this.transform = transform;
		this.c = c;

		this.lambda = lambda;
		lambdaAccess = lambda.realRandomAccess();
		interpolator = new SimilarityTransformInterpolator( transform, c );
	}

	@Override
	public int numSourceDimensions() {

		return transform.numSourceDimensions();
	}

	@Override
	public int numTargetDimensions() {

		return transform.numTargetDimensions();
	}

	@Override
	public void apply(double[] source, double[] target) {
		lambdaAccess.setPosition(source);
		final double lam = lambdaAccess.get().getRealDouble();
		interpolator.get( lam ).apply( source, target );
	}

	@Override
	public void apply(RealLocalizable source, RealPositionable target) {
		lambdaAccess.setPosition(source);
		final double lam = lambdaAccess.get().getRealDouble();
		interpolator.get( lam ).apply( source, target );
	}

	@Override
	public RealTransform copy() {

		return new MaskedSimilarityTransform<T>(transform.copy(), lambda, c );
	}

}
