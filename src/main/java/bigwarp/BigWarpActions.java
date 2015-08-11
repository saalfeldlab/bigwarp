package bigwarp;

import java.awt.event.ActionEvent;

import javax.swing.ActionMap;
import javax.swing.InputMap;

import bdv.tools.ToggleDialogAction;
import bdv.util.AbstractNamedAction;
import bdv.util.AbstractNamedAction.NamedActionAdder;
import bdv.util.KeyProperties;
import bdv.util.KeyProperties.KeyStrokeAdder;
import bdv.viewer.InputActionBindings;

public class BigWarpActions
{
	public static final String TOGGLE_LANDMARK_MODE = "toggle landmark mode";
	
	public static final String BRIGHTNESS_SETTINGS_P = "brightness settings p";
	public static final String BRIGHTNESS_SETTINGS_Q = "brightness settings q";
	public static final String VISIBILITY_AND_GROUPING = "visibility and grouping";
	public static final String SHOW_HELP = "help";
	public static final String CROP = "crop";
	public static final String SAVE_SETTINGS = "save settings";
	public static final String LOAD_SETTINGS = "load settings";

	/**
	 * Create BigDataViewer actions and install them in the specified
	 * {@link InputActionBindings}.
	 *
	 * @param inputActionBindings
	 *            {@link InputMap} and {@link ActionMap} are installed here.
	 * @param bdv
	 *            Actions are targeted at this {@link BigDataViewer}.
	 * @param keyProperties
	 *            user-defined key-bindings.
	 */
	public static void installActionBindings(
			final InputActionBindings inputActionBindings,
			final BigWarp bdv,
			final KeyProperties keyProperties )
	{
		inputActionBindings.addActionMap( "bdv", createActionMap( bdv ) );
		inputActionBindings.addInputMap( "bdv", createInputMap( keyProperties ) );
	}

	public static InputMap createInputMap( final KeyProperties keyProperties )
	{
		final InputMap inputMap = new InputMap();
		final KeyStrokeAdder map = keyProperties.adder( inputMap );

		map.put( TOGGLE_LANDMARK_MODE, "SPACE" );
		map.put( BRIGHTNESS_SETTINGS_P, "S" );
		map.put( BRIGHTNESS_SETTINGS_Q, "D" );
		map.put( SHOW_HELP, "F1", "H" );

		return inputMap;
	}

	public static ActionMap createActionMap( final BigWarp bw )
	{
		final ActionMap actionMap = new ActionMap();
		final NamedActionAdder map = new NamedActionAdder( actionMap );

		map.put( new ToggleLandmarkModeAction( TOGGLE_LANDMARK_MODE, bw ));
		map.put( new ToggleDialogAction( BRIGHTNESS_SETTINGS_P, bw.brightnessDialogP ) );
		map.put( new ToggleDialogAction( BRIGHTNESS_SETTINGS_Q, bw.brightnessDialogQ ) );
		map.put( new ToggleDialogAction( SHOW_HELP, bw.helpDialog ) );

		return actionMap;
	}

	private BigWarpActions()
	{}
	
	public static class ToggleLandmarkModeAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 7370813069619338918L;
		
		private BigWarp bw;
		
		public ToggleLandmarkModeAction( final String name, final BigWarp bw )
		{
			super( name );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			bw.toggleInLandmarkMode( );
			
		}
		
	}
	
}
