#@ File (label="Landmark file") landmarksPath
#@ Integer (label="Number of dimensions", value=3) nd
#@ Boolean (label="As transformation matrix", value=true) asXfmMtx

import bigwarp.landmarks.LandmarkTableModel;

/**
 * Prints the affine part of the transformation defined by the given bigwarp landmarks stored
 * in the passed csv 
 */

def printArray( x ){
	(0..<nd).each{ println( x[it].join( ' '))} 
}

ltm = new LandmarkTableModel( nd );
try
{
	ltm.load( landmarksPath );
} catch ( IOException e )
{
	e.printStackTrace();
	return;
}

// the affine this returns is not the usual transformation matrix,
// but rather a "displacement matrix, i.e:
// 		y = x + Ax
affine = ltm.getTransform().getAffine();


// if we want a the usual transformation matrix, just add in the identity
// 		y = (I + A)x
if( asXfmMtx )
	(0..<nd).each{ affine[it][it] = 1 + affine[it][it] }

printArray( affine )