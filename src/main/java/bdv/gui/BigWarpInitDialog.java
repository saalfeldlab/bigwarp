package bdv.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.saalfeldlab.n5.bdv.N5ViewerTreeCellRenderer;
import org.janelia.saalfeldlab.n5.ij.N5Importer.N5BasePathFun;
import org.janelia.saalfeldlab.n5.ij.N5Importer.N5ViewerReaderFun;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.janelia.saalfeldlab.n5.ui.DatasetSelectorDialog;
import org.janelia.saalfeldlab.n5.ui.N5SwingTreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.jdom2.JDOMException;

import com.formdev.flatlaf.util.UIScale;

import bdv.gui.sourceList.BigWarpSourceListPanel;
import bdv.gui.sourceList.BigWarpSourceTableModel;
import bdv.gui.sourceList.BigWarpSourceTableModel.SourceRow;
import bdv.gui.sourceList.BigWarpSourceTableModel.SourceType;
import bdv.ij.util.ProgressWriterIJ;
import bdv.viewer.Source;
import bigwarp.BigWarp;
import bigwarp.BigWarpData;
import bigwarp.BigWarpInit;
import bigwarp.source.SourceInfo;
import bigwarp.transforms.NgffTransformations;
import bigwarp.transforms.metadata.N5TransformMetadata;
import bigwarp.transforms.metadata.N5TransformMetadataParser;
import bigwarp.transforms.metadata.N5TransformTreeCellRenderer;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.Prefs;
import ij.WindowManager;
import ij.plugin.frame.Recorder;
import mpicbg.spim.data.SpimDataException;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.NativeType;

public class BigWarpInitDialog extends JFrame
{
	private static final long serialVersionUID = -2914972130819029899L;

	public static String listSeparator = ",";

	private boolean imageJOpen;
	private DatasetService datasetService;

	private String initialPath;
	private JTextField projectPathTxt, containerPathText, transformPathText;
	private JLabel messageLabel;
	private JButton okBtn, cancelBtn;
	private JPanel listPanel;
	private JTable sourceTable;
	private JButton browseProjectButton, browseBtn, addN5Button, addN5TransformButton, browseTransformButton;
	private BigWarpSourceTableModel sourceTableModel;
	private JComboBox<String> imagePlusDropdown;
	private JButton addImageButton, addPathButton, addTransformButton;
	private DatasetSelectorDialog selectionDialog, transformSelectionDialog;

	private String lastOpenedContainer = "";
	private String lastBrowsePath = null;
	private ExecutorService exec;

    private Consumer<BigWarpSourceTableModel> okayCallback;
    private Consumer<BigWarpSourceTableModel> cancelCallback;
	private Consumer< String > imagePathUpdateCallback, transformPathUpdateCallback, projectPathUpdateCallback;

	public static final int DEFAULT_OUTER_PAD = 8;
	public static final int DEFAULT_BUTTON_PAD = 3;
	public static final int DEFAULT_MID_PAD = 5;

	private static final String commandName = "BigWarp";
	private static final String projectKey = "project";
	private static final String imagesKey = "images";
	private static final String movingKey = "moving";
	private static final String transformsKey = "transforms";

	public static final String ImageJPrefix = "imagej://";

	private String projectLandmarkPath;
	private String imageList;
	private String movingList;
	private String transformList;

	private boolean initialRecorderState;



	public BigWarpInitDialog( final String title )
	{
		this( title, null );
	}

	public BigWarpInitDialog( final String title, final DatasetService datasetService )
	{
		super( title );
		this.datasetService = datasetService;
		initialPath = "";
		imageJOpen = IJ.getInstance() != null;

		buildN5SelectionDialog();
        final Container content = getContentPane();
        content.add( createContent() );
        pack();

        initializeImagePlusSources();

		cancelCallback = x -> {
			setVisible( false );
			dispose();
			Recorder.record = initialRecorderState;
		};

		okayCallback = x -> {
			macroRecord();
			runBigWarp();
			Recorder.record = initialRecorderState;
			setVisible( false );
		};

		imagePathUpdateCallback = ( p ) -> {
			addPath();
		};

		transformPathUpdateCallback = ( p ) -> {
			addTransform();
		};
	}

	public static void main( String[] args ) {

		final ImageJ ij = new ImageJ();
//		runBigWarp( "/home/john/tmp/boats_lm.csv",
//				new String[]{ "/home/john/tmp/boats.tif", "/home/john/tmp/boats.tif" },
//				new String[]{ "true", "false" },
//				null);

//		runBigWarp( "/home/john/tmp/boats_lm2.csv",
//				new String[]{ "/home/john/tmp/boats.tif", "/home/john/tmp/boats-HR.tif" },
//				new String[]{ "true", "false" },
//				null);

//		runBigWarp( "file:///home/john/Documents/presentations/20231130_BdvCommunity/demo/boats-hr-project.json ",
//				null,
//				null,
//				null);

		new BigWarpInitDialog("bigwarp test").createAndShow();
	}

	public void setInitialRecorderState( final boolean initialRecorderState )
	{
		this.initialRecorderState = initialRecorderState;
	}

	public static < T extends NativeType<T> > BigWarp<?> runBigWarp( final String projectLandmarkPath, final String[] images, final String[] moving, final String[] transforms )
	{
		final String projectLandmarkPathTrim = projectLandmarkPath == null ? null : projectLandmarkPath.trim();
		final BigWarpData< T > data = BigWarpInit.initData();
		final boolean haveProjectLandmarkArg = projectLandmarkPathTrim != null && !projectLandmarkPathTrim.isEmpty();
		final boolean haveProject = haveProjectLandmarkArg && projectLandmarkPathTrim.trim().endsWith(".json");
		final boolean haveLandmarks = haveProjectLandmarkArg && projectLandmarkPathTrim.trim().endsWith(".csv");

		final int nThreads = IJ.getInstance() != null ? Prefs.getThreads() : 1;
		final BigWarpViewerOptions bwOpts = ( BigWarpViewerOptions ) BigWarpViewerOptions.options().numRenderingThreads( nThreads );

		if( !haveProject )
		{
			int id = 0;
			final int N = images.length;
			for( int i = 0; i < N; i++ )
			{
				// TODO better messages for exceptions?
				try
				{
					final LinkedHashMap< Source< T >, SourceInfo > infos = BigWarpInit.createSources( data, images[ i ], id, moving[ i ].equals( "true" ) );

					RealTransform transform = null;
					final Supplier<String> transformSupplier;
					if( transforms != null && transforms.length > i )
					{
						final String transformUrl = transforms[ i ];
						if( transformUrl!= null && !transformUrl.isEmpty() )
						{
							transform = NgffTransformations.open( transformUrl );
							transformSupplier = () -> transformUrl;
						}
						else {
							transformSupplier = null;
						}
					}
					else
					{
						transformSupplier = null;
					}

					// add performs a null check on transform
					BigWarpInit.add( data, infos, transform, transformSupplier );

					id += infos.size();
				}
				catch ( final URISyntaxException e )
				{
					e.printStackTrace();
				}
				catch ( final IOException e )
				{
					e.printStackTrace();
				}
				catch ( final SpimDataException e )
				{
					e.printStackTrace();
				}
			}
		}

		BigWarp<?> bw = null;
		try
		{
			bwOpts.is2D(BigWarp.detectNumDims( data.sources ) == 2);
			data.applyTransformations();

			bw = new BigWarp<>( data, bwOpts, new ProgressWriterIJ() );
			if( haveProject )
				bw.loadSettings( projectLandmarkPathTrim, true );
			else if( haveLandmarks )
				bw.loadLandmarks( projectLandmarkPathTrim );
		}
		catch ( final SpimDataException e )
		{
			e.printStackTrace();
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
		catch ( final JDOMException e )
		{
			e.printStackTrace();
		}

		return bw;
	}

	public <T extends NativeType<T>> void runBigWarp()
	{
		if (Recorder.record)
		{
			Recorder.setCommand(commandName);
			macroRecord();
			Recorder.saveCommand();
		}

		final BigWarpData< T > data = BigWarpInit.initData();

		projectLandmarkPath = projectLandmarkPath == null ? projectPathTxt.getText().trim() : projectLandmarkPath;
		final boolean haveProjectLandmarkArg = projectLandmarkPath != null && !projectLandmarkPath.isEmpty();
		final boolean haveProject = haveProjectLandmarkArg && projectLandmarkPath.endsWith(".json");
		final boolean haveLandmarks = haveProjectLandmarkArg && projectLandmarkPath.endsWith(".csv");

		if( !haveProject )
		{
			int id = 0;
			final int N = sourceTable.getRowCount();
			for( int i = 0; i < N; i++ )
			{
				final SourceRow tableRow = sourceTableModel.get( i );
				if( tableRow.type.equals( SourceType.IMAGEPLUS )  )
				{
					// strip off prefix if present
					final ImagePlus imp = WindowManager.getImage( tableRow.srcName.replaceAll( "^"+ImageJPrefix, "" ) );
					final LinkedHashMap< Source< T >, SourceInfo > infos = BigWarpInit.createSources( data, imp, id, 0, tableRow.moving );
					BigWarpInit.add( data, infos, tableRow.getTransform(), tableRow.getTransformUri() );
					id += infos.size();
				}
				else if( tableRow.type.equals( SourceType.DATASET ))
				{
					final Dataset dataset = datasetService.getDatasets().stream()
							.filter( x -> x.getSource().equals( tableRow.srcName ) )
							.findFirst().get();
					final LinkedHashMap< Source< T >, SourceInfo > infos = BigWarpInit.createSources( data, dataset, id, tableRow.moving );
					BigWarpInit.add( data, infos, tableRow.getTransform(), tableRow.getTransformUri() );
					id += infos.size();
				}
				else
				{
					// deal with exceptions differently?
					try
					{
						final LinkedHashMap< Source< T >, SourceInfo > infos = BigWarpInit.createSources( data, tableRow.srcName, id, tableRow.moving );
						BigWarpInit.add( data, infos, tableRow.getTransform(), tableRow.getTransformUri() );
						id += infos.size();
					}
					catch ( final URISyntaxException e )
					{
						e.printStackTrace();
					}
					catch ( final IOException e )
					{
						e.printStackTrace();
					}
					catch ( final SpimDataException e )
					{
						e.printStackTrace();
					}
				}
			}
		}

		BigWarp<?> bw;
		try
		{
			data.applyTransformations();
			bw = new BigWarp<>( data, new ProgressWriterIJ() );
			if( haveProject )
				bw.loadSettings( projectLandmarkPath, true );
			else if( haveLandmarks )
				bw.loadLandmarks(projectLandmarkPath);
		}
		catch ( final SpimDataException e )
		{
			e.printStackTrace();
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
		catch ( final JDOMException e )
		{
			e.printStackTrace();
		}

	}

	public JPanel createContent()
	{
		final int OUTER_PAD = DEFAULT_OUTER_PAD;
		final int BUTTON_PAD = DEFAULT_BUTTON_PAD;
		final int MID_PAD = DEFAULT_MID_PAD;

		final int frameSizeX = UIScale.scale( 600 );
		final JPanel panel = new JPanel(false);
		panel.setPreferredSize( new Dimension( frameSizeX, UIScale.scale( 335 ) )); // ~ 16:9
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
		ctxt.insets = new Insets(OUTER_PAD, OUTER_PAD, MID_PAD, BUTTON_PAD);
		panel.add( new JLabel("BigWarp project or landmarks:"), ctxt );

		final GridBagConstraints gbcBar = new GridBagConstraints();
		gbcBar.gridx = 1;
		gbcBar.gridy = 0;
		gbcBar.gridwidth = 6;
		gbcBar.gridheight = 1;
		gbcBar.weightx = 1.0;
		gbcBar.weighty = 0.0;
		gbcBar.fill = GridBagConstraints.HORIZONTAL;
		gbcBar.insets = new Insets(OUTER_PAD, OUTER_PAD, MID_PAD, BUTTON_PAD);

		projectPathTxt = new JTextField();
		projectPathTxt.setPreferredSize( new Dimension( frameSizeX / 3, projectPathTxt.getPreferredSize().height ) );
		panel.add(projectPathTxt, gbcBar);

		// gbc bars below are width 4
		gbcBar.gridwidth = 4;

		final GridBagConstraints cProjBrowse = new GridBagConstraints();
		cProjBrowse.gridx = 7;
		cProjBrowse.gridy = 0;
		cProjBrowse.gridwidth = 1;
		cProjBrowse.weightx = 0.0;
		cProjBrowse.fill = GridBagConstraints.HORIZONTAL;
		cProjBrowse.insets = new Insets(OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD);
		browseProjectButton = new JButton("Browse");
		browseProjectButton.addActionListener( e -> { browseProjectDialog(); } );
		panel.add(browseProjectButton, cProjBrowse);


		// Add open imagej image
		ctxt.gridy = 1;
		panel.add( new JLabel("Add open image:"), ctxt );

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 3;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD);
		gbc.anchor = GridBagConstraints.LINE_END;
		imagePlusDropdown = new JComboBox<>( new String[] { "<none>" } );
		panel.add( imagePlusDropdown, gbc );
		updateImagePlusDropdown();

		final GridBagConstraints cadd = new GridBagConstraints();
		cadd.gridx = 5;
		cadd.gridy = 1;
		cadd.gridwidth = 1;
		cadd.weightx = 0.0;
		cadd.fill = GridBagConstraints.NONE;
		cadd.anchor = GridBagConstraints.LINE_START;
		cadd.insets = new Insets(OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD);

		cadd.gridy = 1;
		addImageButton = new JButton("+");
		panel.add( addImageButton, cadd );
		addImageButton.addActionListener( e -> { addImage(); });

		ctxt.gridy = 2;
		final JLabel addFileLabel = new JLabel( "Add image file/folder:");
		panel.add(addFileLabel, ctxt);

		gbcBar.gridy = 2;
		containerPathText = new JTextField();
		containerPathText.setText( initialPath );
		containerPathText.setPreferredSize( new Dimension( frameSizeX / 3, containerPathText.getPreferredSize().height ) );
//		containerPathText.addActionListener( e -> openContainer( n5Fun, () -> getN5RootPath(), pathFun ) );
		panel.add(containerPathText, gbcBar);

		cadd.gridy = 2;
		addPathButton = new JButton("+");
		addPathButton.addActionListener( e -> addPath() );
		panel.add(addPathButton, cadd);

		final GridBagConstraints cbrowse = new GridBagConstraints();
		cbrowse.gridx = 6;
		cbrowse.gridy = 2;
		cbrowse.gridwidth = 1;
		cbrowse.weightx = 0.0;
		cbrowse.fill = GridBagConstraints.HORIZONTAL;
		cbrowse.insets = new Insets(OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD);
		browseBtn = new JButton("Browse");
		browseBtn.addActionListener( e -> { browseImageDialog(); } );
		panel.add(browseBtn, cbrowse);

		final GridBagConstraints cn5 = new GridBagConstraints();
		cn5.gridx = 7;
		cn5.gridy = 2;
		cn5.gridwidth = 1;
		cn5.weightx = 0.0;
		cn5.fill = GridBagConstraints.HORIZONTAL;
		cn5.insets = new Insets(OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD);
		addN5Button = new JButton( "H5/N5/Zarr" );
		panel.add( addN5Button, cn5 );

		addN5Button.addActionListener( e -> {

			selectionDialog = new DatasetSelectorDialog( new N5ViewerReaderFun(), new N5BasePathFun(),
					lastOpenedContainer,
					BigWarpInit.GROUP_PARSERS,
					BigWarpInit.PARSERS);

			selectionDialog.setLoaderExecutor( exec );
			selectionDialog.setTreeRenderer(new N5ViewerTreeCellRenderer(false));

			selectionDialog.setContainerPathUpdateCallback( x -> {
				if ( x != null )
					lastOpenedContainer = x;
			} );

			// figure this out
//			selectionDialog.setCancelCallback( x -> {
//				// set back recorder state if canceled
//				Recorder.record = initialRecorderState;
//			} );

			selectionDialog.setVirtualOption( false );
			selectionDialog.setCropOption( false );

			selectionDialog.run( this::n5DialogCallback );
		});

		// add transforms
		ctxt.gridy = 3;
		panel.add( new JLabel("Add transformation:"), ctxt );

		transformPathText = new JTextField();
		transformPathText.setPreferredSize( new Dimension( frameSizeX / 3, transformPathText.getPreferredSize().height ) );
		gbcBar.gridy = 3;
		panel.add( transformPathText, gbcBar );

		addTransformButton = new JButton( "+" );
		addTransformButton.addActionListener( e -> addTransform() );
		cadd.gridy = 3;
		panel.add( addTransformButton, cadd );

		browseTransformButton = new JButton("Browse");
		browseTransformButton.addActionListener( e -> { browseTransformDialog(); } );
		cbrowse.gridy = 3;
		panel.add( browseTransformButton, cbrowse );

		cn5.gridy = 3;
		addN5TransformButton = new JButton( "H5/N5/Zarr" );
		panel.add( addN5TransformButton, cn5 );

		addN5TransformButton.addActionListener( e -> {
			if (sourceTable.getSelectedRow() < 0)
				IJ.showMessage("Please highlight the row you would like to transform.");
			else
			{

				final N5MetadataParser<?>[] tformParsers = new N5MetadataParser<?>[]{ new N5TransformMetadataParser() };

				transformSelectionDialog = new DatasetSelectorDialog( new N5ViewerReaderFun(), new N5BasePathFun(),
						lastOpenedContainer, new N5MetadataParser[] {}, tformParsers );

				transformSelectionDialog.setLoaderExecutor( exec );
				transformSelectionDialog.setTreeRenderer( new N5TransformTreeCellRenderer( true ) );
				transformSelectionDialog.setContainerPathUpdateCallback( x -> {
					if ( x != null )
						lastOpenedContainer = x;
				} );

				transformSelectionDialog.run(this::n5DialogTransformCallback);

				// remove any existing selection listeners
				final JTree tree = transformSelectionDialog.getJTree();
				for ( final TreeSelectionListener l : tree.getTreeSelectionListeners())
					tree.removeTreeSelectionListener(l);

				// add a new listener for transform metadata
				tree.addTreeSelectionListener(new N5TransformTreeSelectionListener(tree.getSelectionModel()));
			}
		});

		// source list
		final GridBagConstraints clist = new GridBagConstraints();
		clist.gridx = 0;
		clist.gridy = 4;
		clist.gridwidth = 8;
		clist.gridheight = 3;
		clist.weightx = 1.0;
		clist.weighty = 1.0;
		clist.fill = GridBagConstraints.BOTH;
		clist.insets = new Insets(OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD);

		sourceTableModel = new BigWarpSourceTableModel( t -> {
			final String val = NgffTransformations.detectTransforms(t);
			if (val != null)
				showMessage(1000, "Found transformation");

			return val;
		});

        final BigWarpSourceListPanel srcListPanel = new BigWarpSourceListPanel( sourceTableModel );
        sourceTableModel.setContainer( srcListPanel );
        sourceTable = srcListPanel.getJTable();
        sourceTable.putClientProperty("terminateEditOnFocusLost", true);
        panel.add( srcListPanel, clist );

		// bottom button section
		final GridBagConstraints cbot = new GridBagConstraints();
		cbot.gridx = 0;
		cbot.gridy = 7;
		cbot.gridwidth = 4;
		cbot.gridheight = 1;
		cbot.weightx = 1.0;
		cbot.weighty = 0.0;
		cbot.fill = GridBagConstraints.HORIZONTAL;
		cbot.anchor = GridBagConstraints.WEST;
		cbot.insets = new Insets(OUTER_PAD, OUTER_PAD, OUTER_PAD, OUTER_PAD);

		messageLabel = new JLabel("");
		messageLabel.setVisible(true);
		panel.add(messageLabel, cbot);

		okBtn = new JButton("OK");
		okBtn.addActionListener( e -> okayCallback.accept( sourceTableModel ));
		cbot.gridx = 6;
		cbot.weightx = 0.0;
		cbot.gridwidth = 1;
		cbot.ipadx = (int)(40);
		cbot.fill = GridBagConstraints.HORIZONTAL;
		cbot.anchor = GridBagConstraints.EAST;
		cbot.insets = new Insets(MID_PAD, OUTER_PAD, OUTER_PAD, BUTTON_PAD);
		panel.add(okBtn, cbot);

		cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener( e -> cancelCallback.accept( sourceTableModel ));
		cbot.gridx = 7;
		cbot.ipadx = 0;
		cbot.gridwidth = 1;
		cbot.fill = GridBagConstraints.HORIZONTAL;
		cbot.anchor = GridBagConstraints.EAST;
		cbot.insets = new Insets(MID_PAD, BUTTON_PAD, OUTER_PAD, OUTER_PAD);
		panel.add(cancelBtn, cbot);

		return panel;
	}

	public void buildN5SelectionDialog()
	{
		exec = Executors.newFixedThreadPool( Prefs.getThreads() );


		/*
		 * The Dialogs need to be created anew by the action listener
		 */

//		selectionDialog = new DatasetSelectorDialog( new N5ViewerReaderFun(), new N5BasePathFun(),
//				lastOpenedContainer,
//				n5vGroupParsers,
//				n5Parsers);
//
//		selectionDialog.setLoaderExecutor( exec );
//		selectionDialog.setTreeRenderer(new N5ViewerTreeCellRenderer(false));
//
//		selectionDialog.setContainerPathUpdateCallback( x -> {
//			if ( x != null )
//				lastOpenedContainer = x;
//		} );
//
//		// figure this out
////		selectionDialog.setCancelCallback( x -> {
////			// set back recorder state if canceled
////			Recorder.record = initialRecorderState;
////		} );
//
//		selectionDialog.setVirtualOption( false );
//		selectionDialog.setCropOption( false );


//		// transform
//
//		final N5MetadataParser<?>[] tformParsers = new N5MetadataParser<?>[]{ new N5TransformMetadataParser() };
//
//		transformSelectionDialog = new DatasetSelectorDialog( new N5ViewerReaderFun(), new N5BasePathFun(),
//				lastOpenedContainer, new N5MetadataParser[] {}, tformParsers );
//
//		transformSelectionDialog.setLoaderExecutor( exec );
//		transformSelectionDialog.setTreeRenderer( new N5TransformTreeCellRenderer( true ) );
//		transformSelectionDialog.setContainerPathUpdateCallback( x -> {
//			if ( x != null )
//				lastOpenedContainer = x;
//		} );

	}

	public void n5DialogCallback( final DataSelection selection )
	{
		final String n5RootPath = selectionDialog.getN5RootPath();
		for( final N5Metadata m : selection.metadata )
			sourceTableModel.add( n5RootPath + "?" + m.getPath() );

		repaint();
	}

	public void n5DialogTransformCallback( final DataSelection selection )
	{
		final String n5RootPath = transformSelectionDialog.getN5RootPath();
		final int i = sourceTable.getSelectedRow();
		if( selection.metadata.size() > 0 )
			sourceTableModel.setTransform(i, n5RootPath + "?" + selection.metadata.get(0).getPath() );

		repaint();
	}

	protected void addImage()
	{
		if ( !imageJOpen && datasetService == null)
			return;

		final String title = (String)(imagePlusDropdown.getSelectedItem());
		if (!addDataset(title, false))
			addImagePlus(title);

		this.repaint();
	}

	protected boolean addDataset( final String datasetSource, final boolean moving )
	{
		if( datasetService.getDatasets().stream().filter( x -> x.getSource().equals(datasetSource)).count() > 0 )
		{
			sourceTableModel.addDataset( datasetSource, moving );
			return true;
		}
		else
			return false;
	}

	protected void addImagePlus( final String title )
	{
		addImagePlus( title, true );
	}

	protected void addImagePlus( final String title, final boolean moving )
	{
		if ( IJ.getInstance() == null )
			return;

		final ImagePlus imp = WindowManager.getImage( title );

		// TODO consider giving the user information if
		// an image is not added, and / or updating the dropdown menu periodically
		if( !title.isEmpty() && imp != null )
		{
			sourceTableModel.addImagePlus( ImageJPrefix + title, moving );
			repaint();
		}
	}

	protected void addPath()
	{
		final String path = containerPathText.getText();
		if( !path.isEmpty() )
		{
			sourceTableModel.add( path );
			repaint();
		}
	}

	protected void addTransform()
	{
		final String path = transformPathText.getText();
		final int row = sourceTable.getSelectedRow();
		if( !path.isEmpty() && row >= 0 )
		{
			sourceTableModel.setTransform( row, path );
			repaint();
		}
	}

	protected void updateImagePlusDropdown()
	{
		if( !imageJOpen && datasetService == null )
			return;

        // add both images from dataset service and ij1 window manager but avoid duplicates
		final ArrayList<String> titleList = new ArrayList<>();
		if( datasetService != null )
		{
			for( final Dataset d : datasetService.getDatasets() )
				titleList.add(d.getSource());
		}

		// don't need any open windows if we're using N5
		final int[] ids = WindowManager.getIDList();

		// Find any open images
		final int N = ids == null ? 0 : ids.length;
		for ( int i = 0; i < N; ++i )
		{
			final String t = ( WindowManager.getImage( ids[ i ] )).getTitle();
			if( !titleList.contains(t))
				titleList.add(t);
		}

        final String[] titles = new String[titleList.size()];
		for (int i = 0; i < titleList.size(); i++)
			titles[i] = titleList.get(i);

		imagePlusDropdown.setModel( new DefaultComboBoxModel<>( titles ));
	}

	/**
	 * Adds first two image plus images to the source list automatically.
	 */
	public void initializeImagePlusSources()
	{
		final int N = imagePlusDropdown.getModel().getSize();
		if ( N > 0 )
		{
			if( datasetService == null )
				addImagePlus( ( String ) imagePlusDropdown.getItemAt( 0 ), true );
			else
				addDataset( ( String ) imagePlusDropdown.getItemAt( 0 ), true );
		}

		if ( N > 1 )
		{
			if( datasetService == null )
				addImagePlus( ( String ) imagePlusDropdown.getItemAt( 1 ), false );
			else
				addDataset( ( String ) imagePlusDropdown.getItemAt( 1 ), false );
		}
	}

	public static BigWarpInitDialog createAndShow()
	{
		return createAndShow( null );
	}

	public static BigWarpInitDialog createAndShow( final DatasetService datasets )
	{
		// Create and set up the window.
		final BigWarpInitDialog frame = new BigWarpInitDialog( "BigWarp", datasets );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setVisible( true );
		return frame;
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

	public void setImagePathUpdateCallback( final Consumer< String > callback )
	{
		this.imagePathUpdateCallback = callback;
	}

	public void setTransformPathUpdateCallback( final Consumer< String > callback )
	{
		this.transformPathUpdateCallback = callback;
	}

	public void setProjectPathUpdateCallback( final Consumer< String > callback )
	{
		this.projectPathUpdateCallback = callback;
	}

	private String browseProjectDialog()
	{
		final String s = browseDialogGeneral( JFileChooser.FILES_ONLY, new FileNameExtensionFilter( "JSON file", "json" ) );
		projectPathTxt.setText( s );
		if ( projectPathUpdateCallback != null )
			projectPathUpdateCallback.accept( s );

		return s;
	}

	private String browseImageDialog()
	{
		final String s = browseDialogGeneral( JFileChooser.FILES_AND_DIRECTORIES, null );
		containerPathText.setText( s );
		if ( imagePathUpdateCallback != null )
			imagePathUpdateCallback.accept( s );

		return s;
	}

	private String browseTransformDialog()
	{
		final String s = browseDialogGeneral( JFileChooser.FILES_AND_DIRECTORIES, null );
		transformPathText.setText( s );
		if ( transformPathUpdateCallback != null )
			transformPathUpdateCallback.accept( s );

		return s;
	}

	public void setParameters( final String projectLandmarkPath, final String images, final String moving, final String transforms ) {
		this.projectLandmarkPath = projectLandmarkPath;
		this.imageList = images;
		this.movingList = moving;
		this.transformList = transforms;
	}

	public void updateTableFromParameters()
	{
		for( int i = 0; i < sourceTableModel.getRowCount(); i++ )
			sourceTableModel.remove( i );

		final String[] imageParams = imageList.split( listSeparator, -1 );
		final String[] movingParams = movingList.split( listSeparator, -1 );
		final String[] transformParams = transformList.split( listSeparator, -1 );

		final int N = imageParams.length;
		if( movingParams.length != N || transformParams.length != N )
		{
			System.err.println("Parameter arrays must have identical lengths");
			return;
		}

		for( int i = 0; i < N; i++ )
		{
			sourceTableModel.add( imageParams[ i ], movingParams[ i ].trim().equals( "true" ) );
			sourceTableModel.setTransform( i, transformParams[ i ] );
		}
	}

	public void updateParametersFromTable()
	{
		// make source list
		final StringBuffer imageList = new StringBuffer();
		final StringBuffer movingList = new StringBuffer();
		final StringBuffer transformList = new StringBuffer();

		final int N = sourceTable.getRowCount();
		for( int i = 0; i < N; i++ )
		{
			imageList.append( sourceTableModel.get( i ).srcName );
			movingList.append( sourceTableModel.get( i ).moving );
			transformList.append( sourceTableModel.get( i ).transformUrl );
			if( i < N -1 )
			{
				imageList.append( listSeparator );
				movingList.append( listSeparator );
				transformList.append( listSeparator );
			}
		}

		this.imageList = imageList.toString();
		this.movingList = movingList.toString();
		this.transformList = transformList.toString();
	}

	public String macroRecord()
	{
		if( !Recorder.record )
			return "";

		updateParametersFromTable();
//		return String.format( "images=[%s], moving=[%s], transformations=[%s]",
//				imageList.toString(), movingList.toString(), transformList.toString() );

		Recorder.resetCommandOptions();
		Recorder.recordOption(imagesKey, imageList.toString());
		Recorder.recordOption(movingKey, movingList.toString());

		if( transformList != null )
			Recorder.recordOption(transformsKey, transformList.toString());

		return Recorder.getCommandOptions();
	}

	protected void showMessage( int timeMillis, String message )
	{
		messageLabel.setText(message);
		final Timer timer = new Timer( timeMillis, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				messageLabel.setText("");
			}
		});
		timer.setRepeats(false);
		timer.start();
	}

	public static void runMacro( final String args )
	{
		final String project = Macro.getValue( args, projectKey, "" );

		final String[] images = Macro.getValue( args, imagesKey, "" ).split( ",", -1 );
		final String[] moving = Macro.getValue( args, movingKey, "" ).split( ",", -1 );
		final String[] transforms = Macro.getValue( args, transformsKey, "" ).split( ",", -1 );

		if( !project.isEmpty())
		{
			runBigWarp( project, null, null, null );
		}
		else
		{
			if( images.length == 0 || moving.length == 0 )
			{
				System.err.println( "images and moving keys required" );
				return;
			}
			// TODO fix transforms
			runBigWarp( null, images, moving, transforms );
		}

//		System.out.println( "BigWarpInitDialog runMacro");
//		System.out.println( args );
//		final HashMap< String, String > keyVals = BigWarpUtils.parseMacroArguments( args );
//		final Set< String > keys = keyVals.keySet();

//		if( keys.contains("project"))
//		{
//			runBigWarp( keyVals.get( "project" ), null, null, null );
//		}
//		else
//		{
//			if( !keys.contains( "images" ) || !keys.contains( "moving" ))
//			{
//				System.out.println( "images and moving keys required" );
//				return;
//			}
//
//			final String[] images = keyVals.get( "images" ).split( ",", -1 );
//			final String[] moving = keyVals.get( "moving" ).split( ",", -1 );
//
////			final Boolean[] moving = Arrays.stream( keyVals.get( "moving" ).split( ",", -1 ) ).map( x -> {
////						return x.equals( "true" );
////					} ).toArray( Boolean[]::new );
//
////			final String transforms;
////			if( keys.contains( "transforms" ) )
////				transforms = keyVals.get( "transforms" );
////			else
////				transforms = "";
//
//			// TOD fix transforms
//			runBigWarp( null, images, moving, null );
//		}
	}

	/**
	 * Removes selected nodes that do not have transformation metadata.
	 */
	public static class N5TransformTreeSelectionListener implements TreeSelectionListener {

		private TreeSelectionModel selectionModel;

		public N5TransformTreeSelectionListener(final TreeSelectionModel selectionModel) {

			this.selectionModel = selectionModel;
		}

		@Override
		public void valueChanged(final TreeSelectionEvent sel) {

			int i = 0;
			for (final TreePath path : sel.getPaths()) {
				if (!sel.isAddedPath(i))
					continue;

				final Object last = path.getLastPathComponent();
				if (last instanceof N5SwingTreeNode) {
					final N5SwingTreeNode node = ((N5SwingTreeNode)last);
					if (node.getMetadata() == null || !(node.getMetadata() instanceof N5TransformMetadata)) {
						selectionModel.removeSelectionPath(path);
					}
				}
				i++;
			}
		}
	}

}
