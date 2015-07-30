package bdv.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import net.imglib2.ui.util.GuiUtil;
import bdv.img.cache.Cache;
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.InputActionBindings;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerPanel.Options;

public class BigWarpViewerFrame extends JFrame
{

	protected final BigWarpViewerPanel viewerP;
	
	private final InputActionBindings keybindings;
	
	private static final long serialVersionUID = -7630931733043185034L;

	public BigWarpViewerFrame(
			final int width, final int height,
			final List< SourceAndConverter< ? > > sources,
			final int numTimePoints,
			final Cache cache,
			final String title,
			final boolean isMoving )
	{
		this( width, height, sources, numTimePoints, cache, ViewerPanel.options(), title, isMoving );
	}
	
	public BigWarpViewerFrame(
			final int width, final int height,
			final List< SourceAndConverter< ? > > sources,
			final int numTimePoints,
			final Cache cache,
			final Options optional,
			final String title,
			final boolean isMoving )
	{
		super( title, GuiUtil.getSuitableGraphicsConfiguration( GuiUtil.RGB_COLOR_MODEL ) );
			
		if( !isMoving )
		{
			ArrayList< SourceAndConverter< ? >> flippedList = new ArrayList< SourceAndConverter< ? >>();
			flippedList.add( sources.get( 1 ));
			flippedList.add( sources.get( 0 ));
			viewerP = new BigWarpViewerPanel( flippedList, numTimePoints, cache, optional.width( width / 2 ).height( height ), isMoving );
		}
		else
		{
			viewerP = new BigWarpViewerPanel( sources, numTimePoints, cache, optional.width( width / 2 ).height( height ), isMoving );
		}
		
		keybindings = new InputActionBindings();

		getRootPane().setDoubleBuffered( true );
		setPreferredSize( new Dimension( width, height ) );
		add( viewerP, BorderLayout.CENTER);
		
		pack();
		setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				viewerP.stop();
			}
		} );

		SwingUtilities.replaceUIActionMap( getRootPane(), keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( getRootPane(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );
	}
	
	public boolean isMoving()
	{
		return viewerP.getIsMoving();
	}
	
	public BigWarpViewerPanel getViewerPanelP()
	{
		return viewerP;
	}
	
	public InputActionBindings getKeybindings()
	{
		return keybindings;
	}
}
