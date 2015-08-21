package bdv.viewer;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import net.imglib2.ui.util.GuiUtil;
import bdv.gui.BigWarpLandmarkPanel;

public class BigWarpLandmarkFrame extends JFrame {

	private static final long serialVersionUID = -5160678226566479257L;

	 private final BigWarpLandmarkPanel lmPanel;

	private final InputActionBindings keybindings;

	public BigWarpLandmarkFrame( String name, BigWarpLandmarkPanel panel )
	{
		super( name, GuiUtil.getSuitableGraphicsConfiguration( GuiUtil.RGB_COLOR_MODEL )  );
		this.lmPanel = panel;

		keybindings = new InputActionBindings();

		
		setContentPane( lmPanel );
		pack();
		setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		
		SwingUtilities.replaceUIActionMap( getRootPane(), keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( getRootPane(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );
	}

	public InputActionBindings getKeybindings()
	{
		return keybindings;
	}


}
