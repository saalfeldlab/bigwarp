package bigwarp;

import bdv.gui.BigWarpViewerOptions;
import bdv.tools.bookmarks.Bookmarks;
import bigwarp.source.PlateauSphericalMaskSource;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import ij.ImagePlus;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.FinalInterval;
import net.imglib2.RealPoint;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.jdom2.JDOMException;
import org.junit.Test;
import org.xml.sax.SAXException;

public class SerializationTest
{

	public void maskTest()
	{
		PlateauSphericalMaskSource mask = PlateauSphericalMaskSource.build( new RealPoint( 3 ), new FinalInterval( 5, 10, 20 ) );
		Gson gson = BigwarpSettings.gson;
		prettyPrint( gson.toJson( mask.getRandomAccessible() ));
	}

	public void bookmarksTest()
	{
		final Bookmarks bookmarks = new Bookmarks();
		bookmarks.put( "identity", new AffineTransform3D() );
		final AffineTransform3D scale = new AffineTransform3D();
		scale.scale( 1.0, 2.0, 3.0 );
		bookmarks.put( "scale", scale );

		final AffineTransform3D translate = new AffineTransform3D();
		bookmarks.put( "translate", translate );
		translate.translate( Math.random(), Math.random(), Math.random(), Math.random() );

		Gson gson = BigwarpSettings.gson;
		final String json = gson.toJson( bookmarks );
		prettyPrint( json );
	}

	public void autoSaverTest() throws SpimDataException
	{
		final BigWarpAutoSaver saver = new BigWarpAutoSaver( null, 1000 );
		saver.autoSaveDirectory = new File("/tmp");
		final String json = BigwarpSettings.gson.toJson( saver );
		prettyPrint( json );

	}


	public void setupAssignmentsTest() throws SpimDataException, IOException
	{
		final BigWarp<?> bw = createBigWarp();
		final StringWriter stringWriter = new StringWriter();
		final JsonWriter out = new JsonWriter( stringWriter );
		new BigwarpSettings.SetupAssignmentsAdapter(bw.setupAssignments).write( out, bw.setupAssignments );

		prettyPrint( stringWriter.toString());

	}


	public void viewerPanelTest() throws SpimDataException, IOException
	{
		final BigWarp<?> bw = createBigWarp();
		final StringWriter stringWriter = new StringWriter();
		final JsonWriter out = new JsonWriter( stringWriter );
		new BigwarpSettings.BigWarpViewerPanelAdapter( bw.viewerP).write( out, bw.viewerP );

		prettyPrint( stringWriter);

	}


	public void BigWarpSettingsTest() throws SpimDataException, IOException
	{
		final BigWarp<?> bw = createBigWarp();
		bw.setAutoSaver( new BigWarpAutoSaver( bw, 10000 ) );
		final BigwarpSettings settings = bw.getSettings();

		final StringWriter stringWriter = new StringWriter();
		final JsonWriter out = new JsonWriter( stringWriter );

		settings.write( out, settings );
		settings.read( new JsonReader( new StringReader( stringWriter.toString() ) ) );
		prettyPrint( stringWriter);

	}

	/* When creating and closing multiple BigWarp instances, occassionally the comparison test fails.
	 *   It shouldn't be a problem in practice, since this only occurs during testing, but I think it indicates
	 * 	there may be a race condidtion somewhere. To duplicate the issue, run this test multiple times, and
	 * 	it should eventually fail. It is not consistent. */
	private void repeatComparison() throws Exception
	{
		for ( int i = 0; i < 20; i++ )
		{
			System.out.println( i );
			BigWarp< ? > bw = createBigWarp();

			/* Load the known good*/
			final String originalXmlSettings = "src/test/resources/compareKnownXml.bigwarp.settings.xml";
			bw.loadSettings( originalXmlSettings );

			/* save it back out*/
			final File tmpXmlFile = Files.createTempFile( "xml-settings", ".xml" ).toFile();
			bw.saveSettings( tmpXmlFile.getAbsolutePath() );

			/* compare the original and generated */
			XMLUnit.setIgnoreWhitespace( true );
			XMLUnit.setIgnoreComments( true );
			XMLAssert.assertXMLEqual( new FileReader( originalXmlSettings ), new FileReader( tmpXmlFile ) );

			bw.closeAll();
		}

	}

	@Test
	public void compareKnownXmlComparisonTest() throws SpimDataException, IOException, JDOMException, SAXException
	{
		BigWarp<?> bw = createBigWarp();

		final String originalXmlSettings = "src/test/resources/compareKnownXml.bigwarp.settings.xml";
		bw.loadSettings( originalXmlSettings );
		final BigwarpSettings settings = bw.getSettings();

		final File tmpJsonFile = Files.createTempFile( "json-settings", ".json" ).toFile();
		try ( final FileWriter fileWriter = new FileWriter( tmpJsonFile ) )
		{
			final JsonWriter out = new JsonWriter( fileWriter );
			settings.write( out, settings );
		}
		/* Ideally, we should close the instance, and get a new one for this test, but it's not currently safe to do this. See SerializationTest#repeatComparison*/
//		bw.closeAll();
//		bw = createBigWarp();

		bw.loadSettings( tmpJsonFile.getAbsolutePath() );
		final File tmpXmlFile = Files.createTempFile( "xml-settings", ".xml" ).toFile();
		bw.saveSettings( tmpXmlFile.getAbsolutePath() );
		XMLUnit.setIgnoreWhitespace( true );
		XMLUnit.setIgnoreComments( true );
		try
		{
			XMLAssert.assertXMLEqual( new FileReader( originalXmlSettings ), new FileReader( tmpXmlFile ) );
		}
		finally
		{
			bw.closeAll();
		}
	}



	private static void prettyPrint(StringWriter json) {
		prettyPrint( json.toString() );
	}

	private static void prettyPrint( String json )
	{
		final JsonElement parse = JsonParser.parseString( json );
		final JsonObject asJsonObject = parse.getAsJsonObject();

		System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson( asJsonObject ));
	}

	private static BigWarp< ? > createBigWarp() throws SpimDataException
	{
		final BigWarp.BigWarpData< Object > data = BigWarpInit.initData();
		FunctionRandomAccessible< UnsignedByteType > fimg = new FunctionRandomAccessible<>(
				3,
				( l, v ) ->  v.setOne(),
				UnsignedByteType::new );
		ImagePlus imp = ImageJFunctions.wrap( Views.interval( fimg, new FinalInterval( 32, 32, 1 ) ), "img" );
		int i = 0;
		BigWarpInit.add( data, imp, i++, 0, true );
		BigWarpInit.add( data, imp, i++, 0, false );
		BigWarpInit.add( data, imp, i++, 0, false );
		BigWarpInit.add( data, imp, i, 0, false );
		data.wrapUp();
		BigWarpViewerOptions opts = BigWarpViewerOptions.options( false );
		return new BigWarp<>( data, "bigwarp", opts, null );
	}
}
