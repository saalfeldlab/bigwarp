package net.imglib2.realtransform;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealPositionable;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.type.numeric.RealType;

/**
 * Spatially-varying interpolation between two {@link RealTransform}s.
 * <p>
 * Given a {@link RealRandomAccessible} "lambda", and two transformations "a", and "b", implements the transformation
 * lambda * a(x) + (1-lambda) * b(x) for a point x.
 *
 * @author John Bogovic
 *
 * @param <T> lambda's type
 */
public class SpatiallyInterpolatedRealTransform<T extends RealType<T>> implements RealTransform {

	private RealRandomAccessible<T> lambda;

	private RealRandomAccess<T> lambdaAccess;

	private final RealTransform a;

	private final RealTransform b;

	private final RealPoint pa;

	private final RealPoint pb;

	private final double[] arrA;

	private final double[] arrB;

	public SpatiallyInterpolatedRealTransform(RealTransform a, RealTransform b, RealRandomAccessible<T> lambda) {

		assert (a.numTargetDimensions() == b.numTargetDimensions() &&
				a.numSourceDimensions() == b.numSourceDimensions());
		this.a = a;
		this.b = b;
		this.lambda = lambda;
		lambdaAccess = lambda.realRandomAccess();

		final int nd = a.numTargetDimensions();
		arrA = new double[nd];
		arrB = new double[nd];
		pa = RealPoint.wrap(arrA);
		pb = RealPoint.wrap(arrB);
	}

	@Override
	public int numSourceDimensions() {

		return a.numSourceDimensions();
	}

	@Override
	public int numTargetDimensions() {

		return a.numTargetDimensions();
	}

	@Override
	public void apply(double[] source, double[] target) {

		a.apply(source, arrA);
		b.apply(source, arrB);

//		lambdaAccess.setPosition(source);
		for( int i = 0; i < numSourceDimensions(); i++ )
			lambdaAccess.setPosition( source[ i ], i );

		final double am = lambdaAccess.get().getRealDouble();
		final double bm = (1 - am);

		for (int i = 0; i < numTargetDimensions(); i++)
			target[i] = am * arrA[i] + bm * arrB[i];
	}

	@Override
	public void apply(RealLocalizable source, RealPositionable target) {

		a.apply(source, pa);
		b.apply(source, pb);

		lambdaAccess.setPosition(source);
		final double am = lambdaAccess.get().getRealDouble();
		final double bm = (1 - am);

		for (int i = 0; i < numTargetDimensions(); i++)
			target.setPosition(am * pa.getDoublePosition(i) + bm * pb.getDoublePosition(i), i);
	}

	@Override
	public RealTransform copy() {

		return new SpatiallyInterpolatedRealTransform<T>(a.copy(), b.copy(), lambda);
	}

}
