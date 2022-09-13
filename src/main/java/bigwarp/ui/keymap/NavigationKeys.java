package bigwarp.ui.keymap;

import java.util.stream.IntStream;

import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.KeyStrokeAdder.Factory;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;

import bdv.viewer.NavigationActions;
import bigwarp.BigWarpActions;

public class NavigationKeys extends NavigationActions
{

	public NavigationKeys( Factory keyConfig )
	{
		super( keyConfig );
	}

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( BigWarpActions.BIGWARP, BigWarpActions.BIGWARP_CTXT );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( TOGGLE_INTERPOLATION, TOGGLE_INTERPOLATION_KEYS, "Switch between nearest-neighbor and n-linear interpolation mode in BigDataViewer." );
			descriptions.add( TOGGLE_FUSED_MODE, TOGGLE_FUSED_MODE_KEYS, "TODO" );
			descriptions.add( TOGGLE_GROUPING, TOGGLE_GROUPING_KEYS, "TODO" );

			final String[] numkeys = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };
			IntStream.range( 0, numkeys.length ).forEach( i -> {
				descriptions.add( String.format( SET_CURRENT_SOURCE, i ), new String[] { String.format( SET_CURRENT_SOURCE_KEYS_FORMAT, numkeys[ i ] ) }, "TODO" );
				descriptions.add( String.format( TOGGLE_SOURCE_VISIBILITY, i ), new String[] { String.format( TOGGLE_SOURCE_VISIBILITY_KEYS_FORMAT, numkeys[ i ] ) }, "TODO" );
			} );

			descriptions.add( NEXT_TIMEPOINT, NEXT_TIMEPOINT_KEYS, "TODO" );
			descriptions.add( PREVIOUS_TIMEPOINT, PREVIOUS_TIMEPOINT_KEYS, "TODO" );
			descriptions.add( ALIGN_XY_PLANE, ALIGN_XY_PLANE_KEYS, "TODO" );
			descriptions.add( ALIGN_ZY_PLANE, ALIGN_ZY_PLANE_KEYS, "TODO" );
			descriptions.add( ALIGN_XZ_PLANE, ALIGN_XZ_PLANE_KEYS, "TODO" );
		}
	}

}
