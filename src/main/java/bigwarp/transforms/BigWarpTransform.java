/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2022 Howard Hughes Medical Institute.
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
package bigwarp.transforms;

import java.lang.reflect.Field;
import java.util.Arrays;

import bdv.gui.TransformTypeSelectDialog;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.animate.SimilarityModel3D;
import bigwarp.BigWarpData;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.source.PlateauSphericalMaskRealRandomAccessible;
import ij.IJ;
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
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.MaskedSimilarityTransform.Interpolators;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.realtransform.InvertibleWrapped2DTransformAs3D;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.type.numeric.RealType;

public class BigWarpTransform
{
	public static final String TPS = "Thin Plate Spline";
	public static final String AFFINE = "Affine";
	public static final String SIMILARITY = "Similarity";
	public static final String ROTATION = "Rotation";
	public static final String TRANSLATION = "Translation";

	public static final String MASK_INTERP = "LINEAR";
	public static final String SIM_MASK_INTERP = "SIMILARITY";
	public static final String ROT_MASK_INTERP = "ROTATION";

	private final int ndims;

	private final LandmarkTableModel tableModel;

	private String transformType;

	private String maskInterpolationType;

	private InvertibleRealTransform currentTransform;

	private double inverseTolerance = 0.5;

	private int maxIterations = 200;

	private RealRandomAccessible<? extends RealType<?>> lambda;

	private AbstractTransformSolver<?> solver;

	public BigWarpTransform( final LandmarkTableModel tableModel )
	{
		this( tableModel, TPS );
	}

	public BigWarpTransform( final LandmarkTableModel tableModel, final String transformType )
	{
		this.tableModel = tableModel;
		this.ndims = tableModel.getNumdims();
		this.transformType = transformType;
		this.maskInterpolationType = "NONE";
		updateSolver();
	}

	public void setTransformType( final String transformType )
	{
		this.transformType = transformType;
		updateSolver();
	}

	public void setMaskInterpolationType( String maskType )
	{
		this.maskInterpolationType = maskType;
		updateSolver();
	}

	public String getMaskInterpolationType()
	{
		return maskInterpolationType;
	}

	public boolean isMasked()
	{
		return maskInterpolationType.equals( MASK_INTERP ) || maskInterpolationType.equals( ROT_MASK_INTERP ) || maskInterpolationType.equals( SIM_MASK_INTERP );
	}

	public void updateSolver()
	{
		if ( transformType.equals( TPS ) )
		{
			solver = new TpsTransformSolver();
		}
		else
		{
			solver = new ModelTransformSolver( getModelType() );
		}

		if ( maskInterpolationType.equals( MASK_INTERP ) )
		{
			solver = new MaskedTransformSolver( solver, lambda );
		}
		else if ( maskInterpolationType.equals( ROT_MASK_INTERP ) || maskInterpolationType.equals( SIM_MASK_INTERP ) )
		{
			final double[] center = new double[ 3 ];
			if ( lambda instanceof PlateauSphericalMaskRealRandomAccessible )
			{
				((PlateauSphericalMaskRealRandomAccessible) lambda).getCenter()
						.localize( center );
			}
			solver = new MaskedSimRotTransformSolver( solver, lambda, center, Interpolators.valueOf( maskInterpolationType ) );
		}
	}

	public void setInverseTolerance( double inverseTolerance )
	{
		this.inverseTolerance = inverseTolerance;
	}

	public void setInverseMaxIterations( int maxIterations )
	{
		this.maxIterations = maxIterations;
	}

	public double getInverseTolerance()
	{
		return inverseTolerance;
	}

	public int getInverseMaxIterations()
	{
		return maxIterations;
	}

	public String getTransformType()
	{
		return transformType;
	}

	public RealRandomAccessible<? extends RealType<?>> getLambda( )
	{
		return lambda;
	}

	public void setLambda( final RealRandomAccessible< ? extends RealType< ? > > lambda )
	{
		this.lambda = lambda;
	}

	public InvertibleRealTransform getTransformation()
	{
		return getTransformation( -1, true );
	}
	
	public InvertibleRealTransform getTransformation( final int index )
	{
		return getTransformation( index, true );
	}

	public InvertibleRealTransform getTransformation( final boolean force3D )
	{
		return getTransformation( -1, force3D );
	}

	public InvertibleRealTransform getTransformation( final int index, final boolean force3D )
	{
		InvertibleRealTransform invXfm = null;
		if( transformType.equals( TPS ))
		{
			WrappedIterativeInvertibleRealTransform<?> tpsXfm = (WrappedIterativeInvertibleRealTransform< ? >) solver.solve( tableModel, index );
			tpsXfm.getOptimzer().setMaxIters(maxIterations);
			tpsXfm.getOptimzer().setTolerance(inverseTolerance);
			invXfm = tpsXfm;
		}
		else
		{
			invXfm = solver.solve(tableModel, index);
		}

		if( force3D && tableModel.getNumdims() == 2 )
		{
			invXfm = new InvertibleWrapped2DTransformAs3D( invXfm );
		}

		currentTransform = invXfm;
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
		case AFFINE:
			return new AffineModel3D();
		case SIMILARITY:
			return new SimilarityModel3D();
		case ROTATION:
			return new RigidModel3D();
		case TRANSLATION:
			return new TranslationModel3D();
		}
		return null;
	}

	public AbstractAffineModel2D<?> getModel2D()
	{
		switch( transformType ){
		case AFFINE:
			return new AffineModel2D();
		case SIMILARITY:
			return new SimilarityModel2D();
		case ROTATION:
			return new RigidModel2D();
		case TRANSLATION:
			return new TranslationModel2D();
		}
		return null;
	}

	public InvertibleCoordinateTransform getCoordinateTransform()
	{
		if( !transformType.equals( TPS ))
		{
			WrappedCoordinateTransform wct = (WrappedCoordinateTransform)( unwrap2d( getTransformation() ));
			return wct.getTransform();
		}
		return null;
	}

	public InvertibleRealTransform unwrap2d( InvertibleRealTransform ixfm )
	{
		if( ixfm instanceof InvertibleWrapped2DTransformAs3D )
			return ((InvertibleWrapped2DTransformAs3D)ixfm).getTransform();
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
			return affine3d( getTpsBase(), out );
		}
		else
		{
			if( ndims == 2 )
			{
				AbstractAffineModel2D model2d = (AbstractAffineModel2D)getCoordinateTransform();
				return affine3d( model2d, out );

			}
			else if( ndims == 3 )
			{
				AbstractAffineModel3D model3d = (AbstractAffineModel3D)getCoordinateTransform();
				return affine3d( model3d, out );
			}
			else
			{
				System.err.println( "Only support 2d and 3d transformations." );
				return null;
			}
		}
	}
	
	public ThinPlateR2LogRSplineKernelTransform getTpsBase()
	{
		ThinplateSplineTransform tps = getTps();
		if( tps == null )
			return null;
		else
		{
			return tps.getKernelTransform();
		}
	}

	public ThinplateSplineTransform getTps()
	{
		if( transformType.equals( TPS ))
		{
			WrappedIterativeInvertibleRealTransform<?> wiirt = (WrappedIterativeInvertibleRealTransform<?>)( unwrap2d( getTransformation()) );
			return ((ThinplateSplineTransform)wiirt.getTransform());
		}
		return null;
	}

	public void printAffine()
	{
		if( IJ.getInstance() != null )
		{
			IJ.log( affineToString() );
			IJ.log( "" + affine3d() );
		}
		else
		{
			System.out.println( affineToString() );
			System.out.println( affine3d() );
		}
	}

	public String transformToString()
	{
		if( currentTransform == null )
		{
			return "(identity)";
		}

		String s = "";
		if ( currentTransform instanceof InverseRealTransform )
		{
			s = ( ( InverseRealTransform ) currentTransform ).toString();
		}
		else if( currentTransform instanceof WrappedCoordinateTransform )
		{
			s = (( WrappedCoordinateTransform ) currentTransform).getTransform().toString();
		}
		else if( currentTransform instanceof InvertibleWrapped2DTransformAs3D )
		{
			s = ( ( InvertibleWrapped2DTransformAs3D) currentTransform ).toString();
		}
		else
		{
			s = ( ( WrappedIterativeInvertibleRealTransform<?> ) currentTransform ).getTransform().toString();
		}
		System.out.println( s );
		return s;
	}

	public String affineToString()
	{
		String s = "";
		if( getTransformType().equals( TPS ))
		{
			double[][] affine = affinePartOfTpsHC();
			for( int r = 0; r < affine.length; r++ )
			{
				s += Arrays.toString(affine[r]).replaceAll("\\[|\\]||\\s", "");
				if( r < affine.length - 1 )
					s += "\n";
			}
		}
		else if( currentTransform instanceof WrappedCoordinateTransform )
			s = (( WrappedCoordinateTransform ) currentTransform).getTransform().toString();

		return s;
	}

	/**
	 * Returns the affine part of the thin plate spline model, 
	 * as a matrix in homogeneous coordinates.
	 * 
	 * double[i][:] contains the i^th row of the matrix.
	 * 
	 * @return the matrix as a double array
	 */
	public double[][] affinePartOfTpsHC()
	{
		int nr = 3;
		int nc = 4;
		double[][] mtx = null;
		if( ndims == 2 )
		{
			nr = 2;
			nc = 3;
			mtx = new double[2][3];
		}
		else
		{
			mtx = new double[3][4];
		}
		
		ThinPlateR2LogRSplineKernelTransform tps = getTpsBase();
		double[][] tpsAffine = tps.getAffine();
		double[] translation = tps.getTranslation();
		for( int r = 0; r < nr; r++ )
			for( int c = 0; c < nc; c++ )
			{
				if( c == (nc-1))
				{
					mtx[r][c] = translation[r];
				}
				else if( r == c )
				{
					/* the affine doesn't contain the identity "part" of the affine.
					 *	i.e., the tps builds the affine A such that
					 *	y = x + Ax 
					 *  o
					 *  y = ( A + I )x
					 */
					mtx[r][c] = 1 + tpsAffine[ r ][ c ];
				}
				else
				{
					mtx[r][c] = tpsAffine[ r ][ c ];
				}
			}
		return mtx;
	}

	public void initializeInverseParameters( BigWarpData<?> bwData )
	{
		final int N = bwData.numTargetSources();

		double val;
		double highestResDim = 0;

		for( int i = 0; i < N; i++ )
		{
//			SourceAndConverter< ? > src = bwData.sources.get( bwData.targetSourceIndices[ i ]);
			final SourceAndConverter< ? > src = bwData.getTargetSource( i );

			final String name =  src.getSpimSource().getName();
			if( name.equals( "WarpMagnitudeSource" ) ||
					name.equals( "JacobianDeterminantSource" ) ||
					name.equals( "GridSource" ) )
			{
					continue;
			}
			final AffineTransform3D xfm = new AffineTransform3D();
			src.getSpimSource().getSourceTransform( 0, 0, xfm );

			val = xfm.get( 0, 0 );
			highestResDim = val > highestResDim ?  val : highestResDim;
			val = xfm.get( 1, 1 );
			highestResDim = val > highestResDim ?  val : highestResDim;
			val = xfm.get( 2, 2 );
			highestResDim = val > highestResDim ?  val : highestResDim;
		}

		setInverseTolerance( 0.5 * highestResDim);
	}

	public static AffineTransform3D affine3d( AbstractAffineModel2D model2d, AffineTransform3D out )
	{
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
		return out;
	}

	public static AffineTransform3D affine3d( AbstractAffineModel3D model3d, AffineTransform3D out )
	{
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
		return out;
	}

	public static AffineTransform3D affine3d( ThinPlateR2LogRSplineKernelTransform tps, AffineTransform3D out )
	{
		double[][] tpsAffine = tps.getAffine();
		double[] translation = tps.getTranslation();
		int ndims = tps.getNumDims();

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
		return out;
	}

}
