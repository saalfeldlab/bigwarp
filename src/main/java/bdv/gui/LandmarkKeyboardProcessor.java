package bdv.gui;

import java.awt.KeyEventPostProcessor;
import java.awt.event.KeyEvent;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.ui.TransformListener;
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
		
		if( ke.getKeyCode() == KeyEvent.VK_N && ke.getID() == KeyEvent.KEY_RELEASED  )
		{
			// pressing "N" while holding space toggles name visibility
			if(  bw.isInLandmarkMode() )
			{
				bw.getViewerFrameP().viewer.getSettings().toggleNamesVisible();
				bw.getViewerFrameP().viewer.requestRepaint();
				bw.getViewerFrameQ().viewer.getSettings().toggleNamesVisible();
				bw.getViewerFrameQ().viewer.requestRepaint();
			}
		}
		else if( ke.getKeyCode() == KeyEvent.VK_V && ke.getID() == KeyEvent.KEY_RELEASED  )
		{
			// pressing "V" while holding space toggles point visibility
			if(  bw.isInLandmarkMode() )
			{
				bw.getViewerFrameP().viewer.getSettings().togglePointsVisible();
				bw.getViewerFrameP().viewer.requestRepaint();
				bw.getViewerFrameQ().viewer.getSettings().togglePointsVisible();
				bw.getViewerFrameQ().viewer.requestRepaint();
			}
		}
		else if( ke.getKeyCode() == KeyEvent.VK_C && ke.getID() == KeyEvent.KEY_RELEASED  )
		{
			// pressing "C" while holding space computes a new transformation model
			if(  bw.isInLandmarkMode() )
			{
				bw.getViewerFrameP().getViewerPanel().showMessage("Estimating transformation...");
				bw.getViewerFrameQ().getViewerPanel().showMessage("Estimating transformation...");
				
				bw.restimateTransformation();
				bw.getViewerFrameP().getViewerPanel().showMessage("done.");
				bw.getViewerFrameQ().getViewerPanel().showMessage("done.");
			}
		}
		else if( ke.getKeyCode() == KeyEvent.VK_Q && ke.getID() == KeyEvent.KEY_RELEASED  )
		{
			// pressing "Q" makes the view transformation in the other window the same
			// as that for this active window
			
			if( ke.getComponent() == bw.getViewerFrameP().viewer.getDisplay() )
			{
				bw.getViewerFrameP().getViewerPanel().showMessage("Aligning.");
				bw.getViewerFrameQ().getViewerPanel().showMessage("Matching alignment.");
				
				AffineTransform3D viewXfm = new AffineTransform3D();
				bw.getViewerFrameP().viewer.getState().getViewerTransform( viewXfm );
				
				bw.getViewerFrameQ().viewer.animateTransformation( viewXfm );
			}
			
			if( ke.getComponent() == bw.getViewerFrameQ().viewer.getDisplay() )
			{
				bw.getViewerFrameP().getViewerPanel().showMessage("Matching alignment.");
				bw.getViewerFrameQ().getViewerPanel().showMessage("Aligning.");
				
				AffineTransform3D viewXfm = new AffineTransform3D();
				bw.getViewerFrameQ().viewer.getState().getViewerTransform( viewXfm );
				
				bw.getViewerFrameP().viewer.animateTransformation( viewXfm );
			}
		}
		else if( ke.getKeyCode() == KeyEvent.VK_W && ke.getID() == KeyEvent.KEY_RELEASED  )
		{
			// pressing "Q" makes the view transformation in the other window the same
			// as that for this active window
			
			if( ke.getComponent() == bw.getViewerFrameQ().viewer.getDisplay() )
			{
				bw.getViewerFrameP().getViewerPanel().showMessage("Aligning.");
				bw.getViewerFrameQ().getViewerPanel().showMessage("Matching alignment.");
				
				AffineTransform3D viewXfm = new AffineTransform3D();
				bw.getViewerFrameP().viewer.getState().getViewerTransform( viewXfm );
				
				bw.getViewerFrameQ().viewer.animateTransformation( viewXfm );
			}
			
			if( ke.getComponent() == bw.getViewerFrameP().viewer.getDisplay() )
			{
				bw.getViewerFrameP().getViewerPanel().showMessage("Matching alignment.");
				bw.getViewerFrameQ().getViewerPanel().showMessage("Aligning.");
				
				AffineTransform3D viewXfm = new AffineTransform3D();
				bw.getViewerFrameQ().viewer.getState().getViewerTransform( viewXfm );
				
				bw.getViewerFrameP().viewer.animateTransformation( viewXfm );
			}
		}
		else if( ke.getKeyCode() == KeyEvent.VK_ESCAPE && ke.getID() == KeyEvent.KEY_RELEASED  )
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
				bw.getViewerFrameP().getViewerPanel().transformChanged( viewXfm );
				bw.getViewerFrameP().getViewerPanel().getState().setViewerTransform( viewXfm );
			}
			else if ( ke.getComponent() == bw.getViewerFrameQ().getViewerPanel().getDisplay() )
			{
				bw.getViewerFrameQ().getViewerPanel().transformChanged( viewXfm );
				bw.getViewerFrameQ().getViewerPanel().getState().setViewerTransform( viewXfm );
			}
		}
		else if( ke.getKeyCode() == KeyEvent.VK_T && ke.getID() == KeyEvent.KEY_RELEASED  )
		{ 
			boolean newState =  !bw.getOverlayP().getIsTransformed();
			
			if( newState )
				bw.getViewerFrameP().getViewerPanel().showMessage("Displaying warped");
			else
				bw.getViewerFrameP().getViewerPanel().showMessage("Displaying raw");
			
			// Toggle whether moving image isdisplayed as transformed or not
			bw.setIsMovingDisplayTransformed( newState );
			bw.getViewerFrameP().getViewerPanel().requestRepaint();
			
		}
		
		return false;
	}

}
