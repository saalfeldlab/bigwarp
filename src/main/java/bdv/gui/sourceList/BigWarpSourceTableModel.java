package bdv.gui.sourceList;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

public class BigWarpSourceTableModel extends AbstractTableModel 
{
	private static final long serialVersionUID = 5923947651732788341L;

	protected final static String[] colNames = new String[] { "Name", "Moving", "Transform", "remove" };

	protected final String[] columnNames;
	protected final ArrayList<SourceRow> sources;
	protected final ArrayList<RemoveRowButton> rmRowButtons;

	protected static int movingColIdx = 1;
	protected static int removeColIdx = 3;

	private Component container;

	public BigWarpSourceTableModel()
	{
		super();
		columnNames = colNames;
		sources = new ArrayList<>();
		rmRowButtons = new ArrayList<>();
	}
	
	/**
	 * Set the {@link Component} to repaint when a row is removed.
	 * 
	 * @param container the component containing this table
	 */
	public void setContainer( Component container )
	{
		this.container = container;
	}

	public SourceRow get( int i )
	{
		return sources.get( i );
	}

	@Override 
	public String getColumnName( int col ){
		return columnNames[col];
	}

	@Override
	public int getColumnCount()
	{
		return columnNames.length;
	}

	@Override
	public int getRowCount()
	{
		return sources.size();
	}

	@Override
	public Object getValueAt( int r, int c )
	{
		if( c == 3 )
			return rmRowButtons.get( r );
		else
			return sources.get( r ).get( c );
	}

	public Class<?> getColumnClass( int col ){
		if ( col == 1 )
			return Boolean.class;
		else if ( col == 3 )
			return JButton.class;
		else
			return String.class;
	}

	@Override
	public boolean isCellEditable( int row, int col )
	{
		return ( col == movingColIdx ) || ( col == removeColIdx );
	}

	@Override
	public void setValueAt(Object value, int row, int col)
	{
		if( col == movingColIdx )
			sources.get( row ).moving = (Boolean)value;
	}

	public void add( String srcName, boolean moving, boolean isImagePlus )
	{
		final RemoveRowButton rmButton = new RemoveRowButton( sources.size() );
//		rmButton.addActionListener( e -> {
//			System.out.println( "pushed rm row button " + rmButton.getRow() );
//			remove( rmButton.getRow() );
//		});

		rmRowButtons.add( rmButton );
		sources.add( new SourceRow( srcName, moving, "", isImagePlus ));
	}

	public void setTransform( int i, String transform )
	{
		sources.get( i ).transformName = transform;
	}

	public void add( String srcName, boolean moving )
	{
		add( srcName, moving, false );
	}

	public void add( String srcName )
	{
		add( srcName, false );
	}

	public void addImagePlus( String srcName )
	{
		addImagePlus( srcName, false );
	}

	public void addImagePlus( String srcName, boolean isMoving )
	{
		add( srcName, isMoving, true );
	}

	public boolean remove( int i )
	{
		if( i >= sources.size() )
		{
			System.out.println( "NOT REMOVED - SHOULD NEVER BE CALLED" );
			return false;
		}

		sources.remove( i );
		rmRowButtons.remove( i );
		updateRmButtonIndexes();

		if( container != null )
			container.repaint();

		return true;
	}

	private void updateRmButtonIndexes()
	{
		for( int i = 0; i < rmRowButtons.size(); i++ )
			rmRowButtons.get( i ).setRow( i );
	}

	public static class SourceRow
	{
		public String srcName;
		public boolean moving;
		public String transformName;
		
		public boolean isImagePlus;
		
		public SourceRow( String srcName, boolean moving, String transformName, boolean isImagePlus )
		{
			this.srcName = srcName;
			this.moving = moving;
			this.transformName = transformName;
			this.isImagePlus = isImagePlus;
		}

		public SourceRow( String srcName, boolean moving, String transformName )
		{
			this( srcName, moving, transformName, false );
		}

		public Object get( int c )
		{
			if( c == 0 )
				return srcName;
			else if( c == 1 )
				return moving;
			else if ( c == 2 )
				return transformName;
			else
				return null;
		}
	}

	protected static class RemoveRowButton extends JButton {
		private int row;
		public RemoveRowButton( int row )
		{
			super( "-" );
			setRow( row );
		}

		public int getRow()
		{
			return row;
		}

		public void setRow(int row)
		{
			this.row = row;
		}
	}

	/**
	 * From 
	 * http://www.java2s.com/Code/Java/Swing-Components/ButtonTableExample.htm
	 */
	protected static class ButtonRenderer extends JButton implements TableCellRenderer
	{
		public ButtonRenderer()
		{
			setOpaque( true );
		}

		public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
		{
			if ( isSelected )
			{
				setForeground( table.getSelectionForeground() );
				setBackground( table.getSelectionBackground() );
			}
			else
			{
				setForeground( table.getForeground() );
				setBackground( UIManager.getColor( "Button.background" ) );
			}
			setText( ( value == null ) ? "" : ((RemoveRowButton)value).getText());
			return this;
		}
	}

	/**
	 * From 
	 * http://www.java2s.com/Code/Java/Swing-Components/ButtonTableExample.htm
	 */
	protected static class ButtonEditor extends DefaultCellEditor
	{
		protected JButton button;

		private String label;
		
		private RemoveRowButton thisButton;

		private BigWarpSourceTableModel model;

		private boolean isPushed;

		public ButtonEditor( JCheckBox checkBox, BigWarpSourceTableModel model )
		{
			super( checkBox );
			checkBox.setText( "-" );
			this.model = model;

			button = new JButton();
			button.setOpaque( true );
			button.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					fireEditingStopped();
				}
			} );
		}

		public Component getTableCellEditorComponent( JTable table, Object value, boolean isSelected, int row, int column )
		{
			if ( isSelected )
			{
				button.setForeground( table.getSelectionForeground() );
				button.setBackground( table.getSelectionBackground() );
			}
			else
			{
				button.setForeground( table.getForeground() );
				button.setBackground( table.getBackground() );
			}
			thisButton = ((RemoveRowButton)value);
			label = ( value == null ) ? "" : thisButton.getText();
			button.setText( label );
			isPushed = true;
			return button;
		}

		public Object getCellEditorValue()
		{
			if ( isPushed )
			{
				 model.remove( thisButton.getRow() );
			}
			isPushed = false;
			return new String( label );
		}

		public boolean stopCellEditing()
		{
			isPushed = false;
			return super.stopCellEditing();
		}

		protected void fireEditingStopped()
		{
			super.fireEditingStopped();
		}
	}

}
