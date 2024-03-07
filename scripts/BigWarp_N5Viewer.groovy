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
 * 2024-Mar-07 : Use new BigWarp API
 * 
 * @author John Bogovic
 */

import java.lang.Exception;
import java.io.*;
import java.util.*;

import bigwarp.*;
import bdv.cache.SharedQueue;
import bdv.export.*;
import bdv.gui.*;
import bdv.viewer.*;
import net.imglib2.*;
import net.imglib2.type.*;
import net.imglib2.type.numeric.*;

import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.ij.*;


def makeSources(
		BigWarpData bwData,
		File baseDir,
		int baseId,
		boolean isMoving,
		SharedQueue sharedQueue ) 
	throws Exception {
	String n5Path = baseDir.getAbsolutePath();
	N5Importer.N5ViewerReaderFun n5fun = new N5Importer.N5ViewerReaderFun();
	N5Reader n5 = n5fun.apply(n5Path);
	String dataset = new N5Importer.N5BasePathFun().apply(n5Path);
	
	Source src = BigWarpInit.loadN5Source(n5, dataset, sharedQueue);
	BigWarpInit.add( bwData, BigWarpInit.createSources(bwData, src, baseId, isMoving));
	return Collections.singletonList(src);
}

int procs = ij.Prefs.getThreads();
procs = ( procs < 1 ) ? 1 : procs;


BigWarpData bwData = BigWarpInit.initData();
SharedQueue sharedQueue = new SharedQueue( procs );


// build moving sourcesSwitch to N5Viewer style loading
try {
	makeSources(bwData, movingN5Base, 0, true, sharedQueue);
} catch (Exception e) {
	System.err.println("error making moving sources");
	e.printStackTrace();
}

// build target sources
try {
	makeSources(bwData, targetN5Base, bwData.numMovingSources(), false, sharedQueue);
} catch (Exception e) {
	System.err.println("error making target sources");
	e.printStackTrace();
}

try {
	BigWarpViewerOptions opts = (BigWarpViewerOptions)BigWarpViewerOptions.options().numRenderingThreads(procs);
	BigWarp bw = new BigWarp(bwData, opts, new ProgressWriterConsole());

	// load the landmark points if they exist
	if (landmarksFile != null)
		bw.getLandmarkPanel().getTableModel().load(landmarksFile);
} catch (Exception e) {
	e.printStackTrace();
}