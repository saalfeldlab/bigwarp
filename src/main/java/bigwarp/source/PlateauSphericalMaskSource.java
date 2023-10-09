package bigwarp.source;

import bdv.util.RealRandomAccessibleIntervalSource;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RealPoint;
import net.imglib2.type.numeric.real.DoubleType;

public class PlateauSphericalMaskSource extends RealRandomAccessibleIntervalSource< DoubleType >
{
	private PlateauSphericalMaskRealRandomAccessible plateauMask;

	private PlateauSphericalMaskSource( RealPoint pt, Interval interval )
	{
		super( new PlateauSphericalMaskRealRandomAccessible( pt ), interval, new DoubleType(), "transform mask" );
	}

	private PlateauSphericalMaskSource( PlateauSphericalMaskRealRandomAccessible mask, Interval interval )
	{
		super( mask, interval, new DoubleType(), "transform mask" );
		this.plateauMask = mask;
	}

	public static PlateauSphericalMaskSource build( final PlateauSphericalMaskRealRandomAccessible mask, final FinalInterval interval )
	{
		return new PlateauSphericalMaskSource( mask, interval );
	}

	public PlateauSphericalMaskRealRandomAccessible getRandomAccessible()
	{
		return plateauMask;
	}

	public static PlateauSphericalMaskSource build( RealPoint pt, Interval interval )
	{
		PlateauSphericalMaskRealRandomAccessible mask = new PlateauSphericalMaskRealRandomAccessible( pt );
		return new PlateauSphericalMaskSource( mask, interval );
	}
}
