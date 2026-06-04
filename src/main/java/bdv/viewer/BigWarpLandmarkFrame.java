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
package bdv.viewer;

import bdv.util.AWTUtils;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.gui.BigWarpLandmarkPanel;
import bigwarp.BigWarp;
import bigwarp.ui.keymap.KeymapManager;

public class BigWarpLandmarkFrame extends JFrame {

	private static final long serialVersionUID = -5160678226566479257L;

	private final BigWarp<?> bw;

	private BigWarpLandmarkPanel lmPanel;

	private final KeymapManager keymapManager;

	private final InputActionBindings keybindings;

	private final TriggerBehaviourBindings triggerbindings;

	public BigWarpLandmarkFrame( String name, BigWarpLandmarkPanel panel, BigWarp<?> bw, KeymapManager keymapManager )
	{
		super( name, AWTUtils.getSuitableGraphicsConfiguration( AWTUtils.RGB_COLOR_MODEL )  );
		this.bw = bw;
		this.keymapManager = keymapManager;
		setLandmarkPanel( panel );

		keybindings = new InputActionBindings();
		triggerbindings = new TriggerBehaviourBindings();

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
		SwingUtilities.replaceUIInputMap( lmPanel.getJTable(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );
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
