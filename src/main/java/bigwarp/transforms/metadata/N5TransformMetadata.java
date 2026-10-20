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
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.CoordinateTransform;

public class N5TransformMetadata extends AbstractN5Metadata {

	private final CoordinateTransform<?>[] transforms;

	/** The group the transforms were found at. */
	private final String group;

	/**
	 * The attribute key the transforms were found under, e.g.
	 * {@code coordinateTransformations} or {@code ome/scene/coordinateTransformations},
	 * or {@code null} if unknown.
	 */
	private final String key;

	/**
	 * The index of this transform within that key's array, or {@code -1} for
	 * group-level metadata that holds every transform at the key.
	 */
	private final int index;

	public N5TransformMetadata(String path, CoordinateTransform<?>[] transforms) {
		this(path, path, null, -1, transforms);
	}

	public N5TransformMetadata(String path, String group, String key, CoordinateTransform<?>[] transforms) {
		this(path, group, key, -1, transforms);
	}

	public N5TransformMetadata(String path, String group, String key, int index, CoordinateTransform<?>[] transforms) {
		super(path);
		this.group = group;
		this.key = key;
		this.index = index;
		this.transforms = transforms;
	}

	public CoordinateTransform<?>[] getTransforms()
	{
		return transforms;
	}

	public String getGroup()
	{
		return group;
	}

	public String getKey()
	{
		return key;
	}

	public int getIndex()
	{
		return index;
	}

	/**
	 * The attribute path addressing this single transform, i.e. {@code key[index]}
	 * (for example {@code ome/scene/coordinateTransformations[0]}), or {@code null}
	 * when this is group-level metadata or the key is unknown.
	 *
	 * @return the attribute path, or null
	 */
	public String getAttributePath()
	{
		if (key == null || index < 0)
			return null;
		return key + "[" + index + "]";
	}

}
