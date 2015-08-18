package bigwarp.source;

import mpicbg.models.CoordinateTransform;
import net.imglib2.AbstractInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public class GridRandomAccessibleInterval<T extends RealType<T>> extends AbstractInterval implements RandomAccessibleInterval<T> 
{
	CoordinateTransform warp;
	GridRandomAccess< T > ra;
	
	public GridRandomAccessibleInterval( Interval interval, T t, CoordinateTransform warp )
	{
		super( interval );
		this.warp = warp;
		ra = new GridRandomAccess< T >( new long[ interval.numDimensions() ], t, warp );
	}

	@Override
	public RandomAccess<T> randomAccess() {
		return ra.copy();
	}

	@Override
	public RandomAccess<T> randomAccess( Interval interval) {
		return randomAccess();
	}

//	public RandomAccessibleInterval<T> copy()
//	{
//		return new GridRandomAccessibleInterval<T>(
//					this, this.ra.value.copy(), 
//					warp );
//		
//
//	}
	
}