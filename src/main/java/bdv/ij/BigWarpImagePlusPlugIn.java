package bdv.ij;

import java.io.File;
import java.io.IOException;

import org.janelia.saalfeldlab.n5.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.ij.N5Factory;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.MultiscaleMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5ImagePlusMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.PhysicalMetadata;
import org.janelia.utility.ui.RepeatingReleasedEventsFixer;

import bdv.ij.util.ProgressWriterIJ;
import bdv.util.RandomAccessibleIntervalMipmapSource;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bigwarp.BigWarp;
import bigwarp.BigWarp.BigWarpData;
import bigwarp.BigWarpInit;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/**
 * ImageJ plugin to show the current image in BigDataViewer.
 *
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public class BigWarpImagePlusPlugIn implements PlugIn
{

    private ImagePlus movingIp;
    private ImagePlus targetIp;

	public static void main( final String[] args )
	{
		new ImageJ();
		IJ.run("Boats (356K)");
		new BigWarpImagePlusPlugIn().run( null );
	}

	@Override
	public void run( final String arg )
	{
		if ( IJ.versionLessThan( "1.40" ) ) return;

        final int[] ids = WindowManager.getIDList();
        if ( ids == null || ids.length < 1 )
        {
            IJ.showMessage( "You should have at least one image open." );
            return;
        }

        // Find any open images
        final int noneIndex = ids.length;
        final String[] titles = new String[ ids.length + 1 ];

        for ( int i = 0; i < ids.length; ++i )
        {
            titles[ i ] = ( WindowManager.getImage( ids[ i ] ) ).getTitle();
        }
        titles[ ids.length ] = "<None>";

        // Build a dialog to choose the moving and fixed images
        final GenericDialogPlus gd = new GenericDialogPlus( "Big Warp Setup" );

        gd.addMessage( "Image Selection:" );
        final String current = WindowManager.getCurrentImage().getTitle();
        gd.addChoice( "moving_image", titles, current );
        if( titles.length > 1 )
        	gd.addChoice( "target_image", titles, current.equals( titles[ 0 ] ) ? titles[ 1 ] : titles[ 0 ] );
        else 
        	gd.addChoice( "target_image", titles, titles[ 0 ] );

        gd.addMessage( "\nN5/Zarr/HDF5/BDV-XML" );
        gd.addDirectoryOrFileField( "Moving", "" );
        gd.addStringField( "Moving dataset", "" );
        gd.addDirectoryOrFileField( "Target", "" );
        gd.addStringField( "Target dataset", "" );

        gd.addMessage( "" );
        gd.addFileField( "Landmarks file", "" );
        gd.addCheckbox( "Apply transform from landmarks", true );

        gd.showDialog();

        if (gd.wasCanceled()) return;

		final int mvgImgIdx = gd.getNextChoiceIndex();
		final int tgtImgIdx = gd.getNextChoiceIndex();
		movingIp = mvgImgIdx < noneIndex ? WindowManager.getImage( ids[ mvgImgIdx ]) : null;
		targetIp = tgtImgIdx < noneIndex ? WindowManager.getImage( ids[ tgtImgIdx ]) : null;

		final String mvgRoot = gd.getNextString();
		final String mvgDataset = gd.getNextString();
		final String tgtRoot = gd.getNextString();
		final String tgtDataset = gd.getNextString();

		final String landmarkPath = gd.getNextString();
		final boolean applyTransform = gd.getNextBoolean();

		// build bigwarp data
		BigWarpData< ? > bigwarpdata = BigWarpInit.initData();
		int id = 0;
		if ( movingIp != null )
		{
			id += BigWarpInit.add( bigwarpdata, movingIp, id, 0, true );
		}

		if ( !mvgRoot.isEmpty() )
		{
			BigWarpInit.add( bigwarpdata, loadN5Source( mvgRoot, mvgDataset ), id, 0, true );
			id++;
		}

		if ( targetIp != null )
		{
			id += BigWarpInit.add( bigwarpdata, targetIp, id, 0, false );
		}

		if ( !tgtRoot.isEmpty() )
		{
			BigWarpInit.add( bigwarpdata, loadN5Source( tgtRoot, tgtDataset ), id, 0, false );
			id++;
		}
		bigwarpdata.wrapUp();

        // run BigWarp
        try
        {
        	new RepeatingReleasedEventsFixer().install();
			final BigWarp<?> bw = new BigWarp<>( bigwarpdata, "Big Warp",  new ProgressWriterIJ() );

			if( landmarkPath != null && !landmarkPath.isEmpty())
			{
				bw.loadLandmarks( landmarkPath );

				if ( applyTransform )
					bw.setIsMovingDisplayTransformed( applyTransform );
			}

			bw.getViewerFrameP().getViewerPanel().requestRepaint();
			bw.getViewerFrameQ().getViewerPanel().requestRepaint();
			bw.getLandmarkFrame().repaint();
		}
        catch (final SpimDataException e)
        {
			e.printStackTrace();
			return;
		}

	}

	public static String urlFromFile( final File f )
	{
		final String p = f.getPath();
		if ( p.startsWith( "http" ) || p.startsWith( "gs" ) || p.startsWith( "s3" ) )
		{
			int i = p.indexOf( ':' );
			String pre = p.substring( 0, i + 1 );
			String post = p.substring( i + 1 );

			if ( post.startsWith( "//" ) )
				return pre + post;
			else
				return pre + "/" + post;
		}
		else
			return p;
	}

	public static Source<?> loadN5Source( final String n5Root, final String n5Dataset )
	{
		final N5Reader n5;
		try
		{
			n5 = new N5Factory().openReader( n5Root );
		}
		catch ( IOException e ) { 
			e.printStackTrace();
			return null;
		}

		N5TreeNode node = new N5TreeNode( n5Dataset, false );
		try
		{
			N5DatasetDiscoverer.parseMetadataRecursive( n5, node, N5Importer.PARSERS, N5Importer.GROUP_PARSERS );
		}
		catch ( IOException e )
		{}

		N5Metadata meta = node.getMetadata();
		if( meta instanceof MultiscaleMetadata )
		{
			return openAsSourceMulti( n5, (MultiscaleMetadata<?>)meta, true );
		}
		else
		{
			return openAsSource( n5, meta, true );
		}
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static <T extends N5Metadata > Source<?> openAsSource( final N5Reader n5, final T meta, final boolean isVolatile )
	{

		final RandomAccessibleInterval imageRaw;
		final RandomAccessibleInterval image;
		try
		{
			if( isVolatile )
				imageRaw = to3d( N5Utils.openVolatile( n5, meta.getPath() ));
			else
				imageRaw = to3d( N5Utils.open( n5, meta.getPath() ));

			if( meta instanceof N5ImagePlusMetadata 
					&& ((N5ImagePlusMetadata)meta).getType() == ImagePlus.COLOR_RGB
					&& Util.getTypeFromInterval( imageRaw ) instanceof UnsignedIntType )
			{
				image = toColor( imageRaw );
			}
			else
				image = imageRaw;

			if( meta instanceof PhysicalMetadata )
			{
				return new RandomAccessibleIntervalSource( image, Util.getTypeFromInterval( image ), 
						((PhysicalMetadata)meta).physicalTransform3d(), meta.getPath() );
			}
			else
				return new RandomAccessibleIntervalSource( image, Util.getTypeFromInterval( image ), 
						new AffineTransform3D(), meta.getPath() );

		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		return null;
	}

	public static Source<?> openAsSourceMulti( final N5Reader n5, final MultiscaleMetadata<?> multiMeta, final boolean isVolatile )
	{
		final String[] paths = multiMeta.getPaths();
		final AffineTransform3D[] transforms = multiMeta.getTransforms();
		final String unit = multiMeta.units()[0];

		@SuppressWarnings( "rawtypes" )
		final RandomAccessibleInterval[] images = new RandomAccessibleInterval[paths.length];
		final double[][] mipmapScales = new double[ images.length ][ 3 ];
		for ( int s = 0; s < images.length; ++s )
		{
			try
			{
				if( isVolatile )
					images[ s ] = to3d( N5Utils.openVolatile( n5, paths[s] ));
				else
					images[ s ] = to3d( N5Utils.open( n5, paths[s] ));
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}

			mipmapScales[ s ][ 0 ] = transforms[ s ].get( 0, 0 );
			mipmapScales[ s ][ 1 ] = transforms[ s ].get( 1, 1 );
			mipmapScales[ s ][ 2 ] = transforms[ s ].get( 2, 2 );
		}

		@SuppressWarnings( { "unchecked", "rawtypes" } )
		final RandomAccessibleIntervalMipmapSource source = new RandomAccessibleIntervalMipmapSource( 
				images, 
				Util.getTypeFromInterval(images[0]),
				mipmapScales,
				new mpicbg.spim.data.sequence.FinalVoxelDimensions( unit, mipmapScales[0]),
				new AffineTransform3D(),
				multiMeta.getPaths()[0] + "_group" );

		return source;
	}

	private static RandomAccessibleInterval<?> to3d( RandomAccessibleInterval<?> img )
	{
		if( img.numDimensions() == 2 )
			return Views.addDimension( img, 0, 0 );
		else
			return img;
	}

	private static RandomAccessibleInterval<ARGBType> toColor( RandomAccessibleInterval<UnsignedIntType> img )
	{
		return Converters.convertRAI( img,
				new Converter<UnsignedIntType,ARGBType>()
				{
					@Override
					public void convert( UnsignedIntType input, ARGBType output )
					{
						output.set( input.getInt() );
					}
				},
				new ARGBType() );
	}

}
