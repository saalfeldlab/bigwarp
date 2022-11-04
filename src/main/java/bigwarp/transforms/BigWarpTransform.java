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
import java.util.Optional;

import bdv.gui.TransformTypeSelectDialog;
import bdv.util.RandomAccessibleIntervalMipmapSource;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.animate.SimilarityModel3D;
import bigwarp.BigWarpData;
import bigwarp.landmarks.LandmarkTableModel;
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
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.realtransform.Wrapped2DTransformAs3D;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;

public class BigWarpTransform
{
	private final int ndims;

	private final LandmarkTableModel tableModel;

	private String transformType;
	
	private InvertibleRealTransform currentTransform;

	private double inverseTolerance = 0.5;

	private int maxIterations = 200;

	public BigWarpTransform( final LandmarkTableModel tableModel )
	{
		this( tableModel, TransformTypeSelectDialog.TPS );
	}

	public BigWarpTransform( final LandmarkTableModel tableModel, final String transformType )
	{
		this.tableModel = tableModel;
		this.ndims = tableModel.getNumdims();
		this.transformType = transformType;
	}

	public void setTransformType( final String transformType )
	{
		this.transformType = transformType;
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

	public InvertibleRealTransform getTransformation()
	{
		return getTransformation( -1 );
	}
	
	public InvertibleRealTransform getTransformation( final int index )
	{
		InvertibleRealTransform invXfm = null;
		if( transformType.equals( TransformTypeSelectDialog.TPS ))
		{
			WrappedIterativeInvertibleRealTransform<?> tpsXfm = new TpsTransformSolver().solve( tableModel );
			tpsXfm.getOptimzer().setMaxIters(maxIterations);
			tpsXfm.getOptimzer().setTolerance(inverseTolerance);
			invXfm = tpsXfm;
		}
		else
		{
			final double[][] mvgPts;
			final double[][] tgtPts;

			final int numActive = tableModel.numActive();
			mvgPts = new double[ ndims ][ numActive ];
			tgtPts = new double[ ndims ][ numActive ];
			tableModel.copyLandmarks( mvgPts, tgtPts ); // synchronized

			invXfm = new ModelTransformSolver( getModelType() ).solve(mvgPts, tgtPts);
		}

		if( tableModel.getNumdims() == 2 )
		{
			invXfm = new Wrapped2DTransformAs3D( invXfm );
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
			// this will be possible with imglib2-realtransform-4.0.0
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
		else if( currentTransform instanceof Wrapped2DTransformAs3D )
		{
			s = ( ( Wrapped2DTransformAs3D) currentTransform ).toString();
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
		if( getTransformType().equals( TransformTypeSelectDialog.TPS ))
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

}
