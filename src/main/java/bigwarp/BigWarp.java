package bigwarp;

import ij.IJ;
import ij.ImagePlus;

import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ActionMap;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.janelia.utility.ui.RepeatingReleasedEventsFixer;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.RenderTarget;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.ui.TransformListener;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.sequence.TimePoint;
import bdv.BigDataViewer;
import bdv.ViewerImgLoader;
import bdv.export.ProgressWriter;
import bdv.export.ProgressWriterConsole;
import bdv.gui.BigWarpLandmarkPanel;
import bdv.gui.BigWarpViewerFrame;
import bdv.ij.BigWarpImagePlusPlugIn;
import bdv.img.WarpedSource;
import bdv.img.cache.Cache;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.tools.InitializeViewerState;
import bdv.tools.brightness.BrightnessDialog;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.MinMaxGroup;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.util.KeyProperties;
import bdv.viewer.BigWarpOverlay;
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerPanel.Options;
import bdv.viewer.WarpNavigationActions;
import bdv.viewer.animate.TranslationAnimator;
import bdv.viewer.render.MultiResolutionRenderer;
import bdv.viewer.state.ViewerState;
import bigwarp.landmarks.LandmarkTableModel;


public class BigWarp {
	
	protected static final int DEFAULT_WIDTH  = 800;
	protected static final int DEFAULT_HEIGHT = 600;
	
	protected ArrayList< SourceAndConverter< ? > > sources;
	
	protected final SetupAssignments setupAssignments;
	protected final BrightnessDialog brightnessDialog;
	protected final HelpDialog helpDialog;
	
	protected final BigWarpViewerFrame viewerFrameP;
	protected final BigWarpViewerFrame viewerFrameQ;
	protected final BigWarpViewerPanel viewerP;
	protected final BigWarpViewerPanel viewerQ;
	
	protected final BigWarpLandmarkPanel landmarkPanel;
	protected final JFrame landmarkFrame;
	
	protected final BigWarpOverlay fidipOverlayP;
	protected final BigWarpOverlay fidipOverlayQ;
	
	protected double rad = 7;
	protected double LANDMARK_DOT_SIZE = 7; // diameter of dots
	
	protected RealPoint currentLandmark;	
	
	protected LandmarkTableModel landmarkModel;
	protected JTable			 landmarkTable;
	
	//protected MouseLandmarkListener landmarkClickListener;
	protected LandmarkTableListener landmarkModellistener;
	
	MouseLandmarkListener landmarkClickListenerP;
	MouseLandmarkListener landmarkClickListenerQ;
	
	MouseLandmarkTableListener landmarkTableListenerP;
	MouseLandmarkTableListener landmarkTableListenerQ;

	protected int ndims;
	// protected ThinPlateR2LogRSplineKernelTransform estimatedXfmCopy;
	

	/*
	 * landmarks are placed on clicks only if we are inLandmarkMode
	 * during the click
	 */
	protected boolean inLandmarkMode;
	
	public BigWarp( BigWarpData data, final String windowTitle, final ProgressWriter progressWriter ) throws SpimDataException
	{

		sources = data.sources;
		AbstractSequenceDescription<?, ?, ?> seq = data.seq;
		ArrayList<ConverterSetup> converterSetups = data.converterSetups;
		
		ndims = 3;
		ndims = detectNumDims();
		sources = wrapSourcesAsTransformed( sources, ndims, 0 );

		Options options = ViewerPanel.options();
		if( ndims == 2 )
		{
			options.transformEventHandlerFactory( TransformHandler3DWrapping2D.factory() );
		}
		
		// Viewer frame for the moving image
		viewerFrameP = new BigWarpViewerFrame( DEFAULT_WIDTH, DEFAULT_HEIGHT, sources, 1,
				( ( ViewerImgLoader< ?, ? > ) seq.getImgLoader() ).getCache(), options, "Fidip moving", true );
		viewerP = viewerFrameP.getViewerPanelP();

		
		// Viewer frame for the fixed image

		viewerFrameQ = new BigWarpViewerFrame( DEFAULT_WIDTH, DEFAULT_HEIGHT, sources, 1,
				( ( ViewerImgLoader< ?, ? > ) seq.getImgLoader() ).getCache(), options, "Fidip fixed", false );
		viewerQ = viewerFrameQ.getViewerPanelP();
		
		setUpViewerMenu( viewerFrameP );
		setUpViewerMenu( viewerFrameQ );
		
		/* Set up LandmarkTableModel, holds the data and 
		 * interfaces with the LandmarkPanel */
		landmarkModel = new LandmarkTableModel( ndims );
		landmarkModellistener = new LandmarkTableListener();
		landmarkModel.addTableModelListener(landmarkModellistener);
		landmarkTable = new JTable(landmarkModel);
		
		fidipOverlayP = new BigWarpOverlay( viewerP, landmarkModel );
		fidipOverlayQ = new BigWarpOverlay( viewerQ, landmarkModel );
		viewerP.addOverlay( fidipOverlayP );
		viewerQ.addOverlay( fidipOverlayQ );
		
		/* Set up landmark panel */
		landmarkPanel = new BigWarpLandmarkPanel( landmarkModel );
		landmarkPanel.setOpaque( true );
		addDefaultTableMouseListener();
		
		landmarkFrame = new JFrame( "Landmarks" );
		landmarkFrame.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
		landmarkFrame.setContentPane( landmarkPanel );
		landmarkFrame.pack();
		
		setupAssignments = new SetupAssignments( converterSetups, 0, 512 );
		if ( setupAssignments.getMinMaxGroups().size() > 0 )
		{
			final MinMaxGroup group = setupAssignments.getMinMaxGroups().get( 0 );
			for ( final ConverterSetup setup : setupAssignments.getConverterSetups() )
				setupAssignments.moveSetupToGroup( setup, group );
		}
		
		brightnessDialog = new BrightnessDialog( viewerFrameP, setupAssignments );
		helpDialog = new HelpDialog( viewerFrameP );
		
		setUpLandmarkMenus();
		
		/* Set the locations of frames */
		Point viewerFramePloc = viewerFrameP.getLocation();
		viewerFramePloc.setLocation( viewerFramePloc.x + DEFAULT_WIDTH, viewerFramePloc.y );
		viewerFrameQ.setLocation( viewerFramePloc );
		viewerFramePloc.setLocation( viewerFramePloc.x + DEFAULT_WIDTH, viewerFramePloc.y );
		landmarkFrame.setLocation( viewerFramePloc );
		
		for ( final ConverterSetup cs : converterSetups )
			if ( RealARGBColorConverterSetup.class.isInstance( cs ) )
				( ( RealARGBColorConverterSetup ) cs ).setViewer( viewerP );

		final KeyProperties keyProperties = KeyProperties.readPropertyFile();
		
		WarpNavigationActions.installActionBindings( viewerFrameP.getKeybindings(), viewerP, keyProperties, (ndims==2) );
		BigWarpActions.installActionBindings( viewerFrameP.getKeybindings(), this, keyProperties);
		
		WarpNavigationActions.installActionBindings( viewerFrameQ.getKeybindings(), viewerQ, keyProperties, (ndims==2) );
		BigWarpActions.installActionBindings( viewerFrameQ.getKeybindings(), this, keyProperties);
		
		// set2dTransformations( );
		
		landmarkClickListenerP = new MouseLandmarkListener( this.viewerP );
		landmarkClickListenerQ = new MouseLandmarkListener( this.viewerQ );
		
		// have to be safe here and use 3dim point for both 3d and 2d
		currentLandmark = new RealPoint( 3 ); 
		inLandmarkMode = false;
		setupKeyListener( );
		
		InitializeViewerState.initTransform( viewerP );
		InitializeViewerState.initTransform( viewerQ );
		
		viewerFrameP.setVisible(true);
		viewerFrameQ.setVisible(true);
		landmarkFrame.setVisible(true);
	}
	
	protected void setUpViewerMenu( BigWarpViewerFrame vframe )
	{
		final ActionMap actionMap = vframe.getKeybindings().getConcatenatedActionMap();
		System.out.println( actionMap );
		
		JMenuBar viewerMenuBar = new JMenuBar();
		
		JMenu settingsMenu    = new JMenu( "Settings" );
		viewerMenuBar.add( settingsMenu );
		
		final JMenuItem miBrightness = new JMenuItem( actionMap.get( BigWarpActions.BRIGHTNESS_SETTINGS ) );
			
		miBrightness.setText( "Brightness & Color" );
		settingsMenu.add( miBrightness );
		
		vframe.setJMenuBar( viewerMenuBar );
		
		JMenu helpMenu    = new JMenu( "Help" );
		viewerMenuBar.add( helpMenu );
		
		final JMenuItem miHelp = new JMenuItem( actionMap.get( BigWarpActions.SHOW_HELP ) );
		helpMenu.setText( "Show Help" );
		helpMenu.add( miHelp );
	}
	
	protected void setUpLandmarkMenus()
	{
		JMenuBar landmarkMenuBar = new JMenuBar();
        
		JMenu landmarkMenu    = new JMenu( "File" );
        final JMenuItem openItem = new JMenuItem("Import landmarks");
        landmarkMenu.add( openItem );
        
        final JMenuItem saveItem = new JMenuItem("Export landmarks");
        landmarkMenu.add( saveItem );

        landmarkMenu.addSeparator();
        final JMenuItem exportImageItem = new JMenuItem("Export Moving Image");
        landmarkMenu.add( exportImageItem );
        
        landmarkMenu.addSeparator();
        final JMenuItem importXfmItem = new JMenuItem("Import Transformation");
        landmarkMenu.add( importXfmItem );
        
        final JMenuItem exportXfmItem = new JMenuItem("Export Transformation");
        landmarkMenu.add( exportXfmItem );
        
        landmarkMenuBar.add( landmarkMenu );
        landmarkFrame.setJMenuBar( landmarkMenuBar );
        
        openItem.addMouseListener( new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) 
			{
				final JFileChooser fc = new JFileChooser();
				//int returnval = fc.showOpenDialog( landmarkFrame );
				fc.showOpenDialog( landmarkFrame );
				
				File file = fc.getSelectedFile();
				System.out.println("to open file: " + file );
				try 
				{
					landmarkModel.load( file );
				} 
				catch (IOException e1) 
				{
					e1.printStackTrace();
				}
				
				landmarkTable.repaint();
			}
        });
        
        saveItem.addMouseListener( new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) 
			{
				final JFileChooser fc = new JFileChooser();
				// int returnval = fc.showOpenDialog( landmarkFrame );
				fc.showOpenDialog( landmarkFrame );
				
				File file = fc.getSelectedFile();
				System.out.println("to save file: " + file );
				try 
				{
					landmarkModel.save( file );
				} 
				catch (IOException e1) 
				{
					e1.printStackTrace();
				}
				
			}
        });
        
        exportImageItem.addMouseListener( new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) 
			{
				final JFileChooser fc = new JFileChooser();
				//int returnval = fc.showOpenDialog( landmarkFrame );
				fc.showOpenDialog( landmarkFrame );
				
				File file = fc.getSelectedFile();
				
				try 
				{
					exportMovingImage( file );
				}
				catch( Exception e1 )
				{
					e1.printStackTrace();
				}
			}
        });
	
//        exportXfmItem.addMouseListener( new MouseListener(){
//
//			@Override
//			public void mouseClicked(MouseEvent arg0) {}
//			@Override
//			public void mouseEntered(MouseEvent arg0) {}
//			@Override
//			public void mouseExited(MouseEvent arg0) {}
//			@Override
//			public void mousePressed(MouseEvent arg0) {}
//
//			@Override
//			public void mouseReleased(MouseEvent arg0) 
//			{
//				final JFileChooser fc = new JFileChooser();
//				int returnval = fc.showOpenDialog( landmarkFrame );
//				File file = fc.getSelectedFile();
//				FileUtils.
//				try 
//				{
//					exportTransformation( file,  );
//				}
//				catch( Exception e1 )
//				{
//					e1.printStackTrace();
//				}
//			}
//        });
	}
	
	protected void exportMovingImage( File f ) throws IOException, InterruptedException
	{
		// Source< ? > movingSrc = sources.get( 1 ).getSpimSource();
		final RandomAccessibleInterval<?> interval = sources.get( 1 ).getSpimSource().getSource( 0, 0 );
		
		// stolen from bdv.tools.RecordMovieDialog
		class MyTarget implements RenderTarget
		{
			BufferedImage bi;

			@Override
			public BufferedImage setBufferedImage( final BufferedImage bufferedImage )
			{
				bi = bufferedImage;
				return null;
			}

			@Override
			public int getWidth()
			{
				return (int)(interval.max( 0 ) - interval.min( 0 ) + 1);
			}

			@Override
			public int getHeight()
			{
				return (int)(interval.max( 1 ) - interval.min( 1 ) + 1);
			}
		}
		
		final ViewerState renderState = viewerP.getState();
		
		int minz = (int)interval.min( 2 );
		int maxz = (int)interval.max( 2 );
		
		final AffineTransform3D viewXfm = new AffineTransform3D();
		viewXfm.identity();
		
		final MyTarget target = new MyTarget();
		// System.out.println( "target width : " + target.getWidth() );
		// System.out.println( "target height: " + target.getHeight() );
		
		final MultiResolutionRenderer renderer = new MultiResolutionRenderer( target, new PainterThread( null ), new double[] { 1 }, 0, false, 1, null, false, new Cache.Dummy() );
		
		// step through z, rendering each slice as an image and writing it to 
		for( int z = minz; z < maxz; z++ )
		{			
			viewXfm.set( -z, 2, 3 );
			renderState.setViewerTransform( viewXfm );
			renderer.requestRepaint();
			renderer.paint( renderState );

			File thiszFile = new File( String.format( "%s_z-%04d.png", f.getAbsolutePath(), z ) );
			
			System.out.println("exporting slice: " + z + " of " + (maxz - minz) );
			ImageIO.write( target.bi, "png", thiszFile );
		}
	}
	
	protected void addDefaultTableMouseListener()
	{
		landmarkTableListenerP = new MouseLandmarkTableListener( viewerP );
		landmarkTableListenerQ = new MouseLandmarkTableListener( viewerQ );
		landmarkPanel.getJTable().addMouseListener( landmarkTableListenerP );
		landmarkPanel.getJTable().addMouseListener( landmarkTableListenerQ );
	}
	
	private static ArrayList< SourceAndConverter< ? >> wrapSourcesAsTransformed( ArrayList< SourceAndConverter< ? > > sources, int ndims, int warpUsIndices )
	{
		ArrayList< SourceAndConverter< ? >> wrappedSource = new ArrayList< SourceAndConverter< ? >>();
		
		int i = 0;
		for( SourceAndConverter< ? > sac : sources )
		{
			if( i == warpUsIndices )
			{
				wrappedSource.add( wrapSourceAsTransformed( sac, "xfm_" + i, ndims  ));
			}
			else
			{
				wrappedSource.add( sac );
			}
			
			i++;
		}
		return wrappedSource;
	}
	
	private static < T > SourceAndConverter< T > wrapSourceAsTransformed( SourceAndConverter< T > src, String name, int ndims )
	{
		if( src.asVolatile() == null )
		{
			return new SourceAndConverter<T>( 
					new WarpedSource<T>( src.getSpimSource(), name ),
					src.getConverter(), null );
		}
		else
		{
			return new SourceAndConverter<T>( 
				new WarpedSource<T>( src.getSpimSource(), name ),
				src.getConverter(), wrapSourceAsTransformed( src.asVolatile(), name + "_vol", ndims ) );
		}
	}
		 
	private static SpimDataMinimal loadSpimData( String xmlPath ){
		SpimDataMinimal spimData = null;
		try {
			spimData = new XmlIoSpimDataMinimal().load( xmlPath );
			
			if ( WrapBasicImgLoader.wrapImgLoaderIfNecessary( spimData ) )
			{
				System.err.println( "WARNING:\nOpening <SpimData> dataset that is not suited for interactive browsing.\nConsider resaving as HDF5 for better performance." );
			}
			
		} catch (SpimDataException e) 
		{
			e.printStackTrace();
		}

		return spimData;
	}
	
	public void setupKeyListener( )
	{
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor( 
				new LandmarkKeyboardProcessor( ));
	}
	
	protected void restimateTransformation()
	{
		boolean isFirst = false;
		if( landmarkModel.getTransform() == null )
		{
			landmarkModel.initTransformation();
			isFirst = true;
		}
		
		// estimate the forward transformation
		landmarkModel.getTransform().solve();
		landmarkModel.resetWarpedPoints();
		
		( (WarpedSource<?>)(sources.get( 0 ).getSpimSource())).updateTransform( landmarkModel.getTransform().deepCopy() );
//		( (WarpedSource<?>)(sources.get( 0 ).asVolatile().getSpimSource())).updateTransform( landmarkModel.getTransform().deepCopy() );
		
		// display the warped version automatically if this is the first
		// time the transform was computed
		if( isFirst )
			setIsTransformed( true );
		
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
		
	}
	
	protected void setIsTransformed( boolean isTransformed )
	{
		( (WarpedSource<?>)(sources.get( 0 ).getSpimSource())).setIsTransformed( isTransformed );
//		( (WarpedSource<?>)(sources.get( 0 ).asVolatile().getSpimSource())).setIsTransformed( isTransformed );
		fidipOverlayP.setIsTransformed( isTransformed );
	}
	
	protected int detectNumDims()
	{
		// System.out.println( "ndim 0: " + sources.get( 0 ).getSpimSource().getSource( 0, 0 ).dimension( 2 ));
		
		boolean is1Src2d = sources.get( 0 ).getSpimSource().getSource( 0, 0 ).dimension( 2 ) == 1;
		boolean is2Src2d = sources.get( 1 ).getSpimSource().getSource( 0, 0 ).dimension( 2 ) == 1;
		
		int ndims = 3;
		if( is1Src2d && is2Src2d )
			ndims = 2;
		
		return ndims;
	}
	
	protected void set2dTransformations()
	{
		if( ndims == 2 )
		{
			TransformEventHandler< AffineTransform3D > handlerP = TransformHandler3DWrapping2D.factory().create( viewerP.getDisplay() );
			viewerP.getDisplay().setTransformEventHandler( handlerP );
			
			TransformEventHandler< AffineTransform3D > handlerQ = TransformHandler3DWrapping2D.factory().create( viewerQ.getDisplay() );
			viewerQ.getDisplay().setTransformEventHandler( handlerQ );
		}
		
	}
	
	
	public static void main( final String[] args )
	{
//		final String fnP = "/groups/saalfeld/home/bogovicj/dev/bdv/bigdataviewer-core/src/main/resources/bock-FAFB.xml";
		
//		final String fn = "/groups/saalfeld/home/bogovicj/dev/bdv/bigdataviewer-core/src/main/resources/bock-FAFB.xml";
//		final String fnQ = "/groups/saalfeld/home/bogovicj/dev/bdv/bigdataviewer-core/src/main/resources/light-affine.xml";
		
//		final String fnP = "/groups/saalfeld/home/bogovicj/dev/bdv/bigdataviewer-core/src/main/resources/tiffTest.xml";
//		final String fnQ = "/groups/saalfeld/home/bogovicj/dev/bdv/bigdataviewer-core/src/main/resources/tiffTest.xml";
		
//		final String fnP = "/Users/bogovicj/workspaces/bdv/bigdataviewer-core/src/main/resources/tiffTest.xml";
//		final String fnQ = "/Users/bogovicj/workspaces/bdv/bigdataviewer-core/src/main/resources/fakeoutLM.xml";
		
//		final String fnP = "/Users/bogovicj/workspaces/bdv/bdvLandmarkUi/resources/fakeout.xml";
//		final String fnQ = "/Users/bogovicj/workspaces/bdv/bdvLandmarkUi/resources/fakeoutMR.xml";

//		final String fnP = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/fakeout.xml";
//		final String fnQ = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/fakeoutMR.xml";
////		final String fnQ = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/light-brain-template.xml";
//		
//		final String fnLandmarks = "/groups/saalfeld/home/bogovicj/tests/test_bdvtps/cell2mr/pts2.lnmk";
		
//		final String fnP = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/flyc.xml";
//		final String fnQ = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/fruTemplate.xml";
		
		// A 2d example
//		final String fnP = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/dots.xml";
//		final String fnQ = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/gel.xml";
		final String fnP = "/groups/saalfeld/home/bogovicj/tests/Dot_Blot0000.png";
		final String fnQ = "/groups/saalfeld/home/bogovicj/tests/gel0000.png";
//		final String fnLandmarks = "/groups/saalfeld/home/bogovicj/tests/test_bdvtps/dotsAndGenes/dotsAndGenes";

		final String fnLandmarks = "";
		
		try
		{
			System.setProperty( "apple.laf.useScreenMenuBar", "true" );
			new RepeatingReleasedEventsFixer().install();
			
//			ArrayList< SourceAndConverter< ? >> sources = loadSourcesFromXmls( fnP, fnQ );
			
			BigWarp bw;
			if( fnP.endsWith("xml") && fnQ.endsWith("xml"))
				bw = new BigWarp( loadSourcesFromXmls( fnP, fnQ ), new File( fnP ).getName(), new ProgressWriterConsole() );
			else if( fnP.endsWith("png") && fnQ.endsWith("png") )
				bw = new BigWarp( loadSourcesFromImages( fnP, fnQ ), new File( fnP ).getName(), new ProgressWriterConsole() );
			else{
				System.err.println("Error reading files");
				return;
			}
			
			if( !fnLandmarks.isEmpty() )
			{
				bw.landmarkModel.load( new File( fnLandmarks ));
				//fidip.restimateTransformation();
				//fidip.viewerP.requestRepaint();
			}
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
		
		System.out.println("done");
	}
	
	public static BigWarpData loadSourcesFromImages( final String filenameP, final String filenameQ )
	{
		ImagePlus moving_imp = IJ.openImage( filenameP );
		ImagePlus target_imp = IJ.openImage( filenameQ );
		
		System.out.println( "moving_imp: " + moving_imp );
		System.out.println( "target_imp: " + target_imp );
		
		return BigWarpImagePlusPlugIn.buildData(moving_imp, target_imp);
	}
	
	public static BigWarpData loadSourcesFromXmls( final String xmlFilenameP, final String xmlFilenameQ )
	{
		/* Load the first source */
		final SpimDataMinimal spimData = loadSpimData( xmlFilenameP );
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		
		final ArrayList< ConverterSetup > converterSetups = new ArrayList< ConverterSetup >();
		
		ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();
		BigDataViewer.initSetups( spimData, converterSetups, sources );

		/* Load the second source */
		BigDataViewer.initSetups( loadSpimData( xmlFilenameQ ), converterSetups, sources );
		
		return new BigWarpData( sources, seq, converterSetups );
	}
	
	public static class BigWarpData
	{
		public final ArrayList< SourceAndConverter< ? >> sources;
		public final AbstractSequenceDescription< ?, ?, ? > seq;
		public final ArrayList< ConverterSetup > converterSetups;
		
		public BigWarpData( ArrayList< SourceAndConverter< ? >> sources,
				AbstractSequenceDescription< ?, ?, ? > seq, ArrayList< ConverterSetup > converterSetups )
		{
			this.sources = sources;
			this.seq = seq;
			this.converterSetups = converterSetups;
		}
	}
	
	public static BigWarpViewerFrame frameFromXml( String xmlFilePath ) throws SpimDataException{
		final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( xmlFilePath );
		if ( WrapBasicImgLoader.wrapImgLoaderIfNecessary( spimData ) )
		{
			System.err.println( "WARNING:\nOpening <SpimData> dataset that is not suited for interactive browsing.\nConsider resaving as HDF5 for better performance." );
		}
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();

		final ArrayList< ConverterSetup > converterSetups = new ArrayList< ConverterSetup >();
		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();
		BigDataViewer.initSetups( spimData, converterSetups, sources );

		final List< TimePoint > timepoints = seq.getTimePoints().getTimePointsOrdered();

		BigWarpViewerFrame viewerFrame = new BigWarpViewerFrame( DEFAULT_WIDTH, DEFAULT_HEIGHT, sources, timepoints.size(),
				( ( ViewerImgLoader< ?, ? > ) seq.getImgLoader() ).getCache(), "Fidip", true );

		return viewerFrame;
	}
	
	protected class DummyTransformEventHandler implements TransformEventHandler<AffineTransform3D>
	{

		@Override
		public AffineTransform3D getTransform() {
			return null;
		}

		@Override
		public void setTransform(AffineTransform3D transform) {}

		@Override
		public void setCanvasSize(int width, int height, boolean updateTransform) {}

		@Override
		public void setTransformListener(
				TransformListener<AffineTransform3D> transformListener) {	
		}

		@Override
		public String getHelpString() {
			return null;
		}
		
		public String toString(){
			return "Dummy Transform Handler";
		}
	}

	protected class LandmarkKeyboardProcessor implements KeyEventPostProcessor 
	{

		TransformEventHandler<AffineTransform3D> handlerQ;
		TransformEventHandler<AffineTransform3D> handlerP;
		
		DummyTransformEventHandler dummyHandler = new DummyTransformEventHandler();
		
		protected void disableTransformHandlers()
		{
//			System.out.println( "DISABLE TRANSFORMS");
//			System.out.println( "BEFORE viewerP handler" + BigWarp.this.viewerP.getDisplay().getTransformEventHandler() );
			
			// disable navigation listeners
			handlerP = BigWarp.this.viewerP.getDisplay().getTransformEventHandler();
			BigWarp.this.viewerP.getDisplay().setTransformEventHandler( dummyHandler );
			
			BigWarp.this.viewerP.setTransformEnabled( false );
			BigWarp.this.viewerQ.setTransformEnabled( false );
			
			handlerQ = BigWarp.this.viewerQ.getDisplay().getTransformEventHandler();
			BigWarp.this.viewerQ.getDisplay().setTransformEventHandler( dummyHandler );
			
			
//			System.out.println( "AFTER viewerP handler" + BigWarp.this.viewerP.getDisplay().getTransformEventHandler() );
		}
		
		protected void enableTransformHandlers()
		{
//			System.out.println( "ENABLE TRANSFORMS");
//			System.out.println( "BEFORE viewerP handler" + BigWarp.this.viewerP.getDisplay().getTransformEventHandler() );
			
			// enable navigation listeners
			BigWarp.this.viewerP.setTransformEnabled( true );
			BigWarp.this.viewerQ.setTransformEnabled( true );
			
			BigWarp.this.viewerP.getDisplay().setTransformEventHandler( handlerP );
			BigWarp.this.viewerQ.getDisplay().setTransformEventHandler( handlerQ );
			
//			System.out.println( "AFTER viewerP handler" + BigWarp.this.viewerP.getDisplay().getTransformEventHandler() );
		}
		
		@Override
		public boolean postProcessKeyEvent(KeyEvent ke ) {
			// if the event is consumed, ignore it
			if( ke.isConsumed())
				return false;
			
			if( ke.getKeyCode() == KeyEvent.VK_SPACE )
			{
				if( ke.getID() == KeyEvent.KEY_PRESSED )
				{
					if( !BigWarp.this.inLandmarkMode  ){
						BigWarp.this.viewerFrameP.getViewerPanelP().showMessage("Landmark mode on ( Moving image )");
						BigWarp.this.viewerFrameQ.getViewerPanelP().showMessage("Landmark mode on ( Fixed image )");
						BigWarp.this.inLandmarkMode = true;
						
						disableTransformHandlers();
						
					}
					return false;
				}
				else if( ke.getID() == KeyEvent.KEY_RELEASED  )
				{
					BigWarp.this.viewerFrameP.getViewerPanelP().showMessage("Landmark mode off ( Moving image )");
					BigWarp.this.viewerFrameQ.getViewerPanelP().showMessage("Landmark mode off ( Fixed image )");
					
					BigWarp.this.inLandmarkMode = false;
					
					enableTransformHandlers();
					return false;
				}
			}
			else if( ke.getKeyCode() == KeyEvent.VK_N && ke.getID() == KeyEvent.KEY_RELEASED  )
			{
				// pressing "N" while holding space toggles name visibility
				if(  BigWarp.this.inLandmarkMode )
				{
					BigWarp.this.viewerP.getSettings().toggleNamesVisible();
					BigWarp.this.viewerP.requestRepaint();
					BigWarp.this.viewerQ.getSettings().toggleNamesVisible();
					BigWarp.this.viewerQ.requestRepaint();
				}
			}
			else if( ke.getKeyCode() == KeyEvent.VK_V && ke.getID() == KeyEvent.KEY_RELEASED  )
			{
				// pressing "V" while holding space toggles point visibility
				if(  BigWarp.this.inLandmarkMode )
				{
					BigWarp.this.viewerP.getSettings().togglePointsVisible();
					BigWarp.this.viewerP.requestRepaint();
					BigWarp.this.viewerQ.getSettings().togglePointsVisible();
					BigWarp.this.viewerQ.requestRepaint();
				}
			}
			else if( ke.getKeyCode() == KeyEvent.VK_C && ke.getID() == KeyEvent.KEY_RELEASED  )
			{
				// pressing "C" while holding space computes a new transformation model
				if(  BigWarp.this.inLandmarkMode )
				{
					BigWarp.this.viewerFrameP.getViewerPanelP().showMessage("Estimating transformation...");
					BigWarp.this.viewerFrameQ.getViewerPanelP().showMessage("Estimating transformation...");
					
					restimateTransformation();
					BigWarp.this.viewerFrameP.getViewerPanelP().showMessage("done.");
					BigWarp.this.viewerFrameQ.getViewerPanelP().showMessage("done.");
				}
			}
			else if( ke.getKeyCode() == KeyEvent.VK_Q && ke.getID() == KeyEvent.KEY_RELEASED  )
			{
				// pressing "Q" makes the view transformation in the other window the same
				// as that for this active window
				
				if( ke.getComponent() == BigWarp.this.viewerP.getDisplay() )
				{
					BigWarp.this.viewerFrameP.getViewerPanelP().showMessage("Aligning.");
					BigWarp.this.viewerFrameQ.getViewerPanelP().showMessage("Matching alignment.");
					
					AffineTransform3D viewXfm = new AffineTransform3D();
					BigWarp.this.viewerP.getState().getViewerTransform( viewXfm );
					
					BigWarp.this.viewerQ.animateTransformation( viewXfm );
				}
				
				if( ke.getComponent() == BigWarp.this.viewerQ.getDisplay() )
				{
					BigWarp.this.viewerFrameP.getViewerPanelP().showMessage("Matching alignment.");
					BigWarp.this.viewerFrameQ.getViewerPanelP().showMessage("Aligning.");
					
					AffineTransform3D viewXfm = new AffineTransform3D();
					BigWarp.this.viewerQ.getState().getViewerTransform( viewXfm );
					
					BigWarp.this.viewerP.animateTransformation( viewXfm );
				}
			}
			else if( ke.getKeyCode() == KeyEvent.VK_W && ke.getID() == KeyEvent.KEY_RELEASED  )
			{
				// pressing "Q" makes the view transformation in the other window the same
				// as that for this active window
				
				if( ke.getComponent() == BigWarp.this.viewerQ.getDisplay() )
				{
					BigWarp.this.viewerFrameP.getViewerPanelP().showMessage("Aligning.");
					BigWarp.this.viewerFrameQ.getViewerPanelP().showMessage("Matching alignment.");
					
					AffineTransform3D viewXfm = new AffineTransform3D();
					BigWarp.this.viewerP.getState().getViewerTransform( viewXfm );
					
					BigWarp.this.viewerQ.animateTransformation( viewXfm );
				}
				
				if( ke.getComponent() == BigWarp.this.viewerP.getDisplay() )
				{
					BigWarp.this.viewerFrameP.getViewerPanelP().showMessage("Matching alignment.");
					BigWarp.this.viewerFrameQ.getViewerPanelP().showMessage("Aligning.");
					
					AffineTransform3D viewXfm = new AffineTransform3D();
					BigWarp.this.viewerQ.getState().getViewerTransform( viewXfm );
					
					BigWarp.this.viewerP.animateTransformation( viewXfm );
				}
			}
			else if( ke.getKeyCode() == KeyEvent.VK_ESCAPE && ke.getID() == KeyEvent.KEY_RELEASED  )
			{
				if( BigWarp.this.landmarkModel.isPointUpdatePending() )
				{
					BigWarp.this.landmarkModel.restorePendingUpdate();
				}
			}
			else if( ke.getKeyCode() == KeyEvent.VK_R && ke.getID() == KeyEvent.KEY_RELEASED  )
			{ 
				final RandomAccessibleInterval<?> interval = sources.get( 1 ).getSpimSource().getSource( 0, 0 );
				
				AffineTransform3D viewXfm = new AffineTransform3D();
				viewXfm.identity();
				viewXfm.set( -interval.min( 2 ), 2, 3 );
				
				if( ke.getComponent() == BigWarp.this.viewerP.getDisplay() )
				{
					BigWarp.this.viewerP.transformChanged( viewXfm );
					BigWarp.this.viewerP.getState().setViewerTransform( viewXfm );
				}
				else if ( ke.getComponent() == BigWarp.this.viewerQ.getDisplay() )
				{
					BigWarp.this.viewerQ.transformChanged( viewXfm );
					BigWarp.this.viewerQ.getState().setViewerTransform( viewXfm );
				}
			}
			else if( ke.getKeyCode() == KeyEvent.VK_T && ke.getID() == KeyEvent.KEY_RELEASED  )
			{ 
				boolean newState =  !BigWarp.this.fidipOverlayP.getIsTransformed();
				
				if( newState )
					BigWarp.this.viewerFrameP.getViewerPanelP().showMessage("Displaying warped");
				else
					BigWarp.this.viewerFrameP.getViewerPanelP().showMessage("Displaying raw");
				
				// Toggle whether moving image isdisplayed as transformed or not
				BigWarp.this.setIsTransformed( newState );
				BigWarp.this.viewerP.requestRepaint();
				
			}
			
			return false;
		}
	}
	
	protected class MouseLandmarkListener implements MouseListener, MouseMotionListener
	{

		// -1 indicates that no point is selected
		int selectedPointIndex = -1;
		double[] ptarray = new double[ 3 ];
		double[] ptBack = new double[ 3 ];
		
		private BigWarpViewerPanel thisViewer;
		private boolean isMoving;
		
		protected MouseLandmarkListener( BigWarpViewerPanel thisViewer )
		{
			setViewer( thisViewer );
			thisViewer.getDisplay().addHandler( this );
			isMoving = ( thisViewer == BigWarp.this.viewerP );
		}
		
		protected void setViewer( BigWarpViewerPanel thisViewer )
		{
			this.thisViewer = thisViewer;
		}
		
		@Override
		public void mouseClicked(MouseEvent arg0) {}
		
		/**
		 * Returns the index of the landmark under the mouse position,
		 * or -1 if no landmark is at the current position
		 */
		protected int selectedLandmark( double[] pt, boolean isMoving ){
			int N = BigWarp.this.landmarkModel.getRowCount();
			
			double dist = 0;
			double radsq = 100;
			landmarkLoop:
			for( int n = 0; n < N; n++ ){
				dist = 0;
				
				Double[] lmpt = BigWarp.this.landmarkModel.getPoints().get(n);
				String type = "FIXED";
				if( isMoving )
				{
					type = "MOVING";
					for( int i = 0; i < ndims; i++ )
					{
						dist += (pt[i] - lmpt[i]) * (pt[i] - lmpt[i]);

						if( dist > radsq )
							continue landmarkLoop;
					}
				}
				else
				{
					for( int i = 0; i < ndims; i++ )
					{
						dist += (pt[i] - lmpt[i+ndims]) * (pt[i] - lmpt[i+ndims]);

						if( dist > radsq )
							continue landmarkLoop;
					}
				}
				
				if( BigWarp.this.landmarkFrame.isVisible() ){
					BigWarp.this.landmarkTable.setEditingRow( n );
					BigWarp.this.landmarkFrame.repaint();
				}
				return n;
			}
			return -1;
		}
		
		@Override
		public void mouseEntered(MouseEvent arg0) {}

		@Override
		public void mouseExited(MouseEvent arg0) {}

		@Override
		public void mousePressed(MouseEvent arg0) 
		{
			if( BigWarp.this.inLandmarkMode )
			{
				thisViewer.getGlobalMouseCoordinates( BigWarp.this.currentLandmark );
				
				BigWarp.this.currentLandmark.localize( ptarray );
				
				if( isMoving && landmarkModel.getTransform() != null )
				{
					landmarkModel.getTransform().apply( ptarray, ptBack );
					selectedPointIndex = selectedLandmark( ptBack, isMoving );
				}
				else
				{
					selectedPointIndex = selectedLandmark( ptarray, isMoving );
				}
				
				// if we move the fixed point, we need to update the warped 
				// point for the moving image
				// since the point renderer uses the transformed points
				if( selectedPointIndex >= 0 && 
						!isMoving && landmarkModel.getTransform() != null )
				{
					BigWarp.this.landmarkModel.updateWarpedPoint( 
							selectedPointIndex, ptarray );
				}
			}
		}

		@Override
		public void mouseReleased(MouseEvent arg0) 
		{
			// deselect any point that may be selected
			if( BigWarp.this.inLandmarkMode && selectedPointIndex == -1 )
			{
				thisViewer.getGlobalMouseCoordinates( BigWarp.this.currentLandmark );
				currentLandmark.localize( ptarray );
				
				String message = "";
				if( isMoving && landmarkModel.getTransform() != null )
				{
					ptBack = landmarkModel.getTransform().apply( ptarray );
					message = BigWarp.this.landmarkModel.add(  ptBack, isMoving );
					
					BigWarp.this.landmarkModel.updateWarpedPoint( 
							BigWarp.this.landmarkModel.nextRow( isMoving ) - 1, 
							ptarray );
				}
				else
					message = BigWarp.this.landmarkModel.add( ptarray, isMoving );
				
				if( !message.isEmpty() )
					thisViewer.showMessage( message );

				if( BigWarp.this.landmarkFrame.isVisible() ){
					BigWarp.this.landmarkFrame.repaint();
				}
			}
			
			selectedPointIndex = -1;
		}

		@Override
		public void mouseDragged(MouseEvent e) 
		{
			if( BigWarp.this.inLandmarkMode && selectedPointIndex >= 0 )
			{
				
				thisViewer.getGlobalMouseCoordinates( BigWarp.this.currentLandmark );
				
				currentLandmark.localize( ptarray );
				
				if( isMoving && landmarkModel.getTransform() != null )
				{
					landmarkModel.getTransform().apply( ptarray, ptBack );
					BigWarp.this.landmarkModel.updateWarpedPoint( selectedPointIndex, ptarray );
				}
				
				if( !isMoving || landmarkModel.getTransform() == null )
				{
					BigWarp.this.landmarkModel.setPoint( selectedPointIndex, isMoving, ptarray );
				}
				else
				{
					BigWarp.this.landmarkModel.setPoint( selectedPointIndex, isMoving, ptBack );
				}
			}
			if( BigWarp.this.landmarkFrame.isVisible() ){
				BigWarp.this.landmarkFrame.repaint();
			}
			
			// final AffineTransform3D transform = viewerP.getDisplay().getTransformEventHandler().getTransform();
			// System.out.println("xfm: " + transform );

		}

		@Override
		public void mouseMoved(MouseEvent e) {}
		
	}
	
	public class LandmarkTableListener implements TableModelListener 
	{
		@Override
		public void tableChanged(TableModelEvent arg0) 
		{
			BigWarp.this.viewerP.requestRepaint();
			BigWarp.this.viewerQ.requestRepaint();
		}
	}
	
	public class MouseLandmarkTableListener implements MouseListener
	{
		
		BigWarpViewerPanel viewer;
		boolean wasDoubleClick = false;
	    Timer timer;
	    
	    public MouseLandmarkTableListener( BigWarpViewerPanel viewer )
	    {
	    	this.viewer = viewer;
	    }
	 
	    public void mouseClicked( MouseEvent e){
	    	if( BigWarp.this.inLandmarkMode )
	    	{
	    		JTable target = (JTable)e.getSource();
	    		
				int row = target.getSelectedRow();
				int column = target.getSelectedColumn();
	    		
	    		boolean isMoving = ( column > 1 && column < 5 );
	    		
	    		BigWarp.this.landmarkModel.setPointToUpdate( row, isMoving );
	    		
	    	}
	    	else if( e.getClickCount() == 2 ){
				JTable target = (JTable)e.getSource();
				int row = target.getSelectedRow();
				int column = target.getSelectedColumn();

				
				double[] pt = null;
				if( column >= 2 && column <= 4 )
				{
					if( viewer.getIsMoving())
					{
						pt = new double[]{
								(Double)target.getValueAt( row, 2 ),
								(Double)target.getValueAt( row, 3 ),
								(Double)target.getValueAt( row, 4 )
						};
					}
					else
					{
						return;
					}
				}
				else if( column >= 5 && column <= 7 )
				{
					if( viewer.getIsMoving() )
					{
						return;
					}
					else
					{
						pt = new double[]{
								(Double)target.getValueAt( row, 5 ),
								(Double)target.getValueAt( row, 6 ),
								(Double)target.getValueAt( row, 7 )
						};
					}
				}
				
				final AffineTransform3D transform = viewer.getDisplay().getTransformEventHandler().getTransform();
				AffineTransform3D xfmCopy = transform.copy();
				xfmCopy.set( 0.0, 0, 3 );
				xfmCopy.set( 0.0, 1, 3 );
				xfmCopy.set( 0.0, 2, 3 );
				
				double[] center = new double[]{
						viewer.getWidth() / 2,
						viewer.getHeight() / 2,
						0
				};
				double[] ptxfm = new double[3];
				xfmCopy.apply( pt, ptxfm );
				
				
				TranslationAnimator animator = new TranslationAnimator( 
						transform, 
						new double[]{center[0]-ptxfm[0], center[1]-ptxfm[1], -ptxfm[2]}, 
						300 );
				viewer.setTransformAnimator( animator );
				viewer.transformChanged( transform );
				
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseReleased(MouseEvent e) {}

	}
	
}
