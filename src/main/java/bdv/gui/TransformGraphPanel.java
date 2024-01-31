package bdv.gui;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.Common;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.TransformGraph;

import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerStateChange;
import bdv.viewer.ViewerStateChangeListener;
import bigwarp.BigWarp;

public class TransformGraphPanel extends JPanel implements ViewerStateChangeListener
{
	public static final String DEFAULT_COORDINATE_SYSTEM = "<DEFAULT>";

	private static final long serialVersionUID = 3848019442560524183L;
	
	private static final String TRANSFORM_SOURCE_HELP_TEXT = "Select a container containing transformations.";

	private static final String COORD_SYSTEMS_HELP_TEXT = "All available coordinate systems.";

	private static final ViewerStateChange CURRENT_SOURCE_CHANGED = null;

	private final BigWarp< ? > bw;

	private final ImprovedFormattedTextField transformGraphSourceText;
	
//	private final JLabel srcCoordinateSystemLabel;
//
//	private final JComboBox<String> srcCoordinateSystemsDropdown;

	private final JLabel destCoordinateSystemLabel;

	private final JComboBox<String> destCoordinateSystemsDropdown;

	private N5Reader n5;

	private TransformGraph graph;
	
	private boolean active;

	private final ViewerPanel viewerPanel;

	private HashMap<SourceAndConverter<?>, String> sourceToCoordinateSystems;

	public TransformGraphPanel( BigWarp<?> bw, final ViewerPanel viewerPanel, final Container content )
	{
		super( new GridBagLayout() );
		this.bw = bw;
		active = true;
		this.viewerPanel = viewerPanel;

		setBorder( BorderFactory.createCompoundBorder(
		BorderFactory.createEmptyBorder(4, 2, 4, 2),
		BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(
						BorderFactory.createEtchedBorder(),
						"Transformation database" ),
				BorderFactory.createEmptyBorder( 2, 2, 2, 2 ))));

//		autoDetectTransforms = new JCheckBox("Auto-detect transformations");
//		srcCoordinateSystemsDropdown = new JComboBox<>( new String[] { DEFAULT_COORDINATE_SYSTEM });
		destCoordinateSystemsDropdown = new JComboBox<>( new String[] { DEFAULT_COORDINATE_SYSTEM });

//		sourceToCoordinateSystems = new HashMap<>();
//		initializeSourceCoordinateSystems();

		// browse and directory
		final JLabel srcDirLabel = new JLabel("Directory");
		final File startingFolder = bw.getBigwarpSettingsFolder();
		transformGraphSourceText = new ImprovedFormattedTextField(new MessageFormat("{0}"));
		transformGraphSourceText.setCallback( () -> {

			active = false;
			try { 

				final N5URI n5uri = new N5URI(transformGraphSourceText.getText());
				n5 = new N5Factory().gsonBuilder(Common.gsonBuilder()).openReader(n5uri.getContainerPath());
				graph = Common.openGraph(n5, n5uri.getGroupPath());
			} catch(Exception e ) {
				graph = null;
			}

			if( graph != null )
			{
				destCoordinateSystemsDropdown.removeAllItems();
				HashSet<String> coordinateSystems = new HashSet<>();
				graph.getTransforms().stream().forEach( t -> {
					coordinateSystems.add( t.getInput() );
					coordinateSystems.add( t.getOutput() );
				});

				destCoordinateSystemsDropdown.addItem( DEFAULT_COORDINATE_SYSTEM );
				coordinateSystems.forEach( x -> { 
					destCoordinateSystemsDropdown.addItem(x); });

				// make sure default is selected after update
				destCoordinateSystemsDropdown.setSelectedIndex(0);
			}
			active = true;
		});

		final JButton browseBtn = new JButton( "Browse" );
		browseBtn.addActionListener( e -> {

			final JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
			fileChooser.setCurrentDirectory( startingFolder );

			final int ret = fileChooser.showOpenDialog( content );
			if ( ret == JFileChooser.APPROVE_OPTION )
			{
				final File folder = fileChooser.getSelectedFile();
				transformGraphSourceText.setText( folder.getAbsolutePath() );
//				N5URI n5uri;
//				try {
//					n5uri = new N5URI(folder.getAbsolutePath());
//					n5 = new N5Factory().openReader( n5uri.getContainerPath() );
//					graph = Common.openGraph(n5, n5uri.getGroupPath() );
//				} catch (Exception e1) { }
			}
		});

		// coordinate system
		destCoordinateSystemLabel = new JLabel( "Coordinate system");
		destCoordinateSystemLabel.setToolTipText( TRANSFORM_SOURCE_HELP_TEXT );
		destCoordinateSystemsDropdown.setToolTipText( COORD_SYSTEMS_HELP_TEXT );
		destCoordinateSystemsDropdown.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				if( active ) {
					final String cs = (String)destCoordinateSystemsDropdown.getSelectedItem();
					System.out.println( "coordinateSystem: " + cs );
					bw.transformationsFromCoordinateSystem();
				}
			}
		});

		// TODO add src coordinate system stuff when its ready

//		srcCoordinateSystemLabel = new JLabel("Source coordinate system");
//		srcCoordinateSystemLabel.setToolTipText( TRANSFORM_SOURCE_HELP_TEXT );
//		srcCoordinateSystemsDropdown.setToolTipText( COORD_SYSTEMS_HELP_TEXT );
//		srcCoordinateSystemsDropdown.addActionListener( new ActionListener() {
//			@Override
//			public void actionPerformed( ActionEvent e ) {
//				if( active ) {
//					final String cs = (String)srcCoordinateSystemsDropdown.getSelectedItem();
//					System.out.println( "src coordinateSystem: " + cs );
//					updateSourceCoordinateSystemMap();
////					bw.transformationsFromCoordinateSystem();
//				}
//			}
//		});

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(5, 5, 5, 5);

//		add( autoDetectTransforms, gbc );
		add(srcDirLabel, gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(transformGraphSourceText, gbc);

		gbc.gridx = 2;
		gbc.weightx = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		add(browseBtn, gbc);

//		gbc.gridx = 0;
//		gbc.gridy = 1;
//		add(srcCoordinateSystemLabel, gbc);
//		gbc.gridx = 1;
//		add(srcCoordinateSystemsDropdown, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		add(destCoordinateSystemLabel, gbc);
		gbc.gridx = 1;
		add(destCoordinateSystemsDropdown, gbc);
	}

	public TransformGraph getGraph() {

		return graph;
	}

	public N5Reader getN5() {

		return n5;
	}

	public JComboBox<String> getDestinationCoordinateSystemsDropdown() {

		return destCoordinateSystemsDropdown;
	}

	public JTextField getTransformSource() {

		return transformGraphSourceText;
	}


	public void setDestinationCoordinateSystem(final String coordinateSystem) {

		destCoordinateSystemsDropdown.setSelectedItem(coordinateSystem);
	}

	public String getCoordinateSystem() {

		return (String)destCoordinateSystemsDropdown.getSelectedItem();
	}

	/**
	 * After calling deactivate, updates to this panel won't affect Bigwarp.
	 */
	public void deactivate() {

		active = false;
	}

	/**
	 * After calling activate, updates to this panel will affect Bigwarp.
	 */
	public void activate() {

		active = true;
	}
	
	public void initializeSourceCoordinateSystems() {

		sourceToCoordinateSystems.clear();
//		bw.getData().sources.forEach(sac -> {
//			sourceToCoordinateSystems.put(sac, sac.getSpimSource().getName());
//			srcCoordinateSystemsDropdown.addItem(sac.getSpimSource().getName());
//		});
	}
	
	public void updateSourceDropdownOnSourceChange() {

//		srcCoordinateSystemsDropdown.setSelectedItem(
//				sourceToCoordinateSystems.get(viewerPanel.state().getCurrentSource()));
	}

	public void updateSourceCoordinateSystemMap() {

		sourceToCoordinateSystems.put(viewerPanel.state().getCurrentSource(), COORD_SYSTEMS_HELP_TEXT);
	}

	@Override
	public void viewerStateChanged(ViewerStateChange change) {

		// TODO this will be important when the UI is finished, for now, a no-op

//		switch( change )
//		{
//		case CURRENT_SOURCE_CHANGED:
//			SwingUtilities.invokeLater( this::updateSourceDropdownOnSourceChange );
//			break;
//		}
	}

}
