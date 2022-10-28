package bigwarp;

import bdv.gui.BigWarpViewerOptions;
import bdv.tools.bookmarks.Bookmarks;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.MinMaxGroup;
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.state.SourceGroup;
import bdv.viewer.state.XmlIoViewerState;
import bigwarp.source.PlateauSphericalMaskSource;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.List;
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
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

public class SerializationTest
{

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

		Assert.assertEquals( expected, actual );
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
		bookmarksObj.add("identity", identityArray);
		bookmarksObj.add("scale", scaleArray);
		bookmarksObj.add("translate", translateArray);

		Assert.assertEquals( expected, actual );
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

		Assert.assertEquals( expected, actual );
	}

	@Test
	public void setupAssignmentsTest() throws SpimDataException, IOException
	{
		final BigWarp< ? > bw = createBigWarp( new boolean[] { true, false, false, false } );

		final PipedWriter writer = new PipedWriter();
		final PipedReader in = new PipedReader( writer, 10000 );

		final JsonWriter out = new JsonWriter( writer );
		new BigwarpSettings.SetupAssignmentsAdapter( bw.setupAssignments ).write( out, bw.setupAssignments );

		out.close();
		writer.close();
		final JsonElement actual = JsonParser.parseReader( in );

		final JsonObject expected = new JsonObject();
		final JsonArray setups = new JsonArray();
		expected.add( "ConverterSetups", setups );
		final List< MinMaxGroup > minMaxGroups = bw.setupAssignments.getMinMaxGroups();
		for ( final ConverterSetup setup : bw.setupAssignments.getConverterSetups() )
		{
			final JsonObject setupObj = new JsonObject();
			setupObj.addProperty( "id" , setup.getSetupId() );
			setupObj.addProperty( "min" , setup.getDisplayRangeMin() );
			setupObj.addProperty( "max" , setup.getDisplayRangeMax() );
			setupObj.addProperty( "color" , setup.getColor().get() );
			setupObj.addProperty( "groupId" , minMaxGroups.indexOf( bw.setupAssignments.getMinMaxGroup( setup ) ) );
			setups.add( setupObj );
		}
		final JsonArray groups = new JsonArray();
		expected.add( "MinMaxGroups", groups );
		for ( int i = 0; i < minMaxGroups.size(); i++ )
		{
			final MinMaxGroup minMaxGroup = minMaxGroups.get( i );
			final JsonObject groupObj = new JsonObject();
			groupObj.addProperty( "id" , i );
			groupObj.addProperty( "fullRangeMin" , minMaxGroup.getFullRangeMin() );
			groupObj.addProperty( "fullRangeMax" , minMaxGroup.getFullRangeMax() );
			groupObj.addProperty( "rangeMin" , minMaxGroup.getRangeMin() );
			groupObj.addProperty( "rangeMax" , minMaxGroup.getRangeMax() );
			groupObj.addProperty( "currentMin" , minMaxGroup.getMinBoundedValue().getCurrentValue() );
			groupObj.addProperty( "currentMax" , minMaxGroup.getMaxBoundedValue().getCurrentValue() );
			groups.add( groupObj );
		}
		Assert.assertEquals( expected , actual);

	}

	@Test
	public void viewerPanelTest() throws SpimDataException, IOException
	{
		final BigWarp< ? > bw = createBigWarp( new boolean[] { true, false, false, false } );

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
		bw.viewerP.getState().getSources().forEach( source -> sources.add(source.isActive()) );

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
		expected.addProperty( XmlIoViewerState.VIEWERSTATE_DISPLAYMODE_TAG, XmlIoViewerState.VIEWERSTATE_DISPLAYMODE_VALUE_GROUP);
		expected.addProperty( XmlIoViewerState.VIEWERSTATE_INTERPOLATION_TAG, XmlIoViewerState.VIEWERSTATE_INTERPOLATION_VALUE_NEARESTNEIGHBOR );
		expected.addProperty( XmlIoViewerState.VIEWERSTATE_CURRENTSOURCE_TAG, value.getState().getCurrentSource() );
		expected.addProperty( XmlIoViewerState.VIEWERSTATE_CURRENTGROUP_TAG, value.getState().getCurrentGroup() );
		expected.addProperty( XmlIoViewerState.VIEWERSTATE_CURRENTTIMEPOINT_TAG, value.getState().getCurrentTimepoint() );

		Assert.assertEquals( prettyPrint(expected), prettyPrint( actual.getAsJsonObject()) );


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
			BigWarp< ? > bw = createBigWarp( new boolean[] { true, false, false, false } );

			/* Load the known good*/
			final String originalXmlSettings = "src/test/resources/settings/compareKnownXml.bigwarp.settings.xml";
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
		BigWarp< ? > bw = createBigWarp( new boolean[] { true, false, false, false } );

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

	@Test
	public void jsonLoadSaveComparisonTest() throws SpimDataException, IOException, JDOMException
	{
		BigWarp< ? > bw = createBigWarp( new boolean[] { true, false } );

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

		try
		{
			Assert.assertEquals( expectedJson, jsonSettingsOut );
		}
		finally
		{
			bw.closeAll();
		}
	}

	@Test
	public void landmarkComparisonTest() throws SpimDataException, IOException, JDOMException
	{
		BigWarp< ? > bw = createBigWarp( new boolean[] { true, false, false, false } );

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

		try
		{
			Assert.assertEquals( expectedJson, jsonSettingsOut );
		}
		finally
		{
			bw.closeAll();
		}

	}

	private static String prettyPrint( StringWriter json )
	{
		return prettyPrint( json.toString() );
	}

	private static String prettyPrint( String json )
	{
		final JsonElement parse = JsonParser.parseString( json );
		final JsonObject asJsonObject = parse.getAsJsonObject();

		return prettyPrint( asJsonObject );
	}

	private static String prettyPrint (JsonObject json) {

		return new GsonBuilder().setPrettyPrinting().create().toJson( json );
	}

	private static BigWarp< ? > createBigWarp( boolean[] moving ) throws SpimDataException
	{
		final BigWarp.BigWarpData< Object > data = BigWarpInit.initData();
		FunctionRandomAccessible< UnsignedByteType > fimg = new FunctionRandomAccessible<>(
				3,
				( l, v ) -> v.setOne(),
				UnsignedByteType::new );
		ImagePlus imp = ImageJFunctions.wrap( Views.interval( fimg, new FinalInterval( 32, 32, 1 ) ), "img" );
		for ( int i = 0; i < moving.length; i++ )
		{
			BigWarpInit.add( data, imp, i, 0, moving[ i ] );
		}
		data.wrapUp();
		BigWarpViewerOptions opts = BigWarpViewerOptions.options( false );
		return new BigWarp<>( data, "bigwarp", opts, null );
	}

	public static void main( String[] args ) throws SpimDataException
	{
		createBigWarp( new boolean[] { true, false, false, false } );
	}
}
