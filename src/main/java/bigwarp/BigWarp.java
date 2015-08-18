package bigwarp;

import ij.IJ;
import ij.ImagePlus;

import java.awt.Component;
import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;

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
import bdv.gui.LandmarkKeyboardProcessor;
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
import bdv.viewer.LandmarkPointMenu;
import bdv.viewer.MultiBoxOverlay2d;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerPanel.Options;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.WarpNavigationActions;
import bdv.viewer.animate.TranslationAnimator;
import bdv.viewer.overlay.BigWarpSourceOverlayRenderer;
import bdv.viewer.overlay.MultiBoxOverlayRenderer;
import bdv.viewer.render.MultiResolutionRenderer;
import bdv.viewer.state.ViewerState;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.source.GridSource;
import bigwarp.source.WarpMagnitudeSource;

public class BigWarp {
	
	protected static final int DEFAULT_WIDTH  = 800;
	protected static final int DEFAULT_HEIGHT = 600;
	
	protected ArrayList< SourceAndConverter< ? > > sources;
	
//	protected final SetupAssignments setupAssignments;
	protected final BrightnessDialog brightnessDialog;
	protected final HelpDialog helpDialog;
	
	protected final VisibilityAndGroupingDialog activeSourcesDialog;
	
	private final BigWarpViewerFrame viewerFrameP;
	private final BigWarpViewerFrame viewerFrameQ;
	protected final BigWarpViewerPanel viewerP;
	protected final BigWarpViewerPanel viewerQ;
	
	protected final BigWarpLandmarkPanel landmarkPanel;
	protected final LandmarkPointMenu    landmarkPopupMenu;
	protected final JFrame landmarkFrame;
	
	protected final BigWarpOverlay overlayP;
	protected final BigWarpOverlay overlayQ;
	
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
	
	protected TransformEventHandler<AffineTransform3D> handlerQ;
	protected TransformEventHandler<AffineTransform3D> handlerP;
	final static DummyTransformEventHandler dummyHandler = new DummyTransformEventHandler();

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

		// If the images are 2d, use a transform handler that limits transformations to 
		// rotations and scalings of the 2d plane ( z = 0 )
		Options optionsP = ViewerPanel.options();
		Options optionsQ = ViewerPanel.options();
		if( ndims == 2 )
		{
			System.out.println("IS 2D");
			optionsP.transformEventHandlerFactory( TransformHandler3DWrapping2D.factory() );
			optionsQ.transformEventHandlerFactory( TransformHandler3DWrapping2D.factory() );
			
			optionsP.boxOverlayRenderer( new MultiBoxOverlayRenderer( 800, 600, new MultiBoxOverlay2d()) );
			optionsQ.boxOverlayRenderer( new MultiBoxOverlayRenderer( 800, 600, new MultiBoxOverlay2d()) );
		}
		optionsP.sourceInfoOverlayRenderer( new BigWarpSourceOverlayRenderer() );
		optionsQ.sourceInfoOverlayRenderer( new BigWarpSourceOverlayRenderer() );
		
		// Viewer frame for the moving image
		viewerFrameP = new BigWarpViewerFrame( DEFAULT_WIDTH, DEFAULT_HEIGHT, sources, 1,
				( ( ViewerImgLoader< ?, ? > ) seq.getImgLoader() ).getCache(), optionsP, "Fidip moving", true );
		viewerP = getViewerFrameP().getViewerPanel();

		
		// Viewer frame for the fixed image
		viewerFrameQ = new BigWarpViewerFrame( DEFAULT_WIDTH, DEFAULT_HEIGHT, sources, 1,
				( ( ViewerImgLoader< ?, ? > ) seq.getImgLoader() ).getCache(), optionsQ, "Fidip fixed", false );
		viewerQ = getViewerFrameQ().getViewerPanel();
		
		viewerP.setNumDim( ndims );
		viewerQ.setNumDim( ndims );
		
		activeSourcesDialog = new VisibilityAndGroupingDialog( viewerFrameQ, viewerQ.getVisibilityAndGrouping() );
		setUpViewerMenu( getViewerFrameP() );
		setUpViewerMenu( getViewerFrameQ() );
		
		/* Set up LandmarkTableModel, holds the data and 
		 * interfaces with the LandmarkPanel */
		landmarkModel = new LandmarkTableModel( ndims );
		landmarkModellistener = new LandmarkTableListener();
		landmarkModel.addTableModelListener(landmarkModellistener);
		landmarkTable = new JTable(landmarkModel);
		
		overlayP = new BigWarpOverlay( viewerP, landmarkModel );
		overlayQ = new BigWarpOverlay( viewerQ, landmarkModel );
		viewerP.addOverlay( overlayP );
		viewerQ.addOverlay( overlayQ );
		
		/* Set up landmark panel */
		landmarkPanel = new BigWarpLandmarkPanel( landmarkModel );
		landmarkPanel.setOpaque( true );
		addDefaultTableMouseListener();
		
		landmarkFrame = new JFrame( "Landmarks" );
		landmarkFrame.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
		landmarkFrame.setContentPane( landmarkPanel );
		landmarkFrame.pack();
		
		landmarkPopupMenu = new LandmarkPointMenu( landmarkPanel );
		landmarkPopupMenu.setupListeners( );
		
		// TODO brightness dialog setup
		int i = 0;
		ArrayList<ConverterSetup> csetups = new ArrayList<ConverterSetup>();
		for ( final ConverterSetup cs : converterSetups )
		{
			System.out.println("converter setup: " + (i++) );
			csetups.add( cs );
			if ( RealARGBColorConverterSetup.class.isInstance( cs ))
				( ( RealARGBColorConverterSetup ) cs ).setViewer( viewerQ );
		}
		
		SetupAssignments setupAssignments = new SetupAssignments( csetups, 0, 512 );
		if ( setupAssignments.getMinMaxGroups().size() > 0 )
		{
			final MinMaxGroup group = setupAssignments.getMinMaxGroups().get( 0 );
			for ( final ConverterSetup setup : setupAssignments.getConverterSetups() )
				setupAssignments.moveSetupToGroup( setup, group );
		}
		brightnessDialog = new BrightnessDialog( getViewerFrameQ(), setupAssignments );
		helpDialog = new HelpDialog( getViewerFrameP() );
		
		final KeyProperties keyProperties = KeyProperties.readPropertyFile();
		WarpNavigationActions.installActionBindings( getViewerFrameP().getKeybindings(), viewerP, keyProperties, (ndims==2) );
		BigWarpActions.installActionBindings( getViewerFrameP().getKeybindings(), this, keyProperties);
		
		WarpNavigationActions.installActionBindings( getViewerFrameQ().getKeybindings(), viewerQ, keyProperties, (ndims==2) );
		BigWarpActions.installActionBindings( getViewerFrameQ().getKeybindings(), this, keyProperties);
		
		setUpViewerMenu( viewerFrameP );
		setUpViewerMenu( viewerFrameQ );
		setUpLandmarkMenus();
		
		// initialize warp base to affine
		warpMagTypeGroup.getElements().nextElement().doClick();
		
		/* Set the locations of frames */
		Point viewerFramePloc = getViewerFrameP().getLocation();
		viewerFramePloc.setLocation( viewerFramePloc.x + DEFAULT_WIDTH, viewerFramePloc.y );
		getViewerFrameQ().setLocation( viewerFramePloc );
		viewerFramePloc.setLocation( viewerFramePloc.x + DEFAULT_WIDTH, viewerFramePloc.y );
		landmarkFrame.setLocation( viewerFramePloc );
		
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
		
		checkBoxInputMaps();
	}
	
	protected void setUpViewerMenu( BigWarpViewerFrame vframe )
	{
		final ActionMap actionMap = vframe.getKeybindings().getConcatenatedActionMap();
		System.out.println( actionMap );
		
		JMenuBar viewerMenuBar = new JMenuBar();
		
		JMenu settingsMenu    = new JMenu( "Settings" );
		viewerMenuBar.add( settingsMenu );
		
		final JMenuItem miBrightness;
		if( vframe.isMoving() )
			miBrightness = new JMenuItem( actionMap.get( BigWarpActions.BRIGHTNESS_SETTINGS_P ) );
		else
			miBrightness = new JMenuItem( actionMap.get( BigWarpActions.BRIGHTNESS_SETTINGS_Q ) );
		
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
			public void mouseClicked(MouseEvent e) {
				 System.out.println( "clicked open  ");
			}
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
			public void mouseClicked(MouseEvent e) {
				System.out.println( "clicked save ");
			}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) 
			{
				System.out.println( "save save save ");
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
	
	public BigWarpViewerFrame getViewerFrameP() {
		return viewerFrameP;
	}

	public BigWarpViewerFrame getViewerFrameQ() {
		return viewerFrameQ;
	}
	
	public BigWarpOverlay getOverlayP()
	{
		return overlayP;
	}
	
	public BigWarpOverlay getOverlayQ()
	{
		return overlayQ;
	}
	
	public ArrayList<SourceAndConverter<?>> getSources()
	{
		return sources;
	}

	public BigWarpLandmarkPanel getLandmarkPanel()
	{
		return landmarkPanel;
	}
	
	public boolean isInLandmarkMode(){
		return inLandmarkMode;
	}

	public void setInLandmarkMode( boolean inLmMode )
	{
		System.out.println( "setInLandmarkMode" );
		if( inLmMode )
		{
			disableTransformHandlers();
			viewerP.showMessage("Landmark mode on ( Moving image )");
			viewerQ.showMessage("Landmark mode on ( Fixed image )");
		}
		else
		{
			enableTransformHandlers();
			viewerP.showMessage("Landmark mode off ( Moving image )");
			viewerQ.showMessage("Landmark mode off ( Fixed image )");
		}
		
		inLandmarkMode = inLmMode;
	}
	
	public void toggleInLandmarkMode()
	{
		setInLandmarkMode( !inLandmarkMode );
	}
	
	protected void disableTransformHandlers()
	{
		// disable navigation listeners
		handlerP = viewerP.getDisplay().getTransformEventHandler();
		viewerP.getDisplay().setTransformEventHandler( dummyHandler );
		
		viewerP.setTransformEnabled( false );
		viewerQ.setTransformEnabled( false );
		
		handlerQ = viewerQ.getDisplay().getTransformEventHandler();
		viewerQ.getDisplay().setTransformEventHandler( dummyHandler );
	}
	
	protected void enableTransformHandlers()
	{
		// enable navigation listeners
		viewerP.setTransformEnabled( true );
		viewerQ.setTransformEnabled( true );
		
		if( handlerP != null )
			viewerP.getDisplay().setTransformEventHandler( handlerP );
		
		if( handlerQ != null )
			viewerQ.getDisplay().setTransformEventHandler( handlerQ );
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
				new LandmarkKeyboardProcessor( this ));
	}
	
	public void restimateTransformation()
	{
		// TODO restimateTransformation
		boolean isFirst = false;
		if( landmarkModel.getTransform() == null )
		{
			landmarkModel.initTransformation();
			isFirst = true;
		}
		else
		{
			landmarkModel.transferUpdatesToModel();
			System.out.println( "are point valid? " + landmarkModel.validateTransformPoints());
		}
		
		// estimate the forward transformation
		landmarkModel.getTransform().solve();
		landmarkModel.resetWarpedPoints();
		landmarkModel.resetUpdated();
		
		( (WarpedSource<?>)(sources.get( 0 ).getSpimSource())).updateTransform( landmarkModel.getTransform().deepCopy() );
		
		if( sources.get(0).asVolatile() != null )
			( (WarpedSource<?>)(sources.get( 0 ).asVolatile().getSpimSource())).updateTransform( landmarkModel.getTransform().deepCopy() );
		
		// display the warped version automatically if this is the first
		// time the transform was computed
		if( isFirst )
			setIsMovingDisplayTransformed( true );
		
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
	}
	
	public void setIsMovingDisplayTransformed( boolean isTransformed )
	{
		( (WarpedSource<?>)(sources.get( 0 ).getSpimSource())).setIsTransformed( isTransformed );
		
		if( sources.get(0).asVolatile() != null )
			( (WarpedSource<?>)(sources.get( 0 ).asVolatile().getSpimSource())).setIsTransformed( isTransformed );
		
		overlayP.setIsTransformed( isTransformed );
	}
	
	public boolean isMovingDisplayTransformed()
	{
		return ((WarpedSource<?>)(sources.get( 0 ).getSpimSource())).isTransformed();
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
	
	
	public static void main( final String[] args )
	{
		
//		final String fnP = "/groups/saalfeld/home/bogovicj/dev/bdv/bigdataviewer-core/src/main/resources/openconnectome-bock11-neariso.xml";
//		final String fnP = "/groups/saalfeld/home/bogovicj/dev/bdv/bigdataviewer-core/src/main/resources/openconnectome-cardona1-neariso.xml";
//		final String fnQ = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/light-brain-template.xml";
		
//		final String fnP = "/groups/saalfeld/home/bogovicj/dev/bdv/bigdataviewer-core/src/main/resources/tiffTest.xml";
//		final String fnQ = "/groups/saalfeld/home/bogovicj/dev/bdv/bigdataviewer-core/src/main/resources/tiffTest.xml";
		
//		final String fnP = "/Users/bogovicj/workspaces/bdv/bigdataviewer-core/src/main/resources/tiffTest.xml";
//		final String fnQ = "/Users/bogovicj/workspaces/bdv/bigdataviewer-core/src/main/resources/fakeoutLM.xml";
		
//		final String fnP = "/Users/bogovicj/workspaces/bdv/bdvLandmarkUi/resources/fakeout.xml";
//		final String fnQ = "/Users/bogovicj/workspaces/bdv/bdvLandmarkUi/resources/fakeoutMR.xml";

//		final String fnP = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/fakeout.xml";
//		final String fnQ = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/fakeoutMR.xml";
////		final String fnQ = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/light-brain-template.xml";
////		
//		final String fnLandmarks = "/groups/saalfeld/home/bogovicj/tests/test_bdvtps/cell2mr/pts2.lnmk";
		
//		final String fnP = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/flyc.xml";
//		final String fnQ = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/fruTemplate.xml";
//		final String fnLandmarks = "/groups/saalfeld/home/bogovicj/projects/wong_reg/flyc_tps/flyc_tps"; 
		
		// A better 2d example
//		final String fnP = "/groups/saalfeld/home/bogovicj/dev/bdv/bigwarp/src/main/resources/data/histology/KChlP1_invert.png";
//		final String fnQ = "/groups/saalfeld/home/bogovicj/dev/bdv/bigwarp/src/main/resources/data/histology/nissl_1_invert.png";
//		final String fnLandmarks = "/groups/saalfeld/home/bogovicj/dev/bdv/bigwarp/src/main/resources/data/histology/landmarks"; 
		
		// A 2d example
////		final String fnP = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/dots.xml";
////		final String fnQ = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/gel.xml";
		final String fnP = "/groups/saalfeld/home/bogovicj/tests/Dot_Blot0000.png";
		final String fnQ = "/groups/saalfeld/home/bogovicj/tests/gel0000.png";
//		final String fnP = "/Users/bogovicj/tmp/gel0000.png"; // this
//		final String fnQ = "/Users/bogovicj/tmp/Dot_Blot0000.png";
////		final String fnLandmarks = "/groups/saalfeld/home/bogovicj/tests/test_bdvtps/dotsAndGenes/dotsAndGenes";
		final String fnLandmarks = "/groups/saalfeld/home/bogovicj/tests/test_bdvtps/dotsAndGenes2/dotsAndGenes2";

		// grid example
//		final String fnP = "/groups/saalfeld/home/bogovicj/dev/bdv/bigdataviewer-core/src/main/resources/test/grid.png";
//		final String fnQ = "/groups/saalfeld/home/bogovicj/dev/bdv/bigdataviewer-core/src/main/resources/test/grid_blur.png";
//		final String fnLandmarks = "/groups/saalfeld/home/bogovicj/dev/bdv/bigdataviewer-core/src/main/resources/test/gridTest100";
		
//		final String fnLandmarks = "";
		
		try
		{
//			System.setProperty( "apple.laf.useScreenMenuBar", "true" );
			new RepeatingReleasedEventsFixer().install();
			
			BigWarp bw;
			if( fnP.endsWith("xml") && fnQ.endsWith("xml"))
				bw = new BigWarp( loadSourcesFromXmls( fnP, fnQ ), new File( fnP ).getName(), new ProgressWriterConsole() );
			else if( fnP.endsWith("png") && fnQ.endsWith("png") )
				bw = new BigWarp( loadSourcesFromImages( fnP, fnQ ), new File( fnP ).getName(), new ProgressWriterConsole() );
			else{
				System.err.println("Error reading files - should both be xmls or both image files");
				return;
			}
			
			if( !fnLandmarks.isEmpty() )
				bw.landmarkModel.load( new File( fnLandmarks ));
			
		}
		catch ( final Exception e )
		{
			
			e.printStackTrace();
		}
		
		System.out.println("done");
	}
	
	public void checkBoxInputMaps()
	{
		// Disable spacebar for toggling checkboxes
		// Make it enter instead
		// This is super ugly ... why does it have to be this way.
		
		TableCellEditor celled = landmarkTable.getCellEditor( 0, 1 );
		Component c = celled.getTableCellEditorComponent(landmarkTable, new Boolean(true), true, 0, 1 );
		
		InputMap parentInputMap = ((JCheckBox)c).getInputMap().getParent();
		parentInputMap.clear();
		KeyStroke enterDownKS = KeyStroke.getKeyStroke("pressed ENTER" );
		KeyStroke enterUpKS = KeyStroke.getKeyStroke("released ENTER" );
		
		parentInputMap.put( enterDownKS, "pressed" );
		parentInputMap.put(   enterUpKS, "released" );
		
		/* Consider with replacing with something like the below 
		 * Found in BigWarpViewerFrame
		 */
//		SwingUtilities.replaceUIActionMap( getRootPane(), keybindings.getConcatenatedActionMap() );
//		SwingUtilities.replaceUIInputMap( getRootPane(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );
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
			// TODO selectedLandmark
			int N = BigWarp.this.landmarkModel.getRowCount();
			
			double dist = 0;
			double radsq = 100;
			landmarkLoop:
			for( int n = 0; n < N; n++ )
			{
			
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
				
				if( isMoving && landmarkModel.getTransform() != null && BigWarp.this.isMovingDisplayTransformed() )
				{
					landmarkModel.getTransform().apply( ptarray, ptBack );
					selectedPointIndex = selectedLandmark( ptBack, isMoving );
				}
				else
				{
					selectedPointIndex = selectedLandmark( ptarray, isMoving );
				}
				//System.out.println( "HERE: " + isMoving + " selectedPoint: " + selectedPointIndex );
				
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
				
				if( isMoving && landmarkModel.getTransform() != null && BigWarp.this.isMovingDisplayTransformed() )
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
				LandmarkTableModel lmModel = BigWarp.this.landmarkModel;
				thisViewer.getGlobalMouseCoordinates( BigWarp.this.currentLandmark );
				
				currentLandmark.localize( ptarray );
				
				if( isMoving && landmarkModel.getTransform() != null && BigWarp.this.isMovingDisplayTransformed())
				{
					landmarkModel.getTransform().apply( ptarray, ptBack );
					lmModel.updateWarpedPoint( selectedPointIndex, ptarray );
				}
				
				if( !isMoving || landmarkModel.getTransform() == null || !BigWarp.this.isMovingDisplayTransformed())
				{					
					lmModel.setPoint( selectedPointIndex, isMoving, ptarray );
					
					if( !isMoving && !lmModel.isWarpedPositionChanged( selectedPointIndex ))
						lmModel.updateWarpedPoint( selectedPointIndex, ptarray );
				}
				else
				{
					BigWarp.this.landmarkModel.setPoint( selectedPointIndex, isMoving, ptBack );
				}
			}
			if( BigWarp.this.landmarkFrame.isVisible() ){
				BigWarp.this.landmarkFrame.repaint();
			}
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
		// TODO MouseTableListener
		BigWarpViewerPanel viewer;
		boolean wasDoubleClick = false;
	    Timer timer;
	    
	    public MouseLandmarkTableListener( BigWarpViewerPanel viewer )
	    {
	    	this.viewer = viewer;
	    }
	 
	    public void mouseClicked( MouseEvent e)
	    {
	    	
	    	if( BigWarp.this.inLandmarkMode )
	    	{
	    		JTable target = (JTable)e.getSource();
	    		
				int row = target.getSelectedRow();
				int column = target.getSelectedColumn();
	    		
	    		boolean isMoving = ( column > 1 && column < ( 2 + ndims ) );
	    		
	    		BigWarp.this.landmarkModel.setPointToUpdate( row, isMoving );
	    		
	    	}
	    	else if( e.getClickCount() == 2 ){
				JTable target = (JTable)e.getSource();
				int row = target.getSelectedRow();
				int column = target.getSelectedColumn();

				double[] pt = null;
				if( column >= 2 && column < ( 2 + ndims ))
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
				else if( ( 2 + ndims ) >= 5 )
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
				}else{
					// we're in a column that doesnt correspond to a point and
					// should do nothing
					return;
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
				
				// this should work fine in the 2d case
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
	
	protected static class DummyTransformEventHandler implements TransformEventHandler<AffineTransform3D>
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
}
