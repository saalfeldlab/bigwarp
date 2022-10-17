#@ File (label="Landmark file") landmarksPath
#@ Integer (label="Number of dimensions", value=3) nd
#@ String (label="Transformation type", choices={"Thin Plate Spline", "Affine", "Similarity", "Rotation", "Translation" }) transformType

import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;

def prettyPrintAffine( affine, nd )
{
	StringBuffer sb = new StringBuffer();
	for( int r = 0; r < nd; r++ ){
		for( int c = 0; c < nd+1; c++ ){
			sb.append( affine.get( r, c ));
			sb.append( "\t" );
		}
		sb.append( "\n" );
	}
	println( sb );
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

bwt = new BigWarpTransform( ltm, transformType );
prettyPrintAffine( bwt.affine3d(), nd );
	