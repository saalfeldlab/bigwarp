/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2025 Howard Hughes Medical Institute.
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

import bigwarp.landmarks.LandmarkTableModel;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.RandomAccessible;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.SpatiallyInterpolatedRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.type.numeric.RealType;

public class MaskedTransformSolver<T extends RealType<T>, S extends AbstractTransformSolver<?> > extends AbstractTransformSolver< WrappedIterativeInvertibleRealTransform< ? >>
{
	private final S solver;

	private RealRandomAccessible<T> lambda;

	public MaskedTransformSolver( final S solver, final RealRandomAccessible<T> lambda )
	{
		this.lambda = lambda;
		this.solver = solver;
	}

	public S getSolver()
	{
		return solver;
	}

	public void setMask( final RealRandomAccessible<T> lambda )
	{
		this.lambda = lambda;
	}

	@Override
	public WrappedIterativeInvertibleRealTransform<?> solve( final double[][] mvgPts, final double[][] tgtPts )
	{
		return wrap( solver.solve( mvgPts, tgtPts ), lambda );
	}

	@Override
	public WrappedIterativeInvertibleRealTransform< ? > solve( final LandmarkTableModel landmarkTable )
	{
		return wrap( solver.solve( landmarkTable ), lambda );
	}

	@Override
	public WrappedIterativeInvertibleRealTransform< ? > solve( final LandmarkTableModel landmarkTable, final int indexChanged )
	{
		return wrap( solver.solve( landmarkTable, indexChanged ), lambda );
	}

	public static <T extends RealType< T >> WrappedIterativeInvertibleRealTransform< ? > wrap( RealTransform base, RealRandomAccessible< T > lambda )
	{
		final RealTransformSequence identity = new RealTransformSequence();
		return new WrappedIterativeInvertibleRealTransform<>(
				new SpatiallyInterpolatedRealTransform< T >( base, identity, lambda ) );
	}
}
