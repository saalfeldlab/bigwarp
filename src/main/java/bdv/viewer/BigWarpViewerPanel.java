package bdv.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;
import net.imglib2.util.LinAlgHelpers;
import bdv.img.cache.Cache;
import bdv.util.Affine3DHelpers;
import bdv.viewer.BigWarpOverlay;
import bdv.viewer.BigWarpViewerSettings;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.WarpNavigationActions.RotatePlaneAction;
import bdv.viewer.animate.RotationAnimator;
import bdv.viewer.animate.SimilarityTransformAnimator;

public class BigWarpViewerPanel extends ViewerPanel{

	private static final long serialVersionUID = 7706602964307210070L;

	protected BigWarpViewerSettings viewerSettings;
	
	protected BigWarpOverlay overlay;
	
	protected boolean isMoving;
	
	protected boolean transformEnabled = true;

	public static final double R2o2 = Math.sqrt( 2 ) / 2; 
	
//	public static final double[] qpX = new double[]{  1.0,  0.0,  0.0,  0.0 };
//	public static final double[] qpY = new double[]{  0.0,  0.0,  0.0,  1.0 };
//	public static final double[] qnX = new double[]{ -1.0,  0.0,  0.0,  0.0 };
//	public static final double[] qnY = new double[]{  0.0,  0.0,  0.0, -1.0 };
	
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
	
	public void rotateView2d( boolean isClockwise )
	{
		AffineTransform3D startXfm = new AffineTransform3D();
		getState().getViewerTransform( startXfm );

		double[] q = new double[ 4 ];
		double[][] R = new double[ 4 ][ 4 ];
		startXfm.toMatrix( R );
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
		
		System.out.println( "angle: " + angle );
		
		double[] rot = new double[ 4 ];
		LinAlgHelpers.quaternionFromAngleAxis( axis, angle, rot );
		
		double[] qTarget = new double[ 4 ];
		LinAlgHelpers.quaternionMultiply( rot, q, qTarget );

		final AffineTransform3D transform = display.getTransformEventHandler().getTransform();
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
		this.state.setViewerTransform( transform );
	}
	
	public void placeholder()
	{
		// avoid computing angle explicitly ( and thus avoid expensive inverse tan op )
		// and instead do a verbose, but faster if statements
//		System.out.println( startXfm );
//		
//		double m00 = startXfm.get( 0, 0 );
////		double m01 = startXfm.get( 0, 1 );
//		double m10 = startXfm.get( 1, 0 );
////		double m11 = startXfm.get( 1, 1 );
//
////		double xt = m00 + m01;
////		double yt = m10 + m11;
//
//		boolean xpos = ( m00 > 0 );
//		boolean ypos = ( m10 > 0 );

//		RotatePlane2d qTarget;
//		if( isClockwise )
//		{
//			if( xpos && ypos )
//				qTarget = RotatePlane2d.qpY;
//			else if( xpos && !ypos )
//				qTarget = RotatePlane2d.qpX;
//			else if( !xpos && ypos )
//				qTarget = RotatePlane2d.qnX;
//			else
//				qTarget = RotatePlane2d.qnY;
//		}
//		else
//		{
//			if( xpos && ypos )
//				qTarget = RotatePlane2d.qpX;
//			else if( xpos && !ypos )
//				qTarget = RotatePlane2d.qnY;
//			else if( !xpos && ypos )
//				qTarget = RotatePlane2d.qpY;
//			else
//				qTarget = RotatePlane2d.qnX;
//		}
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
    
    public void setTransformEnabled( boolean enabled )
    {
    	transformEnabled = enabled;
    }
    
	@Override
	public synchronized void transformChanged( final AffineTransform3D transform )
	{
		if( transformEnabled )
			super.transformChanged( transform );
	}
	
	public static enum RotatePlane2d
	{
		
		qpX( "pX", new double[]{   1.0, 0.0, 0.0,  0.0 }),
		qpY( "pY", new double[]{  R2o2, 0.0, 0.0, R2o2 }),
		qnX( "nX", new double[]{  -1.0, 0.0, 0.0,  0.0 }),
		qnY( "nY", new double[]{ -R2o2, 0.0, 0.0, R2o2 });

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
