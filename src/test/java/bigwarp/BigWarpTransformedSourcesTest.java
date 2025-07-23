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
package bigwarp;

import bdv.export.ProgressWriterConsole;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.realtransform.AffineTransform3D;

public class BigWarpTransformedSourcesTest
{

	public static void main( String[] args ) throws SpimDataException
	{
		final BigWarpData< ? > data = createData();
		data.applyTransformations();

		final BigWarp<?> bw = new BigWarp<>( data, new ProgressWriterConsole());
		bw.loadLandmarks( "/home/john/tmp/bw_tformTest_landmarks_simple.csv" );
//		bw.loadLandmarks( "/groups/saalfeld/home/bogovicj/tmp/bw_tformTest_landmarks.csv" );

	}

	public static <T> BigWarpData<T> createData()
	{
		final ImagePlus mr = IJ.openImage("/home/john/tmp/mri-stack.tif");
		final ImagePlus t1 = IJ.openImage("/home/john/tmp/t1-head.tif");

//		final ImagePlus mr = IJ.openImage("/groups/saalfeld/home/bogovicj/tmp/mri-stack.tif");
//		final ImagePlus t1 = IJ.openImage("/groups/saalfeld/home/bogovicj/tmp/t1-head.tif");

		final AffineTransform3D translation0 = new AffineTransform3D();
		translation0.translate( -50, -50, -10 );

		final AffineTransform3D translation = new AffineTransform3D();
		translation.translate( -100, -150, -50 );

		int id = 0;
		final BigWarpData< T > data = BigWarpInit.initData();
		BigWarpInit.add( data, BigWarpInit.createSources( data, mr, id++, 0, true ), translation0, null);
		BigWarpInit.add( data, BigWarpInit.createSources( data, t1, id++, 0, false ));
		BigWarpInit.add( data, BigWarpInit.createSources( data, t1, id++, 0, false ), translation, null);

		return data;
	}

}
