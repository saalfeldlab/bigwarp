package bigwarp;

import bdv.tools.bookmarks.Bookmarks;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.MinMaxGroup;
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.state.SourceGroup;
import bdv.viewer.state.XmlIoViewerState;
import bigwarp.loader.ImagePlusLoader;
import bigwarp.source.PlateauSphericalMaskSource;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import ij.ImagePlus;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.FinalInterval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.jdom2.JDOMException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import static bigwarp.BigWarpTestUtils.createTemp3DImage;

public class SerializationTest
{

	private BigWarp< ? > bw;

	@After
	public void	after() {
		if (bw != null) {
			bw.closeAll();
			bw = null;
		}
	}

	@Test
	public void maskTest()
	{
		PlateauSphericalMaskSource mask = PlateauSphericalMaskSource.build( new RealPoint( 3 ), new FinalInterval( 5, 10, 20 ) );
		Gson gson = BigwarpSettings.gson;
		final JsonElement actual = gson.toJsonTree( mask.getRandomAccessible() );

		final JsonObject expected = new JsonObject();
		expected.addProperty( "fallOffShape", "COSINE" );
		expected.addProperty( "squaredRadius", 64.0 );
		expected.addProperty( "squaredSigma", 100.0 );
		final JsonArray center = new JsonArray( 3 );
		center.add( 0.0 );
		center.add( 0.0 );
		center.add( 0.0 );
		expected.add( "center", center );

		BigWarpTestUtils.assertJsonDiff( expected, actual );
	}

	@Test
	public void bookmarksTest()
	{
		final Bookmarks bookmarks = new Bookmarks();
		final AffineTransform3D identity = new AffineTransform3D();
		bookmarks.put( "identity", identity );
		final AffineTransform3D scale = new AffineTransform3D();
		scale.scale( 1.0, 2.0, 3.0 );
		bookmarks.put( "scale", scale );

		final AffineTransform3D translate = new AffineTransform3D();
		bookmarks.put( "translate", translate );
		translate.translate( Math.random(), Math.random(), Math.random(), Math.random() );

		Gson gson = BigwarpSettings.gson;
		final JsonElement actual = gson.toJsonTree( bookmarks );

		final JsonObject expected = new JsonObject();
		final JsonObject bookmarksObj = new JsonObject();
		expected.add( "bookmarks", bookmarksObj );
		final JsonArray identityArray = new JsonArray( 3 );
		for ( final double val : identity.getRowPackedCopy() )
		{
			identityArray.add( val );
		}
		final JsonArray scaleArray = new JsonArray( 3 );
		for ( final double val : scale.getRowPackedCopy() )
		{
			scaleArray.add( val );
		}
		final JsonArray translateArray = new JsonArray( 3 );
		for ( final double val : translate.getRowPackedCopy() )
		{
			translateArray.add( val );
		}
		bookmarksObj.add( "identity", identityArray );
		bookmarksObj.add( "scale", scaleArray );
		bookmarksObj.add( "translate", translateArray );

		BigWarpTestUtils.assertJsonDiff( expected, actual );
	}

	@Test
	public void autoSaverTest()
	{
		final BigWarpAutoSaver saver = new BigWarpAutoSaver( null, 1000 );
		saver.autoSaveDirectory = new File( "/tmp" );
		final JsonElement actual = BigwarpSettings.gson.toJsonTree( saver );

		final JsonObject expected = new JsonObject();
		expected.addProperty( "period", 1000 );
		expected.addProperty( "location", "/tmp" );
		saver.stop();

		BigWarpTestUtils.assertJsonDiff( expected, actual );
	}

	@Test
	public void setupAssignmentsTest() throws SpimDataException, IOException, URISyntaxException
	{
		bw = BigWarpTestUtils.createBigWarp( true, false, false, false );

		final PipedWriter writer = new PipedWriter();
		final PipedReader in = new PipedReader( writer, 1000 );

		final JsonWriter out = new JsonWriter( writer );
		new BigwarpSettings.SetupAssignmentsAdapter( bw.setupAssignments ).write( out, bw.setupAssignments );

		out.close();
		writer.close();
		final JsonElement actual = JsonParser.parseReader( in );

		final JsonObject expected = new JsonObject();
		final JsonObject setups = new JsonObject();
		expected.add( "ConverterSetups", setups );
		final List< MinMaxGroup > minMaxGroups = bw.setupAssignments.getMinMaxGroups();
		for ( final ConverterSetup setup : bw.setupAssignments.getConverterSetups() )
		{
			final JsonObject setupObj = new JsonObject();
			setupObj.addProperty( "min", setup.getDisplayRangeMin() );
			setupObj.addProperty( "max", setup.getDisplayRangeMax() );
			setupObj.addProperty( "color", setup.getColor().get() );
			setupObj.addProperty( "groupId", minMaxGroups.indexOf( bw.setupAssignments.getMinMaxGroup( setup ) ) );
			setups.add( String.valueOf( setup.getSetupId() ), setupObj );
		}
		final JsonObject groups = new JsonObject();
		expected.add( "MinMaxGroups", groups );
		for ( int i = 0; i < minMaxGroups.size(); i++ )
		{
			final MinMaxGroup minMaxGroup = minMaxGroups.get( i );
			final JsonObject groupObj = new JsonObject();
			groupObj.addProperty( "fullRangeMin", minMaxGroup.getFullRangeMin() );
			groupObj.addProperty( "fullRangeMax", minMaxGroup.getFullRangeMax() );
			groupObj.addProperty( "rangeMin", minMaxGroup.getRangeMin() );
			groupObj.addProperty( "rangeMax", minMaxGroup.getRangeMax() );
			groupObj.addProperty( "currentMin", minMaxGroup.getMinBoundedValue().getCurrentValue() );
			groupObj.addProperty( "currentMax", minMaxGroup.getMaxBoundedValue().getCurrentValue() );
			groups.add( String.valueOf( i ), groupObj );
		}

		BigWarpTestUtils.assertJsonDiff( expected, actual );

	}

	@Test
	public void viewerPanelTest() throws SpimDataException, IOException, URISyntaxException
	{
		bw = BigWarpTestUtils.createBigWarp( true, false, false, false );

		final PipedWriter writer = new PipedWriter();
		final PipedReader in = new PipedReader( writer, 10000 );

		final JsonWriter out = new JsonWriter( writer );
		new BigwarpSettings.BigWarpViewerPanelAdapter( bw.viewerP ).write( out, bw.viewerP );

		out.close();
		writer.close();
		final JsonElement actual = JsonParser.parseReader( in );

		final JsonObject expected = new JsonObject();

		final JsonArray sources = new JsonArray();
		expected.add( XmlIoViewerState.VIEWERSTATE_SOURCES_TAG, sources );
		/* All sources are active */
		bw.viewerP.getState().getSources().forEach( source -> sources.add( source.isActive() ) );

		final JsonArray groups = new JsonArray();
		expected.add( XmlIoViewerState.VIEWERSTATE_GROUPS_TAG, groups );

		final BigWarpViewerPanel value = bw.viewerP;
		final List< SourceGroup > sourceGroups = value.getState().getSourceGroups();
		for ( final SourceGroup sourceGroup : sourceGroups )
		{
			final JsonObject sourceGroupObj = new JsonObject();
			sourceGroupObj.addProperty( XmlIoViewerState.VIEWERSTATE_GROUP_ACTIVE_TAG, sourceGroup.isActive() );
			sourceGroupObj.addProperty( XmlIoViewerState.VIEWERSTATE_GROUP_NAME_TAG, sourceGroup.getName() );
			final JsonArray sourceIds = new JsonArray();
			sourceGroupObj.add( XmlIoViewerState.VIEWERSTATE_GROUP_SOURCEID_TAG, sourceIds );
			for ( final Integer sourceId : sourceGroup.getSourceIds() )
			{
				sourceIds.add( sourceId );
			}
			groups.add( sourceGroupObj );
		}
		expected.addProperty( XmlIoViewerState.VIEWERSTATE_DISPLAYMODE_TAG, XmlIoViewerState.VIEWERSTATE_DISPLAYMODE_VALUE_SINGLE );
		expected.addProperty( XmlIoViewerState.VIEWERSTATE_INTERPOLATION_TAG, XmlIoViewerState.VIEWERSTATE_INTERPOLATION_VALUE_NEARESTNEIGHBOR );
		expected.addProperty( XmlIoViewerState.VIEWERSTATE_CURRENTSOURCE_TAG, value.getState().getCurrentSource() );
		expected.addProperty( XmlIoViewerState.VIEWERSTATE_CURRENTGROUP_TAG, value.getState().getCurrentGroup() );
		expected.addProperty( XmlIoViewerState.VIEWERSTATE_CURRENTTIMEPOINT_TAG, value.getState().getCurrentTimepoint() );

		BigWarpTestUtils.assertJsonDiff( expected, actual );
	}

	@Test
	public void sourceFromFileTest() throws SpimDataException, URISyntaxException, IOException, JDOMException
	{
		final BigWarp< Object > bw = BigWarpTestUtils.createBigWarp( "/tmp/img8270806677315563879.tif" );
		bw.loadSettings("src/test/resources/settings/expected.json");
		// Grab the sources
		// Compare the ids, urls, isMoving status, and isActive
		Assert.assertEquals("Wrong Number of Sources", 4, bw.data.sources.size());
		Assert.assertEquals("Wrong Number of Moving Sources", 2, bw.data.numMovingSources());
		Assert.assertEquals("Wrong Number of Target Sources", 2, bw.data.numTargetSources());
		final boolean[] movingById = { true, true, false, false };
		bw.data.sourceInfos.forEach( (id, info) -> {

			Assert.assertEquals("URI Mismatch", "/tmp/img8270806677315563879.tif", info.getUri());
			Assert.assertEquals("Name Mismatch", "img8270806677315563879.tif", info.getName());
			Assert.assertEquals("Moving/Target Mismatch", movingById[id], info.isMoving());
		} );
	}

	@Test
	public void sourceFromImageJTest() throws SpimDataException, URISyntaxException, IOException, JDOMException
	{
		final ImagePlus img = BigWarpTestUtils.generateImagePlus( "generated image" );
		img.setDisplayRange( 5, 15 );
		img.show();

		final String imagejUri = "imagej:///generated image";
		final Path xmlSourceSettings = createNewSettingsWithReplacement(
				"src/test/resources/settings/expected.json",
				new HashMap< String, String >() {
					/* Map filled during construction: see: https://stackoverflow.com/questions/6802483/how-to-directly-initialize-a-hashmap-in-a-literal-way */
					{
						put( "/tmp/img8270806677315563879.tif", imagejUri);
						put( "img8270806677315563879.tif", "generated image" );
					}
				});

		final BigWarp< Object > bw = BigWarpTestUtils.createBigWarp(  );
		bw.loadSettings(xmlSourceSettings.toFile().getCanonicalPath());
		// Grab the sources
		// Compare the ids, urls, isMoving status, and isActive
		Assert.assertEquals("Wrong Number of Sources", 4, bw.data.sources.size());
		Assert.assertEquals("Wrong Number of Moving Sources", 2, bw.data.numMovingSources());
		Assert.assertEquals("Wrong Number of Target Sources", 2, bw.data.numTargetSources());
		final boolean[] movingById = { true, true, false, false };
		bw.data.sourceInfos.forEach( (id, info) -> {

			Assert.assertEquals("URI Mismatch", imagejUri, info.getUri());
			Assert.assertEquals("Name Mismatch", "generated image", info.getName());
			Assert.assertEquals("Moving/Target Mismatch", movingById[id], info.isMoving());
		} );


		assertExpectedSettingsToCurrent( xmlSourceSettings, bw );
	}

	private static void assertExpectedSettingsToCurrent( final Path expectedSettings, final BigWarp< Object > bw ) throws IOException
	{
		/* Save the settings and compare with initial to test the deserialization */
		final Path tempSettings = Files.createTempFile( "deserialization", ".json" );
		tempSettings.toFile().delete();
		bw.saveSettingsJson(tempSettings.toFile().getCanonicalPath());
		final JsonElement expectedJson = JsonParser.parseReader( new FileReader( expectedSettings.toFile() ) );
		final JsonElement actualJson = JsonParser.parseReader( new FileReader( tempSettings.toFile() ) );
		BigWarpTestUtils.assertJsonDiff( expectedJson, actualJson );
	}

	@Test
	public void sourceFromXmlTest() throws SpimDataException, URISyntaxException, IOException, JDOMException
	{
		final String xmlUri = "src/test/resources/mri-stack.xml";
		final Path xmlSourceSettings = createNewSettingsWithReplacement(
				"src/test/resources/settings/expected.json",
				new HashMap< String, String >() {
					/* Map filled during construction: see: https://stackoverflow.com/questions/6802483/how-to-directly-initialize-a-hashmap-in-a-literal-way */
					{
						put( "/tmp/img8270806677315563879.tif", xmlUri);
						put( "img8270806677315563879.tif", "channel 1" );
					}
				});

		final BigWarp< Object > bw = BigWarpTestUtils.createBigWarp(  );
		bw.loadSettings(xmlSourceSettings.toFile().getCanonicalPath());
		// Grab the sources
		// Compare the ids, urls, isMoving status, and isActive
		Assert.assertEquals("Wrong Number of Sources", 4, bw.data.sources.size());
		Assert.assertEquals("Wrong Number of Moving Sources", 2, bw.data.numMovingSources());
		Assert.assertEquals("Wrong Number of Target Sources", 2, bw.data.numTargetSources());
		final boolean[] movingById = { true, true, false, false };
		bw.data.sourceInfos.forEach( (id, info) -> {

			Assert.assertEquals("URI Mismatch", xmlUri, info.getUri());
			Assert.assertEquals("Name Mismatch", "channel 1", info.getName());
			Assert.assertEquals("Moving/Target Mismatch", movingById[id], info.isMoving());
		} );


		/* then save the settings, load it, and compare with initial to test the deserialization */
		assertExpectedSettingsToCurrent( xmlSourceSettings, bw );
	}

	@Test
	public void sourceFromN5Test() throws SpimDataException, URISyntaxException, IOException, JDOMException
	{
		final String n5Uri = "src/test/resources/bigwarp/url/transformTest.n5?img";

		/* Map filled during construction: see: https://stackoverflow.com/questions/6802483/how-to-directly-initialize-a-hashmap-in-a-literal-way */
		final Path xmlSourceSettings = createNewSettingsWithReplacement(
				"src/test/resources/settings/expected.json",
				new HashMap< String, String >() {
					{
						put( "/tmp/img8270806677315563879.tif", n5Uri );
						put( "img8270806677315563879.tif", "img" );
					}
				}
		);

		final BigWarp< Object > bw = BigWarpTestUtils.createBigWarp(  );
		bw.loadSettings(xmlSourceSettings.toFile().getCanonicalPath());
		// Grab the sources
		// Compare the ids, urls, isMoving status, and isActive
		Assert.assertEquals("Wrong Number of Sources", 4, bw.data.sources.size());
		Assert.assertEquals("Wrong Number of Moving Sources", 2, bw.data.numMovingSources());
		Assert.assertEquals("Wrong Number of Target Sources", 2, bw.data.numTargetSources());
		final boolean[] movingById = { true, true, false, false };
		bw.data.sourceInfos.forEach( (id, info) -> {

			Assert.assertEquals("URI Mismatch", n5Uri, info.getUri());
			Assert.assertEquals("Name Mismatch", "img", info.getName());
			Assert.assertEquals("Moving/Target Mismatch", movingById[id], info.isMoving());
		} );

		/* then save the settings, load it, and compare with initial to test the deserialization */
		assertExpectedSettingsToCurrent( xmlSourceSettings, bw );

	}

	private static Path createNewSettingsWithReplacement( final String baseSettings, final Map<String, String> replacements ) throws IOException
	{
		/* Load expected.json and replace source path with desired uri */
		final Path settings = Paths.get( baseSettings );
		final List< String > newLines = Files.readAllLines( settings ).stream().map( line -> {
			String out = line;
			for ( final Map.Entry< String, String > replaceWith : replacements.entrySet() )
			{
				out = out.replaceAll( replaceWith.getKey(), replaceWith.getValue() );
			}
			return out;
		} ).collect( Collectors.toList());
		final Path newSettings = Files.createTempFile( "settings", ".json" );
		return Files.write( newSettings, newLines );
	}

	@Test
	public void repeatComparison() throws Exception
	{
		for ( int i = 0; i < 40; i++ )
		{
			bw = BigWarpTestUtils.createBigWarp( true );

			/* Load the known good*/
			final String originalXmlSettings = "src/test/resources/settings/repeatFail.settings.xml";
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
	public void compareKnownXmlComparisonTest() throws SpimDataException, IOException, JDOMException, SAXException, URISyntaxException
	{
		BigWarp< ? > bw = BigWarpTestUtils.createBigWarp( true, false, false, false );

		final String originalXmlSettings = "src/test/resources/settings/compareKnownXml.bigwarp.settings.xml";
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
//		bw = createBigWarp(new boolean[]{true, false, false, false});

		bw.loadSettings( tmpJsonFile.getAbsolutePath(), true );
		final File tmpXmlFile = Files.createTempFile( "xml-settings", ".xml" ).toFile();
		bw.saveSettings( tmpXmlFile.getAbsolutePath() );
		XMLUnit.setIgnoreWhitespace( true );
		XMLUnit.setIgnoreComments( true );
		XMLAssert.assertXMLEqual( new FileReader( originalXmlSettings ), new FileReader( tmpXmlFile ) );

	}

	@Test
	public void jsonLoadSaveComparisonTest() throws SpimDataException, IOException, JDOMException, URISyntaxException
	{
		bw = BigWarpTestUtils.createBigWarp( "/tmp/img8270806677315563879.tif", true, true, false, false );

		final String expectedJsonFile = "src/test/resources/settings/expected_with_dfield.json";
		bw.loadSettings( expectedJsonFile );
		final BigwarpSettings settings = bw.getSettings();

		final PipedWriter writer = new PipedWriter();
		final PipedReader in = new PipedReader( writer, 10000 );

		final JsonWriter out = new JsonWriter( writer );

		settings.write( out, settings );
		out.close();
		writer.close();

		final JsonElement jsonSettingsOut = JsonParser.parseReader( in );
		final JsonElement expectedJson = JsonParser.parseReader( new FileReader( expectedJsonFile ) );

		BigWarpTestUtils.assertJsonDiff( expectedJson, jsonSettingsOut );
	}

	@Test
	public void landmarkComparisonTest() throws SpimDataException, IOException, JDOMException, URISyntaxException
	{
		bw = BigWarpTestUtils.createBigWarp( "/tmp/img8270806677315563879.tif", true, true, false, false );

		final String xmlSettings = "src/test/resources/settings/compareKnownXml.bigwarp.settings.xml";
		final String csvLandmarks = "src/test/resources/settings/landmarks.csv";
		final String expectedJsonFile = "src/test/resources/settings/expected.json";
		bw.loadSettings( xmlSettings );
		bw.loadLandmarks( csvLandmarks );

		final BigwarpSettings settings = bw.getSettings();

		final PipedWriter writer = new PipedWriter();
		final PipedReader in = new PipedReader( writer, 10000 );

		final JsonWriter out = new JsonWriter( writer );
		settings.write( out, settings );
		out.close();
		writer.close();

		final JsonElement jsonSettingsOut = JsonParser.parseReader( in );
		final JsonElement expectedJson = JsonParser.parseReader( new FileReader( expectedJsonFile ) );

		BigWarpTestUtils.assertJsonDiff( expectedJson, jsonSettingsOut );

	}

	public static <T> void main( String[] args ) throws SpimDataException, URISyntaxException, IOException, JDOMException, InterruptedException
	{

		final ImagePlus img = BigWarpTestUtils.generateImagePlus( "generated image" );
		img.setDisplayRange( 5, 15 );
		img.show();

		final BigWarpData< T > data = BigWarpInit.initData();
		BigWarpInit.add(data, BigWarpInit.createSources( data, img, 123, 0, true ));

		new BigWarp<>(data, null);

		// BigWarp<?> bw = BigWarpTestUtils.createBigWarp("/tmp/img8270806677315563879.tif", true, true, false, false);
		// bw.saveSettingsJson( "/tmp/3d-settings.json" );
		// bw.closeAll();
		// Thread.sleep( 1000 );
		// bw = BigWarpTestUtils.createBigWarp();
		// bw.loadSettings("/tmp/3d-settings.json");
	}
}
