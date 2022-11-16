package bigwarp.url;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import bigwarp.BigWarpData;
import bigwarp.BigWarpInit;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.StackWriter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import mpicbg.spim.data.SpimDataException;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.ij.N5Factory;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class UrlParseTest
{
	public static final String TIFF_FILE_3D = BigWarpTestUtils.createTemp3DImage( "img3d", "tif" );

	public static final String PNG_FILE_2D = BigWarpTestUtils.createTemp2DImage( "img2d", "png" );

	public static final String TIFF_STACK_DIR = BigWarpTestUtils.createTemp3DImageStack( "imgDir3d" );

	private Class< N5FSReader > n5Clazz;

	private Class< N5ZarrReader > zarrClazz;

	private Class< N5HDF5Reader > h5Clazz;

	private HashMap< String, long[] > urlToDimensions;

	private String n5Root, zarrRoot, h5Root;

	@Before
	public void before() throws IOException
	{
		n5Clazz = N5FSReader.class;
		zarrClazz = N5ZarrReader.class;
		h5Clazz = N5HDF5Reader.class;

		n5Root = new File( "src/test/resources/bigwarp/url/transformTest.n5" ).getAbsolutePath();
		zarrRoot = new File( "src/test/resources/bigwarp/url/transformTest.zarr" ).getAbsolutePath();
		h5Root = new File( "src/test/resources/bigwarp/url/transformTest.h5" ).getAbsolutePath();

		urlToDimensions = new HashMap<>();
		final String h5Root = new File( "src/test/resources/bigwarp/url/transformTest.h5" ).getAbsolutePath();
		urlToDimensions.put( h5Root + "?img", new long[] { 6, 8, 10 } );
		urlToDimensions.put( h5Root + "?img2", new long[] { 12, 16, 20 } );

		final String n5Root = new File( "src/test/resources/bigwarp/url/transformTest.n5" ).getAbsolutePath();
		urlToDimensions.put( n5Root + "?img", new long[] { 5, 8, 9 } );
		urlToDimensions.put( n5Root + "?img2", new long[] { 10, 16, 18 } );

		final String zarrRoot = new File( "src/test/resources/bigwarp/url/transformTest.zarr" ).getAbsolutePath();
		urlToDimensions.put( zarrRoot + "?img", new long[] { 4, 6, 8 } );
		urlToDimensions.put( zarrRoot + "?img2", new long[] { 8, 12, 16 } );
	}

	@Test
	public void testFactories()
	{
		N5Factory n5Factory = new N5Factory();
		try
		{
			assertEquals( n5Clazz, n5Factory.openReader( n5Root ).getClass() );
			assertEquals( zarrClazz, n5Factory.openReader( zarrRoot ).getClass() );
			assertEquals( h5Clazz, n5Factory.openReader( h5Root ).getClass() );
		}
		catch ( IOException e )
		{
			fail( e.getMessage() );
		}
	}

	@Test
	public void testUrlSources()
	{
		final String bdvXmlUrl = new File( "src/test/resources/mri-stack.xml" ).getAbsolutePath();

		Source< ? > bdvXmlSrc = loadSourceFromUri( bdvXmlUrl );
		assertNotNull( bdvXmlSrc );

		Source< ? > img3dTif = loadSourceFromUri( TIFF_FILE_3D );
		assertNotNull( img3dTif );
		assertArrayEquals( new long[]{8,8,4}, img3dTif.getSource( 0, 0 ).dimensionsAsLongArray() );

		Source< ? > img2dPng = loadSourceFromUri( PNG_FILE_2D );
		assertNotNull( img2dPng );
		assertArrayEquals( new long[]{8,8,1}, img2dPng.getSource( 0, 0 ).dimensionsAsLongArray() ); // TODO I wrote this to expect [8,8,1], but it might need [8,8].

		Source< ? > img3dTifFromDir = loadSourceFromUri( TIFF_STACK_DIR );
		assertNotNull( img3dTifFromDir );
		assertArrayEquals( new long[]{8,8,4}, img3dTifFromDir.getSource( 0, 0 ).dimensionsAsLongArray() );

		for ( String url : urlToDimensions.keySet() )
		{
			Source< ? > src = loadSourceFromUri( url );
			assertNotNull( src );
			assertArrayEquals( urlToDimensions.get( url ), src.getSource( 0, 0 ).dimensionsAsLongArray() ); // TODO I wrote this to expect [8,8,1], but it might need [8,8].
		}
	}

	@Test
	public void testUrlTransforms()
	{
		final String n5Path = new File( "src/test/resources/bigwarp/url/transformTest.n5" ).getAbsolutePath();

		final String s0Url = n5Path + "?ant&transform=[0]";
		final String s0DefaultUrl = n5Path + "?ant&transform=[0]";

		// TODO when we're ready
//		final Object s0 = loadTransformFromUrl( s0Url );
//		final Object s0Default = loadTransformFromUrl( s0DefaultUrl );

//		assertNotNull( s0 );
//		assertNotNull( s0Default );
//		assertEquals( s0, s0Default );
	}

	@Test
	public void n5FileEquivalencyTest() throws IOException
	{
		final String relativePath = "src/test/resources/bigwarp/url/transformTest.n5";
		final String absolutePath = Paths.get( relativePath ).toAbsolutePath().toFile().getCanonicalPath();
		final String[] variants = new String[]{
				"n5:file://" + absolutePath + "?img#coordinateTransformations[0]",
				"n5:file://" + absolutePath + "?img",
				"n5://" + absolutePath + "?img#coordinateTransformations[0]",
				"n5://" + absolutePath + "?img",
				"file://" + absolutePath + "?img#coordinateTransformations[0]",
				"file://" + absolutePath + "?img",
				"n5:" + absolutePath + "?img#coordinateTransformations[0]",
				"n5:" + absolutePath + "?img",
				absolutePath + "?img#coordinateTransformations[0]",
				absolutePath + "?img",
				"n5:file://" + relativePath + "?img#coordinateTransformations[0]",
				"n5:file://" + relativePath + "?img",
				"n5://" + relativePath + "?img#coordinateTransformations[0]",
				"n5://" + relativePath + "?img",
				"file://" + relativePath + "?img#coordinateTransformations[0]",
				"file://" + relativePath + "?img",
				"n5:" + relativePath + "?img#coordinateTransformations[0]",
				"n5:" + relativePath + "?img",
				relativePath + "?img#coordinateTransformations[0]",
				relativePath + "?img"
		};

		final BigWarpData< Object > data = BigWarpInit.initData();
		try
		{

			final AtomicInteger id = new AtomicInteger( 1 );
			for ( String uri :  variants)
			{
				final int setupId = id.getAndIncrement();
				BigWarpInit.add( data, uri, setupId, new Random().nextBoolean() );
				assertEquals( uri, data.urls.get(setupId ).getA().get());
			}
		}
		catch ( URISyntaxException | IOException | SpimDataException e )
		{
			throw new RuntimeException( e );
		}
	}

	private Object loadTransformFromUrl( String url )
	{
		// TODO Caleb will remove me and replace calls to me with something real
		return null;
	}

	private Source< ? > loadSourceFromUri( String uri )
	{


		final BigWarpData< Object > data = BigWarpInit.initData();
		try
		{
			final Source< ? > source = BigWarpInit.add( data, uri, 0, true );
			data.wrapUp();
			return source;
		}
		catch ( URISyntaxException | IOException | SpimDataException e )
		{
			throw new RuntimeException( e );
		}
	}


}
