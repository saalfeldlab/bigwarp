package bdv.ij;

import org.janelia.utility.ui.RepeatingReleasedEventsFixer;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.DisplayMode;
import bdv.viewer.VisibilityAndGrouping;
import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import ij.CompositeImage;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.LUT;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.type.numeric.ARGBType;

/**
 * ImageJ plugin to show the current image in BigDataViewer.
 *
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public class BigWarpImagePlusPlugIn implements PlugIn
{

    private ImagePlus moving_imp;
    private ImagePlus target_imp;

	public static void main( final String[] args )
	{
//		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		new ImageJ();
		IJ.run("Boats (356K)");
		IJ.run("Boats (356K)");
		new BigWarpImagePlusPlugIn().run( null );
	}

	@Override
	public void run( final String arg )
	{
		if ( IJ.versionLessThan( "1.40" ) ) return;

        final int[] ids = WindowManager.getIDList();
        if ( ids == null || ids.length < 2 )
        {
            IJ.showMessage( "You should have at least two images open." );
            return;
        }

        // Find any open images
        final String[] titles = new String[ ids.length ];
        for ( int i = 0; i < ids.length; ++i )
        {
            titles[ i ] = ( WindowManager.getImage( ids[ i ] ) ).getTitle();
        }

        // Build a dialog to choose the moving and fixed images
        final GenericDialog gd = new GenericDialog( "Big Warp Setup" );
        gd.addMessage( "Image Selection:" );
        final String current = WindowManager.getCurrentImage().getTitle();
        gd.addChoice( "moving_image", titles, current );
        gd.addChoice( "target_image", titles, current.equals( titles[ 0 ] ) ? titles[ 1 ] : titles[ 0 ] );
        gd.showDialog();

        if (gd.wasCanceled()) return;

        moving_imp = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
        target_imp = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );

        try
        {
        	new RepeatingReleasedEventsFixer().install();
			final BigWarp bw = new BigWarp( BigWarpInit.createBigWarpDataFromImages( moving_imp, target_imp ), "Big Warp",  null );
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

	protected void transferChannelSettings( final CompositeImage ci, final SetupAssignments setupAssignments, final VisibilityAndGrouping visibility )
	{
		final int nChannels = ci.getNChannels();
		final int mode = ci.getCompositeMode();
		final boolean transferColor = mode == IJ.COMPOSITE || mode == IJ.COLOR;
		for ( int c = 0; c < nChannels; ++c )
		{
			final LUT lut = ci.getChannelLut( c + 1 );
			final ConverterSetup setup = setupAssignments.getConverterSetups().get( c );
			if ( transferColor )
				setup.setColor( new ARGBType( lut.getRGB( 255 ) ) );
			setup.setDisplayRange( (int)lut.min, (int)lut.max );
		}
		if ( mode == IJ.COMPOSITE )
		{
			final boolean[] activeChannels = ci.getActiveChannels();
			visibility.setDisplayMode( DisplayMode.FUSED );
			for ( int i = 0; i < activeChannels.length; ++i )
				visibility.setSourceActive( i, activeChannels[ i ] );
		}
		else
			visibility.setDisplayMode( DisplayMode.SINGLE );
		visibility.setCurrentSource( ci.getChannel() - 1 );
	}

	protected void transferSettingsRGB( final ImagePlus imp, final SetupAssignments setupAssignments )
	{
		final ConverterSetup setup = setupAssignments.getConverterSetups().get( 0 );
		setup.setDisplayRange( (int)imp.getDisplayRangeMin(), (int)imp.getDisplayRangeMax() );
	}
}
