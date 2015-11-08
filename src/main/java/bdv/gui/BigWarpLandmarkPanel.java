package bdv.gui;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

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
    	 table = new JTable( getTableModel() );
         
         table.setPreferredScrollableViewportSize(new Dimension(400, 800));
         table.setFillsViewportHeight(true);
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
	
}
