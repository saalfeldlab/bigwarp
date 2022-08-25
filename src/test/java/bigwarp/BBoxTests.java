/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2022 Howard Hughes Medical Institute.
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

import static org.junit.Assert.*;
import org.junit.Test;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.BoundingBoxEstimation;

public class BBoxTests {

	static final FinalInterval itvl = new FinalInterval( new long[] { 40, 30, 20 });

	@Test
	public void testCornersAffine()
	{
		final AffineTransform3D xfm = new AffineTransform3D();
		xfm.scale(2, 3, 4);

		final BoundingBoxEstimation bboxCorners = new BoundingBoxEstimation(BoundingBoxEstimation.Method.CORNERS);
		final Interval bbox = bboxCorners.estimatePixelInterval(xfm, itvl);

		assertEquals( "max x ", itvl.max(0) * 2, bbox.max(0) );
		assertEquals( "max y ", itvl.max(1) * 3, bbox.max(1) );
		assertEquals( "max z ", itvl.max(2) * 4, bbox.max(2) );
	}

	@Test
	public void testCornersFaces()
	{
		final AffineTransform3D xfm = new AffineTransform3D();
		xfm.scale(2, 3, 4);

		final BoundingBoxEstimation bboxFaces = new BoundingBoxEstimation(BoundingBoxEstimation.Method.FACES);
		bboxFaces.setSamplesPerDim(5);

		final Interval bbox = bboxFaces.estimatePixelInterval(xfm, itvl);
		assertEquals( "max x ", itvl.max(0) * 2, bbox.max(0) );
		assertEquals( "max y ", itvl.max(1) * 3, bbox.max(1) );
		assertEquals( "max z ", itvl.max(2) * 4, bbox.max(2) );
	}

}
