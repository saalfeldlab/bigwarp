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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import org.janelia.saalfeldlab.n5.codec.DatasetCodecInfo;
import org.janelia.saalfeldlab.n5.imglib2.N5DisplacementField;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.junit.Before;
import org.junit.Ignore;
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
import bigwarp.transforms.NgffTransformations;
import ij.ImagePlus;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.iterator.RealIntervalIterator;
import net.imglib2.realtransform.AffineGet;
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
		final LinkedHashMap<Source<?>, SourceInfo> infos = BigWarpInit.createSources(data, imp, id, 0, isMoving);
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

	/**
	 * {@link #dfieldIgnoreAffineExportTest} recomposes the split parts in memory, so it
	 * only shows that the field had the affine removed - it never checks that the affine
	 * survived being written. This one goes through {@code writeN5} and reads both parts
	 * back out of the container.
	 */
	@Test
	public void dfieldSplitAffineN5ExportTest() throws IOException {

		final BigWarpTransform bwTransform = new BigWarpTransform(ltm);
		final InvertibleRealTransform tform = bwTransform.getTransformation();

		// constant parameters
		final boolean flatten = false;
		final boolean inverse = false;
		final double inverseTolerance = 0.01;
		final int inverseMaxIters = 1;

		final long[] dims = new long[]{47, 56, 7};
		final double[] spacing = new double[]{0.8, 0.8, 1.6};
		final double[] offset = new double[]{0, 0, 0};

		final int[] blkSize = new int[]{32, 32, 32};
		final RawCompression compression = new RawCompression();

		final String format = ExportDisplacementFieldFrame.FMT_N5;
		final int nThreads = 1;

		final FinalRealInterval testItvl = new FinalRealInterval(
				new double[]{3.6, 3.6, 1.6},
				new double[]{32.0, 40.0, 9.6});

		final File tmpFile = Files.createTempDirectory("bw-dfield-test-").toFile();
		tmpFile.deleteOnExit();
		final String n5BasePath = tmpFile.getCanonicalPath() + ".n5";

		final String splitDset = "dfieldSplitAffine";
		BigWarpToDeformationFieldPlugIn.writeN5(n5BasePath, splitDset, ltm, bwTransform, data, dims, spacing, offset, "mm",
				blkSize, compression, nThreads, format, true, flatten, DTYPE.FLOAT, Double.MAX_VALUE, inverse, inverseTolerance, inverseMaxIters);

		final String wholeDset = "dfieldWhole";
		BigWarpToDeformationFieldPlugIn.writeN5(n5BasePath, wholeDset, ltm, bwTransform, data, dims, spacing, offset, "mm",
				blkSize, compression, nThreads, format, false, flatten, DTYPE.FLOAT, Double.MAX_VALUE, inverse, inverseTolerance, inverseMaxIters);

		try (final N5Writer n5 = new N5Factory().openWriter(n5BasePath)) {

			final AffineGet storedAffine = N5DisplacementField.openAffine(n5, splitDset);
			assertNotNull("split export must store the affine part", storedAffine);
			assertArrayEquals("stored affine is the affine part of the tps",
					bwTransform.affinePartOfTps().getRowPackedCopy(), storedAffine.getRowPackedCopy(), 1e-9);

			assertNull("un-split export must not store an affine",
					N5DisplacementField.openAffine(n5, wholeDset));

			// open() recomposes as dfield-then-affine, matching the order writeN5 uses
			final RealTransform recomposed = N5DisplacementField.open(n5, splitDset, false);
			final RealIntervalIterator it = new RealIntervalIterator(testItvl, spacing);
			assertTrue("split affine round trip", compare(tform, recomposed, it, 1e-3));

			// peeling the stored affine back off leaves the residual field, which must not
			// be the whole transformation - otherwise nothing was actually split out and the
			// assertion above would hold for the wrong reason
			final RealTransformSequence residual = new RealTransformSequence();
			residual.add(recomposed);
			residual.add(storedAffine.inverse());

			it.reset();
			assertFalse("the stored field alone is not the whole transformation (the printed distance above is expected)",
					compare(tform, residual, it, 1e-3));

			n5.remove();
		}
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

	@Test
	public void dfieldNgffExportTest() throws IOException {

		final BigWarpTransform bwTransform = new BigWarpTransform(ltm);
		final InvertibleRealTransform tform = bwTransform.getTransformation();

		// constant parameters
		final boolean inverse = false;
		final double inverseTolerance = 0.01;
		final int inverseMaxIters = 1;

		final long[] dims = new long[]{47, 56, 7};
		final double[] spacing = new double[]{0.8, 0.8, 1.6};
		final double[] offset = new double[]{0, 0, 0};

		final int[] blkSsize = new int[]{64, 64, 64};
		final RawCompression compression = new RawCompression();

		final String format = ExportDisplacementFieldFrame.FMT_NGFF;
		final int nThreads = 1;

		final FinalRealInterval testItvl = new FinalRealInterval(
				new double[]{3.6, 3.6, 1.6},
				new double[]{32.0, 40.0, 9.6});

		final File tmpFile = Files.createTempDirectory("bw-dfield-ngff-test-").toFile();
		tmpFile.deleteOnExit();
		final String zarrBasePath = tmpFile.getCanonicalPath() + ".zarr";

		final String fDset = "dfield";
		BigWarpToDeformationFieldPlugIn.writeN5(zarrBasePath, fDset, ltm, bwTransform, data, dims, spacing, offset, "mm",
				blkSsize, compression, nThreads, format, false, DTYPE.FLOAT, Double.MAX_VALUE, inverse, inverseTolerance, inverseMaxIters);

		// read back with the same gson adapters the export wrote with, so the v05
		// CoordinateTransform interface can be deserialized
		try (final N5Writer n5 = new N5Factory().gsonBuilder(NgffTransformations.gsonBuilder()).openWriter(zarrBasePath)) {

			assertTrue(n5.exists(fDset));

			// the NGFF export writes the displacement field at fDset and references it
			// from a coordinate transformation stored on the root group
			final RealTransform dfieldF = NgffTransformations.open(n5, zarrBasePath + "?/#coordinateTransformations[0]");
			assertNotNull("ngff transform", dfieldF);

			final RealIntervalIterator it = new RealIntervalIterator(testItvl, spacing);
			assertTrue("ngff dfield matches transform", compare(tform, dfieldF, it, 1e-3));

			n5.remove();
		}
	}

	/**
	 * The quantized counterpart of {@link #dfieldNgffExportTest}: the same NGFF export,
	 * but asking for {@link DTYPE#SHORT} with a bounded quantization error, as
	 * {@link #dfieldQuantizationTest} does for the N5 format.
	 * <p>
	 * <b>This test is expected to fail.</b> The NGFF branch of
	 * {@code BigWarpToDeformationFieldPlugIn.writeN5} ignores {@code dtype} and
	 * {@code maxError} entirely - it hands the field to
	 * {@code NgffTransformations.save}, which writes it at its in-memory type - so
	 * nothing is quantized and the array is stored at full float width.
	 * <p>
	 * NGFF has no equivalent of the {@code multiplier}/{@code offset} attributes
	 * {@link N5DisplacementField} uses, so the quantization must live in the array's
	 * codec chain: a {@code scale_offset_cast} codec
	 * ({@code org.janelia.saalfeldlab.n5.codec.dataset.ScaleOffsetCastCodec}) that
	 * narrows to {@code int16} on encode and restores real-valued displacements on
	 * decode. That is what the assertions below are written against: readers still
	 * see a floating-point dataset, but the stored blocks are {@code int16}.
	 * <p>
	 * Ignored until the dataset codecs land in a released {@code n5-zarr}: the write
	 * path is already correct - {@code NgffTransformations.quantizationCodecs} builds
	 * the {@code ScaleOffsetCastCodecInfo} and {@code N5Writer.createDataset} returns
	 * it on the attributes it hands back - but the released {@code n5-zarr 2.0.2}
	 * drops array-to-array codecs when it serializes the array metadata. The written
	 * {@code zarr.json} carries only {@code "codecs":[{"name":"bytes",...}]}, so
	 * {@code getDatasetCodecInfos()} reads back empty and the assertion below fails.
	 * Re-enable once {@code n5-zarr.version} in the pom points at a build that
	 * serializes them.
	 */
	@Ignore("needs dataset codec (scale_offset_cast) support from a dev n5-zarr; 2.0.2 drops it on write")
	@Test
	public void dfieldNgffQuantizedExportTest() throws IOException {

		final BigWarpTransform bwTransform = new BigWarpTransform(ltm);
		final InvertibleRealTransform tform = bwTransform.getTransformation();

		// constant parameters
		final boolean inverse = false;
		final double inverseTolerance = 0.01;
		final int inverseMaxIters = 1;

		final long[] dims = new long[]{47, 56, 7};
		final double[] spacing = new double[]{0.8, 0.8, 1.6};
		final double[] offset = new double[]{0, 0, 0};

		final DTYPE quantizedType = DTYPE.SHORT;
		final double maxQuantizationError = 0.01;

		final int[] blkSsize = new int[]{64, 64, 64};
		final RawCompression compression = new RawCompression();

		final String format = ExportDisplacementFieldFrame.FMT_NGFF;
		final int nThreads = 1;

		final FinalRealInterval testItvl = new FinalRealInterval(
				new double[]{3.6, 3.6, 1.6},
				new double[]{32.0, 40.0, 9.6});

		final File tmpFile = Files.createTempDirectory("bw-dfield-ngff-quant-test-").toFile();
		tmpFile.deleteOnExit();
		final String zarrBasePath = tmpFile.getCanonicalPath() + ".zarr";

		final String fDset = "dfield";
		BigWarpToDeformationFieldPlugIn.writeN5(zarrBasePath, fDset, ltm, bwTransform, data, dims, spacing, offset, "mm",
				blkSsize, compression, nThreads, format, false, quantizedType, maxQuantizationError, inverse, inverseTolerance, inverseMaxIters);

		try (final N5Writer n5 = new N5Factory().gsonBuilder(NgffTransformations.gsonBuilder()).openWriter(zarrBasePath)) {

			assertTrue(n5.exists(fDset));

			// the field group holds the multiscales metadata; the array is the child s0
			final String fArray = fDset + "/" + NgffTransformations.FIELD_SCALE_DATASET;
			final DatasetAttributes attrs = n5.getDatasetAttributes(fArray);

			// the dataset's own type is what readers get back, so it must stay real-valued
			final DataType decodedType = attrs.getDataType();
			assertTrue("quantized ngff dfield must still decode to a floating point type, was " + decodedType,
					decodedType == DataType.FLOAT32 || decodedType == DataType.FLOAT64);

			// ... while the blocks actually written are int16. Walk the dataset codec
			// chain to get the on-disk type: each codec reports what it encodes to.
			final DatasetCodecInfo[] codecs = attrs.getDatasetCodecInfos();
			assertTrue("quantized ngff export must add a dataset codec", codecs != null && codecs.length > 0);

			DataType encodedType = decodedType;
			for (final DatasetCodecInfo codec : codecs)
				encodedType = codec.encodedDataType(encodedType);

			assertEquals("quantized ngff dfield must be stored as int16", DataType.INT16, encodedType);

			// and the values must survive the round trip to within the requested error
			// (plus the interpolation error the un-quantized export is held to)
			final RealTransform dfieldF = NgffTransformations.open(n5, zarrBasePath + "?/#coordinateTransformations[0]");
			assertNotNull("ngff transform", dfieldF);

			final RealIntervalIterator it = new RealIntervalIterator(testItvl, spacing);
			assertTrue("quantized ngff dfield matches transform",
					compare(tform, dfieldF, it, 1e-3 + 2 * maxQuantizationError));

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
