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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.formdev.flatlaf.util.UIScale;

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

	// macro recording
	public static final String commandName = "Big Warp to Displacement field";
	protected static final String landmarksKey = "landmarks";
	protected static final String splitAffineKey = "split_affine";
	protected static final String typeKey = "type";
	protected static final String virtualKey = "virtual";
	protected static final String threadsKey = "threads";
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

	private JTextField landmarkPathTxt;
	private JButton browseLandmarksButton;
	private JTextField n5RootTxt;
	private JButton browseN5Button;
	private JTextField n5DatasetTxt;
	private JTextField n5BlockSizeTxt;
	private JComboBox< String > n5CompressionDropdown;
	private JCheckBox splitAffineCheckBox;
	private JCheckBox virtualCheckBox;
	private JComboBox< String > typeComboBox;
	private JSpinner nThreadsField;
	private JButton okBtn;
	private JButton cancelBtn;
	private FieldOfViewPanel fovPanel;

	public ExportDisplacementFieldFrame( BigWarp<?> bw )
	{
		this( bw.getData(), bw.getBwTransform(), bw.getLandmarkPanel().getTableModel());
	}

	public ExportDisplacementFieldFrame( BigWarpData<?> data, BigWarpTransform bwTransform, LandmarkTableModel ltm )
	{
		super( "Export displacement field" );
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
		ExportDisplacementFieldFrame frame = new ExportDisplacementFieldFrame( data, bwTransform, ltm );
		frame.createContent();
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.pack();
		frame.setVisible( true );
	}

	public void createContent()
	{
		final int frameSizeX = UIScale.scale( 600 );

		final JPanel panel = basicPanel();

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

		final JPanel contentPanel = new JPanel();
		contentPanel.setLayout( new GridBagLayout() );

		final GridBagConstraints cGbc = new GridBagConstraints();
		cGbc.gridx = 0;
		cGbc.gridwidth = 4;
		cGbc.fill = GridBagConstraints.HORIZONTAL;

		cGbc.gridy = 0;
		contentPanel.add( panel, cGbc );
		cGbc.gridy = 1;
		contentPanel.add( n5Panel, cGbc );
		cGbc.gridy = 2;
		contentPanel.add( fovPanel, cGbc );

		// bottom button section
		final GridBagConstraints cbot = new GridBagConstraints();
		cbot.gridx = 0;
		cbot.gridy = 3;
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

		if( data != null )
			fovPanel.updateFieldsFromReference();
		else
			fovPanel.updateFieldsFromImageJReference();
	}

	public JPanel basicPanel()
	{
		final int OUTER_PAD = BigWarpInitDialog.DEFAULT_OUTER_PAD;
		final int BUTTON_PAD = BigWarpInitDialog.DEFAULT_BUTTON_PAD;
		final int MID_PAD = BigWarpInitDialog.DEFAULT_MID_PAD;

		final int szX = UIScale.scale( 600 );

		final JPanel panel = new JPanel( false );
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
		gbcBar.gridwidth = 6;
		gbcBar.gridheight = 1;
		gbcBar.weightx = 1.0;
		gbcBar.weighty = 0.0;
		gbcBar.fill = GridBagConstraints.HORIZONTAL;
		gbcBar.insets = new Insets( OUTER_PAD, OUTER_PAD, MID_PAD, BUTTON_PAD );

		final GridBagConstraints cProjBrowse = new GridBagConstraints();
		cProjBrowse.gridx = 7;
		cProjBrowse.gridy = 0;
		cProjBrowse.gridwidth = 1;
		cProjBrowse.weightx = 0.0;
		cProjBrowse.fill = GridBagConstraints.HORIZONTAL;
		cProjBrowse.insets = new Insets( OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD );

		// Don't ask for landmarks if running from a bigwarp instance
		if( bwTransform == null )
		{
			panel.add( new JLabel( "Landmarks:" ), ctxt );

			landmarkPathTxt = new JTextField();
			landmarkPathTxt.setPreferredSize( new Dimension( szX / 3, landmarkPathTxt.getPreferredSize().height ) );
			panel.add( landmarkPathTxt, gbcBar );

			browseLandmarksButton = new JButton( "Browse" );
			browseLandmarksButton.addActionListener( e -> {
				browseLandmarksDialog();
			} );
			panel.add( browseLandmarksButton, cProjBrowse );
		}

		ctxt.gridy = 1;
		ctxt.anchor = GridBagConstraints.LINE_END;
		panel.add( new JLabel( "Split affine:" ), ctxt );

		final GridBagConstraints gbcCheck = new GridBagConstraints();
		gbcCheck.gridx = 1;
		gbcCheck.gridy = 1;
		gbcCheck.insets = new Insets( OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD );
		gbcCheck.anchor = GridBagConstraints.LINE_START;
		splitAffineCheckBox = new JCheckBox();
		panel.add( splitAffineCheckBox, gbcCheck );

		ctxt.gridx = 2;
		ctxt.anchor = GridBagConstraints.LINE_END;
		ctxt.insets = new Insets( OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD );
		ctxt.weightx = 0.1;
		panel.add( new JLabel( "Virtual:" ), ctxt );

		gbcCheck.gridx = 3;
		gbcCheck.weightx = 0.1;
		virtualCheckBox = new JCheckBox();
		panel.add( virtualCheckBox, gbcCheck );

		ctxt.gridx = 4;
		ctxt.anchor = GridBagConstraints.LINE_END;
		panel.add( new JLabel( "Type:" ), ctxt );

		gbcCheck.gridx = 5;
		typeComboBox = new JComboBox< String >( new String[] { 
				BigWarpToDeformationFieldPlugIn.flattenOption,
				BigWarpToDeformationFieldPlugIn.sequenceOption } );
		panel.add( typeComboBox, gbcCheck );

		ctxt.gridx = 6;
		ctxt.anchor = GridBagConstraints.LINE_END;
		panel.add( new JLabel( "Threads:" ), ctxt );

		gbcCheck.gridx = 7;
		nThreadsField = new JSpinner( new SpinnerNumberModel( 1, 1, 9999, 1 ) );
		panel.add( nThreadsField, gbcCheck );
		return panel;
	}

	public JPanel n5OptionsPanel( final int frameSizeX )
	{
		final int OUTER_PAD = BigWarpInitDialog.DEFAULT_OUTER_PAD;
		final int BUTTON_PAD = BigWarpInitDialog.DEFAULT_BUTTON_PAD;
		final int MID_PAD = BigWarpInitDialog.DEFAULT_MID_PAD;

		JPanel panel = new JPanel();
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
				virtualCheckBox.isSelected(),
				(Integer)nThreadsField.getValue(),
				fovPanel.getPixelSize(),
				fovPanel.getSpacing(),
				fovPanel.getMin(),
				fovPanel.getUnit(),
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
		final boolean splitAffine =  args.contains(" " + splitAffineKey );
		final boolean openAsVirtual = args.contains(" " + virtualKey);
		final int threads = Integer.valueOf( Macro.getValue( args, threadsKey, "1" ));

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

		DeformationFieldExportParameters params = new DeformationFieldExportParameters(
				landmarks, splitAffine, type, openAsVirtual, threads,
				pixSize, spacing, min, unit,
				n5Root,
				n5Dataset,
				blockSize,
				BigWarpToDeformationFieldPlugIn.getCompression( n5Compression ) );

		BigWarpToDeformationFieldPlugIn.runFromParameters( params, null, null, null );
	}

}