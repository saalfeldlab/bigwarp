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
package bdv.gui;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

/**
 * Helpers for placing windows so that they are fully visible on a screen.
 */
class WindowPlacement
{
	private WindowPlacement()
	{}

	/**
	 * Returns a location for a window of the given size that lies fully within
	 * the usable area (excluding taskbars / docks) of one screen, as close as
	 * possible to the requested location. The screen used is the one that
	 * overlaps most with the requested window rectangle. If the window is
	 * larger than the screen, its top-left corner is kept visible.
	 */
	static Point clampToScreen( final Point requested, final Dimension size )
	{
		final Rectangle target = new Rectangle( requested, size );
		final Rectangle area = usableBounds( target );
		if ( area == null )
			return requested;

		final int x = Math.max( area.x, Math.min( requested.x, area.x + area.width - size.width ) );
		final int y = Math.max( area.y, Math.min( requested.y, area.y + area.height - size.height ) );
		return new Point( x, y );
	}

	/**
	 * Returns a location immediately to the right of the anchor, top-aligned
	 * with it. If there is no room on the right, the window is placed to the
	 * left of the anchor instead. The result is clamped to the screen.
	 */
	static Point rightOf( final Window anchor, final Dimension size )
	{
		final Rectangle a = anchor.getBounds();
		final Rectangle area = usableBounds( a );
		int x = a.x + a.width;
		if ( area != null && x + size.width > area.x + area.width )
			x = a.x - size.width;

		return clampToScreen( new Point( x, a.y ), size );
	}

	/**
	 * @return the usable bounds of the screen overlapping most with the
	 *         given rectangle, or null if there are no screens.
	 */
	private static Rectangle usableBounds( final Rectangle r )
	{
		if ( GraphicsEnvironment.isHeadless() )
			return null;

		GraphicsConfiguration best = null;
		long bestOverlap = -1;
		for ( final GraphicsDevice d : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices() )
		{
			final GraphicsConfiguration gc = d.getDefaultConfiguration();
			final Rectangle inter = gc.getBounds().intersection( r );
			final long overlap = inter.isEmpty() ? 0 : ( long ) inter.width * inter.height;
			if ( overlap > bestOverlap )
			{
				bestOverlap = overlap;
				best = gc;
			}
		}
		if ( best == null )
			return null;

		final Rectangle bounds = best.getBounds();
		final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets( best );
		return new Rectangle(
				bounds.x + insets.left,
				bounds.y + insets.top,
				bounds.width - insets.left - insets.right,
				bounds.height - insets.top - insets.bottom );
	}
}
