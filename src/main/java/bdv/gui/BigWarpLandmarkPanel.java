package bdv.gui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import bigwarp.landmarks.LandmarkTableModel;

public class BigWarpLandmarkPanel extends JPanel {
	
	private static final long serialVersionUID = 8470689265638231579L;

	protected LandmarkTableModel tableModel;
	protected JTable table;
	
	public final Logger logger = LogManager.getLogger( BigWarpLandmarkPanel.class.getName() );

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

		/*
		 * Add a listener to update the next row the tableModel will edit
		 * based on the selected row.
		 * 
		 * Specifically, when the user changes the selected row of the table, the
		 * listener checks whether any of those rows are "incomplete."
		 * If so, the first row in the selection that does not have a moving image
		 * becomes the next row to be updated for the moving image.
		 * The next target landmark to be updated is changed as well.
		 */
		table.getSelectionModel().addListSelectionListener( new ListSelectionListener()
		{
			@Override
			public void valueChanged( ListSelectionEvent e )
			{
				logger.trace( "table selection changed" );
				boolean setMoving = false;
				boolean setFixed = false;
				int row = table.getSelectedRow();

				// if no rows are selected, the next edit should add a new row
				if( row < 0 )
				{
					tableModel.setNextRow( true, tableModel.getRowCount() );
					tableModel.setNextRow( false, tableModel.getRowCount() );
					return;
				}

				if ( !tableModel.isMovingPoint( row ) && !setMoving )
				{
					tableModel.setNextRow( true, row );
					setMoving = true;
					logger.trace( "nextRow Moving: " + row );
				} else
					tableModel.setNextRow( true, tableModel.getRowCount() );

				if ( !tableModel.isFixedPoint( row ) && !setFixed )
				{
					tableModel.setNextRow( false, row );
					setFixed = true;
					logger.trace( "nextRow Fixed: " + row );
				} else
					tableModel.setNextRow( false, tableModel.getRowCount() );

			}
		});
    }
    
    public void setTableModel( LandmarkTableModel tableModel )
    {
		this.tableModel = tableModel;
		genJTable();
	}
    
    public JTable getJTable()
    {
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
