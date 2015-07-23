package bdv.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import net.imglib2.realtransform.AffineTransform3D;
import bdv.img.cache.Cache;
import bdv.viewer.BigWarpOverlay;
import bdv.viewer.BigWarpViewerSettings;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.animate.SimilarityTransformAnimator;

public class BigWarpViewerPanel extends ViewerPanel{

	private static final long serialVersionUID = 7706602964307210070L;

	protected BigWarpViewerSettings viewerSettings;
	
	protected BigWarpOverlay overlay;
	
	protected boolean isMoving;
	
	public BigWarpViewerPanel( final List< SourceAndConverter< ? > > sources, final int numTimePoints, final Cache cache, boolean isMoving )
	{
		this( sources, numTimePoints, cache, options(), isMoving );
	}
	
	public BigWarpViewerPanel( final List< SourceAndConverter< ? > > sources, final int numTimePoints, final Cache cache, final Options optional, boolean isMoving )
	{
		super( sources, numTimePoints, cache, optional );
		viewerSettings = new BigWarpViewerSettings();
		this.isMoving = isMoving;
	}
	
	public void addOverlay( BigWarpOverlay overlay ){
		this.overlay = overlay;
	}
	
	public BigWarpOverlay getOverlay( ){
		return overlay;
	}
	
	public boolean getIsMoving()
	{
		return isMoving;
	}
	
	@Override
	public void paint()
	{
		super.paint();
	}
	
	@Override
	public void drawOverlays( final Graphics g2 )
	{
		super.drawOverlays( g2 );
		if ( null != overlay ) {
			overlay.setViewerState( state );
			overlay.paint( ( Graphics2D ) g2 );
		}
	}
	
	public BigWarpViewerSettings getSettings(){
		return viewerSettings;
	}
	
    public void animateTransformation( AffineTransform3D destinationXfm, int millis )
    {
    	AffineTransform3D startXfm = new AffineTransform3D();
    	getState().getViewerTransform( startXfm );
    	
    	currentAnimator = 
    			new SimilarityTransformAnimator( startXfm, destinationXfm, 0, 0, millis);
    	currentAnimator.setTime( System.currentTimeMillis() );
    	
		transformChanged( destinationXfm );
    }
    
    public void animateTransformation( AffineTransform3D destinationXfm )
    {
    	animateTransformation( destinationXfm, 300 );
    }
	
}
