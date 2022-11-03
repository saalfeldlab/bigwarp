package bigwarp.url;

import java.io.IOException;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;

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
		h5.createDataset( "img", new long[]{6, 8, 10}, new int[] {16, 16, 16}, DataType.UINT8, new GzipCompression() );
		h5.createDataset( "img2", new long[]{12, 16, 20}, new int[] {20, 20, 20}, DataType.UINT8, new GzipCompression() );

		final N5FSWriter n5 = new N5FSWriter("src/test/resources/bigwarp/url/transformTest.n5" );
		n5.createDataset( "img", new long[]{5, 8, 9}, new int[] {16, 16, 16}, DataType.UINT8, new GzipCompression() );
		n5.createDataset( "img2", new long[]{10, 16, 18}, new int[] {18, 18, 18}, DataType.UINT8, new GzipCompression() );

		final N5ZarrWriter zarr = new N5ZarrWriter("src/test/resources/bigwarp/url/transformTest.zarr" );
		zarr.createDataset( "/img", new long[]{4, 6, 8}, new int[] {16, 16, 16}, DataType.UINT8, new GzipCompression() );
		zarr.createDataset( "/img2", new long[]{8, 12, 16}, new int[] {16, 16, 16}, DataType.UINT8, new GzipCompression() );
	}

}
