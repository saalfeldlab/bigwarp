package bdv.viewer.overlay;

import java.awt.Font;
import java.awt.Graphics2D;

public class BigWarpSourceOverlayRenderer extends SourceInfoOverlayRenderer
{

	@Override
	public synchronized void paint( final Graphics2D g )
	{
		g.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
		g.drawString( sourceName, ( int ) g.getClipBounds().getWidth() / 2, 12 );
		g.drawString( groupName, ( int ) g.getClipBounds().getWidth() / 2, 25 );
	}
}
