package bigwarp.transforms;

import bigwarp.landmarks.LandmarkTableModel;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;

public class TpsTransformSolver implements TransformSolver< WrappedIterativeInvertibleRealTransform< ? >>
{
	private double[][] mvgPts;
	private double[][] tgtPts;

	public WrappedIterativeInvertibleRealTransform<?> solve( final double[][] mvgPts, final double[][] tgtPts )
	{
		return new WrappedIterativeInvertibleRealTransform<ThinplateSplineTransform>( 
				new ThinplateSplineTransform( 
						new ThinPlateR2LogRSplineKernelTransform( tgtPts.length, tgtPts, mvgPts )));
	}

	public WrappedIterativeInvertibleRealTransform<?> solve( 
			final LandmarkTableModel landmarkTable )
	{
		return solve( landmarkTable, -1 );
	}

	public WrappedIterativeInvertibleRealTransform<?> solve( 
			final LandmarkTableModel landmarkTable, final int indexChanged )
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

		return solve( mvgPts, tgtPts );
	}
}
