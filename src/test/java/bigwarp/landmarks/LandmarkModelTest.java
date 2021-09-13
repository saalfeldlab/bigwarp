/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2021 Howard Hughes Medical Institute.
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
package bigwarp.landmarks;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import javax.swing.JTable;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import bdv.gui.BigWarpLandmarkPanel;
import bigwarp.BigWarp;
import bigwarp.landmarks.LandmarkTableModel;

public class LandmarkModelTest
{
	private BigWarpLandmarkPanel panel;

	private JTable table;

	private LandmarkTableModel ltm;

	public final Logger logger = LogManager.getLogger( LandmarkModelTest.class.getName() );

	@Before
	public void before()
	{
		final String fnLandmarks = "src/test/resources/testPoints.csv";
		ltm = new LandmarkTableModel( 2 );

		try
		{
			ltm.load( new File( fnLandmarks ) );
		} catch ( IOException e )
		{
			e.printStackTrace();
		}

		panel = new BigWarpLandmarkPanel( ltm );
		table = panel.getJTable();
	}

	@Test
	public void testNextRowsAndSelection()
	{
		logger.info( ltm.getNextRow( true ) );
		logger.info( ltm.getNextRow( false ) );

		// if we have a complete table
		int nr = ltm.getRowCount(); // num rows ( and the index to append )
		assertEquals(" init table size " , 5, nr );
		assertEquals(" complete table " , nr, ltm.getNextRow( true ));
		assertEquals(" complete table " , nr, ltm.getNextRow( false ));
		assertEquals( "initial selection ", -1, table.getSelectedRow() );

		// add a moving point
		ltm.add( new double[]{ 0.0, 0.0 }, true );
		BigWarp.updateRowSelection( ltm, table, true, nr );
		nr = ltm.getRowCount(); // num rows
		assertEquals(" 1 empty target point " , nr, ltm.getNextRow( true ));
		assertEquals(" 1 empty target point " , nr-1, ltm.getNextRow( false ));
		assertEquals( "selection after add", nr-1, table.getSelectedRow() );
		
		// delete row 6
		// test that the next addition adds a new row for both moving and target
		int k = ltm.getRowCount() - 1;
		ltm.deleteRow( ltm.getRowCount() - 1 );
		BigWarp.updateRowSelection( ltm, table, true, k );
		nr = ltm.getRowCount(); // num rows
		assertEquals("after delete moving", nr, ltm.getNextRow( true ));
		assertEquals("after delete target", nr, ltm.getNextRow( false ));
		
		// add a target point
		ltm.add( new double[]{ 0.0, 0.0 },  false );
		nr = ltm.getRowCount(); // num rows
		assertEquals(" 1 empty target point " , nr-1, ltm.getNextRow( true ));
		assertEquals(" 1 empty target point " , nr, ltm.getNextRow( false ));
		
		// add two more target points
		ltm.add( new double[]{ 2.0, 2.0 },  false ); // 6
		BigWarp.updateRowSelection( ltm, table, true, 6 );
		ltm.add( new double[]{ 3.0, 3.0 },  false ); // 7
		BigWarp.updateRowSelection( ltm, table, true, 7 );
		nr = ltm.getRowCount(); // num rows
		assertEquals( "selection after two adds", nr-1, table.getSelectedRow() );

		// set the next point to be in the middle, make sure nextPointP is incremented correctly
		k = 6;
		ltm.setNextRow( true, k );
		ltm.pointEdit( k, new double[]{ 2.0, 2.0 }, false, true, false, true, null );
		ltm.updateNextRows( k );
		assertEquals(" nextRowP inc " , 7, ltm.getNextRow( true ));
		
		// repeat the above but test that changing the table selection also
		// updates the next point in the ltm
		ltm.deleteRow( k );
		ltm.add( new double[]{ 2.0, 2.0 },  false ); // 7
//		logger.trace("HERE");
//		table.getSelectionModel().setSelectionInterval( 6, 6 );
//		table.setRowSelectionInterval( 6, 6 );
//		try
//		{
//			Thread.sleep( 300 );
//		}catch( Exception e ){ e.printStackTrace(); }
//		assertEquals(" nextRowP table selection ", 6, ltm.getNextRow( true ));

		// test wrapping
		k = ltm.getNextRow( true );
		logger.info( "k: " + k );
		ltm.pointEdit( k, new double[]{ 3.0, 3.0 }, false, true, false, true, null );
		ltm.updateNextRows( k );
		logger.info( ltm.getNextRow( true ) );
		assertEquals(" nextRowP inc wrap ", 5, ltm.getNextRow( true ));

	}

	@After
	public void after()
	{
		ltm = null;
		table = null;
		panel = null;
	}
}
