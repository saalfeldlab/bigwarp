/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2025 Howard Hughes Medical Institute.
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
package bdv.gui.sourceList;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bdv.gui.BigWarpLandmarkPanel.JTableChecking;
import bdv.gui.BigWarpLandmarkPanel.TextFieldCell;
import bdv.gui.BigWarpLandmarkPanel.TextFieldCellEditor;
import bdv.gui.sourceList.BigWarpSourceTableModel.ButtonEditor;
import bdv.gui.sourceList.BigWarpSourceTableModel.ButtonRenderer;

public class BigWarpSourceListPanel extends JPanel
{
	private static final long serialVersionUID = 2370565900502584680L;

	protected BigWarpSourceTableModel tableModel;

	protected JTableChecking table;

	public final Logger logger = LoggerFactory.getLogger( BigWarpSourceListPanel.class );

	public BigWarpSourceListPanel( BigWarpSourceTableModel tableModel )
	{
		super( new GridLayout( 1, 0 ) );

		// set table model re-generates the table
		setTableModel( tableModel );

		final JScrollPane scrollPane = new JScrollPane( table );
		add( scrollPane );
	}

	public BigWarpSourceTableModel getTableModel()
	{
		return tableModel;
	}

	public void genJTable()
	{
		table = new JTableChecking( tableModel );
		table.setPreferredScrollableViewportSize( new Dimension( 500, 70 ) );
		table.setFillsViewportHeight( true );
		table.setShowVerticalLines( false );

		table.setDefaultEditor( String.class,
				new TextFieldCellEditor( new TextFieldCell(table), String.class ));

		table.getColumn( " " ).setCellRenderer( new ButtonRenderer() );
		table.getColumn( " " ).setCellEditor( new ButtonEditor( new JCheckBox(), tableModel ) );
		table.getColumn( " " ).setPreferredWidth( 24 );
		table.getColumn( " " ).setWidth( 24 );
	}

	public void setTableModel( BigWarpSourceTableModel tableModel )
	{
		this.tableModel = tableModel;
		genJTable();
	}

	public JTable getJTable()
	{
		return table;
	}
	

}
