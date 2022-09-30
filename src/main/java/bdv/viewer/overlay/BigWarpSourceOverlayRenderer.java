/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2021 Howard Hughes Medical Institute.
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
package bdv.viewer.overlay;

import java.awt.Font;
import java.awt.Graphics2D;

import bdv.img.WarpedSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerState;

public class BigWarpSourceOverlayRenderer extends SourceInfoOverlayRenderer
{
	private boolean indicateTransformed = true;

	// are any visible source in this viewer transformed
	private boolean anyTransformed = false;

	@Override
	public synchronized void paint( final Graphics2D g )
	{
		super.paint( g );
		if( indicateTransformed && anyTransformed )
		{
			g.setFont( new Font( "Monospaced", Font.BOLD, 16 ) );
			int tformedWidth = g.getFontMetrics().stringWidth( "TRANSFORMED" );
			g.drawString( "TRANSFORMED", 
					( int ) ( g.getClipBounds().getWidth()  - tformedWidth ) / 2,
					( int ) g.getClipBounds().getHeight() - 16 );
		}
	}

	/**
	 * Update data to show in the overlay.
	 *
	 * Checks whether any sources in this viewer are transformed in order to indicate that fact.
	 */
	@Override
	public synchronized void setViewerState( final ViewerState state )
	{
		super.setViewerState( state );

		anyTransformed = false;
		for( SourceAndConverter<?> vs : state.getVisibleSources())
		{
			Source< ? > src = vs.getSpimSource();
			if( src instanceof WarpedSource )
			{
				WarpedSource<?> ws = (WarpedSource<?>)src;
				if( ws.isTransformed() )
				{
					anyTransformed = true;
					break;
				}
			}
		}
	}

	public void setIndicateTransformed( final boolean indicateTransformed )
	{
		this.indicateTransformed = indicateTransformed;
	}
}
