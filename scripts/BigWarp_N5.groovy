#@ String (visibility=MESSAGE, value="<html>Run bigwarp with bigdataviewer n5 images</html>") DOCMSG
#@ File(label="Moving n5 base", required=true, style="directory" ) movingN5Base
#@ String(label="Moving dataset(s). (Comma separated)", required=true ) movingDatasetsIn
#@ File(label="Target n5 base", required=true, style="directory" ) targetN5Base
#@ String(label="Moving dataset(s). (Comma separated)", required=true ) targetDatasetsIn
#@ File(label="Landmark file", required=false, style="extensions:csv") landmarksFile
#@ Boolean(label="volatile?", value=true ) useVolatile

/**
 * A script for running bigwarp using image volumes stored with N5 (https://github.com/saalfeldlab/n5).
 * Requires that the contents N5 fiji update site be installed.
 * 
 * 2020-Jan-06 : Initial script
 * 2020-Jan-07 : Add pyramid support
 * 
 * @author John Bogovic
 */

import java.lang.Exception;
import java.util.regex.Pattern;

import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import bigwarp.landmarks.LandmarkTableModel;

import bdv.util.*;
import bdv.export.*;
import bdv.viewer.*;

import net.imglib2.util.*;

import mpicbg.spim.data.sequence.*;

import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.imglib2.*;

def isPyramid( N5FSReader n5, String group )
{
	/*
	 * return true if there exists at least one dataset under the group
	 * of the form "s#" where # is a number
	 */
	def pattern = Pattern.compile( /s\d/ );

	numMatching = 0;
	subDatasets = n5.list( group );
	for( d in subDatasets )
	{
		numMatching += (d =~ pattern).size()
	}
	return (numMatching >= 1);
}

def makeSources( File n5Base, String[] datasets )
{
	n5 = new N5FSReader( n5Base.getAbsolutePath() );

	sources = [];
	for( dataset in datasets )
	{
		if( isPyramid( n5, dataset ))
		{
			println( "loading pyramid from : " + dataset );
			imgsAndResolutions = N5Utils.openMipmaps( n5, dataset, useVolatile );
			nd = imgsAndResolutions.getA()[ 0 ].numDimensions();

			sources.add( new RandomAccessibleIntervalMipmapSource( 
					imgsAndResolutions.getA(), 
					Util.getTypeFromInterval( imgsAndResolutions.getA()[0] ), 
					imgsAndResolutions.getB(), 
					new FinalVoxelDimensions( "pixel", res = (0..<nd).collect{ x -> 1 } as double[] ),
					dataset ));
		}
		else
		{
			println( "loading dataset from: " + dataset );
			img = N5Utils.open( n5, dataset );
			sources.add( new RandomAccessibleIntervalSource(img, Util.getTypeFromInterval( img ), dataset ));
		}
	}
	return sources as Source[];
}


// build moving and target sources
movingSources = makeSources( movingN5Base, movingDatasetsIn.split(",") as String[] )
targetSources = makeSources( targetN5Base, targetDatasetsIn.split(",") as String[] )

srcNames = []
movingDatasetsIn.split(",").each{ n -> srcNames.add( n )}
targetDatasetsIn.split(",").each{ n -> srcNames.add( n )}

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
