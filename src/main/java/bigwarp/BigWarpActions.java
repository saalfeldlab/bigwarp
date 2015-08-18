package bigwarp;

import java.awt.event.ActionEvent;

import javax.swing.ActionMap;
import javax.swing.InputMap;

import mpicbg.models.AbstractModel;
import mpicbg.models.CoordinateTransform;
import bdv.gui.BigWarpViewerFrame;
import bdv.tools.ToggleDialogAction;
import bdv.util.AbstractNamedAction;
import bdv.util.AbstractNamedAction.NamedActionAdder;
import bdv.util.KeyProperties;
import bdv.util.KeyProperties.KeyStrokeAdder;
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.InputActionBindings;

public class BigWarpActions
{
	public static final String TOGGLE_LANDMARK_MODE = "toggle landmark mode";
	
	public static final String TOGGLE_WARPMAG_VIS = "toggle warp magnitude";
	public static final String TOGGLE_WARPMAG_VIS_P = "toggle warp magnitude p";
	public static final String TOGGLE_WARPMAG_VIS_Q = "toggle warp magnitude q";
	public static final String WARPMAG_BASE = "set warpmag base %s";
	
	public static final String BRIGHTNESS_SETTINGS = "brightness settings";
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

		map.put( VISIBILITY_AND_GROUPING, "F6" );
		map.put( TOGGLE_WARPMAG_VIS, "M" );
		map.put( TOGGLE_LANDMARK_MODE, "SPACE" );
		map.put( BRIGHTNESS_SETTINGS, "S" );
		map.put( SHOW_HELP, "F1", "H" );

		return inputMap;
	}

	public static ActionMap createActionMap( final BigWarp bw )
	{
		final ActionMap actionMap = new ActionMap();
		final NamedActionAdder map = new NamedActionAdder( actionMap );

		map.put( new ToggleDialogAction( VISIBILITY_AND_GROUPING, bw.activeSourcesDialog ) );
		map.put( new ToggleLandmarkModeAction( TOGGLE_LANDMARK_MODE, bw ));
		map.put( new ToggleWarpMagAction( TOGGLE_WARPMAG_VIS, bw ));
		map.put( new ToggleWarpMagAction( TOGGLE_WARPMAG_VIS_P, bw, bw.getViewerFrameP() ));
		map.put( new ToggleWarpMagAction( TOGGLE_WARPMAG_VIS_Q, bw, bw.getViewerFrameQ() ));
		map.put( new ToggleDialogAction( BRIGHTNESS_SETTINGS, bw.brightnessDialog ) );
		map.put( new ToggleDialogAction( SHOW_HELP, bw.helpDialog ) );
		
		
		for( int i = 0; i < bw.baseXfmList.length; i++ ){
			AbstractModel<?> xfm = bw.baseXfmList[ i ];
			map.put( new SetWarpMagBaseAction( String.format( WARPMAG_BASE, xfm.getClass().getName()), bw, i ));
		}
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
	
	public static class SetWarpMagBaseAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 7370813069619338918L;
		
		private BigWarp bw;
		private AbstractModel<?> baseXfm;
		
		public SetWarpMagBaseAction( final String name, final BigWarp bw, int i )
		{
			super( name );
			this.bw = bw;
			this.baseXfm = this.bw.baseXfmList[ i ];
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			bw.setWarpMagBaseline( this.baseXfm );
			
		}
	}
	
	public static class ToggleWarpMagAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 7370813069619338918L;
		
		private BigWarp bw;
		private BigWarpViewerFrame p;
		
		public ToggleWarpMagAction( final String name, final BigWarp bw )
		{
			this( name, bw, null );
		}
		
		public ToggleWarpMagAction( final String name, final BigWarp bw, BigWarpViewerFrame p )
		{
			super( name );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			bw.toggleWarpMagMode( p );
			
		}
	}
	
}
