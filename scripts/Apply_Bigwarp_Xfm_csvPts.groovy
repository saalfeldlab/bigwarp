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
import bigwarp.BigWarp.WrappedCoordinateTransform;

import mpicbg.models.*;
import bdv.gui.TransformTypeSelectDialog;

def getModel3D( final String transformType )
{
	switch( transformType ){
	case TransformTypeSelectDialog.AFFINE:
		return new AffineModel3D();
	case TransformTypeSelectDialog.SIMILARITY:
		return new SimilarityModel3D();
	case TransformTypeSelectDialog.ROTATION:
		return new RigidModel3D();
	case TransformTypeSelectDialog.TRANSLATION:
		return new TranslationModel3D();
	}
	return null;
}

def AbstractAffineModel2D getModel2D( final String transformType)
{
	switch( transformType ){
	case TransformTypeSelectDialog.AFFINE:
		return new AffineModel2D();
	case TransformTypeSelectDialog.SIMILARITY:
		return new SimilarityModel2D();
	case TransformTypeSelectDialog.ROTATION:
		return new RigidModel2D();
	case TransformTypeSelectDialog.TRANSLATION:
		return new TranslationModel2D();
	}
	return null;
}

def fitTransform( Model model, LandmarkTableModel tableModel)
{

	int numActive = tableModel.numActive();
	int ndims = tableModel.getNumdims();
	
	double[][] mvgPts = new double[ ndims ][ numActive ];
	double[][] tgtPts = new double[ ndims ][ numActive ];
	
	tableModel.copyLandmarks( mvgPts, tgtPts );
	
	double[] w = new double[ numActive ];
	Arrays.fill( w, 1.0 );
	
	try {
		model.fit( mvgPts, tgtPts, w );
	} catch (NotEnoughDataPointsException e) {
		e.printStackTrace();
	} catch (IllDefinedDataPointsException e) {
		e.printStackTrace();
	}
		
}

def buildTransform( File landmarksPath, String transformType, int nd )
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
		
	if( transformType.equals( TransformTypeSelectDialog.TPS ))
	{
		return ltm.getTransform();
	}
	else
	{
		// get the appropriate transformation model
		model = null;
		if( nd == 2 )
		{
			model = getModel2D( transformType );
		}
		else if( nd == 3 )
		{
			model = getModel3D( transformType );
		}
		else
		{
			return null;
		}
		fitTransform( model, ltm );
		invXfm = new WrappedCoordinateTransform( (InvertibleCoordinateTransform) model, nd ).inverse()
		return invXfm;
	}
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
transform = buildTransform( landmarksPath, transformType, nd )

// transform all points
outputLines = []
result = new double[ nd ];
boolean firstLine = true
for( l in lines )
{
	// add the first line to the output if it's a header
	if( firstLine && csvHasHeader )
	{
		outputLines.add( l )
		firstLine = false
		continue
	}
	
	// parse line
	pt = l.split(",").collect { s -> Double.parseDouble(s) }
	scale = [ sx, sy, sz ] as double[]

	// elementwise multiplication of pt and scale
	scaledpt = [pt, scale].transpose().collect{ it[0] * it[1]}

	// transform point
	if( needInverseTransform )
	{
		err = transform.inverseTol( scaledpt as double[], result, invTolerance, invMaxIters )
	}
	else
	{
		transform.apply( scaledpt as double[], result )
	}
	
	outputLines.add( result.collect{ x -> Double.toString(x)}.join(","))
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
