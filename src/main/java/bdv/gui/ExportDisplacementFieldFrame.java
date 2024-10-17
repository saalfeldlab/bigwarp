package bdv.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.formdev.flatlaf.util.UIScale;

import bdv.ij.ApplyBigwarpPlugin;
import bdv.ij.BigWarpToDeformationFieldPlugIn;
import bdv.ij.BigWarpToDeformationFieldPlugIn.DeformationFieldExportParameters;
import bdv.viewer.Source;
import bigwarp.BigWarp;
import bigwarp.BigWarpData;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import ij.IJ;
import ij.Macro;
import ij.plugin.frame.Recorder;

public class ExportDisplacementFieldFrame extends JFrame
{
	private static final long serialVersionUID = -6179153725489981013L;

	// formats
	public static final String FMT_NGFF = "NGFF";
	public static final String FMT_BIGWARP_TPS = "TPS";
	public static final String FMT_N5 = "N5";
	public static final String FMT_SLICER = "Slicer";

	public static enum DTYPE {
		BYTE, SHORT, FLOAT, DOUBLE
	};

	// macro recording
	public static final String commandName = "Big Warp to Displacement field";
	protected static final String landmarksKey = "landmarks";
	protected static final String splitAffineKey = "split_affine";
	protected static final String typeKey = "type";
	protected static final String dtypeKey = "dataType";
	protected static final String quantizationErrorKey = "quantizationError";
	protected static final String directionKey = "direction";
	protected static final String inverseToleranceKey = "inverseTolerance";
	protected static final String inverseMaxIterationsKey = "inverseMaxIterations";

	protected static final String virtualKey = "virtual";
	protected static final String threadsKey = "threads";
	protected static final String formatKey = "format";
	protected static final String sizeKey = "pixel_size";
	protected static final String spacingKey = "pixel_spacing";
	protected static final String minKey = "min";
	protected static final String unitKey = "unit";
	protected static final String n5RootKey = "n5_root";
	protected static final String n5DatasetKey = "n5_dataset";
	protected static final String n5BlockSizeKey = "n5_block_size";
	protected static final String n5CompressionKey = "n5_compression";

	private String lastBrowsePath = null;
	private String initialPath = null;

	// no need at the moment for these callbacks to consume parameters, but will leave it this way
    private Consumer<DeformationFieldExportParameters> okayCallback;
    private Consumer<DeformationFieldExportParameters> cancelCallback;

	private BigWarpData< ? > data;
	private BigWarpTransform bwTransform;
	private LandmarkTableModel ltm;

	private boolean imageJOpen;
	private boolean initialRecorderState;

	private boolean n5DatasetChanged;
	private DocumentListener docListener;

	private JPanel contentPanel;
	private JPanel invPanel;

	private JTextField landmarkPathTxt;
	private JButton browseLandmarksButton;
	private JTextField n5RootTxt;
	private JButton browseN5Button;
	private JTextField n5DatasetTxt;
	private JTextField n5BlockSizeTxt;
	private JComboBox< String > n5CompressionDropdown;
	private JCheckBox splitAffineCheckBox;
	private JCheckBox virtualCheckBox;

	private JComboBox<DTYPE> dataTypeComboBox;
	private JSpinner quantizationErrorSpinner;

	private JComboBox< String > typeComboBox;
	private JComboBox< String > directionComboBox;
	private JSpinner nThreadsField;
	private JComboBox< String > formatComboBox;
	private JButton okBtn;
	private JButton cancelBtn;

	// inverse options
	private JSpinner invMaxIterationsSpinner;
	private JSpinner invToleranceSpinner;

	private FieldOfViewPanel fovPanel;

	public ExportDisplacementFieldFrame( BigWarp<?> bw )
	{
		this( bw.getData(), bw.getBwTransform(), bw.getLandmarkPanel().getTableModel());
	}

	public ExportDisplacementFieldFrame( BigWarpData<?> data, BigWarpTransform bwTransform, LandmarkTableModel ltm )
	{
		super( "Export transformation" );
		initialPath = "";
		imageJOpen = IJ.getInstance() != null;

		this.data = data;
		this.bwTransform = bwTransform;
		this.ltm = ltm;

		cancelCallback = x -> {
			dispose();
			setVisible( false );
			Recorder.record = initialRecorderState;
		};

		okayCallback = x -> {
			macroRecord();
			run();
			Recorder.record = initialRecorderState;
			dispose();
			setVisible( false );
		};

		// attach to the n5Dataset text field, keep track of whether user changes it
		// once user change occurs, default values from direction dropdown no longer affect it
		docListener = new DocumentListener() {
			@Override
			public void changedUpdate( DocumentEvent e ) { }

			@Override
			public void insertUpdate( DocumentEvent e ) { n5DatasetChanged = true; }

			@Override
			public void removeUpdate( DocumentEvent e ) { n5DatasetChanged = true; }
		};
	}

	public static void createAndShow()
	{
		createAndShow( null, null, null );
	}

	public static void createAndShow( final BigWarp< ? > bw )
	{
		ExportDisplacementFieldFrame frame = new ExportDisplacementFieldFrame( bw );
		if ( bw == null )
			frame = new ExportDisplacementFieldFrame( null, null, null );
		else
			frame = new ExportDisplacementFieldFrame( bw );

		frame.createContent();
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.pack();
		frame.setVisible( true );
	}

	public static void createAndShow( BigWarpData< ? > data, BigWarpTransform bwTransform, LandmarkTableModel ltm )
	{
		final ExportDisplacementFieldFrame frame = new ExportDisplacementFieldFrame( data, bwTransform, ltm );
		frame.createContent();
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.pack();
		frame.setVisible( true );
	}

	public void createContent()
	{
		final boolean isNonlinear = bwTransform.isNonlinear();

		n5DatasetChanged = false;
		final int frameSizeX = UIScale.scale( 600 );

		final JPanel panel = basicPanel();

		invPanel = inverseOptionsPanel( frameSizeX );
		invPanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"Inverse options" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );


		final JPanel n5Panel = n5OptionsPanel( frameSizeX );
		n5Panel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"N5 options" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );

		// field of view panel
		String unit = "pixel";
		if( data != null )
		{
			final Source< ? > src = data.getMovingSource( 0 ).getSpimSource();
			unit = src.getVoxelDimensions().unit();
		}

		if( isNonlinear )
		{
			fovPanel = new FieldOfViewPanel( data, ltm, bwTransform, unit, 150,
					new double[] { 0, 0, 0 },
					new double[] { 1, 1, 1 },
					new long[] { 256, 256, 128 }
			);

			fovPanel.setBorder( BorderFactory.createCompoundBorder(
					BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
					BorderFactory.createCompoundBorder(
							BorderFactory.createTitledBorder(
									BorderFactory.createEtchedBorder(),
									"Field of view" ),
							BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );
		}

		contentPanel = new JPanel();
		contentPanel.setLayout( new GridBagLayout() );

		final GridBagConstraints cGbc = new GridBagConstraints();
		cGbc.gridx = 0;
		cGbc.gridwidth = 4;
		cGbc.fill = GridBagConstraints.HORIZONTAL;

		cGbc.gridy = 0;
		contentPanel.add( panel, cGbc );
		cGbc.gridy = 1;
		contentPanel.add( invPanel, cGbc );
		invPanel.setEnabled( false );
		invPanel.setVisible( false );
		cGbc.gridy = 2;
		contentPanel.add( n5Panel, cGbc );
		cGbc.gridy = 3;
		if( isNonlinear )
			contentPanel.add( fovPanel, cGbc );

		// bottom button section
		final GridBagConstraints cbot = new GridBagConstraints();
		cbot.gridx = 0;
		cbot.gridy = 4;
		cbot.gridwidth = 1;
		cbot.gridheight = 1;
		cbot.weightx = 1.0;
		cbot.weighty = 0.0;
		cbot.fill = GridBagConstraints.NONE;

		okBtn = new JButton( "OK" );
		okBtn.setPreferredSize( new Dimension( okBtn.getPreferredSize().width, okBtn.getPreferredSize().height ) );
		okBtn.addActionListener( e -> okayCallback.accept( getParams() ) );
		cbot.gridx = 2;
		cbot.gridwidth = 1;
		cbot.insets = new Insets( 20, ( int ) ( frameSizeX * 0.8 ), 20, 2 );
		cbot.anchor = GridBagConstraints.LINE_END;
		contentPanel.add( okBtn, cbot );

		cancelBtn = new JButton( "Cancel" );
		cancelBtn.setPreferredSize( new Dimension( cancelBtn.getPreferredSize().width, cancelBtn.getPreferredSize().height ) );
		cancelBtn.addActionListener( e -> cancelCallback.accept( null ) );
		cbot.gridx = 3;
		cbot.anchor = GridBagConstraints.LINE_START;
		cbot.ipadx = 0;
		cbot.insets = new Insets( 2, 2, 2, 2 );
		contentPanel.add( cancelBtn, cbot );

		final Container content = getContentPane();
		content.add( contentPanel );

		if( isNonlinear )
		{
			if( data != null )
			{
				// set default resolution to target image resolution
				final double[] res = ApplyBigwarpPlugin.getResolution( data, ApplyBigwarpPlugin.TARGET, null );
				fovPanel.setSpacing(res);
				fovPanel.updateFieldsFromReference();
			}
			else
				fovPanel.updateFieldsFromImageJReference();
		}

		addDefaultN5DatasetAction();
	}

	public JPanel basicPanel() {

		final int OUTER_PAD = BigWarpInitDialog.DEFAULT_OUTER_PAD;
		final int BUTTON_PAD = BigWarpInitDialog.DEFAULT_BUTTON_PAD;
		final int MID_PAD = BigWarpInitDialog.DEFAULT_MID_PAD;
		final Insets defaultInsets = new Insets(OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD);

		final Insets leftInsets = new Insets(OUTER_PAD, 10 * BUTTON_PAD, MID_PAD, BUTTON_PAD);
		final Insets rightInsets = new Insets(OUTER_PAD, BUTTON_PAD, MID_PAD, 10 * BUTTON_PAD);

		final int szX = UIScale.scale(600);
		final JPanel panel = new JPanel(false);
		panel.setLayout(new GridBagLayout());

		final GridBagConstraints ctxt = new GridBagConstraints();
		ctxt.gridx = 0;
		ctxt.gridy = 0;
		ctxt.gridwidth = 1;
		ctxt.gridheight = 1;
		ctxt.weightx = 0.0;
		ctxt.weighty = 0.0;
		ctxt.anchor = GridBagConstraints.LINE_END;
		ctxt.fill = GridBagConstraints.NONE;
		ctxt.insets = defaultInsets;

		final GridBagConstraints gbcBar = new GridBagConstraints();
		gbcBar.gridx = 1;
		gbcBar.gridy = 0;
		gbcBar.gridwidth = 6;
		gbcBar.gridheight = 1;
		gbcBar.weightx = 1.0;
		gbcBar.weighty = 0.0;
		gbcBar.fill = GridBagConstraints.HORIZONTAL;
		gbcBar.insets = defaultInsets;

		final GridBagConstraints cProjBrowse = new GridBagConstraints();
		cProjBrowse.gridx = 7;
		cProjBrowse.gridy = 0;
		cProjBrowse.gridwidth = 1;
		cProjBrowse.weightx = 0.0;
		cProjBrowse.fill = GridBagConstraints.HORIZONTAL;
		cProjBrowse.insets = defaultInsets;

		// Don't ask for landmarks if running from a bigwarp instance
		if (bwTransform == null) {
			panel.add(new JLabel("Landmarks:"), ctxt);

			landmarkPathTxt = new JTextField();
			landmarkPathTxt.setPreferredSize(new Dimension(szX / 3, landmarkPathTxt.getPreferredSize().height));
			panel.add(landmarkPathTxt, gbcBar);

			browseLandmarksButton = new JButton("Browse");
			browseLandmarksButton.addActionListener(e -> {
				browseLandmarksDialog();
			});
			panel.add(browseLandmarksButton, cProjBrowse);
		}

		ctxt.gridx = 0;
		ctxt.gridy = 1;
		ctxt.anchor = GridBagConstraints.LINE_END;
		final JLabel typeLabel = new JLabel("Type:");
		typeLabel.setToolTipText(TYPE_HELP_TEXT);
		panel.add(typeLabel, ctxt);

		final GridBagConstraints gbcCheck = new GridBagConstraints();
		gbcCheck.gridx = 1;
		gbcCheck.gridy = 1;
		gbcCheck.insets = defaultInsets;
		gbcCheck.anchor = GridBagConstraints.LINE_START;
		typeComboBox = new JComboBox<String>(new String[]{
				BigWarpToDeformationFieldPlugIn.flattenOption,
				BigWarpToDeformationFieldPlugIn.sequenceOption});
		panel.add(typeComboBox, gbcCheck);

		// want some more padding for direction
		ctxt.gridx = 3;
		ctxt.insets = new Insets(OUTER_PAD, 10 * BUTTON_PAD, MID_PAD, BUTTON_PAD);
		panel.add(new JLabel("Direction:"), ctxt);
		ctxt.insets = defaultInsets;

		gbcCheck.gridx = 4;
		directionComboBox = new JComboBox<String>(new String[]{
				BigWarpToDeformationFieldPlugIn.INVERSE_OPTIONS.FORWARD.toString(),
				BigWarpToDeformationFieldPlugIn.INVERSE_OPTIONS.INVERSE.toString(),
				BigWarpToDeformationFieldPlugIn.INVERSE_OPTIONS.BOTH.toString()
		});
		panel.add(directionComboBox, gbcCheck);

		final GridBagConstraints gbcQuantization = new GridBagConstraints();
		gbcQuantization.gridx = 5;
		gbcQuantization.gridy = 1;
		gbcQuantization.insets = leftInsets;
		gbcQuantization.anchor = GridBagConstraints.LINE_END;
		panel.add(new JLabel("Data type:"), gbcQuantization);

		dataTypeComboBox = new JComboBox<DTYPE>(new DTYPE[]{
				DTYPE.BYTE,
				DTYPE.SHORT,
				DTYPE.FLOAT,
				DTYPE.DOUBLE
		});
		dataTypeComboBox.setSelectedItem(DTYPE.FLOAT);

		gbcQuantization.gridx = 6;
		gbcQuantization.insets = rightInsets;
		gbcQuantization.anchor = GridBagConstraints.LINE_START;
		panel.add(dataTypeComboBox, gbcQuantization);

		ctxt.gridx = 7;
		ctxt.anchor = GridBagConstraints.LINE_END;
		panel.add(new JLabel("Split affine:"), ctxt);

		gbcCheck.gridx = 8;
		splitAffineCheckBox = new JCheckBox();
		panel.add(splitAffineCheckBox, gbcCheck);

		// second row
		ctxt.gridx = 0;
		ctxt.gridy = 2;
		ctxt.anchor = GridBagConstraints.LINE_END;
		panel.add(new JLabel("Threads:"), ctxt);

		gbcCheck.gridx = 1;
		gbcCheck.gridy = 2;
		gbcCheck.fill = GridBagConstraints.HORIZONTAL;
		nThreadsField = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
		panel.add(nThreadsField, gbcCheck);

		ctxt.gridx = 3;
		panel.add(new JLabel("Format:"), ctxt);
		gbcCheck.gridx = 4;
		formatComboBox = new JComboBox<String>(new String[]{FMT_NGFF, FMT_N5, FMT_BIGWARP_TPS});
		panel.add(formatComboBox, gbcCheck);

		gbcQuantization.gridx = 5;
		gbcQuantization.gridy = 2;
		gbcQuantization.insets = leftInsets;
		gbcQuantization.anchor = GridBagConstraints.LINE_END;
		panel.add(new JLabel("Error:"), gbcQuantization);

		gbcQuantization.gridx = 6;
		gbcQuantization.insets = rightInsets;
		quantizationErrorSpinner = new JSpinner(new SpinnerNumberModel(0.01, 1e-9, 999, 0.001));
		final JSpinner.NumberEditor editor = new JSpinner.NumberEditor(quantizationErrorSpinner, "#.######");
		final JFormattedTextField textField = editor.getTextField();
		textField.setColumns(6);
		quantizationErrorSpinner.setEditor(editor);
		panel.add(quantizationErrorSpinner, gbcQuantization);

		ctxt.gridx = 7;
		ctxt.anchor = GridBagConstraints.LINE_END;
		ctxt.insets = new Insets(OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD);
		ctxt.weightx = 0.1;
		panel.add(new JLabel("Virtual:"), ctxt);

		gbcCheck.gridx = 8;
		gbcCheck.weightx = 0.1;
		virtualCheckBox = new JCheckBox();
		panel.add(virtualCheckBox, gbcCheck);

		return panel;
	}

	public JPanel inverseOptionsPanel( final int frameSizeX )
	{
		final int OUTER_PAD = BigWarpInitDialog.DEFAULT_OUTER_PAD;
		final int BUTTON_PAD = BigWarpInitDialog.DEFAULT_BUTTON_PAD;
		final int MID_PAD = BigWarpInitDialog.DEFAULT_MID_PAD;

		final JPanel panel = new JPanel();
		panel.setLayout( new GridBagLayout() );

		final GridBagConstraints ctxt = new GridBagConstraints();
		ctxt.gridx = 0;
		ctxt.gridy = 0;
		ctxt.gridwidth = 1;
		ctxt.gridheight = 1;
		ctxt.weightx = 0.0;
		ctxt.weighty = 0.0;
		ctxt.anchor = GridBagConstraints.LINE_END;
		ctxt.fill = GridBagConstraints.NONE;
		ctxt.insets = new Insets( OUTER_PAD, OUTER_PAD, MID_PAD, BUTTON_PAD );

		final GridBagConstraints gbcBar = new GridBagConstraints();
		gbcBar.gridx = 1;
		gbcBar.gridy = 0;
		gbcBar.gridwidth = 1;
		gbcBar.gridheight = 1;
		gbcBar.weightx = 1.0;
		gbcBar.weighty = 0.0;
		gbcBar.fill = GridBagConstraints.HORIZONTAL;
		gbcBar.insets = new Insets( OUTER_PAD, OUTER_PAD, MID_PAD, BUTTON_PAD );

		ctxt.gridx = 0;
		ctxt.anchor = GridBagConstraints.LINE_END;
		panel.add( new JLabel( "Tolerance:" ), ctxt );

		gbcBar.gridx = 1;
		gbcBar.fill = GridBagConstraints.HORIZONTAL;
		invToleranceSpinner = new JSpinner( new SpinnerNumberModel( 0.5, 1e-9, 999999, 0.01 ) );

		final JSpinner.NumberEditor editor = new JSpinner.NumberEditor(invToleranceSpinner, "###,###.######");
		final JFormattedTextField textField = editor.getTextField();
        textField.setColumns(12);
        invToleranceSpinner.setEditor(editor);

		panel.add( invToleranceSpinner, gbcBar );

		ctxt.gridx = 3;
		ctxt.anchor = GridBagConstraints.LINE_END;
		panel.add( new JLabel( "Max iterations:" ), ctxt );

		gbcBar.gridx = 4;
		gbcBar.fill = GridBagConstraints.HORIZONTAL;
		invMaxIterationsSpinner = new JSpinner( new SpinnerNumberModel( 200, 1, 999999, 1 ) );
		panel.add( invMaxIterationsSpinner, gbcBar );

		return panel;
	}

	public JPanel n5OptionsPanel( final int frameSizeX )
	{
		final int OUTER_PAD = BigWarpInitDialog.DEFAULT_OUTER_PAD;
		final int BUTTON_PAD = BigWarpInitDialog.DEFAULT_BUTTON_PAD;
		final int MID_PAD = BigWarpInitDialog.DEFAULT_MID_PAD;

		final JPanel panel = new JPanel();
		panel.setLayout( new GridBagLayout() );

		final GridBagConstraints ctxt = new GridBagConstraints();
		ctxt.gridx = 0;
		ctxt.gridy = 0;
		ctxt.gridwidth = 1;
		ctxt.gridheight = 1;
		ctxt.weightx = 0.0;
		ctxt.weighty = 0.0;
		ctxt.anchor = GridBagConstraints.LINE_END;
		ctxt.fill = GridBagConstraints.NONE;
		ctxt.insets = new Insets( OUTER_PAD, OUTER_PAD, MID_PAD, BUTTON_PAD );
		panel.add( new JLabel( "Root folder:" ), ctxt );

		final GridBagConstraints gbcBar = new GridBagConstraints();
		gbcBar.gridx = 1;
		gbcBar.gridy = 0;
		gbcBar.gridwidth = 6;
		gbcBar.gridheight = 1;
		gbcBar.weightx = 1.0;
		gbcBar.weighty = 0.0;
		gbcBar.fill = GridBagConstraints.HORIZONTAL;
		gbcBar.insets = new Insets( OUTER_PAD, OUTER_PAD, MID_PAD, BUTTON_PAD );

		n5RootTxt = new JTextField();
		n5RootTxt.setPreferredSize( new Dimension( frameSizeX / 3, n5RootTxt.getPreferredSize().height ) );
		panel.add( n5RootTxt, gbcBar );

		final GridBagConstraints cProjBrowse = new GridBagConstraints();
		cProjBrowse.gridx = 7;
		cProjBrowse.gridy = 0;
		cProjBrowse.gridwidth = 1;
		cProjBrowse.weightx = 0.0;
		cProjBrowse.fill = GridBagConstraints.HORIZONTAL;
		cProjBrowse.insets = new Insets( OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD );
		browseN5Button = new JButton( "Browse" );
		browseN5Button.addActionListener( e -> {
			browseN5Root();
		} );
		panel.add( browseN5Button, cProjBrowse );

		ctxt.gridy = 1;
		panel.add( new JLabel( "Dataset:" ), ctxt );

		gbcBar.gridy = 1;
		n5DatasetTxt = new JTextField();
		n5DatasetTxt.setPreferredSize( new Dimension( frameSizeX / 3, n5DatasetTxt.getPreferredSize().height ) );
		n5DatasetTxt.setText( "dfield" );
		n5DatasetTxt.getDocument().addDocumentListener( docListener );


		panel.add( n5DatasetTxt, gbcBar );

		ctxt.gridy = 2;
		panel.add( new JLabel( "Block size:" ), ctxt );

		gbcBar.gridy = 2;
		n5BlockSizeTxt = new JTextField();
		n5BlockSizeTxt.setPreferredSize( new Dimension( frameSizeX / 3, n5BlockSizeTxt.getPreferredSize().height ) );
		n5BlockSizeTxt.setText( "64" );
		panel.add( n5BlockSizeTxt, gbcBar );

		ctxt.gridy = 3;
		panel.add( new JLabel( "Compression" ), ctxt );

		gbcBar.gridy = 3;
		gbcBar.fill = GridBagConstraints.NONE;
		gbcBar.anchor = GridBagConstraints.LINE_START;
		n5CompressionDropdown = new JComboBox< String >( BigWarpToDeformationFieldPlugIn.compressionOptions );
		panel.add( n5CompressionDropdown, gbcBar );

		return panel;
	}

	private void addDefaultN5DatasetAction()
	{
		directionComboBox.addActionListener( e -> {
			if( n5DatasetChanged ) {
				return;
			}

			final String item = (String)directionComboBox.getSelectedItem();
			if( item.equals( BigWarpToDeformationFieldPlugIn.INVERSE_OPTIONS.FORWARD.toString() ))
			{
				n5DatasetTxt.getDocument().removeDocumentListener( docListener );
				n5DatasetTxt.setText( "dfield" );
				n5DatasetTxt.getDocument().addDocumentListener( docListener );

				SwingUtilities.invokeLater( () -> {
					invPanel.setEnabled( false );
					invPanel.setVisible( false );
					contentPanel.revalidate();
					contentPanel.repaint();
					pack();
				});
			}
			else if( item.equals( BigWarpToDeformationFieldPlugIn.INVERSE_OPTIONS.INVERSE.toString() ))
			{
				n5DatasetTxt.getDocument().removeDocumentListener( docListener );
				n5DatasetTxt.setText( "invdfield" );
				n5DatasetTxt.getDocument().addDocumentListener( docListener );
				SwingUtilities.invokeLater( () -> {
					invPanel.setEnabled( true );
					invPanel.setVisible( true );
					contentPanel.revalidate();
					contentPanel.repaint();
					pack();
				});
			}
			else if( item.equals( BigWarpToDeformationFieldPlugIn.INVERSE_OPTIONS.BOTH.toString() ))
			{
				n5DatasetTxt.getDocument().removeDocumentListener( docListener );
				n5DatasetTxt.setText( "transform" );
				n5DatasetTxt.getDocument().addDocumentListener( docListener );
				SwingUtilities.invokeLater( () -> {
					invPanel.setEnabled( true );
					invPanel.setVisible( true );
					contentPanel.revalidate();
					contentPanel.repaint();
					pack();
				});
			}
		});
	}

	private String browseLandmarksDialog()
	{
		final String s = browseDialogGeneral( JFileChooser.FILES_ONLY, new FileNameExtensionFilter( "csv file", "csv" ) );
		landmarkPathTxt.setText( s );

		return s;
	}

	private String browseN5Root()
	{
		final String s = browseDialogGeneral( JFileChooser.FILES_AND_DIRECTORIES, null );
		n5RootTxt.setText( s );
		return s;
	}

	private String browseDialogGeneral( final int mode, final FileFilter filefilter )
	{

		final JFileChooser fileChooser = new JFileChooser();
		/*
		 * Need to allow files so h5 containers can be opened, and directories
		 * so that filesystem n5's and zarrs can be opened.
		 */
		fileChooser.setFileSelectionMode( mode );
		if( filefilter == null )
		{
			fileChooser.setFileFilter( filefilter );
		}

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

	public DeformationFieldExportParameters getParams()
	{
		final String n5BlockSizeString = n5BlockSizeTxt.getText();
		final int[] blockSize = n5BlockSizeString.isEmpty() ? null :
			Arrays.stream( n5BlockSizeString.split( "," ) ).mapToInt( Integer::parseInt ).toArray();

		return new DeformationFieldExportParameters(
				landmarkPathTxt == null ? "" : landmarkPathTxt.getText(),
				splitAffineCheckBox.isSelected(),
				(String)typeComboBox.getSelectedItem(),
				(DTYPE)dataTypeComboBox.getSelectedItem(),
				(Double)quantizationErrorSpinner.getValue(),
				(String)directionComboBox.getSelectedItem(),
				(Double)invToleranceSpinner.getValue(),
				(Integer)invMaxIterationsSpinner.getValue(),
				virtualCheckBox.isSelected(),
				(Integer)nThreadsField.getValue(),
				(String)formatComboBox.getSelectedItem(),
				fovPanel == null ? null : fovPanel.getPixelSize(),
				fovPanel == null ? null : fovPanel.getSpacing(),
				fovPanel == null ? null : fovPanel.getMin(),
				fovPanel == null ? null : fovPanel.getUnit(),
				n5RootTxt.getText(),
				n5DatasetTxt.getText(),
				blockSize,
				BigWarpToDeformationFieldPlugIn.getCompression( (String)n5CompressionDropdown.getSelectedItem() ) );
	}

	public void run()
	{
		BigWarpToDeformationFieldPlugIn.runFromParameters( getParams(), data, ltm, bwTransform );
	}

	public String macroRecord()
	{
		if( !Recorder.record )
			return "";

		Recorder.setCommand( commandName );
		final String szString = Arrays.stream( fovPanel.getPixelSize() ).mapToObj( Long::toString ).collect( Collectors.joining( "," ) );
		final String spacingString = Arrays.stream( fovPanel.getSpacing() ).mapToObj( Double::toString ).collect( Collectors.joining( "," ) );
		final String minString = Arrays.stream( fovPanel.getMin() ).mapToObj( Double::toString ).collect( Collectors.joining( "," ) );

		Recorder.resetCommandOptions();
		Recorder.recordOption( landmarksKey, landmarkPathTxt.getText().trim() );
		Recorder.recordOption( splitAffineKey );
		Recorder.recordOption( virtualKey );
		Recorder.recordOption( typeKey, ( String ) typeComboBox.getSelectedItem() );
		Recorder.recordOption( threadsKey, Integer.toString( ( Integer ) nThreadsField.getValue() ) );
		Recorder.recordOption( sizeKey, szString );
		Recorder.recordOption( spacingKey, spacingString );
		Recorder.recordOption( minKey, minString );
		Recorder.recordOption( unitKey, fovPanel.getUnit() );

		if( !n5RootTxt.getText().isEmpty() )
		{
			Recorder.recordOption( n5RootKey, n5RootTxt.getText().trim() );
			Recorder.recordOption( n5DatasetKey, n5DatasetTxt.getText().trim() );
			Recorder.recordOption( n5BlockSizeKey, n5BlockSizeTxt.getText().trim() );
			Recorder.recordOption( n5CompressionKey, ( String ) n5CompressionDropdown.getSelectedItem() );
		}

		Recorder.saveCommand();
		return Recorder.getCommandOptions();
	}

	public static void runMacro( String args )
	{
		final String landmarks = Macro.getValue( args, landmarksKey, "" );
		final String type = Macro.getValue( args, typeKey, "" );

		final DTYPE dtype = DTYPE.valueOf(Macro.getValue(args, dtypeKey, ""));
		final double maxQuantizationError = Double.valueOf(Macro.getValue(args, quantizationErrorKey, ""));

		final String direction = Macro.getValue( args, directionKey, "" );
		final double tolerance = Double.valueOf( Macro.getValue( args, inverseToleranceKey, "" ));
		final int maxIters = Integer.valueOf( Macro.getValue( args, inverseMaxIterationsKey, "" ));

		final boolean splitAffine =  args.contains(" " + splitAffineKey );
		final boolean openAsVirtual = args.contains(" " + virtualKey);
		final int threads = Integer.valueOf( Macro.getValue( args, threadsKey, "1" ));
		final String format = Macro.getValue( args, formatKey, FMT_NGFF );

		final double[] min = Arrays.stream( Macro.getValue( args, minKey, "" ).split( "," ) ).mapToDouble( Double::valueOf ).toArray();
		final double[] spacing = Arrays.stream( Macro.getValue( args, spacingKey, "" ).split( "," ) ).mapToDouble( Double::valueOf ).toArray();
		final long[] pixSize = Arrays.stream( Macro.getValue( args, sizeKey, "" ).split( "," ) ).mapToLong( Long::valueOf ).toArray();
		final String unit = Macro.getValue( args, typeKey, "pixel" );

		final String n5Root = Macro.getValue( args, n5RootKey, "" );
		final String n5Dataset = Macro.getValue( args, n5DatasetKey, "" );
		final String n5BlockSizeString = Macro.getValue( args, n5BlockSizeKey, "" );
		final String n5Compression = Macro.getValue( args, n5CompressionKey, "" );

		final int[] blockSize = n5BlockSizeString.isEmpty() ? null :
			Arrays.stream( n5BlockSizeString.split( "," ) ).mapToInt( Integer::parseInt ).toArray();

		final DeformationFieldExportParameters params = new DeformationFieldExportParameters(
				landmarks, splitAffine, type,
				dtype, maxQuantizationError,
				direction, tolerance, maxIters,
				openAsVirtual, threads, format,
				pixSize, spacing, min, unit,
				n5Root,
				n5Dataset,
				blockSize,
				BigWarpToDeformationFieldPlugIn.getCompression(n5Compression));

		BigWarpToDeformationFieldPlugIn.runFromParameters( params, null, null, null );
	}

	private static final String TYPE_HELP_TEXT = "\"Flat\" combines all transformations into a single displacement field. \"Sequence\" keeps "
			+ "the components separate (e.g. affine, nonlinear).";

	private static final String DTYPE_HELP_TEXT = "The data type used to store the displacement field.";

	private static final String DIRECTION_HELP_TEXT = "";

}