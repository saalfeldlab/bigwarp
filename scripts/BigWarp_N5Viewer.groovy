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
 * 2024-Mar-12 : Correctly open multichannel metadata
 * 
 * @author John Bogovic
 */

import java.lang.Exception;
import java.io.*;
import java.util.*;
import java.util.stream.*;

import bigwarp.*;
import bdv.cache.SharedQueue;
import bdv.util.BdvOptions;
import bdv.export.*;
import bdv.gui.*;
import bdv.viewer.*;
import bdv.tools.brightness.ConverterSetup;

import net.imglib2.*;
import net.imglib2.type.*;
import net.imglib2.type.numeric.*;

import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.ij.*;
import org.janelia.saalfeldlab.n5.ui.*;
import org.janelia.saalfeldlab.n5.bdv.*;
import org.janelia.saalfeldlab.n5.universe.*;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;

def makeSources(
		BigWarpData bwData,
		File baseDir,
		int baseId,
		boolean isMoving,
		SharedQueue sharedQueue ) throws Exception {

	String n5Path = baseDir.getAbsolutePath();
	N5Importer.N5ViewerReaderFun n5fun = new N5Importer.N5ViewerReaderFun();
	N5Reader n5 = n5fun.apply(n5Path);
	String dataset = new N5Importer.N5BasePathFun().apply(n5Path);
	
	N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer( n5, 
		N5DatasetDiscoverer.fromParsers( N5ViewerCreator.n5vParsers ), 
		N5DatasetDiscoverer.fromParsers( N5ViewerCreator.n5vGroupParsers ) );

	try {

		N5TreeNode node = discoverer.discoverAndParseRecursive( "" );
		Optional<N5TreeNode> opt = node.getDescendant(dataset);
		if( opt.isPresent() )
		{
			List metadataList = Collections.singletonList(opt.get().getMetadata());

			BdvOptions dummyOpts = BdvOptions.options(); // options not important in this context
			DataSelection selection = new DataSelection(n5, metadataList);
			List<ConverterSetup> converterSetups = new ArrayList<>();
			List<SourceAndConverter> sourcesAndConverters = new ArrayList<>();
			try {
				N5Viewer.buildN5Sources(n5, selection, sharedQueue, converterSetups, sourcesAndConverters, dummyOpts);
				int i = 0;
				for( SourceAndConverter sac : sourcesAndConverters ) {
					BigWarpInit.add( bwData, BigWarpInit.createSources(bwData, sac.getSpimSource(), baseId + i, isMoving));
					i++;
				}
			} catch (IOException e) {
				System.err.println("Error loading image(s) from: " + baseDir);
				return Collections.emptyList();
			}
		}
		else
		{
			System.err.println("Could not find metadata at: " + dataset);
			return Collections.emptyList();
		}
	} catch (IOException e) {
		System.err.println("Could not find metadata at: " + dataset);
		return Collections.emptyList();
	}
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