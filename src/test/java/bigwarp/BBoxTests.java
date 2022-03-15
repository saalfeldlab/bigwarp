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
