package bigwarp.transforms.metadata;

import org.janelia.saalfeldlab.n5.universe.metadata.AbstractN5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.CoordinateTransform;

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
