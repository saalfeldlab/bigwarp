package bdv.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.ij.N5Importer.N5BasePathFun;
import org.janelia.saalfeldlab.n5.ij.N5Importer.N5ViewerReaderFun;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalDatasetMetadata;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.janelia.saalfeldlab.n5.ui.DatasetSelectorDialog;
import org.janelia.saalfeldlab.n5.ui.N5DatasetTreeCellRenderer;

import com.formdev.flatlaf.util.UIScale;

import bdv.gui.sourceList.BigWarpSourceListPanel;
import bdv.gui.sourceList.BigWarpSourceTableModel;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;


public class BigWarpInitDialog extends JFrame
{

	private static final long serialVersionUID = -2914972130819029899L;
	private JButton browseBtn;
	private JTextField containerPathText;
	
	private String initialPath;
	private JLabel messageLabel;
	private JButton okBtn;
	private JButton cancelBtn;
	private JPanel listPanel;
	private JTable sourceTable;
	private JButton addN5Button;
	private BigWarpSourceTableModel sourceTableModel;
	private JComboBox<String> imagePlusDropdown;
	private JButton addImageButton;
	private JButton addPathButton;
	private DatasetSelectorDialog selectionDialog;

	private String lastOpenedContainer = "";
	private ExecutorService exec;

	private static final int DEFAULT_OUTER_PAD = 8;
	private static final int DEFAULT_BUTTON_PAD = 3;
	private static final int DEFAULT_MID_PAD = 5;

	public BigWarpInitDialog( final String title )
	{
		super( title );
		initialPath = "";

		buildN5SelectionDialog();

        final Container content = getContentPane();
        content.add( createContent() );
        pack();

        initializeImagePlusSources();
	}

	public JPanel createContent()
	{
		final int OUTER_PAD = DEFAULT_OUTER_PAD;
		final int BUTTON_PAD = DEFAULT_BUTTON_PAD;
		final int MID_PAD = DEFAULT_MID_PAD;

		final int frameSizeX = getSize().width;

		final JPanel panel = new JPanel(false);
		panel.setLayout(new GridBagLayout());	

		containerPathText = new JTextField();
		containerPathText.setText( initialPath );
		containerPathText.setPreferredSize( new Dimension( frameSizeX / 3, containerPathText.getPreferredSize().height ) );
//		containerPathText.addActionListener( e -> openContainer( n5Fun, () -> getN5RootPath(), pathFun ) );

		final GridBagConstraints ctxt = new GridBagConstraints();
		ctxt.gridx = 0;
		ctxt.gridy = 0;
		ctxt.gridwidth = 1;
		ctxt.gridheight = 1;
		ctxt.weightx = 0.0;
		ctxt.weighty = 0.0;
		ctxt.fill = GridBagConstraints.NONE;
		ctxt.insets = new Insets(OUTER_PAD, OUTER_PAD, MID_PAD, BUTTON_PAD);
		
		final JLabel addFileLabel = new JLabel( "Add file/folder:");
		panel.add(addFileLabel, ctxt);
		
		ctxt.gridx = 1;
		ctxt.gridy = 0;
		ctxt.gridwidth = 4;
		ctxt.gridheight = 1;
		ctxt.weightx = 1.0;
		ctxt.fill = GridBagConstraints.HORIZONTAL;
		panel.add(containerPathText, ctxt);

		final GridBagConstraints cadd = new GridBagConstraints();
		cadd.gridx = 5;
		cadd.gridwidth = 1;
		cadd.weightx = 0.0;
		cadd.fill = GridBagConstraints.HORIZONTAL;
		cadd.insets = new Insets(OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD);
		addPathButton = new JButton("+");
		addPathButton.addActionListener( e -> addPath() );
		panel.add(addPathButton, cadd);

		final GridBagConstraints cbrowse = new GridBagConstraints();
		cbrowse.gridx = 6;
		cbrowse.gridwidth = 1;
		cbrowse.weightx = 0.0;
		cbrowse.anchor = GridBagConstraints.LINE_END;
		cbrowse.fill = GridBagConstraints.HORIZONTAL;
		cbrowse.insets = new Insets(OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD);
		browseBtn = new JButton("Browse");
		panel.add(browseBtn, cbrowse);
		
		// add image / n5
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.insets = new Insets(OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD);
		panel.add( new JLabel("Add open image"), gbc );
		
		gbc.gridx = 1;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.EAST;
		imagePlusDropdown = new JComboBox<>( new String[] { "<none>" } );
		panel.add( imagePlusDropdown, gbc );
		updateImagePlusDropdown();
		
		gbc.gridx = 3;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		addImageButton = new JButton("+");
		panel.add( addImageButton, gbc );
		addImageButton.addActionListener( e -> { addImagePlus(); });

		gbc.gridx = 6;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		addN5Button = new JButton( "H5/N5/Zarr" );
		panel.add( addN5Button, gbc );

		addN5Button.addActionListener( e -> {
			selectionDialog.run( this::n5DialogCallback );
		});

		// source list
		final GridBagConstraints clist = new GridBagConstraints();
		clist.gridx = 0;
		clist.gridy = 2;
		clist.gridwidth = 7;
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
		cbot.gridy = 5;
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
		cbot.gridx = 5;
		cbot.weightx = 0.0;
		cbot.gridwidth = 1;
		cbot.ipadx = (int)(40);
		cbot.fill = GridBagConstraints.HORIZONTAL;
		cbot.anchor = GridBagConstraints.EAST;
		cbot.insets = new Insets(MID_PAD, OUTER_PAD, OUTER_PAD, BUTTON_PAD);
		panel.add(okBtn, cbot);

		cancelBtn = new JButton("Cancel");
		cbot.gridx = 6;
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
		// without
		// multiscale
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
			sourceTableModel.add( m.getPath() );

		repaint();
	}

	protected void addImagePlus()
	{
		if ( IJ.getInstance() == null )
			return;

		final String title = (String)(imagePlusDropdown.getSelectedItem());
		addImagePlus( title );
	}

	protected void addImagePlus( String title )
	{
		if ( IJ.getInstance() == null )
			return;

		final ImagePlus imp = WindowManager.getImage( title );

		// TODO consider giving the user information if
		// an image is not added, and / or updating the dropdown menu periodically
		if( !title.isEmpty() && imp != null )
		{
			sourceTableModel.add( title );
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

	protected void updateImagePlusDropdown()
	{
		if( IJ.getInstance() == null )
			return;

		// don't need any open windows if we're using N5
        final int[] ids = WindowManager.getIDList();

        // Find any open images
        final int N = ids == null ? 0 : ids.length;

        final String[] titles = new String[ N ];
        for ( int i = 0; i < N; ++i )
        {
            titles[ i ] = ( WindowManager.getImage( ids[ i ] )).getTitle();
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
			addImagePlus( ( String ) imagePlusDropdown.getItemAt( 0 ) );

		if ( N > 1 )
			addImagePlus( ( String ) imagePlusDropdown.getItemAt( 1 ) );
	}

    private static void createAndShowGUI() {
        //Create and set up the window.
        BigWarpInitDialog frame = new BigWarpInitDialog("BigWarp");
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
 
        frame.setVisible(true);
    }

	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		IJ.openImage( "/groups/saalfeld/home/bogovicj/tmp/boatsBlur.tif" ).show();
		IJ.openImage( "/groups/saalfeld/home/bogovicj/tmp/boats.tif" ).show();

		createAndShowGUI();
	}

}
