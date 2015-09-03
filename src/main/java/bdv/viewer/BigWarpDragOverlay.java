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
	
	private Color baseColor;
	
	public BigWarpDragOverlay( final BigWarp bw, final BigWarpViewerPanel viewer )
	{
		this.bw = bw;
		this.viewer = viewer;
		movingPoint = new RealPoint( 3 );
		targetPoint = new RealPoint( 3 );
		movingPointScreen = new RealPoint( 3 );
		targetPointScreen = new RealPoint( 3 );
		
		arad = viewer.getSettings().getSpotSize();
		baseColor = viewer.getSettings().getSpotColor();

		mouseListener = new WarpDragMouseListener( bw, viewer );
	}
	
	public void reset()
	{
		completedOK = false;
	}

	
	public void paint( final Graphics2D g ) 
	{
		arad = viewer.getSettings().getSpotSize();
		baseColor = viewer.getSettings().getSpotColor();
		
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
		
		if( !completedOK )
			return;
		
		double[] movingPtArray = new double[ 3 ];
		double[] targetPtArray = new double[ 3 ];
		
		movingPoint.localize(  movingPtArray );
		targetPoint.localize(  targetPtArray );
		
		bw.addPoint( movingPtArray, true,  viewer );
		bw.addPoint( targetPtArray, false, viewer );
		
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
