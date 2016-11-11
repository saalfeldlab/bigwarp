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
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.BigWarpViewerSettings;
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

	private final BigWarp bw;

	private static final long serialVersionUID = -7630931733043185034L;

	public BigWarpViewerFrame(
			BigWarp bw,
			final int width, final int height,
			final List< SourceAndConverter< ? > > sources,
			final BigWarpViewerSettings viewerSettings,
			final CacheControl cache,
			final String title,
			final boolean isMoving,
			final int[] movingIndexList,
			final int[] targetIndexList )
	{
		this( bw, width, height, sources, viewerSettings, cache, ViewerOptions.options(), title, isMoving, movingIndexList, targetIndexList );
	}
	
	public BigWarpViewerFrame(
			BigWarp bw,
			final int width, final int height,
			final List< SourceAndConverter< ? > > sources,
			final BigWarpViewerSettings viewerSettings,
			final CacheControl cache,
			final ViewerOptions optional,
			final String title,
			final boolean isMoving,
			final int[] movingIndexList,
			final int[] targetIndexList )
	{
		super( title, GuiUtil.getSuitableGraphicsConfiguration( GuiUtil.RGB_COLOR_MODEL ) );
		this.bw = bw;
		viewer = new BigWarpViewerPanel( sources, viewerSettings, cache, optional.width( width / 2 ).height( height ), isMoving, movingIndexList, targetIndexList );

		// TODO this needs to change for multi-channel!
		if( !isMoving )
			viewer.getVisibilityAndGrouping().setCurrentSource( 1 );
		
		keybindings = new InputActionBindings();
		triggerbindings = new TriggerBehaviourBindings();

		getRootPane().setDoubleBuffered( true );
		setPreferredSize( new Dimension( width, height ) );
		add( viewer, BorderLayout.CENTER );
		
		pack();

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
	
	public InputActionBindings getKeybindings()
	{
		return keybindings;
	}
}
