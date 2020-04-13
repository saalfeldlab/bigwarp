package bdv.gui;

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

import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.BehaviourTransformEventHandler;
import bdv.cache.CacheControl;
import bdv.tools.brightness.ConverterSetup;
import bdv.ui.BdvDefaultCards;
import bdv.ui.CardPanel;
import bdv.ui.splitpanel.SplitPanel;
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.BigWarpViewerSettings;
import bdv.viewer.ConverterSetups;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import bigwarp.BigWarp;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.ui.util.GuiUtil;

public class BigWarpViewerFrame extends JFrame
{

	protected final BigWarpViewerPanel viewer;
	
	private final InputActionBindings keybindings;

	private final TriggerBehaviourBindings triggerbindings;

	private final BigWarp<?> bw;

	private SplitPanel splitPanel;

	private CardPanel cards;

	private final ConverterSetups setups;

	private static final long serialVersionUID = -7630931733043185034L;

	public BigWarpViewerFrame(
			BigWarp<?> bw,
			final int width, final int height,
			final List< SourceAndConverter< ? > > sources,
			final List< ConverterSetup > converterSetups,
			final BigWarpViewerSettings viewerSettings,
			final CacheControl cache,
			final String title,
			final boolean isMoving,
			final int[] movingIndexList,
			final int[] targetIndexList )
	{
		this( bw, width, height, sources, converterSetups, viewerSettings, cache, BigWarpViewerOptions.options(), title, isMoving, movingIndexList, targetIndexList );
	}
	
	public BigWarpViewerFrame(
			BigWarp<?> bw,
			final int width, final int height,
			final List< SourceAndConverter< ? > > sources,
			final List< ConverterSetup > converterSetups,
			final BigWarpViewerSettings viewerSettings,
			final CacheControl cache,
			final BigWarpViewerOptions optional,
			final String title,
			final boolean isMoving,
			final int[] movingIndexList,
			final int[] targetIndexList )
	{
		super( title, GuiUtil.getSuitableGraphicsConfiguration( GuiUtil.RGB_COLOR_MODEL ) );
		this.bw = bw;
		viewer = new BigWarpViewerPanel( sources, viewerSettings, cache, optional.size( width / 2,  height ), isMoving, movingIndexList, targetIndexList );
		setups = new ConverterSetups( viewer.state() );
		setups.listeners().add( s -> viewer.requestRepaint() );

		if ( converterSetups.size() != sources.size() )
			System.err.println( "WARNING! Constructing BigWarp with converterSetups.size() that is not the same as sources.size()." );
		final int numSetups = Math.min( converterSetups.size(), sources.size() );
		for ( int i = 0; i < numSetups; ++i )
		{
			final SourceAndConverter< ? > source = sources.get( i );
			final ConverterSetup setup = converterSetups.get( i );
			if ( setup != null )
				setups.put( source, setup );
		}

		// TODO this needs to change for multi-channel!
		if( !isMoving )
			viewer.getVisibilityAndGrouping().setCurrentSource( 1 );
		
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

		SwingUtilities.replaceUIActionMap( getRootPane(), keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( getRootPane(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );

		final MouseAndKeyHandler mouseAndKeyHandler = new MouseAndKeyHandler();
		mouseAndKeyHandler.setInputMap( triggerbindings.getConcatenatedInputTriggerMap() );
		mouseAndKeyHandler.setBehaviourMap( triggerbindings.getConcatenatedBehaviourMap() );
		viewer.getDisplay().addHandler( mouseAndKeyHandler );

		final TransformEventHandler< ? > tfHandler = viewer.getDisplay().getTransformEventHandler();
		if ( tfHandler instanceof BehaviourTransformEventHandler )
			( ( BehaviourTransformEventHandler< ? > ) tfHandler ).install( triggerbindings );
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
}
