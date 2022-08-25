/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2022 Howard Hughes Medical Institute.
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

public class BigWarpSourceOverlayRenderer extends SourceInfoOverlayRenderer
{

	@Override
	public synchronized void paint( final Graphics2D g )
	{
		g.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );

		int actual_width = g.getFontMetrics().stringWidth( sourceName );
		g.drawString( sourceName, ( int ) g.getClipBounds().getWidth() - actual_width - 10, 12 );

		if( !groupName.isEmpty() )
		{
			String groupStringBracket = "[ " + groupName + " ]";
			int actual_width_group = g.getFontMetrics().stringWidth( groupStringBracket );
			g.drawString( groupStringBracket,
					( int ) g.getClipBounds().getWidth() - actual_width - actual_width_group - 20, 12 );
		}
	}
}
