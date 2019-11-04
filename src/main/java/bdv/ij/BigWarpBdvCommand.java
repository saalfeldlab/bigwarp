package bdv.ij;

import bdv.ij.util.ProgressWriterIJ;
import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import org.janelia.utility.ui.RepeatingReleasedEventsFixer;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

/**
 * ImageJ command to register two BDV/XML images.
 *
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 * @author Christian Tischer&lt;christian.tischer@embl.de&gt;
 */
@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>Big Warp XML/HDF5" )
public class BigWarpBdvCommand implements Command
{
	@Parameter ( label = "Fixed image file [ xml/hdf5 ]" )
	public File fixedImageFile;

	@Parameter ( label = "Moving image file [ xml/hdf5 ]" )
	public File movingImageFile;

	private ImagePlus moving_imp;
    private ImagePlus target_imp;

	public static void main( final String[] args )
	{
		final BigWarpBdvCommand command = new BigWarpBdvCommand();
		command.fixedImageFile = new File( Class.class.getResource( "mri-stack.xml" ).getFile() );
		command.movingImageFile = new File( Class.class.getResource( "mri-stack.xml" ).getFile() );
		command.run() ;
	}

	@Override
	public void run()
	{
		try
        {
			final SpimData fixedSpimData = new XmlIoSpimData().load( fixedImageFile.getAbsolutePath() );
			final SpimData movingSpimData = new XmlIoSpimData().load( movingImageFile.getAbsolutePath() );
			new RepeatingReleasedEventsFixer().install();
			final BigWarp bw = new BigWarp( BigWarpInit.createBigWarpData( movingSpimData, fixedSpimData ), "Big Warp",  new ProgressWriterIJ() );
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
