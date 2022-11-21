package bigwarp.ui.keymap;

import java.util.stream.IntStream;

import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.KeyStrokeAdder.Factory;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;

import bdv.TransformEventHandler3D;
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
			super( BigWarpActions.BIGWARP, BigWarpActions.BIGWARP_CTXT, "navigation", "bw-table" );
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

			// from TransformEventHandler3D
			descriptions.add( TransformEventHandler3D.DRAG_TRANSLATE, TransformEventHandler3D.DRAG_TRANSLATE_KEYS, "Pan the view by mouse-dragging." );
			descriptions.add( TransformEventHandler3D.ZOOM_NORMAL, TransformEventHandler3D.ZOOM_NORMAL_KEYS, "Zoom in by scrolling." );

			descriptions.add( TransformEventHandler3D.SELECT_AXIS_X, TransformEventHandler3D.SELECT_AXIS_X_KEYS, "Select X as the rotation axis for keyboard rotation." );
			descriptions.add( TransformEventHandler3D.SELECT_AXIS_Y, TransformEventHandler3D.SELECT_AXIS_Y_KEYS, "Select Y as the rotation axis for keyboard rotation." );
			descriptions.add( TransformEventHandler3D.SELECT_AXIS_Z, TransformEventHandler3D.SELECT_AXIS_Z_KEYS, "Select Z as the rotation axis for keyboard rotation." );

			descriptions.add( TransformEventHandler3D.DRAG_ROTATE, TransformEventHandler3D.DRAG_ROTATE_KEYS, "Rotate the view by mouse-dragging." );
			descriptions.add( TransformEventHandler3D.SCROLL_Z, TransformEventHandler3D.SCROLL_Z_KEYS, "Translate in Z by scrolling." );
			descriptions.add( TransformEventHandler3D.ROTATE_LEFT, TransformEventHandler3D.ROTATE_LEFT_KEYS, "Rotate left (counter-clockwise) by 1 degree." );
			descriptions.add( TransformEventHandler3D.ROTATE_RIGHT, TransformEventHandler3D.ROTATE_RIGHT_KEYS, "Rotate right (clockwise) by 1 degree." );
			descriptions.add( TransformEventHandler3D.KEY_ZOOM_IN, TransformEventHandler3D.KEY_ZOOM_IN_KEYS, "Zoom in." );
			descriptions.add( TransformEventHandler3D.KEY_ZOOM_OUT, TransformEventHandler3D.KEY_ZOOM_OUT_KEYS, "Zoom out." );
			descriptions.add( TransformEventHandler3D.KEY_FORWARD_Z, TransformEventHandler3D.KEY_FORWARD_Z_KEYS, "Translate forward in Z." );
			descriptions.add( TransformEventHandler3D.KEY_BACKWARD_Z, TransformEventHandler3D.KEY_BACKWARD_Z_KEYS, "Translate backward in Z." );

			descriptions.add( TransformEventHandler3D.DRAG_ROTATE_FAST, TransformEventHandler3D.DRAG_ROTATE_FAST_KEYS, "Rotate the view by mouse-dragging (fast)." );
			descriptions.add( TransformEventHandler3D.SCROLL_Z_FAST, TransformEventHandler3D.SCROLL_Z_FAST_KEYS, "Translate in Z by scrolling (fast)." );
			descriptions.add( TransformEventHandler3D.ROTATE_LEFT_FAST, TransformEventHandler3D.ROTATE_LEFT_FAST_KEYS, "Rotate left (counter-clockwise) by 10 degrees." );
			descriptions.add( TransformEventHandler3D.ROTATE_RIGHT_FAST, TransformEventHandler3D.ROTATE_RIGHT_FAST_KEYS, "Rotate right (clockwise) by 10 degrees." );
			descriptions.add( TransformEventHandler3D.KEY_ZOOM_IN_FAST, TransformEventHandler3D.KEY_ZOOM_IN_FAST_KEYS, "Zoom in (fast)." );
			descriptions.add( TransformEventHandler3D.KEY_ZOOM_OUT_FAST, TransformEventHandler3D.KEY_ZOOM_OUT_FAST_KEYS, "Zoom out (fast)." );
			descriptions.add( TransformEventHandler3D.KEY_FORWARD_Z_FAST, TransformEventHandler3D.KEY_FORWARD_Z_FAST_KEYS, "Translate forward in Z (fast)." );
			descriptions.add( TransformEventHandler3D.KEY_BACKWARD_Z_FAST, TransformEventHandler3D.KEY_BACKWARD_Z_FAST_KEYS, "Translate backward in Z (fast)." );

			descriptions.add( TransformEventHandler3D.DRAG_ROTATE_SLOW, TransformEventHandler3D.DRAG_ROTATE_SLOW_KEYS, "Rotate the view by mouse-dragging (slow)." );
			descriptions.add( TransformEventHandler3D.SCROLL_Z_SLOW, TransformEventHandler3D.SCROLL_Z_SLOW_KEYS, "Translate in Z by scrolling (slow)." );
			descriptions.add( TransformEventHandler3D.ROTATE_LEFT_SLOW, TransformEventHandler3D.ROTATE_LEFT_SLOW_KEYS, "Rotate left (counter-clockwise) by 0.1 degree." );
			descriptions.add( TransformEventHandler3D.ROTATE_RIGHT_SLOW, TransformEventHandler3D.ROTATE_RIGHT_SLOW_KEYS, "Rotate right (clockwise) by 0.1 degree." );
			descriptions.add( TransformEventHandler3D.KEY_ZOOM_IN_SLOW, TransformEventHandler3D.KEY_ZOOM_IN_SLOW_KEYS, "Zoom in (slow)." );
			descriptions.add( TransformEventHandler3D.KEY_ZOOM_OUT_SLOW, TransformEventHandler3D.KEY_ZOOM_OUT_SLOW_KEYS, "Zoom out (slow)." );
			descriptions.add( TransformEventHandler3D.KEY_FORWARD_Z_SLOW, TransformEventHandler3D.KEY_FORWARD_Z_SLOW_KEYS, "Translate forward in Z (slow)." );
			descriptions.add( TransformEventHandler3D.KEY_BACKWARD_Z_SLOW, TransformEventHandler3D.KEY_BACKWARD_Z_SLOW_KEYS, "Translate backward in Z (slow)." );


			// from TransformEventHandler2D
//			descriptions.add( TransformEventHandler2D.DRAG_TRANSLATE, TransformEventHandler2D.DRAG_TRANSLATE_KEYS, "Pan the view by mouse-dragging. Active in 2D mode." );
//			descriptions.add( TransformEventHandler2D.DRAG_ROTATE, TransformEventHandler2D.DRAG_ROTATE_KEYS, "Rotate the view by mouse-dragging. Active in 2D mode." );
//
//			descriptions.add( TransformEventHandler2D.ZOOM_NORMAL, TransformEventHandler2D.ZOOM_NORMAL_KEYS, "Zoom in by scrolling. Active in 2D mode." );
//			descriptions.add( TransformEventHandler2D.ZOOM_FAST, TransformEventHandler2D.ZOOM_FAST_KEYS, "Zoom in by scrolling (fast). Active in 2D mode." );
//			descriptions.add( TransformEventHandler2D.ZOOM_SLOW, TransformEventHandler2D.ZOOM_SLOW_KEYS, "Zoom in by scrolling (slow). Active in 2D mode." );
//
//			descriptions.add( TransformEventHandler2D.SCROLL_TRANSLATE, TransformEventHandler2D.SCROLL_TRANSLATE_KEYS, "Translate by scrolling. Active in 2D mode." );
//			descriptions.add( TransformEventHandler2D.SCROLL_TRANSLATE_FAST, TransformEventHandler2D.SCROLL_TRANSLATE_FAST_KEYS, "Translate by scrolling (fast). Active in 2D mode." );
//			descriptions.add( TransformEventHandler2D.SCROLL_TRANSLATE_SLOW, TransformEventHandler2D.SCROLL_TRANSLATE_SLOW_KEYS, "Translate by scrolling (slow). Active in 2D mode." );
//
//			descriptions.add( TransformEventHandler2D.ROTATE_LEFT, TransformEventHandler2D.ROTATE_LEFT_KEYS, "Rotate left (counter-clockwise) by 1 degree. Active in 2D mode." );
//			descriptions.add( TransformEventHandler2D.ROTATE_RIGHT, TransformEventHandler2D.ROTATE_RIGHT_KEYS, "Rotate right (clockwise) by 1 degree. Active in 2D mode." );
//			descriptions.add( TransformEventHandler2D.KEY_ZOOM_IN, TransformEventHandler2D.KEY_ZOOM_IN_KEYS, "Zoom in. Active in 2D mode." );
//			descriptions.add( TransformEventHandler2D.KEY_ZOOM_OUT, TransformEventHandler2D.KEY_ZOOM_OUT_KEYS, "Zoom out. Active in 2D mode." );
//
//			descriptions.add( TransformEventHandler2D.ROTATE_LEFT_FAST, TransformEventHandler2D.ROTATE_LEFT_FAST_KEYS, "Rotate left (counter-clockwise) by 10 degrees. Active in 2D mode." );
//			descriptions.add( TransformEventHandler2D.ROTATE_RIGHT_FAST, TransformEventHandler2D.ROTATE_RIGHT_FAST_KEYS, "Rotate right (clockwise) by 10 degrees. Active in 2D mode." );
//			descriptions.add( TransformEventHandler2D.KEY_ZOOM_IN_FAST, TransformEventHandler2D.KEY_ZOOM_IN_FAST_KEYS, "Zoom in (fast). Active in 2D mode." );
//			descriptions.add( TransformEventHandler2D.KEY_ZOOM_OUT_FAST, TransformEventHandler2D.KEY_ZOOM_OUT_FAST_KEYS, "Zoom out (fast). Active in 2D mode." );
//
//			descriptions.add( TransformEventHandler2D.ROTATE_LEFT_SLOW, TransformEventHandler2D.ROTATE_LEFT_SLOW_KEYS, "Rotate left (counter-clockwise) by 0.1 degree. Active in 2D mode." );
//			descriptions.add( TransformEventHandler2D.ROTATE_RIGHT_SLOW, TransformEventHandler2D.ROTATE_RIGHT_SLOW_KEYS, "Rotate right (clockwise) by 0.1 degree. Active in 2D mode." );
//			descriptions.add( TransformEventHandler2D.KEY_ZOOM_IN_SLOW, TransformEventHandler2D.KEY_ZOOM_IN_SLOW_KEYS, "Zoom in (slow). Active in 2D mode." );
//			descriptions.add( TransformEventHandler2D.KEY_ZOOM_OUT_SLOW, TransformEventHandler2D.KEY_ZOOM_OUT_SLOW_KEYS, "Zoom out (slow). Active in 2D mode." );
//
//			descriptions.add( TransformEventHandler2D.SCROLL_ROTATE, TransformEventHandler2D.SCROLL_ROTATE_KEYS, "Rotate by scrolling. Active in 2D mode." );
//			descriptions.add( TransformEventHandler2D.SCROLL_ROTATE_FAST, TransformEventHandler2D.SCROLL_ROTATE_FAST_KEYS, "Rotate by scrolling (fast). Active in 2D mode." );
//			descriptions.add( TransformEventHandler2D.SCROLL_ROTATE_SLOW, TransformEventHandler2D.SCROLL_ROTATE_SLOW_KEYS, "Rotate by scrolling (slow). Active in 2D mode." );
		}
	}


}
