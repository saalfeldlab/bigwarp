package bdv.viewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import bdv.gui.BigWarpLandmarkPanel;

public class LandmarkPointMenu extends JPopupMenu 
{

	private static final long serialVersionUID = -3676180390835767585L;
	
	protected BigWarpLandmarkPanel landmarkPanel;
	
	protected LandmarkMenuHandler handler;
	protected MouseListener popupListener;
	protected JMenuItem deleteItem;
	
	public LandmarkPointMenu( BigWarpLandmarkPanel landmarkPanel )
	{
		this.landmarkPanel = landmarkPanel;
		
		handler = new LandmarkMenuHandler();
		popupListener = new PopupListener();
		
		deleteItem = new JMenuItem("Delete");
		deleteItem.addActionListener( handler );
		
		this.add( deleteItem );
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
	        	LandmarkPointMenu.this.show(
	        			e.getComponent(),
	        			e.getX(), e.getY());
	        }
	    }
	}
	
	private class LandmarkMenuHandler implements ActionListener 
	{
		@Override
		public void actionPerformed(ActionEvent e) 
		{
			System.out.println( e.getSource() );
			int[] selectedRows = landmarkPanel.getJTable().getSelectedRows();
			for( int i = 0; i < selectedRows.length; i++ )
			{
				int j = selectedRows[ i ];
				landmarkPanel.getTableModel().deleteRow( j );
			}
		}
	}
}
