#@ StatusService status
#@ File (label="Input moving points (plain csv)") mvgCsvF
#@ File (label="Input target points (plain csv)") tgtCsvF
#@ File (label="Output BigWarp Landmark file") landmarksF

/*
 * Converts two csv files to a bigwarp landmark correspondence csv file.
 * 
 * John Bogovic 
 * 
 * 27 March 2020 initial version
 */

import ij.IJ;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import bigwarp.landmarks.*;

def parse( row )
{
    pt = []
    println( row );
    row.split(",").each{ pt.add( Double.parseDouble( it )) }
    return pt as double[]
}

def check( pt1, pt2 )
{
	if( pt1.length != pt2.length )
	{
		status.warn("csv files have different numbers of columns. exiting.");
		return false;
	}
	return true;
}


/* Read the csvs */
List< String > mvgLines;
List< String > tgtLines;
try
{
	mvgLines = Files.readAllLines( Paths.get( mvgCsvF.getAbsolutePath() ) );
	tgtLines = Files.readAllLines( Paths.get( tgtCsvF.getAbsolutePath() ) );
}
catch ( IOException e )
{
	e.printStackTrace();
	return;
}

/* check input sizes */
if( mvgLines.size() != tgtLines.size())
{
    status.warn("csv files have different numbers of lines. exiting.");
    return;
}

N = mvgLines.size();
ltm = null;
for( int i = 0; i < N; i++ )
{
    mvgPt = parse( mvgLines.get( i ));
    tgtPt = parse( tgtLines.get( i ));
    if( i == 0 )
    {
		if( !check(mvgPt, tgtPt))
			return;

		/** init the bw landmarks */
		ltm = new LandmarkTableModel( mvgPt.length );
    }

    ltm.add( mvgPt, true );
    ltm.setPoint( i, false, tgtPt );
}

/** write the bw landmarks */
try
{
    ltm.save( landmarksF );
}
catch ( IOException e )
{
    e.printStackTrace();
    return;
}
println("all done");