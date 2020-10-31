package bigwarp.transforms;

import java.lang.reflect.Field;
import java.util.Arrays;

import bdv.gui.TransformTypeSelectDialog;
import bdv.viewer.animate.SimilarityModel3D;
import bigwarp.landmarks.LandmarkTableModel;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import mpicbg.models.AbstractAffineModel2D;
import mpicbg.models.AbstractAffineModel3D;
import mpicbg.models.AffineModel2D;
import mpicbg.models.AffineModel3D;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.InvertibleCoordinateTransform;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.RigidModel2D;
import mpicbg.models.RigidModel3D;
import mpicbg.models.SimilarityModel2D;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.TranslationModel3D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.realtransform.Wrapped2DTransformAs3D;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;

public class BigWarpTransform
{
	private final int ndims;

	private final LandmarkTableModel tableModel;

	private final String transformType;

	public BigWarpTransform( final LandmarkTableModel tableModel, final String transformType )
	{
		this.tableModel = tableModel;
		this.ndims = tableModel.getNumdims();
		this.transformType = transformType;
	}
	
	public InvertibleRealTransform getTransformation()
	{
		return getTransformation( -1 );
	}
	
	public InvertibleRealTransform getTransformation( final int index )
	{
		InvertibleRealTransform invXfm = null;
		if( transformType.equals( TransformTypeSelectDialog.TPS ))
		{
			invXfm = new TpsTransformSolver().solve( tableModel );
		}
		else
		{
			Model<?> model = getModelType();
			fitModel(model);
			int nd = tableModel.getNumdims();
			invXfm = new WrappedCoordinateTransform( (InvertibleCoordinateTransform) model, nd ).inverse();
		}

		if( tableModel.getNumdims() == 2 )
		{
			invXfm = new Wrapped2DTransformAs3D( invXfm );
		}

		return invXfm;
	}

	public void fitModel( final Model<?> model )
	{
		int numActive = tableModel.numActive();

		double[][] mvgPts = new double[ ndims ][ numActive ];
		double[][] tgtPts = new double[ ndims ][ numActive ];

		tableModel.copyLandmarks( mvgPts, tgtPts );

		double[] w = new double[ numActive ];
		Arrays.fill( w, 1.0 );

		try {
			model.fit( mvgPts, tgtPts, w );
		} catch (NotEnoughDataPointsException e) {
			e.printStackTrace();
		} catch (IllDefinedDataPointsException e) {
			e.printStackTrace();
		}
	}

	public Model<?> getModelType()
	{
		if( tableModel.getNumdims() == 2 )
			return getModel2D();
		else
			return getModel3D();
	}

	public AbstractAffineModel3D<?> getModel3D()
	{
		switch( transformType ){
		case TransformTypeSelectDialog.AFFINE:
			return new AffineModel3D();
		case TransformTypeSelectDialog.SIMILARITY:
			return new SimilarityModel3D();
		case TransformTypeSelectDialog.ROTATION:
			return new RigidModel3D();
		case TransformTypeSelectDialog.TRANSLATION:
			return new TranslationModel3D();
		}
		return null;
	}

	public AbstractAffineModel2D<?> getModel2D()
	{
		switch( transformType ){
		case TransformTypeSelectDialog.AFFINE:
			return new AffineModel2D();
		case TransformTypeSelectDialog.SIMILARITY:
			return new SimilarityModel2D();
		case TransformTypeSelectDialog.ROTATION:
			return new RigidModel2D();
		case TransformTypeSelectDialog.TRANSLATION:
			return new TranslationModel2D();
		}
		return null;
	}

	public InvertibleCoordinateTransform getCoordinateTransform()
	{
		if( !transformType.equals( TransformTypeSelectDialog.TPS ))
		{
			WrappedCoordinateTransform wct = (WrappedCoordinateTransform)( unwrap2d( getTransformation() ));
			return wct.getTransform();
		}
		return null;
	}

	public InvertibleRealTransform unwrap2d( InvertibleRealTransform ixfm )
	{
		if( ixfm instanceof Wrapped2DTransformAs3D )
			return ((Wrapped2DTransformAs3D)ixfm).getTransform();
		else
			return ixfm;
	}

	/**
	 * Returns an AffineTransform3D that represents the the transform if the transform
	 * is linear, or is the affine part of the transform if it is non-linear.
	 * 
	 * Returns a valid transform even if the estimated transformation is 2d.
	 * 
	 * @return the affine transform
	 */
	public AffineTransform3D affine3d()
	{
		AffineTransform3D out = new AffineTransform3D();
		if( transformType.equals( TransformTypeSelectDialog.TPS ))
		{
			double[][] tpsAffine = getTpsBase().getAffine();
			double[] translation = getTpsBase().getTranslation();

			double[] affine = new double[ 12 ];
			if( ndims == 2 )
			{
				affine[ 0 ] = 1 + tpsAffine[ 0 ][ 0 ];
				affine[ 1 ] = tpsAffine[ 0 ][ 1 ];
				// dont set affine 2
				affine[ 3 ] = translation[ 0 ];

				affine[ 4 ] = tpsAffine[ 1 ][ 0 ];
				affine[ 5 ] = 1 + tpsAffine[ 1 ][ 1 ];
				// dont set 6
				affine[ 7 ] = translation[ 1 ];

				// dont set 8,9,11
				affine[ 10 ] = 1.0;
			}
			else
			{
				affine[ 0 ] = 1 + tpsAffine[ 0 ][ 0 ];
				affine[ 1 ] = tpsAffine[ 0 ][ 1 ];
				affine[ 2 ] = tpsAffine[ 0 ][ 2 ];
				affine[ 3 ] = translation[ 0 ];

				affine[ 4 ] = tpsAffine[ 1 ][ 0 ];
				affine[ 5 ] = 1 + tpsAffine[ 1 ][ 1 ];
				affine[ 6 ] = tpsAffine[ 1 ][ 2 ];
				affine[ 7 ] = translation[ 1 ];

				affine[ 8 ] = tpsAffine[ 2 ][ 0 ];
				affine[ 9 ] = tpsAffine[ 2 ][ 1 ];
				affine[ 10 ] = 1 + tpsAffine[ 2 ][ 2 ];
				affine[ 11 ] = translation[ 2 ];
			}

			out.set( affine );
		}
		else
		{
			if( ndims == 2 )
			{
				AbstractAffineModel2D model2d = (AbstractAffineModel2D)getCoordinateTransform();

				double[][] mtx = new double[2][3];
				model2d.toMatrix( mtx );

				double[] affine = new double[ 12 ];
				affine[ 0 ] = mtx[ 0 ][ 0 ];
				affine[ 1 ] = mtx[ 0 ][ 1 ];
				// dont set affine 2
				affine[ 3 ] = mtx[ 0 ][ 2 ];

				affine[ 4 ] = mtx[ 1 ][ 0 ];
				affine[ 5 ] = mtx[ 1 ][ 1 ];
				// dont set affine 6
				affine[ 7 ] = mtx[ 1 ][ 2 ];

				// dont set affines 8,9,11
				affine[ 10 ] = 1.0;

				out.set( affine );

			}
			else if( ndims == 3 )
			{
				AbstractAffineModel3D model3d = (AbstractAffineModel3D)getCoordinateTransform();

				double[][] mtx = new double[3][4];
				model3d.toMatrix( mtx );

				double[] affine = new double[ 12 ];
				affine[ 0 ] = mtx[ 0 ][ 0 ];
				affine[ 1 ] = mtx[ 0 ][ 1 ];
				affine[ 2 ] = mtx[ 0 ][ 2 ];
				affine[ 3 ] = mtx[ 0 ][ 3 ];

				affine[ 4 ] = mtx[ 1 ][ 0 ];
				affine[ 5 ] = mtx[ 1 ][ 1 ];
				affine[ 6 ] = mtx[ 1 ][ 2 ];
				affine[ 7 ] = mtx[ 1 ][ 3 ];

				affine[ 8 ] = mtx[ 2 ][ 0 ];
				affine[ 9 ] = mtx[ 2 ][ 1 ];
				affine[ 10 ] = mtx[ 2 ][ 2 ];
				affine[ 11 ] = mtx[ 2 ][ 3 ];

				out.set( affine );
			}
			else
			{
				System.err.println( "Only support 2d and 3d transformations." );
				return null;
			}
		}
		return out;
	}
	
	public ThinPlateR2LogRSplineKernelTransform getTpsBase()
	{
		ThinplateSplineTransform tps = getTps();
		if( tps == null )
			return null;
		else
		{
			// TODO add get method in ThinplateSplineTransform to avoid reflection here
			final Class< ThinplateSplineTransform > c_tps = ThinplateSplineTransform.class;
			try
			{
				final Field tpsField = c_tps.getDeclaredField( "tps" );
				tpsField.setAccessible( true );
				ThinPlateR2LogRSplineKernelTransform tpsbase = (ThinPlateR2LogRSplineKernelTransform)tpsField.get(  tps );
				tpsField.setAccessible( false );

				return tpsbase;
			}
			catch(Exception e )
			{
				e.printStackTrace();
				return null;
			}
		}
	}

	public ThinplateSplineTransform getTps()
	{
		if( transformType.equals( TransformTypeSelectDialog.TPS ))
		{
			WrappedIterativeInvertibleRealTransform<?> wiirt = (WrappedIterativeInvertibleRealTransform<?>)( unwrap2d( getTransformation()) );
			return ((ThinplateSplineTransform)wiirt.getTransform());
		}
		return null;
	}

}
