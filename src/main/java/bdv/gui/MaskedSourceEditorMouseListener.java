package bdv.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;

import bdv.util.Affine3DHelpers;
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.overlay.BigWarpMaskSphereOverlay;
import bigwarp.BigWarp;
import bigwarp.source.PlateauSphericalMaskRealRandomAccessible;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

public class MaskedSourceEditorMouseListener implements MouseListener, MouseMotionListener, MouseWheelListener
{
	protected PlateauSphericalMaskRealRandomAccessible mask;
	protected BigWarpViewerPanel viewer;
	protected List<BigWarpMaskSphereOverlay> overlays;

	private boolean active;
	private RealPoint p;
//	private RealPoint pressPt;

	private BigWarp<?> bw;

	private static final double fastSpeed = 10.0;
	private static final double slowSpeed = 0.1;

	public MaskedSourceEditorMouseListener( int nd, BigWarp<?> bw, BigWarpViewerPanel viewer )
	{
		this.bw = bw;
		this.viewer = viewer;

		this.viewer.getDisplay().addMouseListener( this );
		this.viewer.getDisplay().addMouseWheelListener( this );
		this.viewer.getDisplay().addMouseMotionListener( this );

		overlays = new ArrayList<BigWarpMaskSphereOverlay>();
		overlays.add( bw.getViewerFrameP().getViewerPanel().getMaskOverlay() );
		overlays.add( bw.getViewerFrameQ().getViewerPanel().getMaskOverlay() );

		p = new RealPoint( nd );
//		pressPt = new RealPoint( nd );
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

//		viewer.getGlobalMouseCoordinates( pressPt );
		viewer.getGlobalMouseCoordinates( p );
		if( e.isControlDown() || e.isShiftDown() )
			return;

		synchronized ( mask )
		{
//			mask.setCenter( pressPt );
			mask.setCenter( p );
		}
		bw.setAutoEstimateMask( false );
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
		bw.setAutoEstimateMask( false );
		
		if( e.isControlDown() )
		{
			synchronized ( mask )
			{
//				mask.setSquaredRadius( PlateauSphericalMaskRealRandomAccessible.squaredDistance( p, pressPt ) );
				mask.setSquaredRadius( PlateauSphericalMaskRealRandomAccessible.squaredDistance( p, mask.getCenter() ) );
			}
		}
		else if( e.isShiftDown() )
		{
			synchronized ( mask )
			{
				double d = Math.sqrt( PlateauSphericalMaskRealRandomAccessible.squaredDistance( p, mask.getCenter() ));
				mask.setSigma( d - Math.sqrt( mask.getSquaredRadius()) );
			}
		}
		else
		{
			synchronized ( mask )
			{
				mask.setSquaredRadius( PlateauSphericalMaskRealRandomAccessible.squaredDistance( p, mask.getCenter() ) );
			}
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

		bw.setAutoEstimateMask( false );
		final AffineTransform3D transform = viewer.state().getViewerTransform();
		final double scale = (1.0 / (Affine3DHelpers.extractScale(transform, 0) + 1e-9 ) + 1e-6 );
		final int sign = e.getWheelRotation();

		if( e.isShiftDown() )
			mask.incSquaredSigma( sign * scale * scale * fastSpeed * fastSpeed );
		else if ( e.isControlDown() )
			mask.incSquaredSigma( sign * scale * scale * slowSpeed * slowSpeed );
		else
			mask.incSquaredSigma( sign * scale * scale );

		bw.getViewerFrameP().getViewerPanel().requestRepaint();
		bw.getViewerFrameQ().getViewerPanel().requestRepaint();
	}


}
