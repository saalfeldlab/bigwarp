package bigwarp.source;

import mpicbg.models.CoordinateTransform;
import net.imglib2.AbstractRealInterval;
import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessibleRealInterval;
import net.imglib2.type.numeric.RealType;

public class GridRealRandomAccessibleRealInterval<T extends RealType<T>> extends AbstractRealInterval implements RealRandomAccessibleRealInterval<T> 
{
	GridRealRandomAccess<T> ra;
	
	public GridRealRandomAccessibleRealInterval( Interval interval, T t, CoordinateTransform warp )
	{
		super( interval );
		ra = new GridRealRandomAccess< T >( new double[ interval.numDimensions() ], t, warp );
	}

	@Override
	public RealRandomAccess<T> realRandomAccess() {
		return ra.copy();
	}

	@Override
	public RealRandomAccess<T> realRandomAccess(RealInterval interval) {
		return realRandomAccess();
	}
	
}