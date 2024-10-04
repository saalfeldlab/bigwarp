#@ String (visibility=MESSAGE, value="<html>Run bigwarp with bigdataviewer n5 images</html>") DOCMSG
#@ String(label="Moving n5 base", required=true, style="directory" ) movingN5Base
#@ String(label="Moving dataset(s). (Comma separated)", required=true ) movingDatasetsIn
#@ String(label="Target n5 base", required=true, style="directory" ) targetN5Base
#@ String(label="Target dataset(s). (Comma separated)", required=true ) targetDatasetsIn
#@ File(label="Landmark file", required=false, style="extensions:csv") landmarksFile
#@ Boolean(label="volatile?", value=true ) useVolatile

/**
 * A script for running bigwarp using image volumes stored with N5 (https://github.com/saalfeldlab/n5).
 * Requires that the contents N5 fiji update site be installed.
 * 
 * 2020-Jan-06 : Initial script
 * 2020-Jan-07 : Add pyramid support
 * 2021-Jan-19 : Use arbitrary backends, add metadata parsing
 * 
 * @author John Bogovic
 */

import java.lang.Exception;
import java.util.regex.Pattern;

import ij.*;
import ij.process.*;

import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import bigwarp.landmarks.LandmarkTableModel;

import bdv.util.*;
import bdv.export.*;
import bdv.viewer.*;
import bdv.util.volatiles.*;

import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.ij.*;
import org.janelia.saalfeldlab.n5.metadata.*;

import net.imglib2.*;
import net.imglib2.util.*;
import net.imglib2.realtransform.*;

import mpicbg.spim.data.sequence.*;

import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.bdv.*;
import org.janelia.saalfeldlab.n5.ij.*;
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

parsers = [ new N5ImagePlusMetadata( "" ),
             new N5CosemMetadata( "", null, null ),
             new N5SingleScaleMetadata(),
             new DefaultMetadata( "", -1 ) ] as N5MetadataParser[];


groupParsers = [ new N5CosemMultiScaleMetadata(),
             new N5ViewerMultiscaleMetadataParser() ] as N5GroupParser[];

def makeSources( String n5Base, String[] datasets )
{
    n5 = new N5Importer.N5ViewerReaderFun().apply( n5Base );
    num = Runtime.getRuntime().availableProcessors() / 2;
    if( num < 1 ){ num = 1;  }
 	sharedQueue = new SharedQueue( num as int );

	sources = [];
	for( dataset in datasets )
	{
		n5 = new N5Importer.N5ViewerReaderFun().apply( n5Base );

		N5MetadataParser[] parsers = new N5MetadataParser[] { new N5ImagePlusMetadata( "" ), new N5CosemMetadata( "", null, null ), new N5SingleScaleMetadata(), new DefaultMetadata( "", -1 ) };
		N5GroupParser[] groupParsers = new N5GroupParser[] { new N5CosemMultiScaleMetadata(), new N5ViewerMultiscaleMetadataParser() };

		node = new N5TreeNode( dataset, false );
		discoverer = new N5DatasetDiscoverer( groupParsers, parsers );
		try
		{
			discoverer.discoverThreads( n5, node );
			Thread.sleep( 1000 );
			discoverer.parseGroupsRecursive( node );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		catch ( InterruptedException e )
		{
			e.printStackTrace();
		}
		
		String[] datasetsToOpen;
		AffineTransform3D[] transforms;

		final N5Metadata metadata = node.getMetadata();
		print( metadata );
		if (metadata instanceof N5SingleScaleMetadata) {
			final N5SingleScaleMetadata singleScaleDataset = (N5SingleScaleMetadata) metadata;
			datasetsToOpen = new String[] {singleScaleDataset.getPath()};
			transforms = new AffineTransform3D[] {singleScaleDataset.transform};
		} else if (metadata instanceof N5CosemMultiScaleMetadata) {
			final N5CosemMultiScaleMetadata multiScaleDataset = (N5CosemMultiScaleMetadata) metadata;
			datasetsToOpen = multiScaleDataset.paths;
			transforms = multiScaleDataset.transforms;
		} else if (metadata instanceof ImageplusMetadata )
		{
			ImagePlus iptmp = new ImagePlus( "tmp", new ByteProcessor( 1, 1 ));

			datasetsToOpen = new String[]{ node.getMetadata().getPath() };
			ImageplusMetadata m = ((ImageplusMetadata)node.getMetadata());
			m.writeMetadata( ( N5Metadata ) m, iptmp );

			AffineTransform3D xfm = new AffineTransform3D();
			xfm.set( iptmp.getCalibration().pixelWidth, 0, 0 );
			xfm.set( iptmp.getCalibration().pixelHeight, 1, 1 );
			xfm.set( iptmp.getCalibration().pixelDepth, 2, 2 );
			transforms = new AffineTransform3D[] { xfm };

		} else if (metadata == null) {
			IJ.error("N5 Viewer", "Cannot open dataset where metadata is null");
			return;
		} else {
			IJ.error("N5 Viewer", "Unknown metadata type: " );
			return;
		}

		final RandomAccessibleInterval[] images = new RandomAccessibleInterval[datasetsToOpen.length];
		double[][] scales = new double[images.length][3];
		for ( int s = 0; s < images.length; ++s )
		{
			images[ s ] = N5Utils.openVolatile( n5, datasetsToOpen[s] );
			
			scales[ s ][ 0 ] = transforms[ s ].get( 0, 0 );
			scales[ s ][ 1 ] = transforms[ s ].get( 1, 1 );
			scales[ s ][ 2 ] = transforms[ s ].get( 2, 2 );
		}

		source = new RandomAccessibleIntervalMipmapSource(
				images,
				images[0].getType(),
				scales,
				new mpicbg.spim.data.sequence.FinalVoxelDimensions( "pix", 1, 1, 1),
				"source");

		if( useVolatile )
		{
			sources.add( source.asVolatile(sharedQueue) );	
		}
		else
		{
			sources.add( source );	
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
