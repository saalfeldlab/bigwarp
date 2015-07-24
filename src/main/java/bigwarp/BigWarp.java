package bigwarp;

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

import jitk.spline.ThinPlateR2LogRSplineKernelTransform;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SerializationUtils;
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
import bdv.gui.BigWarpViewerPanel;
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
import bdv.viewer.NavigationActions;
import bdv.viewer.SourceAndConverter;
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
	
	
	// protected ThinPlateR2LogRSplineKernelTransform estimatedXfmInv;

	/*
	 * landmarks are placed on clicks only if we are inLandmarkMode
	 * during the click
	 */
	protected boolean inLandmarkMode;
	
	public BigWarp( final String xmlFilenameP, final String xmlFilenameQ, final String windowTitle, final ProgressWriter progressWriter ) throws SpimDataException
	{

		/* Load the first source */
		final SpimDataMinimal spimData = loadSpimData( xmlFilenameP );
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		final List< TimePoint > timepoints = seq.getTimePoints().getTimePointsOrdered();
		
		final ArrayList< ConverterSetup > converterSetups = new ArrayList< ConverterSetup >();
		
		sources = new ArrayList< SourceAndConverter< ? > >();
		BigDataViewer.initSetups( spimData, converterSetups, sources );

		/* Load the second source */
		BigDataViewer.initSetups( loadSpimData( xmlFilenameQ ), converterSetups, sources );
		
		sources = wrapSourcesAsTransformed( sources );

		viewerFrameP = new BigWarpViewerFrame( DEFAULT_WIDTH, DEFAULT_HEIGHT, sources, timepoints.size(),
				( ( ViewerImgLoader< ?, ? > ) seq.getImgLoader() ).getCache(), "Fidip moving", true );
		viewerP = viewerFrameP.getViewerPanelP();
		
		viewerFrameQ = new BigWarpViewerFrame( DEFAULT_WIDTH, DEFAULT_HEIGHT, sources, timepoints.size(),
				( ( ViewerImgLoader< ?, ? > ) seq.getImgLoader() ).getCache(), "Fidip fixed", false );
		viewerQ = viewerFrameQ.getViewerPanelP();
		
		/* Set up LandmarkTableModel, holds the data and 
		 * interfaces with the LandmarkPanel */
		landmarkModel = new LandmarkTableModel();
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
		
		setupAssignments = new SetupAssignments( converterSetups, 0, 65535 );
		if ( setupAssignments.getMinMaxGroups().size() > 0 )
		{
			final MinMaxGroup group = setupAssignments.getMinMaxGroups().get( 0 );
			for ( final ConverterSetup setup : setupAssignments.getConverterSetups() )
				setupAssignments.moveSetupToGroup( setup, group );
		}
		
		brightnessDialog = new BrightnessDialog( viewerFrameP, setupAssignments );
		
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
		
		NavigationActions.installActionBindings( viewerFrameP.getKeybindings(), viewerP, keyProperties );
		BigWarpActions.installActionBindings( viewerFrameP.getKeybindings(), this, keyProperties);
		
		NavigationActions.installActionBindings( viewerFrameQ.getKeybindings(), viewerQ, keyProperties );
		BigWarpActions.installActionBindings( viewerFrameQ.getKeybindings(), this, keyProperties);
		
		setUpViewerMenu( viewerFrameP );
		setUpViewerMenu( viewerFrameQ );
		
		landmarkClickListenerP = new MouseLandmarkListener( this.viewerP );
		landmarkClickListenerQ = new MouseLandmarkListener( this.viewerQ );
		
		currentLandmark = new RealPoint( 3 );
		inLandmarkMode = false;
		
		setupKeyListener( );
		
		InitializeViewerState.initTransform( viewerP );
		
		viewerFrameP.setVisible(true);
		viewerFrameQ.setVisible(true);
		landmarkFrame.setVisible(true);
		
		System.out.println("face");
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
		System.out.println( "target width : " + target.getWidth() );
		System.out.println( "target height: " + target.getHeight() );
		
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
	
	private static ArrayList< SourceAndConverter< ? >> wrapSourcesAsTransformed( ArrayList< SourceAndConverter< ? > > sources )
	{
		ArrayList< SourceAndConverter< ? >> wrappedSource = new ArrayList< SourceAndConverter< ? >>();
		
		int i = 0;
		for( SourceAndConverter< ? > sac : sources )
		{
			System.out.println( sac );
			wrappedSource.add( wrapSourceAsTransformed( sac, "xfm_" + i  ));
			i++;
		}
		return wrappedSource;
	}
	
	private static < T > SourceAndConverter< T > wrapSourceAsTransformed( SourceAndConverter< T > src, String name )
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
				src.getConverter(), wrapSourceAsTransformed( src.asVolatile(), name + "_vol" ) );
		}
	}
		 
	private SpimDataMinimal loadSpimData( String xmlPath ){
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
//		// move landmark positions into the xfm
//		ArrayList<Double[]> pts = landmarkModel.getPoints();
//		double[][] mvgPts = new double[ 3 ][ pts.size() ];
//		double[][] tgtPts = new double[ 3 ][ pts.size() ];
//		
//		// TODO make this more efficient by not reallocating and re-copying every time
//		for( int i = 0; i < pts.size(); i++ )
//		{
//			for( int j = 0; j < 3; j++ )
//			{
//				// need to find the "inverse TPS" so exchange moving and tgt
//				mvgPts[ j ][ i ] = pts.get( i )[ j ];
//				tgtPts[ j ][ i ] = pts.get( i )[ j + 3 ];
//			}
//		}
		
		if( landmarkModel.getTransform() == null )
			landmarkModel.initTransformation();
		
		// estimate the forward transformation
//		landmarkModel.getTransform().setLandmarks( tgtPts, mvgPts );
		landmarkModel.getTransform().solve();
		landmarkModel.resetWarpedPoints();
		
		( (WarpedSource<?>)(sources.get( 0 ).getSpimSource())).updateTransform( landmarkModel.getTransform().deepCopy() );
		( (WarpedSource<?>)(sources.get( 0 ).asVolatile().getSpimSource())).updateTransform( landmarkModel.getTransform().deepCopy() );
		
		fidipOverlayP.setIsTransformed( true );
		
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
		
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

		final String fnP = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/fakeout.xml";
		final String fnQ = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/fakeoutMR.xml";
//		final String fnQ = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/light-brain-template.xml";
		
		final String fnLandmarks = "/groups/saalfeld/home/bogovicj/tests/test_bdvtps/cell2mr/pts2.lnmk";
		
//		final String fnP = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/flyc.xml";
//		final String fnQ = "/groups/saalfeld/home/bogovicj/dev/bdv/bdvLandmarkUi/resources/fruTemplate.xml";
//		final String fnLandmarks = "";
		
		try
		{
			System.setProperty( "apple.laf.useScreenMenuBar", "true" );
			new RepeatingReleasedEventsFixer().install();
			BigWarp fidip = new BigWarp( fnP, fnQ, new File( fnP ).getName(), new ProgressWriterConsole() );
			
			if( !fnLandmarks.isEmpty() )
			{
				fidip.landmarkModel.load( new File( fnLandmarks ));
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
			System.out.println( "DISABLE TRANSFORMS");
//			System.out.println( "BEFORE viewerP handler" + BigWarp.this.viewerP.getDisplay().getTransformEventHandler() );
			
			// disable navigation listeners
			handlerP = BigWarp.this.viewerP.getDisplay().getTransformEventHandler();
			BigWarp.this.viewerP.getDisplay().setTransformEventHandler( dummyHandler );
			
			BigWarp.this.viewerP.setTransformEnabled( false );
			BigWarp.this.viewerQ.setTransformEnabled( false );
			
//			handlerQ = BigWarp.this.viewerQ.getDisplay().getTransformEventHandler();
//			BigWarp.this.viewerQ.getDisplay().setTransformEventHandler( dummyHandler );
			
			
//			System.out.println( "AFTER viewerP handler" + BigWarp.this.viewerP.getDisplay().getTransformEventHandler() );
		}
		
		protected void enableTransformHandlers()
		{
			System.out.println( "ENABLE TRANSFORMS");
//			System.out.println( "BEFORE viewerP handler" + BigWarp.this.viewerP.getDisplay().getTransformEventHandler() );
			
			// enable navigation listeners
			BigWarp.this.viewerP.getDisplay().setTransformEventHandler( handlerP );
//			BigWarp.this.viewerQ.getDisplay().setTransformEventHandler( handlerQ );
			
			BigWarp.this.viewerP.setTransformEnabled( true );
			BigWarp.this.viewerQ.setTransformEnabled( true );
			
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
					
					if( landmarkModel.getTransform() == null )
					{
						// TODO generalize this
						landmarkModel.initTransformation();
					}
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
				
				BigWarp.this.viewerP.transformChanged( viewXfm );
				BigWarp.this.viewerP.getState().setViewerTransform( viewXfm );
			}
			
			return false;
		}
	}
	
	protected class MouseLandmarkListener implements MouseListener, MouseMotionListener
	{

		// -1 indicates that no point is selected
		int selectedPointIndex = -1;
		double[] ptarray = new double[ 3 ];
		
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
					for( int i = 0; i<3; i++ )
					{
						dist += (pt[i] - lmpt[i]) * (pt[i] - lmpt[i]);

						if( dist > radsq )
							continue landmarkLoop;
					}
				}
				else
				{
					for( int i = 0; i<3; i++ )
					{
						dist += (pt[i] - lmpt[i+3]) * (pt[i] - lmpt[i+3]);

						if( dist > radsq )
							continue landmarkLoop;
					}
				}
				
				System.out.println( type + ": SELECTED LANDMARK " + n );
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
					ptarray = landmarkModel.getTransform().apply( ptarray );
				
				
				selectedPointIndex = selectedLandmark( ptarray, isMoving );
				
				// if we move the fixed point, we need to update the warped 
				// point for the moving image
				// since the point renderer uses the transformed points
				if( selectedPointIndex >=0 && 
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
				
				double[] ptBack = null;
				if( isMoving && landmarkModel.getTransform() != null )
				{
					ptBack = landmarkModel.getTransform().apply( ptarray );
				}
				
				String message = "";
				// int num = Fidip.this.landmarkModel.getRowCount(); 
				if( ptBack == null )
					message = BigWarp.this.landmarkModel.add( ptarray, isMoving );
				else
					message = BigWarp.this.landmarkModel.add(  ptBack, isMoving );
				
				// if its moving and 
				if( isMoving && landmarkModel.getTransform() != null )
				{
					BigWarp.this.landmarkModel.updateWarpedPoint( 
							BigWarp.this.landmarkModel.nextRow( isMoving ) - 1, 
							ptarray );
				}
				
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
				
				double[] ptBack = null;
				if( landmarkModel.getTransform() != null )
				{
					ptBack = new double[ 3 ];
					//System.out.println("WARPED POINT BEFORE: " + ptarray[0] + " " + ptarray[1] + " " + ptarray[2]);
					landmarkModel.getTransform().apply( ptarray, ptBack );
					BigWarp.this.landmarkModel.updateWarpedPoint( selectedPointIndex, ptarray );
					//System.out.println("AFTER: " + ptBack[0] + " " + ptBack[1] + " " + ptBack[2]);
				}
				
				if( ptBack == null )
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
	    		// System.out.println("LANDMARK CLICKED TABLE! row: " + row + "  col: " + column );
	    		
	    		boolean isMoving = ( column > 1 && column < 5 );
	    		
	    		BigWarp.this.landmarkModel.setPointToUpdate( row, isMoving );
	    		
	    	}
	    	else if( e.getClickCount() == 2 ){
				JTable target = (JTable)e.getSource();
				int row = target.getSelectedRow();
				int column = target.getSelectedColumn();

				// System.out.println("CLICKED TABLE! row: " + row + "  col: " + column );
				
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
