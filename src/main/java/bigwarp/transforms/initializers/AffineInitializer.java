package bigwarp.transforms.initializers;

import java.util.function.Function;

import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import bigwarp.transforms.ModelTransformSolver;
import bigwarp.transforms.WrappedCoordinateTransform;
import mpicbg.models.AbstractAffineModel2D;
import mpicbg.models.AbstractAffineModel3D;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;

public class AffineInitializer implements Function<RealPoint, RealPoint> {

	LandmarkTableModel ltm;
	private RealPoint q;

	public AffineInitializer(LandmarkTableModel ltm) {
		this.ltm = ltm;
		q = new RealPoint(ltm.getNumdims());
	}

	@Override
	public RealPoint apply(RealPoint p) {

		AffineGet affine;
		int nd = ltm.getNumdims();
		final BigWarpTransform t = new BigWarpTransform(ltm);
		t.setTransformType(BigWarpTransform.AFFINE);

		final ModelTransformSolver solver = new ModelTransformSolver(t.getModelType());
		final WrappedCoordinateTransform res = solver.solve(ltm);

		if (nd == 2) {
			AffineTransform2D affine2d = new AffineTransform2D();
			affine = BigWarpTransform.affine2d((AbstractAffineModel2D)solver.getModel(), affine2d);
		} else if (nd == 3) {
			AffineTransform3D affine3d = new AffineTransform3D();
			affine = BigWarpTransform.affine3d((AbstractAffineModel3D)solver.getModel(), affine3d);
		} else
			throw new RuntimeException("Affine initialization only supported for 2D or 3D.");

		affine.apply(p, q);
		return q;
	}

}
