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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.stream.IntStream;

import org.junit.Test;

import bdv.viewer.Source;
import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.realtransform.Translation3D;
import net.imglib2.type.numeric.integer.AbstractIntegerType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

public class BigWarpTransformCacheTests {

	/**
	 * Test that cached transformed sources are pixelwise identical to uncached
	 * when using an identity transformation.
	 */
	@Test
	public void cachedIdentityTransform() {

		final Source<UnsignedByteType> msSrc = BigWarpTestUtils.generateMultiscaleSource(2, "msSrc",
				new long[]{32, 16, 8},
				IntStream.of(14, 15).mapToObj(x -> new Point(x, 8, 4)),
				new double[]{1, 1, 1},
				new double[]{0, 0, 0});

		final Scale3D identity = new Scale3D(1, 1, 1);
		final BigWarpData<UnsignedByteType> bwData = BigWarpInit.initData();
		BigWarpInit.add(bwData, BigWarpInit.createSources(bwData, msSrc, 0, true), identity, () -> "identity");
		BigWarpInit.add(bwData, BigWarpInit.createSources(bwData, msSrc, 1, false));
		bwData.applyTransformations();
		bwData.wrapMovingSources();

		Source<UnsignedByteType> mvgSrc = bwData.getMovingSource(0).getSpimSource();
		Source<UnsignedByteType> tgtSrc = bwData.getTargetSource(0).getSpimSource();
		assertEquals("moving source is not cached", CachedCellImg.class, mvgSrc.getSource(0, 0).getClass());

		assertArrayEquals("scale level 0 pixel raster wrong size",
				tgtSrc.getSource(0, 0).dimensionsAsLongArray(),
				mvgSrc.getSource(0, 0).dimensionsAsLongArray());
		assertArrayEquals("scale level 1 pixel raster wrong size",
				tgtSrc.getSource(0, 1).dimensionsAsLongArray(),
				mvgSrc.getSource(0, 1).dimensionsAsLongArray());

		assertTrue("scale level 0 not equal", equal(tgtSrc.getSource(0, 0), mvgSrc.getSource(0, 0)));
		assertTrue("scale level 1 not equal", equal(tgtSrc.getSource(0, 1), mvgSrc.getSource(0, 1)));
	}

	/**
	 * Test that cached transformed sources with an offset are pixelwise
	 * identical to uncached when using an identity transformation.
	 */
	@Test
	public void cachedIdentityTransformOffset() {

		final Source<UnsignedByteType> msSrc = BigWarpTestUtils.generateMultiscaleSource(2, "msSrc",
				new long[]{32, 16, 8},
				IntStream.of(14, 15).mapToObj(x -> new Point(x, 8, 4)),
				new double[]{1, 1, 1},
				new double[]{0.2, 1.3, -2.7});

		final Scale3D identity = new Scale3D(1, 1, 1);
		final BigWarpData<UnsignedByteType> bwData = BigWarpInit.initData();
		BigWarpInit.add(bwData, BigWarpInit.createSources(bwData, msSrc, 0, true), identity, () -> "identity");
		BigWarpInit.add(bwData, BigWarpInit.createSources(bwData, msSrc, 1, false));
		bwData.applyTransformations();
		bwData.wrapMovingSources();

		Source<UnsignedByteType> mvgSrc = bwData.getMovingSource(0).getSpimSource();
		Source<UnsignedByteType> tgtSrc = bwData.getTargetSource(0).getSpimSource();
		assertEquals("moving source is not cached", CachedCellImg.class, mvgSrc.getSource(0, 0).getClass());

		assertTrue("scale level 0 not equal", equal(tgtSrc.getSource(0, 0), mvgSrc.getSource(0, 0)));
		assertTrue("scale level 1 not equal", equal(tgtSrc.getSource(0, 1), mvgSrc.getSource(0, 1)));
	}

	/**
	 * Test that cached transformed sources with an offset are pixelwise
	 * identical to uncached when using an identity transformation.
	 */
	@Test
	public void cachedTranslationTransformOffset() {

		final Source<UnsignedByteType> msSrc = BigWarpTestUtils.generateMultiscaleSource(2, "msSrc",
				new long[]{32, 16, 8},
				IntStream.of(14, 15).mapToObj(x -> new Point(x, 8, 4)),
				new double[]{1, 1, 1},
				new double[]{0.2, 1.3, -2.7});

		final Translation3D translation = new Translation3D(-2.5, 1.1, 3.3);
		final BigWarpData<UnsignedByteType> bwData = BigWarpInit.initData();
		BigWarpInit.add(bwData, BigWarpInit.createSources(bwData, msSrc, 0, true), translation, () -> "translation(-2.5, 1.1, 3.3)");
		BigWarpInit.add(bwData, BigWarpInit.createSources(bwData, msSrc, 1, false));
		bwData.applyTransformations();
		bwData.wrapMovingSources();

		Source<UnsignedByteType> mvgSrc = bwData.getMovingSource(0).getSpimSource();
		Source<UnsignedByteType> tgtSrc = bwData.getTargetSource(0).getSpimSource();
		assertEquals("moving source is not cached", CachedCellImg.class, mvgSrc.getSource(0, 0).getClass());

		assertTrue("scale level 0 not equal", equal(tgtSrc.getSource(0, 0), mvgSrc.getSource(0, 0)));
		assertTrue("scale level 1 not equal", equal(tgtSrc.getSource(0, 1), mvgSrc.getSource(0, 1)));

		final AffineTransform3D mvgTform0 = new AffineTransform3D();
		mvgSrc.getSourceTransform(0, 0, mvgTform0);

		final AffineTransform3D tgtTform0 = new AffineTransform3D();
		tgtSrc.getSourceTransform(0, 0, tgtTform0);
		tgtTform0.preConcatenate(translation.inverse());

		assertArrayEquals("mvg transform is not the translated tgt transform", tgtTform0.getRowPackedCopy(), mvgTform0.getRowPackedCopy(), 1e-9);
	}

	private <T extends AbstractIntegerType<?>> boolean equal(RandomAccessibleInterval<T> a, RandomAccessibleInterval<T> b) {

		final Cursor<T> ca = Views.flatIterable(a).cursor();
		final RandomAccess<T> rab = b.randomAccess();
		while (ca.hasNext()) {
			ca.fwd();
			rab.setPosition(ca);
			if (ca.get().getInteger() != rab.get().getInteger())
				return false;

		}
		return true;
	}
}
