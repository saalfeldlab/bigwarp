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

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeSet;

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
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffSceneParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.scene.NgffScene;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.scene.NgffSceneMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.Common;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.graph.TransformGraph;

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
				n5 = new N5Factory()
						.options(o -> o.gsonBuilder(Common.gsonBuilder()))
						.openReader(n5uri.getContainerPath());

				graph = openGraph(n5, n5uri.getGroupPath());
			} catch(Exception e ) {
				System.err.println("TransformGraphPanel: could not read a transformation graph from \""
						+ transformGraphSourceText.getText() + "\": " + e);
				graph = null;
			}

			destCoordinateSystemsDropdown.removeAllItems();
			destCoordinateSystemsDropdown.addItem( DEFAULT_COORDINATE_SYSTEM );

			if( graph != null )
			{
				coordinateSystemNames( graph ).forEach( x -> {
					destCoordinateSystemsDropdown.addItem(x); });
			}

			// make sure default is selected after update
			destCoordinateSystemsDropdown.setSelectedIndex(0);
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

	/**
	 * Reads the transformation graph at {@code group}, preferring an NGFF scene
	 * ({@link NgffScene#SCENE_KEY}) when one is present there.
	 *
	 * @param n5
	 *            the reader
	 * @param group
	 *            the group to read a graph from
	 * @return the graph, never null
	 */
	private static TransformGraph openGraph( final N5Reader n5, final String group ) {

		// NgffScene resolves its dataset references against basePath by
		// concatenation, so "/" has to be normalized away first
		final String basePath = N5URI.normalizeGroupPath( group );

		final NgffScene scene = new OmeNgffSceneParser( n5 ).parseMetadata( n5, basePath )
				.map( NgffSceneMetadata::getScene ).orElse( null );

		if ( scene != null )
			return scene.getGraph( n5, basePath );

		return Common.openGraph( n5, group );
	}

	/**
	 * The names to offer as destination coordinate systems for {@code graph}.
	 * <p>
	 * Coordinate systems are identified by <b>name only</b>: a scene's graph nodes are
	 * path-qualified (e.g. {@code JRC2018F/JRC2018F}), but sources are named plainly
	 * (e.g. {@code JRC2018F}), so the path is dropped here and re-attached during lookup by
	 * {@link #resolveCoordinateSystem(TransformGraph, String)}.
	 *
	 * @param graph
	 *            the graph
	 * @return the coordinate system names, unqualified and sorted
	 */
	private static Collection< String > coordinateSystemNames( final TransformGraph graph ) {

		final TreeSet< String > names = new TreeSet<>();
		graph.getCoordinateSystems().coordinateSystems().forEach( cs -> names.add( nameOf( cs.getName() ) ) );

		// a graph built from transforms alone may register no coordinate systems
		if ( names.isEmpty() )
			graph.getTransforms().forEach( t -> {
				names.add( nameOf( t.getInput().getQualifiedName() ) );
				names.add( nameOf( t.getOutput().getQualifiedName() ) );
			} );

		return names;
	}

	/**
	 * The name part of a possibly path-qualified coordinate system name --
	 * {@code JRC2018F/JRC2018F} to {@code JRC2018F}.
	 *
	 * @param coordinateSystem
	 *            a (possibly qualified) coordinate system name
	 * @return its name part
	 */
	public static String nameOf( final String coordinateSystem ) {

		if (coordinateSystem == null)
			return null;

		final int i = coordinateSystem.lastIndexOf('/');
		return i < 0 ? coordinateSystem : coordinateSystem.substring(i + 1);
	}

	/**
	 * The node of {@code graph} identified by {@code name}, matching on the coordinate
	 * system's name only and ignoring path qualification.
	 * <p>
	 * {@link TransformGraph#addTransform} registers every edge under its endpoints'
	 * {@code getQualifiedName()}, and {@link TransformGraph#path(String, String)} looks
	 * them up the same way, so a bare name taken from a source or from this panel's
	 * dropdown has to be mapped back to the qualified node before it can resolve.
	 * <p>
	 * Matching by name alone is ambiguous when several datasets of one scene declare a
	 * coordinate system of the same name (an NGFF scene relating datasets that each call
	 * their own space {@code physical} is the usual case). The first match in sorted
	 * order is used, and the ambiguity is reported.
	 *
	 * @param graph
	 *            the graph to look in
	 * @param name
	 *            a coordinate system name, qualified or not
	 * @return the matching qualified node name, or null if the graph has no such node
	 */
	public static String resolveCoordinateSystem( final TransformGraph graph, final String name ) {

		if ( graph == null || name == null )
			return null;

		final TreeSet< String > matches = new TreeSet<>();
		graph.getCoordinateSystems().coordinateSystems().forEach( cs -> {
			if ( cs.getName().equals( name ) || nameOf( cs.getName() ).equals( nameOf( name ) ) )
				matches.add( cs.getName() );
		} );

		if ( matches.isEmpty() )
			return null;

		if ( matches.size() > 1 )
			System.err.println( "TransformGraphPanel: coordinate system \"" + name
					+ "\" is ambiguous, matching " + matches + "; using \"" + matches.first() + "\"" );

		return matches.first();
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
	}

}
