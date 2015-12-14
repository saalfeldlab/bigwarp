package bdv.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import net.imglib2.ui.util.GuiUtil;
import bdv.img.cache.Cache;
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.BigWarpViewerSettings;
import bdv.viewer.InputActionBindings;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import bigwarp.BigWarp;

public class BigWarpViewerFrame extends JFrame
{

	protected final BigWarpViewerPanel viewer;
	
	private final InputActionBindings keybindings;
	
	private final BigWarp bw;

	private static final long serialVersionUID = -7630931733043185034L;

	public BigWarpViewerFrame(
			BigWarp bw,
			final int width, final int height,
			final List< SourceAndConverter< ? > > sources,
			final BigWarpViewerSettings viewerSettings,
			final Cache cache,
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
			final Cache cache,
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

		SwingUtilities.replaceUIActionMap( getRootPane(), keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( getRootPane(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );
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
