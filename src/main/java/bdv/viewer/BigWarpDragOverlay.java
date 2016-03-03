package bdv.viewer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import bigwarp.BigWarp;
import bigwarp.BigWarp.SolveThread;

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
	
	final int ndim;

	private double arad = 4.0;
	
	private Color baseColor;
	
	public BigWarpDragOverlay( final BigWarp bw, final BigWarpViewerPanel viewer, final SolveThread solveThread )
	{
		this.bw = bw;
		this.viewer = viewer;
		ndim = 3;
		movingPoint = new RealPoint( 3 );
		targetPoint = new RealPoint( 3 );
		movingPointScreen = new RealPoint( 3 );
		targetPointScreen = new RealPoint( 3 );
		
		arad = viewer.getSettings().getSpotSize();
		baseColor = viewer.getSettings().getSpotColor();

		mouseListener = new WarpDragMouseListener( bw, viewer, solveThread );
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

	protected class WarpDragMouseListener implements MouseListener, MouseMotionListener
	{
		private BigWarp bw;
		private BigWarpViewerPanel thisViewer;

		private int index;
		double[] targetPtArray = new double[ ndim ];

		final private SolveThread solverThread;

		public WarpDragMouseListener( final BigWarp bw, final BigWarpViewerPanel thisViewer, SolveThread solverThread )
		{
			this.bw = bw;
			setViewer( thisViewer );
			thisViewer.getDisplay().addHandler( this );

			this.solverThread = solverThread;
		}
		
		protected void setViewer( BigWarpViewerPanel thisViewer )
		{
			this.thisViewer = thisViewer;
		}
		
		@Override
		public void mousePressed( MouseEvent e )
		{
			if( thisViewer.isMoving && !bw.isMovingDisplayTransformed())
				return;

			if( bw.isInLandmarkMode() && e.isShiftDown() )
			{
				thisViewer.getGlobalMouseCoordinates( BigWarpDragOverlay.this.movingPoint );
				thisViewer.getGlobalMouseCoordinates( BigWarpDragOverlay.this.targetPoint );

				double[] movingPtArray = new double[ ndim ];
				BigWarpDragOverlay.this.movingPoint.localize( movingPtArray );

				// add undoable point edit - moving point
				bw.getLandmarkPanel().getTableModel().pointEdit( -1, movingPtArray, true, true, true, true );
				index = bw.getLandmarkPanel().getTableModel().getRowCount() - 1;
				
				// add point edit - fixed point, at the same place as the moving point but not undoable
				bw.getLandmarkPanel().getTableModel().pointEdit( index, movingPtArray, false, false, false, false);

				inProgress  = true;
				completedOK = false;
			}
		}
		
		@Override
		public void mouseReleased( MouseEvent e )
		{
			if ( bw.isInLandmarkMode() && e.isShiftDown() )
			{
				completedOK = true;

				reset();

				// make an undo-able action for adding the fixed point
				thisViewer.getGlobalMouseCoordinates( BigWarpDragOverlay.this.targetPoint );
				targetPoint.localize( targetPtArray );
				bw.getLandmarkPanel().getTableModel().pointEdit( index, targetPtArray, false, false, false, true );

				if ( bw.isUpdateWarpOnChange() )
					bw.restimateTransformation();
			}
			else
			{
				// we did not complete
				reset();

				// undo the moving point addition
				// currently not undo-able
				// TODO consider allowing an undo of this
				//bw.getLandmarkPanel().getTableModel().deleteRowHelper( index );
			}
			inProgress = false;
		}
		
		@Override
		public void mouseDragged( MouseEvent e )
		{
			if( bw.isInLandmarkMode() && e.isShiftDown() )
			{
				thisViewer.getGlobalMouseCoordinates( BigWarpDragOverlay.this.targetPoint );
				targetPoint.localize(  targetPtArray );

				// In the click-drag paradigm, the drag part always effects the fixed image
				if( bw.isMovingDisplayTransformed() &&
						bw.getLandmarkPanel().getTableModel().isActive( index ) )
				{
					solverThread.requestResolve( false, index, targetPtArray );
				}

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
