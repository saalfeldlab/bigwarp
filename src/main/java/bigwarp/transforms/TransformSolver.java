package bigwarp.transforms;

import net.imglib2.realtransform.InvertibleRealTransform;

public interface TransformSolver<T extends InvertibleRealTransform>
{
	public T solve( final double[][] mvgPts, final double[][] tgtPts );

	public boolean wasSuccessful();

	public default String getFailureMessage() {
		return "";
	}
}
