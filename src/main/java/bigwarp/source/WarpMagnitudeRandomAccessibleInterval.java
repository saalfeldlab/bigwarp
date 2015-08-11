package bigwarp.source;

import mpicbg.models.CoordinateTransform;
import net.imglib2.AbstractRealInterval;
import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessibleRealInterval;
import net.imglib2.type.numeric.RealType;

public class WarpMagnitudeRandomAccessibleInterval<T extends RealType<T>> extends AbstractRealInterval implements RealRandomAccessibleRealInterval<T> 
{
	CoordinateTransform warp;
	CoordinateTransform base;
	
	WarpMagnitudeRandomAccess< T > ra;
	
	public WarpMagnitudeRandomAccessibleInterval( Interval interval, T t, CoordinateTransform warp, CoordinateTransform base )
	{
		super( interval );
		this.warp = warp;
		this.base = base;
		ra = new WarpMagnitudeRandomAccess< T >( new double[ interval.numDimensions() ], t, warp, base );
	}

	@Override
	public RealRandomAccess<T> realRandomAccess() {
		return ra;
	}

	@Override
	public RealRandomAccess<T> realRandomAccess(RealInterval interval) {
		return realRandomAccess();
	}

}