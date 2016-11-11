package bdv.viewer;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.scijava.ui.behaviour.util.InputActionBindings;

import bdv.gui.BigWarpLandmarkPanel;
import bigwarp.BigWarp;
import net.imglib2.ui.util.GuiUtil;

public class BigWarpLandmarkFrame extends JFrame {

	private static final long serialVersionUID = -5160678226566479257L;

	private final BigWarp bw;

	private BigWarpLandmarkPanel lmPanel;

	private final InputActionBindings keybindings;

	public BigWarpLandmarkFrame( String name, BigWarpLandmarkPanel panel, BigWarp bw )
	{
		super( name, GuiUtil.getSuitableGraphicsConfiguration( GuiUtil.RGB_COLOR_MODEL )  );
		this.bw = bw;
		setLandmarkPanel( panel );

		keybindings = new InputActionBindings();


		// do nothing because the closeAll method in bigWarp is responsible for calling dispose and cleaning up
		setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
		addWindowListener( new WindowAdapter()
		{
			public void windowClosing( final WindowEvent e )
			{
				BigWarpLandmarkFrame.this.bw.closeAll();
			}
		} );
		
		SwingUtilities.replaceUIActionMap( lmPanel.getJTable(), keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap(  lmPanel.getJTable(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );
	}
	
	public void setLandmarkPanel( BigWarpLandmarkPanel panel )
	{
		this.lmPanel = panel;
		setContentPane( lmPanel );
		pack();

	}

	public InputActionBindings getKeybindings()
	{
		return keybindings;
	}


}
