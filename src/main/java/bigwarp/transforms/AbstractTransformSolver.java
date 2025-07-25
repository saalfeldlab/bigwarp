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
import net.imglib2.realtransform.InvertibleRealTransform;

public abstract class AbstractTransformSolver<T extends InvertibleRealTransform> implements TransformSolver<T> {

	public T solve(final LandmarkTableModel landmarkTable) {

		return solve(landmarkTable, -1);
	}

	public T solve(final LandmarkTableModel landmarkTable, final int indexChanged) {

		int numActive = landmarkTable.numActive();
		int ndims = landmarkTable.getNumdims();
		double[][] mvgPts = new double[ndims][numActive];
		double[][] tgtPts = new double[ndims][numActive];
		landmarkTable.copyLandmarks(mvgPts, tgtPts);

		return solve(mvgPts, tgtPts);
	}
}
