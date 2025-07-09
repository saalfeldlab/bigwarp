package net.imglib2.pointinterpolation;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;

import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.Sampler;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.neighborsearch.KNearestNeighborSearch;


public class PointInterpolator<V extends RealLocalizable> extends RealPoint implements RealRandomAccess< RealPoint >
{
	final protected KNearestNeighborSearch< V > search;

	final RealPoint value;

	public PointInterpolator( final KNearestNeighborSearch< V > search )
	{
		super( search.numDimensions() );
		this.search = search;
		value = new RealPoint(search.numDimensions());
	}

	@Override
	public RealPoint get()
	{
		setZero(value);

		// Build matrix of k nearest points
		final int K = search.getK();
		final DMatrixRMaj kPts = new DMatrixRMaj(numDimensions(), K); 	// D x K
		final DMatrixRMaj vPts = new DMatrixRMaj(numDimensions(), K); 	// D x K
		final DMatrixRMaj weights = new DMatrixRMaj(K, 1);				// K x 1
		final DMatrixRMaj pVec = new DMatrixRMaj(numDimensions(), 1, 	// D x 1
				true, positionAsDoubleArray());

		// Search for k nearest neighbors at current position
		int numNeighbors = 0;
		search.search(this);

		// Fill matrix with neighbor positions
		for (int i = 0; i < search.getK(); i++) {

			final Sampler<V> sampler = search.getSampler(i);

			final V value = sampler.get();
			if( value != null )
				numNeighbors++;
			else
				break;

			RealLocalizable neighbor = search.getPosition(i);
			for (int d = 0; d < numDimensions(); d++) {
				kPts.set(d, i, neighbor.getDoublePosition(d));
				vPts.set(d, i, value.getDoublePosition(d));
			}
		}

		if (numNeighbors == 0)
			return value;

		/*
		 * Find a set of weights w of the k nearest points that best approximate
		 * the point p.
		 * 
		 * Then create a linear combination of the values obtained from the
		 * sampler below using those same weights.
		 */
		
		// Solve for weights using least squares: kPts * w = pVec
		final LinearSolverDense<DMatrixRMaj> solver = LinearSolverFactory_DDRM.pseudoInverse(true);
		solver.setA(kPts);
		solver.solve(pVec, weights);

		// Use weights to create linear combination of neighbor values
		for ( int i = 0; i < numNeighbors; ++i )
		{
			final Sampler< V > sampler = search.getSampler( i );
			final V vi = sampler.get();
			double wi = weights.get(i, 0);
			
			// Add weighted contribution to result
			linComboModifyA(value, 1.0, vi, wi);
		}


		return value;
	}

	private void setZero( RealPoint a ) {

		int nd = a.numDimensions();
		for (int i = 0; i < nd; i++)
			a.setPosition(0.0, i);

	}

	private void linComboModifyA( RealPoint a, double wa, RealLocalizable b, double wb ) {
		
		int nd = a.numDimensions();
		for( int i = 0; i < nd; i++ ) {
			a.setPosition( wa * a.getDoublePosition(i) + wb * b.getDoublePosition(i), i );
		}
	}

	@Override
	public PointInterpolator< V > copy()
	{
		return new PointInterpolator<V>(search);
	}

	@Override
	public PointInterpolator< V > copyRealRandomAccess()
	{
		return copy();
	}

	public static class PointInterpolatorFactory<V extends RealLocalizable> implements InterpolatorFactory< RealPoint, KNearestNeighborSearch< V > >
	{

		public PointInterpolatorFactory() {}

		@Override
		public PointInterpolator<V> create(final KNearestNeighborSearch<V> search) {

			return new PointInterpolator<V>(search);
		}

		@Override
		public RealRandomAccess< RealPoint > create( 
				final KNearestNeighborSearch< V > search,
				final RealInterval interval )
		{
			return create( search );
		}
	}
}
