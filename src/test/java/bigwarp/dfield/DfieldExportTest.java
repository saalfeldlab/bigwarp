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
package bigwarp.dfield;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5DisplacementField;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.junit.Before;
import org.junit.Test;

import bdv.gui.ExportDisplacementFieldFrame;
import bdv.gui.ExportDisplacementFieldFrame.DTYPE;
import bdv.ij.BigWarpToDeformationFieldPlugIn;
import bdv.viewer.Source;
import bigwarp.BigWarpData;
import bigwarp.BigWarpInit;
import bigwarp.FieldOfView;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.source.SourceInfo;
import bigwarp.transforms.BigWarpTransform;
import ij.ImagePlus;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.iterator.RealIntervalIterator;
import net.imglib2.realtransform.DisplacementFieldTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class DfieldExportTest {

	private BigWarpData<?> data;
	private BigWarpData<?> dataWithTransform;
	private LandmarkTableModel ltm;

	@Before
	public void setup() {

		final ImagePlus imp = ImagePlusImgs.bytes(64, 64, 16).getImagePlus();
		data = makeData(imp, null);
		dataWithTransform = makeData(imp, new Scale3D(0.5, 0.5, 0.5));

		ltm = new LandmarkTableModel(3);
		try {
			ltm.load(new File("src/test/resources/mr_landmarks_p2p2p4-111.csv"));
		} catch (final IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	private static <T extends RealType<T>> BigWarpData<T> makeData(ImagePlus imp, RealTransform tform) {

		final int id = 0;
		final boolean isMoving = true;
		final BigWarpData<T> data = BigWarpInit.initData();
		final LinkedHashMap<Source<T>, SourceInfo> infos = BigWarpInit.createSources(data, imp, id, 0, isMoving);
		BigWarpInit.add(data, infos, tform, null);
		return data;
	}

	@Test
	public void dfieldExportTest() {

		final BigWarpTransform bwTransform = new BigWarpTransform(ltm);
		bwTransform.setInverseTolerance(0.05);
		bwTransform.setInverseMaxIterations(200);

		final boolean ignoreAffine = false;
		final boolean flatten = true;
		final boolean virtual = false;
		final long[] dims = new long[]{47, 56, 7};
		final double[] spacing = new double[]{0.8, 0.8, 1.6};
		final double[] offset = new double[]{0, 0, 0};
		final int nThreads = 1;

		final FinalRealInterval testItvl = new FinalRealInterval(
				new double[]{3.6, 3.6, 1.6},
				new double[]{32.0, 40.0, 9.6});

		final RealIntervalIterator it = new RealIntervalIterator(testItvl, spacing);

		final ImagePlus dfieldImp = BigWarpToDeformationFieldPlugIn.toImagePlus(
				data, ltm, bwTransform,
				ignoreAffine, flatten, false, virtual,
				dims, spacing, offset,
				nThreads);

		final InvertibleRealTransform tform = bwTransform.getTransformation();
		assertTrue("forward", compare(tform, dfieldImp, it, 1e-3));

		final ImagePlus dfieldInvImp = BigWarpToDeformationFieldPlugIn.toImagePlus(
				data, ltm, bwTransform,
				ignoreAffine, flatten, true, virtual,
				dims, spacing, offset,
				nThreads);

		it.reset();
		assertTrue("inverse", compare(tform.inverse(), dfieldInvImp, it, 0.25));
	}

	@Test
	public void dfieldIgnoreAffineExportTest() {

		final boolean ignoreAffine = true;

		final BigWarpTransform bwTransform = new BigWarpTransform(ltm);

		// constant parameters
		final boolean flatten = true;
		final boolean virtual = false;
		final long[] dims = new long[]{47, 56, 7};
		final double[] spacing = new double[]{0.8, 0.8, 1.6};
		final double[] offset = new double[]{0, 0, 0};
		final int nThreads = 1;

		final FinalRealInterval testItvl = new FinalRealInterval(
				new double[]{3.6, 3.6, 1.6},
				new double[]{32.0, 40.0, 9.6});

		final ImagePlus dfieldImp = BigWarpToDeformationFieldPlugIn.toImagePlus(
				data, ltm, bwTransform,
				ignoreAffine, flatten, false, virtual,
				dims, spacing, offset,
				nThreads);

		final RealTransformSequence total = new RealTransformSequence();
		total.add(toDfield(dfieldImp));
		total.add(bwTransform.affinePartOfTps());

		final RealIntervalIterator it = new RealIntervalIterator(testItvl, spacing);
		final InvertibleRealTransform tform = bwTransform.getTransformation();

		assertTrue("split affine forward", compare(tform, total, it, 1e-3));
	}

	@Test
	public void dfieldConcatExportTest() {

		final BigWarpTransform bwTransform = new BigWarpTransform(ltm);

		// constant parameters
		final boolean ignoreAffine = false;
		final boolean virtual = false;
		final boolean inverse = false;
		final long[] dims = new long[]{47, 56, 7};
		final double[] spacing = new double[]{0.8, 0.8, 1.6};
		final double[] offset = new double[]{0, 0, 0};
		final int nThreads = 1;

		final FinalRealInterval testItvl = new FinalRealInterval(
				new double[]{3.6, 3.6, 1.6},
				new double[]{32.0, 40.0, 9.6});

		// flattened
		final ImagePlus dfieldImpFlat = BigWarpToDeformationFieldPlugIn.toImagePlus(
				dataWithTransform, ltm, bwTransform,
				ignoreAffine, true, inverse, virtual,
				dims, spacing, offset,
				nThreads);
		final DisplacementFieldTransform dfieldFlat = toDfield(dfieldImpFlat);

		final InvertibleRealTransform tform = bwTransform.getTransformation();
		final RealTransform preTransform = dataWithTransform.getSourceInfo(0).getTransform();

		final RealTransformSequence totalTrueTransform = new RealTransformSequence();
		totalTrueTransform.add(tform);
		totalTrueTransform.add(preTransform);

		final RealIntervalIterator it = new RealIntervalIterator(testItvl, spacing);
		assertTrue("flatten forward", compare(totalTrueTransform, dfieldFlat, it, 1e-3));

		// not flattened
		final ImagePlus dfieldImpUnFlat = BigWarpToDeformationFieldPlugIn.toImagePlus(
				dataWithTransform, ltm, bwTransform,
				ignoreAffine, false, inverse, virtual,
				dims, spacing, offset,
				nThreads);
		final DisplacementFieldTransform dfieldUnflat = toDfield(dfieldImpUnFlat);

		it.reset();
		assertTrue("un-flattened forward", compare(tform, dfieldUnflat, it, 1e-3));
	}

	@Test
	public void dfieldQuantizationTest() throws IOException {

		final BigWarpTransform bwTransform = new BigWarpTransform(ltm);

		// constant parameters
		final boolean ignoreAffine = false;
		final boolean virtual = false;
		final boolean inverse = false;
		final double inverseTolerance = 0.01;
		final int inverseMaxIters = 1;

		final long[] dims = new long[]{47, 56, 7};
		final double[] spacing = new double[]{0.8, 0.8, 1.6};
		final double[] offset = new double[]{0, 0, 0};

		final DTYPE quantizedType = DTYPE.SHORT;
		final double maxQuantizationError = 0.01;

		final int[] blkSsize = new int[]{32, 32, 32};
		final RawCompression compression = new RawCompression();

		final String format = ExportDisplacementFieldFrame.FMT_N5;
		final int nThreads = 1;

		final FinalRealInterval testItvl = new FinalRealInterval(
				new double[]{3.6, 3.6, 1.6},
				new double[]{32.0, 40.0, 9.6});

		final File tmpFile = Files.createTempDirectory("bw-dfield-test-").toFile();
		tmpFile.deleteOnExit();
		final String n5BasePath = tmpFile.getCanonicalPath() + ".n5";

		final String fDset = "dfieldFloat";
		BigWarpToDeformationFieldPlugIn.writeN5(n5BasePath, fDset, ltm, bwTransform, data, dims, spacing, offset, "mm",
				blkSsize, compression, nThreads, format, false, DTYPE.FLOAT, Double.MAX_VALUE, inverse, inverseTolerance, inverseMaxIters);

		final String sDset = "dfieldShort";
		BigWarpToDeformationFieldPlugIn.writeN5(n5BasePath, sDset, ltm, bwTransform, data, dims, spacing, offset, "mm",
				blkSsize, compression, nThreads, format, false, DTYPE.SHORT, maxQuantizationError, inverse, inverseTolerance, inverseMaxIters);

		try (final N5Writer n5 = new N5Factory().openWriter(n5BasePath)) {
			assertTrue(n5.exists(fDset));
			final DatasetAttributes fAttrs = n5.getDatasetAttributes(fDset);
			assertTrue(fAttrs.getDataType().equals(DataType.FLOAT32));

			assertTrue(n5.exists(sDset));
			final DatasetAttributes sAttrs = n5.getDatasetAttributes(sDset);
			assertTrue(sAttrs.getDataType().equals(DataType.INT16));

			try {
				assertNotNull(n5.getAttribute(sDset, N5DisplacementField.MULTIPLIER_ATTR, double.class));
			} catch (final N5Exception ignore) {}

			final RealTransform dfieldF = N5DisplacementField.open(n5, fDset, false);
			final RealTransform dfieldS = N5DisplacementField.open(n5, sDset, false);

			final RealIntervalIterator it = new RealIntervalIterator(testItvl, spacing);
			assertTrue("quantization error", compare(dfieldF, dfieldS, it, 2 * maxQuantizationError));

			n5.remove();
		}
	}

	@Test
	public void dfieldFieldOfViewTest() throws IOException {

		final BigWarpTransform bwTransform = new BigWarpTransform(ltm);
		final InvertibleRealTransform tform = bwTransform.getTransformation();

		// constant parameters
		final boolean inverse = false;
		final double inverseTolerance = 0.01;
		final int inverseMaxIters = 1;

		final long[] dims = new long[]{11, 13, 17};
		final double[] spacing = new double[]{0.5, 1.0, 2.0};
		final double[] offset = new double[]{100, 50, 25};

		final int[] blkSsize = new int[]{64, 64, 64};
		final RawCompression compression = new RawCompression();

		final String format = ExportDisplacementFieldFrame.FMT_N5;
		final int nThreads = 1;

		final RealInterval totalItvl = FieldOfView.computeInterval( spacing, offset, dims );
		final RealInterval testItvl = FieldOfView.expand(totalItvl, -spacing[0], -spacing[1], -spacing[2]);

		final File tmpFile = Files.createTempDirectory("bw-dfield-test-").toFile();
		tmpFile.deleteOnExit();
		final String n5BasePath = tmpFile.getCanonicalPath() + ".n5";

		final String fDset = "dfield";
		BigWarpToDeformationFieldPlugIn.writeN5(n5BasePath, fDset, ltm, bwTransform, data, dims, spacing, offset, "mm",
				blkSsize, compression, nThreads, format, false, DTYPE.FLOAT, Double.MAX_VALUE, inverse, inverseTolerance, inverseMaxIters);

		try (final N5Writer n5 = new N5Factory().openWriter(n5BasePath)) {

			assertTrue(n5.exists(fDset));
			final RealTransform dfieldF = N5DisplacementField.open(n5, fDset, false);
			final RealIntervalIterator it = new RealIntervalIterator(testItvl, spacing);
			assertTrue("test over offset field-of-view", compare(tform, dfieldF, it, 1e-3));

			n5.remove();
		}
	}

	public static DisplacementFieldTransform toDfield(final ImagePlus dfieldImp) {

		final double[] spacing = new double[]{
				dfieldImp.getCalibration().pixelWidth, dfieldImp.getCalibration().pixelHeight, dfieldImp.getCalibration().pixelDepth
		};
		final double[] offset = new double[]{
				dfieldImp.getCalibration().xOrigin, dfieldImp.getCalibration().yOrigin, dfieldImp.getCalibration().zOrigin
		};

		final RandomAccessibleInterval<FloatType> img = ImageJFunctions.wrapRealNative(dfieldImp);
		final RandomAccessibleInterval<FloatType> dfimg = Views.moveAxis(img, 2, 0);
		return new DisplacementFieldTransform(dfimg, spacing, offset);
	}

	public static boolean compare(final RealTransform tform, final ImagePlus dfieldImp, final RealIntervalIterator it, final double tol) {

		return compare(tform, toDfield(dfieldImp), it, tol);
	}

	public static boolean compare(final RealTransform a, final RealTransform b, final RealIntervalIterator it, final double tol) {

		final RealPoint gt = new RealPoint(3);
		final RealPoint df = new RealPoint(3);
		while (it.hasNext()) {
			it.fwd();
			a.apply(it, gt);
			b.apply(it, df);
			final double dist = Util.distance(gt, df);
			if (dist > tol) {
				System.out.println("it  : " + it);
				System.out.println("dist: " + dist);
				return false;
			}
		}

		return true;
	}

}
