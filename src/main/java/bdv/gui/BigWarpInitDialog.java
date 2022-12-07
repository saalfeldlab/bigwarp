package bdv.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.ij.N5Importer.N5BasePathFun;
import org.janelia.saalfeldlab.n5.ij.N5Importer.N5ViewerReaderFun;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalDatasetMetadata;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.janelia.saalfeldlab.n5.ui.DatasetSelectorDialog;
import org.janelia.saalfeldlab.n5.ui.N5DatasetTreeCellRenderer;
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
	private DatasetSelectorDialog selectionDialog;

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

	private String projectPath;
	private String imageList;
	private String movingList;
	private String transformList;

	private boolean initialRecorderState;

	public BigWarpInitDialog()
	{
	}

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
			System.out.println( "add image: " + p ); 
			addPath();
		};

		transformPathUpdateCallback = ( p ) -> { 
			System.out.println( "add transform: " + p ); 
			addTransform();
		};
	}

	public void setInitialRecorderState( boolean initialRecorderState )
	{
		this.initialRecorderState = initialRecorderState;
	}

	public static void main( String[] args ) throws IOException
	{
//		ImageJ ij2 = new ImageJ();
//		ij2.ui().showUI();

//		ImageJ ij = new ImageJ();
//
//		IJ.openImage( "/groups/saalfeld/home/bogovicj/tmp/boatsBlur.tif" ).show();
//		IJ.openImage( "/groups/saalfeld/home/bogovicj/tmp/boats.tif" ).show();
//
//		IJ.openImage( "/home/john/tmp/boats.tif" ).show();
//		IJ.openImage( "/home/john/tmp/boatsBlur.tif" ).show();
//
//		IJ.openImage( "/home/john/tmp/mri-stack.tif" ).show();
//		IJ.openImage( "/home/john/tmp/t1-head.tif" ).show();

		new ImageJ();
//		IJ.openImage( "/home/john/tmp/mri-stack.tif" ).show();
//		String macroOptions = "images=imagej://mri-stack.tif,imagej://mri-stack.tif moving=true,false transforms=,";
//		runMacro( macroOptions );

//		IJ.openImage( "/home/john/tmp/t1-head.tif" ).show();
		IJ.openImage( "/home/john/tmp/mri-stack.tif" ).show();
		createAndShow();
	}

	public static <T> void runBigWarp( String projectPath, String[] images, String[] moving, String[] transforms )
	{
		final BigWarpData< T > data = BigWarpInit.initData();
		final boolean haveProject = projectPath != null && !projectPath.isEmpty();

		if( !haveProject )
		{
			int id = 0;
			final int N = images.length;
			for( int i = 0; i < N; i++ )
			{
				// TODO deal with exceptions?
				try
				{
					final LinkedHashMap< Source< T >, SourceInfo > infos = BigWarpInit.createSources( data, images[ i ], id, moving[ i ].equals( "true" ) );

					final String transformUrl = transforms[ i ];
					RealTransform transform = null;
					if( transformUrl!= null && !transformUrl.isEmpty() )
						transform = NgffTransformations.open( transformUrl );

					if( transform != null )
						BigWarpInit.add( data, infos, transform );
					else
						BigWarpInit.add( data, infos );

					id += infos.size();
				}
				catch ( URISyntaxException e )
				{
					e.printStackTrace();
				}
				catch ( IOException e )
				{
					e.printStackTrace();
				}
				catch ( SpimDataException e )
				{
					e.printStackTrace();
				}
			}
		}

		BigWarp<?> bw;
		try
		{
			data.applyTransformations();
			bw = new BigWarp<>( data, new ProgressWriterIJ() );
			if( haveProject )
				bw.loadSettings( projectPath );
		}
		catch ( SpimDataException e )
		{
			e.printStackTrace();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		catch ( JDOMException e )
		{
			e.printStackTrace();
		}

	}

	public <T> void runBigWarp()
	{
		if (Recorder.record)
		{
			Recorder.setCommand(commandName);
			macroRecord();
			Recorder.saveCommand();
		}

		final BigWarpData< T > data = BigWarpInit.initData();
		final boolean haveProject = projectPath != null && !projectPath.isEmpty();

		if( !haveProject )
		{
			int id = 0;
			final int N = sourceTable.getRowCount();
			for( int i = 0; i < N; i++ )
			{
				SourceRow tableRow = sourceTableModel.get( i );
				if( tableRow.type.equals( SourceType.IMAGEPLUS )  )
				{
					// strip off prefix if present
					final ImagePlus imp = WindowManager.getImage( tableRow.srcName.replaceAll( "^"+ImageJPrefix, "" ) );
					LinkedHashMap< Source< T >, SourceInfo > infos = BigWarpInit.createSources( data, imp, id, 0, tableRow.moving );
					BigWarpInit.add( data, infos, tableRow.getTransform() );
					id += infos.size();
				}
				else if( tableRow.type.equals( SourceType.DATASET ))
				{
					Dataset dataset = datasetService.getDatasets().stream()
							.filter( x -> x.getSource().equals( tableRow.srcName ) )
							.findFirst().get();
					LinkedHashMap< Source< T >, SourceInfo > infos = BigWarpInit.createSources( data, dataset, id, tableRow.moving );
					BigWarpInit.add( data, infos, tableRow.getTransform() );
					id += infos.size();
				}
				else
				{
					// TODO deal with exceptions
					try
					{
						LinkedHashMap< Source< T >, SourceInfo > infos = BigWarpInit.createSources( data, tableRow.srcName, id, tableRow.moving );
						BigWarpInit.add( data, infos, tableRow.getTransform() );
						id += infos.size();
					}
					catch ( URISyntaxException e )
					{
						e.printStackTrace();
					}
					catch ( IOException e )
					{
						e.printStackTrace();
					}
					catch ( SpimDataException e )
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
				bw.loadSettings( projectPath );
		}
		catch ( SpimDataException e )
		{
			e.printStackTrace();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		catch ( JDOMException e )
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
		panel.add( new JLabel("Open BigWarp project:"), ctxt );

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
			selectionDialog.run( this::n5DialogCallback );
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

		sourceTableModel = new BigWarpSourceTableModel();
        final BigWarpSourceListPanel srcListPanel = new BigWarpSourceListPanel( sourceTableModel );
        sourceTableModel.setContainer( srcListPanel );
        sourceTable = srcListPanel.getJTable();
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

		selectionDialog = new DatasetSelectorDialog( new N5ViewerReaderFun(), new N5BasePathFun(), 
				lastOpenedContainer, new N5MetadataParser[] {}, // no
				N5Importer.PARSERS );

		selectionDialog.setLoaderExecutor( exec );
		selectionDialog.setTreeRenderer( new N5DatasetTreeCellRenderer( true ) );

		// restrict canonical metadata to those with spatial metadata, but
		// without multiscale
		selectionDialog.getTranslationPanel().setFilter( x -> ( x instanceof CanonicalDatasetMetadata ) );

		selectionDialog.setContainerPathUpdateCallback( x -> {
			if ( x != null )
				lastOpenedContainer = x;
		} );

		// figure this out
//		selectionDialog.setCancelCallback( x -> {
//			// set back recorder state if canceled
//			Recorder.record = initialRecorderState;
//		} );

		selectionDialog.setVirtualOption( true );
		selectionDialog.setCropOption( true );
	}

	public void n5DialogCallback( DataSelection selection )
	{
		final String n5RootPath = selectionDialog.getN5RootPath();
		for( N5Metadata m : selection.metadata )
			sourceTableModel.add( n5RootPath + "?" + m.getPath() );

		repaint();
	}

	protected void addImage()
	{
		if ( !imageJOpen && datasetService == null)
			return;

		if( datasetService != null )
		{
			final String title = (String)(imagePlusDropdown.getSelectedItem());
			addDataset( title, false );
		}
		else
		{
			final String title = (String)(imagePlusDropdown.getSelectedItem());
			addImagePlus( title );
		}
		this.repaint();
	}

	protected void addDataset( String datasetSource, boolean moving )
	{
		sourceTableModel.addDataset( datasetSource, moving );
	}

	protected void addImagePlus( String title )
	{
		addImagePlus( title, true );
	}

	protected void addImagePlus( String title, boolean moving )
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
		int row = sourceTable.getSelectedRow();
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

        final String[] titles;
		if( datasetService != null )
		{
			int i = 0;
			titles = new String[ datasetService.getDatasets().size() ];
			for( Dataset d : datasetService.getDatasets() )
				titles[i++] = d.getSource();
		}
		else
		{
			// don't need any open windows if we're using N5
			final int[] ids = WindowManager.getIDList();

			// Find any open images
			final int N = ids == null ? 0 : ids.length;

			titles = new String[ N ];
			for ( int i = 0; i < N; ++i )
			{
				titles[ i ] = ( WindowManager.getImage( ids[ i ] )).getTitle();
			}
		}

		imagePlusDropdown.setModel( new DefaultComboBoxModel<>( titles ));
	}

	/**
	 * Adds first two image plus images to the sourc list automatically.
	 *
	 * Make sure to call {@link updateImagePlusDropdown} before calling this
	 * method.
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

	public void fillTableFromProject()
	{
		// TODO implement me
		System.out.println( "implement me" );
	}

	public static BigWarpInitDialog createAndShow()
	{
		return createAndShow( null );
	}

	public static BigWarpInitDialog createAndShow( DatasetService datasets )
	{
		// Create and set up the window.
		BigWarpInitDialog frame = new BigWarpInitDialog( "BigWarp", datasets );
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

	public void setParameters( String projectPath, String images, String moving, String transforms ) {
		this.projectPath = projectPath;
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

	public static void runMacro( String args )
	{
		String project = Macro.getValue( args, projectKey, "" );

		String[] images = Macro.getValue( args, imagesKey, "" ).split( ",", -1 );
		String[] moving = Macro.getValue( args, movingKey, "" ).split( ",", -1 );
		String[] transforms = Macro.getValue( args, transformsKey, "" ).split( ",", -1 );
		
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
			runBigWarp( null, images, moving, null );
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

}
