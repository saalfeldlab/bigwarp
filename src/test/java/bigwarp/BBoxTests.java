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

import static org.junit.Assert.*;
import org.junit.Test;

import bdv.ij.ApplyBigwarpPlugin;
import bdv.util.RandomAccessibleIntervalSource;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.BoundingBoxEstimation;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.ConstantUtils;
import net.imglib2.util.Intervals;

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

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Test
	public void testPhysicalBoundingBoxEstimation() {

		final double EPS = 1e-9;

		final FinalInterval pixelInterval = Intervals.createMinMax(0, 0, 0, 32, 16, 8);
		System.out.println(pixelInterval);

		final AffineTransform3D identity = new AffineTransform3D();
		final UnsignedByteType type = new UnsignedByteType();
		final RandomAccessibleIntervalSource src = new RandomAccessibleIntervalSource(
				ConstantUtils.constantRandomAccessibleInterval(type, pixelInterval), type, identity, "test1");

		RealInterval physicalInterval = ApplyBigwarpPlugin.getPhysicalInterval(src, new Scale3D(5, 7, 11));
		assertEquals("scale x min", 0, physicalInterval.realMin(0), EPS);
		assertEquals("scale y min", 0, physicalInterval.realMin(1), EPS);
		assertEquals("scale z min", 0, physicalInterval.realMin(2), EPS);

		assertEquals("scale x max", 32 * 5, physicalInterval.realMax(0), EPS);
		assertEquals("scale y max", 16 * 7, physicalInterval.realMax(1), EPS);
		assertEquals("scale z max", 8 * 11, physicalInterval.realMax(2), EPS);

		final AffineTransform3D scale = new AffineTransform3D();
		scale.scale(2, 3, 4);
		final RandomAccessibleIntervalSource srcScaled = new RandomAccessibleIntervalSource(
				ConstantUtils.constantRandomAccessibleInterval(type, pixelInterval), type, scale, "test2");

		physicalInterval = ApplyBigwarpPlugin.getPhysicalInterval(srcScaled, new Scale3D(5, 7, 11));
		assertEquals("scale x min", 0, physicalInterval.realMin(0), EPS);
		assertEquals("scale y min", 0, physicalInterval.realMin(1), EPS);
		assertEquals("scale z min", 0, physicalInterval.realMin(2), EPS);

		assertEquals("scale x max", 32 * 5 * 2, physicalInterval.realMax(0), EPS);
		assertEquals("scale y max", 16 * 7 * 3, physicalInterval.realMax(1), EPS);
		assertEquals("scale z max", 8 * 11 * 4, physicalInterval.realMax(2), EPS);
	}

}
