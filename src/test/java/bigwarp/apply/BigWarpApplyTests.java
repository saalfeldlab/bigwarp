package bigwarp.apply;

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
import net.imglib2.type.numeric.integer.UnsignedByteType;

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
		assertEquals(0, img.getAt(14, 6, 6).get());
	}

	@Test
	public void testExportOffset() {

		final long[] pt = new long[] {16, 8, 4};
		final long[] tgtOffset = new long[] { -3, -2, -1 };
		final double[] tgtOffsetDouble = Arrays.stream(tgtOffset).mapToDouble( x -> x ).toArray();

		final long[] tlatedPt = IntStream.of(0, 1, 2).mapToLong( i -> {
			return pt[i] - tgtOffset[i];
		}).toArray();

		double[] resolution = new double[] {1,1,1};
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
		final List<ImagePlus> resList = transformToTarget( mvg, tgt, ltm ); 
		assertEquals(1, resList.size());

		final ImagePlus res = resList.get(0);
		assertResolutionsEqual(tgt, res);
		assertOriginsEqual(tgt, res);

		final Img<UnsignedByteType> img = ImageJFunctions.wrapByte(res);
		assertEquals(1, img.getAt(tlatedPt).get());
		assertEquals(0, img.getAt(0, 0, 0).get());
		assertEquals(0, img.getAt(14, 6, 6).get());
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

	private static List<ImagePlus> transformToTarget(ImagePlus mvg, ImagePlus tgt, LandmarkTableModel ltm) {

		return ApplyBigwarpPlugin.apply(mvg, tgt,
				ltm,
				BigWarpTransform.AFFINE,
				ApplyBigwarpPlugin.TARGET,
				null, // fov pt filter
				ApplyBigwarpPlugin.TARGET,
				null, // res option
				null, // fov spac
				null, // offset spac
				Interpolation.NEARESTNEIGHBOR,
				false, // virtual
				true, // wait
				1); // nThreads
	}

}
