package bdv.viewer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import bigwarp.BigWarp;

public class BigWarpDragOverlay
{
	private BigWarp bw;
	private BigWarpViewerPanel viewer;
	
	private boolean  inProgress  = false;
	private boolean  completedOK = false;
	
	private final RealPoint movingPoint;
	private final RealPoint targetPoint;
	
	private final RealPoint movingPointScreen;
	private final RealPoint targetPointScreen;
	
	WarpDragMouseListener mouseListener;
	private final AffineTransform3D transform = new AffineTransform3D();
	
	private double arad = 4.0;
	
//	private Color startColor = new Color( Integer.parseInt( "EDA50D", 16 ) ); // an orange
//	private Color endColor   = new Color( Integer.parseInt( "09A067", 16 ) ); // a green 
//	private Color lineColor  = new Color( Integer.parseInt( "8447CE", 16 ) ); // a purple 
	
	private Color baseColor;
	
	public BigWarpDragOverlay( final BigWarp bw, final BigWarpViewerPanel viewer )
	{
		this.bw = bw;
		this.viewer = viewer;
		movingPoint = new RealPoint( 3 );
		targetPoint = new RealPoint( 3 );
		movingPointScreen = new RealPoint( 3 );
		targetPointScreen = new RealPoint( 3 );
		

		arad = ( Double ) viewer.getSettings().get( 
				 BigWarpViewerSettings.KEY_SPOT_SIZE);
		
		baseColor = ( Color ) viewer.getSettings().get( 
				 BigWarpViewerSettings.KEY_COLOR );
		
		mouseListener = new WarpDragMouseListener( bw, viewer );
	}
	
	public void reset()
	{
		completedOK = false;
	}
	
	public void paint( final Graphics2D g ) 
	{
		// System.out.println("BigWarpDragOverlay - PAINT" );
		if( inProgress )
		{
			viewer.getState().getViewerTransform( transform );
			
			//System.out.println("BigWarpDragOverlay - PAINT IN PROGRESS" );
			transform.apply( movingPoint, movingPointScreen );
			transform.apply( targetPoint, targetPointScreen );
			
			g.setColor( baseColor.brighter() );
			g.fillRect( ( int ) ( movingPointScreen.getDoublePosition( 0 ) - arad ), 
						( int ) ( movingPointScreen.getDoublePosition( 1 ) - arad ), 
						( int ) ( 2 * arad ), ( int ) ( 2 * arad ) );
			
			g.setColor( baseColor.darker() );
			g.fillRect( ( int ) ( targetPointScreen.getDoublePosition( 0 ) - arad ), 
					( int ) ( targetPointScreen.getDoublePosition( 1 ) - arad ), 
					( int ) ( 2 * arad ), ( int ) ( 2 * arad ) );
			
			g.setColor( baseColor );
			g.drawLine(
					( int ) ( movingPointScreen.getDoublePosition( 0 )), 
					( int ) ( movingPointScreen.getDoublePosition( 1 )), 
					( int ) ( targetPointScreen.getDoublePosition( 0 )), 
					( int ) ( targetPointScreen.getDoublePosition( 1 )));
			
		}
	}
	
	public void updateLandmarkModel( )
	{
		System.out.println( "BigWarpDragOverlay: updateLandmarkModel" );
		
		if( !completedOK )
			return;
		
		double[] movingPtArray = new double[ 3 ];
		double[] targetPtArray = new double[ 3 ];
		
		movingPoint.localize(  movingPtArray );
		targetPoint.localize(  targetPtArray );
		
		bw.addPoint( movingPtArray, true,  viewer );
		bw.addPoint( targetPtArray, false, viewer );
		
//		// if points were placed in a transformed viewer, the clicked
//		// points need to be transformed before being added
//		if( viewer.getOverlay().isTransformed )
//		{
//			System.out.println( "Adding TRANSFORMED points");
//			LandmarkTableModel lm = bw.getLandmarkPanel().getTableModel();
//			
//			double[] movingPtXfm = new double[ 3 ];
//			double[] targetPtXfm = new double[ 3 ];
//			
//			lm.getTransform().apply( movingPtArray, movingPtXfm );
//			lm.getTransform().apply( targetPtArray, targetPtXfm );
//			
//			bw.addPoint( movingPtXfm, true,  viewer );
//			bw.addPoint( targetPtXfm, false, viewer );
//			
////			lm.add( movingPtXfm, true );
////			lm.add( targetPtXfm, false );
//		}
//		else
//		{
//			System.out.println( "Adding points");
////			lm.add( movingPtArray, true );
////			lm.add( targetPtArray, false );
//			bw.addPoint( movingPtArray, true,  viewer );
//			bw.addPoint( targetPtArray, false, viewer );
//		}
		
		reset();
	}
	
	protected class WarpDragMouseListener implements MouseListener, MouseMotionListener
	{
		private BigWarp bw;
		private BigWarpViewerPanel thisViewer;
		
		public WarpDragMouseListener( final BigWarp bw, final BigWarpViewerPanel thisViewer )
		{
			this.bw = bw;
			setViewer( thisViewer );
			thisViewer.getDisplay().addHandler( this );
		}
		
		protected void setViewer( BigWarpViewerPanel thisViewer )
		{
			this.thisViewer = thisViewer;
		}
		
		@Override
		public void mousePressed( MouseEvent e )
		{
			if( bw.isInLandmarkMode() && e.isShiftDown() )
			{
				thisViewer.getGlobalMouseCoordinates( BigWarpDragOverlay.this.movingPoint );
				thisViewer.getGlobalMouseCoordinates( BigWarpDragOverlay.this.targetPoint );
				
				inProgress  = true;
				completedOK = false;
			}
		}
		
		@Override
		public void mouseReleased( MouseEvent e )
		{
			if( bw.isInLandmarkMode() && e.isShiftDown() )
			{
				completedOK = true;
				updateLandmarkModel();
			}
			else
			{
				// we did not complete
				reset();
			}
			inProgress = false;
		}
		
		@Override
		public void mouseDragged( MouseEvent e )
		{
			if( bw.isInLandmarkMode() && e.isShiftDown() )
			{
				thisViewer.getGlobalMouseCoordinates( BigWarpDragOverlay.this.targetPoint );
				thisViewer.requestRepaint();
			}
		}

		@Override
		public void mouseMoved( MouseEvent e ){}

		@Override
		public void mouseClicked( MouseEvent e ){}

		@Override
		public void mouseEntered( MouseEvent e ){}

		@Override
		public void mouseExited( MouseEvent e ){}

	}
	
}
