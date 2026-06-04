/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2025 Howard Hughes Medical Institute.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
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
