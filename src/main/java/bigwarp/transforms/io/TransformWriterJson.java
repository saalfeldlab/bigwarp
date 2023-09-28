package bigwarp.transforms.io;


import bigwarp.BigWarp;
import bigwarp.BigwarpSettings;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.source.PlateauSphericalMaskRealRandomAccessible;
import bigwarp.transforms.BigWarpTransform;
import com.google.gson.JsonObject;

import bdv.util.BoundedRange;

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
		} catch (final IOException e) {
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
			final JsonObject json = BigwarpSettings.gson.fromJson( reader, JsonObject.class );

			read( bw, json );

		} catch ( final IOException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static JsonObject write(LandmarkTableModel ltm, BigWarpTransform bwTransform) {

		final JsonObject transformObj = new JsonObject();
		transformObj.addProperty("type", bwTransform.getTransformType() );
		transformObj.add("landmarks", ltm.toJson());

		if( bwTransform.isMasked() )
		{
			final JsonObject maskObj = new JsonObject();
			if (bwTransform.getLambda() instanceof PlateauSphericalMaskRealRandomAccessible) {
				final PlateauSphericalMaskRealRandomAccessible mask = (PlateauSphericalMaskRealRandomAccessible)bwTransform.getLambda();
				maskObj.add("parameters", BigwarpSettings.gson.toJsonTree(mask));
			}
			maskObj.add("range", BigwarpSettings.gson.toJsonTree(bwTransform.getMaskIntensityBounds()));
			maskObj.addProperty("interpolationType", bwTransform.getMaskInterpolationType() );
			transformObj.add("mask", maskObj);
		}

		return transformObj;
	}

	public static void read( final BigWarp< ? > bw, final JsonObject json )
	{
		if( json.has( "landmarks" ))
		{
			final int nd = json.get("landmarks").getAsJsonObject().get("numDimensions").getAsInt();
			if( bw.numDimensions() != nd )
				bw.changeDimensionality(nd == 2);

			bw.getLandmarkPanel().getTableModel().fromJson( json );
		}

		if( json.has( "mask" ))
		{
			final JsonObject maskParams = json.get("mask").getAsJsonObject();

			if( maskParams.has("parameters"))
			{
				bw.addTransformMaskSource();
				final JsonObject paramsObj = maskParams.get("parameters").getAsJsonObject();
				final PlateauSphericalMaskRealRandomAccessible maskFromJson = BigwarpSettings.gson.fromJson( paramsObj, PlateauSphericalMaskRealRandomAccessible.class );
				bw.setTransformMaskProperties(
						maskFromJson.getFallOffShape(),
						maskFromJson.getSquaredRadius(),
						maskFromJson.getCenter().positionAsDoubleArray());
			}else
				bw.connectMaskSource();

			bw.setTransformMaskType(maskParams.get("interpolationType").getAsString());

			if( maskParams.has("range"))
			{
				final BoundedRange maskRange = BigwarpSettings.gson.fromJson(maskParams.get("range"), BoundedRange.class);
				bw.getBwTransform().setMaskIntensityBounds(maskRange.getMin(), maskRange.getMax());
				bw.setTransformMaskRange(maskRange.getMin(), maskRange.getMax());
			}
		}

		if( json.has( "type" ))
			bw.setTransformType(json.get("type").getAsString());

	}

}
