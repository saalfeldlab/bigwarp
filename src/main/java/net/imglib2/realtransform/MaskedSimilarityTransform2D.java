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
