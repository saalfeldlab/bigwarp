package bigwarp.transforms;

import bigwarp.landmarks.LandmarkTableModel;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;

public class TpsTransformSolver implements TransformSolver< WrappedIterativeInvertibleRealTransform< ? >>
{
	private double[][] mvgPts;
	private double[][] tgtPts;

	private boolean success = false;

	private String failureMessage = "";

	public WrappedIterativeInvertibleRealTransform<?> solve( final double[][] mvgPts, final double[][] tgtPts )
	{
		WrappedIterativeInvertibleRealTransform<ThinplateSplineTransform> transform = new WrappedIterativeInvertibleRealTransform<ThinplateSplineTransform>( 
				new ThinplateSplineTransform( 
						new ThinPlateR2LogRSplineKernelTransform( tgtPts.length, tgtPts, mvgPts )));

		success = true;
		return transform;
	}

	public WrappedIterativeInvertibleRealTransform<?> rtsolve( 
			final LandmarkTableModel landmarkTable )
	{
		return solve( landmarkTable, -1 );
	}

	public WrappedIterativeInvertibleRealTransform<?> solve( 
			final LandmarkTableModel landmarkTable, final int indexChanged )
	{
		synchronized( landmarkTable )
		{
			int numActive = landmarkTable.numActive();
			int ndims = landmarkTable.getNumdims();

			if( mvgPts == null || mvgPts[0].length != numActive )
			{
				mvgPts = new double[ ndims ][ numActive ];
				tgtPts = new double[ ndims ][ numActive ];
				landmarkTable.copyLandmarks( mvgPts, tgtPts );
			}
			else if( indexChanged >= 0 )
			{
				landmarkTable.copyLandmarks( indexChanged, mvgPts, tgtPts );
			}
		}

		return solve( mvgPts, tgtPts );
	}

	@Override
	public boolean wasSuccessful() {
		return success;
	}

	@Override
	public String getFailureMessage() {
		return failureMessage;
	}
}
