/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2025 Howard Hughes Medical Institute.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
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
		h5.close();

		final N5FSWriter n5 = new N5FSWriter("src/test/resources/bigwarp/url/transformTest.n5" );
		n5.createDataset( "img", new long[]{5, 8, 9}, new int[] {16, 16, 16}, DataType.UINT8, new GzipCompression() );
		n5.createDataset( "img2", new long[]{10, 16, 18}, new int[] {18, 18, 18}, DataType.UINT8, new GzipCompression() );
		n5.close();

		final N5ZarrWriter zarr = new N5ZarrWriter("src/test/resources/bigwarp/url/transformTest.zarr" );
		zarr.createDataset( "/img", new long[]{4, 6, 8}, new int[] {16, 16, 16}, DataType.UINT8, new GzipCompression() );
		zarr.createDataset( "/img2", new long[]{8, 12, 16}, new int[] {16, 16, 16}, DataType.UINT8, new GzipCompression() );
		zarr.close();
	}

}
