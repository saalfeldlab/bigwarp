package bdv.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import bdv.viewer.ViewerPanel;
import bigwarp.BigWarp;
import bigwarp.source.PlateauSphericalMaskRealRandomAccessible;
import net.imglib2.RealPoint;

public class MaskedSourceEditorMouseListener implements MouseListener, MouseMotionListener, MouseWheelListener
{
	private PlateauSphericalMaskRealRandomAccessible mask;

	private boolean active;
	private RealPoint p;

	private BigWarp<?> bw;
	private ViewerPanel viewer;

	public MaskedSourceEditorMouseListener( int nd, BigWarp<?> bw, ViewerPanel viewer )
	{
		this.bw = bw;
		this.viewer = viewer;
		viewer.getDisplay().addMouseListener( this );
		viewer.getDisplay().addMouseWheelListener( this );
		viewer.getDisplay().addMouseMotionListener( this );

		p = new RealPoint( nd );
		active = false;
	}

	public void setActive( boolean active )
	{
		this.active = active;
		bw.getViewerFrameP().setTransformEnabled( !active );
		bw.getViewerFrameQ().setTransformEnabled( !active );

		final String msg = active ? "Mask Edit On" : "Mask Edit Off";
		bw.getViewerFrameP().getViewerPanel().showMessage( msg );
		bw.getViewerFrameQ().getViewerPanel().showMessage( msg );
	}

	public void toggleActive( )
	{
		setActive( !active );
	}

	public void setMask( PlateauSphericalMaskRealRandomAccessible mask )
	{
		this.mask = mask;
	}

	public PlateauSphericalMaskRealRandomAccessible getMask()
	{
		return mask;
	}

	@Override
	public void mouseClicked( MouseEvent e ) { }

	@Override
	public void mouseEntered( MouseEvent e ) { }

	@Override
	public void mouseExited( MouseEvent e ) { }

	@Override
	public void mousePressed( MouseEvent e )
	{
		if( !active )
			return;

		viewer.getGlobalMouseCoordinates( p );
		synchronized ( mask )
		{
			mask.setCenter( p );
		}
//		viewer.requestRepaint();
		bw.getViewerFrameP().getViewerPanel().requestRepaint();
		bw.getViewerFrameQ().getViewerPanel().requestRepaint();
	}

	@Override
	public void mouseMoved( MouseEvent e ) { }

	@Override
	public void mouseDragged( MouseEvent e )
	{
		if( !active )
			return;

		viewer.getGlobalMouseCoordinates( p );
		synchronized ( mask )
		{
			mask.setSquaredRadius( PlateauSphericalMaskRealRandomAccessible
					.squaredDistance( p, mask.getCenter() ) );
		}
		bw.getViewerFrameP().getViewerPanel().requestRepaint();
		bw.getViewerFrameQ().getViewerPanel().requestRepaint();

	}

	@Override
	public void mouseReleased( MouseEvent e ) { }

	@Override
	public void mouseWheelMoved( MouseWheelEvent e )
	{
		if( !active )
			return;

		// TODO vary based on screen scale
		final int sign = e.getWheelRotation();
		if( e.isShiftDown() )
			mask.incSquaredSigma( sign * 10 );
		else if ( e.isControlDown() )
			mask.incSquaredSigma( sign * 0.1 );
		else
			mask.incSquaredSigma( sign * 1.0 );

		bw.getViewerFrameP().getViewerPanel().requestRepaint();
		bw.getViewerFrameQ().getViewerPanel().requestRepaint();
	}


}
