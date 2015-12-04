package bdv.viewer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import bdv.img.WarpedSource;
import bdv.img.cache.Cache;
import bdv.util.Affine3DHelpers;
import bdv.util.Prefs;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.animate.OverlayAnimator;
import bdv.viewer.animate.RotationAnimator;
import bdv.viewer.animate.RotationAnimator2D;
import bdv.viewer.animate.SimilarityTransformAnimator3D;
import bdv.viewer.animate.SimilarityTransformAnimator2D;
import bdv.viewer.state.SourceState;

public class BigWarpViewerPanel extends ViewerPanel
{

	private static final long serialVersionUID = 7706602964307210070L;

	protected List< SourceAndConverter< ? > > sources;

	protected BigWarpViewerSettings viewerSettings;
	
	protected BigWarpOverlay overlay;
	
	protected BigWarpDragOverlay dragOverlay;
	
	protected boolean isMoving;
	
	protected boolean transformEnabled = true;
	
	protected AffineTransform3D destXfm;
	
	protected int ndims;

	protected int movingSourceIndex = 0; //TODO HAVE A SETTER FOR THIS

	// root two over two
	public static final double R2o2 = Math.sqrt( 2 ) / 2; 
	
	ViewerOptions options;
	
	public BigWarpViewerPanel( final List< SourceAndConverter< ? > > sources, final BigWarpViewerSettings viewerSettings, final Cache cache, boolean isMoving )
	{
		this( sources, viewerSettings, cache, ViewerOptions.options(), isMoving );
	}
	
	public BigWarpViewerPanel( final List< SourceAndConverter< ? > > sources, final BigWarpViewerSettings viewerSettings, final Cache cache, final ViewerOptions optional, boolean isMoving )
	{
		super( sources, 1, cache, optional );
		this.sources = sources;
		this.viewerSettings = viewerSettings;
		options = optional;
		this.isMoving = isMoving;
		destXfm = new AffineTransform3D();
	}

	public boolean isInFixedImageSpace()
	{
		return !isMoving || ((WarpedSource<?>)(sources.get( movingSourceIndex ).getSpimSource())).isTransformed();
	}
	
	public void addOverlay( BigWarpOverlay overlay ){
		this.overlay = overlay;
	}
	
	public BigWarpOverlay getOverlay( ){
		return overlay;
	}

	public void addDragOverlay( BigWarpDragOverlay dragOverlay ){
		this.dragOverlay = dragOverlay;
	}

	public BigWarpDragOverlay getDragOverlay(){
		return dragOverlay;
	}
	
	public boolean getIsMoving()
	{
		return isMoving;
	}
	
	public void setNumDim( int ndim )
	{
		this.ndims = ndim;
	}

	@Override
	public void paint()
	{
		if ( currentAnimator!=null && currentAnimator.isComplete() )
			transformChanged( destXfm );
		
		super.paint();
	}
	
	@Override
	public void drawOverlays( final Graphics g )
	{
		multiBoxOverlayRenderer.setViewerState( state );
		multiBoxOverlayRenderer.updateVirtualScreenSize( display.getWidth(), display.getHeight() );
		multiBoxOverlayRenderer.paint( ( Graphics2D ) g );

		sourceInfoOverlayRenderer.setViewerState( state );
		sourceInfoOverlayRenderer.paint( ( Graphics2D ) g );

		if ( Prefs.showScaleBar() )
		{
			scaleBarOverlayRenderer.setViewerState( state );
			scaleBarOverlayRenderer.paint( ( Graphics2D ) g );
		}

		final RealPoint gPos = new RealPoint( 3 );
		getGlobalMouseCoordinates( gPos );
		
		final String mousePosGlobalString;
		if( ndims == 2 )
			mousePosGlobalString = String.format( "(%6.1f,%6.1f)", gPos.getDoublePosition( 0 ), gPos.getDoublePosition( 1 ));
		else
			mousePosGlobalString = String.format( "(%6.1f,%6.1f,%6.1f)", gPos.getDoublePosition( 0 ), gPos.getDoublePosition( 1 ), gPos.getDoublePosition( 2 ) );
			
		g.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
		g.setColor( Color.white );
		g.drawString( mousePosGlobalString, ( int ) g.getClipBounds().getWidth() - 170, 12 );

		boolean requiresRepaint = multiBoxOverlayRenderer.isHighlightInProgress();

		final long currentTimeMillis = System.currentTimeMillis();
		final ArrayList< OverlayAnimator > overlayAnimatorsToRemove = new ArrayList< OverlayAnimator >();
		for ( final OverlayAnimator animator : overlayAnimators )
		{
			animator.paint( ( Graphics2D ) g, currentTimeMillis );
			requiresRepaint |= animator.requiresRepaint();
			if ( animator.isComplete() )
				overlayAnimatorsToRemove.add( animator );
		}
		overlayAnimators.removeAll( overlayAnimatorsToRemove );

		if ( requiresRepaint )
			display.repaint();
		
		if ( null != overlay ) {
			overlay.setViewerState( state );
			overlay.paint( ( Graphics2D ) g );
		}
		
		if ( dragOverlay != null ) {
			//dragOverlay.setViewerState( state );
			dragOverlay.paint( ( Graphics2D ) g );
		}
	}
	
	public BigWarpViewerSettings getSettings()
	{
		return viewerSettings;
	}
	
	public synchronized void rotateView2dOld( boolean isClockwise )
	{
		if( !transformEnabled )
			return;
		
		final SourceState< ? > source = state.getSources().get( state.getCurrentSource() );
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSpimSource().getSourceTransform( state.getCurrentTimepoint(), 0, sourceTransform );
		
		final AffineTransform3D transform = display.getTransformEventHandler().getTransform();

		double[] q = new double[ 4 ];
		double[][] R = new double[ 4 ][ 4 ];
		sourceTransform.toMatrix( R );
		LinAlgHelpers.quaternionFromR( R, q );

		double angle = LinAlgHelpers.angleFromR( R );
		double[] axis = new double[ 3 ];
		if( isClockwise )
			axis[ 2 ] = 1;
		else
			axis[ 2 ] = -1;
		
		double PI2 = Math.PI / 2;
		while( angle > PI2 )
			angle -= PI2;

		angle = PI2 - angle;
		
		if( angle < PI2 / 16 )
			angle = PI2;
		
		double[] rot = new double[ 4 ];
		LinAlgHelpers.quaternionFromAngleAxis( axis, angle, rot );
		
		double[] qTarget = new double[ 4 ];
		LinAlgHelpers.quaternionMultiply( rot, q, qTarget );

		double centerX;
		double centerY;
		if ( mouseCoordinates.isMouseInsidePanel() )
		{
			centerX = mouseCoordinates.getX();
			centerY = mouseCoordinates.getY();
		}
		else
		{
			centerY = getHeight() / 2.0;
			centerX = getWidth() / 2.0;
		}
	
		currentAnimator = new RotationAnimator( transform, centerX, centerY, qTarget, 300 );
		currentAnimator.setTime( System.currentTimeMillis() );
		transformChanged( transform );
		
	}

	public void displayViewerTransforms()
	{
//		final SourceState< ? > source = state.getSources().get( state.getCurrentSource() );
//		final AffineTransform3D sourceTransform = new AffineTransform3D();
//		source.getSpimSource().getSourceTransform( state.getCurrentTimepoint(), 0, sourceTransform );
		
		final AffineTransform3D transform = display.getTransformEventHandler().getTransform();
		
		final AffineTransform3D stateTransform = new AffineTransform3D();
		state.getViewerTransform( stateTransform );
		
		final double[] q = new double[ 4 ];
		Affine3DHelpers.extractRotationAnisotropic( transform, q );
	
		System.out.println( stateTransform );
	}
	
	public synchronized void rotateView2d( boolean isClockwise )
	{
		final SourceState< ? > source = state.getSources().get( state.getCurrentSource() );
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSpimSource().getSourceTransform( state.getCurrentTimepoint(), 0, sourceTransform );

		final AffineTransform3D transform = display.getTransformEventHandler().getTransform();

		// avoid computing angle explicitly ( and thus avoid expensive inverse tan op )
		// and instead do a verbose, but faster if statements
		double m00 = transform.get( 0, 0 );
		double m01 = transform.get( 0, 1 );
		double m10 = transform.get( 1, 0 );
		double m11 = transform.get( 1, 1 );

		boolean xpos = ( m00 + m10 > 0 );
		boolean ypos = ( m01 + m11 > 0 );
		
		RotatePlane2d qTarget;
		if( isClockwise )
		{
			if( xpos && ypos )
				qTarget = RotatePlane2d.qpY;
			else if( xpos && !ypos )
				qTarget = RotatePlane2d.qnX;
			else if( !xpos && ypos )
				qTarget = RotatePlane2d.qpX;
			else if( !xpos && !ypos )
				qTarget = RotatePlane2d.qnY;
			else
				qTarget = null; 
		}
		else
		{
			if( xpos && ypos )
				qTarget = RotatePlane2d.qnY;
			else if( xpos && !ypos )
				qTarget = RotatePlane2d.qpX;
			else if( !xpos && ypos )
				qTarget = RotatePlane2d.qnX;
			else if( !xpos && !ypos )
				qTarget = RotatePlane2d.qpY;
			else
				qTarget = null; 
		}
		
		if( qTarget == null ) return;
		
		double[][] R = new double[4][4];
		LinAlgHelpers.quaternionToR( qTarget.qAlign, R );
		R[3][3] = 1.0;
		destXfm.set( R );
		
		double centerX;
		double centerY;
		if ( mouseCoordinates.isMouseInsidePanel() )
		{
			centerX = mouseCoordinates.getX();
			centerY = mouseCoordinates.getY();
		}
		else
		{
			centerY = getHeight() / 2.0;
			centerX = getWidth() / 2.0;
		}
		
//		currentAnimator = new RotationAnimator( transform, centerX, centerY, q, 300 );
//		currentAnimator = new RotationAnimator( transform, centerX, centerY, qTarget.qAlign, 300 );
		currentAnimator = new RotationAnimator2D( transform, centerX, centerY, destXfm, 300 );

		currentAnimator.setTime( System.currentTimeMillis() );
		transformChanged( transform );
	}
	
    public synchronized void animateTransformation( AffineTransform3D destinationXfm, int millis )
    {
    	
    	AffineTransform3D startXfm = new AffineTransform3D();
    	getState().getViewerTransform( startXfm );
    	
		double centerX;
		double centerY;
		if ( mouseCoordinates.isMouseInsidePanel() )
		{
			centerX = mouseCoordinates.getX();
			centerY = mouseCoordinates.getY();
		}
		else
		{
			centerY = getHeight() / 2.0;
			centerX = getWidth() / 2.0;
		}
    	
		if( ndims == 2 ){
			currentAnimator = 
				new SimilarityTransformAnimator2D( startXfm, destinationXfm, centerX, centerY, millis );
		}else
		{
			currentAnimator = 
    			new SimilarityTransformAnimator3D( startXfm, destinationXfm, centerX, centerY, millis/2 );
		}
    	
    	currentAnimator.setTime( System.currentTimeMillis() );
		transformChanged( destinationXfm );
    }
    
    public void animateTransformation( AffineTransform3D destinationXfm )
    {
    	animateTransformation( destinationXfm, 300 );
    }
    
    public void setTransformEnabled( boolean enabled )
    {
    	transformEnabled = enabled;
    }
    
    public boolean getTransformEnabled()
    {
    	return transformEnabled;
    }
    
	@Override
	public synchronized void transformChanged( final AffineTransform3D transform )
	{
		if( transformEnabled )
			super.transformChanged( transform );
	}
	
	public static enum RotatePlane2d
	{
		
		qpX( "pX", new double[]{   1.0, 0.0, 0.0,   0.0 }),
		qpY( "pY", new double[]{  R2o2, 0.0, 0.0,  R2o2 }),
		qnX( "nX", new double[]{   0.0, 0.0, 0.0,   1.0 }),
		qnY( "nY", new double[]{  R2o2, 0.0, 0.0, -R2o2 });
		
		private final String name;

		public String getName()
		{
			return name;
		}

		private final double[] qAlign;

		private RotatePlane2d( final String name, final double[] q )
		{
			this.name = name;
			this.qAlign = q;
		}
	}
	
}
