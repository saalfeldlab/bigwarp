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

import bigwarp.landmarks.LandmarkTableModel;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;

public class RegTpsTransformSolver implements TransformSolver< WrappedIterativeInvertibleRealTransform< ? >>
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
}
