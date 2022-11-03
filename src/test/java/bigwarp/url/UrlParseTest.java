package bigwarp.url;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.ij.N5Factory;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.junit.Before;
import org.junit.Test;

public class UrlParseTest
{
	
	private Class<N5FSReader> n5Clazz;
	private Class<N5ZarrReader> zarrClazz;
	private Class<N5HDF5Reader> h5Clazz;

	@Before
	public void before()
	{
		n5Clazz = N5FSReader.class;
		zarrClazz = N5ZarrReader.class;
		h5Clazz = N5HDF5Reader.class;
	}

	@Test
	public void testFactories()
	{
		final String n5Path = new File( "src/test/resources/bigwarp/url/transformTest.n5" ).getAbsolutePath();
		final String zarrPath = new File( "src/test/resources/bigwarp/url/transformTest.zarr" ).getAbsolutePath();
		final String h5Path = new File( "src/test/resources/bigwarp/url/transformTest.h5" ).getAbsolutePath();

		N5Factory n5Factory = new N5Factory();
		try
		{
			N5Reader n5 = n5Factory.openReader( n5Path );
			assertEquals( n5Clazz, n5.getClass());

			n5 = n5Factory.openReader( zarrPath );
			assertEquals( zarrClazz, n5.getClass());

			n5 = n5Factory.openReader( h5Path );
			assertEquals( h5Clazz, n5.getClass());
		}
		catch ( IOException e )
		{
			fail( e.getMessage() );
		}
	}

}
