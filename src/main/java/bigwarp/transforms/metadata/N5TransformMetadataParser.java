package bigwarp.transforms.metadata;

import java.util.Optional;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.CoordinateTransform;

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
