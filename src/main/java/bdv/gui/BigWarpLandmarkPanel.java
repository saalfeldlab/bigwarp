package bdv.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;

import javax.swing.CellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;

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

		table.setDefaultEditor( String.class,
				new TextFieldCellEditor( new TextFieldCell(table), String.class ));

		/*
		 * The line below should work according to
		 * https://tips4java.wordpress.com/2008/12/12/table-stop-editing/
		 * but does not seem to work.
		 */
//		table.putClientProperty( "terminateEditOnFocusLost", Boolean.TRUE );


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
	
	/**
	 * A JTable implementation that prevents keybindings from being propagated
	 * while editing cells. This prevents hotkeys from being activated.
	 *
	 */
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
			{
				return false;
			}
			else
				return super.processKeyBinding( ks, e, condition, pressed );
		}
    }

    public class TextFieldCell extends JTextField
    {
		private static final long serialVersionUID = -4327183047476876882L;

		public TextFieldCell( JTable cellTable )
		{
			super();
			final JTable table = cellTable;

			this.addFocusListener( new FocusListener()
			{
				@Override
				public void focusGained( FocusEvent e )
				{
					// do nothing
				}

				@Override
				public void focusLost( FocusEvent e )
				{
					CellEditor cellEditor = table.getCellEditor();
					if ( cellEditor != null )
					{
						if ( cellEditor.getCellEditorValue() != null )
						{
							cellEditor.stopCellEditing();
						} else
						{
							cellEditor.cancelCellEditing();
						}
					}
				}
			});
		}
    }

    public class TextFieldCellEditor extends DefaultCellEditor
    {
		private static final long serialVersionUID = 9185738725311357320L;

		TextFieldCell textField;    // an instance of edit field
		Class<?> columnClass;       // specifies cell type class
		Object valueObject;         // for storing correct value before editing

		public TextFieldCellEditor( TextFieldCell tf, Class< ? > cc )
		{
			super( tf );
			textField = tf;
			columnClass = cc;
			valueObject = null;
		}

		@Override
		public boolean stopCellEditing()
		{
			super.delegate.setValue( textField.getText() );
            fireEditingStopped();
            return true;
		}

		@Override
		public Component getTableCellEditorComponent( JTable table, Object value,
				boolean isSelected, int row, int column )
		{
			TextFieldCell tf = (TextFieldCell) super.getTableCellEditorComponent( table,
					value, isSelected, row, column );
			if ( value != null )
			{
				tf.setText( value.toString() );
			}
			// we have to save current value to restore it on another cell
			// selection if edited value couldn't be parsed to this cell's type
			valueObject = value;
			return tf;
		}

		@Override
		public Object getCellEditorValue()
		{
		    try {
		        // converting edited value to specified cell's type
		        if (columnClass.equals(Double.class))
		            return Double.parseDouble(textField.getText());
		        else if (columnClass.equals(Float.class))
		            return Float.parseFloat(textField.getText());
		        else if (columnClass.equals(Integer.class))
		            return Integer.parseInt(textField.getText());
		        else if (columnClass.equals(Byte.class))
		            return Byte.parseByte(textField.getText());
		        else if (columnClass.equals(String.class))
		            return textField.getText();
		    }
		    catch (NumberFormatException ex) {

		    }

		    // this handles restoring cell's value on jumping to another cell
		    if (valueObject != null) {
		        if (valueObject instanceof Double)
		            return ((Double)valueObject).doubleValue();
		        else if (valueObject instanceof Float)
		            return ((Float)valueObject).floatValue();
		        else if (valueObject instanceof Integer)
		            return ((Integer)valueObject).intValue();
		        else if (valueObject instanceof Byte)
		            return ((Byte)valueObject).byteValue();
		        else if (valueObject instanceof String)
		            return (String)valueObject;
		    }

		    return null;
		}
    }
}
