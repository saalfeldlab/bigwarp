#@ File (label="Landmark file") landmarksPath
#@ File (label="Input points (csv)") inCsv
#@ File (label="Output points (csv)") outCsv
#@ String ( label="Direction", choices={"Moving to target", "Target to moving"}, style="radioButtonHorizontal") inverseOrForward
#@ Double (label="Inverse tolerance", value=0.1 ) invTolerance
#@ Integer (label="Inverse maximum iterations", value=200) invMaxIters

import java.io.*;
import java.nio.file.*;
import java.util.*;
import bigwarp.landmarks.*;

needInverseTransform = inverseOrForward.equals("Moving to target")

// read the input points
List< String > lines;
try
{
	lines = Files.readAllLines( Paths.get( inCsv.getAbsolutePath() ) );
}
catch ( IOException e )
{
	e.printStackTrace();
	return null;
}

int nd = lines.get( 0 ).split(",").length;

// load the transform
ltm = new LandmarkTableModel( nd );
try
{
	ltm.load( landmarksPath );
}
catch ( IOException e )
{
	e.printStackTrace();
	return;
}

// transform all points
transform = ltm.getTransform();
result = new double[ nd ];

outputLines = []
for( l in lines )
{
	// parse line
	pt = l.split(",").collect { s -> Double.parseDouble(s) }

	// transform point
	if( needInverseTransform )
	{
		err = transform.inverseTol( pt as double[], result, invTolerance, invMaxIters )
	}
	else
	{
		transform.apply( pt as double[], result )
	}
	
	outputLines.add( result.collect{ x -> Double.toString(x)}.join(","))
}

println( outputLines )

// write output
try
{
	Files.write( Paths.get( outCsv.getAbsolutePath() ), outputLines );
}
catch ( IOException e )
{
	e.printStackTrace();
}
