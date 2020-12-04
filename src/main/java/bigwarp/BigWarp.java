package bigwarp;

import bdv.util.Bounds;
import bdv.viewer.ConverterSetups;
import bdv.viewer.DisplayMode;
import bdv.viewer.TransformListener;
import bdv.viewer.ViewerState;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FileDialog;
import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.registration.ViewTransformAffine;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.janelia.utility.ui.RepeatingReleasedEventsFixer;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.scijava.ui.behaviour.io.InputTriggerConfig;

import bdv.BigDataViewer;
import bdv.cache.CacheControl;
import bdv.export.ProgressWriter;
import bdv.export.ProgressWriterConsole;
import bdv.gui.BigWarpLandmarkPanel;
import bdv.gui.BigWarpMessageAnimator;
import bdv.gui.BigWarpViewerFrame;
import bdv.gui.BigWarpViewerOptions;
import bdv.gui.BigwarpLandmarkSelectionPanel;
import bdv.gui.LandmarkKeyboardProcessor;
import bdv.gui.TransformTypeSelectDialog;
import bdv.ij.ApplyBigwarpPlugin;
import bdv.ij.BigWarpToDeformationFieldPlugIn;
import bdv.ij.util.ProgressWriterIJ;
import bdv.img.WarpedSource;
import bdv.tools.InitializeViewerState;
import bdv.tools.VisibilityAndGroupingDialog;
import bdv.tools.bookmarks.Bookmarks;
import bdv.tools.bookmarks.BookmarksEditor;
import bdv.tools.brightness.BrightnessDialog;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.BigWarpDragOverlay;
import bdv.viewer.BigWarpLandmarkFrame;
import bdv.viewer.BigWarpOverlay;
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.BigWarpViewerSettings;
import bdv.viewer.Interpolation;
import bdv.viewer.LandmarkPointMenu;
import bdv.viewer.MultiBoxOverlay2d;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SynchronizedViewerState;
import bdv.viewer.ViewerPanel;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.WarpNavigationActions;
import bdv.viewer.animate.SimilarityModel3D;
import bdv.viewer.animate.TranslationAnimator;
import bdv.viewer.overlay.BigWarpSourceOverlayRenderer;
import bdv.viewer.overlay.MultiBoxOverlayRenderer;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.loader.ImagePlusLoader.ColorSettings;
import bigwarp.source.GridSource;
import bigwarp.source.JacobianDeterminantSource;
import bigwarp.source.WarpMagnitudeSource;
import bigwarp.util.BigWarpUtils;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import jitk.spline.XfmUtils;
import mpicbg.models.AbstractAffineModel2D;
import mpicbg.models.AbstractAffineModel3D;
import mpicbg.models.AbstractModel;
import mpicbg.models.AffineModel2D;
import mpicbg.models.AffineModel3D;
import mpicbg.models.CoordinateTransform;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.InvertibleCoordinateTransform;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.RigidModel2D;
import mpicbg.models.RigidModel3D;
import mpicbg.models.SimilarityModel2D;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.TranslationModel3D;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealPositionable;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.img.imageplus.FloatImagePlus;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.realtransform.Wrapped2DTransformAs3D;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class BigWarp< T >
{

	protected static final int DEFAULT_WIDTH = 600;

	protected static final int DEFAULT_HEIGHT = 400;

	public static final int GRID_SOURCE_ID = 1696993146;

	public static final int WARPMAG_SOURCE_ID = 956736363;

	public static final int JACDET_SOURCE_ID = 1006827158;

	protected BigWarpViewerOptions options;

	protected BigWarpData< T > data;

	// descriptive names for indexing sources
	protected int[] movingSourceIndexList;

	protected int[] targetSourceIndexList;

	protected List< SourceAndConverter< T > > sources;
	
	protected final SetupAssignments setupAssignments;

	protected final BrightnessDialog brightnessDialog;

	protected final WarpVisFrame warpVisDialog;

	protected final HelpDialog helpDialog;

	protected final VisibilityAndGroupingDialog activeSourcesDialogP;

	protected final VisibilityAndGroupingDialog activeSourcesDialogQ;

	final AffineTransform3D fixedViewXfm;

	private final Bookmarks bookmarks;

	protected final BookmarksEditor bookmarkEditorP;

	protected final BookmarksEditor bookmarkEditorQ;

	private final BigWarpViewerFrame viewerFrameP;

	private final BigWarpViewerFrame viewerFrameQ;

	protected final BigWarpViewerPanel viewerP;

	protected final BigWarpViewerPanel viewerQ;

	protected final AffineTransform3D initialViewP;

	protected final AffineTransform3D initialViewQ;

	private JMenuItem toggleAlwaysWarpMenuP;

	private JMenuItem toggleAlwaysWarpMenuQ;

	protected BigWarpLandmarkPanel landmarkPanel;

	protected final LandmarkPointMenu landmarkPopupMenu;

	protected final BigWarpLandmarkFrame landmarkFrame;

	protected final BigWarpViewerSettings viewerSettings;

	protected final BigWarpOverlay overlayP;

	protected final BigWarpOverlay overlayQ;

	protected final BigWarpDragOverlay dragOverlayP;

	protected final BigWarpDragOverlay dragOverlayQ;

	protected RealPoint currentLandmark;

	protected LandmarkTableModel landmarkModel;

	protected InvertibleRealTransform currentTransform;

	protected JTable landmarkTable;

	protected LandmarkTableListener landmarkModellistener;

	protected MouseLandmarkListener landmarkClickListenerP;

	protected MouseLandmarkListener landmarkClickListenerQ;

	protected MouseLandmarkTableListener landmarkTableListener;

	protected BigWarpMessageAnimator message;

	protected final Set< KeyEventPostProcessor > keyEventPostProcessorSet = new HashSet< KeyEventPostProcessor >();

	private final RepeatingReleasedEventsFixer repeatedKeyEventsFixer;

	protected final SourceAndConverter< FloatType > gridSource;

	protected final SourceAndConverter< FloatType > warpMagSource;

	protected final SourceAndConverter< FloatType > jacDetSource;

	protected final AbstractModel< ? >[] baseXfmList;

	private final double[] ptBack;

	private SolveThread solverThread;

	private long keyClickMaxLength = 250;
	
	protected TransformTypeSelectDialog transformSelector;

	protected String transformType = TransformTypeSelectDialog.TPS;

	protected AffineTransform3D tmpTransform = new AffineTransform3D();

	/*
	 * landmarks are placed on clicks only if we are inLandmarkMode during the
	 * click
	 */
	protected boolean inLandmarkMode;

	protected int baselineModelIndex;

	// file selection
	final JFrame fileFrame;

	final FileDialog fileDialog;

	protected File lastDirectory;

	protected boolean updateWarpOnPtChange = false;

	protected boolean firstWarpEstimation = true;

	JMenu fileMenu;

	final ProgressWriter progressWriter;

	private static ImageJ ij;

	protected static Logger logger = LogManager.getLogger( BigWarp.class.getName() );

	private SpimData movingSpimData;

	private File movingImageXml;

	private CopyOnWriteArrayList< TransformListener< InvertibleRealTransform > > transformListeners = new CopyOnWriteArrayList<>( );

	final int ndims;

	public BigWarp( final BigWarpData<T> data, final String windowTitle, final ProgressWriter progressWriter ) throws SpimDataException
	{
		this( data, windowTitle, BigWarpViewerOptions.options( ( detectNumDims( data.sources ) == 2 ) ), progressWriter );
	}

	public BigWarp( final BigWarpData<T> data, final String windowTitle,  BigWarpViewerOptions options, final ProgressWriter progressWriter ) throws SpimDataException
	{
		repeatedKeyEventsFixer = RepeatingReleasedEventsFixer.installAnyTime();

		ij = IJ.getInstance();

		if( progressWriter == null )
			this.progressWriter = new ProgressWriterConsole();
		else
			this.progressWriter = progressWriter;

		this.data = data;
		this.options = options;

		ptBack = new double[ 3 ];
		if( options.is2d )
			ndims = 2;
		else
			ndims = 3;

		/*
		 * Set up LandmarkTableModel, holds the data and interfaces with the
		 * LandmarkPanel
		 */
		landmarkModel = new LandmarkTableModel( ndims );
		landmarkModellistener = new LandmarkTableListener();
		landmarkModel.addTableModelListener( landmarkModellistener );
		addTransformListener( landmarkModel );

		/* Set up landmark panel */
		landmarkPanel = new BigWarpLandmarkPanel( landmarkModel );
		landmarkPanel.setOpaque( true );
		landmarkTable = landmarkPanel.getJTable();
		landmarkTable.setDefaultRenderer( Object.class, new WarningTableCellRenderer() );
		addDefaultTableMouseListener();

		landmarkFrame = new BigWarpLandmarkFrame( "Landmarks", landmarkPanel, this );

		baseXfmList = new AbstractModel< ? >[ 3 ];
		setupWarpMagBaselineOptions( baseXfmList, ndims );

		fixedViewXfm = new AffineTransform3D();
		//sources.get( targetSourceIndexList[ 0 ] ).getSpimSource().getSourceTransform( 0, 0, fixedViewXfm );
		data.sources.get( data.targetSourceIndices[ 0 ] ).getSpimSource().getSourceTransform( 0, 0, fixedViewXfm );

		baselineModelIndex = 0;
		warpMagSource = addWarpMagnitudeSource( data, "WarpMagnitudeSource" );
		jacDetSource = addJacobianDeterminantSource( data, "JacobianDeterminantSource" );
		gridSource = addGridSource( data, "GridSource" );

		this.sources = this.data.sources;
		final List< ConverterSetup > converterSetups = data.converterSetups;
		this.movingSourceIndexList = data.movingSourceIndices;
		this.targetSourceIndexList = data.targetSourceIndices;
		Arrays.sort( movingSourceIndexList );
		Arrays.sort( targetSourceIndexList );

		sources = wrapSourcesAsTransformed( data.sources, ndims, data );

		setGridType( GridSource.GRID_TYPE.LINE );

		viewerSettings = new BigWarpViewerSettings();

		// key properties
		final InputTriggerConfig keyProperties = BigDataViewer.getInputTriggerConfig( options );
		options = options.inputTriggerConfig( keyProperties );

		// Viewer frame for the moving image
		viewerFrameP = new BigWarpViewerFrame( this, DEFAULT_WIDTH, DEFAULT_HEIGHT, (List)sources, converterSetups, viewerSettings,
				data.cache, options, "Bigwarp moving image", true, movingSourceIndexList, targetSourceIndexList );

		viewerP = getViewerFrameP().getViewerPanel();

		// Viewer frame for the fixed image
		viewerFrameQ = new BigWarpViewerFrame( this, DEFAULT_WIDTH, DEFAULT_HEIGHT, (List)sources, converterSetups, viewerSettings,
				data.cache, options, "Bigwarp fixed image", false, movingSourceIndexList, targetSourceIndexList );

		viewerQ = getViewerFrameQ().getViewerPanel();

		// setup messaging
		message = options.getMessageAnimator();
		message.setViewers( viewerP, viewerQ );
		landmarkModel.setMessage( message );

		// If the images are 2d, use a transform handler that limits
		// transformations to rotations and scalings of the 2d plane ( z = 0 )
		if ( options.is2d )
		{

			final Class< ViewerPanel > c_vp = ViewerPanel.class;
			try
			{
				final Field overlayRendererField = c_vp.getDeclaredField( "multiBoxOverlayRenderer" );
				overlayRendererField.setAccessible( true );

				final MultiBoxOverlayRenderer overlayRenderP = new MultiBoxOverlayRenderer( DEFAULT_WIDTH, DEFAULT_HEIGHT );
				final MultiBoxOverlayRenderer overlayRenderQ = new MultiBoxOverlayRenderer( DEFAULT_WIDTH, DEFAULT_HEIGHT );
				
				// TODO hopefully I won't' need reflection any more
				final Field boxField = overlayRenderP.getClass().getDeclaredField( "box" );
				boxField.setAccessible( true );
				boxField.set( overlayRenderP, new MultiBoxOverlay2d() );
				boxField.set( overlayRenderQ, new MultiBoxOverlay2d() );
				boxField.setAccessible( false );

				overlayRendererField.set( viewerP, overlayRenderP );
				overlayRendererField.set( viewerQ, overlayRenderQ );
				overlayRendererField.setAccessible( false );

			}
			catch ( final Exception e )
			{
				e.printStackTrace();
			}
		}

		try
		{
			// TODO hopefully I won't' need reflection any more
			final Class< ViewerPanel > c_vp = ViewerPanel.class;
			final Field sourceInfoOverlayRendererField = c_vp.getDeclaredField( "sourceInfoOverlayRenderer" );
			sourceInfoOverlayRendererField.setAccessible( true );
			sourceInfoOverlayRendererField.set( viewerP, new BigWarpSourceOverlayRenderer() );
			sourceInfoOverlayRendererField.set( viewerQ, new BigWarpSourceOverlayRenderer() );
			sourceInfoOverlayRendererField.setAccessible( false );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}

		viewerP.setNumDim( ndims );
		viewerQ.setNumDim( ndims );

		activeSourcesDialogP = new VisibilityAndGroupingDialog( viewerFrameP, viewerP.getVisibilityAndGrouping() );
		activeSourcesDialogP.setTitle( "visibility and grouping ( moving )" );
		activeSourcesDialogQ = new VisibilityAndGroupingDialog( viewerFrameQ, viewerQ.getVisibilityAndGrouping() );
		activeSourcesDialogQ.setTitle( "visibility and grouping ( fixed )" );
		
		// set warp mag source to inactive at the start
		viewerP.state().setSourceActive( warpMagSource, false );
		viewerQ.state().setSourceActive( warpMagSource, false );
		// set warp grid source to inactive at the start
		viewerP.state().setSourceActive( gridSource, false );
		viewerQ.state().setSourceActive( gridSource, false );

		overlayP = new BigWarpOverlay( viewerP, landmarkPanel );
		overlayQ = new BigWarpOverlay( viewerQ, landmarkPanel );
		viewerP.addOverlay( overlayP );
		viewerQ.addOverlay( overlayQ );

		solverThread = new SolveThread( this );
		solverThread.start();
		
		dragOverlayP = new BigWarpDragOverlay( this, viewerP, solverThread );
		dragOverlayQ = new BigWarpDragOverlay( this, viewerQ, solverThread );
		viewerP.addDragOverlay( dragOverlayP );
		viewerQ.addDragOverlay( dragOverlayQ );

		landmarkPopupMenu = new LandmarkPointMenu( this );
		landmarkPopupMenu.setupListeners();

		setupAssignments = new SetupAssignments( new ArrayList<>( converterSetups ), 0, 65535 );

		brightnessDialog = new BrightnessDialog( landmarkFrame, setupAssignments );
		helpDialog = new HelpDialog( landmarkFrame );

		transformSelector = new TransformTypeSelectDialog( landmarkFrame, this );
		
		warpVisDialog = new WarpVisFrame( viewerFrameQ, this ); // dialogs have
																// to be
																// constructed
																// before action
																// maps are made


		WarpNavigationActions.installActionBindings( getViewerFrameP().getKeybindings(), viewerFrameP, keyProperties, ( ndims == 2 ) );
		BigWarpActions.installActionBindings( getViewerFrameP().getKeybindings(), this, keyProperties );

		WarpNavigationActions.installActionBindings( getViewerFrameQ().getKeybindings(), viewerFrameQ, keyProperties, ( ndims == 2 ) );
		BigWarpActions.installActionBindings( getViewerFrameQ().getKeybindings(), this, keyProperties );

		BigWarpActions.installLandmarkPanelActionBindings( landmarkFrame.getKeybindings(), this, landmarkTable, keyProperties );

		// this call has to come after the actions are set
		warpVisDialog.setActions();

		setUpViewerMenu( viewerFrameP );
		setUpViewerMenu( viewerFrameQ );
		setUpLandmarkMenus();

		/* Set the locations of frames */
		final Point viewerFramePloc = getViewerFrameP().getLocation();
		viewerFramePloc.setLocation( viewerFramePloc.x + DEFAULT_WIDTH, viewerFramePloc.y );
		getViewerFrameQ().setLocation( viewerFramePloc );
		viewerFramePloc.setLocation( viewerFramePloc.x + DEFAULT_WIDTH, viewerFramePloc.y );
		landmarkFrame.setLocation( viewerFramePloc );

		landmarkClickListenerP = new MouseLandmarkListener( this.viewerP );
		landmarkClickListenerQ = new MouseLandmarkListener( this.viewerQ );

		// have to be safe here and use 3dim point for both 3d and 2d
		currentLandmark = new RealPoint( 3 );
		inLandmarkMode = false;
		setupKeyListener();

		// set initial transforms so data are visible
		InitializeViewerState.initTransform( viewerP );
		InitializeViewerState.initTransform( viewerQ );

		initialViewP = new AffineTransform3D();
		initialViewQ = new AffineTransform3D();
		viewerP.state().getViewerTransform( initialViewP );
		viewerQ.state().getViewerTransform( initialViewQ );

		// set colors and min/max ranges
		data.transferChannelSettings( viewerFrameP );
		data.transferChannelSettings( viewerFrameQ );

		// set brightness contrast to appropriate values
		InitializeViewerState.initBrightness( 0.001, 0.999, viewerP.state(), viewerFrameP.getConverterSetups() );
		InitializeViewerState.initBrightness( 0.001, 0.999, viewerQ.state(), viewerFrameQ.getConverterSetups() );

		viewerFrameP.setVisible( true );
		viewerFrameQ.setVisible( true );

		landmarkFrame.pack();
		landmarkFrame.setVisible( true );

		checkBoxInputMaps();

		// file selection
		fileFrame = new JFrame( "Select File" );
		fileDialog = new FileDialog( fileFrame );
		lastDirectory = null;

		// default to linear interpolation
		fileFrame.setVisible( false );

		// add focus listener
		//new BigwarpFocusListener( this );

		bookmarks = new Bookmarks();
		bookmarkEditorP = new BookmarksEditor( viewerP, viewerFrameP.getKeybindings(), bookmarks );
		bookmarkEditorQ = new BookmarksEditor( viewerQ, viewerFrameQ.getKeybindings(), bookmarks );

		// add landmark mode listener
		//addKeyEventPostProcessor( new LandmarkModeListener() );
	}

	public int numDimensions()
	{
		return ndims;
	}

	public void addKeyEventPostProcessor( final KeyEventPostProcessor ke )
	{
		keyEventPostProcessorSet.add( ke );
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor( ke );
	}

	public void removeKeyEventPostProcessor( final KeyEventPostProcessor ke )
	{
		keyEventPostProcessorSet.remove( ke );
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventPostProcessor( ke );
	}

	public void closeAll()
	{
		final ArrayList< KeyEventPostProcessor > ks = new ArrayList< KeyEventPostProcessor >( keyEventPostProcessorSet );
		for ( final KeyEventPostProcessor ke : ks )
			removeKeyEventPostProcessor( ke );

		repeatedKeyEventsFixer.remove();

		viewerFrameP.setVisible( false );
		viewerFrameQ.setVisible( false );
		landmarkFrame.setVisible( false );

		viewerFrameP.getViewerPanel().stop();
		viewerFrameQ.getViewerPanel().stop();

		viewerFrameP.dispose();
		viewerFrameQ.dispose();
		landmarkFrame.dispose();
	}

	public void setUpdateWarpOnChange( final boolean updateWarpOnPtChange )
	{
		this.updateWarpOnPtChange = updateWarpOnPtChange;
	}

	public void toggleUpdateWarpOnChange()
	{
		this.updateWarpOnPtChange = !this.updateWarpOnPtChange;

		if ( updateWarpOnPtChange )
		{
			message.showMessage( "Always estimate transform on change" );

			// toggleAlwaysWarpMenuP.setText( "Toggle always warp off" );
			// toggleAlwaysWarpMenuQ.setText( "Toggle always warp off" );
		}
		else
		{
			message.showMessage( "Estimate transform on request only" );

			// toggleAlwaysWarpMenuP.setText( "Warp on every point change" );
			// toggleAlwaysWarpMenuQ.setText( "Toggle always warp on" );
		}
	}

	public boolean isInLandmarkMode()
	{
		return inLandmarkMode;
	}

	public void toggleInLandmarkMode()
	{
		setInLandmarkMode( !inLandmarkMode );
	}

	public boolean isUpdateWarpOnChange()
	{
		return updateWarpOnPtChange;
	}

	public void invertPointCorrespondences()
	{
		landmarkModel = landmarkModel.invert();
		landmarkPanel.setTableModel( landmarkModel );
	}

	public File getLastDirectory()
	{
		return lastDirectory;
	}

	public void setLastDirectory( final File dir )
	{
		this.lastDirectory = dir;
	}

	public void setSpotColor( final Color c )
	{
		viewerSettings.setSpotColor( c );
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
	}

	protected void setUpViewerMenu( final BigWarpViewerFrame vframe )
	{
		// TODO setupviewermenu

		final ActionMap actionMap = vframe.getKeybindings().getConcatenatedActionMap();

		final JMenuBar viewerMenuBar = new JMenuBar();

		JMenu fileMenu = new JMenu( "File" );
		viewerMenuBar.add( fileMenu );

		final JMenuItem openItem = new JMenuItem( actionMap.get( BigWarpActions.LOAD_LANDMARKS ) );
		openItem.setText( "Import landmarks" );
		fileMenu.add( openItem );

		final JMenuItem saveItem = new JMenuItem( actionMap.get( BigWarpActions.SAVE_LANDMARKS ));
		saveItem.setText( "Export landmarks" );
		fileMenu.add( saveItem );

		fileMenu.addSeparator();
		final JMenuItem miLoadSettings = new JMenuItem( actionMap.get( BigWarpActions.LOAD_SETTINGS ) );
		miLoadSettings.setText( "Load settings" );
		fileMenu.add( miLoadSettings );

		final JMenuItem miSaveSettings = new JMenuItem( actionMap.get( BigWarpActions.SAVE_SETTINGS ) );
		miSaveSettings.setText( "Save settings" );
		fileMenu.add( miSaveSettings );

		if( ij != null )
		{
			fileMenu.addSeparator();
			final JMenuItem exportToImagePlus = new JMenuItem( actionMap.get( BigWarpActions.EXPORT_IP ) );
			exportToImagePlus.setText( "Export as ImagePlus" );
			fileMenu.add( exportToImagePlus );
			
			final JMenuItem exportWarpField = new JMenuItem( actionMap.get( BigWarpActions.EXPORT_WARP ) );
			exportWarpField.setText( "Export warp field" );
			fileMenu.add( exportWarpField );
		}

		final JMenu settingsMenu = new JMenu( "Settings" );
		viewerMenuBar.add( settingsMenu );

		final JMenuItem toggleAlwaysWarpMenu;
		if ( vframe.isMoving() )
		{
			toggleAlwaysWarpMenuP = new JMenuItem( new BigWarpActions.ToggleAlwaysEstimateTransformAction( "", vframe )  );
			toggleAlwaysWarpMenu = toggleAlwaysWarpMenuP;
		}
		else
		{
			toggleAlwaysWarpMenuQ = new JMenuItem( new BigWarpActions.ToggleAlwaysEstimateTransformAction( "", vframe ) );
			toggleAlwaysWarpMenu = toggleAlwaysWarpMenuQ;
		}

		toggleAlwaysWarpMenu.setText( "Toggle warp on drag" );
		settingsMenu.add( toggleAlwaysWarpMenu );

		final JMenuItem miBrightness = new JMenuItem( actionMap.get( BigWarpActions.BRIGHTNESS_SETTINGS ) );
		miBrightness.setText( "Brightness & Color" );
		settingsMenu.add( miBrightness );

		/* Warp Visualization */
		final JMenuItem warpVisMenu = new JMenuItem( actionMap.get( BigWarpActions.SHOW_WARPTYPE_DIALOG ) );
		warpVisMenu.setText( "BigWarp Options" );
		settingsMenu.add( warpVisMenu );

		vframe.setJMenuBar( viewerMenuBar );

		final JMenu helpMenu = new JMenu( "Help" );
		viewerMenuBar.add( helpMenu );

		final JMenuItem miHelp = new JMenuItem( actionMap.get( BigWarpActions.SHOW_HELP ) );
		miHelp.setText( "Show Help Menu" );
		helpMenu.add( miHelp );
	}

	protected void setupImageJExportOption()
	{
		final ActionMap actionMap = landmarkFrame.getKeybindings().getConcatenatedActionMap();

		final JMenuItem exportToImagePlus = new JMenuItem( actionMap.get( BigWarpActions.EXPORT_IP ) );
		exportToImagePlus.setText( "Export as ImagePlus" );
		fileMenu.add( exportToImagePlus );
		
		final JMenuItem exportWarpField = new JMenuItem( actionMap.get( BigWarpActions.EXPORT_WARP ) );
		exportWarpField.setText( "Export warp field" );
		fileMenu.add( exportWarpField );

	}

	public void exportAsImagePlus( boolean virtual )
	{
		exportAsImagePlus( virtual, "" );
	}

	@Deprecated
	public void saveMovingImageToFile()
	{
		System.out.println( "saveMovingImageToFile" );
		final JFileChooser fileChooser = new JFileChooser( getLastDirectory() );
		File proposedFile = new File( sources.get( movingSourceIndexList[ 0 ] ).getSpimSource().getName() );

		fileChooser.setSelectedFile( proposedFile );
		final int returnVal = fileChooser.showSaveDialog( null );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
		{
			proposedFile = fileChooser.getSelectedFile();
			try
			{
				System.out.println("save warped image");
				exportAsImagePlus( false, proposedFile.getCanonicalPath() );
			} catch ( final IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	public File saveMovingImageXml( )
	{
		return saveMovingImageXml( null );
	}

	public File saveMovingImageXml( String proposedFilePath )
	{
		System.out.println( "saveWarpedMovingImageXml" );

		if ( movingSpimData == null )
		{
			IJ.log("Cannot save warped moving image XML, because the input image was not a BDV/XML.");
			return null;
		}

		final AffineTransform3D bigWarpTransform = getMovingToFixedTransformAsAffineTransform3D();

		System.out.println( "bigWarp transform as affine 3d: " + bigWarpTransform.toString() );

		movingSpimData.getViewRegistrations().getViewRegistration( 0, 0 ).preconcatenateTransform( new ViewTransformAffine( "Big Warp: " + transformType, bigWarpTransform ) );

		File proposedFile;
		if ( proposedFilePath == null )
		{
			final JFileChooser fileChooser = new JFileChooser( movingImageXml.getParent() );
			proposedFile = new File( movingImageXml.getName().replace( ".xml", "-bigWarp.xml" ) );

			fileChooser.setSelectedFile( proposedFile );
			final int returnVal = fileChooser.showSaveDialog( null );
			if ( returnVal == JFileChooser.APPROVE_OPTION )
				proposedFile = fileChooser.getSelectedFile();
			else
				return null;
		}
		else
		{
			proposedFile = new File( proposedFilePath );
		}

		try
		{
			System.out.println("save warped image xml");
			new XmlIoSpimData().save( movingSpimData, proposedFile.getAbsolutePath() );
		} catch ( SpimDataException e )
		{
			e.printStackTrace();
		}

		return proposedFile;
	}

	public AffineTransform3D getMovingToFixedTransformAsAffineTransform3D()
	{
		double[][] affine3DMatrix = new double[ 3 ][ 4 ];
		double[][] affine2DMatrix = new double[ 2 ][ 3 ];

		if ( currentTransform == null )
		{
			return null;
		}

		final InvertibleCoordinateTransform transform =
				( ( WrappedCoordinateTransform ) currentTransform ).ct_inv;

		if ( transform instanceof AffineModel3D )
		{
			((AffineModel3D)transform).toMatrix( affine3DMatrix );
		}
		else if ( transform instanceof SimilarityModel3D )
		{
			((SimilarityModel3D)transform).toMatrix( affine3DMatrix );
		}
		else if ( transform instanceof RigidModel3D )
		{
			((RigidModel3D)transform).toMatrix( affine3DMatrix );
		}
		else if ( transform instanceof TranslationModel3D )
		{
			((TranslationModel3D)transform).toMatrix( affine3DMatrix );
		}
		else if ( transform instanceof AffineModel2D )
		{
			((AffineModel2D)transform).toMatrix( affine2DMatrix );
			affineMatrix2DtoAffineMatrix3D( affine2DMatrix, affine3DMatrix );
		}
		else if ( transform instanceof SimilarityModel2D )
		{
			((SimilarityModel2D)transform).toMatrix( affine2DMatrix );
			affineMatrix2DtoAffineMatrix3D( affine2DMatrix, affine3DMatrix );
		}
		else if ( transform instanceof RigidModel2D )
		{
			((RigidModel2D)transform).toMatrix( affine2DMatrix );
			affineMatrix2DtoAffineMatrix3D( affine2DMatrix, affine3DMatrix );
		}
		else if ( transform instanceof TranslationModel2D )
		{
			((TranslationModel2D)transform).toMatrix( affine2DMatrix );
			affineMatrix2DtoAffineMatrix3D( affine2DMatrix, affine3DMatrix );
		}
		else
		{
			IJ.error("Cannot convert transform of type " + transform.getClass().toString()
			+ "\nto an 3D affine tranform.");
			return null;
		}

		final AffineTransform3D bigWarpTransform = new AffineTransform3D();
		bigWarpTransform.set( affine3DMatrix );
		return bigWarpTransform;
	}

	private void affineMatrix2DtoAffineMatrix3D( double[][] affine2DMatrix,  double[][] affine3DMatrix )
	{
		for ( int d = 0; d < 2; ++d )
		{
			affine3DMatrix[ d ][ 0 ] = affine2DMatrix[ d ][ 0 ];
			affine3DMatrix[ d ][ 1 ] = affine2DMatrix[ d ][ 1 ];
			affine3DMatrix[ d ][ 3 ] = affine2DMatrix[ d ][ 2 ];
		}
	}

	public void exportAsImagePlus( boolean virtual, String path )
	{
		if( ij == null )
			return;

		final GenericDialog gd = new GenericDialog( "Apply Big Warp transform" );

		gd.addMessage( "Field of view and resolution:" );
		gd.addChoice( "Resolution", 
				new String[]{ ApplyBigwarpPlugin.TARGET, ApplyBigwarpPlugin.MOVING, ApplyBigwarpPlugin.SPECIFIED },
				ApplyBigwarpPlugin.TARGET );

		gd.addChoice( "Field of view", 
				new String[]{ ApplyBigwarpPlugin.TARGET, 
						ApplyBigwarpPlugin.MOVING_WARPED,
						ApplyBigwarpPlugin.UNION_TARGET_MOVING,
						ApplyBigwarpPlugin.LANDMARK_POINTS, 
						ApplyBigwarpPlugin.LANDMARK_POINT_CUBE_PIXEL,
						ApplyBigwarpPlugin.LANDMARK_POINT_CUBE_PHYSICAL,
						ApplyBigwarpPlugin.SPECIFIED_PIXEL,
						ApplyBigwarpPlugin.SPECIFIED_PHYSICAL
						},
				ApplyBigwarpPlugin.TARGET );

		gd.addStringField( "point filter", "" );
		
		gd.addMessage( "Resolution");
		gd.addNumericField( "x", 1.0, 4 );
		gd.addNumericField( "y", 1.0, 4 );
		gd.addNumericField( "z", 1.0, 4 );
		
		gd.addMessage( "Offset");
		gd.addNumericField( "x", 0.0, 4 );
		gd.addNumericField( "y", 0.0, 4 );
		gd.addNumericField( "z", 0.0, 4 );
		
		gd.addMessage( "Field of view");
		gd.addNumericField( "x", -1, 0 );
		gd.addNumericField( "y", -1, 0 );
		gd.addNumericField( "z", -1, 0 );
		
		gd.addMessage( "Output options");
		gd.addChoice( "Interpolation", new String[]{ "Nearest Neighbor", "Linear" }, "Linear" );
		
		gd.addMessage( "Virtual: fast to display,\n"
				+ "low memory requirements,\nbut slow to navigate" );
		gd.addCheckbox( "virtual?", false );
		int defaultCores = (int)Math.ceil( Runtime.getRuntime().availableProcessors()/4);
		gd.addNumericField( "threads", defaultCores, 0 );

		gd.showDialog();

		if ( gd.wasCanceled() )
			return;
		
		String resolutionOption = gd.getNextChoice();
		String fieldOfViewOption = gd.getNextChoice();
		String fieldOfViewPointFilter = gd.getNextString();
		
		double[] resolutionSpec = new double[ 3 ];
		resolutionSpec[ 0 ] = gd.getNextNumber();
		resolutionSpec[ 1 ] = gd.getNextNumber();
		resolutionSpec[ 2 ] = gd.getNextNumber();
		
		double[] offsetSpec = new double[ 3 ];
		offsetSpec[ 0 ] = gd.getNextNumber();
		offsetSpec[ 1 ] = gd.getNextNumber();
		offsetSpec[ 2 ] = gd.getNextNumber();
		
		double[] fovSpec = new double[ 3 ];
		fovSpec[ 0 ] = gd.getNextNumber();
		fovSpec[ 1 ] = gd.getNextNumber();
		fovSpec[ 2 ] = gd.getNextNumber();

		String interpType = gd.getNextChoice();
		boolean isVirtual = gd.getNextBoolean();
		int nThreads = (int)gd.getNextNumber();
		
		final Interpolation interp;
		if( interpType.equals( "Nearest Neighbor" ))
			interp = Interpolation.NEARESTNEIGHBOR;
		else
			interp = Interpolation.NLINEAR;

		double[] res = ApplyBigwarpPlugin.getResolution( this.data, resolutionOption, resolutionSpec );
		List<Interval> outputIntervalList = ApplyBigwarpPlugin.getPixelInterval( this.data, this.landmarkModel, fieldOfViewOption, 
				fieldOfViewPointFilter, fovSpec, offsetSpec, res );

		final List<String> matchedPtNames = new ArrayList<>();
		if( outputIntervalList.size() > 1 )
			ApplyBigwarpPlugin.fillMatchedPointNames( matchedPtNames, getLandmarkPanel().getTableModel(), fieldOfViewPointFilter );
		
		// export has to be treated differently if we're doing fov's around
		// landmark centers (because multiple images can be exported this way )
		if( matchedPtNames.size() > 0 )
		{
			BigwarpLandmarkSelectionPanel<T> selection = new BigwarpLandmarkSelectionPanel<>( 
					data, sources, fieldOfViewOption,
					outputIntervalList, matchedPtNames, interp,
					offsetSpec, res, isVirtual, nThreads, 
					progressWriter );
		}
		else
		{
			// export
			ApplyBigwarpPlugin.runExport( data, sources, fieldOfViewOption,
					outputIntervalList, matchedPtNames, interp,
					offsetSpec, res, isVirtual, nThreads, 
					progressWriter, true );
		}
	}

	public void exportWarpField()
	{
		if ( ij == null )
			return;

		final GenericDialog gd = new GenericDialog( "BigWarp to Deformation" );
		gd.addMessage( "Deformation field export:" );
		gd.addCheckbox( "Ignore affine part", false );
		gd.addNumericField( "threads", 1, 0 );
		gd.showDialog();

		if ( gd.wasCanceled() )
			return;

		boolean ignoreAffine = gd.getNextBoolean();
		int nThreads = ( int ) gd.getNextNumber();

		RandomAccessibleInterval< ? > tgtInterval = sources.get( targetSourceIndexList[ 0 ] ).getSpimSource().getSource( 0, 0 );

		int ndims = landmarkModel.getNumdims();
		long[] dims;
		if ( ndims <= 2 )
		{
			dims = new long[ 3 ];
			dims[ 0 ] = tgtInterval.dimension( 0 );
			dims[ 1 ] = tgtInterval.dimension( 1 );
			dims[ 2 ] = 2;
		}
		else
		{
			dims = new long[ 4 ];
			dims[ 0 ] = tgtInterval.dimension( 0 );
			dims[ 1 ] = tgtInterval.dimension( 1 );
			dims[ 2 ] = 3;
			dims[ 3 ] = tgtInterval.dimension( 2 );
		}

		double[] resolutions = new double[ 3 ];
		VoxelDimensions voxelDim = sources.get( targetSourceIndexList[ 0 ] ).getSpimSource().getVoxelDimensions();
		voxelDim.dimensions( resolutions );

		AffineTransform pixToPhysical = new AffineTransform( ndims );
		pixToPhysical.set( resolutions[ 0 ], 0, 0 );
		pixToPhysical.set( resolutions[ 1 ], 1, 1 );
		if ( ndims > 2 )
			pixToPhysical.set( resolutions[ 2 ], 2, 2 );

		FloatImagePlus< FloatType > deformationField = ImagePlusImgs.floats( dims );

		RandomAccessibleInterval< FloatType > dfieldPerm;
		if ( ndims > 2 )
			dfieldPerm = Views.permute( deformationField, 2, 3 );
		else
			dfieldPerm = deformationField;


		ImagePlus dfieldIp = deformationField.getImagePlus();
		dfieldIp.getCalibration().pixelWidth = resolutions[ 0 ];
		dfieldIp.getCalibration().pixelHeight = resolutions[ 1 ];
		dfieldIp.getCalibration().pixelDepth = resolutions[ 2 ];

		ThinPlateR2LogRSplineKernelTransform tpsRaw = landmarkModel.getTransform();
		ThinPlateR2LogRSplineKernelTransform tpsUseMe = tpsRaw;
		if ( ignoreAffine )
			tpsUseMe = new ThinPlateR2LogRSplineKernelTransform( tpsRaw.getSourceLandmarks(), null, null, tpsRaw.getKnotWeights() );

		ThinplateSplineTransform tps = new ThinplateSplineTransform( tpsUseMe );
		BigWarpToDeformationFieldPlugIn.fromRealTransform( tps, pixToPhysical, dfieldPerm, nThreads );

		String title = "bigwarp dfield";
		if ( ignoreAffine )
			title += " (no affine)";

		dfieldIp.setTitle( title );
		dfieldIp.show();
	}

	protected void setUpLandmarkMenus()
	{
		final ActionMap actionMap = landmarkFrame.getKeybindings().getConcatenatedActionMap();

		final JMenuBar landmarkMenuBar = new JMenuBar();
		fileMenu = new JMenu( "File" );
		final JMenuItem openItem = new JMenuItem( actionMap.get( BigWarpActions.LOAD_LANDMARKS ) );
		openItem.setText( "Import landmarks" );
		fileMenu.add( openItem );

		final JMenuItem saveItem = new JMenuItem( actionMap.get( BigWarpActions.SAVE_LANDMARKS ));
		saveItem.setText( "Export landmarks" );
		fileMenu.add( saveItem );

		fileMenu.addSeparator();
		final JMenuItem exportImageItem = new JMenuItem( "Export Moving Image" );

		landmarkMenuBar.add( fileMenu );
		landmarkFrame.setJMenuBar( landmarkMenuBar );
		//	exportMovingImage( file, state, progressWriter );

		final JMenuItem saveExport = new JMenuItem( actionMap.get( BigWarpActions.SAVE_WARPED ) );
		saveExport.setText( "Save warped image" );
		fileMenu.add( saveExport );

		final JMenu landmarkMenu = new JMenu( "Landmarks" );
		final JMenuItem landmarkGridItem = new JMenuItem( actionMap.get( BigWarpActions.LANDMARK_GRID_DIALOG ) );
		landmarkGridItem.setText( "Build landmark grid" );
		landmarkMenu.add( landmarkGridItem );

		landmarkMenuBar.add( landmarkMenu );


		if( ij != null )
			setupImageJExportOption();
	}

	public Bookmarks getBookmarks()
	{
		return bookmarks;
	}

	public BigWarpViewerFrame getViewerFrameP()
	{
		return viewerFrameP;
	}

	public BigWarpViewerFrame getViewerFrameQ()
	{
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

	public SetupAssignments getSetupAssignments()
	{
		return setupAssignments;
	}

	public BigWarpData<T> getData()
	{
		return data;
	}

	public List< SourceAndConverter< T > > getSources()
	{
		return sources;
	}

	public BigWarpLandmarkFrame getLandmarkFrame()
	{
		return landmarkFrame;
	}

	public BigWarpLandmarkPanel getLandmarkPanel()
	{
		return landmarkPanel;
	}

	public String transformToString()
	{
		if( currentTransform == null )
		{
			return "(identity)";
		}

		String s = "";
		if ( currentTransform instanceof InverseRealTransform )
		{
			s = ( ( InverseRealTransform ) currentTransform ).toString();
		}
		else if( currentTransform instanceof WrappedCoordinateTransform )
		{
			s = (( WrappedCoordinateTransform ) currentTransform).ct.toString();
		}
		else if( currentTransform instanceof Wrapped2DTransformAs3D )
		{
			s = ( ( Wrapped2DTransformAs3D) currentTransform ).toString();
		}
		else
		{
			s = ( ( WrappedIterativeInvertibleRealTransform<?> ) currentTransform ).getTransform().toString();
		}
		progressWriter.out().println( s );
		return s;
	}

	public void printAffine()
	{
		if( ij != null )
		{
			IJ.log( affineToString() );
			IJ.log( "" + affine3d() );
		}
		else
		{
			System.out.println( affineToString() );
			System.out.println( affine3d() );
		}
	}

	/**
	 * Returns an AffineGet that represents the the transform if the transform
	 * is linear, or is the affine part of the transform if it is non-linear.
	 * 
	 * The output is an AffineTransform2D or AffineTransform3D
	 * 
	 * @return the affine transform
	 */
	public AffineGet affine()
	{
		if( ndims == 2 )
			return affine2d();
		else 
			return affine3d();
	}

	/**
	 * Returns an AffineTransform2D that represents the the transform if the transform
	 * is linear, or is the affine part of the transform if it is non-linear.
	 * 
	 * Returns a valid transform only if the estimated transform is 2d.  Returns null
	 * if the transformation is 3d.
	 * 
	 * @return the affine transform
	 */
	public AffineTransform2D affine2d()
	{
		if( ndims != 2 )
		{
			System.err.println( "affine2d() only works for 2d transformations." );
			return null;
		}

		AffineTransform2D out = new AffineTransform2D();
		if( getTransformType().equals( TransformTypeSelectDialog.TPS ))
		{
			double[][] tpsAffine = getTpsBase().getAffine();
			double[] translation = getTpsBase().getTranslation();

			double[] affine = new double[ 6 ];
			affine[ 0 ] = 1 + tpsAffine[ 0 ][ 0 ];
			affine[ 1 ] = tpsAffine[ 0 ][ 1 ];
			affine[ 2 ] = translation[ 0 ];

			affine[ 3 ] = tpsAffine[ 1 ][ 0 ];
			affine[ 4 ] = 1 + tpsAffine[ 1 ][ 1 ];
			affine[ 5 ] = translation[ 1 ];

			out.set( affine );
		}
		else
		{
			AbstractAffineModel2D model2d = (AbstractAffineModel2D)getCoordinateTransform();

			double[][] mtx = new double[2][3];
			model2d.toMatrix( mtx );

			double[] affine = new double[ 6 ];
			affine[ 0 ] = mtx[ 0 ][ 0 ];
			affine[ 1 ] = mtx[ 0 ][ 1 ];
			affine[ 2 ] = mtx[ 0 ][ 2 ];

			affine[ 3 ] = mtx[ 1 ][ 0 ];
			affine[ 4 ] = mtx[ 1 ][ 1 ];
			affine[ 5 ] = mtx[ 1 ][ 2 ];

			out.set( affine );
		}
		return out;
	}
	
	/**
	 * Returns an AffineTransform3D that represents the the transform if the transform
	 * is linear, or is the affine part of the transform if it is non-linear.
	 * 
	 * Returns a valid transform even if the estimated transformation is 2d.
	 * 
	 * @return the affine transform
	 */
	public AffineTransform3D affine3d()
	{
		AffineTransform3D out = new AffineTransform3D();
		if( getTransformType().equals( TransformTypeSelectDialog.TPS ))
		{
			double[][] tpsAffine = getTpsBase().getAffine();
			double[] translation = getTpsBase().getTranslation();

			double[] affine = new double[ 12 ];
			if( ndims == 2 )
			{
				affine[ 0 ] = 1 + tpsAffine[ 0 ][ 0 ];
				affine[ 1 ] = tpsAffine[ 0 ][ 1 ];
				// dont set affine 2
				affine[ 3 ] = translation[ 0 ];

				affine[ 4 ] = tpsAffine[ 1 ][ 0 ];
				affine[ 5 ] = 1 + tpsAffine[ 1 ][ 1 ];
				// dont set 6
				affine[ 7 ] = translation[ 1 ];

				// dont set 8,9,11
				affine[ 10 ] = 1.0;
			}
			else
			{
				affine[ 0 ] = 1 + tpsAffine[ 0 ][ 0 ];
				affine[ 1 ] = tpsAffine[ 0 ][ 1 ];
				affine[ 2 ] = tpsAffine[ 0 ][ 2 ];
				affine[ 3 ] = translation[ 0 ];

				affine[ 4 ] = tpsAffine[ 1 ][ 0 ];
				affine[ 5 ] = 1 + tpsAffine[ 1 ][ 1 ];
				affine[ 6 ] = tpsAffine[ 1 ][ 2 ];
				affine[ 7 ] = translation[ 1 ];

				affine[ 8 ] = tpsAffine[ 2 ][ 0 ];
				affine[ 9 ] = tpsAffine[ 2 ][ 1 ];
				affine[ 10 ] = 1 + tpsAffine[ 2 ][ 2 ];
				affine[ 11 ] = translation[ 2 ];
			}

			out.set( affine );
		}
		else
		{
			if( ndims == 2 )
			{
				AbstractAffineModel2D model2d = (AbstractAffineModel2D)getCoordinateTransform();

				double[][] mtx = new double[2][3];
				model2d.toMatrix( mtx );

				double[] affine = new double[ 12 ];
				affine[ 0 ] = mtx[ 0 ][ 0 ];
				affine[ 1 ] = mtx[ 0 ][ 1 ];
				// dont set affine 2
				affine[ 3 ] = mtx[ 0 ][ 2 ];

				affine[ 4 ] = mtx[ 1 ][ 0 ];
				affine[ 5 ] = mtx[ 1 ][ 1 ];
				// dont set affine 6
				affine[ 7 ] = mtx[ 1 ][ 2 ];

				// dont set affines 8,9,11
				affine[ 10 ] = 1.0;

				out.set( affine );

			}
			else if( ndims == 3 )
			{
				AbstractAffineModel3D model3d = (AbstractAffineModel3D)getCoordinateTransform();

				double[][] mtx = new double[3][4];
				model3d.toMatrix( mtx );

				double[] affine = new double[ 12 ];
				affine[ 0 ] = mtx[ 0 ][ 0 ];
				affine[ 1 ] = mtx[ 0 ][ 1 ];
				affine[ 2 ] = mtx[ 0 ][ 2 ];
				affine[ 3 ] = mtx[ 0 ][ 3 ];

				affine[ 4 ] = mtx[ 1 ][ 0 ];
				affine[ 5 ] = mtx[ 1 ][ 1 ];
				affine[ 6 ] = mtx[ 1 ][ 2 ];
				affine[ 7 ] = mtx[ 1 ][ 3 ];

				affine[ 8 ] = mtx[ 2 ][ 0 ];
				affine[ 9 ] = mtx[ 2 ][ 1 ];
				affine[ 10 ] = mtx[ 2 ][ 2 ];
				affine[ 11 ] = mtx[ 2 ][ 3 ];

				out.set( affine );
			}
			else
			{
				System.err.println( "Only support 2d and 3d transformations." );
				return null;
			}
		}
		return out;
	}
	
	public AbstractAffineModel2D getModel2D( final String transformType )
	{ 
		switch( transformType ){
		case TransformTypeSelectDialog.AFFINE:
			return ((AffineModel2D)((WrappedCoordinateTransform)currentTransform).getTransform());
		case TransformTypeSelectDialog.SIMILARITY:
			return ((SimilarityModel2D)((WrappedCoordinateTransform)currentTransform).getTransform());
		case TransformTypeSelectDialog.ROTATION:
			return ((RigidModel2D)((WrappedCoordinateTransform)currentTransform).getTransform());
		case TransformTypeSelectDialog.TRANSLATION:
			return ((TranslationModel2D)((WrappedCoordinateTransform)currentTransform).getTransform());
		}

		return null;
	}

	public AbstractAffineModel3D getModel3D( final String transformType )
	{ 
		switch( transformType ){
		case TransformTypeSelectDialog.AFFINE:
			return ((AffineModel3D)((WrappedCoordinateTransform)currentTransform).getTransform());
		case TransformTypeSelectDialog.SIMILARITY:
			return ((SimilarityModel3D)((WrappedCoordinateTransform)currentTransform).getTransform());
		case TransformTypeSelectDialog.ROTATION:
			return ((RigidModel3D)((WrappedCoordinateTransform)currentTransform).getTransform());
		case TransformTypeSelectDialog.TRANSLATION:
			return ((TranslationModel3D)((WrappedCoordinateTransform)currentTransform).getTransform());
		}

		return null;
	}

	public String affineToString()
	{
		String s = "";
		if( getTransformType().equals( TransformTypeSelectDialog.TPS ))
		{
			double[][] affine = affinePartOfTpsHC();
			for( int r = 0; r < affine.length; r++ )
			{
				s += Arrays.toString(affine[r]).replaceAll("\\[|\\]||\\s", "");
				if( r < affine.length - 1 )
					s += "\n";
			}
		}
		else
			s = (( WrappedCoordinateTransform ) currentTransform).ct.toString();

		return s;
	}

	/**
	 * Returns the affine part of the thin plate spline model, 
	 * as a matrix in homogeneous coordinates.
	 * 
	 * double[i][:] contains the i^th row of the matrix.
	 * 
	 * @return the matrix as a double array
	 */
	public double[][] affinePartOfTpsHC()
	{
		int nr = 3;
		int nc = 4;
		double[][] mtx = null;
		if( options.is2d )
		{
			nr = 2;
			nc = 3;
			mtx = new double[2][3];
		}
		else
		{
			mtx = new double[3][4];
		}

		double[][] tpsAffine = landmarkModel.getTransform().getAffine();
		double[] translation = landmarkModel.getTransform().getTranslation();
		for( int r = 0; r < nr; r++ )
			for( int c = 0; c < nc; c++ )
			{
				if( c == (nc-1))
				{
					mtx[r][c] = translation[r];
				}
				else if( r == c )
				{
					/* the affine doesn't contain the identity "part" of the affine.
					 *	i.e., the tps builds the affine A such that
					 *	y = x + Ax 
					 *  o
					 *  y = ( A + I )x
					 */
					mtx[r][c] = 1 + tpsAffine[ r ][ c ];
				}
				else
				{
					mtx[r][c] = tpsAffine[ r ][ c ];
				}
			}
		return mtx;
	}

	public synchronized void setInLandmarkMode( final boolean inLmMode )
	{

		if( inLandmarkMode == inLmMode )
			return;

		if ( inLmMode )
		{
			disableTransformHandlers();
			message.showMessage( "Landmark mode on" );
			viewerFrameP.setCursor( Cursor.getPredefinedCursor( Cursor.CROSSHAIR_CURSOR ) );
			viewerFrameQ.setCursor( Cursor.getPredefinedCursor( Cursor.CROSSHAIR_CURSOR ) );
		}
		else
		{
			enableTransformHandlers();
			message.showMessage( "Landmark mode off" );
			viewerFrameP.setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
			viewerFrameQ.setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
		}

		inLandmarkMode = inLmMode;
	}

	/**
	 *
	 * @param ptarray location of the clicked point
	 * @param isMoving is the viewer in moving space
	 * @param selectedPointIndex the index of the selected point
	 * @param viewer the BigWarpViewerPanel clicked on
	 */
	public void updatePointLocation( final double[] ptarray, final boolean isMoving, final int selectedPointIndex, final BigWarpViewerPanel viewer )
	{
		final boolean isMovingViewerXfm = viewer.getOverlay().getIsTransformed();

		// TODO avoid duplicate effort and comment this section
		if ( landmarkModel.getTransform() == null || !isMovingViewerXfm )
		{
			landmarkModel.setPoint( selectedPointIndex, isMoving, ptarray, false, currentTransform );

			if ( !isMoving && !landmarkModel.isWarped( selectedPointIndex ) )
				landmarkModel.updateWarpedPoint( selectedPointIndex, ptarray );
		}
		else
		{
			landmarkModel.getTransform().apply( ptarray, ptBack );
			landmarkModel.updateWarpedPoint( selectedPointIndex, ptarray );
		}

		if ( landmarkFrame.isVisible() )
		{
			landmarkFrame.repaint();
			viewer.requestRepaint();
		}
	}

	public void updatePointLocation( final double[] ptarray, final boolean isMoving, final int selectedPointIndex )
	{
		if ( isMoving )
			updatePointLocation( ptarray, isMoving, selectedPointIndex, viewerP );
		else
			updatePointLocation( ptarray, isMoving, selectedPointIndex, viewerQ );
	}

	public int updateWarpedPoint( final double[] ptarray, final boolean isMoving )
	{
		final int selectedPointIndex = selectedLandmark( ptarray, isMoving );

		// if a fixed point is changing its location,
		// we need to update the warped position for the corresponding moving point
		// so that it can be rendered correctly
		if ( selectedPointIndex >= 0 && !isMoving && landmarkModel.getTransform() != null )
		{
			landmarkModel.updateWarpedPoint( selectedPointIndex, ptarray );
		}

		return selectedPointIndex;
	}

	public void updateRowSelection( boolean isMoving, int lastRowEdited )
	{
		updateRowSelection( landmarkModel, landmarkTable, isMoving, lastRowEdited );
		landmarkPanel.repaint();
	}

	public static void updateRowSelection(
			LandmarkTableModel landmarkModel, JTable table, 
			boolean isMoving, int lastRowEdited )
	{
		logger.trace( "updateRowSelection " );

		int i = landmarkModel.getNextRow( isMoving );
		if ( i < table.getRowCount() )
		{
			logger.trace( "  landmarkTable ( updateRowSelection ) selecting row " + i );
			table.setRowSelectionInterval( i, i );
		} else if( lastRowEdited >= 0 && lastRowEdited < table.getRowCount() )
			table.setRowSelectionInterval( lastRowEdited, lastRowEdited );
	}

	/**
	 * Returns the index of the selected row, if it is unpaired, -1 otherwise
	 * 
	 * @param isMoving isMoving
	 * @return index of the selected row
	 */
	public int getSelectedUnpairedRow( boolean isMoving )
	{
		int row = landmarkTable.getSelectedRow();
		if( row >= 0 && ( isMoving ? !landmarkModel.isMovingPoint( row ) : !landmarkModel.isFixedPoint( row )))
			return row;

		return -1;
	}

	/**
	 * Updates the global variable ptBack
	 *
	 * @param ptarray the point location
	 * @param isMoving is the point location in moving image space
	 * @param viewer the viewer panel
	 * @return an error string if an error occurred, empty string otherwise
	 */
	public String addPoint( final double[] ptarray, final boolean isMoving, final BigWarpViewerPanel viewer )
	{
		final boolean isViewerTransformed = viewer.getOverlay().getIsTransformed();

		if ( !isMoving && viewer.getIsMoving() && !isViewerTransformed )
			return "Adding a fixed point in moving image space not supported";
		else
		{
			addPoint( ptarray, isMoving );
			return "";
		}
	}

	/**
	 * Updates the global variable ptBack
	 *
	 * @param ptarray the point location
	 * @param isMoving is the point location in moving image space
	 * @return true if a new row was created
	 */
	public boolean addPoint( final double[] ptarray, final boolean isMoving )
	{
		final boolean isWarped = ( isMoving && landmarkModel.getTransform() != null && BigWarp.this.isMovingDisplayTransformed() );

		InvertibleRealTransform transform; 
		if( options.is2d  && currentTransform != null )
			transform = ((Wrapped2DTransformAs3D)currentTransform).getTransform();
		else
			transform = currentTransform;

		// TODO check this (current transform part)
		final boolean didAdd = BigWarp.this.landmarkModel.pointEdit( -1, ptarray, false, isMoving, isWarped, true, transform ); 

		if ( BigWarp.this.landmarkFrame.isVisible() )
		{
			BigWarp.this.landmarkFrame.repaint();
		}
		return didAdd;
	}

	protected int selectedLandmark( final double[] pt, final boolean isMoving )
	{
		return selectedLandmark( pt, isMoving, true );
	}

	/**
	 * Returns the index of the landmark closest to the input point,
	 * if it is within a certain distance threshold.
	 * 
	 * Updates the global variable ptBack
	 *
	 * @param pt the point location
	 * @param isMoving is the point location in moving image space
	 * @param selectInTable also select the landmark in the table 
	 * @return the index of the selected landmark
	 */
	protected int selectedLandmark( final double[] pt, final boolean isMoving, final boolean selectInTable )
	{
		logger.trace( "clicked: " + XfmUtils.printArray( pt ) );

		// TODO selectedLandmark
		final int N = landmarkModel.getRowCount();

		// a point will be selected if you click inside the spot ( with a 5 pixel buffer )
		double radsq = ( viewerSettings.getSpotSize() * viewerSettings.getSpotSize() ) + 5 ;
		final AffineTransform3D viewerXfm = new AffineTransform3D();
		if ( isMoving ) //&& !isMovingDisplayTransformed() )
		{
			viewerP.state().getViewerTransform( viewerXfm );
			radsq = viewerP.getSettings().getSpotSize();
		}
		else
		{
			viewerQ.state().getViewerTransform( viewerXfm );
			radsq = viewerQ.getSettings().getSpotSize();
		}
		radsq = ( radsq * radsq );
		final double scale = computeScaleAssumeRigid( viewerXfm );

		double dist = 0.0;
		int bestIdx = -1;
		double smallestDist = Double.MAX_VALUE;

		logger.trace( "  selectedLandmarkHelper dist scale: " + scale );
		logger.trace( "  selectedLandmarkHelper      radsq: " + radsq );

		for ( int n = 0; n < N; n++ )
		{
			final Double[] lmpt;
			if( isMoving && landmarkModel.isWarped( n ) && isMovingDisplayTransformed() )
			{
				lmpt = landmarkModel.getWarpedPoints().get( n );
			}
			else if( isMoving && isMovingDisplayTransformed() )
			{
				lmpt = landmarkModel.getPoints( false ).get( n );
			}
			else
			{
				lmpt = landmarkModel.getPoints( isMoving ).get( n );
			}

			dist = 0.0;
			for ( int i = 0; i < landmarkModel.getNumdims(); i++ )
			{
				dist += ( pt[ i ] - lmpt[ i ] ) * ( pt[ i ] - lmpt[ i ] );
			}

			dist *= ( scale * scale );
			logger.trace( "    dist squared of lm index : " + n + " is " + dist );
			if ( dist < radsq && dist < smallestDist )
			{
				smallestDist = dist;
				bestIdx = n;
			}
		}

		if ( selectInTable && landmarkFrame.isVisible() )
		{
			if( landmarkTable.isEditing())
			{
				landmarkTable.getCellEditor().stopCellEditing();
			}

			landmarkTable.setEditingRow( bestIdx );
			landmarkFrame.repaint();
		}
		logger.trace( "selectedLandmark: " + bestIdx );
		return bestIdx;
	}

	public static double computeScaleAssumeRigid( final AffineTransform3D xfm )
	{
		return xfm.get( 0, 0 ) + xfm.get( 0, 1 ) + xfm.get( 0, 2 );
	}

	protected void disableTransformHandlers()
	{
		// disable navigation listeners
		viewerFrameP.setTransformEnabled( false );
		viewerFrameQ.setTransformEnabled( false );
	}

	protected void enableTransformHandlers()
	{
		// enable navigation listeners
		viewerFrameP.setTransformEnabled( true );
		viewerFrameQ.setTransformEnabled( true );
	}

	private void printSourceTransforms()
	{
		final AffineTransform3D xfm = new AffineTransform3D();
		for( SourceAndConverter<?> sac  : data.sources )
		{
			sac.getSpimSource().getSourceTransform(0, 0, xfm );
			double det = BigWarpUtils.det( xfm );
			System.out.println( "source xfm ( " + sac.getSpimSource().getName()  + " ) : " + xfm );
			System.out.println( "    det = " + det );
		}
	}

	private void printViewerTransforms()
	{
		final AffineTransform3D xfm = new AffineTransform3D();
		viewerP.state().getViewerTransform( xfm );
		System.out.println( "mvg viewer xfm: " + xfm );
		System.out.println( "    det   = " + BigWarpUtils.det( xfm ));
		System.out.println( "    dotxy = " + BigWarpUtils.dotXy( xfm ));

		viewerQ.state().getViewerTransform( xfm );
		System.out.println( "tgt viewer xfm: " + xfm );
		System.out.println( "    det   = " + BigWarpUtils.det( xfm ));
		System.out.println( "    dotxy = " + BigWarpUtils.dotXy( xfm ));
	}

	/**
	 * Changes the view transformation of 'panelToChange' to match that of 'panelToMatch' 
	 * @param panelToChange the viewer panel whose transform will change
	 * @param panelToMatch the viewer panel the transform will come from
	 */
	protected void matchWindowTransforms( final BigWarpViewerPanel panelToChange, final BigWarpViewerPanel panelToMatch )
	{
		panelToChange.showMessage( "Aligning" );
		panelToMatch.showMessage( "Matching alignment" );
	
		// get the transform from panelToMatch
		final AffineTransform3D viewXfm = new AffineTransform3D();
		panelToMatch.state().getViewerTransform( viewXfm );

		// change transform of panelToChange
		panelToChange.animateTransformation( viewXfm );
	}

	public void matchOtherViewerPanelToActive()
	{
		if ( inLandmarkMode )
		{
			message.showMessage( "Can't move viewer in landmark mode." );
			return;
		}

		BigWarpViewerPanel panelToChange;
		BigWarpViewerPanel panelToMatch;

		if ( viewerFrameP.isActive() )
		{
			panelToChange = viewerQ;
			panelToMatch = viewerP;
		}
		else if ( viewerFrameQ.isActive() )
		{
			panelToChange = viewerP;
			panelToMatch = viewerQ;
		}
		else
			return;

		matchWindowTransforms( panelToChange, panelToMatch );
	}

	public void matchActiveViewerPanelToOther()
	{
		if ( inLandmarkMode )
		{
			message.showMessage( "Can't move viewer in landmark mode." );
			return;
		}

		BigWarpViewerPanel panelToChange;
		BigWarpViewerPanel panelToMatch;

		if ( viewerFrameP.isActive() )
		{
			panelToChange = viewerP;
			panelToMatch = viewerQ;
		}
		else if ( viewerFrameQ.isActive() )
		{
			panelToChange = viewerQ;
			panelToMatch = viewerP;
		}
		else
			return;

		matchWindowTransforms( panelToChange, panelToMatch );
	}

	public void warpToNearest( BigWarpViewerPanel viewer )
	{
		if ( inLandmarkMode )
		{
			message.showMessage( "Can't move viewer in landmark mode." );
			return;
		}

		RealPoint mousePt = new RealPoint( 3 ); // need 3d point even for 2d images
		viewer.getGlobalMouseCoordinates( mousePt );
		warpToLandmark( landmarkModel.getIndexNearestTo( mousePt, viewer.getIsMoving() ),  viewer );
	}

	public void warpToLandmark( int row, BigWarpViewerPanel viewer )
	{
		if( inLandmarkMode )
		{
			message.showMessage( "Can't move viewer in landmark mode." );
			return;
		}

		int offset = 0;
		int ndims = landmarkModel.getNumdims();
		double[] pt = null;
		if( viewer.getIsMoving() && viewer.getOverlay().getIsTransformed() )
		{
			if ( BigWarp.this.landmarkModel.isWarped( row ) )
			{
				pt = LandmarkTableModel.toPrimitive( BigWarp.this.landmarkModel.getWarpedPoints().get( row ) );
			}
			else
			{
				offset = ndims;
			}
		}else if( !viewer.getIsMoving() )
		{
			offset = ndims;
		}

		if ( pt == null )
		{
			if ( ndims == 3 )

				pt = new double[] {
						(Double) landmarkModel.getValueAt( row, offset + 2 ),
						(Double) landmarkModel.getValueAt( row, offset + 3 ),
						(Double) landmarkModel.getValueAt( row, offset + 4 ) };
			else
				pt = new double[] {
						(Double) landmarkModel.getValueAt( row, offset + 2 ),
						(Double) landmarkModel.getValueAt( row, offset + 3 ), 0.0 };
		}

		// we have an unmatched point
		if ( Double.isInfinite( pt[ 0 ] ) )
			return;

		final AffineTransform3D transform = viewer.state().getViewerTransform();
		final AffineTransform3D xfmCopy = transform.copy();
		xfmCopy.set( 0.0, 0, 3 );
		xfmCopy.set( 0.0, 1, 3 );
		xfmCopy.set( 0.0, 2, 3 );

		final double[] center = new double[] { viewer.getWidth() / 2, viewer.getHeight() / 2, 0 };
		final double[] ptxfm = new double[ 3 ];
		xfmCopy.apply( pt, ptxfm );

		// select appropriate row in the table
		landmarkTable.setRowSelectionInterval( row, row );

		// this should work fine in the 2d case
		final TranslationAnimator animator = new TranslationAnimator( transform, new double[] { center[ 0 ] - ptxfm[ 0 ], center[ 1 ] - ptxfm[ 1 ], -ptxfm[ 2 ] }, 300 );
		viewer.setTransformAnimator( animator );
	}

	public void goToBookmark()
	{
		if ( viewerFrameP.isActive() )
		{
			bookmarkEditorP.initGoToBookmark();
		}
		else if ( viewerFrameQ.isActive() )
		{
			bookmarkEditorQ.initGoToBookmark();
		}
	}

	public void resetView()
	{
		final RandomAccessibleInterval< ? > interval = getSources().get( 1 ).getSpimSource().getSource( 0, 0 );

		final AffineTransform3D viewXfm = new AffineTransform3D();
		viewXfm.identity();
		viewXfm.set( -interval.min( 2 ), 2, 3 );

		if ( viewerFrameP.isActive() )
		{
			if ( viewerP.getOverlay().getIsTransformed() )
				viewerP.animateTransformation( initialViewQ );
			else
				viewerP.animateTransformation( initialViewP );
		}
		else if ( viewerFrameQ.isActive() )
			viewerQ.animateTransformation( initialViewQ );
	}

	public void toggleNameVisibility()
	{
		viewerSettings.toggleNamesVisible();
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
	}

	public void togglePointVisibility()
	{
		viewerSettings.togglePointsVisible();
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
	}

	/**
	 * Toggles whether the moving image is displayed after warping (in the same
	 * space as the fixed image), or in its native space.
	 * 
	 * @return true of the display mode changed
	 */
	public boolean toggleMovingImageDisplay()
	{
		// If this is the first time calling the toggle, there may not be enough
		// points to estimate a reasonable transformation.  
		// return early if an re-estimation did not occur
		boolean success = restimateTransformation();
		logger.trace( "toggleMovingImageDisplay, success: " + success );
		if ( !success )
		{
			message.showMessage( "Require at least 4 points to estimate a transformation" );
			return false;
		}

		final boolean newState = !getOverlayP().getIsTransformed();

		if ( newState )
			message.showMessage( "Displaying warped" );
		else
			message.showMessage( "Displaying raw" );

		// Toggle whether moving image is displayed as transformed or not
		setIsMovingDisplayTransformed( newState );
		return success;
	}

	protected void addDefaultTableMouseListener()
	{
		landmarkTableListener = new MouseLandmarkTableListener();
		landmarkPanel.getJTable().addMouseListener( landmarkTableListener );
	}

	public void setGridType( final GridSource.GRID_TYPE method )
	{
		( ( GridSource< ? > ) gridSource.getSpimSource() ).setMethod( method );
	}

	public static <T> List< SourceAndConverter<T> > wrapSourcesAsTransformed( final List< SourceAndConverter<T> > sources,
			final int ndims,
			final BigWarpData<?> data )
	{
		final List< SourceAndConverter<T>> wrappedSource = new ArrayList<>();

		int[] warpUsIndices = data.movingSourceIndices;
		HashMap<SourceAndConverter<?>, ColorSettings> colorSettings = data.sourceColorSettings;

		int i = 0;
		for ( final SourceAndConverter<T>sac : sources )
		{
			int idx = Arrays.binarySearch( warpUsIndices, i );
			if ( idx >= 0 )
			{
				SourceAndConverter<T> newSac = wrapSourceAsTransformed( sac, "xfm_" + i, ndims );
				wrappedSource.add( newSac );
				colorSettings.put( newSac, colorSettings.get( sac ));
			}
			else
			{
				wrappedSource.add( sac );
			}

			i++;
		}
		return wrappedSource;
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	private static < T > SourceAndConverter< FloatType > addJacobianDeterminantSource( final BigWarpData< T > data, final String name )
	{
		// TODO think about whether its worth it to pass a type parameter.
		// or should we just stick with Doubles?

		final JacobianDeterminantSource< FloatType > jdSource = new JacobianDeterminantSource<>( name, data, new FloatType() );
		final RealARGBColorConverter< FloatType > converter = RealARGBColorConverter.create( new FloatType(), 0, 512 );
		converter.setColor( new ARGBType( 0xffffffff ) );
		final SourceAndConverter< FloatType > soc = new SourceAndConverter<>( jdSource, converter, null );
		data.converterSetups.add( BigDataViewer.createConverterSetup( soc, JACDET_SOURCE_ID ) );
		data.sources.add( ( SourceAndConverter ) soc );
		return soc;
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	private static < T > SourceAndConverter< FloatType > addWarpMagnitudeSource(  final BigWarpData< T > data, final String name )
	{
		// TODO think about whether its worth it to pass a type parameter.
		// or should we just stick with Doubles?

		final WarpMagnitudeSource< FloatType > magSource = new WarpMagnitudeSource<>( name, data, new FloatType() );
		final RealARGBColorConverter< FloatType > converter = RealARGBColorConverter.create( new FloatType(), 0, 512 );
		converter.setColor( new ARGBType( 0xffffffff ) );
		final SourceAndConverter< FloatType > soc = new SourceAndConverter<>( magSource, converter, null );
		data.converterSetups.add( BigDataViewer.createConverterSetup( soc, WARPMAG_SOURCE_ID ) );
		data.sources.add( ( SourceAndConverter ) soc );
		return soc;
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	private static < T > SourceAndConverter< FloatType > addGridSource( final BigWarpData< T > data, final String name )
	{
		// TODO think about whether its worth it to pass a type parameter.
		// or should we just stick with Floats?

		final GridSource< FloatType > gridSource = new GridSource<>( name, data, new FloatType(), null );
		final RealARGBColorConverter< FloatType > converter = RealARGBColorConverter.create( new FloatType(), 0, 512 );
		converter.setColor( new ARGBType( 0xffffffff ) );
		final SourceAndConverter< FloatType > soc = new SourceAndConverter<>( gridSource, converter, null );
		data.converterSetups.add( BigDataViewer.createConverterSetup( soc, GRID_SOURCE_ID ) );
		data.sources.add( ( SourceAndConverter ) soc );
		return soc;
	}

	private static < T > SourceAndConverter< T > wrapSourceAsTransformed( final SourceAndConverter< T > src, final String name, final int ndims )
	{
		if ( src.asVolatile() == null )
		{
			return new SourceAndConverter< T >( new WarpedSource< T >( src.getSpimSource(), name ), src.getConverter(), null );
		}
		else
		{
			return new SourceAndConverter< T >( new WarpedSource< T >( src.getSpimSource(), name ), src.getConverter(), wrapSourceAsTransformed( src.asVolatile(), name + "_vol", ndims ) );
		}
	}

	public void setupKeyListener()
	{
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor( new LandmarkKeyboardProcessor( this ) );
	}

	public void setWarpVisGridType( final GridSource.GRID_TYPE type )
	{
		( ( GridSource< ? > ) gridSource.getSpimSource() ).setMethod( type );
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
	}

	public void setWarpGridWidth( final double width )
	{
		( ( GridSource< ? > ) gridSource.getSpimSource() ).setGridWidth( width );
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
	}

	public void setWarpGridSpacing( final double spacing )
	{
		( ( GridSource< ? > ) gridSource.getSpimSource() ).setGridSpacing( spacing );
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
	}

	protected void setupWarpMagBaselineOptions( final CoordinateTransform[] xfm, final int ndim )
	{
		if ( ndim == 2 )
		{
			xfm[ 0 ] = new AffineModel2D();
			xfm[ 1 ] = new SimilarityModel2D();
			xfm[ 2 ] = new RigidModel2D();
		}
		else
		{
			xfm[ 0 ] = new AffineModel3D();
			xfm[ 1 ] = new SimilarityModel3D();
			xfm[ 2 ] = new RigidModel3D();
		}
	}

	public void setWarpMagBaselineIndex( int index )
	{
		baselineModelIndex = index;
		fitBaselineWarpMagModel();
	}

	protected void fitBaselineWarpMagModel()
	{
		final int numActive = landmarkModel.numActive();
		if( numActive < 4 )
			return;

		final int ndims = landmarkModel.getNumdims();
		final double[][] p = new double[ ndims ][ numActive ];
		final double[][] q = new double[ ndims ][ numActive ];
		final double[] w = new double[ numActive ];

		int k = 0;
		for ( int i = 0; i < landmarkModel.getTransform().getNumLandmarks(); i++ )
		{
			if ( landmarkModel.isActive( i ) )
			{
				w[ k ] = 1.0;

				for ( int d = 0; d < ndims; d++ )
				{
					p[ d ][ k ] = landmarkModel.getMovingPoint( i )[ d ];
					q[ d ][ k ] = landmarkModel.getFixedPoint( i )[ d ];
				}
				k++;
			}
		}

		try
		{
			final AbstractModel< ? > baseline = this.baseXfmList[ baselineModelIndex ];

			baseline.fit( p, q, w );  // FITBASELINE
			WrappedCoordinateTransform baselineTransform = new WrappedCoordinateTransform(
					(InvertibleCoordinateTransform)baseline, ndims );

			// the transform to compare is the inverse (because we use it for rendering)
			// so need to give the inverse transform for baseline as well
			( ( WarpMagnitudeSource< ? > ) warpMagSource.getSpimSource() ).setBaseline( baselineTransform.inverse() );
		}
		catch ( final NotEnoughDataPointsException e )
		{
			e.printStackTrace();
		}
		catch ( final IllDefinedDataPointsException e )
		{
			e.printStackTrace();
		}
		getViewerFrameP().getViewerPanel().requestRepaint();
		getViewerFrameQ().getViewerPanel().requestRepaint();
	}

	public void setMovingSpimData( SpimData movingSpimData, File movingImageXml )
	{
		this.movingSpimData = movingSpimData;
		this.movingImageXml = movingImageXml;
	}

	public enum WarpVisType
	{
		NONE, WARPMAG, JACDET, GRID
	};

	public void setWarpVisMode( final WarpVisType type, BigWarpViewerFrame viewerFrame, final boolean both )
	{
		if ( viewerFrame == null )
		{
			if ( viewerFrameP.isActive() )
			{
				viewerFrame = viewerFrameP;
			}
			else if ( viewerFrameQ.isActive() )
			{
				viewerFrame = viewerFrameQ;
			}
			else if ( both )
			{
				setWarpVisMode( type, viewerFrameP, false );
				setWarpVisMode( type, viewerFrameQ, false );
				return;
			}
			else
			{
				return;
			}

		}
//
//		int offImgIndex = 0;
//		int onImgIndex = 1;
//
//		if ( viewerFrame == viewerFrameP )
//		{
//			offImgIndex = 1;
//			onImgIndex = 0;
//		}

		if ( landmarkModel.getTransform() == null )
		{
			message.showMessage( "No warp - estimate warp first." );
			return;
		}
		final ViewerState state = viewerFrame.getViewerPanel().state();

		switch ( type )
		{
		case JACDET:
		{
			// turn warp mag on
			state.setSourceActive( warpMagSource, false );
			state.setSourceActive( jacDetSource, true );
			state.setSourceActive( gridSource, false );

		}
		case WARPMAG:
		{
			// turn warp mag on
			state.setSourceActive( warpMagSource, true );
			state.setSourceActive( jacDetSource, false );
			state.setSourceActive( gridSource, false );
//			vg.setSourceActive( offImgIndex, false );

			// estimate the max warp
//			final WarpMagnitudeSource< ? > wmSrc = ( ( WarpMagnitudeSource< ? > ) sources.get( warpMagSourceIndex ).getSpimSource() );
//			final double maxval = wmSrc.getMax( landmarkModel );

			// set the slider
//			( ( RealARGBColorConverter< FloatType > ) ( sources.get( warpMagSourceIndex ).getConverter() ) ).setMax( maxval );

			state.setDisplayMode( DisplayMode.FUSED );
			message.showMessage( "Displaying Warp Magnitude" );
			break;
		}
		case GRID:
		{
			// turn grid vis on

			state.setSourceActive( warpMagSource, false );
			state.setSourceActive( jacDetSource, false );
			state.setSourceActive( gridSource, true );
//			vg.setSourceActive( offImgIndex, false );

			state.setDisplayMode( DisplayMode.FUSED );
			message.showMessage( "Displaying Warp Grid" );
			break;
		}
		default:
		{
			state.setSourceActive( warpMagSource, false );
			state.setSourceActive( gridSource, false );
//			vg.setSourceActive( offImgIndex, true );

//			vg.setFusedEnabled( false );
			message.showMessage( "Turning off warp vis" );
			break;
		}
		}
	}

	public void toggleWarpVisMode( BigWarpViewerFrame viewerFrame )
	{
//		int offImgIndex = 0;
//		int onImgIndex = 1;
		if ( viewerFrame == null )
		{
			if ( viewerFrameP.isActive() )
			{
				viewerFrame = viewerFrameP;
			}
			else if ( viewerFrameQ.isActive() )
			{
				viewerFrame = viewerFrameQ;
			}
			else
				return;
		}

//		if ( viewerFrame == viewerFrameP )
//		{
//			offImgIndex = 1;
//			onImgIndex = 0;
//		}

		if ( landmarkModel.getTransform() == null )
		{
			message.showMessage( "No warp - estimate warp first." );
			return;
		}

		final ViewerState state = viewerFrame.getViewerPanel().state();

		// TODO consider remembering whether fused was on before displaying
		// warpmag
		// so that its still on or off after we turn it off
		if ( state.isSourceActive( warpMagSource ) ) // warp mag is visible,
														// turn it off
		{
			state.setSourceActive( warpMagSource, false );

//			vg.setSourceActive( offImgIndex, true );

			state.setDisplayMode( state.getDisplayMode().withFused( false ) );
			message.showMessage( "Removing Warp Magnitude" );
		}
		else // warp mag is invisible, turn it on
		{
			state.setSourceActive( warpMagSource, true );
//			vg.setSourceActive( offImgIndex, false );

			// estimate the max warp
//			final WarpMagnitudeSource< ? > wmSrc = ( ( WarpMagnitudeSource< ? > ) sources.get( warpMagSourceIndex ).getSpimSource() );
//			final double maxval = wmSrc.getMax( landmarkModel );

			// set the slider
//			( ( RealARGBColorConverter< FloatType > ) ( sources.get( warpMagSourceIndex ).getConverter() ) ).setMax( maxval );

			state.setDisplayMode( state.getDisplayMode().withFused( false ) );
			message.showMessage( "Displaying Warp Magnitude" );
		}

		viewerFrame.getViewerPanel().requestRepaint();
	}

	private void setTransformationMovingSourceOnly( final InvertibleRealTransform transform )
	{
		this.currentTransform = transform;

		for ( int i = 0; i < movingSourceIndexList.length; i++ )
		{
			int idx = movingSourceIndexList [ i ];

			// the xfm must always be 3d for bdv to be happy.
			// when bigwarp has 2d images though, the z- component will be left unchanged
			//InverseRealTransform xfm = new InverseRealTransform( new TpsTransformWrapper( 3, transform ));

			// the updateTransform method creates a copy of the transform
			( ( WarpedSource< ? > ) ( sources.get( idx ).getSpimSource() ) ).updateTransform( transform );
			if ( sources.get( 0 ).asVolatile() != null )
				( ( WarpedSource< ? > ) ( sources.get( idx ).asVolatile().getSpimSource() ) ).updateTransform( transform );
		}
	}

	private synchronized void notifyTransformListeners( )
	{
		for ( final TransformListener< InvertibleRealTransform > l : transformListeners )
			l.transformChanged( currentTransform );
	}

	private void setTransformationAll( final InvertibleRealTransform transform )
	{
		setTransformationMovingSourceOnly( transform );

		final WarpMagnitudeSource< ? > wmSrc = ( ( WarpMagnitudeSource< ? > ) warpMagSource.getSpimSource() );
		final JacobianDeterminantSource< ? > jdSrc = ( ( JacobianDeterminantSource< ? > ) jacDetSource.getSpimSource() );
		final GridSource< ? > gSrc = ( ( GridSource< ? > ) gridSource.getSpimSource() );

		wmSrc.setWarp( transform );
		fitBaselineWarpMagModel();
	
		if( transform instanceof ThinplateSplineTransform )
		{
			jdSrc.setTransform( (ThinplateSplineTransform)transform );
		}
		else if ( transform instanceof WrappedIterativeInvertibleRealTransform )
		{
			jdSrc.setTransform( (ThinplateSplineTransform)((WrappedIterativeInvertibleRealTransform)transform).getTransform() );
		}
		else
			jdSrc.setTransform( null );

		gSrc.setWarp( transform );
	}

	public boolean restimateTransformation()
	{
		if ( landmarkModel.getActiveRowCount() < 4 )
		{
			return false;
		}
		// TODO restimateTransformation
		// This distinction is unnecessary right now, because
		// transferUpdatesToModel just calls initTransformation.. but this may
		// change
//		if ( landmarkModel.getTransform() == null )
//			landmarkModel.initTransformation();
//		else
//			landmarkModel.transferUpdatesToModel();

		solverThread.requestResolve( true, -1, null );

		// display the warped version automatically if this is the first
		// time the transform was computed
		if ( firstWarpEstimation )
		{
			setUpdateWarpOnChange( true );
			firstWarpEstimation = false;
		}

		viewerP.requestRepaint();
		viewerQ.requestRepaint();

		notifyTransformListeners();

		return true;
	}

	public void setIsMovingDisplayTransformed( final boolean isTransformed )
	{
		for( int i = 0 ; i < movingSourceIndexList.length; i ++ )
		{
			int movingSourceIndex = movingSourceIndexList[ i ];

			( ( WarpedSource< ? > ) ( sources.get( movingSourceIndex ).getSpimSource() ) ).setIsTransformed( isTransformed );

			if ( sources.get( movingSourceIndex ).asVolatile() != null )
				( ( WarpedSource< ? > ) ( sources.get( movingSourceIndex ).asVolatile().getSpimSource() ) ).setIsTransformed( isTransformed );
		}

		overlayP.setIsTransformed( isTransformed );

		viewerP.requestRepaint();

		if ( viewerQ.state().getDisplayMode().hasFused() )
		{
			viewerQ.requestRepaint();
		}
	}

	/**
	 * Returns true if the currently selected row in the landmark table is missing on the the landmarks
	 * @return true if there is a missing value
	 */
	public boolean isRowIncomplete()
	{
		LandmarkTableModel ltm = landmarkPanel.getTableModel();
		return ltm.isPointUpdatePending() || ltm.isPointUpdatePendingMoving();
	}

	public boolean isMovingDisplayTransformed()
	{
		// this implementation is okay, so long as all the moving images have the same state of 'isTransformed'
		return ( ( WarpedSource< ? > ) ( sources.get( movingSourceIndexList[ 0 ] ).getSpimSource() ) ).isTransformed();
	}

	/**
	 * The display will be in 3d if any of the input sources are 3d.
	 * @return dimension of the input sources
	 */
	protected int detectNumDims()
	{
		return detectNumDims( sources );
	}

	/**
	 * The display will be in 3d if any of the input sources are 3d.
	 * @param sources the sources
	 * @param <T> the type
	 * @return dimension of the input sources
	 */
	protected static <T> int detectNumDims( List< SourceAndConverter< T > > sources )
	{
		boolean isAnySource3d = false;
		for ( SourceAndConverter< T > sac : sources )
		{
			long[] dims = new long[ sac.getSpimSource().getSource( 0, 0 ).numDimensions() ];
			sac.getSpimSource().getSource( 0, 0 ).dimensions( dims );

			if ( sac.getSpimSource().getSource( 0, 0 ).dimension( 2 ) > 1 )
			{
				isAnySource3d = true;
				break;
			}
		}

		int ndims = 2;
		if ( isAnySource3d )
			ndims = 3;

		return ndims;
	}

	public static void main( final String[] args )
	{
		new ImageJ();
		
		// TODO main
		String fnP = "";
		String fnQ = "";
		String fnLandmarks = "";

		int i = 0;
		if ( args.length >= 2 )
		{
			fnP = args[ i++ ];
			fnQ = args[ i++ ];
		}
		else
		{
			System.err.println( "Must provide at least 2 inputs for moving and target image files" );
			System.exit( 1 );
		}

		if ( args.length > i )
			fnLandmarks = args[ i++ ];

		boolean doInverse = false;
		if ( args.length > i )
		{
			try
			{
				doInverse = Boolean.parseBoolean( args[ i++ ] );
				System.out.println( "parsed inverse param: " + doInverse );
			}
			catch ( final Exception e )
			{
				// no inverse param
				System.err.println( "Warning: Failed to parse inverse parameter." );
			}
		}

		try
		{
			System.setProperty( "apple.laf.useScreenMenuBar", "false" );


			ProgressWriterIJ progress = new ProgressWriterIJ();
			BigWarp<?> bw;
			BigWarpData<?> bwdata; 
			if ( fnP.endsWith( "xml" ) && fnQ.endsWith( "xml" ) )
			{
				bwdata = BigWarpInit.createBigWarpDataFromXML( fnP, fnQ );
				bw = new BigWarp<>( bwdata, new File( fnP ).getName(), progress );
			}
			else if ( fnP.endsWith( "xml" ) && !fnQ.endsWith( "xml" ) )
			{
				final ImagePlus impQ = IJ.openImage( fnQ );
				bwdata = BigWarpInit.createBigWarpDataFromXMLImagePlus( fnP, impQ );
				bw = new BigWarp<>( bwdata, new File( fnP ).getName(), progress );
			}
			else if ( !fnP.endsWith( "xml" ) && fnQ.endsWith( "xml" ) )
			{
				final ImagePlus impP = IJ.openImage( fnP );
				bwdata = BigWarpInit.createBigWarpDataFromImagePlusXML( impP, fnQ );
				bw = new BigWarp<>( bwdata, new File( fnP ).getName(), progress );
			}
			else
			{
				final ImagePlus impP = IJ.openImage( fnP );
				final ImagePlus impQ = IJ.openImage( fnQ );

				if ( !( impP == null || impQ == null ) )
				{
					bwdata = BigWarpInit.createBigWarpDataFromImages( impP, impQ );
					
					bw = new BigWarp<>( bwdata, new File( fnP ).getName(), progress );
				}
				else
				{
					System.err.println( "Error reading images" );
					return;
				}
			}

			
			if ( !fnLandmarks.isEmpty() )
				bw.getLandmarkPanel().getTableModel().load( new File( fnLandmarks ) );

			if ( doInverse )
				bw.invertPointCorrespondences();

		}
		catch ( final Exception e )
		{

			e.printStackTrace();
		}
	}
	
	private void viewerXfmTest()
	{
		AffineTransform3D srcTransform0 = new AffineTransform3D();
		sources.get( 0 ).getSpimSource().getSourceTransform(0, 0, srcTransform0 );

		AffineTransform3D srcTransform1 = new AffineTransform3D();
		sources.get( 1 ).getSpimSource().getSourceTransform(0, 0, srcTransform1 );

		AffineTransform3D viewerTransformM = new AffineTransform3D();
		viewerP.state().getViewerTransform( viewerTransformM );

		AffineTransform3D viewerTransformT = new AffineTransform3D();
		viewerQ.state().getViewerTransform( viewerTransformT );

		System.out.println( " " );
		System.out.println( " " );
		System.out.println( "srcXfm 0 " + srcTransform0 );
		System.out.println( "srcXfm 1 " + srcTransform1 );

		System.out.println( "viewerXfm M " + viewerTransformM );
		System.out.println( "viewerXfm T " + viewerTransformT );
		System.out.println( " " );
		System.out.println( " " );

	}

	public void checkBoxInputMaps()
	{
		// Disable spacebar for toggling checkboxes
		// Make it enter instead
		// This is super ugly ... why does it have to be this way.

		final TableCellEditor celled = landmarkTable.getCellEditor( 0, 1 );
		final Component c = celled.getTableCellEditorComponent( landmarkTable, Boolean.TRUE, true, 0, 1 );

		final InputMap parentInputMap = ( ( JCheckBox ) c ).getInputMap().getParent();
		parentInputMap.clear();
		final KeyStroke enterDownKS = KeyStroke.getKeyStroke( "pressed ENTER" );
		final KeyStroke enterUpKS = KeyStroke.getKeyStroke( "released ENTER" );

		parentInputMap.put( enterDownKS, "pressed" );
		parentInputMap.put( enterUpKS, "released" );

		/*
		 * Consider with replacing with something like the below Found in
		 * BigWarpViewerFrame
		 */
//		SwingUtilities.replaceUIActionMap( getRootPane(), keybindings.getConcatenatedActionMap() );
//		SwingUtilities.replaceUIInputMap( getRootPane(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );
	}

	public static class BigWarpData< T >
	{
		public final List< SourceAndConverter< T > > sources;

		public final List< ConverterSetup > converterSetups;

		public final CacheControl cache;

		public int[] movingSourceIndices;

		public int[] targetSourceIndices;

		public final ArrayList< Integer > movingSourceIndexList;

		public final ArrayList< Integer > targetSourceIndexList;

		public final HashMap< Integer, ColorSettings > setupSettings;

		public final HashMap< SourceAndConverter<?>, ColorSettings > sourceColorSettings;

		public BigWarpData( final List< SourceAndConverter< T > > sources, final List< ConverterSetup > converterSetups, final CacheControl cache, int[] movingSourceIndices, int[] targetSourceIndices )
		{
			this.sources = sources;
			this.converterSetups = converterSetups;
			this.movingSourceIndices = movingSourceIndices;
			this.targetSourceIndices = targetSourceIndices;

			this.movingSourceIndexList = new ArrayList<>();
			this.targetSourceIndexList = new ArrayList<>();
			

			if ( cache == null )
				this.cache = new CacheControl.Dummy();
			else
				this.cache = cache;

			setupSettings = new HashMap<>();
			sourceColorSettings = new HashMap<>();
		}

		public void wrapUp()
		{
			movingSourceIndices = movingSourceIndexList.stream().mapToInt( x -> x ).toArray();
			targetSourceIndices = targetSourceIndexList.stream().mapToInt( x -> x ).toArray();

			Arrays.sort( movingSourceIndices );
			Arrays.sort( targetSourceIndices );
		}

		/**
		 * @deprecated
		 */
		public void transferChannelSettings( final SetupAssignments setupAssignments, final VisibilityAndGrouping visibility )
		{
			for( Integer key : setupSettings.keySet() )
				setupSettings.get( key ).updateSetup( setupAssignments );
		}

		public void transferChannelSettings( final BigWarpViewerFrame viewer )
		{
			SynchronizedViewerState state = viewer.getViewerPanel().state();
			ConverterSetups setups = viewer.getConverterSetups();
			synchronized ( state )
			{
				for ( SourceAndConverter< ? > sac : state.getSources() )
					if ( sourceColorSettings.containsKey( sac ) )
					{
						if ( sourceColorSettings.get( sac ) == null )
							continue;

						sourceColorSettings.get( sac ).updateSetup( setups.getConverterSetup( sac ) );
					}
			}
		}
	}

	protected class LandmarkModeListener implements KeyEventPostProcessor
	{
		@Override
		public boolean postProcessKeyEvent( final KeyEvent ke )
		{
			if ( ke.isConsumed() )
				return false;

			if ( ke.getKeyCode() == KeyEvent.VK_SPACE )
			{
				if ( ke.getID() == KeyEvent.KEY_PRESSED )
				{
					BigWarp.this.setInLandmarkMode( true );
					return false;
				}
				else if ( ke.getID() == KeyEvent.KEY_RELEASED )
				{
					BigWarp.this.setInLandmarkMode( false );
					return false;
				}
			}
			return false;
		}
	}

	// TODO,
	// consider this
	// https://github.com/kwhat/jnativehook
	// for arbitrary modifiers
	protected class MouseLandmarkListener implements MouseListener, MouseMotionListener
	{

		// -1 indicates that no point is selected
		int selectedPointIndex = -1;

		double[] ptarrayLoc = new double[ 3 ];

		double[] ptBackLoc = new double[ 3 ];

		private BigWarpViewerPanel thisViewer;

		private boolean isMoving;

		private long pressTime;

		private RealPoint hoveredPoint;

		private double[] hoveredArray;

		protected MouseLandmarkListener( final BigWarpViewerPanel thisViewer )
		{
			setViewer( thisViewer );
			thisViewer.getDisplay().addHandler( this );
			isMoving = ( thisViewer == BigWarp.this.viewerP );
			hoveredArray = new double[ 3 ];
			hoveredPoint = RealPoint.wrap( hoveredArray );
		}

		protected void setViewer( final BigWarpViewerPanel thisViewer )
		{
			this.thisViewer = thisViewer;
		}

		@Override
		public void mouseClicked( final MouseEvent arg0 )
		{}

		@Override
		public void mouseEntered( final MouseEvent arg0 )
		{}

		@Override
		public void mouseExited( final MouseEvent arg0 )
		{}

		@Override
		public void mousePressed( final MouseEvent e )
		{
			pressTime = System.currentTimeMillis();

			// shift down is reserved for drag overlay
			if ( e.isShiftDown() ) { return; }

			if ( BigWarp.this.isInLandmarkMode() )
			{
				thisViewer.getGlobalMouseCoordinates( BigWarp.this.currentLandmark );

				BigWarp.this.currentLandmark.localize( ptarrayLoc );
				selectedPointIndex = BigWarp.this.selectedLandmark( ptarrayLoc, isMoving );

				if ( selectedPointIndex >= 0 )
				{
					landmarkTable.setRowSelectionInterval( selectedPointIndex, selectedPointIndex );
					landmarkFrame.repaint();
					BigWarp.this.landmarkModel.setLastPoint( selectedPointIndex, isMoving );
				}
			}
		}

		@Override
		public void mouseReleased( final MouseEvent e )
		{
			long clickLength = System.currentTimeMillis() - pressTime;

			if( clickLength < keyClickMaxLength && selectedPointIndex != -1 )
				return;

			// shift when
			boolean isMovingLocal = isMoving;
			if ( e.isShiftDown() && e.isControlDown() )
			{ 
				System.out.println( "shift-control release");
				isMovingLocal = !isMoving;
			}
			else if( e.isShiftDown())
			{
				// shift is reserved for click-drag
				return;
			}
			else if ( e.isControlDown() )
			{
				if ( BigWarp.this.isInLandmarkMode() && selectedPointIndex < 0 )
				{
					thisViewer.getGlobalMouseCoordinates( BigWarp.this.currentLandmark );
					addFixedPoint( BigWarp.this.currentLandmark, isMovingLocal );
				}
				return;
			}

			boolean wasNewRowAdded = false;

			// deselect any point that may be selected
			if ( BigWarp.this.isInLandmarkMode() )
			{
				thisViewer.getGlobalMouseCoordinates( BigWarp.this.currentLandmark );
				currentLandmark.localize( ptarrayLoc );

				if ( selectedPointIndex == -1 )
					wasNewRowAdded = addPoint( ptarrayLoc, isMovingLocal );
				else
				{
					final boolean isWarped = isMovingLocal && ( landmarkModel.getTransform() != null ) && ( BigWarp.this.isMovingDisplayTransformed() );
					wasNewRowAdded = BigWarp.this.landmarkModel.pointEdit( selectedPointIndex, ptarrayLoc, false, isMovingLocal, isWarped, true, currentTransform );
				}

				if ( updateWarpOnPtChange && !wasNewRowAdded )
				{
					// here, if a new row is added, then only one of the point
					// pair was added.
					// if we changed and existing row, then we have both points
					// in the pair and should recompute
					BigWarp.this.restimateTransformation();
				}

				if( wasNewRowAdded )
					updateRowSelection( isMovingLocal, landmarkModel.getRowCount() - 1 );
				else
					updateRowSelection( isMovingLocal, selectedPointIndex );
			}

			BigWarp.this.landmarkModel.resetLastPoint();
			selectedPointIndex = -1;
		}

		@Override
		public void mouseDragged( final MouseEvent e )
		{
			// shift down is reserved for drag overlay
			if ( e.isShiftDown() ) { return; }

			if ( BigWarp.this.isInLandmarkMode() && selectedPointIndex >= 0 )
			{
				thisViewer.getGlobalMouseCoordinates( BigWarp.this.currentLandmark );
				currentLandmark.localize( ptarrayLoc );

				if ( BigWarp.this.isMovingDisplayTransformed() &&
						thisViewer.doUpdateOnDrag() &&
						BigWarp.this.landmarkModel.isActive( selectedPointIndex ) )
				{
					logger.trace("Drag resolve");
					solverThread.requestResolve( isMoving, selectedPointIndex, ptarrayLoc );
				}
				else
				{
					// Make a non-undoable edit so that the point can be displayed correctly
					// the undoable action is added on mouseRelease
					if( isMoving && isMovingDisplayTransformed() )
					{
						logger.trace("Drag moving transformed");
						// The moving image:
						// Update the warped point during the drag even if there is a corresponding fixed image point
						// Do this so the point sticks on the mouse
						BigWarp.this.landmarkModel.pointEdit(
								selectedPointIndex,
								BigWarp.this.landmarkModel.getTransform().apply( ptarrayLoc ),
								false, isMoving, ptarrayLoc, false );
						thisViewer.requestRepaint();
					}
					else
					{
						logger.trace("Drag default");
						// The fixed image
						BigWarp.this.landmarkModel.pointEdit( selectedPointIndex, ptarrayLoc, false, isMoving, false, false, currentTransform );
						thisViewer.requestRepaint();
					}
				}
			}
		}

		@Override
		public void mouseMoved( final MouseEvent e )
		{
			thisViewer.getGlobalMouseCoordinates( hoveredPoint );
			int hoveredIndex = BigWarp.this.selectedLandmark( hoveredArray, isMoving, false );
			thisViewer.setHoveredIndex( hoveredIndex );
		}

		/**
		 * Adds a point in the moving and fixed images at the same point.
		 * @param pt the point
		 * @param isMovingImage is the point in moving image space
		 */
		public void addFixedPoint( final RealPoint pt, final boolean isMovingImage )
		{
			if ( isMovingImage && viewerP.getTransformEnabled() )
			{
				// Here we clicked in the space of the moving image
				currentLandmark.localize( ptarrayLoc );
				addPoint( ptarrayLoc, true, viewerP );
				addPoint( ptarrayLoc, false, viewerQ );
			}
			else
			{
				currentLandmark.localize( ptarrayLoc );
				addPoint( ptarrayLoc, true, viewerP );
				addPoint( ptarrayLoc, false, viewerQ );
			}
			if ( updateWarpOnPtChange )
				BigWarp.this.restimateTransformation();
		}
	}

	public class WarningTableCellRenderer extends DefaultTableCellRenderer
	{
		private static final long serialVersionUID = 7836269349663370123L;

		@Override
		public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
		{
			LandmarkTableModel model = ( LandmarkTableModel ) table.getModel();
			Component c = super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
			if ( model.rowNeedsWarning( row ) )
				c.setBackground( LandmarkTableModel.WARNINGBGCOLOR );
			else
				c.setBackground( LandmarkTableModel.DEFAULTBGCOLOR );
			return c;
		}
	}

	public class LandmarkTableListener implements TableModelListener
	{
		@Override
		public void tableChanged( final TableModelEvent e )
		{
			// re-estimate if a a point was set to or from active
			// note - this covers "resetting" points as well
			if( e.getColumn() == LandmarkTableModel.ACTIVECOLUMN )
			{
				BigWarp.this.restimateTransformation();
				BigWarp.this.landmarkPanel.repaint();
			}
		}
	}

	public class MouseLandmarkTableListener implements MouseListener
	{
		boolean wasDoubleClick = false;

		Timer timer;

		public MouseLandmarkTableListener()
		{}

		@Override
		public void mouseClicked( final MouseEvent e )
		{
			final int ndims = landmarkModel.getNumdims();
//			if ( BigWarp.this.isInLandmarkMode() )
//			{
//				final JTable target = ( JTable ) e.getSource();
//
//				final int row = target.getSelectedRow();
//				final int column = target.getSelectedColumn();
//
//				final boolean isMoving = ( column > 1 && column < ( 2 + ndims ) );
//
//				BigWarp.this.landmarkModel.clearPt( row, isMoving );
//
//			}
//			else if ( e.getClickCount() == 2 )
			if ( e.getClickCount() == 2 )
			{
				final JTable target = ( JTable ) e.getSource();
				final int row = target.getSelectedRow();
				final int column = target.getSelectedColumn();

				if( row < 0 )
					return;

				double[] pt = null;
				int offset = 0;

				final BigWarpViewerPanel viewer;
				if ( column >= ( 2 + ndims ) )
				{
					// clicked on a fixed point
					viewer = BigWarp.this.viewerQ;
					offset = ndims;
				}
				else if ( column >= 2 && column < ( 2 + ndims ) )
				{
					// clicked on a moving point
					viewer = BigWarp.this.viewerP;

					if ( BigWarp.this.viewerP.getOverlay().getIsTransformed() )
						if ( BigWarp.this.landmarkModel.isWarped( row ) )
							pt = LandmarkTableModel.toPrimitive( BigWarp.this.landmarkModel.getWarpedPoints().get( row ) );
						else
							offset = ndims;
				}
				else
				{
					// we're in a column that doesn't correspond to a point and
					// should do nothing
					return;
				}

				// the pt variable might be set above by grabbing the warped point.
				// if so, stick with it, else grab the appropriate value from the table
				if ( pt == null )
				{
					if ( ndims == 3 )
						pt = new double[] { ( Double ) target.getValueAt( row, offset + 2 ), ( Double ) target.getValueAt( row, offset + 3 ), ( Double ) target.getValueAt( row, offset + 4 ) };
					else
						pt = new double[] { ( Double ) target.getValueAt( row, offset + 2 ), ( Double ) target.getValueAt( row, offset + 3 ), 0.0 };
				}

				// we have an unmatched point
				if ( Double.isInfinite( pt[ 0 ] ) )
					return;

				final AffineTransform3D transform = viewer.state().getViewerTransform();
				final AffineTransform3D xfmCopy = transform.copy();
				xfmCopy.set( 0.0, 0, 3 );
				xfmCopy.set( 0.0, 1, 3 );
				xfmCopy.set( 0.0, 2, 3 );

				final double[] center = new double[] { viewer.getWidth() / 2, viewer.getHeight() / 2, 0
				};
				final double[] ptxfm = new double[ 3 ];
				xfmCopy.apply( pt, ptxfm );

				// this should work fine in the 2d case
				final TranslationAnimator animator = new TranslationAnimator( transform, new double[] { center[ 0 ] - ptxfm[ 0 ], center[ 1 ] - ptxfm[ 1 ], -ptxfm[ 2 ] }, 300 );
				viewer.setTransformAnimator( animator );
			}
			else
			{
				final JTable target = ( JTable ) e.getSource();
				final int row = target.rowAtPoint( e.getPoint() );

				// if we click in the table but not on a row, deselect everything
				if( row < 0 && target.getRowCount() > 0  )
					target.removeRowSelectionInterval( 0, target.getRowCount() - 1 );
			}
		}

		@Override
		public void mouseEntered( final MouseEvent e )
		{}

		@Override
		public void mouseExited( final MouseEvent e )
		{}

		@Override
		public void mousePressed( final MouseEvent e )
		{}

		@Override
		public void mouseReleased( final MouseEvent e )
		{}

	}

	public void setTransformType( final String type )
	{
		this.transformType = type;
		transformSelector.setTransformType( transformType );
		this.restimateTransformation();
	}

	public String getTransformType()
	{
		return transformType;
	}

	/**
	 * Use getTps, getTpsBase, or getTransformation instead
	 * @return
	 */
	@Deprecated
	public ThinPlateR2LogRSplineKernelTransform getTransform()
	{
		return landmarkPanel.getTableModel().getTransform();
	}

	public ThinplateSplineTransform getTps()
	{
		if( transformType.equals( TransformTypeSelectDialog.TPS ))
		{
			WrappedIterativeInvertibleRealTransform<?> wiirt = (WrappedIterativeInvertibleRealTransform<?>)( unwrap2d( getTransformation()) );
			return ((ThinplateSplineTransform)wiirt.getTransform());
		}
		return null;
	}

	public ThinPlateR2LogRSplineKernelTransform getTpsBase()
	{
		ThinplateSplineTransform tps = getTps();
		if( tps == null )
			return null;
		else
		{
			// TODO add get method in ThinplateSplineTransform to avoid reflection here
			final Class< ThinplateSplineTransform > c_tps = ThinplateSplineTransform.class;
			try
			{
				final Field tpsField = c_tps.getDeclaredField( "tps" );
				tpsField.setAccessible( true );
				ThinPlateR2LogRSplineKernelTransform tpsbase = (ThinPlateR2LogRSplineKernelTransform)tpsField.get(  tps );
				tpsField.setAccessible( false );

				return tpsbase;
			}
			catch(Exception e )
			{
				e.printStackTrace();
				return null;
			}
		}
	}

	public InvertibleCoordinateTransform getCoordinateTransform()
	{
		if( !transformType.equals( TransformTypeSelectDialog.TPS ))
		{
			WrappedCoordinateTransform wct = (WrappedCoordinateTransform)( unwrap2d( getTransformation() ));
			return wct.ct;
		}
		return null;
	}

	public synchronized void addTransformListener( TransformListener< InvertibleRealTransform > listener )
	{
		transformListeners.add( listener );
	}

	public InvertibleRealTransform unwrap2d( InvertibleRealTransform ixfm )
	{
		if( ixfm instanceof Wrapped2DTransformAs3D )
			return ((Wrapped2DTransformAs3D)ixfm).getTransform();
		else
			return ixfm;
	}

	public InvertibleRealTransform getTransformation()
	{
		return getTransformation( -1 );
	}

	public InvertibleRealTransform getTransformation( final int index )
	{
		int ndims = 3;
		InvertibleRealTransform invXfm = null;
		if( transformType.equals( TransformTypeSelectDialog.TPS ))
		{
			final ThinPlateR2LogRSplineKernelTransform xfm;
			if ( index >= 0 ) // a point position is modified
			{
				LandmarkTableModel tableModel = getLandmarkPanel().getTableModel();
				if ( !tableModel.getIsActive( index ) )
					return null;

				int numActive = tableModel.numActive();
				ndims = tableModel.getNumdims();

				double[][] mvgPts = new double[ ndims ][ numActive ];
				double[][] tgtPts = new double[ ndims ][ numActive ];

				tableModel.copyLandmarks( mvgPts, tgtPts );

				// need to find the "inverse TPS" - the transform from target space to moving space.
				xfm = new ThinPlateR2LogRSplineKernelTransform( ndims, tgtPts, mvgPts );
			}
			else // a point is added
			{
				landmarkModel.initTransformation();
				ndims = landmarkModel.getNumdims();
				xfm = getLandmarkPanel().getTableModel().getTransform();
			}
			invXfm = new WrappedIterativeInvertibleRealTransform<>( new ThinplateSplineTransform( xfm ));
		}
		else
		{
			Model<?> model = getModelType();
			fitModel(model);
			int nd = landmarkModel.getNumdims();
			invXfm = new WrappedCoordinateTransform( (InvertibleCoordinateTransform) model, nd ).inverse();
		}

		if( options.is2d )
		{
			invXfm = new Wrapped2DTransformAs3D( invXfm );
		}

		return invXfm;
	}

	public void fitModel( final Model<?> model )
	{
		LandmarkTableModel tableModel = getLandmarkPanel().getTableModel();

		int numActive = tableModel.numActive();
		int ndims = tableModel.getNumdims();

		double[][] mvgPts = new double[ ndims ][ numActive ];
		double[][] tgtPts = new double[ ndims ][ numActive ];

		tableModel.copyLandmarks( mvgPts, tgtPts );

		double[] w = new double[ numActive ];
		Arrays.fill( w, 1.0 );

		try {
			model.fit( mvgPts, tgtPts, w );
		} catch (NotEnoughDataPointsException e) {
			e.printStackTrace();
		} catch (IllDefinedDataPointsException e) {
			e.printStackTrace();
		}
	}

	public Model<?> getModelType()
	{
		if( landmarkModel.getNumdims() == 2 )
			return getModel2D();
		else
			return getModel3D();
	}

	public AbstractAffineModel3D<?> getModel3D()
	{
		switch( transformType ){
		case TransformTypeSelectDialog.AFFINE:
			return new AffineModel3D();
		case TransformTypeSelectDialog.SIMILARITY:
			return new SimilarityModel3D();
		case TransformTypeSelectDialog.ROTATION:
			return new RigidModel3D();
		case TransformTypeSelectDialog.TRANSLATION:
			return new TranslationModel3D();
		}
		return null;
	}

	public AbstractAffineModel2D<?> getModel2D()
	{
		switch( transformType ){
		case TransformTypeSelectDialog.AFFINE:
			return new AffineModel2D();
		case TransformTypeSelectDialog.SIMILARITY:
			return new SimilarityModel2D();
		case TransformTypeSelectDialog.ROTATION:
			return new RigidModel2D();
		case TransformTypeSelectDialog.TRANSLATION:
			return new TranslationModel2D();
		}
		return null;
	}

	public static class SolveThread extends Thread
	{
		private boolean pleaseResolve;

		private BigWarp<?> bw;

		private boolean isMoving;

		private int index;

		private double[] pt;

		public SolveThread( final BigWarp<?> bw )
		{
			this.bw = bw;
			pleaseResolve = false;
		}

		@Override
		public void run()
		{
			while ( !isInterrupted() )
			{
				final boolean b;
				synchronized ( this )
				{
					b = pleaseResolve;
					pleaseResolve = false;
				}
				if ( b )
				{
					try
					{
						InvertibleRealTransform invXfm = bw.getTransformation( index );

						if ( invXfm == null )
							return;

						if ( index < 0 )
						{
							// reset active warped points
							bw.landmarkModel.resetWarpedPoints();

							// re-compute all warped points for non-active points
							bw.landmarkModel.updateAllWarpedPoints( bw.currentTransform );

							// update sources with the new transformation
							bw.setTransformationAll( invXfm );
							bw.fitBaselineWarpMagModel();
						}
						else
						{
							// update the transform and warped point
							bw.setTransformationMovingSourceOnly( invXfm );
						}

						// update fixed point - but don't allow undo/redo
						// and update warped point
						// both for rendering purposes
						if ( !isMoving )
							bw.getLandmarkPanel().getTableModel().setPoint( index, isMoving, pt, false, bw.currentTransform );

						/*
						 * repaint both panels so that: 
						 * 1) new transform is displayed
						 * 2) points are rendered
						 */
						bw.getViewerFrameP().getViewerPanel().requestRepaint();
						bw.getViewerFrameQ().getViewerPanel().requestRepaint();

//						bw.notifyTransformListeners();
					}

					catch ( final RejectedExecutionException e )
					{
						// this happens when the rendering threadpool
						// is killed before the painter thread.
					}
				}

				synchronized ( this )
				{
					try
					{
						if ( !pleaseResolve )
							wait();
					}
					catch ( final InterruptedException e )
					{
						break;
					}
				}
			}
		}

		public void requestResolve( final boolean isMoving, final int index, final double[] newpt )
		{
			synchronized ( this )
			{
				pleaseResolve = true;
				this.isMoving = isMoving;
				this.index = index;
				if ( newpt != null )
					this.pt = Arrays.copyOf( newpt, newpt.length );

				notify();
			}
		}
		
	}
	
	public static class WrappedCoordinateTransform implements InvertibleRealTransform
	{
		private final InvertibleCoordinateTransform ct;
		private final InvertibleCoordinateTransform ct_inv;
		private final int nd;

		public WrappedCoordinateTransform( InvertibleCoordinateTransform ct, int nd )
		{
            this.nd = nd;
			this.ct = ct;
			this.ct_inv = ct.createInverse();
		}

		public InvertibleCoordinateTransform getTransform()
        {
			return ct;
		}

		@Override
		public void apply(double[] src, double[] tgt)
        {
			double[] res = ct.apply( src );
            System.arraycopy( res, 0, tgt, 0, res.length );
		}

		@Override
		public void apply( RealLocalizable src, RealPositionable tgt )
        {
            double[] srcpt = new double[ src.numDimensions() ];
            src.localize( srcpt );

            double[] res = ct.apply( srcpt );
            tgt.setPosition( res );
		}

		@Override
		public int numSourceDimensions()
        {
			return nd;
		}

		@Override
		public int numTargetDimensions()
        {
			return nd; 
		}

		@Override
		public void applyInverse( double[] src, double[] tgt )
        {
		    double[] res = ct_inv.apply( tgt );
            System.arraycopy( res, 0, src, 0, res.length );    
		}

		@Override
		public void applyInverse( RealPositionable src, RealLocalizable tgt )
        {
            double[] tgtpt = new double[ tgt.numDimensions() ];
            tgt.localize( tgtpt );
            
            double[] res = ct_inv.apply( tgtpt );
            src.setPosition( res );
		}

		@Override
		public WrappedCoordinateTransform copy()
        {
			return new WrappedCoordinateTransform( ct, nd );
		}

		@Override
		public WrappedCoordinateTransform inverse()
        {
			return new WrappedCoordinateTransform( ct_inv, nd );
		}
	}

	protected void saveLandmarks()
	{
		final JFileChooser fileChooser = new JFileChooser( getLastDirectory() );
		File proposedSettingsFile = new File( "landmarks.csv" );

		fileChooser.setSelectedFile( proposedSettingsFile );
		final int returnVal = fileChooser.showSaveDialog( null );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
		{
			proposedSettingsFile = fileChooser.getSelectedFile();
			try
			{
				System.out.println("save landmarks");
				saveLandmarks( proposedSettingsFile.getCanonicalPath() );
			} catch ( final IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	protected void saveLandmarks( final String filename ) throws IOException
	{
		landmarkModel.save(new File( filename ));
	}

	protected void loadLandmarks()
	{
		final JFileChooser fileChooser = new JFileChooser( getLastDirectory() );
		File proposedSettingsFile = new File( "landmarks.csv" );

		fileChooser.setSelectedFile( proposedSettingsFile );
		final int returnVal = fileChooser.showOpenDialog( null );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
		{
			proposedSettingsFile = fileChooser.getSelectedFile();
			try
			{
				loadLandmarks( proposedSettingsFile.getCanonicalPath() );
			} catch ( final IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	public void loadLandmarks( final String filename )
	{
		File file = new File( filename );
		setLastDirectory( file.getParentFile() );
		try
		{
			landmarkModel.load( file );
		}
		catch ( final IOException e1 )
		{
			e1.printStackTrace();
		}

		boolean didCompute = restimateTransformation();

		// didCompute = false means that there were not enough points
		// in the loaded points, so we should display the 'raw' moving
		// image
		if ( !didCompute )
			setIsMovingDisplayTransformed( false );

		viewerP.requestRepaint();
		viewerQ.requestRepaint();
		landmarkFrame.repaint();
	}

	protected void saveSettings()
	{
		final JFileChooser fileChooser = new JFileChooser( getLastDirectory() );
		File proposedSettingsFile = new File( "bigwarp.settings.xml" );

		fileChooser.setSelectedFile( proposedSettingsFile );
		final int returnVal = fileChooser.showSaveDialog( null );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
		{
			proposedSettingsFile = fileChooser.getSelectedFile();
			try
			{
				saveSettings( proposedSettingsFile.getCanonicalPath() );
			} catch ( final IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	protected void saveSettings( final String xmlFilename ) throws IOException
	{
		final Element root = new Element( "Settings" );

		Element viewerPNode = new Element( "viewerP" );
		Element viewerQNode = new Element( "viewerQ" );

		root.addContent( viewerPNode );
		root.addContent( viewerQNode );

		viewerPNode.addContent( viewerP.stateToXml() );
		viewerQNode.addContent( viewerQ.stateToXml() );

		root.addContent( setupAssignments.toXml() );
		root.addContent( bookmarks.toXml() );
		final Document doc = new Document( root );
		final XMLOutputter xout = new XMLOutputter( Format.getPrettyFormat() );
		xout.output( doc, new FileWriter( xmlFilename ) );
	}

	protected void loadSettings()
	{
		final JFileChooser fileChooser = new JFileChooser( getLastDirectory() );
		File proposedSettingsFile = new File( "bigwarp.settings.xml" );

		fileChooser.setSelectedFile( proposedSettingsFile );
		final int returnVal = fileChooser.showOpenDialog( null );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
		{
			proposedSettingsFile = fileChooser.getSelectedFile();
			try
			{
				loadSettings( proposedSettingsFile.getCanonicalPath() );
			} catch ( final Exception e )
			{
				e.printStackTrace();
			}
		}
	}

	protected void loadSettings( final String xmlFilename ) throws IOException,
			JDOMException
	{
		final SAXBuilder sax = new SAXBuilder();
		final Document doc = sax.build( xmlFilename );
		final Element root = doc.getRootElement();
		viewerP.stateFromXml( root.getChild( "viewerP" ) );
		viewerQ.stateFromXml( root.getChild( "viewerQ" ) );
		setupAssignments.restoreFromXml( root );
		bookmarks.restoreFromXml( root );
		activeSourcesDialogP.update();
		activeSourcesDialogQ.update();

		viewerFrameP.repaint();
		viewerFrameQ.repaint();
	}
}
