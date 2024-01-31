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
package bdv.gui;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import bdv.export.ProgressWriter;
import bdv.ij.ApplyBigwarpPlugin;
import bdv.ij.ApplyBigwarpPlugin.WriteDestinationOptions;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarpData;
import net.imglib2.Interval;

public class BigwarpLandmarkSelectionPanel<T> extends JPanel
{
	
	private static final long serialVersionUID = -6996547400011766393L;

	JFrame frame;
	JTable table;
	private final PointSelectorTableModel selectionTable;
	private boolean doExport = false;

	final BigWarpData< T > data;
	final List< SourceAndConverter< T >> sources;
	final String fieldOfViewOption;
	final List<Interval> outputIntervalList;
	final List<String> matchedPtNames;
	final Interpolation interp;
	final double[] offsetIn;
	final double[] resolution;
	final boolean isVirtual;
	final int nThreads;
	final ProgressWriter progressWriter;

	/**
	 * Displays a dialog showing point matches
	 * 
	 * @param data the data
	 * @param sources list of sources
	 * @param fieldOfViewOption option for inferring the field of view
	 * @param outputIntervalList list of output intervals
	 * @param matchedPtNames names of points for matching
	 * @param interp interpolation
	 * @param offsetIn the offset
	 * @param resolution the resolution
	 * @param isVirtual if true
	 * @param nThreads number of threads
	 * @param progressWriter progress writer
	 */
	public BigwarpLandmarkSelectionPanel( 
			final BigWarpData<T> data,
			final List< SourceAndConverter<T>> sources,
			final String fieldOfViewOption,
			final List<Interval> outputIntervalList,
			final List<String> matchedPtNames,
			final Interpolation interp,
			final double[] offsetIn,
			final double[] resolution,
			final boolean isVirtual,
			final int nThreads,
			final ProgressWriter progressWriter )
	{
		// set fields used for export
		this.data = data;
		this.sources = sources;
		this.fieldOfViewOption = fieldOfViewOption;
		this.outputIntervalList = outputIntervalList;
		this.matchedPtNames = matchedPtNames;
		this.interp = interp;
		this.offsetIn = offsetIn;
		this.resolution = resolution;
		this.isVirtual = isVirtual;
		this.nThreads = nThreads;
		this.progressWriter = progressWriter;


		selectionTable = new PointSelectorTableModel( matchedPtNames );
		genJTable();
		
		setLayout( new BorderLayout() );

		// add informational window
		JPanel infoPanel = new JPanel ();
		infoPanel.setBorder ( new TitledBorder ( new EtchedBorder (), "" ) );

		JTextArea textField = new JTextArea( 2, 20 );
		textField.append( String.format("Matched %d points\n", selectionTable.getRowCount() ) );
		textField.append( "Select points to use below and press OK to continue export." );
		textField.setEditable( false );
		infoPanel.add( textField );
	    add( infoPanel, BorderLayout.NORTH );

        // Create the scroll pane for the table
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER );

		JPanel bottomPanel = new JPanel ();
        bottomPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

        // add buttons
        JButton okButton = new JButton("OK");
        bottomPanel.add( okButton, BorderLayout.SOUTH );

        JButton cancelButton = new JButton("Cancel");
        bottomPanel.add( cancelButton, BorderLayout.SOUTH );
        
        add( bottomPanel, BorderLayout.SOUTH );
 
        // set button actions
        okButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				filterPoints( matchedPtNames, outputIntervalList, selectionTable );

				ApplyBigwarpPlugin.runExport( data, sources, fieldOfViewOption,
						outputIntervalList, matchedPtNames, interp,
						offsetIn, resolution, isVirtual, nThreads, 
						progressWriter, true, false, null );

				frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
			}
		});

        cancelButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
			}
		});

        // build the frame
		setOpaque( true );
		frame = buildAndShowFrame();
	}
	
	public static void filterPoints(
			final List<String> matchedPtNames,
			final List<Interval> intervals,
			final PointSelectorTableModel model )
	{
		int N = model.getRowCount() - 1;
		for( int i = N; i >= 0; i-- )
		{
			if( !model.selected.get( i ))
			{
				matchedPtNames.remove( i );
				intervals.remove( i );
			}
		}
	}

	public JFrame buildAndShowFrame()
	{
		JFrame selectionFrame = new JFrame( "Point selection" );
		selectionFrame.setContentPane( this );
		selectionFrame.pack();
		selectionFrame.setVisible( true );
		return selectionFrame;
	}
	
	public JFrame getFrame()
	{
		return frame;
	}

	public boolean doExport()
	{
		return doExport;
	}

	public void genJTable()
	{
		
		table = new JTable( selectionTable );

		table.setPreferredScrollableViewportSize( new Dimension( 400, 800 ) );
		table.setFillsViewportHeight( true );
		table.setShowVerticalLines( false );
	}
	
	public static class PointSelectorTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 3826256129500169587L;

		final ArrayList< Boolean > selected;

		final ArrayList< String > names;

		public PointSelectorTableModel( final List< String > nameListIn )
		{
			this.names = new ArrayList<>();
			this.selected = new ArrayList<>();

			for( String s : nameListIn )
			{
				names.add( s );
				selected.add( true );
			}
		}
		
		@Override
		public boolean isCellEditable( int row, int col )
		{
			return ( col == 1 );
		}

		@Override
		public int getColumnCount()
		{
			return 2;
		}

		@Override
		public int getRowCount()
		{
			return names.size();
		}
		
		@Override
		public Class<?> getColumnClass( int col ){
			if( col == 0 ){
				return String.class;
			}else {
				return Boolean.class;
			}
		}

		@Override
		public String getColumnName( final int col )
		{
			if( col == 0 )
				return "Name";
			else
				return "Selected";
		}

		@Override
		public Object getValueAt( int rowIndex, int columnIndex )
		{
			if( columnIndex == 0 )
				return names.get( rowIndex );
			else
				return selected.get( rowIndex );
		}
		
		@Override
		public void setValueAt( Object value, int row, int col )
		{
			if ( col == 0 )
			{
				names.set( row, ( String ) value );
			}
			else
			{
				selected.set( row, ( Boolean ) value );
			}				
		}

	}
}
