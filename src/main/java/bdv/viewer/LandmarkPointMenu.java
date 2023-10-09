/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2022 Howard Hughes Medical Institute.
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
package bdv.viewer;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.scijava.ui.behaviour.util.AbstractNamedAction;

import bdv.gui.BigWarpLandmarkPanel;
import bigwarp.BigWarp;
import bigwarp.BigWarpActions;
import bigwarp.landmarks.LandmarkTableModel;

public class LandmarkPointMenu extends JPopupMenu 
{

	public static final boolean MOVING = true;
	public static final boolean FIXED = false;

//	public static final String CLEAR_MOVING = "table clear moving";
//	public static final String CLEAR_FIXED = "table clear fixed";
//	public static final String CLEAR_SELECTED_MOVING = "table clear selected moving";
//	public static final String CLEAR_SELECTED_FIXED = "table clear selected fixed";
//
//	public static final String DELETE = "table delete";
//	public static final String DELETE_SELECTED = "table delete selected ";
//
//	public static final String ACTIVATE_SELECTED = "table activate selected";
//	public static final String DEACTIVATE_SELECTED = "table deactivate selected ";

	private static final long serialVersionUID = -3676180390835767585L;
	
	protected BigWarpLandmarkPanel landmarkPanel;
	protected BigWarp< ? > bw;

	public ClearSelectedHandler clearSelectedMoving;
	public ClearSelectedHandler clearSelectedFixed;

	public DeleteSelectedHandler deleteSelectedHandler;
	public ActivateSelectedHandler activateSelectedHandler;
	public DeactivateSelectedHandler deactivateSelectedHandler;

	public AddToSelection addAboveHandler;
	public AddToSelection addAllAboveHandler;
	public AddToSelection addBelowHandler;
	public AddToSelection addAllBelowHandler;

	protected MouseListener popupListener;
	protected JMenuItem deleteAllItem;
	protected JMenuItem activateAllItem;
	protected JMenuItem deactivateAllItem;

	protected JMenuItem clearSelectedMovingItem;
	protected JMenuItem clearSelectedFixedItem;

	private Point clickPt;
	
	public LandmarkPointMenu( BigWarp< ? > bw )
	{
		this( bw.getLandmarkPanel() );
		this.bw = bw;
	}
	
	public LandmarkPointMenu( BigWarpLandmarkPanel landmarkPanel )
	{
		this.landmarkPanel = landmarkPanel;

		deleteSelectedHandler = new DeleteSelectedHandler( BigWarpActions.DELETE_SELECTED );
		activateSelectedHandler = new ActivateSelectedHandler( BigWarpActions.ACTIVATE_SELECTED );
		deactivateSelectedHandler = new DeactivateSelectedHandler( BigWarpActions.DEACTIVATE_SELECTED );

		clearSelectedMoving = new ClearSelectedHandler( BigWarpActions.CLEAR_SELECTED_MOVING, MOVING );
		clearSelectedFixed = new ClearSelectedHandler( BigWarpActions.CLEAR_SELECTED_FIXED, FIXED );

		addAboveHandler = new AddToSelection( BigWarpActions.LANDMARK_SELECT_ABOVE, true, false );
		addAllAboveHandler = new AddToSelection( BigWarpActions.LANDMARK_SELECT_ALL_ABOVE, true, true );
		addBelowHandler = new AddToSelection( BigWarpActions.LANDMARK_SELECT_BELOW, false, false );
		addAllBelowHandler = new AddToSelection( BigWarpActions.LANDMARK_SELECT_ALL_BELOW, false, true );

		popupListener = new PopupListener();

		deleteAllItem = new JMenuItem( "Delete" );
		deleteAllItem.addActionListener( deleteSelectedHandler );

		clearSelectedMovingItem = new JMenuItem( "Clear moving" );
		clearSelectedMovingItem.addActionListener( clearSelectedMoving );

		clearSelectedFixedItem = new JMenuItem( "Clear fixed" );
		clearSelectedFixedItem.addActionListener( clearSelectedFixed );

		activateAllItem = new JMenuItem( "Activate" );
		activateAllItem.addActionListener( activateSelectedHandler );

		deactivateAllItem = new JMenuItem( "Deactivate" );
		deactivateAllItem.addActionListener( deactivateSelectedHandler );

		this.add( deleteAllItem );

		this.addSeparator();
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
			LandmarkTableModel model = landmarkPanel.getTableModel();
			model.deleteRow( j );

			updateBigwarp( bw, landmarkPanel.getTableModel() );

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

			updateBigwarp( bw, landmarkPanel.getTableModel() );

			landmarkPanel.repaint();
		}
	}

	public static void updateBigwarp( final BigWarp< ? > bw, final LandmarkTableModel model )
	{
		if( bw != null )
		{
			if( model.getActiveRowCount() < 4  && 
				bw.getOverlayP().getIsTransformed() )
			{
				bw.getViewerFrameP().getViewerPanel().showMessage( "Require 4 points for transform" );
				bw.setIsMovingDisplayTransformed( false );
			}
			else
			{
				bw.restimateTransformation();
			}
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
			final int[] selectedRows = landmarkPanel.getJTable().getSelectedRows();
			Arrays.sort( selectedRows );

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
			final int[] selectedRows = landmarkPanel.getJTable().getSelectedRows();
			Arrays.sort( selectedRows );

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

	public class AddToSelection extends AbstractNamedAction
	{
		private static final long serialVersionUID = -904756750247052099L;

		private final boolean before;
		private final boolean all;

		public AddToSelection( final String name, final boolean before, final boolean all )
		{
			super( name );
			this.before = before;
			this.all = all;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			final int[] selectedRows = landmarkPanel.getJTable().getSelectedRows();
			if( selectedRows.length == 0 )
				return;

			Arrays.sort( selectedRows );

			int i;
			int j;
			if ( before )
			{
				j = selectedRows[ 0 ];
				i = all ? 0 : j - 1;
			} else
			{
				i = selectedRows[ selectedRows.length - 1 ];
				j = all ? landmarkPanel.getJTable().getRowCount() - 1 : i + 1;
			}
			landmarkPanel.getJTable().getSelectionModel().addSelectionInterval( i, j );

			if ( bw != null )
				bw.restimateTransformation();

			landmarkPanel.repaint();
		}
	}

}
