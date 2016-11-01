package bdv.viewer.overlay;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Stroke;

import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.state.ViewerState;
import bigwarp.BigWarp;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

public class CrosshairOverlay
{

	/** The viewer state. */
	private ViewerState state;

	private final BigWarp bw;

	private final BigWarpViewerPanel viewer;

	private RealPoint location;

	private double width;

	/** The transform for the viewer current viewpoint. */
	private final AffineTransform3D transform;

	public CrosshairOverlay( final BigWarp bw, final BigWarpViewerPanel viewer )
	{
		this.bw = bw;
		this.viewer = viewer;
		transform = new AffineTransform3D();
		location = new RealPoint( 3 );
		width = 25;
	}

	public void paint( final Graphics2D g ) 
	{
		if( !bw.isInLandmarkMode() || width <= 0 )
			return;
		
		/*
		 * Collect current view.
		 */
		state.getViewerTransform( transform );
		
		// Save graphic device original settings
		final Composite originalComposite = g.getComposite();
		final Stroke originalStroke = g.getStroke();
		final Color originalColor = g.getColor();

		final RealPoint viewerCoords = new RealPoint( 3 );
		transform.apply( location, viewerCoords );

		g.drawLine( 
				(int)(viewerCoords.getDoublePosition( 0 )), 
				(int)(viewerCoords.getDoublePosition( 1 ) - width/2),
				(int)(viewerCoords.getDoublePosition( 0 )), 
				(int)(viewerCoords.getDoublePosition( 1 ) + width/2));

		g.drawLine( 
				(int)(viewerCoords.getDoublePosition( 0 ) - width/2), 
				(int)(viewerCoords.getDoublePosition( 1 )),
				(int)(viewerCoords.getDoublePosition( 0 ) + width/2), 
				(int)(viewerCoords.getDoublePosition( 1 )));

		// Restore graphic device original settings
		g.setComposite( originalComposite );
		g.setStroke( originalStroke );
		g.setColor( originalColor );
	}

	/**
	 * Update data to show in the overlay.
	 */
	public void setViewerState( final ViewerState state )
	{
		this.state = state;
	}

	public void setWidth( final double width )
	{
		this.width = width;
	}

	public double getWidth()
	{
		return width;
	}

	public void setLocation( RealPoint newPoint )
	{
		location.setPosition( newPoint );
	}
}