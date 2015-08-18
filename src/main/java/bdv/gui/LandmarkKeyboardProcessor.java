package bdv.gui;

import java.awt.KeyEventPostProcessor;
import java.awt.event.KeyEvent;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import bigwarp.BigWarp;

public class LandmarkKeyboardProcessor implements KeyEventPostProcessor 
{
	BigWarp bw;
	
	public LandmarkKeyboardProcessor( BigWarp bw )
	{
		this.bw = bw;
	}
	
	@Override
	public boolean postProcessKeyEvent(KeyEvent ke ) {
		// if the event is consumed, ignore it
		if( ke.isConsumed())
			return false;
		
		if( ke.getKeyCode() == KeyEvent.VK_ESCAPE && ke.getID() == KeyEvent.KEY_RELEASED  )
		{
			if( bw.getLandmarkPanel().getTableModel().isPointUpdatePending() )
			{
				bw.getLandmarkPanel().getTableModel().restorePendingUpdate();
			}
		}
		else if( ke.getKeyCode() == KeyEvent.VK_R && ke.getID() == KeyEvent.KEY_RELEASED  )
		{ 
			final RandomAccessibleInterval<?> interval = bw.getSources().get( 1 ).getSpimSource().getSource( 0, 0 );
			
			AffineTransform3D viewXfm = new AffineTransform3D();
			viewXfm.identity();
			viewXfm.set( -interval.min( 2 ), 2, 3 );
			
			if( ke.getComponent() == bw.getViewerFrameP().getViewerPanel().getDisplay() )
			{
				bw.getViewerFrameP().getViewerPanel().animateTransformation( viewXfm );
			}
			else if ( ke.getComponent() == bw.getViewerFrameQ().getViewerPanel().getDisplay() )
			{
				bw.getViewerFrameQ().getViewerPanel().animateTransformation( viewXfm );
			}
		}
		return false;
	}

}
