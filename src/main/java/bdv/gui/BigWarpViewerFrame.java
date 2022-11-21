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
package bdv.gui;

import bdv.TransformEventHandler;
import bdv.util.AWTUtils;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.scijava.ui.behaviour.InputTriggerMap;
import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.cache.CacheControl;
import bdv.ui.BdvDefaultCards;
import bdv.ui.CardPanel;
import bdv.ui.appearance.AppearanceManager;
import bdv.ui.splitpanel.SplitPanel;
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.BigWarpViewerSettings;
import bdv.viewer.ConverterSetups;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import bigwarp.ui.keymap.KeymapManager;

public class BigWarpViewerFrame extends JFrame
{

	protected final BigWarpViewerPanel viewer;
	
	private final InputActionBindings keybindings;

	private final TriggerBehaviourBindings triggerbindings;

	private final BigWarp<?> bw;

	private SplitPanel splitPanel;

	private CardPanel cards;

	private final Behaviours transformBehaviours;

	private final ConverterSetups setups;

	private final KeymapManager keymapManager;

	private final AppearanceManager appearanceManager;

	private static final long serialVersionUID = -7630931733043185034L;

	public BigWarpViewerFrame(
			BigWarp<?> bw,
			final int width, final int height,
			final List< SourceAndConverter< ? > > sources,
			final BigWarpViewerSettings viewerSettings,
			final CacheControl cache,
			final String title,
			final boolean isMoving,
			final int[] movingIndexList,
			final int[] targetIndexList )
	{
		this( bw, width, height, sources, viewerSettings, cache, 
				new KeymapManager( BigWarp.configDir ),
				new AppearanceManager( BigWarp.configDir ),
				BigWarpViewerOptions.options(), title, isMoving, movingIndexList, targetIndexList );
	}
	
	public BigWarpViewerFrame(
			BigWarp<?> bw,
			final int width, final int height,
			final List< SourceAndConverter< ? > > sources,
			final BigWarpViewerSettings viewerSettings,
			final CacheControl cache,
			final KeymapManager keymapManager,
			final AppearanceManager appearanceManager,
			final BigWarpViewerOptions optional,
			final String title,
			final boolean isMoving,
			final int[] movingIndexList,
			final int[] targetIndexList )
	{
		super( title, AWTUtils.getSuitableGraphicsConfiguration( AWTUtils.RGB_COLOR_MODEL ) );
		this.bw = bw;
		this.keymapManager = keymapManager;
		this.appearanceManager = appearanceManager;

		viewer = new BigWarpViewerPanel( sources, viewerSettings, cache, optional.size( width / 2,  height ), isMoving, movingIndexList, targetIndexList );
		setups = new ConverterSetups( viewer.state() );
		setups.listeners().add( s -> viewer.requestRepaint() );

//		if ( converterSetups.size() != sources.size() )
//			System.err.println( "WARNING! Constructing BigWarp with converterSetups.size() that is not the same as sources.size()." );
//
//		final int numSetups = Math.min( converterSetups.size(), sources.size() );
//		for ( int i = 0; i < numSetups; ++i )
//		{
//			final SourceAndConverter< ? > source = sources.get( i );
//			final ConverterSetup setup = converterSetups.get( i );
//			if ( setup != null )
//				setups.put( source, setup );
//		}

		if ( !isMoving )
		{
			viewer.state().setCurrentSource( viewer.state().getSources().get( bw.getData().targetSourceIndices[ 0 ] ));
		}

		keybindings = new InputActionBindings();
		triggerbindings = new TriggerBehaviourBindings();

		cards = new CardPanel();
		BdvDefaultCards.setup( cards, viewer, setups );
		splitPanel = new SplitPanel( viewer, cards );

		getRootPane().setDoubleBuffered( true );
		add( splitPanel, BorderLayout.CENTER );
		pack();

		setPreferredSize( new Dimension( width, height ) );

		setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
		addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				BigWarpViewerFrame.this.bw.closeAll();
			}
		} );

		// repaint on table selection change so rendering of selected points is updated
		bw.getLandmarkPanel().getJTable().getSelectionModel().addListSelectionListener( new ListSelectionListener()
		{
			@Override
			public void valueChanged( ListSelectionEvent e )
			{
				viewer.requestRepaint();
			}
		});

		SwingUtilities.replaceUIActionMap( viewer, keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( viewer, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );

		final MouseAndKeyHandler mouseAndKeyHandler = new MouseAndKeyHandler();
		mouseAndKeyHandler.setInputMap( triggerbindings.getConcatenatedInputTriggerMap() );
		mouseAndKeyHandler.setBehaviourMap( triggerbindings.getConcatenatedBehaviourMap() );
		mouseAndKeyHandler.setKeypressManager( optional.values.getKeyPressedManager(), viewer.getDisplayComponent() );
		viewer.getDisplay().addHandler( mouseAndKeyHandler );

		transformBehaviours = new Behaviours( optional.values.getInputTriggerConfig(), "bigwarp", "navigation" );
		transformBehaviours.install( triggerbindings, "transform" );

		final TransformEventHandler tfHandler = viewer.getTransformEventHandler();
		tfHandler.install( transformBehaviours );
	}

	public boolean isMoving()
	{
		return viewer.getIsMoving();
	}

	public BigWarpViewerPanel getViewerPanel()
	{
		return viewer;
	}

	public CardPanel getCardPanel()
	{
		return cards;
	}

	public SplitPanel getSplitPanel()
	{
		return splitPanel;
	}

	public ConverterSetups getConverterSetups()
	{
		return setups;
	}

	public void expandAndFocusCardPanel()
	{
		getSplitPanel().setCollapsed( false );
		getSplitPanel().getRightComponent().requestFocusInWindow();
	}

	public void collapseCardPanel()
	{
		getSplitPanel().setCollapsed( true );
		viewer.requestFocusInWindow();
	}
	
	public InputActionBindings getKeybindings()
	{
		return keybindings;
	}

	public void setTransformEnabled( final boolean enabled )
	{
		viewer.setTransformEnabled( enabled );
		if ( enabled )
			triggerbindings.removeInputTriggerMap( "block_transform" );
		else
			triggerbindings.addInputTriggerMap( "block_transform", new InputTriggerMap(), "transform" );
	}

	/**
	 * Get {@code Behaviours} hook where TransformEventHandler behaviours are installed.
	 * This is installed in {@link #getTriggerbindings} with the id "transform".
	 * The hook can be used to update the keymap and install additional behaviours.
	 */
	public Behaviours getTransformBehaviours()
	{
		return transformBehaviours;
	}

}
