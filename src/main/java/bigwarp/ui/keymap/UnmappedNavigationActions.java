package bigwarp.ui.keymap;

import java.util.stream.IntStream;

import org.scijava.ui.behaviour.KeyStrokeAdder.Factory;
import org.scijava.ui.behaviour.util.Actions;

import bdv.viewer.AbstractViewerPanel;
import bdv.viewer.AbstractViewerPanel.AlignPlane;
import bdv.viewer.NavigationActions;
import bdv.viewer.ViewerState;
import bigwarp.BigWarpActions;

public class UnmappedNavigationActions extends NavigationActions {

	public UnmappedNavigationActions(Factory keyConfig) {
		super(keyConfig);
	}

	public static void install( final Actions actions, boolean is2D )
	{
		installModeActions( actions );
		installSourceActions( actions );
		installTimeActions( actions );
		installAlignPlaneActions( actions, is2D );
	}

	public static void installModeActions( final Actions actions )
	{
		actions.runnableAction( () -> {}, TOGGLE_INTERPOLATION, BigWarpActions.NOT_MAPPED );
		actions.runnableAction(	() -> {}, TOGGLE_FUSED_MODE, BigWarpActions.NOT_MAPPED );
		actions.runnableAction(	() -> {}, TOGGLE_GROUPING, BigWarpActions.NOT_MAPPED );
	}

	public static void installTimeActions( final Actions actions )
	{
		actions.runnableAction( () -> {}, NEXT_TIMEPOINT, BigWarpActions.NOT_MAPPED );
		actions.runnableAction( () -> {}, PREVIOUS_TIMEPOINT, BigWarpActions.NOT_MAPPED );
	}

	public static void installSourceActions( final Actions actions )
	{
		final String[] numkeys = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };
		IntStream.range( 0, numkeys.length ).forEach( i -> {
			actions.runnableAction( () -> {},
					String.format( SET_CURRENT_SOURCE, i ),
					String.format( SET_CURRENT_SOURCE_KEYS_FORMAT, numkeys[ i ] ) );
			actions.runnableAction( () -> {},
					String.format( TOGGLE_SOURCE_VISIBILITY, i ),
					String.format( TOGGLE_SOURCE_VISIBILITY_KEYS_FORMAT, numkeys[ i ] ) );
		} );
	}

	public static void installAlignPlaneActions( final Actions actions, boolean is2D )
	{
		actions.runnableAction( () -> {}, ALIGN_XY_PLANE, BigWarpActions.NOT_MAPPED );
		if ( !is2D )
		{
			actions.runnableAction( () -> {}, ALIGN_ZY_PLANE, BigWarpActions.NOT_MAPPED );
			actions.runnableAction( () -> {}, ALIGN_XZ_PLANE, BigWarpActions.NOT_MAPPED );
		}
	}

}
