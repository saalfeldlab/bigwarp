package bigwarp.transforms;

import java.util.Arrays;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.InvertibleCoordinateTransform;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;

public class ModelTransformSolver implements TransformSolver< WrappedCoordinateTransform >
{
	private Model< ? > model;

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
		} catch (NotEnoughDataPointsException e) {
			e.printStackTrace();
		} catch (IllDefinedDataPointsException e) {
			e.printStackTrace();
		}
		return new WrappedCoordinateTransform( ( InvertibleCoordinateTransform ) model, mvgPts.length ).inverse();
	}
}
