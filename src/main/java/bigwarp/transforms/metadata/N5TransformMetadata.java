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
package bigwarp.transforms.metadata;

import org.janelia.saalfeldlab.n5.universe.metadata.AbstractN5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;

public class N5TransformMetadata extends AbstractN5Metadata {

	private CoordinateTransform<?>[] transforms;

	public N5TransformMetadata(String path, CoordinateTransform<?>[] transforms) {
		super(path);
		this.transforms = transforms;
	}

	public CoordinateTransform<?>[] getTransforms()
	{
		return transforms;
	}

}
