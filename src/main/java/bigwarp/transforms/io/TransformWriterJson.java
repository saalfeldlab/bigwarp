package bigwarp.transforms.io;

import bigwarp.BigWarp;
import bigwarp.BigwarpSettings;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.source.PlateauSphericalMaskRealRandomAccessible;
import bigwarp.transforms.BigWarpTransform;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class TransformWriterJson {

	public static void write(LandmarkTableModel ltm, BigWarpTransform bwTransform, File f ) {

		final JsonObject transformObj = write( ltm, bwTransform );

		try {
			final Path path = Paths.get(f.getCanonicalPath());
			final OpenOption[] options = new OpenOption[]{StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE};
			final Writer writer = Channels.newWriter(FileChannel.open(path, options), StandardCharsets.UTF_8.name());
			BigwarpSettings.gson.toJson(transformObj, writer);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void read( final File f, final BigWarp<?> bw )
	{
		try
		{
			final Path path = Paths.get(f.getCanonicalPath());
			final OpenOption[] options = new OpenOption[]{StandardOpenOption.READ};
			final Reader reader = Channels.newReader(FileChannel.open(path, options), StandardCharsets.UTF_8.name());
			JsonObject json = BigwarpSettings.gson.fromJson( reader, JsonObject.class );

			read( bw, json );

		} catch ( IOException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static JsonObject write(LandmarkTableModel ltm, BigWarpTransform bwTransform) {

		JsonObject transformObj = new JsonObject();
		transformObj.addProperty("type", bwTransform.getTransformType() );
		transformObj.addProperty("maskInterpolationType", bwTransform.getMaskInterpolationType() );
		transformObj.add("landmarks", ltm.toJson());

		if( bwTransform.isMasked() )
		{
			PlateauSphericalMaskRealRandomAccessible mask = (PlateauSphericalMaskRealRandomAccessible)bwTransform.getLambda();
			transformObj.add("mask", BigwarpSettings.gson.toJsonTree( mask ));
		}

		return transformObj;
	}

	public static void read( final BigWarp< ? > bw, final JsonObject json )
	{
		if( json.has( "landmarks" ))
			bw.getLandmarkPanel().getTableModel().fromJson( json );

		final String maskInterpolationType = json.get( "maskInterpolationType" ).getAsString();
		bw.getBwTransform().setMaskInterpolationType( maskInterpolationType );

		if( json.has( "mask" ))
		{
			JsonObject maskParams = json.get("mask").getAsJsonObject();
			final PlateauSphericalMaskRealRandomAccessible maskFromJson = BigwarpSettings.gson.fromJson( maskParams, PlateauSphericalMaskRealRandomAccessible.class );

			final PlateauSphericalMaskRealRandomAccessible mask = bw.getTransformMaskSource().getRandomAccessible();
			mask.setFalloffShape( maskFromJson.getFallOffShape() );
			mask.setSquaredRadius( maskFromJson.getSquaredRadius() );
			mask.setCenter( maskFromJson.getCenter() );
		}
	}

}
