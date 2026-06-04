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

import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bigwarp.source.SourceInfo;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/*
 * See
 * https://github.com/bigdataviewer/bigdataviewer-playground/issues/249
 */
public class Sources2Dtests {

	public static void main(String[] args) throws SpimDataException {
		run();
	}

	public static <T extends NativeType<T> & RealType<T>> void run() throws SpimDataException
	{
		Source<T> mvg = loadSource("https://imagej.nih.gov/ij/images/boats.gif",  20);
		Source<T> tgt = loadSource("https://imagej.nih.gov/ij/images/boats.gif", -20);

		BigWarpData<T> bwdata = BigWarpInit.initData();
		BigWarpInit.add(bwdata, mvg, 0, 0, true);
		final SourceInfo mvgInfo = new SourceInfo( 0, true, "mvg", () -> "https://imagej.nih.gov/ij/images/boats.gif" );
		mvgInfo.setSourceAndConverter( bwdata.sources.get( bwdata.sources.size() - 1 ) );
		bwdata.sourceInfos.put( 0, mvgInfo );

		BigWarpInit.add(bwdata, tgt, 1, 0, false);
		final SourceInfo tgtInfo = new SourceInfo( 1, true, "tgt", () -> "https://imagej.nih.gov/ij/images/boats.gif" );
		bwdata.sourceInfos.put( 1, tgtInfo );
		tgtInfo.setSourceAndConverter( bwdata.sources.get( bwdata.sources.size() - 1 ) );

		bwdata.wrapUp();

		BigWarp bw = new BigWarp(bwdata, null);
	}

	public static <T extends NativeType<T> & RealType<T>> Source<T> loadSource( String path, double zOffset )
	{
		ImagePlus imp = IJ.openImage(path);
		RandomAccessibleInterval<T> img = ImageJFunctions.wrap(imp);
		if( img.numDimensions() == 2 )
			img = Views.addDimension(img, 0, 0);

		AffineTransform3D xfm = new AffineTransform3D();
		xfm.translate(0, 0, zOffset);

		return new RandomAccessibleIntervalSource<>(img, img.getType(), xfm, imp.getTitle());
	}

}
