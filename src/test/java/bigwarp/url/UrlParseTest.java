package bigwarp.url;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.ij.N5Factory;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.junit.Before;
import org.junit.Test;

import bdv.viewer.Source;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;

public class UrlParseTest
{
	
	private Class<N5FSReader> n5Clazz;
	private Class<N5ZarrReader> zarrClazz;
	private Class<N5HDF5Reader> h5Clazz;

	private HashMap<String,long[]> urlToDimensions;

	private String n5Root, zarrRoot, h5Root;
	private String img3dTifPath, img2dPngPath;

	@Before
	public void before()
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

		ImagePlus img3d = NewImage.createByteImage( "img3d", 8, 8, 4, NewImage.FILL_RAMP );
		img3dTifPath = new File( "src/test/resources/bigwarp/url/img.tif").getAbsolutePath();
		IJ.save( img3d, img3dTifPath );

		ImagePlus img2d = NewImage.createByteImage( "img2d", 8, 8, 1, NewImage.FILL_RAMP );
		img2dPngPath = new File( "src/test/resources/bigwarp/url/img2d.png").getAbsolutePath();
		IJ.save( img2d, img2dPngPath );
	}

	@Test
	public void testFactories()
	{
		N5Factory n5Factory = new N5Factory();
		try
		{
			assertEquals( n5Clazz, n5Factory.openReader( n5Root ).getClass());
			assertEquals( zarrClazz, n5Factory.openReader( zarrRoot ).getClass());
			assertEquals( h5Clazz, n5Factory.openReader( h5Root ).getClass());
		}
		catch ( IOException e )
		{
			fail( e.getMessage() );
		}
	}

	public void testUrlSources()
	{
		final String bdvXmlUrl = new File( "src/test/resources/mri-stack.xml" ).getAbsolutePath();

		Source<?> bdvXmlSrc = loadSourceFromUrl( bdvXmlUrl );
//		assertNotNull( bdvXmlSrc );

		Source<?> img3dTif = loadSourceFromUrl( img3dTifPath );
//		assertNotNull( img3dTif );
//		assertArrayEquals( new long[]{8,8,4}, img3dTif.getSource( 0, 0 ).dimensionsAsLongArray() );

		Source<?> img2dPng = loadSourceFromUrl( img2dPngPath );
//		assertNotNull( img2dPng );
//		assertArrayEquals( new long[]{8,8,1}, img2dPng.getSource( 0, 0 ).dimensionsAsLongArray() ); // TODO I wrote this to expect [8,8,1], but it might need [8,8].

		for( String url : urlToDimensions.keySet() )
		{
			Source<?> src = loadSourceFromUrl( url );
//			assertNotNull( src );
//			assertArrayEquals( urlToDimensions.get( url ), src.getSource( 0, 0 ).dimensionsAsLongArray() ); // TODO I wrote this to expect [8,8,1], but it might need [8,8].
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

	private Object loadTransformFromUrl( String url )
	{
		// TODO Caleb will remove me and replace calls to me with something real
		return null;
	}

	private Source<?> loadSourceFromUrl( String url )
	{
		// TODO Caleb will remove me and replace calls to me with something real
		return null;
	}

}
