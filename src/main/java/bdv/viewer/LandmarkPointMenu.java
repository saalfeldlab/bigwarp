package bdv.viewer;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.scijava.ui.behaviour.util.AbstractNamedAction;

import bdv.gui.BigWarpLandmarkPanel;
import bigwarp.BigWarp;

public class LandmarkPointMenu extends JPopupMenu 
{

	public static final boolean MOVING = true;
	public static final boolean FIXED = false;

	public static final String CLEAR_MOVING = "table clear moving";
	public static final String CLEAR_FIXED = "table clear fixed";
	public static final String CLEAR_SELECTED_MOVING = "table clear selected moving";
	public static final String CLEAR_SELECTED_FIXED = "table clear selected fixed";

	public static final String DELETE = "table delete";
	public static final String DELETE_SELECTED = "table delete selected ";

	public static final String ACTIVATE = "table activate";
	public static final String ACTIVATE_SELECTED = "table activate selected ";

	private static final long serialVersionUID = -3676180390835767585L;
	
	protected BigWarpLandmarkPanel landmarkPanel;
	protected BigWarp bw;
	
//	public ClearHandler clearMoving;
//	public ClearHandler clearFixed;
	public ClearSelectedHandler clearSelectedMoving;
	public ClearSelectedHandler clearSelectedFixed;

//	public DeleteHandler deleteHandler;
	public DeleteSelectedHandler deleteSelectedHandler;
	public ActivateSelectedHandler activateAllHandler;
	public DeactivateSelectedHandler deactivateAllHandler;
	
	protected MouseListener popupListener;
//	protected JMenuItem deleteSingleItem;
	protected JMenuItem deleteAllItem;
	protected JMenuItem activateAllItem;
	protected JMenuItem deactivateAllItem;

//	protected JMenuItem clearMovingItem;
//	protected JMenuItem clearFixedItem;
	protected JMenuItem clearSelectedMovingItem;
	protected JMenuItem clearSelectedFixedItem;

	private Point clickPt;
	
	public LandmarkPointMenu( BigWarp bw )
	{
		this( bw.getLandmarkPanel() );
		this.bw = bw;
	}
	
	public LandmarkPointMenu( BigWarpLandmarkPanel landmarkPanel )
	{
		this.landmarkPanel = landmarkPanel;

//		deleteHandler = new DeleteHandler( DELETE );
		deleteSelectedHandler = new DeleteSelectedHandler( DELETE_SELECTED );
		activateAllHandler = new ActivateSelectedHandler( ACTIVATE );
		deactivateAllHandler = new DeactivateSelectedHandler( ACTIVATE_SELECTED );

//		clearMoving = new ClearHandler( CLEAR_MOVING, MOVING );
//		clearFixed = new ClearHandler( CLEAR_FIXED, FIXED );
		clearSelectedMoving = new ClearSelectedHandler( CLEAR_SELECTED_MOVING, MOVING );
		clearSelectedFixed = new ClearSelectedHandler( CLEAR_SELECTED_FIXED, FIXED );

		popupListener = new PopupListener();

//		deleteSingleItem = new JMenuItem( "Delete" );
//		deleteSingleItem.addActionListener( deleteHandler );

		deleteAllItem = new JMenuItem( "Delete" );
		deleteAllItem.addActionListener( deleteSelectedHandler );

//		clearMovingItem = new JMenuItem( "Clear moving point" );
//		clearMovingItem.addActionListener( clearMoving );
//
//		clearFixedItem = new JMenuItem( "Clear fixed point" );
//		clearFixedItem.addActionListener( clearFixed );

		clearSelectedMovingItem = new JMenuItem( "Clear moving" );
		clearSelectedMovingItem.addActionListener( clearSelectedMoving );

		clearSelectedFixedItem = new JMenuItem( "Clear fixed" );
		clearSelectedFixedItem.addActionListener( clearSelectedFixed );

		activateAllItem = new JMenuItem( "Activate" );
		activateAllItem.addActionListener( activateAllHandler );

		deactivateAllItem = new JMenuItem( "Deactivate" );
		deactivateAllItem.addActionListener( deactivateAllHandler );

//		this.add( deleteSingleItem );
		this.add( deleteAllItem );

		this.addSeparator();
//		this.add( clearMovingItem );
//		this.add( clearFixedItem );
		this.add( clearSelectedMovingItem );
		this.add( clearSelectedFixedItem );

		this.addSeparator();
		this.add( activateAllItem );
		this.add( deactivateAllItem );
	}
	
	public void setupListeners( )
	{
		landmarkPanel.getJTable().addMouseListener( popupListener );
	}
	
	private class PopupListener extends MouseAdapter 
	{
		public void mousePressed( MouseEvent e ) 
		{
	        maybeShowPopup( e );
	    }
		
		private void maybeShowPopup( MouseEvent e ) 
		{
	        if ( isRightClick( e ))
	        {
	        	clickPt = e.getPoint();
	        	LandmarkPointMenu.this.show(
	        			e.getComponent(),
	        			e.getX(), e.getY());
	        }
	    }

		private boolean isRightClick( MouseEvent e )
		{
			if ( e.getButton() == MouseEvent.BUTTON3 )
				return true;
			else return false;
		}
	}

	public class DeleteHandler extends AbstractNamedAction
	{
		private static final long serialVersionUID = -3405442710985689438L;

		public DeleteHandler( final String name )
		{
			super( name );
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			int j = landmarkPanel.getJTable().rowAtPoint( clickPt );
			landmarkPanel.getTableModel().deleteRow( j );

			if( bw != null )
				bw.restimateTransformation();
			
			landmarkPanel.repaint();
		}
	}

	public class DeleteSelectedHandler extends AbstractNamedAction
	{
		private static final long serialVersionUID = -6499718644014842587L;

		public DeleteSelectedHandler( String name )
		{
			super( name );
		}

		@Override
		public void actionPerformed(ActionEvent e) 
		{
			int[] selectedRows = landmarkPanel.getJTable().getSelectedRows();

			// do in reverse order so that the index
			for( int i = selectedRows.length - 1; i >= 0; i-- )
			{
				int j = selectedRows[ i ];
				landmarkPanel.getTableModel().deleteRow( j );
			}

			if( bw != null )
				bw.restimateTransformation();
			
			landmarkPanel.repaint();
		}
	}
	
	public class ClearHandler extends AbstractNamedAction
	{
		private static final long serialVersionUID = -2082631635521009679L;
		final boolean moving;

		public ClearHandler( final String name, final boolean isMoving )
		{
			super( name );
			this.moving = isMoving;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			int j = landmarkPanel.getJTable().rowAtPoint( clickPt );
			landmarkPanel.getTableModel().clearPt( j, moving );

			if ( bw != null )
				bw.restimateTransformation();

			landmarkPanel.repaint();
		}
	}

	public class ClearSelectedHandler extends AbstractNamedAction
	{
		private static final long serialVersionUID = -7023942870606943587L;
		final boolean moving;

		public ClearSelectedHandler( final String name, final boolean isMoving )
		{
			super( name );
			this.moving = isMoving;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			int[] selectedRows = landmarkPanel.getJTable().getSelectedRows();

			// do in reverse order so that the index
			for ( int i = selectedRows.length - 1; i >= 0; i-- )
			{
				int j = selectedRows[ i ];
				landmarkPanel.getTableModel().clearPt( j, moving );
			}

			if ( bw != null )
				bw.restimateTransformation();

			landmarkPanel.repaint();
		}
	}

	public class ActivateSelectedHandler extends AbstractNamedAction
	{
		private static final long serialVersionUID = 3617611493865908091L;

		public ActivateSelectedHandler( String name )
		{
			super( name );
		}

		@Override
		public void actionPerformed(ActionEvent e) 
		{
			int[] selectedRows = landmarkPanel.getJTable().getSelectedRows();

			// do in reverse order so that the index
			for( int i = selectedRows.length - 1; i >= 0; i-- )
			{
				int j = selectedRows[ i ];
				landmarkPanel.getTableModel().setIsActive( j, true );
			}

			if( bw != null )
				bw.restimateTransformation();
			
			landmarkPanel.repaint();
		}
	}
	
	public class DeactivateSelectedHandler extends AbstractNamedAction
	{
		private static final long serialVersionUID = -7703797647219426189L;

		public DeactivateSelectedHandler( String name )
		{
			super( name );
		}

		@Override
		public void actionPerformed(ActionEvent e) 
		{
			int[] selectedRows = landmarkPanel.getJTable().getSelectedRows();

			// do in reverse order so that the index
			for( int i = selectedRows.length - 1; i >= 0; i-- )
			{
				int j = selectedRows[ i ];
				landmarkPanel.getTableModel().setIsActive( j, false );
			}

			if( bw != null )
				bw.restimateTransformation();
			
			landmarkPanel.repaint();
		}
	}
}
