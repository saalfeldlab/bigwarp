/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2025 Howard Hughes Medical Institute.
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
import bigwarp.transforms.BigWarpTransform;
import bigwarp.transforms.MaskedSimRotTransformSolver;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

public class MaskedSourceEditorMouseListener implements MouseListener, MouseMotionListener, MouseWheelListener
{
	private static final String MASK_NONE_MSG = "No mask to edit.";

	protected PlateauSphericalMaskRealRandomAccessible mask;
	protected BigWarpViewerPanel viewer;
	protected List<BigWarpMaskSphereOverlay> overlays;

	private boolean active;
	private boolean dragged = false;

	private RealPoint p;
	private RealPoint c;
	private RealPoint pressPt;

	private BigWarp<?> bw;

	private MaskedSimRotTransformSolver<?> solver;

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

		p = new RealPoint( 3 );
		c = new RealPoint( 3 );
		pressPt = new RealPoint( 3 );
		active = false;
	}

	public void setActive( boolean active )
	{

		final String maskInterpType = bw.getBwTransform().getMaskInterpolationType();
		if( active && maskInterpType.equals( BigWarpTransform.NO_MASK_INTERP))
		{
			bw.getViewerFrameP().getViewerPanel().showMessage( MASK_NONE_MSG );
			bw.getViewerFrameQ().getViewerPanel().showMessage( MASK_NONE_MSG );
			mask = null;
			return;
		}

		this.active = active;
		updateMask();
		bw.getViewerFrameP().setTransformEnabled( !active );
		bw.getViewerFrameQ().setTransformEnabled( !active );

		updateSolver();

		final String msg = active ? "Mask Edit On" : "Mask Edit Off";
		bw.getViewerFrameP().getViewerPanel().showMessage( msg );
		bw.getViewerFrameQ().getViewerPanel().showMessage( msg );
	}

	private void updateMask() {

		setMask(bw.getTransformPlateauMaskSource().getRandomAccessible());
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
			if( mask != null ) {
				storeMaskCenter();
			}

			viewer.getGlobalMouseCoordinates( pressPt );
			dragged = true;
		}

		viewer.getGlobalMouseCoordinates( p );
		bw.setAutoEstimateMask( false );
		
		if( e.isControlDown() )
		{
			if( mask != null ) {
				storeMaskCenter();
				final double r2 = PlateauSphericalMaskRealRandomAccessible.squaredDistance( p, c );
				updateMaskSquaredRadius(r2);
			}
		}
		else if( e.isShiftDown() )
		{
			if( mask != null ) {
				storeMaskCenter();
				final double d = Math.sqrt( PlateauSphericalMaskRealRandomAccessible.squaredDistance( p, c ));
				updateMaskSigma(d);
			}
		}
		else
		{
			// p - pressPt inside the setPosition is the delta
			// c is the original center (before dragging started)
			// the code below sets the mask center to (c + delta)
			for( int i = 0; i < p.numDimensions(); i++ )
				p.setPosition( c.getDoublePosition(i) + p.getDoublePosition(i) - pressPt.getDoublePosition(i), i);

			updateMaskCenter(p);
			updateSolverCenter(p);
		}

		bw.getViewerFrameP().getViewerPanel().requestRepaint();
		bw.getViewerFrameQ().getViewerPanel().requestRepaint();
	}

	@Override
	public void mouseReleased( MouseEvent e ) {

		if( !active )
			return;

		if( e.isControlDown() || e.isShiftDown() )
			return;

		if( !dragged )
		{
			viewer.getGlobalMouseCoordinates( pressPt );

			updateMaskCenter(pressPt);

			bw.setAutoEstimateMask( false );
			bw.getViewerFrameP().getViewerPanel().requestRepaint();
			bw.getViewerFrameQ().getViewerPanel().requestRepaint();

			updateSolverCenter(pressPt);
		}
		else
			dragged = false;
	}

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

	private void updateSolver() {

		final AbstractTransformSolver<?> solver = bw.getBwTransform().getSolver();
		if (solver instanceof MaskedSimRotTransformSolver)
			this.solver = (MaskedSimRotTransformSolver<?>)solver;
	}

	private void updateSolverCenter(final RealPoint p) {

		if (solver != null)
			solver.setCenter(p);
	}

	private void storeMaskCenter() {

		mask.getCenter().localize(c);
	}

	private void updateMaskCenter(final RealPoint p) {

		synchronized (mask) {
			mask.setCenter(p);
		}
	}

	private void updateMaskSigma(final double d) {

		synchronized (mask) {
			mask.setSigma(d - Math.sqrt(mask.getSquaredRadius()));
		}
	}

	private void updateMaskSquaredRadius(final double r2) {

		synchronized (mask) {
			mask.setSquaredRadius(r2);
		}
	}

}
