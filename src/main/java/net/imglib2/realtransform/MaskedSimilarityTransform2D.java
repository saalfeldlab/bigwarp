package net.imglib2.realtransform;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.MaskedSimilarityTransform.Interpolators;
import net.imglib2.type.numeric.RealType;

/**
 * Spatially-varying mask for a {@link RealTransform}.
 * 
 * @param <T>
 *            mask type
 */
public class MaskedSimilarityTransform2D<T extends RealType<T>> implements RealTransform {

	private final RealRandomAccessible<T> lambda;

	private RealRandomAccess<T> lambdaAccess;

	private final AffineTransform2D transform;

	private final AffineInterpolator interpolator;

	private final double[] c;
	
	private final Interpolators interp;

	public MaskedSimilarityTransform2D(final AffineTransform2D transform, final RealRandomAccessible<T> lambda) {

		this(transform, lambda, new double[3], Interpolators.SIMILARITY);
	}

	public MaskedSimilarityTransform2D(final AffineTransform2D transform, final RealRandomAccessible<T> lambda, boolean flip) {

		this(transform, lambda, new double[3], Interpolators.SIMILARITY);
	}

	public MaskedSimilarityTransform2D(final AffineTransform2D transform, final RealRandomAccessible<T> lambda, double[] c) {

		this(transform, lambda, c, Interpolators.SIMILARITY);
	}

	public MaskedSimilarityTransform2D(final AffineTransform2D transform, final RealRandomAccessible<T> lambda, Interpolators interp) {

		this(transform, lambda, new double[3], interp);
	}

	public MaskedSimilarityTransform2D(final AffineTransform2D transform, final RealRandomAccessible<T> lambda, double[] c, Interpolators interp) {

		assert (transform.numSourceDimensions() == lambda.numDimensions());
		this.transform = transform;
		this.c = c;
		this.lambda = lambda;
		this.interp = interp;
		lambdaAccess = lambda.realRandomAccess();

		if (interp == Interpolators.SIMILARITY)
			interpolator = new SimilarityTransformInterpolator2D(transform, c);
		else
			interpolator = new RotationTransformInterpolator2D(transform, c);
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

		for (int i = 0; i < source.length; i++)
			lambdaAccess.setPosition(source[i], i);

		final double lam = lambdaAccess.get().getRealDouble();
		interpolator.get(lam).apply(source, target);
	}

	@Override
	public void apply(RealLocalizable source, RealPositionable target) {

		lambdaAccess.setPosition(source);
		final double lam = lambdaAccess.get().getRealDouble();
		interpolator.get(lam).apply(source, target);
	}

	@Override
	public RealTransform copy() {

		return new MaskedSimilarityTransform2D<T>(transform.copy(), lambda, c, interp);
	}

}
