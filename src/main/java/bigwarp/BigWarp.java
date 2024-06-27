/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2022 Howard Hughes Medical Institute.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package bigwarp;

import bdv.TransformState;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

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
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.ij.N5ScalePyramidExporter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.TransformGraph;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.TransformPath;
import org.janelia.utility.geom.BoundingSphereRitter;
import org.janelia.utility.geom.Sphere;
import org.janelia.utility.ui.RepeatingReleasedEventsFixer;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.util.UIScale;
import com.google.gson.stream.JsonReader;

import bdv.BigDataViewer;
import bdv.export.ProgressWriter;
import bdv.export.ProgressWriterConsole;
import bdv.gui.BigWarpLandmarkPanel;
import bdv.gui.BigWarpMessageAnimator;
import bdv.gui.BigWarpViewerFrame;
import bdv.gui.BigWarpViewerOptions;
import bdv.gui.BigwarpLandmarkSelectionPanel;
import bdv.gui.ExportDisplacementFieldFrame;
import bdv.gui.LandmarkKeyboardProcessor;
import bdv.gui.MaskOptionsPanel;
import bdv.gui.MaskedSourceEditorMouseListener;
import bdv.gui.TransformGraphPanel;
import bdv.gui.TransformTypeSelectDialog;
import bdv.ij.ApplyBigwarpPlugin;
import bdv.ij.ApplyBigwarpPlugin.WriteDestinationOptions;
import bdv.ij.BigWarpToDeformationFieldPlugIn;
import bdv.ij.util.ProgressWriterIJ;
import bdv.img.WarpedSource;
import bdv.tools.InitializeViewerState;
import bdv.tools.PreferencesDialog;
import bdv.tools.VisibilityAndGroupingDialog;
import bdv.tools.bookmarks.Bookmarks;
import bdv.tools.bookmarks.BookmarksEditor;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.ui.appearance.AppearanceManager;
import bdv.ui.appearance.AppearanceSettingsPage;
import bdv.ui.appearance.BwAppearanceManager;
import bdv.ui.keymap.Keymap;
import bdv.ui.keymap.KeymapSettingsPage;
import bdv.util.BoundedRange;
import bdv.util.Bounds;
import bdv.viewer.AbstractViewerPanel.AlignPlane;
import bdv.viewer.BigWarpDragOverlay;
import bdv.viewer.BigWarpLandmarkFrame;
import bdv.viewer.BigWarpOverlay;
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.BigWarpViewerSettings;
import bdv.viewer.ConverterSetups;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.LandmarkPointMenu;
import bdv.viewer.MultiBoxOverlay2d;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SourceGroup;
import bdv.viewer.SynchronizedViewerState;
import bdv.viewer.TransformListener;
import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerState;
import bdv.viewer.ViewerStateChange;
import bdv.viewer.ViewerStateChangeListener;
import bdv.viewer.WarpNavigationActions;
import bdv.viewer.animate.SimilarityModel3D;
import bdv.viewer.animate.TranslationAnimator;
import bdv.viewer.overlay.BigWarpMaskSphereOverlay;
import bdv.viewer.overlay.BigWarpSourceOverlayRenderer;
import bdv.viewer.overlay.MultiBoxOverlayRenderer;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.source.GridSource;
import bigwarp.source.JacobianDeterminantSource;
import bigwarp.source.PlateauSphericalMaskRealRandomAccessible;
import bigwarp.source.PlateauSphericalMaskRealRandomAccessible.FalloffShape;
import bigwarp.source.PlateauSphericalMaskSource;
import bigwarp.source.SourceInfo;
import bigwarp.source.WarpMagnitudeSource;
import bigwarp.transforms.AbstractTransformSolver;
import bigwarp.transforms.BigWarpTransform;
import bigwarp.transforms.MaskedSimRotTransformSolver;
import bigwarp.transforms.WrappedCoordinateTransform;
import bigwarp.transforms.io.TransformWriterJson;
import bigwarp.ui.keymap.KeymapManager;
import bigwarp.ui.keymap.NavigationKeys;
import bigwarp.util.BigWarpUtils;
import dev.dirs.ProjectDirectories;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.util.LinkedHashMap;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import jitk.spline.XfmUtils;
import mpicbg.models.AbstractModel;
import mpicbg.models.AffineModel2D;
import mpicbg.models.AffineModel3D;
import mpicbg.models.CoordinateTransform;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.InvertibleCoordinateTransform;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.RigidModel2D;
import mpicbg.models.RigidModel3D;
import mpicbg.models.SimilarityModel2D;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.TranslationModel3D;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.registration.ViewTransformAffine;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.BoundingBoxEstimation;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.InvertibleWrapped2DTransformAs3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.realtransform.inverse.RealTransformFiniteDerivatives;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

public class BigWarp< T >
{
	public static String configDir = ProjectDirectories.from( "sc", "fiji", "bigwarp" ).configDir;

	protected static final int DEFAULT_WIDTH = 400;

	protected static final int DEFAULT_HEIGHT = 300;

	public static final int GRID_SOURCE_ID = 1696993146;

	public static final int WARPMAG_SOURCE_ID = 956736363;

	public static final int JACDET_SOURCE_ID = 1006827158;

	public static final int TRANSFORM_MASK_SOURCE_ID = 33872301;

	protected BigWarpViewerOptions options;

	protected BigWarpData< T > data;

	protected final SetupAssignments setupAssignments;
	protected final WarpVisFrame warpVisDialog;

	protected final HelpDialog helpDialog;

	private final KeymapManager keymapManager;

	private final AppearanceManager appearanceManager;

	protected final SourceInfoDialog sourceInfoDialog;

	protected final VisibilityAndGroupingDialog activeSourcesDialogP;

	protected final VisibilityAndGroupingDialog activeSourcesDialogQ;

	protected final PreferencesDialog preferencesDialog;

	final AffineTransform3D fixedViewXfm;

	private Bookmarks bookmarks;

	protected final BookmarksEditor bookmarkEditorP;

	protected final BookmarksEditor bookmarkEditorQ;

	private final BigWarpViewerFrame viewerFrameP;

	private final BigWarpViewerFrame viewerFrameQ;

	protected final BigWarpViewerPanel viewerP;

	protected final BigWarpViewerPanel viewerQ;

	protected final BigWarpActions tableActions;

	protected AffineTransform3D initialViewP;

	protected AffineTransform3D initialViewQ;

	private JMenuItem toggleAlwaysWarpMenuP;

	private JMenuItem toggleAlwaysWarpMenuQ;

	protected BigWarpLandmarkPanel landmarkPanel;

	protected LandmarkPointMenu landmarkPopupMenu;

	protected BigWarpLandmarkFrame landmarkFrame;

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

	protected MaskedSourceEditorMouseListener maskSourceMouseListenerP;

	protected MaskedSourceEditorMouseListener maskSourceMouseListenerQ;

	protected BigWarpMessageAnimator message;

	protected final Set< KeyEventPostProcessor > keyEventPostProcessorSet = new HashSet< KeyEventPostProcessor >();

	private final RepeatingReleasedEventsFixer repeatedKeyEventsFixer;

	protected GridSource gridSource;

	protected WarpMagnitudeSource<FloatType> warpMagSource;

	protected JacobianDeterminantSource<FloatType> jacDetSource;

	protected SourceAndConverter< ? extends RealType<?>> transformMaskSource;

	protected Source< ? extends RealType<?>> transformMask;

	protected PlateauSphericalMaskSource plateauTransformMask;

	protected final AbstractModel< ? >[] baseXfmList;

	private final double[] ptBack;

	private SolveThread solverThread;

	private BigWarpTransform bwTransform;

	protected SourceGroup mvgGrp, tgtGrp;

	private BoundingBoxEstimation bboxOptions;

	private long keyClickMaxLength = 250;

	protected TransformTypeSelectDialog transformSelector;

	protected AffineTransform3D tmpTransform = new AffineTransform3D();

	/*
	 * landmarks are placed on clicks only if we are inLandmarkMode during the
	 * click
	 */
	protected boolean inLandmarkMode;

	protected int baselineModelIndex;

	// file selection
	protected JFrame fileFrame;

	protected FileDialog fileDialog;

	final JFileChooser fileChooser;

	protected File autoSaveDirectory;

	protected File lastDirectory;

	protected File lastLandmarks;

	protected BigWarpAutoSaver autoSaver;

	protected boolean updateWarpOnPtChange = false;

	protected boolean firstWarpEstimation = true;

	JMenu fileMenu;

	final ProgressWriter progressWriter;

	private static ImageJ ij;

	protected static Logger logger = LoggerFactory.getLogger( BigWarp.class );

	//TODO Caleb: John, can this be replaced by info from BigWarpData/SourceInfo url?
	private SpimData movingSpimData;

	private File movingImageXml;

	private CopyOnWriteArrayList< TransformListener< InvertibleRealTransform > > transformListeners = new CopyOnWriteArrayList<>( );

	int ndims;

	@Deprecated
	public BigWarp( final BigWarpData< T > data, final String windowTitle, final ProgressWriter progressWriter ) throws SpimDataException
	{
		this( data, BigWarpViewerOptions.options().is2D( detectNumDims( data.sources ) == 2 ), progressWriter );
	}

	@Deprecated
	public BigWarp( final BigWarpData< T > data, final String windowTitle, BigWarpViewerOptions options, final ProgressWriter progressWriter ) throws SpimDataException {
		this(data, options, progressWriter );
	}

	public BigWarp( final BigWarpData< T > data, final ProgressWriter progressWriter ) throws SpimDataException {
		this( data, BigWarpViewerOptions.options().is2D( detectNumDims( data.sources ) == 2 ), progressWriter );
	}

	public BigWarp( final BigWarpData<T> data,  BigWarpViewerOptions options, final ProgressWriter progressWriter ) throws SpimDataException
	{
		final KeymapManager optionsKeymapManager = options.getValues().getKeymapManager();
		final AppearanceManager optionsAppearanceManager = options.values.getAppearanceManager();
		keymapManager = optionsKeymapManager != null ? optionsKeymapManager : new KeymapManager( configDir );
		appearanceManager = optionsAppearanceManager != null ? optionsAppearanceManager : new BwAppearanceManager( configDir );

		InputTriggerConfig inputTriggerConfig = options.values.getInputTriggerConfig();
		final Keymap keymap = this.keymapManager.getForwardSelectedKeymap();
		if ( inputTriggerConfig == null )
			inputTriggerConfig = keymap.getConfig();

		repeatedKeyEventsFixer = RepeatingReleasedEventsFixer.installAnyTime();

		ij = IJ.getInstance();

		if( progressWriter == null )
			this.progressWriter = new ProgressWriterConsole();
		else
			this.progressWriter = progressWriter;

		this.data = data;
		this.options = options;

		ptBack = new double[ 3 ];
		if( options.values.is2D() )
			ndims = 2;
		else
			ndims = 3;

		/*
		 * Set up LandmarkTableModel, holds the data and interfaces with the
		 * LandmarkPanel
		 */

		/* Set up landmark panel */
		setupLandmarkFrame();

		baseXfmList = new AbstractModel< ? >[ 3 ];
		setupWarpMagBaselineOptions( baseXfmList, ndims );

		fixedViewXfm = new AffineTransform3D();
		viewerSettings = new BigWarpViewerSettings();

		// key properties
		final InputTriggerConfig keyProperties = BigDataViewer.getInputTriggerConfig( options );
		options = options.inputTriggerConfig( keyProperties );

		final int width = UIScale.scale( DEFAULT_WIDTH );
		final int height = UIScale.scale( DEFAULT_HEIGHT );

		final List<SourceAndConverter<?>> srcs = (List)data.sources;

		// Viewer frame for the moving image
		viewerFrameP = new BigWarpViewerFrame(this, width, height, srcs, data.converterSetups,
				viewerSettings, data.cache, keymapManager, appearanceManager, options, "BigWarp moving image", true);

		viewerP = getViewerFrameP().getViewerPanel();

		// Viewer frame for the fixed image
		viewerFrameQ = new BigWarpViewerFrame(this, width, height, srcs, data.converterSetups,
				viewerSettings, data.cache, keymapManager, appearanceManager, options, "BigWarp fixed image", false);

		viewerQ = getViewerFrameQ().getViewerPanel();

		// setup messaging
		message = options.getMessageAnimator();
		message.setViewers( viewerP, viewerQ );
		landmarkModel.setMessage( message );

		// If the images are 2d, use a transform handler that limits
		// transformations to rotations and scalings of the 2d plane ( z = 0 )
		if ( options.values.is2D() )
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

		overlayP = new BigWarpOverlay( viewerP, landmarkPanel );
		overlayQ = new BigWarpOverlay( viewerQ, landmarkPanel );
		viewerP.addOverlay( overlayP );
		viewerQ.addOverlay( overlayQ );

		bwTransform = new BigWarpTransform( landmarkModel );
		bwTransform.initializeInverseParameters(data);

		solverThread = new SolveThread( this );
		solverThread.start();

		bboxOptions = new BoundingBoxEstimation( BoundingBoxEstimation.Method.FACES, 5 );

		dragOverlayP = new BigWarpDragOverlay( this, viewerP, solverThread );
		dragOverlayQ = new BigWarpDragOverlay( this, viewerQ, solverThread );
		viewerP.addDragOverlay( dragOverlayP );
		viewerQ.addDragOverlay( dragOverlayQ );

		landmarkPopupMenu = new LandmarkPointMenu( this );
		landmarkPopupMenu.setupListeners();

		setupAssignments = new SetupAssignments( new ArrayList<>( data.converterSetups ), 0, 65535 );

		helpDialog = new HelpDialog( landmarkFrame );
		sourceInfoDialog = new SourceInfoDialog( landmarkFrame, data );

		transformSelector = new TransformTypeSelectDialog( landmarkFrame, this );

		// dialogs have to be constructed before action maps are made
		warpVisDialog = new WarpVisFrame( viewerFrameQ, this );

		preferencesDialog = new PreferencesDialog( landmarkFrame, keymap, new String[] { "bigwarp", "navigation", "bw-table" } );
		preferencesDialog.addPage( new AppearanceSettingsPage( "Appearance", appearanceManager ) );
		preferencesDialog.addPage( new KeymapSettingsPage( "Keymap", this.keymapManager, new KeymapManager(), this.keymapManager.getCommandDescriptions() ) );

		fileChooser = new JFileChooser();
		fileChooser.setFileFilter( new FileFilter()
		{
			@Override
			public String getDescription()
			{
				return "xml files";
			}

			@Override
			public boolean accept( final File f )
			{
				if ( f.isDirectory() )
					return true;
				if ( f.isFile() )
				{
					final String s = f.getName();
					final int i = s.lastIndexOf( '.' );
					if ( i > 0 && i < s.length() - 1 )
					{
						final String ext = s.substring( i + 1 ).toLowerCase();
						return ext.equals( "xml" );
					}
				}
				return false;
			}
		} );

		appearanceManager.appearance().updateListeners().add( viewerFrameP::repaint );
		appearanceManager.appearance().updateListeners().add( viewerFrameQ::repaint );
		appearanceManager.appearance().updateListeners().add( landmarkFrame::repaint );
		appearanceManager.addLafComponent( fileChooser );
		SwingUtilities.invokeLater(() -> appearanceManager.updateLookAndFeel());

		final Actions navigationActionsP = new Actions( inputTriggerConfig, "navigationMvg" );
		navigationActionsP.install( getViewerFrameP().getKeybindings(), "navigationMvg" );
		NavigationKeys.install( navigationActionsP, getViewerFrameP().getViewerPanel(), options.values.is2D() );

		final Actions navigationActionsQ = new Actions( inputTriggerConfig, "navigationFix" );
		navigationActionsQ.install( getViewerFrameQ().getKeybindings(), "navigationFix" );
		NavigationKeys.install( navigationActionsQ, getViewerFrameQ().getViewerPanel(), options.values.is2D() );

		final BigWarpActions bwActionsP = new BigWarpActions( inputTriggerConfig, "bigwarpMvg" );
		BigWarpActions.installViewerActions( bwActionsP, getViewerFrameP(), this );

		final BigWarpActions bwActionsQ = new BigWarpActions( inputTriggerConfig, "bigwarpFix" );
		BigWarpActions.installViewerActions( bwActionsQ, getViewerFrameQ(), this );

		tableActions = new BigWarpActions( inputTriggerConfig, "bw-table" );
		BigWarpActions.installTableActions( tableActions, getLandmarkFrame().getKeybindings(), this );
//		UnmappedNavigationActions.install( tableActions, options.values.is2D() );

		keymap.updateListeners().add( () -> {

			bwActionsP.updateKeyConfig( keymap.getConfig() );
			bwActionsQ.updateKeyConfig( keymap.getConfig() );
			tableActions.updateKeyConfig( keymap.getConfig() );

			viewerFrameP.getTransformBehaviours().updateKeyConfig( keymap.getConfig() );
			viewerFrameQ.getTransformBehaviours().updateKeyConfig( keymap.getConfig() );
		} );

		// this call has to come after the actions are set
		warpVisDialog.setActions();
		warpVisDialog.toleranceSpinner.setValue( bwTransform.getInverseTolerance() );

		setUpViewerMenu( viewerFrameP );
		setUpViewerMenu( viewerFrameQ );
		setUpLandmarkMenus();

		/* Set the locations of frames */
		viewerFrameP.setLocation( 0, 0 );
		viewerFrameP.setSize( width, height );
		viewerFrameQ.setLocation( width, 0 );
		viewerFrameQ.setSize( width, height );
		landmarkFrame.setLocation( 2 * width, 0 );

		landmarkClickListenerP = new MouseLandmarkListener( this.viewerP );
		landmarkClickListenerQ = new MouseLandmarkListener( this.viewerQ );

		// have to be safe here and use 3dim point for both 3d and 2d
		currentLandmark = new RealPoint( 3 );
		inLandmarkMode = false;
		setupKeyListener();

		// save the initial viewer transforms
		initialViewP = new AffineTransform3D();
		initialViewQ = new AffineTransform3D();
		viewerP.state().getViewerTransform( initialViewP );
		viewerQ.state().getViewerTransform( initialViewQ );

		checkBoxInputMaps();

		if ( ij == null || (IJ.getDirectory( "current" ) == null) )
			lastDirectory = new File( System.getProperty( "user.home" ));
		else
			lastDirectory = new File( IJ.getDirectory( "current" ));

		// add focus listener
		//new BigwarpFocusListener( this );

		bookmarks = new Bookmarks();
		bookmarkEditorP = new BookmarksEditor( viewerP, viewerFrameP.getKeybindings(), bookmarks );
		bookmarkEditorQ = new BookmarksEditor( viewerQ, viewerFrameQ.getKeybindings(), bookmarks );

		// add landmark mode listener
		//addKeyEventPostProcessor( new LandmarkModeListener() );

		baselineModelIndex = 0;

		if( data.sources.size() > 0 )
			initialize();

		createMovingTargetGroups();
		viewerP.state().setCurrentGroup( mvgGrp );
		viewerQ.state().setCurrentGroup( tgtGrp );
//		viewerQ.state().changeListeners().add(warpVisDialog.transformGraphPanel);

		SwingUtilities.invokeLater( () -> {
			viewerFrameP.setVisible( true );
			viewerFrameQ.setVisible( true );
			landmarkFrame.setVisible( true );

			fileFrame = new JFrame( "Select File" );
			fileDialog = new FileDialog( fileFrame );
			fileFrame.setVisible( false );
		});
	}

	public void changeDimensionality(boolean is2D) {

		if (options.values.is2D() == is2D)
			return;

		options.is2D( is2D );
		if( options.values.is2D() )
			ndims = 2;
		else
			ndims = 3;

		/* update landmark model with new dimensionality */
		landmarkModel = new LandmarkTableModel(ndims);
		landmarkModel.addTableModelListener(landmarkModellistener);
		addTransformListener(landmarkModel);
		landmarkModel.setMessage(message);

		landmarkPanel.setTableModel(landmarkModel);
		setupLandmarkFrame();

		landmarkPopupMenu = new LandmarkPointMenu(this);
		landmarkPopupMenu.setupListeners();
		BigWarpActions.installTableActions(tableActions, getLandmarkFrame().getKeybindings(), this);

		setupWarpMagBaselineOptions( baseXfmList, ndims );

		final Class< ViewerPanel > c_vp = ViewerPanel.class;
		try
		{
			final Field transformEventHandlerField = c_vp.getDeclaredField("transformEventHandler");
			transformEventHandlerField.setAccessible(true);
			transformEventHandlerField.set(viewerP, options.values.getTransformEventHandlerFactory()
					.create(TransformState.from(viewerP.state()::getViewerTransform, viewerP.state()::setViewerTransform)));
			transformEventHandlerField.set(viewerQ, options.values.getTransformEventHandlerFactory()
					.create(TransformState.from(viewerQ.state()::getViewerTransform, viewerQ.state()::setViewerTransform)));
			transformEventHandlerField.setAccessible(false);
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}

		viewerFrameP.updateTransformBehaviors( options );
		viewerFrameQ.updateTransformBehaviors( options );

		// If the images are 2d, use a transform handler that limits
		// transformations to rotations and scalings of the 2d plane ( z = 0 )
		if ( options.values.is2D() )
		{

			// final Class< ViewerPanel > c_vp = ViewerPanel.class;
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

		viewerP.setNumDim( ndims );
		viewerQ.setNumDim( ndims );

//		overlayP.is2D( options.values.is2D() );
//		overlayQ.is2D( options.values.is2D() );

		bwTransform = new BigWarpTransform( landmarkModel );
		bwTransform.initializeInverseParameters(data);

		transformSelector = new TransformTypeSelectDialog( landmarkFrame, this );

		final InputTriggerConfig keyProperties = BigDataViewer.getInputTriggerConfig( options );
		WarpNavigationActions.installActionBindings( getViewerFrameP().getKeybindings(), viewerFrameP, keyProperties, ( ndims == 2 ) );
		BigWarpActions.installActionBindings( getViewerFrameP().getKeybindings(), this, keyProperties );

		WarpNavigationActions.installActionBindings( getViewerFrameQ().getKeybindings(), viewerFrameQ, keyProperties, ( ndims == 2 ) );
		BigWarpActions.installActionBindings( getViewerFrameQ().getKeybindings(), this, keyProperties );

		BigWarpActions.installLandmarkPanelActionBindings( landmarkFrame.getKeybindings(), this, landmarkTable, keyProperties );

		warpVisDialog.toleranceSpinner.setValue( bwTransform.getInverseTolerance() );

		SwingUtilities.invokeLater( () -> {
			landmarkFrame.setVisible( true );
		});
	}

	public void initialize()
	{
		wrapMovingSources( ndims, data );

		// starting view
		if( data.numTargetSources() > 0 )
			data.getTargetSource( 0 ).getSpimSource().getSourceTransform( 0, 0, fixedViewXfm );

//TODO Expose adding these sources via UI

//		final ARGBType white = new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ));
//
//		warpMagSource = addWarpMagnitudeSource( data, ndims == 2, "Warp magnitude" );
//		jacDetSource = addJacobianDeterminantSource( data, "Jacobian determinant" );
//		gridSource = addGridSource( data, "GridSource" );
//		setGridType( GridSource.GRID_TYPE.LINE );
//
//		transformMaskSource = addTransformMaskSource( data, ndims, "Transform mask" );
//		bwTransform.setLambda( transformMask.getRandomAccessible() );
//		addMaskMouseListener();
//
//		// set warp mag source to inactive at the start
//		viewerP.state().setSourceActive( warpMagSource, false );
//		viewerQ.state().setSourceActive( warpMagSource, false );
//		data.sourceColorSettings.put( warpMagSource, new ImagePlusLoader.ColorSettings( -1, 0, 15, white ));
//
//		// set warp grid source to inactive at the start
//		viewerP.state().setSourceActive( gridSource, false );
//		viewerQ.state().setSourceActive( gridSource, false );
//		data.sourceColorSettings.put( gridSource, new ImagePlusLoader.ColorSettings( -1, 0, 255, white ));

		// set jacobian determinant source to inactive at the start
//		viewerP.state().setSourceActive( jacDetSource, false );
//		viewerQ.state().setSourceActive( jacDetSource, false );
//		data.sourceColorSettings.put( jacDetSource, new ImagePlusLoader.ColorSettings( -1, 0.0, 1.0, white ));

		synchronizeSources();

		data.transferChannelSettings( viewerFrameP );
		data.transferChannelSettings( viewerFrameQ );

		updateSourceBoundingBoxEstimators();

		setAllSourcesActiveInFused();
		createMovingTargetGroups();
		viewerP.state().setCurrentGroup( mvgGrp );
		viewerP.state().setCurrentGroup( tgtGrp );

		// set initial transforms so data are visible
//		SwingUtilities.invokeLater( () -> {

			// show moving sources in the moving viewer
			if( data.numMovingSources() == 1 )
				viewerP.state().setCurrentSource( data.getMovingSource( 0 ) );
			else
			{
				viewerP.state().setDisplayMode( DisplayMode.GROUP );
				viewerP.state().setCurrentGroup( mvgGrp );
			}

			// show fixed sources in the fixed viewer
			if( data.numTargetSources() == 1 )
				viewerQ.state().setCurrentSource( data.getTargetSource( 0 ) );
			else
			{
				viewerQ.state().setDisplayMode( DisplayMode.GROUP );
				viewerQ.state().setCurrentGroup( tgtGrp );
			}

			InitializeViewerState.initTransform( viewerP );
			InitializeViewerState.initTransform( viewerQ );

			// save the initial viewer transforms
			initialViewP = new AffineTransform3D();
			initialViewQ = new AffineTransform3D();
			viewerP.state().getViewerTransform( initialViewP );
			viewerQ.state().getViewerTransform( initialViewQ );
//		} );
	}

	protected void setupLandmarkFrame()
	{
		Point loc = null;
		Dimension sz = null;
		if ( landmarkFrame != null )
		{
			loc = landmarkFrame.getLocation();
			sz = landmarkFrame.getSize();

			landmarkModel = null;
			landmarkFrame.setVisible( false );
			landmarkFrame.dispose();
			landmarkFrame = null;
			landmarkPanel = null;

		}

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
		landmarkFrame = new BigWarpLandmarkFrame( "Landmarks", landmarkPanel, this, keymapManager );

		landmarkPopupMenu = new LandmarkPointMenu( this );
		landmarkPopupMenu.setupListeners();

		if( overlayP != null )
			overlayP.setLandmarkPanel(landmarkPanel);

		if( overlayQ != null )
			overlayQ.setLandmarkPanel(landmarkPanel);

		if ( loc != null )
			landmarkFrame.setLocation( loc );

		if ( sz != null )
			landmarkFrame.setSize( sz );

		SwingUtilities.invokeLater( () -> {
			setUpLandmarkMenus();
			landmarkFrame.pack();
		});
	}

	public void synchronizeSources()
	{
		final SynchronizedViewerState pState = viewerP.state();
		final SynchronizedViewerState qState = viewerQ.state();

		final Set<SourceAndConverter<?>> activeSourcesP = new HashSet<>(pState.getActiveSources());
		final Set<SourceAndConverter<?>> activeSourcesQ = new HashSet<>(qState.getActiveSources());

		pState.clearSources();
		qState.clearSources();

		final ArrayList<ConverterSetup> converterSetupsToRemove = new ArrayList<>(setupAssignments.getConverterSetups());
		converterSetupsToRemove.forEach( setupAssignments::removeSetup );

		for ( int i = 0; i < data.sources.size(); i++ )
		{
			final SourceAndConverter< T > sac = data.sources.get( i );
			pState.addSource( sac );
			if (activeSourcesP.contains(sac)) {
				pState.setSourceActive(sac, true);
			}
			qState.addSource( sac );
			if (activeSourcesQ.contains(sac)) {
				qState.setSourceActive(sac, true);
			}

			// update the viewer converter setups too
			final ConverterSetup setup = data.converterSetups.get( i );

			viewerFrameP.getConverterSetups().put( sac, setup );
			viewerFrameQ.getConverterSetups().put( sac, setup );
			setupAssignments.addSetup( setup );
		}
	}

	/**
	 * Sets the viewer state so that every source is shown in vused mode
	 */
	protected void setAllSourcesActiveInFused() {

		viewerP.state().setSourcesActive(data.sources, true);
		viewerQ.state().setSourcesActive(data.sources, true);
	}

	/**
	 * Create two source groups - one for moving images,
	 * and the other for target images, for both viewer frames.
	 *
	 * Ensure sources are synchronized with {@link #synchronizeSources()}
	 * before calling this method.
	 */
	public void createMovingTargetGroups()
	{
		mvgGrp = new SourceGroup();
		tgtGrp = new SourceGroup();

		final SynchronizedViewerState pState = viewerP.state();
		pState.addGroup( mvgGrp );
		pState.addGroup( tgtGrp );
		pState.setGroupName( mvgGrp, "moving images" );
		pState.setGroupName( tgtGrp, "target images" );

		final SynchronizedViewerState qState = viewerQ.state();
		qState.addGroup( mvgGrp );
		qState.addGroup( tgtGrp );
		qState.setGroupName( mvgGrp, "moving images" );
		qState.setGroupName( tgtGrp, "target images" );

		for ( final SourceAndConverter< ? > sac : data.sources )
		{
			if ( data.isMoving( sac ) )
			{
				viewerP.state().addSourceToGroup( sac, mvgGrp );
				viewerQ.state().addSourceToGroup( sac, mvgGrp );
			}
			else
			{
				viewerP.state().addSourceToGroup( sac, tgtGrp );
				viewerQ.state().addSourceToGroup( sac, tgtGrp );
			}
		}
	}

	public int numDimensions()
	{
		return ndims;
	}

	/**
	 * TODO Make a PR that updates this method in InitializeViewerState in bdv-core
	 * @deprecated Use {@link InitializeViewerState} method instead.
	 *
	 * @param cumulativeMinCutoff the min image intensity
	 * @param cumulativeMaxCutoff the max image intensity
	 * @param state the viewer state
	 * @param converterSetups the converter setups
	 */
	@Deprecated
	public static void initBrightness( final double cumulativeMinCutoff, final double cumulativeMaxCutoff, final ViewerState state, final ConverterSetups converterSetups )
	{
		final SourceAndConverter< ? > current = state.getCurrentSource();
		if ( current == null )
			return;
		final Source< ? > source = current.getSpimSource();
		final int timepoint = state.getCurrentTimepoint();
		final Bounds bounds = InitializeViewerState.estimateSourceRange( source, timepoint, cumulativeMinCutoff, cumulativeMaxCutoff );
		final ConverterSetup setup = converterSetups.getConverterSetup( current );
		setup.setDisplayRange( bounds.getMinBound(), bounds.getMaxBound() );
	}

	/**
	 * @param sources to add; typically the output of a {#{@link BigWarpInit#createSources(BigWarpData, String, int, boolean)}} call
	 */
	public void addSources( LinkedHashMap<Source< T >, SourceInfo> sources)
	{

		BigWarpInit.add( data, sources);
		synchronizeSources();
	}

	public void addSource( Source< T > source, boolean moving )
	{
		data.addSource( source, moving );
		synchronizeSources();
	}

	public void addSource( Source<T> source )
	{
		addSource( source, false );
	}

	public int removeSource( SourceInfo info )
	{
		final int removedIdx = data.remove( info );
		synchronizeSources();
		return removedIdx;
	}

	public void removeSource( int i )
	{
		data.remove( i );
		synchronizeSources();
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

		final JMenu fileMenu = new JMenu( "File" );
		viewerMenuBar.add( fileMenu );

		final JMenuItem loadProject = new JMenuItem( actionMap.get( BigWarpActions.LOAD_PROJECT ) );
		loadProject.setText( "Load project" );
		fileMenu.add( loadProject );

		final JMenuItem saveProject = new JMenuItem( actionMap.get( BigWarpActions.SAVE_PROJECT ) );
		saveProject.setText( "Save project" );
		fileMenu.add( saveProject );

		fileMenu.addSeparator();
		final JMenuItem openItem = new JMenuItem( actionMap.get( BigWarpActions.LOAD_LANDMARKS ) );
		openItem.setText( "Import landmarks" );
		fileMenu.add( openItem );

		final JMenuItem saveItem = new JMenuItem( actionMap.get( BigWarpActions.SAVE_LANDMARKS ));
		saveItem.setText( "Export landmarks" );
		fileMenu.add( saveItem );

		fileMenu.addSeparator();
		final JMenuItem openMask = new JMenuItem( actionMap.get( BigWarpActions.MASK_IMPORT ));
		openMask.setText( "Import mask" );
		fileMenu.add( openMask );

		final JMenuItem removeMask = new JMenuItem( actionMap.get( BigWarpActions.MASK_REMOVE ));
		removeMask.setText( "Remove mask" );
		fileMenu.add( removeMask );

		fileMenu.addSeparator();
		final JMenuItem miLoadSettings = new JMenuItem( actionMap.get( BigWarpActions.LOAD_SETTINGS ));
		miLoadSettings.setText( "Load settings" );
		fileMenu.add( miLoadSettings );

		final JMenuItem miSaveSettings = new JMenuItem( actionMap.get( BigWarpActions.SAVE_SETTINGS ));
		miSaveSettings.setText( "Save settings" );
		fileMenu.add( miSaveSettings );


		fileMenu.addSeparator();
		final JMenuItem exportToImagePlus = new JMenuItem( actionMap.get( BigWarpActions.EXPORT_IMAGE ));
		exportToImagePlus.setText( "Export moving image" );
		fileMenu.add( exportToImagePlus );

		final JMenuItem exportWarpField = new JMenuItem( actionMap.get( BigWarpActions.EXPORT_WARP ));
		exportWarpField.setText( "Export transformation" );
		fileMenu.add( exportWarpField );

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

		/* */
		final JMenuItem transformTypeMenu = new JMenuItem( actionMap.get( BigWarpActions.TRANSFORM_TYPE ) );
		transformTypeMenu.setText( "Transformation Options" );
		settingsMenu.add( transformTypeMenu );

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

		final JMenuItem miSrcInfo = new JMenuItem( actionMap.get( BigWarpActions.SHOW_SOURCE_INFO ) );
		miSrcInfo.setText( "Show source information" );
		helpMenu.add( miSrcInfo );
	}

	protected void setupImageJExportOption()
	{
		final ActionMap actionMap = landmarkFrame.getKeybindings().getConcatenatedActionMap();

		final JMenuItem exportToImagePlus = new JMenuItem( actionMap.get( BigWarpActions.EXPORT_IMAGE ) );
		exportToImagePlus.setText( "Export moving image" );
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
		final JFileChooser fileChooser = new JFileChooser( getLastDirectory() );
		File proposedFile = new File( data.getMovingSource( 0 ).getSpimSource().getName() );

		fileChooser.setSelectedFile( proposedFile );
		final int returnVal = fileChooser.showSaveDialog( null );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
		{
			proposedFile = fileChooser.getSelectedFile();
			try
			{
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

		if ( movingSpimData == null )
		{
			IJ.log("Cannot save warped moving image XML, because the input image was not a BDV/XML.");
			return null;
		}

		final AffineTransform3D bigWarpTransform = getMovingToFixedTransformAsAffineTransform3D();

		System.out.println( "bigWarp transform as affine 3d: " + bigWarpTransform.toString() );

		movingSpimData.getViewRegistrations().getViewRegistration( 0, 0 ).preconcatenateTransform(
				new ViewTransformAffine( "Big Warp: " + bwTransform.getTransformType(), bigWarpTransform ) );

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
			new XmlIoSpimData().save( movingSpimData, proposedFile.getAbsolutePath() );
		} catch ( final SpimDataException e )
		{
			e.printStackTrace();
		}

		return proposedFile;
	}

	public AffineTransform3D getMovingToFixedTransformAsAffineTransform3D()
	{
		final double[][] affine3DMatrix = new double[ 3 ][ 4 ];
		final double[][] affine2DMatrix = new double[ 2 ][ 3 ];

		if ( currentTransform == null )
		{
			return null;
		}

		final InvertibleCoordinateTransform transform =
				( ( WrappedCoordinateTransform ) currentTransform ).getTransform().createInverse();

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

		final GenericDialogPlus gd = new GenericDialogPlus( "Apply Big Warp transform" );

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

		gd.addMessage( "Other Output options");
		gd.addChoice( "Interpolation", new String[]{ "Nearest Neighbor", "Linear" }, "Linear" );

		gd.addMessage( "Virtual: fast to display,\n"
				+ "low memory requirements,\nbut slow to navigate" );
		gd.addCheckbox( "virtual?", false );
		final int defaultCores = (int)Math.ceil( Runtime.getRuntime().availableProcessors()/4);
		gd.addNumericField( "threads", defaultCores, 0 );

		gd.addMessage( "Writing options (leave empty to opena new image window)" );
		gd.addDirectoryOrFileField( "File or n5 root", "" );
		gd.addStringField( "n5 dataset", "" );
		gd.addStringField( "n5 block size", "32" );
		gd.addChoice( "n5 compression", new String[] {
				N5ScalePyramidExporter.GZIP_COMPRESSION,
				N5ScalePyramidExporter.RAW_COMPRESSION,
				N5ScalePyramidExporter.LZ4_COMPRESSION,
				N5ScalePyramidExporter.XZ_COMPRESSION,
				N5ScalePyramidExporter.BLOSC_COMPRESSION },
			N5ScalePyramidExporter.GZIP_COMPRESSION );

		gd.showDialog();

		if ( gd.wasCanceled() )
			return;

		final String resolutionOption = gd.getNextChoice();
		final String fieldOfViewOption = gd.getNextChoice();
		final String fieldOfViewPointFilter = gd.getNextString();

		final double[] resolutionSpec = new double[ 3 ];
		resolutionSpec[ 0 ] = gd.getNextNumber();
		resolutionSpec[ 1 ] = gd.getNextNumber();
		resolutionSpec[ 2 ] = gd.getNextNumber();

		final double[] offsetSpec = new double[ 3 ];
		offsetSpec[ 0 ] = gd.getNextNumber();
		offsetSpec[ 1 ] = gd.getNextNumber();
		offsetSpec[ 2 ] = gd.getNextNumber();

		final double[] fovSpec = new double[ 3 ];
		fovSpec[ 0 ] = gd.getNextNumber();
		fovSpec[ 1 ] = gd.getNextNumber();
		fovSpec[ 2 ] = gd.getNextNumber();


		final String interpType = gd.getNextChoice();
		final boolean isVirtual = gd.getNextBoolean();
		final int nThreads = (int)gd.getNextNumber();

		final String fileOrN5Root = gd.getNextString();
		final String n5Dataset = gd.getNextString();
		final String blockSizeString = gd.getNextString();
		final String compressionString = gd.getNextChoice();

		final int[] blockSize = ApplyBigwarpPlugin.parseBlockSize( blockSizeString, this.ndims );
		final Compression compression = ApplyBigwarpPlugin.getCompression( compressionString );
		final WriteDestinationOptions writeOpts = new ApplyBigwarpPlugin.WriteDestinationOptions( fileOrN5Root, n5Dataset,
				blockSize, compression );

		final Interpolation interp;
		if( interpType.equals( "Nearest Neighbor" ))
			interp = Interpolation.NEARESTNEIGHBOR;
		else
			interp = Interpolation.NLINEAR;

		final double[] res = ApplyBigwarpPlugin.getResolution( this.data, resolutionOption, resolutionSpec );

		final List<Interval> outputIntervalList = ApplyBigwarpPlugin.getPixelInterval( this.data,
				this.landmarkModel, this.currentTransform,
				fieldOfViewOption, fieldOfViewPointFilter, bboxOptions, fovSpec, offsetSpec, res );

		final List<String> matchedPtNames = new ArrayList<>();
		if( outputIntervalList.size() > 1 )
			ApplyBigwarpPlugin.fillMatchedPointNames( matchedPtNames, getLandmarkPanel().getTableModel(), fieldOfViewPointFilter );


		// export has to be treated differently if we're doing fov's around
		// landmark centers (because multiple images can be exported this way )
		if( matchedPtNames.size() > 0 )
		{
			final BigwarpLandmarkSelectionPanel<T> selection = new BigwarpLandmarkSelectionPanel<>(
					data, data.sources, fieldOfViewOption,
					outputIntervalList, matchedPtNames, interp,
					offsetSpec, res, isVirtual, nThreads,
					progressWriter );
		}
		else
		{
			if( writeOpts.n5Dataset != null && !writeOpts.n5Dataset.isEmpty())
			{
				final String unit = ApplyBigwarpPlugin.getUnit( data, resolutionOption );
				// export async
				new Thread()
				{
					@Override
					public void run()
					{
						progressWriter.setProgress( 0.01 );
						ApplyBigwarpPlugin.runN5Export( data, fieldOfViewOption,
								outputIntervalList.get(0), interp,
								offsetSpec, res, unit,
								progressWriter, writeOpts,
								Executors.newFixedThreadPool( nThreads ));

						progressWriter.setProgress( 1.00 );
					}
				}.start();
			}
			else
			{
				// export
				final boolean show = ( writeOpts.pathOrN5Root == null  || writeOpts.pathOrN5Root.isEmpty() );
				ApplyBigwarpPlugin.runExport( data, data.sources, fieldOfViewOption,
						outputIntervalList, matchedPtNames, interp,
						offsetSpec, res, isVirtual, nThreads,
						progressWriter, show, false, writeOpts );
			}
		}
	}

	public void exportWarpField()
	{
		ExportDisplacementFieldFrame.createAndShow( this );
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
		return data.sources;
	}

	public BigWarpLandmarkFrame getLandmarkFrame()
	{
		return landmarkFrame;
	}

	public BigWarpLandmarkPanel getLandmarkPanel()
	{
		return landmarkPanel;
	}

	public boolean isInLandmarkMode()
	{
		return inLandmarkMode;
	}

	public void toggleInLandmarkMode()
	{
		setInLandmarkMode( !inLandmarkMode );
	}

	public void setInLandmarkMode( final boolean inLmMode )
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
		final int i = landmarkModel.getNextRow( isMoving );
		if ( i < table.getRowCount() )
		{
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
		final int row = landmarkTable.getSelectedRow();
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
		if( options.values.is2D()  && currentTransform != null )
			transform = ((InvertibleWrapped2DTransformAs3D)currentTransform).getTransform();
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
		// this gets called on startup for some reason, so put this check in
		if( landmarkModel == null )
			return -1;

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

		synchronized( landmarkModel )
		{
			final int N = landmarkModel.getRowCount();
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

		}
		return bestIdx;
	}

	public void selectAllLandmarks()
	{
		getLandmarkPanel().getJTable().selectAll();
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
		for( final SourceAndConverter<?> sac  : data.sources )
		{
			sac.getSpimSource().getSourceTransform(0, 0, xfm );
			final double det = BigWarpUtils.det( xfm );
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

	protected void alignActive( final AlignPlane plane )
	{
		if ( viewerFrameP.isActive() )
		{
			viewerFrameP.getViewerPanel().align( plane );
		}
		else if ( viewerFrameQ.isActive() )
		{
			viewerFrameQ.getViewerPanel().align( plane );
		}
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

	/**
	 * Centers the active viewer at a landmark whose index is an increment from the currently
	 * selected landmark.
	 *
	 * @param inc the increment
	 */
	public void jumpToLandmarkRelative( int inc )
	{
		final int[] selectedRows =  getLandmarkPanel().getJTable().getSelectedRows();

		int row = 0;
		if( selectedRows.length > 0 )
			row = selectedRows[ selectedRows.length - 1 ];

		row = row + inc; // increment to get the *next* row

		// wrap to start if necessary
		if( row >= getLandmarkPanel().getTableModel().getRowCount() )
			row = 0;
		else if( row < 0 )
			row = getLandmarkPanel().getTableModel().getRowCount() - 1;

		// select new row
		getLandmarkPanel().getJTable().setRowSelectionInterval( row, row );

		if( getViewerFrameP().isActive() )
		{
			jumpToLandmark( row, getViewerFrameP().getViewerPanel() );
		}
		else
		{
			jumpToLandmark( row, getViewerFrameQ().getViewerPanel() );
		}
	}

	public void jumpToNextLandmark()
	{
		jumpToLandmarkRelative( 1 );
	}

	public void jumpToPrevLandmark()
	{
		jumpToLandmarkRelative( -1 );
	}

	public void jumpToNearestLandmark()
	{
		if( getViewerFrameP().isActive() )
			jumpToNearestLandmark( getViewerFrameP().getViewerPanel() );
		else
			jumpToNearestLandmark( getViewerFrameQ().getViewerPanel() );
	}

	public void jumpToNearestLandmark( BigWarpViewerPanel viewer )
	{
		if ( inLandmarkMode )
		{
			message.showMessage( "Can't move viewer in landmark mode." );
			return;
		}

		final RealPoint mousePt = new RealPoint( 3 ); // need 3d point even for 2d images
		viewer.getGlobalMouseCoordinates( mousePt );
		jumpToLandmark( landmarkModel.getIndexNearestTo( mousePt, viewer.getIsMoving() ),  viewer );
	}

	public void jumpToSelectedLandmark()
	{
		final int[] selectedRows = getLandmarkPanel().getJTable().getSelectedRows();

		int row = 0;
		if( selectedRows.length > 0 )
			row = selectedRows[ 0 ];

		if( getViewerFrameP().isActive() )
			jumpToLandmark( row, getViewerFrameP().getViewerPanel() );
		else
			jumpToLandmark( row, getViewerFrameQ().getViewerPanel() );
	}

	public void jumpToLandmark( int row, BigWarpViewerPanel viewer )
	{
		if( inLandmarkMode )
		{
			message.showMessage( "Can't move viewer in landmark mode." );
			return;
		}

		if ( BigWarp.this.landmarkModel.getRowCount() < 1 )
		{
			message.showMessage( "No landmarks found." );
			return;
		}

		int offset = 0;
		final int ndims = landmarkModel.getNumdims();
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
						(Double) landmarkModel.getValueAt( row, offset + 3 ),
						0.0 };
		}

		// we have an unmatched point
		if ( Double.isInfinite( pt[ 0 ] ) )
			return;

		final AffineTransform3D startTransform = viewer.state().getViewerTransform();
		final AffineTransform3D destinationTransform = startTransform.copy();
		destinationTransform.set( 0.0, 0, 3 );
		destinationTransform.set( 0.0, 1, 3 );
		destinationTransform.set( 0.0, 2, 3 );

		final double[] center = new double[] { viewer.getWidth() / 2, viewer.getHeight() / 2, 0 };
		final double[] ptxfm = new double[ 3 ];
		destinationTransform.apply( pt, ptxfm );

		// select appropriate row in the table
		landmarkTable.setRowSelectionInterval( row, row );

		// if 2d, make sure the viewer transform change doesn't change the z-slice shown
		final double[] translation; //  = new double[] { center[ 0 ] - ptxfm[ 0 ], center[ 1 ] - ptxfm[ 1 ], -ptxfm[ 2 ] };
		if( ndims == 2 )
			translation = new double[] { center[ 0 ] - ptxfm[ 0 ], center[ 1 ] - ptxfm[ 1 ], startTransform.get(2, 3) };
		else
			translation = new double[] { center[ 0 ] - ptxfm[ 0 ], center[ 1 ] - ptxfm[ 1 ], -ptxfm[ 2 ] };

		// this should work fine in the 2d case
		final TranslationAnimator animator = new TranslationAnimator( startTransform, translation, 300 );
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

	public void goToBookmarkRotation() {

		if (getViewerFrameP().isActive())
			bookmarkEditorP.initGoToBookmarkRotation();
		else if (getViewerFrameP().isActive())
			bookmarkEditorQ.initGoToBookmarkRotation();
	}

	public void setBookmark() {

		if (getViewerFrameP().isActive())
			bookmarkEditorP.initSetBookmark();
		else if (getViewerFrameQ().isActive())
			bookmarkEditorQ.initSetBookmark();
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
		final boolean success = restimateTransformation();
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

	public void toggleMaskOverlayVisibility()
	{
		final BigWarpMaskSphereOverlay overlay = getViewerFrameQ().getViewerPanel().getMaskOverlay();
		if( overlay != null )
			overlay.toggleVisible();
	}

	public void setMaskOverlayVisibility( final boolean visible )
	{
		final BigWarpMaskSphereOverlay overlay = getViewerFrameQ().getViewerPanel().getMaskOverlay();
		if( overlay != null )
			overlay.setVisible( visible );
	}

	protected void addDefaultTableMouseListener()
	{
		landmarkTableListener = new MouseLandmarkTableListener();
		landmarkPanel.getJTable().addMouseListener( landmarkTableListener );
	}

	protected void addMaskMouseListener()
	{
		final Color[] maskColors = new Color[]{ Color.ORANGE, Color.YELLOW };
		// only render mask overlay on target window
		viewerQ.setMaskOverlay( new BigWarpMaskSphereOverlay( viewerQ, maskColors, numDimensions() == 3 ));

		maskSourceMouseListenerQ = new MaskedSourceEditorMouseListener( getLandmarkPanel().getTableModel().getNumdims(), this, viewerQ );
		maskSourceMouseListenerQ.setActive( false );
		maskSourceMouseListenerQ.setMask( plateauTransformMask.getRandomAccessible() );

		plateauTransformMask.getRandomAccessible().setOverlays( Arrays.asList( viewerQ.getMaskOverlay() ));
	}

	public void setGridType( final GridSource.GRID_TYPE method )
	{
		gridSource.setMethod( method );
	}
	
	/**
	 * 
	 */
	public void transformationsFromCoordinateSystem() {

		System.out.println( "transformationsFromCoordinateSystem" );
		final TransformGraph graph = warpVisDialog.transformGraphPanel.getGraph();
		if( graph == null )
			return;

		final String destCsName = warpVisDialog.transformGraphPanel.getCoordinateSystem();
		final boolean isDefault = destCsName.equals(TransformGraphPanel.DEFAULT_COORDINATE_SYSTEM);

		System.out.println( "  : " + destCsName );

		boolean anyUpdated = false;
		for( SourceInfo srcInfo : data.sourceInfos.values() )
		{
			final String csName = srcInfo.getSourceAndConverter().getSpimSource().getName();
			Optional<TransformPath> p = graph.path(destCsName, csName);	
			if( isDefault || csName.equals(destCsName)) 
			{
				data.setTransformation(srcInfo, null, null);
				anyUpdated = true;
			}
			else if( !p.isPresent())
			{
				// presenting coordinate systems in "forward" direction even though we need an "inverse"
				System.err.println( String.format( "WARNING: no suitable transformation found from %s to %s", csName, destCsName ));
			}
			else if( !csName.equals(destCsName)) 
			{
				final RealTransform tform = p.get().totalTransform(warpVisDialog.transformGraphPanel.getN5());
				// TODO make appropriate uri
				// but this is not trivil when the transform is a sequence that is not explicitly stored 
				data.setTransformation(srcInfo, tform, null);
				anyUpdated = true;
			}
			// do nothing 
		}

		if( anyUpdated )
			synchronizeSources();
	}

	public static < T > void wrapMovingSources( final int ndims, final BigWarpData< T > data )
	{
		data.wrapMovingSources();
	}

	public static < T > void wrapMovingSources( final int ndims, final BigWarpData< T > data, int id )
	{
		data.wrapMovingSources(data.getSourceInfo( id ));
	}

	public static < T > List< SourceAndConverter< T > > wrapSourcesAsTransformed( final LinkedHashMap< Integer, SourceInfo > sources,
			final int ndims, final BigWarpData< T > data )
	{
		return data.wrapSourcesAsTransformed();
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	private static < T > JacobianDeterminantSource addJacobianDeterminantSource( final int ndims, final BigWarpData< T > data, final String name )
	{
		final JacobianDeterminantSource jdSource = new JacobianDeterminantSource<>( name, data, new FloatType() );
		final LinkedHashMap< Source<T>, SourceInfo > infos = BigWarpInit.createSources( data, jdSource, JACDET_SOURCE_ID, false );
		final LinkedHashMap< Integer, SourceInfo >  id2Info = new LinkedHashMap<>( 1 );
		id2Info.put( JACDET_SOURCE_ID, infos.get( jdSource ) );
		BigWarpInit.add( data, infos );

		data.getConverterSetup( JACDET_SOURCE_ID ).setDisplayRange( -1, 2 );

		return jdSource;
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	private static < T > WarpMagnitudeSource addWarpMagnitudeSource( final BigWarpData< T > data, final boolean is2D, final String name )
	{
		final WarpMagnitudeSource magSource = new WarpMagnitudeSource<>( name, data, new FloatType() );
		final LinkedHashMap< Source<T>, SourceInfo > infos = BigWarpInit.createSources( data, magSource, WARPMAG_SOURCE_ID, false );
		final LinkedHashMap< Integer, SourceInfo >  id2Info = new LinkedHashMap<>( 1 );
		id2Info.put( WARPMAG_SOURCE_ID, infos.get( magSource ) );
		BigWarpInit.add( data, infos );

		return magSource;
	}

	private static < T > GridSource addGridSource( final int ndims, final BigWarpData< T > data, final String name, boolean isTransformed )
	{
		// this does not set the transformation
		// TODO think about whether its worth it to pass a type parameter.
		final GridSource gridSource = new GridSource<>( name, data, new FloatType(), null );
		gridSource.setMethod( GridSource.GRID_TYPE.LINE );
		final LinkedHashMap< Source<T>, SourceInfo > infos = BigWarpInit.createSources( data, gridSource, GRID_SOURCE_ID, true );
		final LinkedHashMap< Integer, SourceInfo >  id2Info = new LinkedHashMap<>( 1 );
		id2Info.put( GRID_SOURCE_ID, infos.get( gridSource ) );
		BigWarpInit.add( data, infos );
		wrapMovingSources( ndims, data, GRID_SOURCE_ID );

		// the source is transformed
		final SourceAndConverter<?> sourceAndConverter = data.sourceInfos.get(GRID_SOURCE_ID).getSourceAndConverter();
		( ( WarpedSource< ? > ) sourceAndConverter.getSpimSource() ).setIsTransformed( isTransformed );

		return gridSource;
	}

	/**
	 * Call this method when the transform mask type or options have changed.
	 */
	public void updateTransformMask()
	{
		final MaskOptionsPanel maskOpts = warpVisDialog.maskOptionsPanel;
		final String type = maskOpts.getType();
		final boolean masked = maskOpts.isMask();

		// add the transform mask if necessary
		if( masked && transformMaskSource == null )
			addTransformMaskSource();

		// update the bigwarp transform
		final boolean isVirtualMask = isVirtualMask();
		getBwTransform().setMaskInterpolationType( type );
		setMaskOverlayVisibility( maskOpts.showMaskOverlay() && masked && isVirtualMask );

		if (masked && isVirtualMask)
			autoEstimateMask();

		restimateTransformation();
		getViewerFrameQ().getViewerPanel().requestRepaint();
		getViewerFrameP().getViewerPanel().requestRepaint();
	}

	private boolean isVirtualMask() {

		return plateauTransformMask == transformMask;
	}

	public void refreshTransformMask()
	{
		getBwTransform().setLambda(
			transformMaskSource.getSpimSource().getInterpolatedSource(0, 0, Interpolation.NLINEAR));
	}

	public void setTransformMaskRange( double min, double max )
	{
		getBwTransform().setMaskIntensityBounds(min, max);
		warpVisDialog.maskOptionsPanel.getMaskRangeSlider().setRange(
				new BoundedRange( min, max, min, max ));
	}

	public void setTransformMaskType( final String maskInterpolationType )
	{
		getBwTransform().setMaskInterpolationType(maskInterpolationType);
		warpVisDialog.maskOptionsPanel.getMaskTypeDropdown().setSelectedItem(maskInterpolationType);
	}

	public void setTransformMaskProperties( final FalloffShape falloffShape,
			final double squaredRadius, double[] center )
	{
		warpVisDialog.maskOptionsPanel.getMaskFalloffTypeDropdown().setSelectedItem(falloffShape);

		final PlateauSphericalMaskRealRandomAccessible mask = getTransformPlateauMaskSource().getRandomAccessible();
		mask.setFalloffShape( falloffShape );
		mask.setSquaredRadius( squaredRadius );
		mask.setCenter( center );
	}

	public void setTransformMaskProperties( final FalloffShape falloffShape,
			final double squaredRadius,
			final double squaredSigma,
			double[] center )
	{
		warpVisDialog.maskOptionsPanel.getMaskFalloffTypeDropdown().setSelectedItem(falloffShape);

		final PlateauSphericalMaskRealRandomAccessible mask = getTransformPlateauMaskSource().getRandomAccessible();
		mask.setFalloffShape( falloffShape );
		mask.setSquaredRadius( squaredRadius );
		mask.setSquaredSigma( squaredSigma );
		mask.setCenter( center );
	}

	public void importTransformMaskSourceDialog() {

		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );
		fileChooser.setCurrentDirectory(lastDirectory);

		final int ret = fileChooser.showOpenDialog(landmarkFrame);
		if ( ret == JFileChooser.APPROVE_OPTION ) {

			final File selection = fileChooser.getSelectedFile();
			importTransformMaskSource( selection.getAbsolutePath() );
		}

	}

	@SuppressWarnings( { "unchecked" } )
	public void importTransformMaskSource( final String uri ) {

		// first remove any existing mask source
		final SourceInfo srcInfo = data.sourceInfos.get(TRANSFORM_MASK_SOURCE_ID);
		if( srcInfo != null )
			removeSource(srcInfo);

		LinkedHashMap<Source<T>, SourceInfo> infos;
		try {
			infos = BigWarpInit.createSources(data, uri, TRANSFORM_MASK_SOURCE_ID, false);
			BigWarpInit.add( data, infos, null, null );

			infos.entrySet().stream().map( e -> { return e.getKey(); }).findFirst().ifPresent( x -> {
				transformMask = (Source<? extends RealType<?>>)x;
			});
			synchronizeSources();

		} catch (final URISyntaxException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final SpimDataException e) {
			e.printStackTrace();
		}

		bwTransform.setLambda( transformMask.getInterpolatedSource(0, 0, Interpolation.NLINEAR));
		updateTransformMask();

		final RealType<?> type = transformMask.getType();
		if( !(type instanceof DoubleType ) && !(type instanceof FloatType ))
		{
			final double min = type.getMinValue();
			final double max = type.getMaxValue();
			bwTransform.setMaskIntensityBounds( min, max );
			warpVisDialog.maskOptionsPanel.getMaskRangeSlider().setRange(new BoundedRange( min, max, min, max ));
		}
	}

	/**
	 * Run this after loading projet to set the transform mask from a loaded source
	 */
	@SuppressWarnings("unchecked")
	public void connectMaskSource()
	{
		transformMask = (Source<? extends RealType<?>>)data.sourceInfos.get(TRANSFORM_MASK_SOURCE_ID).getSourceAndConverter().getSpimSource();
		bwTransform.setLambda( transformMask.getInterpolatedSource(0, 0, Interpolation.NLINEAR));
	}

	public void removeMaskSource() {

		final SourceInfo srcInfo = data.sourceInfos.get(TRANSFORM_MASK_SOURCE_ID);
		if( srcInfo != null )
			removeSource(srcInfo);

		transformMask = null;
		transformMaskSource = null;
		bwTransform.setLambda(null);
		bwTransform.setMaskInterpolationType(BigWarpTransform.NO_MASK_INTERP);
		warpVisDialog.maskOptionsPanel.getMaskTypeDropdown().setSelectedItem(BigWarpTransform.NO_MASK_INTERP);

		final BigWarpMaskSphereOverlay overlay = getViewerFrameQ().getViewerPanel().getMaskOverlay();
		if( overlay != null )
			overlay.setVisible(false);

		updateTransformMask();

		restimateTransformation();
	}

	@SuppressWarnings( { "unchecked", "rawtypes", "hiding" } )
	public <T extends RealType<T>> SourceAndConverter< T > addTransformMaskSource()
	{
		if( warpVisDialog == null )
			return null;

		if( transformMask != null)
		{
			return ( SourceAndConverter< T > ) data.getSourceInfo( TRANSFORM_MASK_SOURCE_ID ).getSourceAndConverter();
		}

		// think about whether its worth it to pass a type parameter. or should we just stick with Floats?
		final BoundingBoxEstimation bbe = new BoundingBoxEstimation();
		final AffineTransform3D affine = new AffineTransform3D();
		data.getTargetSource( 0 ).getSpimSource().getSourceTransform( 0, 0, affine );
		final Interval itvl = bbe.estimatePixelInterval(  affine, data.getTargetSource( 0 ).getSpimSource().getSource( 0, 0 ) );

		plateauTransformMask = PlateauSphericalMaskSource.build( new RealPoint( 3 ), itvl );
		transformMask = plateauTransformMask;


		final RealARGBColorConverter< T > converter = (RealARGBColorConverter<T>)RealARGBColorConverter.create( plateauTransformMask.getType(), 0, 1 );
		converter.setColor( new ARGBType( 0xffffffff ) );
		final SourceAndConverter< T > soc = new SourceAndConverter<T>( (Source<T>)transformMask, converter, null );
		data.converterSetups.add( BigDataViewer.createConverterSetup( soc, TRANSFORM_MASK_SOURCE_ID ) );
		data.sources.add( ( SourceAndConverter ) soc );

		final SourceInfo sourceInfo = new SourceInfo( TRANSFORM_MASK_SOURCE_ID, false, transformMask.getName(), null, null );
		sourceInfo.setSourceAndConverter( soc );
		data.sourceInfos.put( TRANSFORM_MASK_SOURCE_ID, sourceInfo);

		// connect to UI
		warpVisDialog.maskOptionsPanel.setMask( plateauTransformMask );
		addMaskMouseListener();
		bwTransform.setLambda( plateauTransformMask.getRandomAccessible() );
		bwTransform.setMaskIntensityBounds(0, 1);

		final ArrayList<BigWarpMaskSphereOverlay> overlayList = new ArrayList<>();
		final BigWarpMaskSphereOverlay overlay = new BigWarpMaskSphereOverlay( viewerQ, ndims==3 );
		overlayList.add( overlay );

		// first attach the overlay to the viewer
		getViewerFrameQ().getViewerPanel().setMaskOverlay( overlay );
		plateauTransformMask.getRandomAccessible().setOverlays( overlayList );


		synchronizeSources();
		transformMaskSource = soc;
		return soc;
	}

	public Source<? extends RealType<?>> getTansformMaskSource()
	{
		return transformMask;
	}

	public PlateauSphericalMaskSource getTransformPlateauMaskSource()
	{
		return plateauTransformMask;
	}

	public void setupKeyListener()
	{
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor( new LandmarkKeyboardProcessor( this ) );
	}

	public void setWarpVisGridType( final GridSource.GRID_TYPE type )
	{
		gridSource.setMethod( type );
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
	}

	public void setWarpGridWidth( final double width )
	{
		gridSource.setGridWidth( width );
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
	}

	public void setWarpGridSpacing( final double spacing )
	{
		gridSource.setGridSpacing( spacing );
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
	}

	public void setAutoSaver( final BigWarpAutoSaver autoSaver )
	{
		this.autoSaver = autoSaver;
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

		landmarkModel.copyLandmarks( p, q );
		Arrays.fill( w, 1.0 );

		if( warpMagSource != null )
		{
			try
			{
				final AbstractModel< ? > baseline = this.baseXfmList[ baselineModelIndex ];
				baseline.fit( p, q, w );  // FITBASELINE
				final WrappedCoordinateTransform baselineTransform = new WrappedCoordinateTransform(
						(InvertibleCoordinateTransform)baseline, ndims );

				// the transform to compare is the inverse (because we use it for rendering)
				// so need to give the inverse transform for baseline as well
				warpMagSource.setBaseline( baselineTransform.inverse() );
			}
			catch ( final IllDefinedDataPointsException | NotEnoughDataPointsException e )
			{
				e.printStackTrace();
			}

			getViewerFrameP().getViewerPanel().requestRepaint();
			getViewerFrameQ().getViewerPanel().requestRepaint();
		}
	}

	public void setMovingSpimData( SpimData movingSpimData, File movingImageXml )
	{
		this.movingSpimData = movingSpimData;
		this.movingImageXml = movingImageXml;
	}

	public void setBookmarks( final Bookmarks bookmarks )
	{
		this.bookmarks = bookmarks;
	}

	public void setAutoEstimateMask( final boolean selected )
	{
		warpVisDialog.maskOptionsPanel.getAutoEstimateMaskButton().setSelected( selected );
	}

	public void autoEstimateMask()
	{
		if( landmarkModel.numActive() < 4 )
			return;

		if( warpVisDialog.autoEstimateMask() && bwTransform.isMasked() && transformMask instanceof PlateauSphericalMaskSource )
		{
			final Sphere sph = BoundingSphereRitter.boundingSphere(landmarkModel.getFixedPointsCopy());
			plateauTransformMask.getRandomAccessible().setCenter(sph.getCenter());
			plateauTransformMask.getRandomAccessible().setRadius(sph.getRadius());

			final AbstractTransformSolver< ? > solver = getBwTransform().getSolver();
			if( solver instanceof MaskedSimRotTransformSolver )
			{
				((MaskedSimRotTransformSolver)solver).setCenter( sph.getCenter() );
			}
		}
	}

	public enum WarpVisType
	{
		NONE, WARPMAG, JACDET, GRID
	}

	@Deprecated
	public void setWarpVisMode( final WarpVisType type, BigWarpViewerFrame viewerFrame, final boolean both )
	{
		setWarpVisMode( type );
	}

	protected void setWarpVisMode( final WarpVisType type )
	{
		// TODO replace the above with this method
		BigWarpViewerFrame viewerFrame ;
		if ( viewerFrameP.isActive() )
			viewerFrame = viewerFrameP;
		else
			viewerFrame = viewerFrameQ;

		if ( currentTransform == null )
		{
			message.showMessage( "No warp - estimate warp first." );
			return;
		}
		final ViewerState state = viewerFrame.getViewerPanel().state();

		switch ( type )
		{
		case JACDET:
		{
			// turn jacobian determinant
			if ( jacDetSource == null )
			{
				jacDetSource = addJacobianDeterminantSource( ndims, data, "Jacobian determinant" );
				updateJacobianTransformation(currentTransform);
				synchronizeSources();
			}
			showSourceFused( viewerFrame, JACDET_SOURCE_ID );

			viewerFrame.getViewerPanel().showMessage( "Displaying Jacobian Determinant" );
			break;
		}
		case WARPMAG:
		{
			// turn warp mag on
			if ( warpMagSource == null )
			{

				warpMagSource = addWarpMagnitudeSource( data, ndims == 2, "Warp magnitude" );
				synchronizeSources();
			}
			showSourceFused( viewerFrame, WARPMAG_SOURCE_ID );

			// estimate the max warp
//			final WarpMagnitudeSource< ? > wmSrc = ( ( WarpMagnitudeSource< ? > ) sources.get( warpMagSourceIndex ).getSpimSource() );
//			final double maxval = wmSrc.getMax( landmarkModel );

			// set the slider
//			( ( RealARGBColorConverter< FloatType > ) ( sources.get( warpMagSourceIndex ).getConverter() ) ).setMax( maxval );


			viewerFrame.getViewerPanel().showMessage( "Displaying Warp Magnitude" );
			break;
		}
		case GRID:
		{
			// turn grid vis on
			if ( gridSource == null )
			{
				gridSource = addGridSource( ndims, data, "Transform grid", isMovingDisplayTransformed() );
				setTransform( data.sourceInfos.get(GRID_SOURCE_ID), currentTransform );
				synchronizeSources();
				data.getConverterSetup( GRID_SOURCE_ID ).setDisplayRange( 0, 512 );
			}
			showSourceFused( viewerFrame, GRID_SOURCE_ID );

			viewerFrame.getViewerPanel().showMessage( "Displaying Warp Grid" );
			break;
		}
		default:
		{
			if ( warpMagSource != null )
				state.setSourceActive( data.getSourceInfo( WARPMAG_SOURCE_ID ).getSourceAndConverter(), false );

			if ( gridSource != null )
				state.setSourceActive( data.getSourceInfo( GRID_SOURCE_ID ).getSourceAndConverter(), false );

			message.showMessage( "Turning off warp vis" );
			break;
		}
		}
	}

	public void toggleWarpVisMode( BigWarpViewerFrame viewerFrame )
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
			else
				return;
		}

		if( getBwTransform().getTransformation() == null )
		{
			message.showMessage( "No warp - estimate warp first." );
			return;
		}

		final ViewerState state = viewerFrame.getViewerPanel().state();

		// TODO consider remembering whether fused was on before displaying warpmag
		// so that its still on or off after we turn it off

		final SourceAndConverter< ? > wmSac = data.getSourceInfo( WARPMAG_SOURCE_ID ).getSourceAndConverter();
		if ( state.isSourceActive( wmSac ) ) // warp mag is visible, turn it off
		{
			state.setSourceActive( wmSac, false );

			state.setDisplayMode( state.getDisplayMode().withFused( false ) );
			message.showMessage( "Removing Warp Magnitude" );
		}
		else // warp mag is invisible, turn it on
		{
			state.setSourceActive( wmSac, true );

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

	private void showSourceFused(BigWarpViewerFrame viewerFrame, int sourceId) {

		if (viewerFrame == null) {
			if (viewerFrameP.isActive()) {
				viewerFrame = viewerFrameP;
			} else if (viewerFrameQ.isActive()) {
				viewerFrame = viewerFrameQ;
			} else
				return;
		}

		if (getBwTransform().getTransformation() == null) {
			message.showMessage("No warp - estimate warp first.");
			return;
		}

		final SourceAndConverter< ? > newSrc = data.getSourceInfo( sourceId ).getSourceAndConverter();

		final ViewerState state = viewerFrame.getViewerPanel().state();
		if (!state.getDisplayMode().equals(DisplayMode.FUSED)) {
			final SourceAndConverter<?> currentSource = state.getCurrentSource();
			for( final SourceAndConverter<?> src : state.getSources())
				state.setSourceActive(src, src == currentSource || src == newSrc );
		}
		else {

			final SourceInfo warpMagSrcInfo = data.getSourceInfo( WARPMAG_SOURCE_ID );
			final SourceInfo gridSrcInfo = data.getSourceInfo( GRID_SOURCE_ID );
			final SourceInfo jacDetSrcInfo = data.getSourceInfo( JACDET_SOURCE_ID );

			// un-dispolay all the warp vis sources
			if (warpMagSrcInfo != null)
				state.setSourceActive(warpMagSrcInfo.getSourceAndConverter(), false);

			if (gridSrcInfo != null)
				state.setSourceActive(gridSrcInfo.getSourceAndConverter(), false);

			if (jacDetSrcInfo != null)
				state.setSourceActive(jacDetSrcInfo.getSourceAndConverter(), false);

			// activate the requested one
			state.setSourceActive(newSrc, true);
		}

		state.setDisplayMode(DisplayMode.FUSED);
	}

	private void setTransformationMovingSourceOnly( final InvertibleRealTransform transform )
	{
		this.currentTransform = transform;
		data.sourceInfos.values().forEach( sourceInfo -> {
			setTransform( sourceInfo, transform );
		} );
	}

	private void setTransform( final SourceInfo sourceInfo, final InvertibleRealTransform transform )
	{
		if ( sourceInfo.isMoving() )
		{
			// the xfm must always be 3d for bdv to be happy.
			// when bigwarp has 2d images though, the z- component will be left unchanged
			// InverseRealTransform xfm = new InverseRealTransform( new TpsTransformWrapper( 3, transform ));

			// the updateTransform method creates a copy of the transform
			final SourceAndConverter< ? > sac = sourceInfo.getSourceAndConverter();
			final WarpedSource< ? > wsrc = ( WarpedSource< ? > ) sac.getSpimSource();
			wsrc.updateTransform( transform );
			if ( sac.asVolatile() != null )
				( ( WarpedSource< ? > ) sourceInfo.getSourceAndConverter().asVolatile().getSpimSource() ).updateTransform( transform );
		}
	}

	public void updateSourceBoundingBoxEstimators()
	{
		data.sourceInfos.values().forEach( sourceInfo -> {
			if ( sourceInfo.isMoving() )
			{
				// the xfm must always be 3d for bdv to be happy.
				// when bigwarp has 2d images though, the z- component will be left unchanged
				//InverseRealTransform xfm = new InverseRealTransform( new TpsTransformWrapper( 3, transform ));

				// the updateTransform method creates a copy of the transform
				( ( WarpedSource< ? > ) sourceInfo.getSourceAndConverter().getSpimSource() ).setBoundingBoxEstimator( bboxOptions.copy() );
				if ( data.sources.get( 0 ).asVolatile() != null )
					( ( WarpedSource< ? > ) sourceInfo.getSourceAndConverter().asVolatile().getSpimSource() ).setBoundingBoxEstimator( bboxOptions.copy() );
			}
		} );
	}

	private synchronized void notifyTransformListeners( )
	{
		for ( final TransformListener< InvertibleRealTransform > l : transformListeners )
			l.transformChanged( currentTransform );
	}

	private void updateJacobianTransformation( final InvertibleRealTransform transform )
	{
		if( transform instanceof ThinplateSplineTransform )
		{
			jacDetSource.setTransform( (ThinplateSplineTransform)transform );
		}
		else if ( transform instanceof WrappedIterativeInvertibleRealTransform )
		{
			final RealTransform xfm = ((WrappedIterativeInvertibleRealTransform)transform).getTransform();
			if( xfm instanceof ThinplateSplineTransform )
				jacDetSource.setTransform( (ThinplateSplineTransform) xfm );
			else
				jacDetSource.setTransform( new RealTransformFiniteDerivatives( xfm ));
		}
		else if ( transform instanceof InvertibleWrapped2DTransformAs3D )
		{
			updateJacobianTransformation(((InvertibleWrapped2DTransformAs3D)transform).getTransform());
		}
		else
			jacDetSource.setTransform( null );
	}

	private void setTransformationAll( final InvertibleRealTransform transform )
	{
		setTransformationMovingSourceOnly( transform );

		if( warpMagSource != null )
		{
			warpMagSource.setWarp( transform );
			fitBaselineWarpMagModel();
		}

		if( jacDetSource != null )
			updateJacobianTransformation(transform);
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

	public synchronized void setIsMovingDisplayTransformed( final boolean isTransformed )
	{

		data.sourceInfos.values().forEach( sourceInfo -> {
			if ( sourceInfo.isMoving() )
			{
				final SourceAndConverter< ? > sourceAndConverter = sourceInfo.getSourceAndConverter();
				( ( WarpedSource< ? > ) sourceAndConverter.getSpimSource() ).setIsTransformed( isTransformed );

				if ( sourceAndConverter.asVolatile() != null )
					( ( WarpedSource< ? > ) sourceAndConverter.asVolatile().getSpimSource() ).setIsTransformed( isTransformed );
			}
		} );

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
		final LandmarkTableModel ltm = landmarkPanel.getTableModel();
		return ltm.isPointUpdatePending() || ltm.isPointUpdatePendingMoving();
	}

	public boolean isMovingDisplayTransformed()
	{
		// this implementation is okay, so long as all the moving images have the same state of 'isTransformed'
//		return ( ( WarpedSource< ? > ) ( data.sources.get( data.movingSourceIndexList.get( 0 ) ).getSpimSource() ) ).isTransformed();

		// TODO better to explicitly keep track of this?
		if( data.sources.size() <= 0 )
			return true;
		else if( data.numMovingSources() > 0 )
			return ( ( WarpedSource< ? > ) ( data.getMovingSource( 0 ).getSpimSource() ) ).isTransformed();
		else
			return false;
	}

	/**
	 * The display will be in 3d if any of the input sources are 3d.
	 * @return dimension of the input sources
	 */
	protected int detectNumDims()
	{
		return detectNumDims( data.sources );
	}

	/**
	 * The display will be in 3d if any of the input sources are 3d.
	 * @param sources the sources
	 * @param <T> the type
	 * @return dimension of the input sources
	 */
	public static <T> int detectNumDims( List< SourceAndConverter< T > > sources )
	{
		// default to 3D if bigwarp is opened without any sources
		if( sources.size() == 0 )
			return 3;

		boolean isAnySource3d = false;
		for ( final SourceAndConverter< T > sac : sources )
		{
			final long[] dims = new long[ sac.getSpimSource().getSource( 0, 0 ).numDimensions() ];
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


			final ProgressWriterIJ progress = new ProgressWriterIJ();
			BigWarp bw;
			BigWarpData bwdata;
			if ( fnP.endsWith( "xml" ) && fnQ.endsWith( "xml" ) )
			{
				bwdata = BigWarpInit.createBigWarpDataFromXML( fnP, fnQ );
				bw = new BigWarp<>( bwdata, progress );
			}
			else if ( fnP.endsWith( "xml" ) && !fnQ.endsWith( "xml" ) )
			{
				final ImagePlus impQ = IJ.openImage( fnQ );
				bwdata = BigWarpInit.createBigWarpDataFromXMLImagePlus( fnP, impQ );
				bw = new BigWarp<>( bwdata, progress );
			}
			else if ( !fnP.endsWith( "xml" ) && fnQ.endsWith( "xml" ) )
			{
				final ImagePlus impP = IJ.openImage( fnP );
				bwdata = BigWarpInit.createBigWarpDataFromImagePlusXML( impP, fnQ );
				bw = new BigWarp<>( bwdata, progress );
			}
			else if (!fnP.isEmpty() && !fnQ.isEmpty())
			{
				final ImagePlus impP = IJ.openImage( fnP );
				final ImagePlus impQ = IJ.openImage( fnQ );

				// For testing display and color settings
//				impP.setDisplayRange( 10, 200 );
//				impQ.setDisplayRange( 20, 180 );
//				impP.show();
//				impQ.show();

				if ( !( impP == null || impQ == null ) )
				{
					bwdata = BigWarpInit.createBigWarpDataFromImages( impP, impQ );
					bw = new BigWarp<>( bwdata, progress );
				}
				else
				{
					System.err.println( "Error reading images" );
					return;
				}
			}
			else
			{
				bw = new BigWarp<>( new BigWarpData<>(), progress );
			}

			if ( !fnLandmarks.isEmpty() )
				bw.loadLandmarks( fnLandmarks );

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
		final AffineTransform3D srcTransform0 = new AffineTransform3D();
		data.sources.get( 0 ).getSpimSource().getSourceTransform(0, 0, srcTransform0 );

		final AffineTransform3D srcTransform1 = new AffineTransform3D();
		data.sources.get( 1 ).getSpimSource().getSourceTransform(0, 0, srcTransform1 );

		final AffineTransform3D viewerTransformM = new AffineTransform3D();
		viewerP.state().getViewerTransform( viewerTransformM );

		final AffineTransform3D viewerTransformT = new AffineTransform3D();
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
			final long clickLength = System.currentTimeMillis() - pressTime;

			if( clickLength < keyClickMaxLength && selectedPointIndex != -1 )
				return;

			// shift when
			boolean isMovingLocal = isMoving;
			if ( e.isShiftDown() && e.isControlDown() )
			{
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
					solverThread.requestResolve( isMoving, selectedPointIndex, ptarrayLoc );
				}
				else
				{
					// Make a non-undoable edit so that the point can be displayed correctly
					// the undoable action is added on mouseRelease
					if( isMoving && isMovingDisplayTransformed() )
					{
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
			final int hoveredIndex = BigWarp.this.selectedLandmark( hoveredArray, isMoving, false );
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
			final LandmarkTableModel model = ( LandmarkTableModel ) table.getModel();
			final Component c = super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
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
			if ( e.getColumn() == LandmarkTableModel.ACTIVECOLUMN )
			{
				BigWarp.this.restimateTransformation();
				BigWarp.this.landmarkPanel.repaint();
			}
			autoEstimateMask();
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

				final boolean moving;
				final BigWarpViewerPanel viewer;
				if ( column >= ( 2 + ndims ) )
				{
					// clicked on a fixed point
					viewer = BigWarp.this.viewerQ;
					moving = false;
				}
				else if ( column >= 2 && column < ( 2 + ndims ) )
				{
					// clicked on a moving point
					viewer = BigWarp.this.viewerP;
					moving = true;
				}
				else
				{
					// we're in a column that doesn't correspond to a point and should do nothing
					moving = true;
					return;
				}

				final LandmarkTableModel ltm = BigWarp.this.landmarkModel;
				if( moving && !ltm.isMovingPoint( row ))
					return;

				if( !moving && !ltm.isFixedPoint( row ))
					return;

				jumpToLandmark(row, viewer);
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
		updateTransformTypePanel(type);
		updateTransformTypeDialog(type);
		bwTransform.setTransformType(type);
		this.restimateTransformation();
	}

	public void setTransformTypeUpdateUI( final String type )
	{
		setTransformType( type );
		updateTransformTypeDialog( type );
		updateTransformTypePanel( type );
	}

	/**
	 * Update the transformation selection dialog to reflect the given transform type selection.
	 *
	 * @param type the transformation type
	 */
	public void updateTransformTypeDialog( final String type )
	{
		transformSelector.deactivate();
		transformSelector.setTransformType( type );
		transformSelector.activate();
	}

	/**
	 * Update the transformation selection panel in the options dialog to reflect the given transform type selection.
	 *
	 * @param type the transformation type
	 */
	public void updateTransformTypePanel( final String type )
	{
		warpVisDialog.transformTypePanel.deactivate();
		warpVisDialog.transformTypePanel.setType( type );
		warpVisDialog.transformTypePanel.activate();
	}

	public String getTransformType()
	{
		return bwTransform.getTransformType();
	}

	public BigWarpTransform getBwTransform()
	{
		return bwTransform;
	}

	public BoundingBoxEstimation getBoxEstimation()
	{
		return bboxOptions;
	}

	/**
	 * @deprecated Use getTps, getTpsBase, or getTransformation instead
	 *
	 * @return the {@link ThinPlateR2LogRSplineKernelTransform} iinnstance
	 */
	@Deprecated
	public ThinPlateR2LogRSplineKernelTransform getTransform()
	{
		return landmarkPanel.getTableModel().getTransform();
	}

	public synchronized void addTransformListener( TransformListener< InvertibleRealTransform > listener )
	{
		transformListeners.add( listener );
	}


//	public InvertibleRealTransform getTransformation( final int index )
//	{
//		int ndims = 3;
//		InvertibleRealTransform invXfm = null;
//		if( transformType.equals( TransformTypeSelectDialog.TPS ))
//		{
//			final ThinPlateR2LogRSplineKernelTransform xfm;
//			if ( index >= 0 ) // a point position is modified
//			{
//				LandmarkTableModel tableModel = getLandmarkPanel().getTableModel();
//				if ( !tableModel.getIsActive( index ) )
//					return null;
//
//				int numActive = tableModel.numActive();
//				ndims = tableModel.getNumdims();
//
//				double[][] mvgPts = new double[ ndims ][ numActive ];
//				double[][] tgtPts = new double[ ndims ][ numActive ];
//
//				tableModel.copyLandmarks( mvgPts, tgtPts );
//
//				// need to find the "inverse TPS" - the transform from target space to moving space.
//				xfm = new ThinPlateR2LogRSplineKernelTransform( ndims, tgtPts, mvgPts );
//			}
//			else // a point is added
//			{
//				landmarkModel.initTransformation();
//				ndims = landmarkModel.getNumdims();
//				xfm = getLandmarkPanel().getTableModel().getTransform();
//			}
//			invXfm = new WrappedIterativeInvertibleRealTransform<>( new ThinplateSplineTransform( xfm ));
//		}
//		else
//		{
//			Model<?> model = getModelType();
//			fitModel(model);
//			int nd = landmarkModel.getNumdims();
//			invXfm = new WrappedCoordinateTransform( (InvertibleCoordinateTransform) model, nd ).inverse();
//		}
//
//		if( options.is2d )
//		{
//			invXfm = new Wrapped2DTransformAs3D( invXfm );
//		}
//
//		return invXfm;
//	}

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
						final InvertibleRealTransform invXfm = bw.bwTransform.getTransformation( index );

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
//							bw.setTransformationMovingSourceOnly( invXfm );
//							bw.data.updateEditableTransformation( invXfm );
							bw.setTransformationAll( invXfm );
						}

						// update fixed point - but don't allow undo/redo
						// and update warped point
						// both for rendering purposes
						if ( !isMoving )
						{
							bw.getLandmarkPanel().getTableModel().setPoint( index, isMoving, pt, false, bw.currentTransform );
						}

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

	/**
	 * Saves landmarks to a new File in the user's bigwarp folder.
	 */
	public void autoSaveLandmarks()
	{
		final File baseFolder;
		if ( autoSaver.autoSaveDirectory != null )
			baseFolder = autoSaver.autoSaveDirectory;
		else
			baseFolder = getBigwarpSettingsFolder();

		final File proposedLandmarksFile = new File( baseFolder.getAbsolutePath() +
				File.separator + "bigwarp_landmarks_" +
				new SimpleDateFormat( "yyyyMMdd-HHmmss" ).format( Calendar.getInstance().getTime() ) +
				".csv" );

		try
		{
			saveLandmarks( proposedLandmarksFile.getCanonicalPath() );
		}
		catch ( final IOException e ) { e.printStackTrace(); }
	}

	/**
	 * Saves landmarks to either the last File the user
	 * saved landmarks to, or a unique location in the user's bigwarp folder.
	 *
	 */
	public void quickSaveLandmarks()
	{
		if(lastLandmarks != null)
		{
			try
			{
				saveLandmarks( lastLandmarks.getCanonicalPath() );
			}
			catch ( final IOException e ) { e.printStackTrace(); }
		}
		else
		{
			autoSaveLandmarks();
			return;
		}
	}

	/**
	 * Returns the default location for bigwarp settings / auto saved files: ~/.bigwarp
	 * @return the folder
	 */
	public File getBigwarpSettingsFolder()
	{

		final File hiddenFolder = new File( System.getProperty( "user.home" ) + File.separator + ".bigwarp");
		boolean exists = hiddenFolder.isDirectory();
		if( !exists )
		{
			exists = hiddenFolder.mkdir();
		}

		if( exists )
			return hiddenFolder;
		else
			return null;
	}

	/**
	 * Returns the {@link BigWarpAutoSaver}.
	 *
	 * @return the {@link BigWarpAutoSaver} instance.
	 */
	public BigWarpAutoSaver getAutoSaver()
	{
		return autoSaver;
	}

	public void stopAutosave()
	{
		if( autoSaver != null )
		{
			autoSaver.stop();
			autoSaver = null;
		}
	}

	protected void saveLandmarks()
	{
		final JFileChooser fileChooser = new JFileChooser( getLastDirectory() );
		File proposedLandmarksFile;
		if(lastLandmarks != null)
			proposedLandmarksFile = lastLandmarks;
		else
			proposedLandmarksFile = new File( "landmarks.csv" );

		fileChooser.setSelectedFile( proposedLandmarksFile );
		final int returnVal = fileChooser.showSaveDialog( null );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
		{
			proposedLandmarksFile = fileChooser.getSelectedFile();
			try
			{
				saveLandmarks( proposedLandmarksFile.getCanonicalPath() );
				lastLandmarks = proposedLandmarksFile;
			} catch ( final IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	protected void saveLandmarks( final String filename ) throws IOException
	{
		if( filename.endsWith("csv"))
			landmarkModel.save(new File( filename ));
		else if( filename.endsWith("json"))
			TransformWriterJson.write(landmarkModel, bwTransform, new File( filename ));
	}

	protected void loadLandmarks()
	{
		final JFileChooser fileChooser = new JFileChooser( getLastDirectory() );
		File proposedLandmarksFile = new File( "landmarks.csv" );

		fileChooser.setSelectedFile( proposedLandmarksFile );
		final int returnVal = fileChooser.showOpenDialog( null );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
		{
			proposedLandmarksFile = fileChooser.getSelectedFile();
			try
			{
				loadLandmarks( proposedLandmarksFile.getCanonicalPath() );
			} catch ( final IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	public void loadLandmarks( final String filename )
	{

		final String filenameTrim = filename.trim();
		String name = filenameTrim;
		try {
			final URI uri = new URI( filenameTrim );
			if( uri != null && uri.getScheme() != null )
				name = uri.getSchemeSpecificPart();
		}
		catch ( URISyntaxException e ) { }

		final File file = new File( name );
		setLastDirectory( file.getParentFile() );
		if( name.endsWith( "csv" ))
		{
			try
			{
				landmarkModel.load( file );
			}
			catch ( final IOException e1 )
			{
				e1.printStackTrace();
			}
		}
		else if( name.endsWith( "json" ))
		{
			TransformWriterJson.read( file, this );
		}

		final boolean didCompute = restimateTransformation();

		// didCompute = false means that there were not enough points
		// in the loaded points, so we should display the 'raw' moving
		// image
		if ( !didCompute )
			setIsMovingDisplayTransformed( false );

		autoEstimateMask();

		viewerP.requestRepaint();
		viewerQ.requestRepaint();
		landmarkFrame.repaint();
	}

	protected void saveSettings()
	{
		final File proposedSettingsFile = new File( "bigwarp.settings.xml" );
		saveSettingsOrProject( proposedSettingsFile );
	}

	protected void saveProject()
	{
		final File proposedSettingsFile = new File( "bigwarp-project.json" );
		saveSettingsOrProject( proposedSettingsFile );
	}

	protected void saveSettingsOrProject( final File proposedFile )
	{
		final JFileChooser fileChooser = new JFileChooser( getLastDirectory() );

		File settingsFile;
		fileChooser.setSelectedFile( proposedFile );
		final int returnVal = fileChooser.showSaveDialog( null );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
		{
			settingsFile = fileChooser.getSelectedFile();
			try
			{
				final String canonicalPath = settingsFile.getCanonicalPath();
				if ( canonicalPath.endsWith( ".xml" ) )
				{
					saveSettings( canonicalPath );
				}
				else
				{
					saveSettingsJson( canonicalPath );
				}
			}
			catch ( final IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	protected void saveSettings( final String xmlFilename ) throws IOException
	{
		final Element root = new Element( "Settings" );

		final Element viewerPNode = new Element( "viewerP" );
		final Element viewerQNode = new Element( "viewerQ" );

		root.addContent( viewerPNode );
		root.addContent( viewerQNode );

		viewerPNode.addContent( viewerP.stateToXml() );
		viewerQNode.addContent( viewerQ.stateToXml() );

		root.addContent( setupAssignments.toXml() );
		root.addContent( bookmarks.toXml() );

		final Element autoSaveNode = new Element( "autosave" );
		final Element autoSaveLocation = new Element( "location" );
		if ( autoSaver != null && autoSaver.autoSaveDirectory != null )
			autoSaveLocation.setText( autoSaver.autoSaveDirectory.getAbsolutePath() );
		else
			autoSaveLocation.setText( getBigwarpSettingsFolder().getAbsolutePath() );

		final Element autoSavePeriod = new Element( "period" );
		final String periodString = autoSaver == null ? "-1" : Long.toString(autoSaver.getPeriod());
		autoSavePeriod.setText( periodString );

		autoSaveNode.addContent( autoSaveLocation );
		autoSaveNode.addContent( autoSavePeriod );

		root.addContent( autoSaveNode );
		if ( transformMask != null )
		{
			// TODO check if is imported mask
			root.addContent( plateauTransformMask.getRandomAccessible().toXml() );
		}

		final Document doc = new Document( root );
		final XMLOutputter xout = new XMLOutputter( Format.getPrettyFormat() );
		xout.output( doc, new FileWriter( xmlFilename ) );
	}

	protected void saveSettingsJson( final String jsonFilename ) throws IOException
	{
		final BigwarpSettings settings = getSettings();
		settings.serialize( jsonFilename );
	}

	public BigwarpSettings getSettings()
	{
		return new BigwarpSettings(
				this,
				viewerP,
				viewerQ,
				setupAssignments,
				bookmarks,
				autoSaver,
				landmarkModel,
				bwTransform,
				data.sourceInfos
		);
	}

	protected void loadSettings()
	{
		loadSettingsOrProject( new File( "bigwarp.settings.xml" ) );
	}

	protected void loadProject()
	{
		loadSettingsOrProject( new File( "bigwarp-project.json" ) );
	}

	protected void loadSettingsOrProject( final File f )
	{
		final JFileChooser fileChooser = new JFileChooser( getLastDirectory() );

		File settingsFile;
		fileChooser.setSelectedFile( f );
		final int returnVal = fileChooser.showOpenDialog( null );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
		{
			settingsFile = fileChooser.getSelectedFile();

			try {
				loadSettings(settingsFile.getCanonicalPath(), true);
			} catch (IOException | JDOMException e) {
				e.printStackTrace();
			}

//			try {
//				SwingUtilities.invokeAndWait( () -> {
//					try {
//						loadSettings(settingsFile.getCanonicalPath(), true);
//					} catch (final IOException e) {
//						e.printStackTrace();
//					} catch (final JDOMException e) {
//						e.printStackTrace();
//					}
//				});
//			} catch (final Exception e) {
//				e.printStackTrace();
//			}

			// TODO I may need this
//			Executors.newSingleThreadExecutor().execute(new Runnable() {
//				@Override
//				public void run() {
//					try {
//						loadSettings(settingsFile.getCanonicalPath(), true);
//					} catch (final Exception e) {
//						e.printStackTrace();
//					}
//				}
//			});
		}
	}

	private void addInternalSource(int id) {
		final boolean sourceWithIdPresent = data.sources.stream().map( it -> it.getConverter()).filter( it -> it instanceof ConverterSetup ).filter( it -> ( ( ConverterSetup ) it ).getSetupId() == id ).findAny().isPresent();
		if (sourceWithIdPresent) {
			return;
		}
		switch ( id )
		{
		case GRID_SOURCE_ID:
			gridSource = addGridSource( ndims, data, "GridSource", isMovingDisplayTransformed());
			setGridType( GridSource.GRID_TYPE.LINE );
			break;
		case WARPMAG_SOURCE_ID:
			warpMagSource = addWarpMagnitudeSource( data, ndims == 2, "Warp magnitude" );
			break;
		case JACDET_SOURCE_ID:
			jacDetSource = addJacobianDeterminantSource( ndims, data, "Jacobian determinant" );
			break;
		case TRANSFORM_MASK_SOURCE_ID:
			updateTransformMask();
			break;
		default:
			break;
		}
	}

	public void loadSettings( final String jsonOrXmlFilename ) throws IOException,
			JDOMException
	{
		loadSettings( jsonOrXmlFilename, false );
	}

	public void loadSettings( final String jsonOrXmlFilename, boolean overwriteSources ) throws IOException,
			JDOMException
	{
		final String filenameTrim = jsonOrXmlFilename.trim();
		String name = filenameTrim;
		try {
			final URI uri = new URI( filenameTrim );
			if( uri != null && uri.getScheme() != null )
				name = uri.getSchemeSpecificPart();
		}
		catch ( URISyntaxException e ) { }

		if ( name.endsWith( ".xml" ) )
		{
			final SAXBuilder sax = new SAXBuilder();
			final Document doc = sax.build( name );
			final Element root = doc.getRootElement();

			/* add default sources if present */
			final List< Element > converterSetups = root.getChild( "SetupAssignments" ).getChild( "ConverterSetups" ).getChildren( "ConverterSetup" );
			for ( final Element converterSetup : converterSetups )
			{
				final int id = Integer.parseInt( converterSetup.getChild( "id" ).getText() );
				addInternalSource( id );
			}
			synchronizeSources();

			viewerP.stateFromXml( root.getChild( "viewerP" ) );
			viewerQ.stateFromXml( root.getChild( "viewerQ" ) );
			setupAssignments.restoreFromXml( root );
			bookmarks.restoreFromXml( root );
			activeSourcesDialogP.update();
			activeSourcesDialogQ.update();

			// auto-save settings
			final Element autoSaveElem = root.getChild( "autosave" );
			final String autoSavePath = autoSaveElem.getChild( "location" ).getText();
			final long autoSavePeriod = Integer.parseInt( autoSaveElem.getChild( "period" ).getText() );
			BigWarpAutoSaver.setAutosaveOptions( this, autoSavePeriod, autoSavePath );

			// TODO check if is imported mask
			final Element maskSettings = root.getChild( "transform-mask" );
			if ( maskSettings != null )
				plateauTransformMask.getRandomAccessible().fromXml( maskSettings );
		}
		else
		{
			final BigwarpSettings settings = getSettings();
			settings.setOverwriteSources( overwriteSources );
			settings.read( new JsonReader( new FileReader( name ) ) );

			// TODO I may need this
//			Executors.newSingleThreadExecutor().execute(new Runnable() {
//				@Override
//				public void run() {
//					try {
//						settings.read( new JsonReader( new FileReader( jsonOrXmlFilename ) ) );
//					} catch (final Exception e) {
//						e.printStackTrace();
//					}
//				}
//			});

			// TODO when source transformation panel is ready
//			warpVisDialog.transformGraphPanel.initializeSourceCoordinateSystems();

			activeSourcesDialogP.update();
			activeSourcesDialogQ.update();
		}

		viewerFrameP.repaint();
		viewerFrameQ.repaint();
	}
}
