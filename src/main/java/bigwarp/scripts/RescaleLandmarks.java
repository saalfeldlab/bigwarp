package bigwarp.scripts;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import bigwarp.landmarks.LandmarkTableModel;

/**
 * Scales a set of landmark points. Useful if one changes
 * the resolution of images on which landmarks were placed.
 * 
 * Inputs:
 * (input landmarks csv path)
 * (output landmarks csv path)
 * (comma separated moving landmark scales)
 * (comma separated target landmark scales)
 * 
 * Example inputs:
 * landmarks.csv
 * landmarks-scaled.csv
 * 2,2,2
 * 0.5,0.5,0.5
 * 
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 *
 */
public class RescaleLandmarks
{

	public static void main( String[] args ) throws IOException
	{
		String pts_fn = args[ 0 ];
		String output_fn = args[ 1 ];

		double[] movingScales = parseArray( args[ 2 ] );
		double[] targetScales = parseArray( args[ 3 ] );
		int ndims = movingScales.length;
		
		LandmarkTableModel ltm = new LandmarkTableModel( ndims );

		// read the existing point pairs
		ltm.load( new File( pts_fn ) );

		scaleLandmarks( ltm, movingScales, true );
		scaleLandmarks( ltm, targetScales, false );

		// write it out
		ltm.save( new File( output_fn ) );

		System.out.println( "finished" );
		System.exit( 0 );
	}

	public static void scaleLandmarks( LandmarkTableModel ltm, double[] scales, boolean isMoving )
	{
		ArrayList< Double[] > pts = ltm.getPoints( isMoving );
		for( int i = 0; i < ltm.getRowCount(); i++ )
		{
			scale( pts.get(i), scales );
		}
	}
	
	public static void scale( Double[] point, double[] scale )
	{
		for ( int i = 0; i < point.length; i++ )
		{
			point[ i ] = new Double( point[ i ].doubleValue() * scale[ i ] );
		}
	}

	public static double[] parseArray( String arrayString )
	{
		String[] strings = arrayString.split( "," );
		double[] out = new double[ strings.length ];
		for ( int i = 0; i < strings.length; i++ )
		{
			out[ i ] = Double.parseDouble( strings[ i ] );
		}
		return out;
	}

}
