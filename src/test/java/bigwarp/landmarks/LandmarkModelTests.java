package bigwarp.landmarks;

import java.io.File;
import java.io.IOException;

import javax.swing.JTable;

import junit.framework.TestCase;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Test;

import bdv.gui.BigWarpLandmarkPanel;
import bigwarp.BigWarp;
import bigwarp.landmarks.LandmarkTableModel;

public class LandmarkModelTests extends TestCase
{
	private BigWarpLandmarkPanel panel;

	private JTable table;

	private LandmarkTableModel ltm;

	public final Logger logger = LogManager.getLogger( LandmarkModelTests.class.getName() );

	@Override
	protected void setUp()
	{
		final String fnLandmarks = "src/main/resources/testPoints.csv";
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
		assertEquals(" complete table " , nr, ltm.getNextRow( true ));
		assertEquals(" complete table " , nr, ltm.getNextRow( false ));
		assertEquals( "initial selection ", -1, table.getSelectedRow() );

		// add a moving point
		ltm.add( new double[]{ 0.0, 0.0 },  true );
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
		ltm.pointEdit( k, new double[]{ 2.0, 2.0 }, false, true, false, true );
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
		ltm.pointEdit( k, new double[]{ 3.0, 3.0 }, false, true, false, true );
		ltm.updateNextRows( k );
		logger.info( ltm.getNextRow( true ) );
		assertEquals(" nextRowP inc wrap ", 5, ltm.getNextRow( true ));

	}

	@Override
	protected void tearDown()
	{
		ltm = null;
		table = null;
		panel = null;
	}
}
