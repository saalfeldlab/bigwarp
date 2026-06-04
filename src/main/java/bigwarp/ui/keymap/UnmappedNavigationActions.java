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
