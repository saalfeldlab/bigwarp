package bigwarp.apply;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Test;

import bdv.ij.ApplyBigwarpPlugin;
import bdv.viewer.Interpolation;
import bigwarp.BigWarpData;
import bigwarp.BigWarpInit;
import bigwarp.BigWarpTestUtils;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import ij.ImagePlus;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.iterator.RealIntervalIterator;
import net.imglib2.realtransform.BoundingBoxEstimation;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;

public class BigWarpApplyTests {

	private static final double EPS = 1e-6;

	@Test
	public void testExportSimple() {

		final long[] pt = new long[]{16, 8, 4};
		final ImagePlus mvg = new BigWarpTestUtils.TestImagePlusBuilder().title("mvg")
				.position(pt).build();

		final ImagePlus tgt = new BigWarpTestUtils.TestImagePlusBuilder().title("tgt")
				.position(pt).build();

		final BigWarpData<UnsignedByteType> bwData = BigWarpInit.initData();
		BigWarpInit.add(bwData, BigWarpInit.createSources(bwData, mvg, 0, 0, true));
		BigWarpInit.add(bwData, BigWarpInit.createSources(bwData, tgt, 1, 0, false));
		bwData.wrapMovingSources();

		final LandmarkTableModel ltm = BigWarpTestUtils.identityLandmarks(3);
		final List<ImagePlus> resList = transformToTarget(mvg, tgt, ltm);
		assertEquals(1, resList.size());

		final ImagePlus res = resList.get(0);
		assertResolutionsEqual(tgt, res);
		assertOriginsEqual(tgt, res);;

		final Img<UnsignedByteType> img = ImageJFunctions.wrapByte(res);
		assertEquals(1, img.getAt(pt).get());
		assertEquals(0, img.getAt(0, 0, 0).get());
		assertEquals(0, img.getAt(pt[0] - 2, pt[1] - 2, pt[2] - 2).get());
	}

	@Test
	public void testExportOffset() {

		final long[] pt = new long[]{16, 8, 4};
		final long[] tgtOffset = new long[]{-3, -2, -1};
		final double[] tgtOffsetDouble = Arrays.stream(tgtOffset).mapToDouble(x -> x).toArray();

		final long[] tlatedPt = IntStream.of(0, 1, 2).mapToLong(i -> {
			return pt[i] - tgtOffset[i];
		}).toArray();

		double[] resolution = new double[]{1, 1, 1};
		final ImagePlus mvg = new BigWarpTestUtils.TestImagePlusBuilder().title("mvg")
				.resolution(resolution)
				.position(pt).build();

		final ImagePlus tgt = new BigWarpTestUtils.TestImagePlusBuilder().title("tgt")
				.resolution(resolution)
				.position(pt).offset(tgtOffsetDouble).build();

		final BigWarpData<UnsignedByteType> bwData = BigWarpInit.initData();
		BigWarpInit.add(bwData, BigWarpInit.createSources(bwData, mvg, 0, 0, true));
		BigWarpInit.add(bwData, BigWarpInit.createSources(bwData, tgt, 1, 0, false));
		bwData.wrapMovingSources();

		final LandmarkTableModel ltm = BigWarpTestUtils.identityLandmarks(3);
		final List<ImagePlus> resList = transformToTarget(mvg, tgt, ltm);
		assertEquals(1, resList.size());

		final ImagePlus res = resList.get(0);
		assertResolutionsEqual(tgt, res);
		assertOriginsEqual(tgt, res);

		final Img<UnsignedByteType> img = ImageJFunctions.wrapByte(res);
		assertEquals(1, img.getAt(tlatedPt).get());
		assertEquals(0, img.getAt(0, 0, 0).get());
		assertEquals(0, img.getAt(tlatedPt[0] - 2, tlatedPt[1] - 2, tlatedPt[2] - 2).get());
	}

	@Test
	public void testExportFovOffset() {

		final double[] resolution = new double[]{1, 1, 1};
		final long[] pt = new long[]{16, 8, 4};

		// choose the min such that the non-zero point is at [0,0,0]
		final double[] min = Arrays.stream(pt).mapToDouble(x -> x).toArray();

		// choose the fov such that the image is size [2,2,2]
		final double[] fov = IntStream.of(0, 1, 2).mapToDouble(i -> {
			return 1.9 * resolution[i];
		}).toArray();

		final ImagePlus mvg = new BigWarpTestUtils.TestImagePlusBuilder().title("mvg")
				.resolution(resolution)
				.position(pt).build();

		final BigWarpData<UnsignedByteType> bwData = BigWarpInit.initData();
		BigWarpInit.add(bwData, BigWarpInit.createSources(bwData, mvg, 0, 0, true));
		bwData.wrapMovingSources();

		final LandmarkTableModel ltm = BigWarpTestUtils.identityLandmarks(3);
		final List<ImagePlus> resList = transformToSpec(
				mvg,
				min, fov, resolution,
				ltm);

		assertEquals(1, resList.size());
		final ImagePlus result = resList.get(0);
		assertResolutionsEqual(resolution, result);
		assertOriginsEqual(min, result);

		final Img<UnsignedByteType> img = ImageJFunctions.wrapByte(result);
		assertArrayEquals("result image the wrong size", new long[]{2, 2, 2}, Intervals.dimensionsAsLongArray(img));

		assertEquals(1, img.getAt(0, 0, 0).get());
		assertEquals(0, img.getAt(1, 1, 1).get());
	}

	@Test
	public void testExportPtsSimple() {

		final double[] resolution = new double[]{1, 1, 1};
		final long[] pt = new long[]{16, 8, 4};

		final ImagePlus mvg = new BigWarpTestUtils.TestImagePlusBuilder().title("mvg")
				.resolution(resolution)
				.position(pt).build();

		final BigWarpData<UnsignedByteType> bwData = BigWarpInit.initData();
		BigWarpInit.add(bwData, BigWarpInit.createSources(bwData, mvg, 0, 0, true));
		bwData.wrapMovingSources();

		// add a set of landmarks that define a 3x3x3 box around the non-zero pixel
		final double[] min = new double[]{15.0, 7.0, 3.0};
		final double[] max = new double[]{17.0, 9.0, 5.0};
		final double[] step = new double[]{1.0, 1.0, 1.0};
		final long[] expectedResultSize = new long[]{3, 3, 3};

		final LandmarkTableModel ltm = BigWarpTestUtils.identityLandmarks(3);
		BigWarpTestUtils.addBboxLandmarks(ltm, new RealIntervalIterator(min, max, step), "bbox-%d");
		final List<ImagePlus> resList = transformToPtsSpec(mvg, resolution, ltm, "bbox.*");

		assertEquals(1, resList.size());
		final ImagePlus result = resList.get(0);
		assertResolutionsEqual(resolution, result);
		assertOriginsEqual(min, result);

		final Img<UnsignedByteType> img = ImageJFunctions.wrapByte(result);
		assertArrayEquals("result image the wrong size", expectedResultSize, Intervals.dimensionsAsLongArray(img));
		assertEquals(1, img.getAt(1, 1, 1).get());
		assertEquals(0, img.getAt(0, 0, 0).get());
	}

	@Test
	public void testExportMvgWarped() {

		final long[] pt = new long[]{16, 8, 4};
		final long[] size = new long[]{32, 16, 8};

		final ImagePlus mvg = new BigWarpTestUtils.TestImagePlusBuilder().title("mvg")
				.size(size)
				.position(pt).build();

		final BigWarpData<UnsignedByteType> bwData = BigWarpInit.initData();
		BigWarpInit.add(bwData, BigWarpInit.createSources(bwData, mvg, 0, 0, true));
		bwData.wrapMovingSources();

		final Scale3D tform = new Scale3D(4, 3, 2);
		final LandmarkTableModel ltm = BigWarpTestUtils.landmarks(new IntervalIterator(new int[]{2, 2, 2}), tform);

		final long[] expectedResultMax = new long[3];
		Arrays.setAll(expectedResultMax, i -> (long)((size[i] - 1) * tform.get(i, i)));

		final List<ImagePlus> resList = transformMvgWarped(mvg, ltm);

		assertEquals(1, resList.size());
		final ImagePlus result = resList.get(0);
		assertResolutionsEqual(mvg, result);
		assertOriginsEqual(mvg, result);

		final Img<UnsignedByteType> img = ImageJFunctions.wrapByte(result);
		assertArrayEquals("result image the wrong size", expectedResultMax, Intervals.maxAsLongArray(img));
		assertEquals(1, img.getAt(16 * 4, 8 * 3, 4 * 2).get());
		assertEquals(0, img.getAt(0, 0, 0).get());
	}

	private static void assertResolutionsEqual(ImagePlus expected, ImagePlus actual) {

		assertEquals("width", expected.getCalibration().pixelWidth, actual.getCalibration().pixelWidth, EPS);
		assertEquals("height", expected.getCalibration().pixelHeight, actual.getCalibration().pixelHeight, EPS);
		assertEquals("depth", expected.getCalibration().pixelDepth, actual.getCalibration().pixelDepth, EPS);
	}

	private static void assertOriginsEqual(ImagePlus expected, ImagePlus actual) {

		assertEquals("origin x", expected.getCalibration().xOrigin, actual.getCalibration().xOrigin, EPS);
		assertEquals("origin y", expected.getCalibration().yOrigin, actual.getCalibration().yOrigin, EPS);
		assertEquals("origin z", expected.getCalibration().zOrigin, actual.getCalibration().zOrigin, EPS);
	}

	private static void assertResolutionsEqual(double[] expected, ImagePlus actual) {

		assertEquals("width", expected[0], actual.getCalibration().pixelWidth, EPS);
		assertEquals("height", expected[0], actual.getCalibration().pixelHeight, EPS);
		assertEquals("depth", expected[2], actual.getCalibration().pixelDepth, EPS);
	}

	private static void assertOriginsEqual(double[] expected, ImagePlus actual) {

		assertEquals("origin x", expected[0], actual.getCalibration().xOrigin, EPS);
		assertEquals("origin y", expected[1], actual.getCalibration().yOrigin, EPS);
		assertEquals("origin z", expected[2], actual.getCalibration().zOrigin, EPS);
	}

	private static List<ImagePlus> transformToTarget(ImagePlus mvg, ImagePlus tgt, LandmarkTableModel ltm) {

		final BigWarpData<?> bwData = BigWarpInit.createBigWarpDataFromImages(mvg, tgt);
		bwData.wrapMovingSources();
		final BoundingBoxEstimation bboxEst = new BoundingBoxEstimation(BoundingBoxEstimation.Method.CORNERS);
		final InvertibleRealTransform invXfm = new BigWarpTransform( ltm, BigWarpTransform.AFFINE ).getTransformation();

		 return ApplyBigwarpPlugin.apply(
				bwData,
				ltm,
				invXfm,
				BigWarpTransform.AFFINE, // tform type
				ApplyBigwarpPlugin.TARGET, // fov option 
				null,
				bboxEst,
				ApplyBigwarpPlugin.TARGET,
				null,
				null,
				null,
				Interpolation.NEARESTNEIGHBOR,
				false, // virtual 
				1, // nThreads
				true,
				null, // writeOpts
				false);
	}

	private static List<ImagePlus> transformToSpec(final ImagePlus mvg,
			final double[] offset, final double[] fov, final double[] res,
			final LandmarkTableModel ltm) {

		ImagePlus tgt = null;
		final BigWarpData<?> bwData = BigWarpInit.createBigWarpDataFromImages(mvg, tgt);
		bwData.wrapMovingSources();
		final BoundingBoxEstimation bboxEst = new BoundingBoxEstimation(BoundingBoxEstimation.Method.CORNERS);
		final InvertibleRealTransform invXfm = new BigWarpTransform( ltm, BigWarpTransform.AFFINE ).getTransformation();
		
		 return ApplyBigwarpPlugin.apply(
					bwData,
					ltm,
					invXfm,
					BigWarpTransform.AFFINE, // tform type
					ApplyBigwarpPlugin.SPECIFIED_PHYSICAL, // fov option 
					null,
					bboxEst,
					ApplyBigwarpPlugin.SPECIFIED,
					res,
					fov,
					offset,
					Interpolation.NEARESTNEIGHBOR,
					false, // virtual 
					1, // nThreads
					true,
					null, // writeOpts
					false);
	}

	private static List<ImagePlus> transformToPtsSpec(final ImagePlus mvg,
			final double[] res,
			final LandmarkTableModel ltm,
			final String fieldOfViewPointFilter) {

		ImagePlus tgt = null;
		final BigWarpData<?> bwData = BigWarpInit.createBigWarpDataFromImages(mvg, tgt);
		bwData.wrapMovingSources();
		final BoundingBoxEstimation bboxEst = new BoundingBoxEstimation(BoundingBoxEstimation.Method.CORNERS);
		final InvertibleRealTransform invXfm = new BigWarpTransform( ltm, BigWarpTransform.AFFINE ).getTransformation();
		
		 return ApplyBigwarpPlugin.apply(
					bwData,
					ltm,
					invXfm,
					BigWarpTransform.AFFINE, // tform type
					ApplyBigwarpPlugin.LANDMARK_POINTS, // fov option 
					fieldOfViewPointFilter,
					bboxEst,
					ApplyBigwarpPlugin.SPECIFIED,
					res,
					null, // fov
					null, // offset
					Interpolation.NEARESTNEIGHBOR,
					false, // virtual 
					1, // nThreads
					true,
					null, // writeOpts
					false);
	}

	private static List<ImagePlus> transformMvgWarped(final ImagePlus mvg,
			final LandmarkTableModel ltm) {

		ImagePlus tgt = null;
		final BigWarpData<?> bwData = BigWarpInit.createBigWarpDataFromImages(mvg, tgt);
		bwData.wrapMovingSources();
		final BoundingBoxEstimation bboxEst = new BoundingBoxEstimation(BoundingBoxEstimation.Method.CORNERS);
		final InvertibleRealTransform invXfm = new BigWarpTransform( ltm, BigWarpTransform.AFFINE ).getTransformation();

		return ApplyBigwarpPlugin.apply(
				bwData,
				ltm,
				invXfm,
				BigWarpTransform.AFFINE,
				ApplyBigwarpPlugin.MOVING_WARPED,
				"", // fov pt filter
				bboxEst,
				ApplyBigwarpPlugin.MOVING,
				null, // res option
				null, // fov spec
				null, // offset spac
				Interpolation.NEARESTNEIGHBOR,
				false, // virtual
				1, // nThreads
				true,
				null, // writeOpts
				false);
	}

}
