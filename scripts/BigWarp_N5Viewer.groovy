#@ String (visibility=MESSAGE, value="<html>Run bigwarp with bigdataviewer n5 images</html>") DOCMSG
#@ File(label="Moving n5 base", required=true, style="directory" ) movingN5Base
#@ File(label="Target n5 base", required=true, style="directory" ) targetN5Base
#@ File(label="Landmark file", required=false, style="extensions:csv") landmarksFile

/**
 * A script for running bigwarp using image volumes stored with N5 (https://github.com/saalfeldlab/n5).
 * Requires that the contents N5 fiji update site be installed. Derived
 * from BigWarp_N5.groovy.
 * 
 * 2020-Jan-06 : Initial script
 * 2020-Jan-07 : Add pyramid support
 * 2020-Feb-13 : Switch to N5Viewer style loading
 * 
 * @author John Bogovic
 */

import java.lang.Exception;
import java.util.regex.Pattern;

import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import bigwarp.landmarks.LandmarkTableModel;

import bdv.util.*;
import bdv.util.volatiles.*;
import bdv.export.*;
import bdv.viewer.*;


import net.imglib2.util.*;

import mpicbg.spim.data.sequence.*;

import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.imglib2.*;
import org.janelia.saalfeldlab.n5.bdv.*;
import org.janelia.saalfeldlab.n5.bdv.dataaccess.*;

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


def makeSourcesN5Viewer( File n5Base, SharedQueue sharedQueue )
{
	try
	{
		dataAccessFactory = new DataAccessFactory( DataAccessType.FILESYSTEM );
	}
	catch ( final DataAccessException e )
	{
		return null;
	}

	n5Path = n5Base.getAbsolutePath();
	final N5Reader n5 = dataAccessFactory.createN5Reader( n5Path );
	final N5ExportMetadataReader metadata = N5ExportMetadata.openForReading( n5 );
	
	final int numChannels = metadata.getNumChannels();
	
	final String displayName = metadata.getName() != null ? metadata.getName() : "";
	final int numTimepoints = 1;
	Prefs.showScaleBar( true );

	
	sources = [];
	for ( int c = 0; c < numChannels; ++c )
	{
		final Source<?> volatileSource = N5MultiscaleSource.getVolatileSource( n5, c, displayName, sharedQueue );
		sources.add( volatileSource );
	}
	return sources as Source[];
}

procs = Runtime.getRuntime().availableProcessors() / 2;
if( procs < 1 )
	procs = 1;

sharedQueue = new SharedQueue( (int) procs );

// build moving and target sources
movingSources = makeSourcesN5Viewer( movingN5Base, sharedQueue );
targetSources = makeSourcesN5Viewer( targetN5Base, sharedQueue );

srcNames = []
movingSources.each{ x -> srcNames.add( x.getName() )};
targetSources.each{ x -> srcNames.add( x.getName() )};

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
