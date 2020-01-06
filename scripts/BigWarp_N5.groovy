#@ String (visibility=MESSAGE, value="<html>Run bigwarp with bigdataviewer n5 images</html>") DOCMSG
#@ File(label="Moving n5 base", required=true, style="directory" ) movingN5Base
#@ String(label="Moving dataset(s). (Comma separated)", required=true ) movingDatasetsIn
#@ File(label="Target n5 base", required=true, style="directory" ) targetN5Base
#@ String(label="Moving dataset(s). (Comma separated)", required=true ) targetDatasetsIn
#@ File(label="Landmark file", required=false, style="extensions:csv") landmarksFile

/**
 * A script for running bigwarp using image volumes stored with N5 (https://github.com/saalfeldlab/n5).
 * Requires that the contents N5 fiji update site be installed.
 * 
 * 2020-Jan-06 : Initial script
 * 
 * @author John Bogovic
 */

import java.lang.Exception;
import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import bigwarp.landmarks.LandmarkTableModel;

import bdv.util.*;
import bdv.export.*;
import bdv.viewer.*;
import net.imglib2.util.*;

import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.imglib2.*;


def makeSources( File n5Base, String[] datasets )
{
	sources = []
	for( dataset in datasets )
	{
		img = N5Utils.open( new N5FSReader( n5Base.getAbsolutePath() ), dataset );
		sources.add( new RandomAccessibleIntervalSource(img, Util.getTypeFromInterval( img ), dataset ));
	}
	return sources as Source[];
}

// build moving and target sources
movingSources = makeSources( movingN5Base, movingDatasetsIn.split(",") as String[] )
targetSources = makeSources( targetN5Base, targetDatasetsIn.split(",") as String[] )

srcNames = []
movingDatasetsIn.split(",").each{ n -> srcNames.add( "moving " + n )}
targetDatasetsIn.split(",").each{ n -> srcNames.add( "target " + n )}

bwData = BigWarpInit.createBigWarpData( movingSources, targetSources, srcNames as String[] );

try
{
    bw = new BigWarp( bwData, "bigwarp", new ProgressWriterConsole() );

    // load the landmark points if they exist
    if ( landmarksFile != null )
        bw.getLandmarkPanel().getTableModel().load( landmarksFile );
}
catch(Exception e)
{
    e.printStackTrace();
}

