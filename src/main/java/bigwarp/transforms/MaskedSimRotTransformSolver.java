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

import bdv.viewer.animate.SimilarityModel3D;
import bigwarp.landmarks.LandmarkTableModel;
import mpicbg.models.AbstractAffineModel3D;
import mpicbg.models.RigidModel3D;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.SpatiallyInterpolatedRealTransform;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.MaskedSimilarityTransform;
import net.imglib2.realtransform.MaskedSimilarityTransform.Interpolators;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.type.numeric.RealType;

public class MaskedSimRotTransformSolver<T extends RealType<T>> extends AbstractTransformSolver< WrappedIterativeInvertibleRealTransform< ? >>
{
	private final AbstractTransformSolver<?> baseSolver;
	private final ModelTransformSolver interpSolver;
	private final RealRandomAccessible<T> lambda;
	private final double[] center;
	private final Interpolators interp;

	public MaskedSimRotTransformSolver( AbstractTransformSolver<?> solver, RealRandomAccessible<T> lambda, double[] center, Interpolators interp )
	{
		this.lambda = lambda;
		this.center = center;
		this.interp = interp;
		baseSolver = solver;

		if( interp == Interpolators.SIMILARITY )
			interpSolver = new ModelTransformSolver( new SimilarityModel3D() );
		else
			interpSolver = new ModelTransformSolver( new RigidModel3D() );
	}

	@SuppressWarnings("rawtypes")
	public WrappedIterativeInvertibleRealTransform<?> solve( final double[][] mvgPts, final double[][] tgtPts )
	{
		WrappedCoordinateTransform simXfm = interpSolver.solve( mvgPts, tgtPts );

		AffineTransform3D sim = new AffineTransform3D();
		BigWarpTransform.affine3d( (AbstractAffineModel3D)simXfm.getTransform(), sim );

		final MaskedSimilarityTransform<?> msim = new MaskedSimilarityTransform( sim, lambda, center, interp );

		final double[][] xfmMvg = transformPoints( simXfm, mvgPts );
		final InvertibleRealTransform baseTransform = baseSolver.solve( xfmMvg, tgtPts );

		final RealTransformSequence seq = new RealTransformSequence();
		seq.add( msim );
		seq.add( baseTransform );

		return wrap( seq, lambda );
	}

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

	public static <T extends RealType<T>> WrappedIterativeInvertibleRealTransform<?> wrap( RealTransform base, RealRandomAccessible<T> lambda )
	{
		final RealTransformSequence identity = new RealTransformSequence();
		return new WrappedIterativeInvertibleRealTransform<>( new SpatiallyInterpolatedRealTransform<T>( base, identity, lambda ));
	}

	private static double[][] transformPoints( RealTransform xfm, double[][] pts )
	{
		int nd = pts.length;
		int np = pts[0].length;

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
