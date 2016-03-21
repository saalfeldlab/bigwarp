package bigwarp.scripts;

import java.io.File;
import java.io.IOException;

import bigwarp.landmarks.LandmarkTableModel;

/**
 * Inverts a set of landmark point pairs so moving points become fixex and vice versa.
 *  
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 *
 */
public class InvertWarp
{

	public static void main( String[] args ) throws IOException
	{
		int ndims = 3;
		// String pts_fn = "/groups/saalfeld/saalfeldlab/tem-light-reg/HRtemplate-2-tomo/landmarks_list";
//		String output_fn = "/groups/saalfeld/saalfeldlab/tem-light-reg/HRtemplate-2-tomo/landmarks_template-2-tomo";
		
		String pts_fn = "/groups/saalfeld/home/bogovicj/projects/wong_reg/flyc_tps/flyc_tps";
		String output_fn = "/groups/saalfeld/home/bogovicj/projects/wong_reg/flyc_tps/flyc_tps_inv";

		LandmarkTableModel ltm = new LandmarkTableModel( ndims );
		
		// read the existing point pairs 
		ltm.load( new File( pts_fn) );
		
		LandmarkTableModel ltm_inv = ltm.invert(); 
		
		// write it out
		ltm_inv.save( new File( output_fn) );
		
		System.out.println( "finished" );
		System.exit( 0 );
	}

	
}
