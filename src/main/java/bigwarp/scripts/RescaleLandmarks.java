package bigwarp.scripts;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import bigwarp.landmarks.LandmarkTableModel;

/**
 * Scales a set of landmark points.
 * Both landmark and target points were 
 * 
 * Inputs:
 * (space separated scales( (input path) (output path)
 * 
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 *
 */
public class RescaleLandmarks
{

	public static void main( String[] args ) throws IOException
	{
		double[] scales = parseArray( args[0] );
		int ndims = scales.length;
		String pts_fn = args[1];
		String output_fn = args[2];

		LandmarkTableModel ltm = new LandmarkTableModel( ndims );
		
		// read the existing point pairs 
		ltm.load( new File( pts_fn) );
		
		ArrayList< Double[] > movingPts = ltm.getPoints( true );
		ArrayList< Double[] > targetPts = ltm.getPoints( false );
		for( int i = 0; i < ltm.getRowCount(); i++ )
		{
			
			scale( movingPts.get(i), scales );
			scale( targetPts.get(i), scales );
		}
		
		// write it out
		ltm.save( new File( output_fn) );
		
		System.out.println( "finished" );
		System.exit( 0 );
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
