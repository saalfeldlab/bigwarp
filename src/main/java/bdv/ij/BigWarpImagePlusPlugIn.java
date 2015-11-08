package bdv.ij;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.janelia.utility.ui.RepeatingReleasedEventsFixer;

import bdv.BigDataViewer;
import bdv.img.imagestack.ImageStackImageLoader;
import bdv.img.virtualstack.VirtualStackImageLoader;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.DisplayMode;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.VisibilityAndGrouping;
import bigwarp.BigWarp;
import ij.CompositeImage;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.LUT;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

/**
 * ImageJ plugin to show the current image in BigDataViewer.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class BigWarpImagePlusPlugIn implements PlugIn
{
	
    private ImagePlus moving_imp;
    private ImagePlus target_imp;
    
	public static void main( final String[] args )
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		new ImageJ();
		IJ.run("Confocal Series (2.2MB)");
//		IJ.run("Fly Brain (1MB)");
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
			BigWarp bw = new BigWarp( buildData( moving_imp, target_imp, null ), "Big Warp",  null );
			ImageJ ij = IJ.getInstance();
			bw.setImageJInstance( ij );
		} 
        catch (SpimDataException e) 
        {
			e.printStackTrace();
			return;
		}
        
	}
	
	public static BigWarp.BigWarpData buildData( ImagePlus moving_imp, ImagePlus target_imp, double[] resolutions )
	{
		if( resolutions != null )
		{
			int N = resolutions.length / 2;
			double[] res1 = new double[ N ];
			double[] res2 = new double[ N ];

			for( int i = 0; i < resolutions.length; i++ )
			{
				res1[ i ] = resolutions[ i ];
				res2[ i ] = resolutions[ i + N ];
			}
			BigWarp.BigWarpData moving_data = getSource( moving_imp, null, res1 );
			BigWarp.BigWarpData moving_and_target_data = getSource( target_imp, moving_data, res2 );
			return moving_and_target_data;
			
		}else{
			BigWarp.BigWarpData moving_data = getSource( moving_imp, null, null );
			BigWarp.BigWarpData moving_and_target_data = getSource( target_imp, moving_data, null );
			return moving_and_target_data;
		}
		
		
//		ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();
//		sources.add( moving_data.sources.get(0) );
//		sources.add( target_data.sources.get(0) );
//		
//		BigWarp.BigWarpData data = new BigWarp.BigWarpData( sources, moving_data.seq, moving_data.converterSetups );
		
		
	}
	
	public static BigWarp.BigWarpData getSource( ImagePlus imp, BigWarp.BigWarpData dataIn )
	{
		return getSource( imp, dataIn, null );
	}
	
	public static BigWarp.BigWarpData getSource( ImagePlus imp, BigWarp.BigWarpData dataIn, double[] resolutions )
	{
		
		// get calibration and image size
		final double pw;
		final double ph;
		final double pd;
		
		if( resolutions == null )
		{
			pw = imp.getCalibration().pixelWidth;
			ph = imp.getCalibration().pixelHeight;
			pd = imp.getCalibration().pixelDepth;
		}
		else
		{
			pw = resolutions[ 0 ];
			ph = resolutions[ 1 ];
			pd = resolutions[ 2 ];
		}
		
		String punit = imp.getCalibration().getUnit();
		if ( punit == null || punit.isEmpty() )
			punit = "px";
		final FinalVoxelDimensions voxelSize = new FinalVoxelDimensions( punit, pw, ph, pd );
		final int w = imp.getWidth();
		final int h = imp.getHeight();
		final int d = imp.getNSlices();
		final FinalDimensions size = new FinalDimensions( new int[] { w, h, d } );

		// create ImgLoader wrapping the image
		final BasicImgLoader imgLoader;
		if ( imp.getStack().isVirtual() )
		{
			switch ( imp.getType() )
			{
			case ImagePlus.GRAY8:
				imgLoader = VirtualStackImageLoader.createUnsignedByteInstance( imp );
				break;
			case ImagePlus.GRAY16:
				imgLoader = VirtualStackImageLoader.createUnsignedShortInstance( imp );
				break;
			case ImagePlus.GRAY32:
				imgLoader = VirtualStackImageLoader.createFloatInstance( imp );
				break;
			case ImagePlus.COLOR_RGB:
			default:
				imgLoader = VirtualStackImageLoader.createARGBInstance( imp );
				break;
			}
		}
		else
		{
			switch ( imp.getType() )
			{
			case ImagePlus.GRAY8:
				imgLoader = ImageStackImageLoader.createUnsignedByteInstance( imp );
				break;
			case ImagePlus.GRAY16:
				imgLoader = ImageStackImageLoader.createUnsignedShortInstance( imp );
				break;
			case ImagePlus.GRAY32:
				imgLoader = ImageStackImageLoader.createFloatInstance( imp );
				break;
			case ImagePlus.COLOR_RGB:
			default:
				imgLoader = ImageStackImageLoader.createARGBInstance( imp );
				break;
			}
		}

		final int numTimepoints = imp.getNFrames();
		final int numSetups = imp.getNChannels();

		// create setups from channels
		final HashMap< Integer, BasicViewSetup > setups = new HashMap< Integer, BasicViewSetup >( numSetups );
		for ( int s = 0; s < numSetups; ++s )
		{
			final BasicViewSetup setup = new BasicViewSetup( s, String.format( "channel %d", s + 1 ), size, voxelSize );
			setup.setAttribute( new Channel( s + 1 ) );
			setups.put( s, setup );
		}

		// create timepoints
		final ArrayList< TimePoint > timepoints = new ArrayList< TimePoint >( numTimepoints );
		for ( int t = 0; t < numTimepoints; ++t )
			timepoints.add( new TimePoint( t ) );
		

		// create ViewRegistrations from the images calibration
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceTransform.set( pw, 0, 0, 0, 0, ph, 0, 0, 0, 0, pd, 0 );
		final ArrayList< ViewRegistration > registrations = new ArrayList< ViewRegistration >();
		for ( int t = 0; t < numTimepoints; ++t )
			for ( int s = 0; s < numSetups; ++s )
				registrations.add( new ViewRegistration( t, s, sourceTransform ) );

		final File basePath = new File(".");
		
		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( new TimePoints( timepoints ), setups, imgLoader, null );
		final SpimDataMinimal spimData = new SpimDataMinimal( basePath, seq, new ViewRegistrations( registrations ) );
		if ( WrapBasicImgLoader.wrapImgLoaderIfNecessary( spimData ) )
		{
			System.err.println( "WARNING:\nOpening <SpimData> dataset that is not suited for interactive browsing.\nConsider resaving as HDF5 for better performance." );
		}
		
		ArrayList< SourceAndConverter< ? > > sources;
		final ArrayList< ConverterSetup > converterSetups;
		BigWarp.BigWarpData data;
		if( dataIn == null )
		{
			converterSetups = new ArrayList< ConverterSetup >();
			sources = new ArrayList< SourceAndConverter< ? > >();
			
			BigDataViewer.initSetups( spimData, converterSetups, sources );
			data = new BigWarp.BigWarpData( sources, seq, null, converterSetups );
		}
		else
		{
			converterSetups = dataIn.converterSetups;
			sources = dataIn.sources;
			
			BigDataViewer.initSetups( spimData, converterSetups, sources );
			data = new BigWarp.BigWarpData( sources, dataIn.seqP, seq, converterSetups );
		}
		
		return data;
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
