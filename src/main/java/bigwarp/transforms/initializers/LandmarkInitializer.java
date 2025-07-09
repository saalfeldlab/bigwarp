package bigwarp.transforms.initializers;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;

import bigwarp.landmarks.LandmarkTableModel;
import net.imglib2.KDTree;
import net.imglib2.RealPoint;
import net.imglib2.neighborsearch.KNearestNeighborSearch;
import net.imglib2.neighborsearch.KNearestNeighborSearchOnKDTree;
import net.imglib2.pointinterpolation.PointInterpolator;

public class LandmarkInitializer extends PointInterpolator<RealPoint> implements Function<RealPoint,RealPoint>{

	public LandmarkInitializer( LandmarkTableModel ltm, int N ) {
		super(init(ltm, N));
	}
	
	private static KNearestNeighborSearch<RealPoint> init(LandmarkTableModel ltm, int N) {
		final List<RealPoint> srcPositions = toPointList(ltm.getMovingPoints());
		final List<RealPoint> targetPositions = toPointList(ltm.getFixedPoints());
		KDTree<RealPoint> tree = new KDTree<RealPoint>(srcPositions, targetPositions);
		return new KNearestNeighborSearchOnKDTree<>(tree, N);
	}

	@Override
	public RealPoint apply(RealPoint p) {
		setPosition(p);
		return get();
	}

	private static List<RealPoint> toPointList( List<Double[]> pts ) {

		return pts.stream().map(p -> {
			return RealPoint.wrap(ArrayUtils.toPrimitive(p));
		}).collect(Collectors.toList());
	}


}
