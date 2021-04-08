package bigwarp.transforms;

import net.imglib2.realtransform.InvertibleRealTransform;

public interface TransformSolver<T extends InvertibleRealTransform>
{
	public T solve( final double[][] mvgPts, final double[][] tgtPts );
}
