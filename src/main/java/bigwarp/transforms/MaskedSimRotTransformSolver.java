/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2021 Howard Hughes Medical Institute.
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

import java.util.Arrays;

import bdv.viewer.animate.SimilarityModel3D;
import bigwarp.landmarks.LandmarkTableModel;
import mpicbg.models.AbstractAffineModel2D;
import mpicbg.models.AbstractAffineModel3D;
import mpicbg.models.RigidModel2D;
import mpicbg.models.RigidModel3D;
import mpicbg.models.SimilarityModel2D;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.MaskedSimilarityTransform;
import net.imglib2.realtransform.MaskedSimilarityTransform.Interpolators;
import net.imglib2.realtransform.MaskedSimilarityTransform2D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.type.numeric.RealType;

public class MaskedSimRotTransformSolver<T extends RealType<T>> extends AbstractTransformSolver< WrappedIterativeInvertibleRealTransform< ? >>
{
	private final AbstractTransformSolver<?> baseSolver;
	private final ModelTransformSolver interpSolver;
	private RealRandomAccessible<T> lambda;
	private final double[] center;
	private final Interpolators interp;
	private final int ndims;

	public MaskedSimRotTransformSolver( AbstractTransformSolver<?> solver, RealRandomAccessible<T> lambda, double[] center, Interpolators interp )
	{
		this( 3, solver, lambda, center, interp );
	}

	public MaskedSimRotTransformSolver( int nd, AbstractTransformSolver<?> solver, RealRandomAccessible<T> lambda, double[] center, Interpolators interp )
	{
		this.ndims = nd;
		this.lambda = lambda;
		this.center = center;
		this.interp = interp;
		baseSolver = solver;

		if( interp == Interpolators.SIMILARITY )
			if( nd == 2 )
				interpSolver = new ModelTransformSolver( new SimilarityModel2D() );
			else
				interpSolver = new ModelTransformSolver( new SimilarityModel3D() );
		else
			if( nd == 2 )
				interpSolver = new ModelTransformSolver( new RigidModel2D() );
			else
				interpSolver = new ModelTransformSolver( new RigidModel3D() );

		System.out.println( this );
	}

	public void setMask( final RealRandomAccessible<T> lambda )
	{
		this.lambda = lambda;
	}

	@Override
	public String toString()
	{
		return String.format( "MaskedSolver.  center %s; interp: %s ", Arrays.toString( center ), this.interp.toString() );
	}

	public void setCenter( double[] c )
	{
		// assume center is always longer than c
		System.arraycopy( c, 0, center, 0, c.length );
	}

	public void setCenter( RealLocalizable c )
	{
		c.localize( center );
	}

	@Override
	@SuppressWarnings("rawtypes")
	public WrappedIterativeInvertibleRealTransform<?> solve( final double[][] mvgPts, final double[][] tgtPts )
	{
		final WrappedCoordinateTransform simXfm = interpSolver.solve( tgtPts, mvgPts );

		RealTransform msim;
		if ( ndims == 2 )
		{
			final AffineTransform2D sim = new AffineTransform2D();
			BigWarpTransform.affine2d( ( AbstractAffineModel2D ) interpSolver.getModel(), sim );
			msim = new MaskedSimilarityTransform2D( sim, lambda, center, interp );
		}
		else
		{
			final AffineTransform3D sim = BigWarpTransform.toAffine3D( ( AbstractAffineModel3D ) interpSolver.getModel() );
			msim = new MaskedSimilarityTransform( sim, lambda, center, interp );
		}

		final double[][] xfmTgt = transformPoints( msim, tgtPts );
		final InvertibleRealTransform baseTransform = baseSolver.solve( mvgPts, xfmTgt );

		final RealTransformSequence seq = new RealTransformSequence();
		seq.add( msim );
		seq.add( baseTransform );

		return new WrappedIterativeInvertibleRealTransform<>( MaskedTransformSolver.wrap( seq, lambda ) );
	}

	@Override
	public WrappedIterativeInvertibleRealTransform<?> solve(
			final LandmarkTableModel landmarkTable )
	{
		final int numActive = landmarkTable.numActive();
		final int nd = landmarkTable.getNumdims();
		final double[][] mvgPts = new double[ nd ][ numActive ];
		final double[][] tgtPts = new double[ nd ][ numActive ];
		landmarkTable.copyLandmarks( mvgPts, tgtPts ); // synchronized
		return solve( mvgPts, tgtPts );
	}

	private static double[][] transformPoints( RealTransform xfm, double[][] pts )
	{
		final int nd = pts.length;
		final int np = pts[0].length;

		final double[] tmp = new double[nd];
		final double[][] out = new double[ nd ][ np ];

		for( int i = 0; i < np; i++ )
		{
			for( int d = 0; d < nd; d++ )
				tmp[d] = pts[d][i];

			xfm.apply(tmp,tmp);

			for( int d = 0; d < nd; d++ )
				out[d][i] = tmp[d];
		}

		return out;
	}
}
