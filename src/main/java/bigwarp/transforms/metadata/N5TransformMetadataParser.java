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

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.scene.NgffScene;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.CoordinateTransform;

import com.google.gson.JsonParseException;

import bigwarp.transforms.NgffTransformations;

public class N5TransformMetadataParser implements N5MetadataParser<N5TransformMetadata> {

	/**
	 * The attribute keys searched for coordinate transformations by default, in order:
	 * <ul>
	 * <li>{@code coordinateTransformations} - BigWarp's own exports</li>
	 * <li>{@code ome/coordinateTransformations} - written by {@code SpacesTransforms.serialize}</li>
	 * <li>{@code ome/scene/coordinateTransformations} - an NGFF scene</li>
	 * </ul>
	 */
	public static final String[] DEFAULT_TRANSFORM_KEYS = {
			CoordinateTransform.KEY,
			"ome/" + CoordinateTransform.KEY,
			NgffScene.SCENE_KEY + "/" + CoordinateTransform.KEY };

	private final String[] transformKeys;

	public N5TransformMetadataParser() {

		this(DEFAULT_TRANSFORM_KEYS);
	}

	/**
	 * @param transformKeys
	 *            the attribute keys to search for coordinate transformations, in order
	 */
	public N5TransformMetadataParser(final String... transformKeys) {

		this.transformKeys = transformKeys;
	}

	/**
	 * Parses the coordinate transformations found under the first of this parser's transform
	 * keys that holds any at the given node.
	 */
	@Override
	public Optional<N5TransformMetadata> parseMetadata(N5Reader n5, N5TreeNode node) {

		final N5Reader n5Tform = new N5Factory().gsonBuilder( NgffTransformations.gsonBuilder() ).openReader( n5.getURI().toString() );
		for (final String key : transformKeys) {

			final CoordinateTransform<?>[] cts;
			try {
				cts = n5Tform.getAttribute(node.getPath(), key, CoordinateTransform[].class);
			} catch (final N5Exception | JsonParseException | ClassCastException e) {
				// something is at this key, but not an array of transforms
				continue;
			}

			if (cts == null || cts.length == 0)
				continue;

			// the adapter returns null for a transform it can't read (e.g. an unknown type).
			// nulls can't be dropped since transforms are later addressed by their index here
			if (Arrays.stream(cts).anyMatch(Objects::isNull)) {
				System.err.println("N5TransformMetadataParser: skipping " + key + " at \"" + node.getPath()
						+ "\", some transforms could not be read (check their \"type\")");
				continue;
			}

			return Optional.of(new N5TransformMetadata(node.getPath(), node.getPath(), key, cts));
		}

		return Optional.empty();
	}

}
