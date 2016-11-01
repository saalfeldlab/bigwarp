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
