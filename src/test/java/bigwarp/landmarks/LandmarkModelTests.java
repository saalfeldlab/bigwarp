package bigwarp.landmarks;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Test;

import bigwarp.landmarks.LandmarkTableModel;

public class LandmarkModelTests extends TestCase
{
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
	}

	@Test
	public void testNextRow()
	{
		logger.info( ltm.getNextRow( true ) );
		logger.info( ltm.getNextRow( false ) );

		// if we have a complete table
		int nr = ltm.getRowCount(); // num rows ( and the index to append )
		assertEquals(" complete table " , nr, ltm.getNextRow( true ));
		assertEquals(" complete table " , nr, ltm.getNextRow( false ));

		// add a moving point
		ltm.add( new double[]{ 0.0, 0.0 },  true );
		nr = ltm.getRowCount(); // num rows
		assertEquals(" 1 empty target point " , nr, ltm.getNextRow( true ));
		assertEquals(" 1 empty target point " , nr-1, ltm.getNextRow( false ));
		
		// delete row 6
		ltm.deleteRow( ltm.getRowCount() - 1 );
		
		// add a target point
		ltm.add( new double[]{ 0.0, 0.0 },  false );
		nr = ltm.getRowCount(); // num rows
		assertEquals(" 1 empty target point " , nr-1, ltm.getNextRow( true ));
		assertEquals(" 1 empty target point " , nr, ltm.getNextRow( false ));
		
		// add two more moving points
		ltm.add( new double[]{ 2.0, 2.0 },  false ); // 6
		ltm.add( new double[]{ 3.0, 3.0 },  false ); // 7 
		nr = ltm.getRowCount(); // num rows

		// set the next point to be in the middle, make sure nextPointP is incremented correctly
		int k = 6;
		ltm.setNextRow( true, k );
		ltm.pointEdit( k, new double[]{ 2.0, 2.0 }, false, true, false, true );
		ltm.updateNextRows( k );
		logger.info( ltm.getNextRow( true ) );
		assertEquals(" nextRowP inc " , 7, ltm.getNextRow( true ));
		
		// test wrapping 
		k = ltm.getNextRow( true );
		logger.info( "k: " + k );
		ltm.pointEdit( k, new double[]{ 3.0, 3.0 }, false, true, false, true );
		ltm.updateNextRows( k );
		logger.info( ltm.getNextRow( true ) );
		assertEquals(" nextRowP inc wrap ", 5, ltm.getNextRow( true ));
		
//		logger.info( ltm.getRowCount() );
//		logger.info( ltm.nextRow( false ) );
	}

	@Override
	protected void tearDown()
	{
		ltm = null;
	}
}
