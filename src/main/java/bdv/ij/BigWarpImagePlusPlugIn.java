package bdv.ij;

import org.janelia.utility.ui.RepeatingReleasedEventsFixer;

import bdv.ij.util.ProgressWriterIJ;
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
        if ( ids == null || ids.length < 1 )
        {
            IJ.showMessage( "You should have at least one image open." );
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
        if( titles.length > 1 )
        	gd.addChoice( "target_image", titles, current.equals( titles[ 0 ] ) ? titles[ 1 ] : titles[ 0 ] );
        else 
        	gd.addChoice( "target_image", titles, titles[ 0 ] );
        gd.showDialog();

        if (gd.wasCanceled()) return;

        moving_imp = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
        target_imp = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );

        try
        {
        	new RepeatingReleasedEventsFixer().install();
			final BigWarp bw = new BigWarp( BigWarpInit.createBigWarpDataFromImages( moving_imp, target_imp ), "Big Warp",  new ProgressWriterIJ() );
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

}
