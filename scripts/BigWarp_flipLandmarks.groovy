#@ Integer (label="Number of dimensions", value=3) ndims
#@ File (label="Input BigWarp Landmark file") inF
#@ File (label="Output BigWarp Landmark file") outF

/*
 * Exchanges moving and fixed landmarks
 * 
 * John Bogovic 
 * 
 * 14 July 2020 initial version
 */

import bigwarp.landmarks.*;

ltm = new LandmarkTableModel( ndims )
ltm.load( inF, true )
ltm.save( outF )