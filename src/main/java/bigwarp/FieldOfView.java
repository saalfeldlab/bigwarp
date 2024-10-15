package bigwarp;

import bdv.viewer.Source;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.BoundingBoxEstimation;

public class FieldOfView {

	public static RealInterval getPhysicaInterval(final Source<?> source) {

		final AffineTransform3D tform = new AffineTransform3D();
		return BoundingBoxEstimation.corners(tform, source.getSource(0, 0));
	}

	public static RealInterval fromMinSize(final double[] min, final double[] size) {

		final int nd = size.length;
		final double[] max = new double[nd];
		for (int i = 0; i < nd; i++)
			max[i] = min[i] + size[i];

		return new FinalRealInterval(min, max);
	}

	public static RealInterval fromPixelMinSize(final double[] minPixel, final double[] sizePixel, final double[] resolution) {

		final int nd = sizePixel.length;
		final double[] min = new double[nd];
		final double[] max = new double[nd];
		for (int i = 0; i < nd; i++) {
			min[i] = resolution[i] * min[i];
			max[i] = resolution[i] * (min[i] + sizePixel[i]);
		}

		return new FinalRealInterval(min, max);
	}

	public static RealInterval computeInterval(
			final double[] resolution,
			final double[] offset,
			final long[] dimensions) {

		final int nd = resolution.length;
		final double[] max = new double[resolution.length];
		for (int i = 0; i < nd; i++) {
			max[i] = offset[i] + resolution[i] * dimensions[i];
		}
		return new FinalRealInterval(offset, max);
	}

	public static RealInterval expand(
			final RealInterval interval,
			final double... amounts) {

		final int nd = interval.numDimensions();
		final double[] min = new double[nd];
		final double[] max = new double[nd];

		for (int i = 0; i < nd; i++) {
			min[i] = interval.realMin(i) - amounts[i];
			max[i] = interval.realMax(i) + amounts[i];
		}
		return new FinalRealInterval(min, max);
	}

}
