package bigwarp.source;

import java.util.function.BiConsumer;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.RealRandomAccessibleIntervalSource;
import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;

public class PlateauSphericalMaskRealRandomAccessible implements RealRandomAccessible< DoubleType >
{
	private PlateauFunction pfun;
	private FunctionRealRandomAccessible< DoubleType > rra;
	
	private double plateauR = 8;

	private double outerSigma = 1;

	public PlateauSphericalMaskRealRandomAccessible( int n, RealPoint pt )
	{
		pfun = new PlateauFunction( pt );
		rra = new FunctionRealRandomAccessible<>( 3, pfun, DoubleType::new );
	}
	
	public static void main( String[] args )
	{
		long S = 50;
		RealPoint pt = new RealPoint( new double[]{ S, S, S } );

		PlateauSphericalMaskRealRandomAccessible img = new PlateauSphericalMaskRealRandomAccessible( 3, pt );
		Interval interval = Intervals.createMinSize( 0, 0, 0, 2*S, 2*S, 2*S );

		BdvOptions options = BdvOptions.options().screenScales( new double[] { 1 } );
		BdvStackSource< DoubleType > bdv = BdvFunctions.show( img.rra, interval, "img", options );
		bdv.getBdvHandle().getSetupAssignments().getMinMaxGroups().get( 0 ).setRange( 0, 1 );
		
	}

	public class PlateauFunction implements BiConsumer< RealLocalizable, DoubleType > {

		public RealPoint point;

		public double plateauR = 8;
		public double plateauR2 = plateauR*plateauR;

		public double outerSigma = 1;
		
		public double invSqrSigma = 1 / outerSigma / outerSigma;
	
		public PlateauFunction( RealPoint point ) {
			this.point = point;
		}

		@Override
		public void accept( RealLocalizable x, DoubleType v )
		{
			v.setZero();
			double r2 = squaredDistance( x, point );
			if( r2 <= plateauR )
				v.setOne();
			else
			{
				double t = (r2 - plateauR);
				// TODO sample exp function and interpolate to speed up
				v.set( Math.exp( -0.5 * t * invSqrSigma ) );
//				v.set( Math.cos( t * 0.5  + 0.5 ));
//				v.set( 1 / t );
			}
		}
		
	}
	
	final public static double squaredDistance( final RealLocalizable position1, final RealLocalizable position2 )
	{
		double dist = 0;

		final int n = position1.numDimensions();
		for ( int d = 0; d < n; ++d )
		{
			final double pos = position2.getDoublePosition( d ) - position1.getDoublePosition( d );

			dist += pos * pos;
		}

		return dist;
	}

	@Override
	public int numDimensions()
	{
		return rra.numDimensions();
	}

	@Override
	public RealRandomAccess< DoubleType > realRandomAccess()
	{
		return rra.realRandomAccess();
	}

	@Override
	public RealRandomAccess< DoubleType > realRandomAccess( RealInterval interval )
	{
		return rra.realRandomAccess( interval );
	}

}
