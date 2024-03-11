#@ File (label="Landmark file") landmarksPath
#@ File (label="Input points (csv)") inCsv
#@ File (label="Output points (csv)") outCsv
#@ String ( label="Direction", choices={"Moving to target", "Target to moving"}, style="radioButtonHorizontal") inverseOrForward
#@ String (label="Transformation type", choices={"Thin Plate Spline", "Affine", "Similarity", "Rotation", "Translation" }) transformType
#@ Double (label="Inverse tolerance", value=0.1 ) invTolerance
#@ Integer (label="Inverse maximum iterations", value=200) invMaxIters
#@ Boolean (label="Csv has header row?", value=false) csvHasHeader
#@ Double (label="x coordinate scale", value=1.0 ) sx
#@ Double (label="y coordinate scale", value=1.0 ) sy
#@ Double (label="z coordinate scale", value=1.0 ) sz

import java.io.*;
import java.nio.file.*;
import java.util.*;
import bigwarp.landmarks.*;
import bigwarp.transforms.*;
import net.imglib2.realtransform.*;
import net.imglib2.realtransform.inverse.*;


def buildTransform( File landmarksPath, String transformType, int nd, boolean needInverse, double invTolerance, int maxIters )
{

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

	bwTransform = new BigWarpTransform( ltm, transformType );
	xfm = bwTransform.getTransformation();

    if( xfm instanceof Wrapped2DTransformAs3D )
        xfm = ((Wrapped2DTransformAs3D)xfm).getTransform();

	if( needInverse )
    {
        if( transformType.equals( "Thin Plate Spline" ))
        {
            xfm.getOptimzer().setMaxIters( maxIters );
            xfm.getOptimzer().setTolerance( invTolerance );
        }
		xfm = xfm.inverse();
    }

    return xfm;
}

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

// get the transformation to apply
int nd = lines.get( 0 ).split(",").length;
transform = buildTransform( landmarksPath, transformType, nd, needInverseTransform, invTolerance, invMaxIters );

// transform all points
outputLines = [];
result = new double[ nd ];
boolean firstLine = true;
for( l in lines )
{
	// add the first line to the output if it's a header
	if( firstLine && csvHasHeader )
	{
		outputLines.add( l );
		firstLine = false;
		continue;
	}
	
	// parse line
	pt = l.split(",").collect { s -> Double.parseDouble(s) };
	scale = [ sx, sy, sz ] as double[];


	// elementwise multiplication of pt and scale
	scaledpt = [pt, scale].transpose().collect{ it[0] * it[1]};

	// transform point
	try {
		transform.apply( scaledpt as double[], result );
	} catch( Exception e ) {
		System.err.println("Warning: failed to transform " + Arrays.toString( scaledpt ));
		Arrays.fill(result, Double.NaN);
	}

	outputLines.add( result.collect{ x -> Double.toString(x)}.join(","));
}

// write output
try
{
	Files.write( Paths.get( outCsv.getAbsolutePath() ), outputLines );
}
catch ( IOException e )
{
	e.printStackTrace();
}

