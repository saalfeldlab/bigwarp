package net.imglib2.realtransform;

import net.imglib2.util.LinAlgHelpers;

import bdv.util.Affine3DHelpers;
import bdv.viewer.animate.AbstractTransformAnimator;

public class RotationTransformInterpolator extends AbstractTransformAnimator
{
	private final double[] qStart;

	private final double[] qDiff;

	private final double[] p;

	private final double[] pDiff;

	public RotationTransformInterpolator(final AffineTransform3D transform, final double[] p) {
		super(1);
		AffineTransform3D transformEnd = new AffineTransform3D();
		transformEnd.set(transform);

		this.p = p;
		qStart = new double[]{1, 0, 0, 0};
		final double[] qStartInv = new double[4];
		final double[] qEnd = new double[4];
		final double[] qEndInv = new double[4];
		qDiff = new double[4];
		LinAlgHelpers.quaternionInvert(qStart, qStartInv);

		Affine3DHelpers.extractRotation(transformEnd, qEnd);
		LinAlgHelpers.quaternionInvert(qEnd, qEndInv);

		LinAlgHelpers.quaternionMultiply(qStartInv, qEnd, qDiff);
		if (qDiff[0] < 0)
			LinAlgHelpers.scale(qDiff, -1, qDiff);

		// translation needed to from reconstructed to target transformation
		pDiff = new double[3];
		double[] tmp = new double[3];
		get(1).apply( p, tmp);
		transform.apply(p, pDiff);
		LinAlgHelpers.subtract(pDiff, tmp, pDiff);
	}

	@Override
	public AffineTransform3D get(final double t) {

		final double[] qDiffCurrent = new double[4];
		final double[] qCurrent = new double[4];
		LinAlgHelpers.quaternionPower(qDiff, t, qDiffCurrent);
		LinAlgHelpers.quaternionMultiply(qStart, qDiffCurrent, qCurrent);

		final double[][] Rcurrent = new double[3][3];
		LinAlgHelpers.quaternionToR(qCurrent, Rcurrent);

		final double[][] m = new double[3][4];
		for (int r = 0; r < 3; ++r) {
			for (int c = 0; c < 3; ++c)
				m[r][c] = Rcurrent[r][c];
		}

		final AffineTransform3D transform = new AffineTransform3D();
		transform.set(m);

		double[] pCurrent = new double[3];
		double[] pTgt = new double[3];
		transform.apply(p, pCurrent);

		LinAlgHelpers.scale( pDiff, t, pTgt );
		LinAlgHelpers.add( p, pTgt, pTgt );
		LinAlgHelpers.subtract( pTgt, pCurrent, pTgt );
		transform.translate( pTgt );

		return transform;

	}

}