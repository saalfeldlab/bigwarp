package bigwarp.transforms;

import java.util.Arrays;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.InvertibleCoordinateTransform;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;

public class ModelTransformSolver implements TransformSolver< WrappedCoordinateTransform >
{
	private Model< ? > model;

	private boolean success = false;

	private String failureMessage = "";

	public ModelTransformSolver( Model< ? > model )
	{
		this.model = model;
	}

	public WrappedCoordinateTransform solve( final double[][] mvgPts, final double[][] tgtPts )
	{
		double[] w = new double[ mvgPts[ 0 ].length ];
		Arrays.fill( w, 1.0 );

		try {
			model.fit( mvgPts, tgtPts, w );
			return new WrappedCoordinateTransform( ( InvertibleCoordinateTransform ) model, mvgPts.length ).inverse();
		} catch (NotEnoughDataPointsException e) {
			failureMessage = "Not enough data points";
			success = false;
		} catch (IllDefinedDataPointsException e) {
			failureMessage = "Ill-defined data points";
			success = false;
		}
		return null;
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
