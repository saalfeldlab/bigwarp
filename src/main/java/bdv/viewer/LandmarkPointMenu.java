package bdv.viewer;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import bdv.gui.BigWarpLandmarkPanel;
import bigwarp.BigWarp;

public class LandmarkPointMenu extends JPopupMenu 
{

	private static final long serialVersionUID = -3676180390835767585L;
	
	protected BigWarpLandmarkPanel landmarkPanel;
	protected BigWarp bw;
	
	protected DeleteOneSelectedHandler oneHandler;
	protected DeleteAllSelectedHandler allHandler;
	protected MouseListener popupListener;
	protected JMenuItem deleteSingleItem;
	protected JMenuItem deleteAllItem;

	private Point clickPt;
	
	public LandmarkPointMenu( BigWarp bw )
	{
		this( bw.getLandmarkPanel() );
		this.bw = bw;
	}
	
	public LandmarkPointMenu( BigWarpLandmarkPanel landmarkPanel )
	{
		this.landmarkPanel = landmarkPanel;
		
		oneHandler = new DeleteOneSelectedHandler();
		allHandler = new DeleteAllSelectedHandler();
		popupListener = new PopupListener();

		deleteSingleItem = new JMenuItem("Delete");
		deleteSingleItem.addActionListener( oneHandler );

		deleteAllItem = new JMenuItem("Delete all selected");
		deleteAllItem.addActionListener( allHandler );

		this.add( deleteSingleItem );
		this.add( deleteAllItem );
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
	        if (e.isPopupTrigger())
	        {
	        	clickPt = e.getPoint();
	        	LandmarkPointMenu.this.show(
	        			e.getComponent(),
	        			e.getX(), e.getY());
	        }
	    }
	}

	private class DeleteOneSelectedHandler implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			int j = landmarkPanel.getJTable().rowAtPoint( clickPt );
			landmarkPanel.getTableModel().deleteRow( j );

			if( bw != null )
				bw.restimateTransformation();
		}
	}
	
	private class DeleteAllSelectedHandler implements ActionListener
	{
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
		}
	}
}
