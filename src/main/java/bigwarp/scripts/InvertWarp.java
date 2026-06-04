/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2025 Howard Hughes Medical Institute.
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
