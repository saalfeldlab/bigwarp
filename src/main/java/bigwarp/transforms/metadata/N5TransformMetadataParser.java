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

import java.util.Optional;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;

import bigwarp.transforms.NgffTransformations;

public class N5TransformMetadataParser implements N5MetadataParser<N5TransformMetadata> {

	@Override
	public Optional<N5TransformMetadata> parseMetadata(N5Reader n5, N5TreeNode node) {

		final N5Reader n5Tform = new N5Factory().gsonBuilder( NgffTransformations.gsonBuilder() ).openReader( n5.getURI().toString() );
		final CoordinateTransform<?>[] cts = n5Tform.getAttribute(node.getPath(), "coordinateTransformations", CoordinateTransform[].class);

		if( cts != null )
			return Optional.of( new N5TransformMetadata(node.getPath(), cts));
		else
			return Optional.empty();
	}

}
