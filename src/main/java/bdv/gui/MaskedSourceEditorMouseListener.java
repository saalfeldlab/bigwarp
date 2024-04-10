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
import bigwarp.transforms.AbstractTransformSolver;
import bigwarp.transforms.MaskedSimRotTransformSolver;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

public class MaskedSourceEditorMouseListener implements MouseListener, MouseMotionListener, MouseWheelListener
{
	protected PlateauSphericalMaskRealRandomAccessible mask;
	protected BigWarpViewerPanel viewer;
	protected List<BigWarpMaskSphereOverlay> overlays;

	private boolean active;
	private boolean dragged = false;

	private RealPoint p;
	private RealPoint c;
	private RealPoint pressPt;

	private BigWarp<?> bw;

	private static final double fastSpeed = 10.0;
	private static final double slowSpeed = 0.1;

	/**
	 * Updates {@link PlateauSphericalMaskRealRandomAccessible} parameters and {@link MaskedSimRotTransformSolver} center.
	 *
	 * @param nd number of dimensions
	 * @param bw BigWarp instance
	 * @param viewer the viewer panel
	 */
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

		p = new RealPoint( 3 );
		c = new RealPoint( 3 );
		pressPt = new RealPoint( 3 );
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

		dragged = false;
	}

	@Override
	public void mouseMoved( MouseEvent e ) { }

	@Override
	public void mouseDragged( MouseEvent e )
	{
		if( !active )
			return;

		// store starting center at start of drag
		if( !dragged )
		{
			if( mask != null )
				mask.getCenter().localize( c );

			viewer.getGlobalMouseCoordinates( pressPt );
			dragged = true;
		}

		viewer.getGlobalMouseCoordinates( p );
		bw.setAutoEstimateMask( false );
		
		if( e.isControlDown() && mask != null )
		{
			mask.getCenter().localize( c );
			final double d = PlateauSphericalMaskRealRandomAccessible.squaredDistance( p, c );
			synchronized ( mask )
			{
				mask.setSquaredRadius( d );
			}
		}
		else if( e.isShiftDown() && mask != null )
		{
			mask.getCenter().localize( c );
			final double d = Math.sqrt( PlateauSphericalMaskRealRandomAccessible.squaredDistance( p, c ));
			synchronized ( mask )
			{
				mask.setSigma( d - Math.sqrt( mask.getSquaredRadius()) );
			}
		}
		else
		{
			// p - pressPt inside the setPosition is the delta
			// c is the original center (before dragging started)
			// the code below sets the mask center to (c + delta)
			for( int i = 0; i < p.numDimensions(); i++ )
				p.setPosition( c.getDoublePosition(i) + p.getDoublePosition(i) - pressPt.getDoublePosition(i), i);

			if( mask != null )
			{
				synchronized ( mask )
				{
					mask.setCenter(p);
				}
			}

			AbstractTransformSolver< ? > solver = bw.getBwTransform().getSolver();
			if( solver instanceof MaskedSimRotTransformSolver )
			{
				((MaskedSimRotTransformSolver)solver).setCenter( p );
			}
		}

		bw.getViewerFrameP().getViewerPanel().requestRepaint();
		bw.getViewerFrameQ().getViewerPanel().requestRepaint();
	}

	@Override
	public void mouseReleased( MouseEvent e ) {

		if( !active || mask == null )
			return;

		if( e.isControlDown() || e.isShiftDown() )
			return;

		if( !dragged )
		{
			viewer.getGlobalMouseCoordinates( pressPt );
			if( mask != null )
			{
				synchronized ( mask )
				{
					mask.setCenter( pressPt );
				}
			}

			bw.setAutoEstimateMask( false );
			bw.getViewerFrameP().getViewerPanel().requestRepaint();
			bw.getViewerFrameQ().getViewerPanel().requestRepaint();
		}
		else
			dragged = false;
	}

	@Override
	public void mouseWheelMoved( MouseWheelEvent e )
	{
		if( !active || mask == null )
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
