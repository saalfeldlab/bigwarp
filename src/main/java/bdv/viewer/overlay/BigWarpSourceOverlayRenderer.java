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
		g.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );

		int actualWidth = g.getFontMetrics().stringWidth( sourceName );
		g.drawString( sourceName, ( int ) g.getClipBounds().getWidth() - actualWidth - 10, 12 );

		if( !groupName.isEmpty() )
		{
			String groupStringBracket = "[ " + groupName + " ]";
			int actual_width_group = g.getFontMetrics().stringWidth( groupStringBracket );
			g.drawString( groupStringBracket,
					( int ) g.getClipBounds().getWidth() - actualWidth - actual_width_group - 20, 12 );
		}

		if( indicateTransformed )
		{
			g.setFont( new Font( "Monospaced", Font.PLAIN, 16 ) );
			int tformedWidth = g.getFontMetrics().stringWidth( "TRANSFORMED" );
			g.drawString( "TRANSFORMED", 
					( int ) ( g.getClipBounds().getWidth()  - tformedWidth ) / 2,
					( int ) g.getClipBounds().getHeight() - 24 );
		}
	}

//	/**
//	 * Update data to show in the overlay.
//	 */
//	@Override
//	public synchronized void setViewerState( final ViewerState state )
//	{
//		super.setViewerState( state );
//
//		anyTransformed = false;
//
//		for( SourceAndConverter<?> vs : state.getVisibleSources())
//		{
//			vs.getSpimSource();
//		}
//
//	}

	public void indicateTransformed( final boolean indicateTransformed )
	{
		this.indicateTransformed = indicateTransformed;
	}
}
