/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2025 Howard Hughes Medical Institute.
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
package bdv.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.formdev.flatlaf.util.UIScale;

import bdv.ij.ApplyBigwarpPlugin;
import bdv.ij.ApplyBigwarpTransformPlugIn;
import bdv.ij.ApplyBigwarpTransformPlugIn.ApplyTransformParameters;
import bdv.ij.BigWarpToDeformationFieldPlugIn;
import bdv.viewer.Source;
import bigwarp.BigWarp;
import bigwarp.BigWarpData;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import ij.IJ;
import ij.Macro;
import ij.WindowManager;
import ij.plugin.frame.Recorder;
import net.imglib2.realtransform.BoundingBoxEstimation;

/**
 * Dialog for applying a BigWarp transformation to images.
 * <p>
 * The interactive counterpart to the {@code GenericDialogPlus} in
 * {@link ApplyBigwarpPlugin}: rather than asking for a field-of-view option plus nine
 * numeric fields whose units and relevance depend on that option, a
 * {@link FieldOfViewPanel} resolves the chosen reference to concrete min / size / spacing /
 * pixel-count values that are shown and can be edited directly.
 *
 * @author John Bogovic
 */
public class ApplyTransformFrame extends JFrame
{
	private static final long serialVersionUID = -3624074709254333025L;

	public static final String commandName = "Big Warp Apply Transform";

	// macro recording keys
	protected static final String landmarksKey = "landmarks";
	protected static final String movingImageKey = "moving_image";
	protected static final String movingRootKey = "moving_root";
	protected static final String movingDatasetKey = "moving_dataset";
	protected static final String targetImageKey = "target_image";
	protected static final String targetRootKey = "target_root";
	protected static final String targetDatasetKey = "target_dataset";
	protected static final String transformTypeKey = "transform_type";

	protected static final String sizeKey = "pixel_size";
	protected static final String spacingKey = "pixel_spacing";
	protected static final String minKey = "min";
	protected static final String unitKey = "unit";

	protected static final String fovReferenceKey = "fov_reference";
	protected static final String pointFilterKey = "point_filter";
	protected static final String bboxMethodKey = "bbox_method";
	protected static final String bboxSamplesKey = "bbox_samples";

	protected static final String interpolationKey = "interpolation";
	protected static final String virtualKey = "virtual";
	protected static final String threadsKey = "threads";

	/*
	 * Macro.getValue finds "<key>=" anywhere in the option string, so a key that is a
	 * suffix of another key silently reads the wrong value - "dataset" would match inside
	 * "moving_dataset". Keep every key distinct under suffix comparison.
	 */
	protected static final String destinationKey = "destination";
	protected static final String pathKey = "output_path";
	protected static final String datasetKey = "output_dataset";
	protected static final String blockSizeKey = "output_block_size";
	protected static final String compressionKey = "output_compression";

	private static final String LIST_SEPARATOR = ",";

	private String lastBrowsePath = null;
	private String initialPath = null;

	private Consumer<ApplyTransformParameters> okayCallback;
	private Consumer<ApplyTransformParameters> cancelCallback;

	private BigWarpData<?> data;
	private BigWarpTransform bwTransform;
	private LandmarkTableModel ltm;

	/** True when the dialog owns the inputs, i.e. it was not opened from a BigWarp instance. */
	private final boolean standalone;

	/**
	 * Set once every widget exists. Setting a dropdown's initial selection fires its
	 * listener, so the input listeners can run before the output widgets are built.
	 */
	private boolean contentCreated;

	private boolean imageJOpen;
	private boolean initialRecorderState;

	private JPanel contentPanel;

	// input widgets (standalone only)
	private JTextField landmarkPathTxt;
	private JComboBox<String> movingImageDropdown;
	private JTextField movingRootTxt;
	private JTextField movingDatasetTxt;
	private JComboBox<String> targetImageDropdown;
	private JTextField targetRootTxt;
	private JTextField targetDatasetTxt;
	private JComboBox<String> transformTypeDropdown;

	// field of view
	private JPanel fovContainer;
	private FieldOfViewPanel fovPanel;
	/** Whether the current fovPanel was built against a BigWarpData. */
	private boolean fovPanelHasData;
	/** Dimensionality the current fovPanel was built for. */
	private int fovPanelNdims;

	// output widgets
	private JComboBox<String> interpolationDropdown;
	private JCheckBox virtualCheckBox;
	private JSpinner nThreadsSpinner;
	private JComboBox<String> destinationDropdown;

	// writing widgets
	private JPanel writePanel;
	private JLabel pathLabel;
	private JTextField pathTxt;
	private JButton browsePathButton;
	private JLabel datasetLabel;
	private JTextField datasetTxt;
	private JLabel blockSizeLabel;
	private JTextField blockSizeTxt;
	private JLabel compressionLabel;
	private JComboBox<String> compressionDropdown;

	private JButton okBtn;
	private JButton cancelBtn;

	public ApplyTransformFrame( final BigWarp<?> bw )
	{
		this( bw.getData(), bw.getBwTransform(), bw.getLandmarkPanel().getTableModel() );
	}

	public ApplyTransformFrame( final BigWarpData<?> data, final BigWarpTransform bwTransform, final LandmarkTableModel ltm )
	{
		super( "Apply transformation" );
		initialPath = "";
		imageJOpen = IJ.getInstance() != null;

		this.data = data;
		this.bwTransform = bwTransform;
		this.ltm = ltm;
		this.standalone = ( bwTransform == null );

		cancelCallback = x -> {
			Recorder.record = initialRecorderState;
			dispose();
		};

		okayCallback = params -> {
			macroRecord( params );
			run( params );
			Recorder.record = initialRecorderState;
			dispose();
		};
	}

	/**
	 * Restores this state when the dialog closes.
	 * <p>
	 * Callers that disabled the macro recorder while building the dialog should pass the
	 * value they saw, so that accepting or cancelling does not silently turn recording off.
	 *
	 * @param state the recorder state to restore
	 */
	public void setInitialRecorderState( final boolean state )
	{
		this.initialRecorderState = state;
	}

	public static void createAndShow()
	{
		createAndShow( null, null, null );
	}

	public static void createAndShow( final BigWarp<?> bw )
	{
		if ( bw == null )
			createAndShow( null, null, null );
		else
			createAndShow( bw.getData(), bw.getBwTransform(), bw.getLandmarkPanel().getTableModel() );
	}

	public static void createAndShow( final BigWarpData<?> data, final BigWarpTransform bwTransform,
			final LandmarkTableModel ltm )
	{
		final ApplyTransformFrame frame = new ApplyTransformFrame( data, bwTransform, ltm );
		frame.createContent();
		// only dispose this dialog - EXIT_ON_CLOSE would take Fiji's jvm down with it
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.pack();
		frame.setVisible( true );
	}

	public void createContent()
	{
		final int frameSizeX = UIScale.scale( 600 );

		contentPanel = new JPanel();
		contentPanel.setLayout( new GridBagLayout() );

		final GridBagConstraints cGbc = new GridBagConstraints();
		cGbc.gridx = 0;
		cGbc.gridwidth = 4;
		cGbc.fill = GridBagConstraints.HORIZONTAL;

		int row = 0;
		if ( standalone )
		{
			final JPanel inputPanel = titled( inputPanel( frameSizeX ), "Input" );
			cGbc.gridy = row++;
			contentPanel.add( inputPanel, cGbc );
		}

		fovContainer = new JPanel( new BorderLayout() );
		titled( fovContainer, "Field of view" );
		cGbc.gridy = row++;
		contentPanel.add( fovContainer, cGbc );

		cGbc.gridy = row++;
		contentPanel.add( titled( outputOptionsPanel( frameSizeX ), "Output options" ), cGbc );

		writePanel = titled( writingPanel( frameSizeX ), "Writing options" );
		cGbc.gridy = row++;
		contentPanel.add( writePanel, cGbc );

		// bottom button section
		final GridBagConstraints cbot = new GridBagConstraints();
		cbot.gridy = row;
		cbot.gridwidth = 1;
		cbot.gridheight = 1;
		cbot.weightx = 1.0;
		cbot.weighty = 0.0;
		cbot.fill = GridBagConstraints.NONE;

		okBtn = new JButton( "OK" );
		okBtn.addActionListener( e -> okayCallback.accept( getParams() ) );
		cbot.gridx = 2;
		cbot.insets = new Insets( 20, ( int ) ( frameSizeX * 0.8 ), 20, 2 );
		cbot.anchor = GridBagConstraints.LINE_END;
		contentPanel.add( okBtn, cbot );

		cancelBtn = new JButton( "Cancel" );
		cancelBtn.addActionListener( e -> cancelCallback.accept( null ) );
		cbot.gridx = 3;
		cbot.anchor = GridBagConstraints.LINE_START;
		cbot.ipadx = 0;
		cbot.insets = new Insets( 2, 2, 2, 2 );
		contentPanel.add( cancelBtn, cbot );

		final Container content = getContentPane();
		content.add( contentPanel );

		contentCreated = true;

		buildFovPanel();
		if ( standalone )
			refreshInputs();
		else
			fovPanel.updateFieldsFromReference();

		updateDestinationVisibility();
	}

	/**
	 * Applies this codebase's standard titled border to a panel.
	 *
	 * @param panel the panel
	 * @param title the title
	 * @return the same panel
	 */
	private static JPanel titled( final JPanel panel, final String title )
	{
		panel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								title ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );
		return panel;
	}

	private JPanel inputPanel( final int frameSizeX )
	{
		final int OUTER_PAD = BigWarpInitDialog.DEFAULT_OUTER_PAD;
		final int BUTTON_PAD = BigWarpInitDialog.DEFAULT_BUTTON_PAD;
		final int MID_PAD = BigWarpInitDialog.DEFAULT_MID_PAD;
		final Insets defaultInsets = new Insets( OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD );

		final JPanel panel = new JPanel();
		panel.setLayout( new GridBagLayout() );

		final GridBagConstraints ctxt = new GridBagConstraints();
		ctxt.gridx = 0;
		ctxt.gridy = 0;
		ctxt.gridwidth = 1;
		ctxt.weightx = 0.0;
		ctxt.anchor = GridBagConstraints.LINE_END;
		ctxt.fill = GridBagConstraints.NONE;
		ctxt.insets = defaultInsets;

		final GridBagConstraints gbcBar = new GridBagConstraints();
		gbcBar.gridx = 1;
		gbcBar.gridy = 0;
		gbcBar.gridwidth = 4;
		gbcBar.weightx = 1.0;
		gbcBar.fill = GridBagConstraints.HORIZONTAL;
		gbcBar.insets = defaultInsets;

		final GridBagConstraints cBrowse = new GridBagConstraints();
		cBrowse.gridx = 5;
		cBrowse.gridy = 0;
		cBrowse.gridwidth = 1;
		cBrowse.weightx = 0.0;
		cBrowse.fill = GridBagConstraints.HORIZONTAL;
		cBrowse.insets = defaultInsets;

		// landmarks
		panel.add( new JLabel( "Landmarks:" ), ctxt );
		landmarkPathTxt = new JTextField();
		landmarkPathTxt.setPreferredSize( new Dimension( frameSizeX / 3, landmarkPathTxt.getPreferredSize().height ) );
		landmarkPathTxt.addFocusListener( refreshOnFocusLost() );
		panel.add( landmarkPathTxt, gbcBar );

		final JButton browseLandmarksButton = new JButton( "Browse" );
		browseLandmarksButton.addActionListener( e -> {
			final String s = browseDialogGeneral( JFileChooser.FILES_ONLY, new FileNameExtensionFilter( "csv file", "csv" ) );
			if ( s != null )
			{
				landmarkPathTxt.setText( s );
				refreshInputs();
			}
		} );
		panel.add( browseLandmarksButton, cBrowse );

		final String[] titles = imagePlusTitles();

		// moving image
		ctxt.gridy = 1;
		panel.add( new JLabel( "Moving image:" ), ctxt );
		movingImageDropdown = new JComboBox<>( titles );
		movingImageDropdown.addActionListener( e -> refreshInputs() );
		final GridBagConstraints gbcCombo = new GridBagConstraints();
		gbcCombo.gridx = 1;
		gbcCombo.gridy = 1;
		gbcCombo.gridwidth = 2;
		gbcCombo.weightx = 1.0;
		gbcCombo.fill = GridBagConstraints.HORIZONTAL;
		gbcCombo.insets = defaultInsets;
		panel.add( movingImageDropdown, gbcCombo );

		// target image
		ctxt.gridx = 3;
		panel.add( new JLabel( "Target image:" ), ctxt );
		targetImageDropdown = new JComboBox<>( titles );
		targetImageDropdown.addActionListener( e -> refreshInputs() );
		gbcCombo.gridx = 4;
		gbcCombo.gridwidth = 2;
		panel.add( targetImageDropdown, gbcCombo );
		ctxt.gridx = 0;

		// default the moving image to the current window, target to something else
		if ( titles.length > 1 )
		{
			final String current = currentImageTitle( titles );
			movingImageDropdown.setSelectedItem( current );
			targetImageDropdown.setSelectedItem( current.equals( titles[ 0 ] ) ? titles[ 1 ] : titles[ 0 ] );
		}

		// n5 sources
		ctxt.gridy = 2;
		panel.add( new JLabel( "or moving n5:" ), ctxt );
		gbcBar.gridy = 2;
		gbcBar.gridwidth = 2;
		movingRootTxt = new JTextField();
		movingRootTxt.setToolTipText( N5_SOURCE_HELP_TEXT );
		movingRootTxt.addFocusListener( refreshOnFocusLost() );
		panel.add( movingRootTxt, gbcBar );

		gbcBar.gridx = 3;
		movingDatasetTxt = new JTextField();
		movingDatasetTxt.setToolTipText( N5_DATASET_HELP_TEXT );
		movingDatasetTxt.addFocusListener( refreshOnFocusLost() );
		panel.add( movingDatasetTxt, gbcBar );

		cBrowse.gridy = 2;
		final JButton browseMovingButton = new JButton( "Browse" );
		browseMovingButton.addActionListener( e -> {
			final String s = browseDialogGeneral( JFileChooser.FILES_AND_DIRECTORIES, null );
			if ( s != null )
			{
				movingRootTxt.setText( s );
				refreshInputs();
			}
		} );
		panel.add( browseMovingButton, cBrowse );

		ctxt.gridy = 3;
		panel.add( new JLabel( "or target n5:" ), ctxt );
		gbcBar.gridx = 1;
		gbcBar.gridy = 3;
		targetRootTxt = new JTextField();
		targetRootTxt.setToolTipText( N5_SOURCE_HELP_TEXT );
		targetRootTxt.addFocusListener( refreshOnFocusLost() );
		panel.add( targetRootTxt, gbcBar );

		gbcBar.gridx = 3;
		targetDatasetTxt = new JTextField();
		targetDatasetTxt.setToolTipText( N5_DATASET_HELP_TEXT );
		targetDatasetTxt.addFocusListener( refreshOnFocusLost() );
		panel.add( targetDatasetTxt, gbcBar );

		cBrowse.gridy = 3;
		final JButton browseTargetButton = new JButton( "Browse" );
		browseTargetButton.addActionListener( e -> {
			final String s = browseDialogGeneral( JFileChooser.FILES_AND_DIRECTORIES, null );
			if ( s != null )
			{
				targetRootTxt.setText( s );
				refreshInputs();
			}
		} );
		panel.add( browseTargetButton, cBrowse );

		// transform type
		ctxt.gridy = 4;
		panel.add( new JLabel( "Transform type:" ), ctxt );
		transformTypeDropdown = new JComboBox<>( new String[] {
				BigWarpTransform.TPS,
				BigWarpTransform.AFFINE,
				BigWarpTransform.SIMILARITY,
				BigWarpTransform.ROTATION,
				BigWarpTransform.TRANSLATION } );
		transformTypeDropdown.setSelectedItem( BigWarpTransform.TPS );
		transformTypeDropdown.addActionListener( e -> refreshInputs() );
		gbcCombo.gridx = 1;
		gbcCombo.gridy = 4;
		gbcCombo.gridwidth = 2;
		panel.add( transformTypeDropdown, gbcCombo );

		return panel;
	}

	private JPanel outputOptionsPanel( final int frameSizeX )
	{
		final int OUTER_PAD = BigWarpInitDialog.DEFAULT_OUTER_PAD;
		final int BUTTON_PAD = BigWarpInitDialog.DEFAULT_BUTTON_PAD;
		final int MID_PAD = BigWarpInitDialog.DEFAULT_MID_PAD;
		final Insets defaultInsets = new Insets( OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD );
		final Insets leftInsets = new Insets( OUTER_PAD, 10 * BUTTON_PAD, MID_PAD, BUTTON_PAD );

		final JPanel panel = new JPanel();
		panel.setLayout( new GridBagLayout() );

		final GridBagConstraints ctxt = new GridBagConstraints();
		ctxt.gridx = 0;
		ctxt.gridy = 0;
		ctxt.weightx = 0.0;
		ctxt.anchor = GridBagConstraints.LINE_END;
		ctxt.fill = GridBagConstraints.NONE;
		ctxt.insets = defaultInsets;

		final GridBagConstraints gbcCtrl = new GridBagConstraints();
		gbcCtrl.gridx = 1;
		gbcCtrl.gridy = 0;
		gbcCtrl.weightx = 0.0;
		gbcCtrl.anchor = GridBagConstraints.LINE_START;
		gbcCtrl.fill = GridBagConstraints.HORIZONTAL;
		gbcCtrl.insets = defaultInsets;

		panel.add( new JLabel( "Interpolation:" ), ctxt );
		interpolationDropdown = new JComboBox<>( ApplyBigwarpTransformPlugIn.interpolationOptions );
		interpolationDropdown.setSelectedItem( ApplyBigwarpTransformPlugIn.INTERP_LINEAR );
		panel.add( interpolationDropdown, gbcCtrl );

		ctxt.gridx = 2;
		ctxt.insets = leftInsets;
		panel.add( new JLabel( "Threads:" ), ctxt );
		ctxt.insets = defaultInsets;
		gbcCtrl.gridx = 3;
		final int defaultThreads = Math.max( 1, ( int ) Math.ceil( Runtime.getRuntime().availableProcessors() / 4.0 ) );
		nThreadsSpinner = new JSpinner( new SpinnerNumberModel( defaultThreads, 1, 9999, 1 ) );
		panel.add( nThreadsSpinner, gbcCtrl );

		ctxt.gridx = 4;
		ctxt.insets = leftInsets;
		final JLabel virtualLabel = new JLabel( "Virtual:" );
		virtualLabel.setToolTipText( VIRTUAL_HELP_TEXT );
		panel.add( virtualLabel, ctxt );
		ctxt.insets = defaultInsets;
		gbcCtrl.gridx = 5;
		virtualCheckBox = new JCheckBox();
		virtualCheckBox.setToolTipText( VIRTUAL_HELP_TEXT );
		panel.add( virtualCheckBox, gbcCtrl );

		ctxt.gridx = 0;
		ctxt.gridy = 1;
		panel.add( new JLabel( "Write to:" ), ctxt );
		gbcCtrl.gridx = 1;
		gbcCtrl.gridy = 1;
		gbcCtrl.gridwidth = 2;
		destinationDropdown = new JComboBox<>( ApplyBigwarpTransformPlugIn.destinationOptions );
		destinationDropdown.setSelectedItem( ApplyBigwarpTransformPlugIn.DEST_IMAGEJ );
		destinationDropdown.addActionListener( e -> updateDestinationVisibility() );
		panel.add( destinationDropdown, gbcCtrl );

		return panel;
	}

	private JPanel writingPanel( final int frameSizeX )
	{
		final int OUTER_PAD = BigWarpInitDialog.DEFAULT_OUTER_PAD;
		final int BUTTON_PAD = BigWarpInitDialog.DEFAULT_BUTTON_PAD;
		final int MID_PAD = BigWarpInitDialog.DEFAULT_MID_PAD;

		final JPanel panel = new JPanel();
		panel.setLayout( new GridBagLayout() );

		final GridBagConstraints ctxt = new GridBagConstraints();
		ctxt.gridx = 0;
		ctxt.gridy = 0;
		ctxt.weightx = 0.0;
		ctxt.anchor = GridBagConstraints.LINE_END;
		ctxt.fill = GridBagConstraints.NONE;
		ctxt.insets = new Insets( OUTER_PAD, OUTER_PAD, MID_PAD, BUTTON_PAD );

		final GridBagConstraints gbcBar = new GridBagConstraints();
		gbcBar.gridx = 1;
		gbcBar.gridy = 0;
		gbcBar.gridwidth = 6;
		gbcBar.weightx = 1.0;
		gbcBar.fill = GridBagConstraints.HORIZONTAL;
		gbcBar.insets = new Insets( OUTER_PAD, OUTER_PAD, MID_PAD, BUTTON_PAD );

		pathLabel = new JLabel( "Root folder:" );
		panel.add( pathLabel, ctxt );

		pathTxt = new JTextField();
		pathTxt.setPreferredSize( new Dimension( frameSizeX / 3, pathTxt.getPreferredSize().height ) );
		panel.add( pathTxt, gbcBar );

		final GridBagConstraints cBrowse = new GridBagConstraints();
		cBrowse.gridx = 7;
		cBrowse.gridy = 0;
		cBrowse.gridwidth = 1;
		cBrowse.weightx = 0.0;
		cBrowse.fill = GridBagConstraints.HORIZONTAL;
		cBrowse.insets = new Insets( OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD );
		browsePathButton = new JButton( "Browse" );
		browsePathButton.addActionListener( e -> {
			final String s = browseDialogGeneral( JFileChooser.FILES_AND_DIRECTORIES, null );
			if ( s != null )
				pathTxt.setText( s );
		} );
		panel.add( browsePathButton, cBrowse );

		ctxt.gridy = 1;
		datasetLabel = new JLabel( "Dataset:" );
		panel.add( datasetLabel, ctxt );
		gbcBar.gridy = 1;
		datasetTxt = new JTextField();
		datasetTxt.setPreferredSize( new Dimension( frameSizeX / 3, datasetTxt.getPreferredSize().height ) );
		panel.add( datasetTxt, gbcBar );

		ctxt.gridy = 2;
		blockSizeLabel = new JLabel( "Block size:" );
		panel.add( blockSizeLabel, ctxt );
		gbcBar.gridy = 2;
		blockSizeTxt = new JTextField();
		blockSizeTxt.setPreferredSize( new Dimension( frameSizeX / 3, blockSizeTxt.getPreferredSize().height ) );
		blockSizeTxt.setText( "64" );
		panel.add( blockSizeTxt, gbcBar );

		ctxt.gridy = 3;
		compressionLabel = new JLabel( "Compression:" );
		panel.add( compressionLabel, ctxt );
		gbcBar.gridy = 3;
		gbcBar.fill = GridBagConstraints.NONE;
		gbcBar.anchor = GridBagConstraints.LINE_START;
		compressionDropdown = new JComboBox<>( BigWarpToDeformationFieldPlugIn.compressionOptions );
		panel.add( compressionDropdown, gbcBar );

		return panel;
	}

	/**
	 * Shows only the writing controls that the chosen destination actually uses.
	 */
	private void updateDestinationVisibility()
	{
		final String dest = ( String ) destinationDropdown.getSelectedItem();
		final boolean toN5 = ApplyBigwarpTransformPlugIn.DEST_N5.equals( dest );
		final boolean toFile = ApplyBigwarpTransformPlugIn.DEST_FILE.equals( dest );

		pathLabel.setText( toN5 ? "Root folder:" : "File:" );

		datasetLabel.setVisible( toN5 );
		datasetTxt.setVisible( toN5 );
		blockSizeLabel.setVisible( toN5 );
		blockSizeTxt.setVisible( toN5 );
		compressionLabel.setVisible( toN5 );
		compressionDropdown.setVisible( toN5 );

		SwingUtilities.invokeLater( () -> {
			writePanel.setVisible( toN5 || toFile );
			contentPanel.revalidate();
			contentPanel.repaint();
			pack();
		} );
	}

	private FocusAdapter refreshOnFocusLost()
	{
		return new FocusAdapter() {

			@Override
			public void focusLost( final FocusEvent e )
			{
				refreshInputs();
			}
		};
	}

	/**
	 * Rebuilds the data, landmarks and transformation from the input widgets, then brings
	 * the field of view panel in line with them.
	 * <p>
	 * Called on the discrete input events - a browse, a dropdown change, a text field
	 * losing focus - rather than on every keystroke, since it opens images.
	 */
	private void refreshInputs()
	{
		if ( !standalone || !contentCreated )
			return;

		final ApplyTransformParameters params = getParams();

		BigWarpData<?> newData = null;
		LandmarkTableModel newLtm = null;
		BigWarpTransform newTransform = null;
		try
		{
			newData = ApplyBigwarpTransformPlugIn.buildDataQuietly( params );
			if ( newData != null )
			{
				final int nd = BigWarp.detectNumDims( newData.sources );
				newLtm = ApplyBigwarpTransformPlugIn.loadLandmarksQuietly( params, nd );
				if ( newLtm != null )
					newTransform = new BigWarpTransform( newLtm, params.transformType );
			}
		}
		catch ( final Exception e )
		{
			// an incomplete or bad selection is expected while the user is still typing
			newData = null;
			newLtm = null;
			newTransform = null;
		}

		this.data = newData;
		this.ltm = newLtm;
		this.bwTransform = newTransform;

		final int nd = newLtm != null ? newLtm.getNumdims() : 3;
		if ( fovPanel == null || fovPanelHasData != ( newData != null && newTransform != null ) || fovPanelNdims != nd )
			buildFovPanel();
		else
		{
			fovPanel.setData( newData );
			fovPanel.setLandmarks( newLtm );
			fovPanel.setTransform( newTransform );
			fovPanel.updateFieldsFromReference();
		}
	}

	/**
	 * (Re)builds the field of view panel.
	 * <p>
	 * The panel decides which reference options to offer when it is constructed - open
	 * ImagePlus windows when it has no {@link BigWarpData}, the target / warped moving /
	 * landmark options when it does - and its row count depends on the dimensionality, so
	 * it has to be replaced rather than updated when either of those changes.
	 */
	private void buildFovPanel()
	{
		final boolean hasData = data != null && bwTransform != null;
		final int nd = ltm != null ? ltm.getNumdims() : 3;

		String unit = "pixel";
		if ( hasData && data.numMovingSources() > 0 )
		{
			final Source<?> src = data.getMovingSource( 0 ).getSpimSource();
			if ( src.getVoxelDimensions() != null )
				unit = src.getVoxelDimensions().unit();
		}

		// carry the current values over so a rebuild is not a reset
		final double[] min = fovPanel == null ? new double[] { 0, 0, 0 } : padTo( fovPanel.getMin(), nd, 0 );
		final double[] spacing = fovPanel == null ? new double[] { 1, 1, 1 } : padTo( fovPanel.getSpacing(), nd, 1 );
		final long[] pixSize = fovPanel == null ? new long[] { 256, 256, 128 } : padTo( fovPanel.getPixelSize(), nd, 1 );

		final FieldOfViewPanel newPanel = new FieldOfViewPanel( hasData ? data : null, ltm, bwTransform, unit, 150,
				min, spacing, pixSize, true );

		if ( fovPanel != null )
			fovContainer.remove( fovPanel );

		fovPanel = newPanel;
		fovPanelHasData = hasData;
		fovPanelNdims = nd;
		fovContainer.add( fovPanel, BorderLayout.CENTER );

		if ( hasData )
			fovPanel.updateFieldsFromReference();
		else if ( imageJOpen )
			fovPanel.updateFieldsFromImageJReference();

		SwingUtilities.invokeLater( () -> {
			contentPanel.revalidate();
			contentPanel.repaint();
			pack();
		} );
	}

	private static double[] padTo( final double[] x, final int n, final double fill )
	{
		final double[] out = new double[ n ];
		Arrays.fill( out, fill );
		System.arraycopy( x, 0, out, 0, Math.min( n, x.length ) );
		return out;
	}

	private static long[] padTo( final long[] x, final int n, final long fill )
	{
		final long[] out = new long[ n ];
		Arrays.fill( out, fill );
		System.arraycopy( x, 0, out, 0, Math.min( n, x.length ) );
		return out;
	}

	private String[] imagePlusTitles()
	{
		final String[] titles = IJ.getInstance() == null ? new String[ 0 ] : WindowManager.getImageTitles();
		final String[] out = new String[ titles.length + 1 ];
		System.arraycopy( titles, 0, out, 0, titles.length );
		out[ titles.length ] = ApplyBigwarpTransformPlugIn.NO_IMAGE;
		return out;
	}

	private String currentImageTitle( final String[] titles )
	{
		if ( IJ.getInstance() != null && WindowManager.getCurrentImage() != null )
			return WindowManager.getCurrentImage().getTitle();

		return titles[ titles.length - 1 ];
	}

	private String browseDialogGeneral( final int mode, final FileFilter filefilter )
	{
		final JFileChooser fileChooser = new JFileChooser();
		/*
		 * Need to allow files so h5 containers can be opened, and directories so that
		 * filesystem n5's and zarrs can be opened.
		 */
		fileChooser.setFileSelectionMode( mode );
		if ( filefilter != null )
			fileChooser.setFileFilter( filefilter );

		if ( lastBrowsePath != null && !lastBrowsePath.isEmpty() )
			fileChooser.setCurrentDirectory( new File( lastBrowsePath ) );
		else if ( initialPath != null && !initialPath.isEmpty() )
			fileChooser.setCurrentDirectory( new File( initialPath ) );
		else if ( imageJOpen )
		{
			File f = null;
			final String currDir = IJ.getDirectory( "current" );
			final String homeDir = IJ.getDirectory( "home" );
			if ( currDir != null )
				f = new File( currDir );
			else if ( homeDir != null )
				f = new File( homeDir );

			fileChooser.setCurrentDirectory( f );
		}

		final int ret = fileChooser.showOpenDialog( this );
		if ( ret != JFileChooser.APPROVE_OPTION )
			return null;

		final String path = fileChooser.getSelectedFile().getAbsolutePath();
		lastBrowsePath = path;
		return path;
	}

	public ApplyTransformParameters getParams()
	{
		return new ApplyTransformParameters(
				landmarkPathTxt == null ? "" : landmarkPathTxt.getText(),
				movingImageDropdown == null ? "" : ( String ) movingImageDropdown.getSelectedItem(),
				movingRootTxt == null ? "" : movingRootTxt.getText(),
				movingDatasetTxt == null ? "" : movingDatasetTxt.getText(),
				targetImageDropdown == null ? "" : ( String ) targetImageDropdown.getSelectedItem(),
				targetRootTxt == null ? "" : targetRootTxt.getText(),
				targetDatasetTxt == null ? "" : targetDatasetTxt.getText(),
				transformTypeDropdown == null ? BigWarpTransform.TPS : ( String ) transformTypeDropdown.getSelectedItem(),
				fovPanel == null ? null : fovPanel.getPixelSize(),
				fovPanel == null ? null : fovPanel.getSpacing(),
				fovPanel == null ? null : fovPanel.getMin(),
				fovPanel == null ? "pixel" : fovPanel.getUnit(),
				fovPanel == null ? ApplyBigwarpPlugin.SPECIFIED : fovPanel.getReference(),
				fovPanel == null ? "" : fovPanel.getPointFilter(),
				fovPanel == null ? "" : fovPanel.getBoundingBoxMethod(),
				fovPanel == null ? 5 : fovPanel.getBoundingBoxSamples(),
				( String ) interpolationDropdown.getSelectedItem(),
				virtualCheckBox.isSelected(),
				( Integer ) nThreadsSpinner.getValue(),
				( String ) destinationDropdown.getSelectedItem(),
				pathTxt.getText(),
				datasetTxt.getText(),
				blockSizeTxt.getText(),
				( String ) compressionDropdown.getSelectedItem() );
	}

	public void run( final ApplyTransformParameters params )
	{
		// snapshot of the data as the dialog resolved it, so the worker touches no widgets
		final BigWarpData<?> runData = data;
		final LandmarkTableModel runLtm = ltm;
		final BigWarpTransform runTransform = bwTransform;
		new Thread()
		{
			@Override
			public void run()
			{
				ApplyBigwarpTransformPlugIn.runFromParameters( params, runData, runLtm, runTransform );
			}
		}.start();
	}

	public String macroRecord( final ApplyTransformParameters params )
	{
		if ( !Recorder.record )
			return "";

		Recorder.setCommand( commandName );
		Recorder.resetCommandOptions();

		for ( final Map.Entry<String, String> e : macroOptionMap( params, standalone ).entrySet() )
		{
			if ( e.getValue() == null )
				Recorder.recordOption( e.getKey() );
			else
				Recorder.recordOption( e.getKey(), e.getValue() );
		}

		Recorder.saveCommand();
		return Recorder.getCommandOptions();
	}

	/**
	 * The options describing these parameters, in recording order.
	 * <p>
	 * Both {@link #macroRecord} and {@link #toMacroOptions} read this, so what gets written
	 * cannot drift out of step with what {@link #parseMacroOptions} reads back. A null
	 * value means a bare flag, which is how ImageJ records a checkbox.
	 *
	 * @param params the parameters
	 * @param includeInputs whether to record the input selections, which are meaningless
	 *            when the dialog was opened from a BigWarp instance
	 * @return the options, in order
	 */
	private static Map<String, String> macroOptionMap( final ApplyTransformParameters params, final boolean includeInputs )
	{
		final Map<String, String> opts = new LinkedHashMap<>();

		if ( includeInputs )
		{
			opts.put( landmarksKey, trimmed( params.landmarkPath ) );
			putIfSet( opts, movingImageKey, params.movingImage );
			putIfSet( opts, movingRootKey, params.movingRoot );
			putIfSet( opts, movingDatasetKey, params.movingDataset );
			putIfSet( opts, targetImageKey, params.targetImage );
			putIfSet( opts, targetRootKey, params.targetRoot );
			putIfSet( opts, targetDatasetKey, params.targetDataset );
			opts.put( transformTypeKey, params.transformType );
		}

		opts.put( sizeKey, join( params.pixSize ) );
		opts.put( spacingKey, join( params.spacing ) );
		opts.put( minKey, join( params.min ) );
		opts.put( unitKey, params.unit );

		opts.put( fovReferenceKey, params.fovReference );
		putIfSet( opts, pointFilterKey, params.pointFilter );
		opts.put( bboxMethodKey, params.bboxMethod );
		opts.put( bboxSamplesKey, Integer.toString( params.bboxSamples ) );

		opts.put( interpolationKey, params.interpolation );
		if ( params.virtual )
			opts.put( virtualKey, null );

		opts.put( threadsKey, Integer.toString( params.nThreads ) );
		opts.put( destinationKey, params.destination );

		if ( !ApplyBigwarpTransformPlugIn.DEST_IMAGEJ.equals( params.destination ) )
		{
			opts.put( pathKey, trimmed( params.path ) );
			if ( ApplyBigwarpTransformPlugIn.DEST_N5.equals( params.destination ) )
			{
				opts.put( datasetKey, trimmed( params.dataset ) );
				opts.put( blockSizeKey, trimmed( params.blockSizeString ) );
				opts.put( compressionKey, params.compressionString );
			}
		}

		return opts;
	}

	/**
	 * Renders these parameters as an ImageJ macro option string.
	 * <p>
	 * The same options {@link #macroRecord} hands the {@link Recorder}, but without needing
	 * one, so that a round trip through {@link #parseMacroOptions} can be tested.
	 *
	 * @param params the parameters
	 * @param includeInputs whether to include the input selections
	 * @return the option string
	 */
	public static String toMacroOptions( final ApplyTransformParameters params, final boolean includeInputs )
	{
		final StringBuilder sb = new StringBuilder();
		for ( final Map.Entry<String, String> e : macroOptionMap( params, includeInputs ).entrySet() )
		{
			sb.append( e.getKey() );
			if ( e.getValue() != null )
			{
				sb.append( '=' );
				sb.append( quote( e.getValue() ) );
			}
			sb.append( ' ' );
		}
		return sb.toString();
	}

	/**
	 * ImageJ delimits a value containing spaces with brackets. An empty value needs them
	 * too, so that it does not read back as a bare flag.
	 *
	 * @param value the value
	 * @return the value as it appears in an option string
	 */
	private static String quote( final String value )
	{
		return value.isEmpty() || value.indexOf( ' ' ) >= 0 ? "[" + value + "]" : value;
	}

	private static String trimmed( final String s )
	{
		return s == null ? "" : s.trim();
	}

	private static void putIfSet( final Map<String, String> opts, final String key, final String value )
	{
		if ( value != null && !value.isEmpty() && !value.equals( ApplyBigwarpTransformPlugIn.NO_IMAGE ) )
			opts.put( key, value );
	}

	/**
	 * Reads back what {@link #toMacroOptions} wrote.
	 *
	 * @param args the macro options
	 * @return the parameters
	 */
	public static ApplyTransformParameters parseMacroOptions( final String args )
	{
		return new ApplyTransformParameters(
				Macro.getValue( args, landmarksKey, "" ),
				Macro.getValue( args, movingImageKey, "" ),
				Macro.getValue( args, movingRootKey, "" ),
				Macro.getValue( args, movingDatasetKey, "" ),
				Macro.getValue( args, targetImageKey, "" ),
				Macro.getValue( args, targetRootKey, "" ),
				Macro.getValue( args, targetDatasetKey, "" ),
				Macro.getValue( args, transformTypeKey, BigWarpTransform.TPS ),
				parseLongs( Macro.getValue( args, sizeKey, "" ) ),
				parseDoubles( Macro.getValue( args, spacingKey, "" ) ),
				parseDoubles( Macro.getValue( args, minKey, "" ) ),
				Macro.getValue( args, unitKey, "pixel" ),
				Macro.getValue( args, fovReferenceKey, ApplyBigwarpPlugin.SPECIFIED ),
				Macro.getValue( args, pointFilterKey, "" ),
				Macro.getValue( args, bboxMethodKey, BoundingBoxEstimation.Method.FACES.toString() ),
				Integer.parseInt( Macro.getValue( args, bboxSamplesKey, "5" ) ),
				Macro.getValue( args, interpolationKey, ApplyBigwarpTransformPlugIn.INTERP_LINEAR ),
				macroFlag( args, virtualKey ),
				Integer.parseInt( Macro.getValue( args, threadsKey, "1" ) ),
				Macro.getValue( args, destinationKey, ApplyBigwarpTransformPlugIn.DEST_IMAGEJ ),
				Macro.getValue( args, pathKey, "" ),
				Macro.getValue( args, datasetKey, "" ),
				Macro.getValue( args, blockSizeKey, "" ),
				Macro.getValue( args, compressionKey, "" ) );
	}

	public static void runMacro( final String args )
	{
		ApplyBigwarpTransformPlugIn.runFromParameters( parseMacroOptions( args ), null, null, null );
	}

	/**
	 * ImageJ records a checked box as a bare keyword. Match it as a whole token rather
	 * than a substring, so a value that happens to contain the key does not look like a
	 * set flag.
	 *
	 * @param args the macro options
	 * @param key the option key
	 * @return whether the flag is present
	 */
	private static boolean macroFlag( final String args, final String key )
	{
		if ( args == null )
			return false;

		return Arrays.stream( args.split( "\\s+" ) ).anyMatch( s -> s.equals( key ) );
	}

	private static String join( final long[] x )
	{
		return x == null ? "" : Arrays.stream( x ).mapToObj( Long::toString ).collect( Collectors.joining( LIST_SEPARATOR ) );
	}

	private static String join( final double[] x )
	{
		return x == null ? "" : Arrays.stream( x ).mapToObj( Double::toString ).collect( Collectors.joining( LIST_SEPARATOR ) );
	}

	private static long[] parseLongs( final String s )
	{
		if ( s == null || s.isEmpty() )
			return null;

		return Arrays.stream( s.split( LIST_SEPARATOR ) ).mapToLong( Long::valueOf ).toArray();
	}

	private static double[] parseDoubles( final String s )
	{
		if ( s == null || s.isEmpty() )
			return null;

		return Arrays.stream( s.split( LIST_SEPARATOR ) ).mapToDouble( Double::valueOf ).toArray();
	}

	private static final String VIRTUAL_HELP_TEXT = "Virtual images are fast to open and need little memory, "
			+ "but are slow to navigate.";

	private static final String N5_SOURCE_HELP_TEXT = "An n5, zarr, or hdf5 container, or a bdv xml. "
			+ "Used only when no image window is selected above.";

	private static final String N5_DATASET_HELP_TEXT = "The dataset inside the container.";

	public static void main( final String[] args )
	{
		new ij.ImageJ();
		createAndShow();
	}
}
