package bdv.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import bdv.util.Affine3DHelpers;
import bdv.viewer.ViewerPanel;
import bigwarp.BigWarp;
import bigwarp.source.PlateauSphericalMaskRealRandomAccessible;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

public class MaskedSourceEditorMouseListener implements MouseListener, MouseMotionListener, MouseWheelListener
{
	private PlateauSphericalMaskRealRandomAccessible mask;

	private boolean active;
	private RealPoint p;

	private BigWarp<?> bw;
	private ViewerPanel viewer;

	private static final double fastSpeed = 10.0;
	private static final double slowSpeed = 0.1;

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

		final AffineTransform3D transform = viewer.state().getViewerTransform();
		final double scale = (1 / Affine3DHelpers.extractScale(transform, 0)) + 0.05;
		final int sign = e.getWheelRotation();
		if( e.isShiftDown() )
			mask.incSquaredSigma( sign * scale * fastSpeed );
		else if ( e.isControlDown() )
			mask.incSquaredSigma( sign * scale * slowSpeed);
		else
			mask.incSquaredSigma( sign * scale );

		bw.getViewerFrameP().getViewerPanel().requestRepaint();
		bw.getViewerFrameQ().getViewerPanel().requestRepaint();
	}


}
