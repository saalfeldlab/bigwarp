package bigwarp.transforms.io;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import bdv.gui.TransformTypeSelectDialog;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.source.PlateauSphericalMaskRealRandomAccessible;
import bigwarp.transforms.BigWarpTransform;

public class TransformWriterJson {

	private final Gson gson;

	public TransformWriterJson()
	{
		gson = new Gson();
	}

	public void write(LandmarkTableModel ltm, BigWarpTransform bwTransform, File f ) {

		JsonObject transformObj = new JsonObject();
		transformObj.add("type", new JsonPrimitive( bwTransform.getTransformType() ));
		transformObj.add("landmarks", ltm.toJson());

		if( bwTransform.getTransformType().equals( TransformTypeSelectDialog.MASKEDTPS) )
		{
			JsonObject maskObj = new JsonObject();
			PlateauSphericalMaskRealRandomAccessible mask = (PlateauSphericalMaskRealRandomAccessible)bwTransform.getLambda();
			maskObj.add("type", new JsonPrimitive("gaussianSphericalPlateau"));
			maskObj.add("center", gson.toJsonTree(mask.getCenter().positionAsDoubleArray()));
			maskObj.add("squaredRadius", new JsonPrimitive(mask.getSquaredRadius()));
			maskObj.add("squaredSigma", new JsonPrimitive(mask.getSquaredSigma()));
			transformObj.add( "mask", maskObj );
		}

		try {
			final Path path = Paths.get(f.getCanonicalPath());
			final OpenOption[] options = new OpenOption[]{StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE};
			final Writer writer = Channels.newWriter(FileChannel.open(path, options), StandardCharsets.UTF_8.name());
			gson.toJson(transformObj, writer);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
