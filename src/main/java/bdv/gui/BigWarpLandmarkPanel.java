package bdv.gui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;

import bigwarp.landmarks.LandmarkTableModel;

public class BigWarpLandmarkPanel extends JPanel {
	
	private static final long serialVersionUID = 8470689265638231579L;

	protected LandmarkTableModel tableModel;
	protected JTable table;
	
	
    public BigWarpLandmarkPanel( LandmarkTableModel tableModel ) {
        
    	super(new GridLayout(1,0));
    	setTableModel( tableModel );
        
        genJTable();
        
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        //Add the scroll pane to this panel.
        add(scrollPane);
    }

    public LandmarkTableModel getTableModel() {
		return tableModel;
	}
    
    public void genJTable()
    {
		table = new JTableChecking( getTableModel() );

		table.setPreferredScrollableViewportSize( new Dimension( 400, 800 ) );
		table.setFillsViewportHeight( true );
		table.setShowVerticalLines( false );
    }
    
    public void setTableModel( LandmarkTableModel tableModel )
    {
		this.tableModel = tableModel;
		genJTable();
	}
    
    public JTable getJTable(){
    	return table;
    }
	
    public class JTableChecking extends JTable
    {
		private static final long serialVersionUID = 1437406384583869710L;

		public JTableChecking( LandmarkTableModel tableModel )
		{
			super( tableModel );
		}

		protected boolean processKeyBinding(
				KeyStroke ks, KeyEvent e,
				int condition, boolean pressed )
		{
			if ( isEditing() )
				return false;
			else
				return super.processKeyBinding( ks, e, condition, pressed );
		}
    }
}
