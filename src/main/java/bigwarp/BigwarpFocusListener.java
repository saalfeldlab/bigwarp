package bigwarp;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class BigwarpFocusListener extends WindowAdapter
{
	final private BigWarp bw;

	public BigwarpFocusListener( BigWarp bw )
	{
		this.bw = bw;
		bw.getLandmarkFrame().addWindowFocusListener( this );
		bw.getViewerFrameP().addWindowFocusListener( this );
		bw.getViewerFrameQ().addWindowFocusListener( this );
	}

	@Override
	public void windowGainedFocus( WindowEvent e )
	{
		// System.out.println( "Gained focus " + e.getWindow() );
	}

	@Override
	public void windowLostFocus( WindowEvent e )
	{
		bw.setInLandmarkMode( false );
	}
}
