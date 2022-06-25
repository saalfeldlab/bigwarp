package bigwarp.source;

import bdv.util.RealRandomAccessibleIntervalSource;
import net.imglib2.Interval;
import net.imglib2.RealPoint;
import net.imglib2.type.numeric.real.DoubleType;

public class PlateauSphericalMaskSource extends RealRandomAccessibleIntervalSource< DoubleType >
{
	private PlateauSphericalMaskRealRandomAccessible plateauMask;

	private PlateauSphericalMaskSource( int n, RealPoint pt, Interval interval )
	{
		super( new PlateauSphericalMaskRealRandomAccessible( n, pt ), interval, new DoubleType(), "plateau-mask" );
	}

	private PlateauSphericalMaskSource( PlateauSphericalMaskRealRandomAccessible mask, Interval interval )
	{
		super( mask, interval, new DoubleType(), "plateau-mask" );
		this.plateauMask = mask;
	}
	
	public 
	
	public static PlateauSphericalMaskSource build( int n, RealPoint pt, Interval interval )
	{
		PlateauSphericalMaskRealRandomAccessible mask = new PlateauSphericalMaskRealRandomAccessible( n, pt );
		return new PlateauSphericalMaskSource( mask, interval );
	}
}
