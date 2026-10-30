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
package bigwarp.scripts;

import java.util.Arrays;

import ij.ImagePlus;
import ij.measure.Calibration;
import net.imglib2.realtransform.ScaleAndTranslation;

/**
 * An output pixel grid: size, spacing and origin (the physical position
 * of pixel zero), in nd dimensions. Physical coordinates are
 * <code>pixel * spacing + origin</code>, with the origin in physical units, as
 * BigWarp loads images.
 */
class OutputGrid
{
	final long[] size;

	final double[] spacing;

	final double[] origin;

	OutputGrid( final long[] size, final double[] spacing, final double[] origin )
	{
		this.size = size;
		this.spacing = spacing;
		this.origin = origin;
	}

	/**
	 * The grid of the image, unless overridden. A physical interval from
	 * min and max replaces the image's field of view, and spacing replaces
	 * its spacing; the field of view is sampled from min at the spacing,
	 * up to and including max.
	 *
	 * @param imp the image whose grid is the default
	 * @param nd the number of dimensions
	 * @param min the field of view min, or null for the image's
	 * @param max the field of view max, or null for the image's
	 * @param spacingOverride the spacing (1 or nd values), or null for the image's
	 * @return the grid
	 */
	static OutputGrid create( final ImagePlus imp, final int nd,
			final double[] min, final double[] max, final double[] spacingOverride )
	{
		final ScaleAndTranslation toPhysical = pixelToPhysical( imp, nd );
		final long[] imgSize = nd == 2
				? new long[] { imp.getWidth(), imp.getHeight() }
				: new long[] { imp.getWidth(), imp.getHeight(), imp.getNSlices() };

		final double[] spacing = new double[ nd ];
		final double[] origin = new double[ nd ];
		for ( int d = 0; d < nd; d++ )
		{
			spacing[ d ] = spacingOverride == null ? toPhysical.getScale( d )
					: spacingOverride[ spacingOverride.length == 1 ? 0 : d ];
			origin[ d ] = toPhysical.getTranslation( d );
		}

		// the image's own grid, exactly
		if ( min == null && spacingOverride == null )
			return new OutputGrid( imgSize, spacing, origin );

		final double[] lo = new double[ nd ];
		final double[] hi = new double[ nd ];
		for ( int d = 0; d < nd; d++ )
		{
			lo[ d ] = min != null ? min[ d ] : origin[ d ];
			hi[ d ] = max != null ? max[ d ] : origin[ d ] + ( imgSize[ d ] - 1 ) * toPhysical.getScale( d );
		}
		return sample( lo, hi, spacing );
	}

	/**
	 * The grid sampling a physical interval from min at the spacing, up to and
	 * including max, with no image to take defaults from.
	 *
	 * @param nd the number of dimensions
	 * @param min the field of view min
	 * @param max the field of view max
	 * @param spacingOverride the spacing (1 or nd values), or null for 1 in every dimension
	 * @return the grid
	 */
	static OutputGrid create( final int nd, final double[] min, final double[] max, final double[] spacingOverride )
	{
		final double[] spacing = new double[ nd ];
		for ( int d = 0; d < nd; d++ )
			spacing[ d ] = spacingOverride == null ? 1.0 : spacingOverride[ spacingOverride.length == 1 ? 0 : d ];

		return sample( min.clone(), max.clone(), spacing );
	}

	private static OutputGrid sample( final double[] lo, final double[] hi, final double[] spacing )
	{
		final int nd = spacing.length;
		final long[] size = new long[ nd ];
		for ( int d = 0; d < nd; d++ )
		{
			if ( !( spacing[ d ] > 0 ) || Double.isInfinite( spacing[ d ] ) )
				throw new IllegalArgumentException( "spacing must be positive, got " + Arrays.toString( spacing ) );

			if ( !( hi[ d ] >= lo[ d ] ) )
				throw new IllegalArgumentException( String.format( "field of view max must be at least min in dimension %d, got [%f, %f]", d, lo[ d ], hi[ d ] ) );

			// a small tolerance, so that a max on the grid is included despite rounding
			size[ d ] = (long) Math.floor( ( hi[ d ] - lo[ d ] ) / spacing[ d ] + 1e-6 ) + 1;
			if ( size[ d ] > Integer.MAX_VALUE )
				throw new IllegalArgumentException( String.format( "the output is too large in dimension %d: %d pixels", d, size[ d ] ) );
		}
		return new OutputGrid( size, spacing, lo );
	}

	/**
	 * Pixel to physical coordinates of this grid.
	 */
	ScaleAndTranslation toPhysical()
	{
		return new ScaleAndTranslation( spacing, origin );
	}

	/**
	 * Sets the calibration of an image sampled on this grid, origin in physical
	 * units, as the images are read. For a 2d grid, the z spacing and origin,
	 * and for any grid the unit, are taken from the reference image if given.
	 *
	 * @param out the image on this grid
	 * @param reference the image the grid came from, or null
	 */
	void calibrate( final ImagePlus out, final ImagePlus reference )
	{
		final int nd = size.length;
		final Calibration cal = out.getCalibration();
		final Calibration rcal = reference != null ? reference.getCalibration() : null;
		cal.pixelWidth = spacing[ 0 ];
		cal.pixelHeight = spacing[ 1 ];
		cal.pixelDepth = nd == 2 ? ( rcal != null ? spacing( rcal.pixelDepth ) : 1.0 ) : spacing[ 2 ];
		cal.xOrigin = origin[ 0 ];
		cal.yOrigin = origin[ 1 ];
		cal.zOrigin = nd == 2 ? ( rcal != null ? rcal.zOrigin : 0.0 ) : origin[ 2 ];
		if ( rcal != null )
			cal.setUnit( rcal.getUnit() );
	}

	static double spacing( final double in )
	{
		return ( Double.isNaN( in ) || Double.isInfinite( in ) || in == 0 ) ? 1.0 : in;
	}

	/**
	 * Pixel to physical coordinates, pixel * spacing + origin, in nd dimensions.
	 */
	static ScaleAndTranslation pixelToPhysical( final ImagePlus imp, final int nd )
	{
		final Calibration cal = imp.getCalibration();
		final double[] scale = { spacing( cal.pixelWidth ), spacing( cal.pixelHeight ), spacing( cal.pixelDepth ) };
		final double[] origin = { cal.xOrigin, cal.yOrigin, cal.zOrigin };
		return new ScaleAndTranslation( Arrays.copyOf( scale, nd ), Arrays.copyOf( origin, nd ) );
	}
}
