package bdv.ij;

import java.io.File;
import java.io.IOException;

import bigwarp.BigWarp;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import bigwarp.transforms.NgffTransformations;
import bigwarp.transforms.WrappedCoordinateTransform;
import fiji.util.gui.GenericDialogPlus;
import ij.ImageJ;
import ij.plugin.PlugIn;
import mpicbg.models.Model;
import net.imglib2.realtransform.AffineGet;

public class BigWarpTransformExportPlugin implements PlugIn
{
	private boolean promptLandmarks = true;

	private BigWarpTransform bwTransform;

	public static void main( final String[] args )
	{
		new ImageJ();
		new BigWarpTransformExportPlugin().run( null );
	}

	public void runFromBigWarpInstance( final BigWarp< ? > bw )
	{
		promptLandmarks = false;
		bwTransform = bw.getBwTransform();
		run( null );
	}

	@Override
	public void run( final String arg )
	{
		// TODO deal with macro recordability

		final GenericDialogPlus gd = new GenericDialogPlus( "Export BigWarp Transformation" );
		gd.addMessage( "Transformation export:" );
		if ( promptLandmarks )
		{
			gd.addFileField( "landmarks_file", "" );
		}

		gd.addChoice( "Transform type",
				new String[] {
					BigWarpTransform.AFFINE,
					BigWarpTransform.SIMILARITY,
					BigWarpTransform.ROTATION,
					BigWarpTransform.TRANSLATION },
				BigWarpTransform.AFFINE );

		gd.addFileField( "Output json file or n5", "" );
		gd.addStringField( "N5 dataset", "" );
		gd.addStringField( "N5 attribute name", "" );

		gd.showDialog();
		if ( gd.wasCanceled() )
			return;

		String landmarksPath = null;
		if ( promptLandmarks )
		{
			landmarksPath = gd.getNextString();
		}

		final String transformTypeOption = gd.getNextChoice();
		final String fileOrN5Root = gd.getNextString();
		final String n5Dataset = gd.getNextString();
		final String n5Attr = gd.getNextString();

		if ( bwTransform == null )
		{
			try
			{
				final LandmarkTableModel ltm = LandmarkTableModel.loadFromCsv( new File( landmarksPath ), false );
				bwTransform = new BigWarpTransform( ltm, transformTypeOption );
			}
			catch ( final IOException e )
			{
				e.printStackTrace();
				return;
			}
		}

		String url = fileOrN5Root;
		if ( !n5Dataset.isEmpty() )
			url = url + "?" + n5Dataset;

		if ( !n5Attr.isEmpty() )
			url = url + "#" + n5Attr;

		final WrappedCoordinateTransform wct = ( WrappedCoordinateTransform ) bwTransform.getTransformation( false );
		final AffineGet affine = bwTransform.toImglib2( ( Model< ? > ) wct.getTransform() );
		NgffTransformations.save( url, NgffTransformations.createAffine( affine ) );
	}

}
