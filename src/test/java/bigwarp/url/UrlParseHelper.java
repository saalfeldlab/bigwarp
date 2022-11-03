package bigwarp.url;

import java.io.IOException;

import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;

public class UrlParseHelper
{

	public static void main( String[] args ) throws IOException
	{
		final N5HDF5Writer h5 = new N5HDF5Writer("src/test/resources/bigwarp/url/transformTest.h5", 32 );
		final String data = "[\n"
				+ "    {\n"
				+ "        \"type\" : \"scale\",\n"
				+ "        \"scale\" : [1,1,1],\n"
				+ "        \"input\" : \"ant-in\",\n"
				+ "        \"output\" : \"ant-out\"\n"
				+ "    }\n"
				+ "]" ;

		h5.createGroup( "ant" );
		h5.setAttribute( "ant", "coordinateTransformations", data );
	}

}
