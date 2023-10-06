/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2022 Howard Hughes Medical Institute.
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
package bigwarp;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.TableCellEditor;

import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.KeyStrokeAdder;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.InputActionBindings;

import bdv.gui.BigWarpViewerFrame;
import bdv.tools.ToggleDialogAction;
import bdv.util.Prefs;
import bdv.viewer.LandmarkPointMenu;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.AbstractViewerPanel.AlignPlane;
import bigwarp.landmarks.LandmarkGridGenerator;
import bigwarp.source.GridSource;
import bigwarp.util.BigWarpUtils;
import mpicbg.models.AbstractModel;
import net.imglib2.realtransform.AffineTransform3D;

public class BigWarpActions extends Actions
{
	public static final CommandDescriptionProvider.Scope BIGWARP = new CommandDescriptionProvider.Scope( "bigwarp" );
	public static final String BIGWARP_CTXT = "bigwarp";
	public static final String NOT_MAPPED = "not mapped";

	public static final String LANDMARK_MODE_ON  = "landmark mode on";
	public static final String LANDMARK_MODE_OFF  = "landmark mode off";
	public static final String TRANSFORM_TYPE = "transform type";

	// General options
	public static final String CLOSE_DIALOG = "close dialog window";
	public static final String[] CLOSE_DIALOG_KEYS = new String[] { NOT_MAPPED };

	public static final String LOAD_PROJECT = "load project";
	public static final String[] LOAD_PROJECT_KEYS  = new String[]{ "ctrl shift O" };

	public static final String SAVE_PROJECT = "save project";
	public static final String[] SAVE_PROJECT_KEYS  = new String[]{ "ctrl shift S" };

	public static final String EXPAND_CARDS = "expand and focus cards panel";
	public static final String[] EXPAND_CARDS_KEYS = new String[] { "P" };

	public static final String COLLAPSE_CARDS = "collapse cards panel";
	public static final String[] COLLAPSE_CARDS_KEYS = new String[] { "shift P", "shift ESCAPE" };

	public static final String PREFERENCES_DIALOG = "Preferences";
	public static final String[] PREFERENCES_DIALOG_KEYS= new String[] { "meta COMMA", "ctrl COMMA" };

	// Display options
	public static final String   TOGGLE_LANDMARK_MODE  = "landmark mode toggle";
	public static final String[] TOGGLE_LANDMARK_MODE_KEYS  = new String[]{ "SPACE" };

	public static final String   TOGGLE_POINTS_VISIBLE  = "toggle points visible";
	public static final String[] TOGGLE_POINTS_VISIBLE_KEYS  = new String[]{ "V" };

	public static final String TOGGLE_POINT_NAMES_VISIBLE  = "toggle point names visible";
	public static final String[] TOGGLE_POINT_NAMES_VISIBLE_KEYS  = new String[]{ "N" };

	public static final String TOGGLE_MOVING_IMAGE_DISPLAY = "toggle moving image display";
	public static final String[] TOGGLE_MOVING_IMAGE_DISPLAY_KEYS = new String[]{ "T" };

	public static final String TOGGLE_BOX_AND_TEXT_OVERLAY_VISIBLE  = "toggle box and text overlay visible";
	public static final String[] TOGGLE_BOX_AND_TEXT_OVERLAY_VISIBLE_KEYS  = new String[]{ "F8" };

	public static final String ESTIMATE_WARP = "estimate warp";
	public static final String[] ESTIMATE_WARP_KEYS = new String[] { "C" };

	public static final String PRINT_TRANSFORM = "print transform";
	public static final String[] PRINT_TRANSFORM_KEYS = new String[]{ "control shift T" };

	public static final String TOGGLE_ESTIMATE_WARP_ONDRAG = "toggle estimate warp on drag";
	public static final String[] TOGGLE_ESTIMATE_WARP_ONDRAG_KEYS = new String[]{ NOT_MAPPED };

	public static final String SAVE_SETTINGS = "save settings";
	public static final String[] SAVE_SETTINGS_KEYS = new String[]{ NOT_MAPPED };

	public static final String LOAD_SETTINGS = "load settings";
	public static final String[] LOAD_SETTINGS_KEYS = new String[]{ NOT_MAPPED };

	public static final String BRIGHTNESS_SETTINGS = "brightness settings";
	public static final String[] BRIGHTNESS_SETTINGS_KEYS = new String[]{ "S" };

	public static final String VISIBILITY_AND_GROUPING = "visibility and grouping %s";
	public static final String VISIBILITY_AND_GROUPING_MVG = String.format( VISIBILITY_AND_GROUPING, "moving" );
	public static final String[] VISIBILITY_AND_GROUPING_MVG_KEYS = new String[]{ "F3" };

	public static final String VISIBILITY_AND_GROUPING_TGT = String.format( VISIBILITY_AND_GROUPING, "target" );
	public static final String[] VISIBILITY_AND_GROUPING_TGT_KEYS = new String[]{ "F4" };

	public static final String  SHOW_HELP = "help";
	public static final String[] SHOW_HELP_KEYS = new String[] { "F1" };

	public static final String SHOW_SOURCE_INFO = "show source info";

	// Warp visualization options
	public static final String SHOW_WARPTYPE_DIALOG = "show warp vis dialog" ;
	public static final String[] SHOW_WARPTYPE_DIALOG_KEYS = new String[]{ "U" };

	public static final String SET_WARPTYPE_VIS = "set warp vis type %s" ;

	public static final String SET_WARPTYPE_VIS_P = "p " + SET_WARPTYPE_VIS;

	public static final String SET_WARPTYPE_VIS_Q = "q " + SET_WARPTYPE_VIS;

	public static final String WARPMAG_BASE = "set warpmag base %s";
	public static final String WARPVISGRID = "set warp vis grid %s";
	public static final String WARPVISDIALOG = "warp vis dialog";

	// Navigation options
	public static final String   RESET_VIEWER = "reset active viewer";
	public static final String[] RESET_VIEWER_KEYS = new String[]{"R"};


	public static final String ALIGN_VIEW_TRANSFORMS = "align view transforms %s";
	public static final String ALIGN_OTHER_TO_ACTIVE = String.format( ALIGN_VIEW_TRANSFORMS, AlignViewerPanelAction.TYPE.OTHER_TO_ACTIVE );
	public static final String[] ALIGN_OTHER_TO_ACTIVE_KEYS = new String[] { "Q" };

	public static final String ALIGN_ACTIVE_TO_OTHER = String.format( ALIGN_VIEW_TRANSFORMS, AlignViewerPanelAction.TYPE.ACTIVE_TO_OTHER );
	public static final String[] ALIGN_ACTIVE_TO_OTHER_KEYS = new String[] { "W" };

	public static final String JUMP_TO_SELECTED_POINT = "center on selected landmark";
	public static final String[] JUMP_TO_SELECTED_POINT_KEYS = new String[]{ "D" };

	public static final String JUMP_TO_NEXT_POINT = "center on next landmark";
	public static final String[] JUMP_TO_NEXT_POINT_KEYS = new String[]{ "ctrl D"};

	public static final String JUMP_TO_PREV_POINT = "center on prev landmark";
	public static final String[] JUMP_TO_PREV_POINT_KEYS = new String[]{ "ctrl shift D"};

	public static final String JUMP_TO_NEAREST_POINT = "center on nearest landmark";
	public static final String[] JUMP_TO_NEAREST_POINT_KEYS = new String[]{ "E" };

	// landmark options
	public static final String LOAD_LANDMARKS = "load landmarks";
	public static final String[] LOAD_LANDMARKS_KEYS = new String[]{ "control O" };

	public static final String SAVE_LANDMARKS = "save landmarks";
	public static final String[] SAVE_LANDMARKS_KEYS = new String[]{ "control S" };

	public static final String QUICK_SAVE_LANDMARKS = "quick save landmarks";
	public static final String[] QUICK_SAVE_LANDMARKS_KEYS = new String[]{ "control Q" };

	public static final String SET_BOOKMARK = "set bookmark";
	public static final String[] SET_BOOKMARK_KEYS = new String[]{ "shift B" };

	public static final String GO_TO_BOOKMARK = "go to bookmark";
	public static final String[] GO_TO_BOOKMARK_KEYS = new String[]{ "B" };

	public static final String GO_TO_BOOKMARK_ROTATION = "go to bookmark rotation";
	public static final String[] GO_TO_BOOKMARK_ROTATION_KEYS = new String[]{ "control shift B" };

	public static final String UNDO = "undo";
	public static final String[] UNDO_KEYS = new String[]{ "control Z" };

	public static final String REDO = "redo";
	public static final String[] REDO_KEYS = new String[]{ "control shift Z", "control Y" };

	public static final String SELECT_TABLE_ROWS = "select table row %d";

	public static final String LANDMARK_SELECT_ALL = "select all landmarks";
	public static final String[] LANDMARK_SELECT_ALL_KEYS = new String[]{ "ctrl A"};

	public static final String LANDMARK_DESELECT_ALL = "deselect all landmarks";
	public static final String[] LANDMARK_DESELECT_ALL_KEYS = new String[]{ "ESCAPE", "ctrl shift A" };

	public static final String LANDMARK_SELECT_ABOVE = "select landmark above";
	public static final String[] LANDMARK_SELECT_ABOVE_KEYS = new String[]{ "ctrl UP"};

	public static final String LANDMARK_SELECT_ALL_ABOVE = "select all landmarks above";
	public static final String[] LANDMARK_SELECT_ALL_ABOVE_KEYS = new String[]{ "ctrl shift UP"};

	public static final String LANDMARK_SELECT_BELOW = "select landmark below";
	public static final String[] LANDMARK_SELECT_BELOW_KEYS = new String[]{ "ctrl DOWN"};

	public static final String LANDMARK_SELECT_ALL_BELOW = "select all landmarks below";
	public static final String[] LANDMARK_SELECT_ALL_BELOW_KEYS = new String[]{ "ctrl shift DOWN"};

	public static final String LANDMARK_DELETE_SELECTED = "delete selected landmarks";
	public static final String[] LANDMARK_DELETE_SELECTED_KEYS = new String[]{ "DELETE" };

	public static final String LANDMARK_DEACTIVATE_SELECTED = "deactivate selected landmarks";
	public static final String[] LANDMARK_DEACTIVATE_SELECTED_KEYS = new String[]{ "BACK_SPACE" };

	public static final String[] LANDMARK_ACTIVATE_SELECTED_KEYS = new String[]{ "ctrl BACK_SPACE" };
	public static final String LANDMARK_ACTIVATE_SELECTED = "activate selected landmarks";

	public static final String LANDMARK_GRID_DIALOG = "landmark grid dialog";

	public static final String MASK_IMPORT = "import mask";
	public static final String MASK_REMOVE = "remove mask";
	public static final String MASK_SIZE_EDIT = "mask edit";
	public static final String MASK_VIS_TOGGLE = "mask vis toggle";

	// export options
	public static final String SAVE_WARPED = "save warped";
	public static final String SAVE_WARPED_XML = "save warped xml";
	public static final String[] SAVE_WARPED_XML_KEYS = new String[] { "ctrl E" };

	public static final String EXPORT_IP = "export imageplus";
	public static final String[] EXPORT_IP_KEYS = new String[] { "ctrl shift W" };

	public static final String EXPORT_WARP = "export warp field";
	public static final String[] EXPORT_WARP_KEYS = new String[] { "ctrl W" };

	public static final String EXPORT_AFFINE = "export affine";
	public static final String[] EXPORT_AFFINE_KEYS = new String[] { "ctrl A" };

	public static final String CLEAR_MOVING = "table clear moving";
	public static final String[] CLEAR_MOVING_KEYS = new String[] { "BACK_SPACE" };

	public static final String CLEAR_FIXED = "table clear fixed";
	public static final String[] CLEAR_FIXED_KEYS = new String[] { "ctrl BACK_SPACE" };

	public static final String CLEAR_SELECTED_MOVING = "table clear selected moving";
	public static final String[] CLEAR_SELECTED_MOVING_KEYS = new String[] { "ctrl BACK_SPACE" };

	public static final String CLEAR_SELECTED_FIXED = "table clear selected fixed";
	public static final String[] CLEAR_SELECTED_FIXED_KEYS = new String[] { "ctrl BACK_SPACE" };

	public static final String DELETE = "table delete";
	public static final String[] DELETE_KEYS = new String[] { NOT_MAPPED };

	public static final String DELETE_SELECTED = "table delete selected ";
	public static final String[] DELETE_SELECTED_KEYS = new String[] { "DELETE" };

	public static final String ACTIVATE_SELECTED = "table activate selected";
	public static final String[] ACTIVATE_SELECTED_KEYS = new String[] { NOT_MAPPED };

	public static final String DEACTIVATE_SELECTED = "table deactivate selected ";
	public static final String[] DEACTIVATE_SELECTED_KEYS = new String[]{ NOT_MAPPED };

	public static final String DEBUG = "debug";
	public static final String GARBAGE_COLLECTION = "garbage collection";

	public static final String XYPLANE = "xyPlane";
	public static final String[] XYPLANE_KEYS = new String[] { "shift Z" };

	public static final String YZPLANE = "yzPlane";
	public static final String[] YZPLANE_KEYS = new String[] { "shift X" };

	public static final String XZPLANE = "xzPlane";
	public static final String[] XZPLANE_KEYS = new String[] { "shift Y", "shift A" };

	public BigWarpActions( final KeyStrokeAdder.Factory keyConfig, String name )
	{
		this( keyConfig, "bigwarp", name );
	}

	public BigWarpActions( final KeyStrokeAdder.Factory keyConfig, String context, String name )
	{
		super( keyConfig, context, name );
	}

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( BIGWARP, "bigwarp", "bw-table", "navigation" );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( CLOSE_DIALOG, CLOSE_DIALOG_KEYS, "Close bigwarp." );

			descriptions.add( SAVE_PROJECT, SAVE_PROJECT_KEYS, "Save a bigwarp project." );
			descriptions.add( LOAD_PROJECT, LOAD_PROJECT_KEYS, "Load a bigwarp project." );

			descriptions.add( TOGGLE_LANDMARK_MODE, TOGGLE_LANDMARK_MODE_KEYS, "Toggle landmark mode." );
			descriptions.add( TOGGLE_MOVING_IMAGE_DISPLAY, TOGGLE_MOVING_IMAGE_DISPLAY_KEYS, "Toggle landmark mode." );

			descriptions.add( SHOW_HELP, SHOW_HELP_KEYS, "Show the Help dialog." );
			descriptions.add( SHOW_WARPTYPE_DIALOG, SHOW_WARPTYPE_DIALOG_KEYS, "Show the BigWarp options dialog." );
			descriptions.add( PREFERENCES_DIALOG, PREFERENCES_DIALOG_KEYS, "Show the appearance and keymap dialog." );

			descriptions.add( BRIGHTNESS_SETTINGS, BRIGHTNESS_SETTINGS_KEYS, "Show the Brightness & Colors dialog." );
			descriptions.add( VISIBILITY_AND_GROUPING_MVG, VISIBILITY_AND_GROUPING_MVG_KEYS, "Show the Visibility&Grouping dialog for the moving frame." );
			descriptions.add( VISIBILITY_AND_GROUPING_TGT, VISIBILITY_AND_GROUPING_TGT_KEYS, "Show the Visibility&Grouping dialog for the fixed frame." );
			descriptions.add( SAVE_SETTINGS, SAVE_SETTINGS_KEYS, "Save the BigDataViewer settings to a settings.xml file." );
			descriptions.add( LOAD_SETTINGS, LOAD_SETTINGS_KEYS, "Load the BigDataViewer settings from a settings.xml file." );

			descriptions.add( SET_BOOKMARK, SET_BOOKMARK_KEYS, "Set a labeled bookmark at the current location." );
			descriptions.add( GO_TO_BOOKMARK, GO_TO_BOOKMARK_KEYS, "Retrieve a labeled bookmark location." );
			descriptions.add( GO_TO_BOOKMARK_ROTATION, GO_TO_BOOKMARK_ROTATION_KEYS, "Retrieve a labeled bookmark, set only the orientation." );

			descriptions.add( RESET_VIEWER, RESET_VIEWER_KEYS, "Resets the view to the view on startup." );
			descriptions.add( ALIGN_OTHER_TO_ACTIVE, ALIGN_OTHER_TO_ACTIVE_KEYS, "Sets the view of the non-active viewer to match the active viewer." );
			descriptions.add( ALIGN_ACTIVE_TO_OTHER, ALIGN_ACTIVE_TO_OTHER_KEYS, "Sets the view of the active viewer to match the non-active viewer." );
			descriptions.add( JUMP_TO_SELECTED_POINT, JUMP_TO_SELECTED_POINT_KEYS, "Center the viewer on the selected landmark." );
			descriptions.add( JUMP_TO_NEAREST_POINT, JUMP_TO_NEAREST_POINT_KEYS, "Center the viewer on the nearest landmark." );
			descriptions.add( JUMP_TO_NEXT_POINT, JUMP_TO_NEXT_POINT_KEYS, "Center the viewer on the next landmark." );
			descriptions.add( JUMP_TO_PREV_POINT, JUMP_TO_PREV_POINT_KEYS, "Center the viewer on the previous landmark." );

			// cards
			descriptions.add( EXPAND_CARDS, EXPAND_CARDS_KEYS, "Expand and focus the BigDataViewer card panel" );
			descriptions.add( COLLAPSE_CARDS, COLLAPSE_CARDS_KEYS, "Collapse the BigDataViewer card panel" );

			// export
			descriptions.add( EXPORT_IP, EXPORT_IP_KEYS, "Export moving image to ImageJ." );
			descriptions.add( SAVE_WARPED_XML, SAVE_WARPED_XML_KEYS, "Export moving image to BigDataViewer xml/h5." );
			descriptions.add( EXPORT_WARP, EXPORT_WARP_KEYS, "Show the dialog to export the displacement field." );
			descriptions.add( EXPORT_AFFINE, EXPORT_AFFINE_KEYS, "Print the affine transformation." );
			descriptions.add( PRINT_TRANSFORM,PRINT_TRANSFORM_KEYS, "Prints the current transformation." );

			// landmarks
			descriptions.add( LOAD_LANDMARKS, LOAD_LANDMARKS_KEYS, "Load landmark from a file." );
			descriptions.add( SAVE_LANDMARKS, SAVE_LANDMARKS_KEYS, "Save landmark from a file." );
			descriptions.add( QUICK_SAVE_LANDMARKS, QUICK_SAVE_LANDMARKS_KEYS, "Quick save landmarks.");
			descriptions.add( UNDO, UNDO_KEYS, "Undo the last landmark change." );
			descriptions.add( REDO, REDO_KEYS, "Redo the last landmark change." );

			descriptions.add( TOGGLE_POINTS_VISIBLE, TOGGLE_POINTS_VISIBLE_KEYS, "Toggle visibility of landmarks." );
			descriptions.add( TOGGLE_POINT_NAMES_VISIBLE, TOGGLE_POINT_NAMES_VISIBLE_KEYS , "Toggle visibility of landmark names." );

			descriptions.add( TOGGLE_BOX_AND_TEXT_OVERLAY_VISIBLE, TOGGLE_BOX_AND_TEXT_OVERLAY_VISIBLE_KEYS, "Toggle visibility of bounding box and source information." );
			descriptions.add( TOGGLE_POINTS_VISIBLE, TOGGLE_POINTS_VISIBLE_KEYS, "Toggle visibility of landmark points." );
			descriptions.add( TOGGLE_POINT_NAMES_VISIBLE, TOGGLE_POINT_NAMES_VISIBLE_KEYS, "Toggle visibility of landmark point names." );

			descriptions.add( LANDMARK_ACTIVATE_SELECTED, LANDMARK_ACTIVATE_SELECTED_KEYS, "Activate selected landmarks." );
			descriptions.add( LANDMARK_DEACTIVATE_SELECTED, LANDMARK_DEACTIVATE_SELECTED_KEYS, "Deactivate selected landmarks." );

			// alignment
			descriptions.add( XYPLANE, XYPLANE_KEYS, "xy plane" );
			descriptions.add( XZPLANE, XZPLANE_KEYS, "xz plane" );
			descriptions.add( YZPLANE, YZPLANE_KEYS, "yz plane" );
		}
	}

	@Plugin( type = CommandDescriptionProvider.class )
	public static class TableDescriptions extends CommandDescriptionProvider
	{
		public TableDescriptions()
		{
			super( BIGWARP, "bw-table", "bigwarp", "navigation" );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( SAVE_PROJECT, SAVE_PROJECT_KEYS, "Save a bigwarp project." );
			descriptions.add( LOAD_PROJECT, LOAD_PROJECT_KEYS, "Load a bigwarp project." );

			descriptions.add( CLEAR_MOVING, CLEAR_MOVING_KEYS, "Clears moving landmark under the mouse cursor." );
			descriptions.add( CLEAR_FIXED, CLEAR_FIXED_KEYS, "Clears fixed landmark under the mouse cursor." );
			descriptions.add( CLEAR_SELECTED_MOVING, CLEAR_SELECTED_MOVING_KEYS, "Clears moving landmark for currently selected row." );
			descriptions.add( CLEAR_SELECTED_FIXED, CLEAR_SELECTED_FIXED_KEYS, "Clears fixed landmark for currently selected row." );

			descriptions.add( DELETE, DELETE_KEYS, "Delete table row under the mouse cursor" );
			descriptions.add( DELETE_SELECTED, DELETE_SELECTED_KEYS, "Delete all selected rows in the table" );

			descriptions.add( ACTIVATE_SELECTED, ACTIVATE_SELECTED_KEYS, "Activate all selected rows in the table" );
			descriptions.add( DEACTIVATE_SELECTED, DEACTIVATE_SELECTED_KEYS, "Deactivate all selected rows in the table" );

			descriptions.add( LOAD_LANDMARKS, LOAD_LANDMARKS_KEYS, "Load landmark from a file." );
			descriptions.add( SAVE_LANDMARKS, SAVE_LANDMARKS_KEYS, "Save landmark from a file." );
			descriptions.add( QUICK_SAVE_LANDMARKS, QUICK_SAVE_LANDMARKS_KEYS, "Quick save landmarks.");

			descriptions.add( LANDMARK_SELECT_ALL, LANDMARK_SELECT_ALL_KEYS, "Select all landmarks." );
			descriptions.add( LANDMARK_DESELECT_ALL, LANDMARK_DESELECT_ALL_KEYS, "Deselect all landmarks." );

			descriptions.add( LANDMARK_DELETE_SELECTED, LANDMARK_DELETE_SELECTED_KEYS, "Delete selected landmarks." );
			descriptions.add( LANDMARK_ACTIVATE_SELECTED, LANDMARK_ACTIVATE_SELECTED_KEYS, "Activate selected landmarks." );
			descriptions.add( LANDMARK_DEACTIVATE_SELECTED, LANDMARK_DEACTIVATE_SELECTED_KEYS, "Deactivate selected landmarks." );

			descriptions.add( LANDMARK_SELECT_ABOVE, LANDMARK_SELECT_ABOVE_KEYS, "Add the row above the curent selection to the selection" );
			descriptions.add( LANDMARK_SELECT_ALL_ABOVE, LANDMARK_SELECT_ALL_ABOVE_KEYS, "Add the all rows above the curent selection to the selection" );
			descriptions.add( LANDMARK_SELECT_BELOW, LANDMARK_SELECT_BELOW_KEYS, "Add the row below the curent selection to the selection" );
			descriptions.add( LANDMARK_SELECT_ALL_BELOW, LANDMARK_SELECT_ALL_BELOW_KEYS, "Add the all rows below the curent selection to the selection" );

			descriptions.add( UNDO, UNDO_KEYS, "Undo the last landmark change." );
			descriptions.add( REDO, REDO_KEYS, "Redo the last landmark change." );

			descriptions.add( TOGGLE_POINTS_VISIBLE, TOGGLE_POINTS_VISIBLE_KEYS, "Toggle visibility of landmark points." );
			descriptions.add( TOGGLE_POINT_NAMES_VISIBLE, TOGGLE_POINT_NAMES_VISIBLE_KEYS, "Toggle visibility of landmark point names." );
		}
	}

	public static void installViewerActions(
			Actions actions,
			final BigWarpViewerFrame bwFrame,
			final BigWarp< ? > bw )
	{

		final InputActionBindings inputActionBindings = bwFrame.getKeybindings();
		actions.install( inputActionBindings, "bw" );

		actions.runnableAction( bw::saveProject, SAVE_PROJECT, SAVE_PROJECT_KEYS );
		actions.runnableAction( bw::loadProject, LOAD_PROJECT, LOAD_PROJECT_KEYS );

		actions.runnableAction( () -> { bw.getBwTransform().transformToString(); }, PRINT_TRANSFORM, PRINT_TRANSFORM_KEYS);
		actions.runnableAction( bw::toggleInLandmarkMode, TOGGLE_LANDMARK_MODE, TOGGLE_LANDMARK_MODE_KEYS);
		actions.runnableAction( bw::toggleMovingImageDisplay, TOGGLE_MOVING_IMAGE_DISPLAY, TOGGLE_MOVING_IMAGE_DISPLAY_KEYS );

		actions.namedAction( new TogglePointsVisibleAction( TOGGLE_POINTS_VISIBLE, bw ), TOGGLE_POINTS_VISIBLE_KEYS);
		actions.namedAction( new TogglePointNameVisibleAction( TOGGLE_POINT_NAMES_VISIBLE, bw ), TOGGLE_POINT_NAMES_VISIBLE_KEYS);

		// navigation
		actions.runnableAction( bw::resetView, RESET_VIEWER, RESET_VIEWER_KEYS);
		actions.runnableAction( bw::matchOtherViewerPanelToActive, ALIGN_OTHER_TO_ACTIVE, ALIGN_OTHER_TO_ACTIVE_KEYS );
		actions.runnableAction( bw::matchActiveViewerPanelToOther, ALIGN_ACTIVE_TO_OTHER, ALIGN_ACTIVE_TO_OTHER_KEYS );
		actions.runnableAction( bw::jumpToSelectedLandmark, JUMP_TO_SELECTED_POINT, JUMP_TO_SELECTED_POINT_KEYS );
		actions.runnableAction( bw::jumpToNearestLandmark, JUMP_TO_NEAREST_POINT, JUMP_TO_NEAREST_POINT_KEYS );
		actions.runnableAction( bw::jumpToNextLandmark, JUMP_TO_NEXT_POINT, JUMP_TO_NEXT_POINT_KEYS );
		actions.runnableAction( bw::jumpToPrevLandmark, JUMP_TO_PREV_POINT, JUMP_TO_PREV_POINT_KEYS );

		// bookmarks
		actions.runnableAction( bw::goToBookmark, GO_TO_BOOKMARK, GO_TO_BOOKMARK_KEYS );
		actions.runnableAction( bw::goToBookmarkRotation, GO_TO_BOOKMARK_ROTATION, GO_TO_BOOKMARK_ROTATION_KEYS );
		actions.runnableAction( bw::setBookmark, SET_BOOKMARK, SET_BOOKMARK_KEYS );

		// cards
		actions.runnableAction( bwFrame::expandAndFocusCardPanel, EXPAND_CARDS, EXPAND_CARDS_KEYS );
		actions.runnableAction( bwFrame::collapseCardPanel, COLLAPSE_CARDS, COLLAPSE_CARDS_KEYS );

		// export
		actions.runnableAction( bw::exportWarpField, EXPORT_WARP, EXPORT_WARP_KEYS );
		actions.runnableAction( () -> { bw.getBwTransform().printAffine(); }, EXPORT_AFFINE, EXPORT_AFFINE_KEYS );

		// dialogs
		actions.namedAction( new ToggleDialogAction( SHOW_HELP, bw.helpDialog ), SHOW_HELP_KEYS );
		actions.namedAction( new ToggleDialogAction( VISIBILITY_AND_GROUPING_MVG, bw.activeSourcesDialogP ), VISIBILITY_AND_GROUPING_MVG_KEYS );
		actions.namedAction( new ToggleDialogAction( VISIBILITY_AND_GROUPING_TGT, bw.activeSourcesDialogQ ), VISIBILITY_AND_GROUPING_TGT_KEYS );
		actions.namedAction( new ToggleDialogAction( SHOW_WARPTYPE_DIALOG, bw.warpVisDialog ), SHOW_WARPTYPE_DIALOG_KEYS );
		actions.namedAction( new ToggleDialogAction( PREFERENCES_DIALOG, bw.preferencesDialog ), PREFERENCES_DIALOG_KEYS );

		// landmarks unbound
		actions.runnableAction( () -> { bw.getLandmarkPanel().getJTable().selectAll(); }, LANDMARK_SELECT_ALL, NOT_MAPPED );
		actions.runnableAction( () -> { bw.getLandmarkPanel().getJTable().clearSelection(); }, LANDMARK_DESELECT_ALL, NOT_MAPPED );

		actions.namedAction( bw.landmarkPopupMenu.deleteSelectedHandler, LANDMARK_DELETE_SELECTED_KEYS );

		actions.namedAction( bw.landmarkPopupMenu.activateSelectedHandler, LANDMARK_ACTIVATE_SELECTED_KEYS );
		actions.namedAction( bw.landmarkPopupMenu.deactivateSelectedHandler, LANDMARK_DEACTIVATE_SELECTED_KEYS );

		actions.namedAction( bw.landmarkPopupMenu.addAboveHandler, LANDMARK_SELECT_ABOVE_KEYS );
		actions.namedAction( bw.landmarkPopupMenu.addAllAboveHandler, LANDMARK_SELECT_ALL_ABOVE_KEYS );
		actions.namedAction( bw.landmarkPopupMenu.addBelowHandler, LANDMARK_SELECT_BELOW_KEYS );
		actions.namedAction( bw.landmarkPopupMenu.addAllBelowHandler, LANDMARK_SELECT_ALL_BELOW_KEYS );

		// landmarks bound
		actions.runnableAction( bw::loadLandmarks, LOAD_LANDMARKS, LOAD_LANDMARKS_KEYS );
		actions.runnableAction( bw::saveLandmarks, SAVE_LANDMARKS, SAVE_LANDMARKS_KEYS );
		actions.runnableAction( bw::quickSaveLandmarks, QUICK_SAVE_LANDMARKS, QUICK_SAVE_LANDMARKS_KEYS );

		actions.namedAction( new UndoRedoAction( UNDO, bw ), UNDO_KEYS );
		actions.namedAction( new UndoRedoAction( REDO, bw ), REDO_KEYS );

		actions.namedAction( new ToggleBoxAndTexOverlayVisibility( TOGGLE_BOX_AND_TEXT_OVERLAY_VISIBLE, bw ), TOGGLE_BOX_AND_TEXT_OVERLAY_VISIBLE_KEYS);
		actions.namedAction( new TogglePointsVisibleAction( TOGGLE_POINTS_VISIBLE, bw ),  TOGGLE_POINTS_VISIBLE_KEYS );
		actions.namedAction( new TogglePointNameVisibleAction( TOGGLE_POINT_NAMES_VISIBLE, bw ), TOGGLE_POINT_NAMES_VISIBLE_KEYS);

		actions.runnableAction( () -> bw.alignActive( AlignPlane.XY ), XYPLANE, XYPLANE_KEYS );
		actions.runnableAction( () -> bw.alignActive( AlignPlane.XZ ), XZPLANE, XZPLANE_KEYS );
		actions.runnableAction( () -> bw.alignActive( AlignPlane.ZY ), YZPLANE, YZPLANE_KEYS );
	}

	public static void installTableActions(
			Actions actions,
			final InputActionBindings inputActionBindings,
			final BigWarp< ? > bw )
	{
		actions.install( inputActionBindings, "bw-table" );

		actions.runnableAction( bw::saveProject, SAVE_PROJECT, SAVE_PROJECT_KEYS );
		actions.runnableAction( bw::loadProject, LOAD_PROJECT, LOAD_PROJECT_KEYS );

		// unmapped
		actions.runnableAction( () -> { bw.getBwTransform().transformToString(); }, PRINT_TRANSFORM, PRINT_TRANSFORM_KEYS);
		actions.runnableAction( bw::toggleInLandmarkMode, TOGGLE_LANDMARK_MODE, TOGGLE_LANDMARK_MODE_KEYS);
		actions.runnableAction( bw::toggleMovingImageDisplay, TOGGLE_MOVING_IMAGE_DISPLAY, TOGGLE_MOVING_IMAGE_DISPLAY_KEYS );

		// navigation
		actions.runnableAction( bw::resetView, RESET_VIEWER, NOT_MAPPED );
		actions.runnableAction( bw::matchOtherViewerPanelToActive, ALIGN_OTHER_TO_ACTIVE, NOT_MAPPED );
		actions.runnableAction( bw::matchActiveViewerPanelToOther, ALIGN_ACTIVE_TO_OTHER, NOT_MAPPED );
		actions.runnableAction( bw::jumpToSelectedLandmark, JUMP_TO_SELECTED_POINT, NOT_MAPPED );
		actions.runnableAction( bw::jumpToNearestLandmark, JUMP_TO_NEAREST_POINT, NOT_MAPPED );
		actions.runnableAction( bw::jumpToNextLandmark, JUMP_TO_NEXT_POINT, NOT_MAPPED );
		actions.runnableAction( bw::jumpToPrevLandmark, JUMP_TO_PREV_POINT, NOT_MAPPED );

		// bookmarks
		actions.runnableAction( bw::goToBookmark, GO_TO_BOOKMARK, NOT_MAPPED );
		actions.runnableAction( bw::goToBookmarkRotation, GO_TO_BOOKMARK_ROTATION, NOT_MAPPED );
		actions.runnableAction( bw::setBookmark, SET_BOOKMARK, NOT_MAPPED );

		// cards
		actions.runnableAction( ()->{}, EXPAND_CARDS, NOT_MAPPED );
		actions.runnableAction( ()->{}, COLLAPSE_CARDS, NOT_MAPPED );

		// export
		actions.runnableAction( bw::exportWarpField, EXPORT_WARP, EXPORT_WARP_KEYS );
		actions.runnableAction( () -> { bw.getBwTransform().printAffine(); }, EXPORT_AFFINE, EXPORT_AFFINE_KEYS );

		// dialogs
		actions.namedAction( new ToggleDialogAction( SHOW_HELP, bw.helpDialog ), SHOW_HELP_KEYS );
		actions.namedAction( new ToggleDialogAction( VISIBILITY_AND_GROUPING_MVG, bw.activeSourcesDialogP ), VISIBILITY_AND_GROUPING_MVG_KEYS );
		actions.namedAction( new ToggleDialogAction( VISIBILITY_AND_GROUPING_TGT, bw.activeSourcesDialogQ ), VISIBILITY_AND_GROUPING_TGT_KEYS );
		actions.namedAction( new ToggleDialogAction( SHOW_WARPTYPE_DIALOG, bw.warpVisDialog ), SHOW_WARPTYPE_DIALOG_KEYS );
		actions.namedAction( new ToggleDialogAction( PREFERENCES_DIALOG, bw.preferencesDialog ), PREFERENCES_DIALOG_KEYS );

		// landmarks
		actions.runnableAction( () -> { bw.getLandmarkPanel().getJTable().selectAll(); }, LANDMARK_SELECT_ALL, LANDMARK_SELECT_ALL_KEYS );
		actions.runnableAction( () -> { bw.getLandmarkPanel().getJTable().clearSelection(); }, LANDMARK_DESELECT_ALL, LANDMARK_DESELECT_ALL_KEYS );

		actions.namedAction( bw.landmarkPopupMenu.deleteSelectedHandler, LANDMARK_DELETE_SELECTED_KEYS );

		actions.namedAction( bw.landmarkPopupMenu.activateSelectedHandler, LANDMARK_ACTIVATE_SELECTED_KEYS );
		actions.namedAction( bw.landmarkPopupMenu.deactivateSelectedHandler, LANDMARK_DEACTIVATE_SELECTED_KEYS );

		actions.namedAction( bw.landmarkPopupMenu.addAboveHandler, LANDMARK_SELECT_ABOVE_KEYS );
		actions.namedAction( bw.landmarkPopupMenu.addAllAboveHandler, LANDMARK_SELECT_ALL_ABOVE_KEYS );
		actions.namedAction( bw.landmarkPopupMenu.addBelowHandler, LANDMARK_SELECT_BELOW_KEYS );
		actions.namedAction( bw.landmarkPopupMenu.addAllBelowHandler, LANDMARK_SELECT_ALL_BELOW_KEYS );

		actions.runnableAction( bw::loadLandmarks, LOAD_LANDMARKS, LOAD_LANDMARKS_KEYS );
		actions.runnableAction( bw::saveLandmarks, SAVE_LANDMARKS, SAVE_LANDMARKS_KEYS );
		actions.runnableAction( bw::quickSaveLandmarks, QUICK_SAVE_LANDMARKS, QUICK_SAVE_LANDMARKS_KEYS );

		actions.namedAction( new UndoRedoAction( UNDO, bw ), UNDO_KEYS );
		actions.namedAction( new UndoRedoAction( REDO, bw ), REDO_KEYS );

		actions.namedAction( new ToggleBoxAndTexOverlayVisibility( TOGGLE_BOX_AND_TEXT_OVERLAY_VISIBLE, bw ), NOT_MAPPED );
		actions.namedAction( new TogglePointsVisibleAction( TOGGLE_POINTS_VISIBLE, bw ),  TOGGLE_POINTS_VISIBLE_KEYS );
		actions.namedAction( new TogglePointNameVisibleAction( TOGGLE_POINT_NAMES_VISIBLE, bw ), TOGGLE_POINT_NAMES_VISIBLE_KEYS);
	}

	/**
	 * Create BigWarp actions and install them in the specified
	* {@link InputActionBindings}.
	 *
	 * @param inputActionBindings
	 *            {@link InputMap} and {@link ActionMap} are installed here.
	 * @param bw
	 *            Actions are targeted at this {@link BigWarp}.
	 * @param keyProperties
	 *            user-defined key-bindings.
	 */
	public static void installActionBindings(
			final InputActionBindings inputActionBindings,
			final BigWarp< ? > bw,
			final KeyStrokeAdder.Factory keyProperties )
	{
		inputActionBindings.addActionMap( "bw", createActionMap( bw ) );
		inputActionBindings.addInputMap( "bw", createInputMap( keyProperties ) );

		inputActionBindings.addActionMap( "bwV", createActionMapViewer( bw ) );
		inputActionBindings.addInputMap( "bwv", createInputMapViewer( keyProperties ) );
	}

	public static void installLandmarkPanelActionBindings(
			final InputActionBindings inputActionBindings,
			final BigWarp< ? > bw,
			final JTable landmarkTable,
			final KeyStrokeAdder.Factory keyProperties )
	{
		inputActionBindings.addActionMap( "bw", createActionMap( bw ) );
		inputActionBindings.addInputMap( "bw", createInputMap( keyProperties ) );

		final TableCellEditor celled = landmarkTable.getCellEditor( 0, 1 );
		final Component c = celled.getTableCellEditorComponent(landmarkTable, Boolean.TRUE, true, 0, 1 );

		final InputMap parentInputMap = ((JCheckBox)c).getInputMap().getParent();
		parentInputMap.clear();
		final KeyStroke enterDownKS = KeyStroke.getKeyStroke("pressed ENTER" );
		final KeyStroke enterUpKS = KeyStroke.getKeyStroke("released ENTER" );

		parentInputMap.put( enterDownKS, "pressed" );
		parentInputMap.put(   enterUpKS, "released" );
	}

	public static InputMap createInputMapViewer( final KeyStrokeAdder.Factory keyProperties )
	{
		final InputMap inputMap = new InputMap();
		final KeyStrokeAdder map = keyProperties.keyStrokeAdder( inputMap );

		map.put(RESET_VIEWER, "R");

		map.put( String.format( VISIBILITY_AND_GROUPING, "moving" ), "F3" );
		map.put( String.format( VISIBILITY_AND_GROUPING, "target" ), "F4" );
		map.put( TRANSFORM_TYPE, "F2" );

		map.put( String.format( ALIGN_VIEW_TRANSFORMS, AlignViewerPanelAction.TYPE.OTHER_TO_ACTIVE ), "Q" );
		map.put( String.format( ALIGN_VIEW_TRANSFORMS, AlignViewerPanelAction.TYPE.ACTIVE_TO_OTHER ), "W" );

		map.put( TOGGLE_MOVING_IMAGE_DISPLAY, "T" );

		map.put( JUMP_TO_SELECTED_POINT, "D" );
		map.put( String.format( JUMP_TO_NEXT_POINT, true), "ctrl D" );
		map.put( String.format( JUMP_TO_NEXT_POINT, false), "ctrl shift D" );
		map.put( JUMP_TO_NEAREST_POINT, "E" );

		map.put( EXPORT_WARP, "ctrl W" );
		map.put( EXPORT_AFFINE, "ctrl A" );

		map.put( GO_TO_BOOKMARK, "B" );
		map.put( GO_TO_BOOKMARK_ROTATION, "O" );
		map.put( SET_BOOKMARK, "shift B" );

		return inputMap;
	}

	public static ActionMap createActionMapViewer( final BigWarp< ? > bw )
	{
		final ActionMap actionMap = new ActionMap();

		new ToggleDialogAction( String.format( VISIBILITY_AND_GROUPING, "moving" ), bw.activeSourcesDialogP ).put( actionMap );
		new ToggleDialogAction( String.format( VISIBILITY_AND_GROUPING, "target" ), bw.activeSourcesDialogQ ).put( actionMap );
		new ToggleDialogAction( TRANSFORM_TYPE, bw.transformSelector ).put( actionMap );

		for( final BigWarp.WarpVisType t: BigWarp.WarpVisType.values())
		{
			new SetWarpVisTypeAction( t, bw ).put( actionMap );
			new SetWarpVisTypeAction( t, bw, bw.getViewerFrameP() ).put( actionMap );
			new SetWarpVisTypeAction( t, bw, bw.getViewerFrameQ() ).put( actionMap );
		}

		new ResetActiveViewerAction( bw ).put( actionMap );
		new AlignViewerPanelAction( bw, AlignViewerPanelAction.TYPE.ACTIVE_TO_OTHER ).put( actionMap );
		new AlignViewerPanelAction( bw, AlignViewerPanelAction.TYPE.OTHER_TO_ACTIVE ).put( actionMap );
		new WarpToSelectedAction( bw ).put( actionMap );
		new JumpToNextAction( bw, true ).put( actionMap );
		new JumpToNextAction( bw, false ).put( actionMap );
		new JumpToNearest( bw ).put( actionMap );

		for( final GridSource.GRID_TYPE t : GridSource.GRID_TYPE.values())
			new SetWarpVisGridTypeAction( String.format( WARPVISGRID, t.name()), bw, t ).put( actionMap );

		new SetBookmarkAction( bw ).put( actionMap );
		new GoToBookmarkAction( bw ).put( actionMap );
		new GoToBookmarkRotationAction( bw ).put( actionMap );

		new SaveSettingsAction( bw ).put( actionMap );
		new LoadSettingsAction( bw ).put( actionMap );

		new SaveProjectAction( bw ).put( actionMap );
		new LoadProjectAction( bw ).put( actionMap );

		return actionMap;
	}

	public static InputMap createInputMap( final KeyStrokeAdder.Factory keyProperties )
	{
		final InputMap inputMap = new InputMap();
		final KeyStrokeAdder map = keyProperties.keyStrokeAdder( inputMap );

		map.put( SHOW_WARPTYPE_DIALOG, "U" );
		map.put( TOGGLE_LANDMARK_MODE, "SPACE" );

		map.put( BRIGHTNESS_SETTINGS, "S" );
//		map.put( LANDMARK_MODE_ON, "pressed SPACE" );
//		// the few lines below are super ugly, but are necessary for robustness
//		map.put( LANDMARK_MODE_ON, "shift pressed SPACE" );
//		map.put( LANDMARK_MODE_ON, "ctrl pressed SPACE" );
//		map.put( LANDMARK_MODE_ON, "alt pressed SPACE" );
//		map.put( LANDMARK_MODE_ON, "alt ctrl pressed SPACE" );
//		map.put( LANDMARK_MODE_ON, "alt shift pressed SPACE" );
//		map.put( LANDMARK_MODE_ON, "ctrl shift pressed SPACE" );
//		map.put( LANDMARK_MODE_ON, "alt ctrl shift pressed SPACE" );
//
//		map.put( LANDMARK_MODE_OFF, "released SPACE", "released" );
//		// the few lines below are super ugly, but are necessary for robustness
//		map.put( LANDMARK_MODE_OFF, "shift released SPACE", "released" );
//		map.put( LANDMARK_MODE_OFF, "ctrl released SPACE", "released" );
//		map.put( LANDMARK_MODE_OFF, "alt released SPACE", "released" );
//		map.put( LANDMARK_MODE_OFF, "alt ctrl released SPACE", "released" );
//		map.put( LANDMARK_MODE_OFF, "alt shift released SPACE", "released" );
//		map.put( LANDMARK_MODE_OFF, "ctrl shift released SPACE", "released" );
//		map.put( LANDMARK_MODE_OFF, "alt ctrl shift released SPACE", "released" );

		map.put( SHOW_HELP, "F1", "H" );

		map.put( TOGGLE_POINTS_VISIBLE, "V" );
		map.put( TOGGLE_POINT_NAMES_VISIBLE, "N" );
		map.put( ESTIMATE_WARP, "C" );

		map.put( MASK_SIZE_EDIT, "M" );
		map.put( MASK_VIS_TOGGLE, "control M" );

		map.put( UNDO, "control Z" );
		map.put( REDO, "control Y" );
		map.put( REDO, "control shift Z" );

		map.put( SAVE_LANDMARKS, "control S" );
		map.put( QUICK_SAVE_LANDMARKS, "control Q" );
		map.put( LOAD_LANDMARKS, "control O" );

		map.put( EXPORT_IP, "control E" );
//		map.put( SAVE_WARPED, "control alt shift E" );
		map.put( SAVE_WARPED_XML, "control shift E" );

		map.put( CLEAR_SELECTED_MOVING, "BACK_SPACE" );
		map.put( CLEAR_SELECTED_FIXED, "control BACK_SPACE" );
		map.put( DELETE_SELECTED, "DELETE" );

		map.put(  String.format( SELECT_TABLE_ROWS, -1 ), "shift ESCAPE" );

		map.put( TOGGLE_BOX_AND_TEXT_OVERLAY_VISIBLE, "F8" );
		map.put( GARBAGE_COLLECTION, "F9" );
		map.put( PRINT_TRANSFORM, "control shift T" );
		map.put( DEBUG, "F11" );

		return inputMap;
	}

	public static ActionMap createActionMap( final BigWarp< ? > bw )
	{
		final ActionMap actionMap = new ActionMap();

		/*
		 * The below two lines with ui-behavior-1.6.- or so
		 */
//		new LandmarkModeAction( LANDMARK_MODE_ON, bw, true ).put( actionMap );
//		new LandmarkModeAction( LANDMARK_MODE_OFF, bw, false ).put( actionMap );

//		new ToggleLandmarkModeAction( LANDMARK_MODE_ON, bw ).put( actionMap );
//		new ToggleLandmarkModeAction( LANDMARK_MODE_OFF, bw ).put( actionMap );


//		bw.landmarkPopupMenu.deleteSelectedHandler.put( actionMap );
//		bw.landmarkPopupMenu.activateAllHandler.put( actionMap );
//		bw.landmarkPopupMenu.deactivateAllHandler.put( actionMap );
//
//		bw.landmarkPopupMenu.clearAllMoving.put( actionMap );
//		bw.landmarkPopupMenu.clearAllFixed.put( actionMap );

		new ToggleLandmarkModeAction( TOGGLE_LANDMARK_MODE, bw ).put( actionMap );

		new ToggleDialogAction( SHOW_WARPTYPE_DIALOG, bw.warpVisDialog ).put( actionMap );

		new ToggleDialogAction( SHOW_HELP, bw.helpDialog ).put( actionMap );
		new ToggleDialogAction( SHOW_SOURCE_INFO, bw.sourceInfoDialog ).put( actionMap );

		new SaveWarpedAction( bw ).put( actionMap );
		new SaveWarpedXmlAction( bw ).put( actionMap );
		new ExportImagePlusAction( bw ).put( actionMap );
		new ExportWarpAction( bw ).put( actionMap );
		new ExportAffineAction( bw ).put( actionMap );

		new LoadLandmarksAction( bw ).put( actionMap );
		new SaveLandmarksAction( bw ).put( actionMap );
		new QuickSaveLandmarksAction( bw ).put( actionMap );

		new LandmarkGridDialogAction( bw ).put( actionMap );

		new TogglePointsVisibleAction( TOGGLE_POINTS_VISIBLE, bw ).put( actionMap );
		new TogglePointNameVisibleAction( TOGGLE_POINT_NAMES_VISIBLE, bw ).put( actionMap );
		new ToggleBoxAndTexOverlayVisibility( TOGGLE_BOX_AND_TEXT_OVERLAY_VISIBLE, bw ).put( actionMap );
		new ToggleMovingImageDisplayAction( TOGGLE_MOVING_IMAGE_DISPLAY, bw ).put( actionMap );
		new EstimateWarpAction( ESTIMATE_WARP, bw ).put( actionMap );

		// MASK
		new MaskSizeEdit( bw ).put(actionMap);
		new MaskVisToggle( bw ).put(actionMap);
		new MaskImport( bw ).put(actionMap);
		new MaskRemove( bw ).put(actionMap);

		for( int i = 0; i < bw.baseXfmList.length; i++ ){
			final AbstractModel<?> xfm = bw.baseXfmList[ i ];
			new SetWarpMagBaseAction( String.format( WARPMAG_BASE, xfm.getClass().getName()), bw, i ).put( actionMap );
		}

		new UndoRedoAction( UNDO, bw ).put( actionMap );
		new UndoRedoAction( REDO, bw ).put( actionMap );

		new TableSelectionAction( String.format( SELECT_TABLE_ROWS, -1 ), bw.getLandmarkPanel().getJTable(), -1 ).put( actionMap );

		new GarbageCollectionAction( GARBAGE_COLLECTION ).put( actionMap );
		new DebugAction( DEBUG, bw ).put( actionMap );
		new PrintTransformAction( PRINT_TRANSFORM, bw ).put( actionMap );

		return actionMap;
	}

	public static class UndoRedoAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = -5413579107763110117L;

		private BigWarp< ? > bw;
		private boolean isRedo;

		public UndoRedoAction( final String name, BigWarp< ? > bw )
		{
			super( name );
			this.bw = bw;

			isRedo = false;

			if ( name.equals( REDO ) )
				isRedo = true;

		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			// I would love for this check to work instead of using a try-catch
			// bug it doesn't seem to be consistent
//			if( isRedo && manager.canRedo() ){
			try {

				if( isRedo )
				{
					bw.getLandmarkPanel().getTableModel().getUndoManager().redo();
					bw.message.showMessage( "Redo" );
				}
				else
				{
					//			} else if( manager.canUndo() ) {
//					bw.getLandmarkPanel().getTableModel().getUndoManager().
					bw.getLandmarkPanel().getTableModel().getUndoManager().undo();
					bw.message.showMessage( "Undo" );
				}

				/*
				 * Keep the stuff below in the try-catch block to avoid unnecessary calls
				 * if there is nothing to undo/redo
				 */
				if( this.bw.updateWarpOnPtChange )
					this.bw.restimateTransformation();

				// repaint
				this.bw.getLandmarkPanel().repaint();
			}
			catch( final Exception ex )
			{
				if( isRedo )
				{
					bw.message.showMessage("Can't redo");
				}
				else
				{
					bw.message.showMessage("Can't undo");
				}
			}
		}
	}

	public static class LandmarkModeAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 4079013525930019558L;

		private BigWarp< ? > bw;

		private final boolean isOn;

		public LandmarkModeAction( final String name, final BigWarp< ? > bw, final boolean on )
		{
			super( name );
			this.bw = bw;
			this.isOn = on;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			bw.setInLandmarkMode( isOn );
		}
	}

	public static class ToggleLandmarkModeAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 234323425930019L;

		private BigWarp< ? > bw;

		public ToggleLandmarkModeAction( final String name, final BigWarp< ? > bw )
		{
			super( name );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			bw.setInLandmarkMode( !bw.inLandmarkMode );
		}
	}

	public static class ToggleAlwaysEstimateTransformAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 2909830484701853577L;

		private BigWarpViewerFrame bwvp;

		public ToggleAlwaysEstimateTransformAction( final String name, final BigWarpViewerFrame bwvp )
		{
			super( name );
			this.bwvp = bwvp;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			bwvp.getViewerPanel().toggleUpdateOnDrag();
		}
	}

	public static class GarbageCollectionAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = -4487441057212703143L;

		public GarbageCollectionAction( final String name )
		{
			super( name );
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			System.out.println( "GARBAGE COLLECTION" );
			System.gc();
		}
	}

	public static class PrintTransformAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 6065343788485350279L;

		private BigWarp< ? > bw;

		public PrintTransformAction( final String name, final BigWarp< ? > bw )
		{
			super( name );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			bw.getBwTransform().transformToString();
		}
	}
	public static class DebugAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 7408679512565343805L;

		private BigWarp< ? > bw;

		public DebugAction( final String name, final BigWarp< ? > bw )
		{
			super( name );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			System.out.println( "Debug" );

//			System.out.println( "viewerP is Transformed: " + bw.isMovingDisplayTransformed() );
//			LandmarkTableModel ltm = this.bw.getLandmarkPanel().getTableModel();
//			 ltm.printState();
			// ltm.validateTransformPoints();

			// System.out.println( ltm.getChangedSinceWarp() );
//			 System.out.println( ltm.getWarpedPoints() );
//			ltm.printWarpedPoints();

			final AffineTransform3D xfm = new AffineTransform3D();
			bw.viewerP.state().getViewerTransform( xfm );
			System.out.println( "mvg xfm " + xfm  + "   DET = " + BigWarpUtils.det( xfm ));

			bw.viewerQ.state().getViewerTransform( xfm );
			System.out.println( "tgt xfm " + xfm + "   DET = " + BigWarpUtils.det( xfm ));

//			BigWarpData< ? > data = bw.getData();
//			for( int mi : data.movingSourceIndices )
//			{
//				((SourceAndConverter<?>)data.sources.get( mi )).getSpimSource().getSourceTransform( 0, 0, xfm );
//				System.out.println( "mvg src xfm " + xfm  );
//			}
//
//			for( int ti : data.targetSourceIndices )
//			{
//				((SourceAndConverter<?>)data.sources.get( ti )).getSpimSource().getSourceTransform( 0, 0, xfm );
//				System.out.println( "tgt src xfm " + xfm  );
//			}
//
//			System.out.println( " " );
		}
	}

	public static class EstimateWarpAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = -210012348709096037L;

		private BigWarp< ? > bw;

		public EstimateWarpAction( final String name, final BigWarp< ? > bw )
		{
			super( name );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			bw.restimateTransformation();
		}
	}

	public static class ToggleMovingImageDisplayAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 6495981071796613953L;

		private BigWarp< ? > bw;

		public ToggleMovingImageDisplayAction( final String name, final BigWarp< ? > bw )
		{
			super( name );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			bw.toggleMovingImageDisplay();
		}
	}

	public static class TogglePointNameVisibleAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 2639535533224809586L;

		private BigWarp< ? > bw;

		public TogglePointNameVisibleAction( final String name, final BigWarp< ? > bw )
		{
			super( name );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			bw.toggleNameVisibility();
		}
	}

	public static class ToggleBoxAndTexOverlayVisibility extends AbstractNamedAction
	{
		private static final long serialVersionUID = -900781969157241037L;

		private BigWarp< ? > bw;

		public ToggleBoxAndTexOverlayVisibility( final String name, final BigWarp< ? > bw )
		{
			super( name );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
//			bw.getViewerFrameP().getViewerPanel().toggleBoxOverlayVisible();
//			bw.getViewerFrameQ().getViewerPanel().toggleBoxOverlayVisible();
//			bw.getViewerFrameP().getViewerPanel().toggleTextOverlayVisible();
//			bw.getViewerFrameQ().getViewerPanel().toggleTextOverlayVisible();
			Prefs.showTextOverlay(!Prefs.showTextOverlay());
			Prefs.showMultibox(!Prefs.showMultibox());

			bw.getViewerFrameP().repaint();
			bw.getViewerFrameQ().repaint();
		}
	}

	public static class TogglePointsVisibleAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 8747830204501341125L;
		private BigWarp< ? > bw;

		public TogglePointsVisibleAction( final String name, final BigWarp< ? > bw )
		{
			super( name );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			bw.togglePointVisibility();
		}
	}

	public static class ResetActiveViewerAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = -130575800163574517L;

		private BigWarp< ? > bw;

		public ResetActiveViewerAction( final BigWarp< ? > bw )
		{
			super( String.format( RESET_VIEWER ) );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			bw.resetView();
		}
	}

	public static class AlignViewerPanelAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = -7023242695323421450L;

		public enum TYPE { ACTIVE_TO_OTHER, OTHER_TO_ACTIVE }

		private BigWarp< ? >bw;
		private TYPE type;

		public AlignViewerPanelAction( final BigWarp< ? > bw, TYPE type )
		{
			super( String.format( ALIGN_VIEW_TRANSFORMS, type ) );
			this.bw = bw;
			this.type = type;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			if( type == TYPE.ACTIVE_TO_OTHER )
				bw.matchActiveViewerPanelToOther();
			else
				bw.matchOtherViewerPanelToActive();
		}
	}

	public static class SetWarpMagBaseAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 7370813069619338918L;

		private BigWarp< ? > bw;
		private int i;

		public SetWarpMagBaseAction( final String name, final BigWarp< ? > bw, int i )
		{
			super( name );
			this.bw = bw;
			this.i = i;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			bw.setWarpMagBaselineIndex( i );
		}
	}

	public static class SetWarpVisGridTypeAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 7370813069619338918L;

		private final BigWarp< ? > bw;
		private final GridSource.GRID_TYPE type;

		public SetWarpVisGridTypeAction( final String name, final BigWarp< ? > bw, final GridSource.GRID_TYPE type )
		{
			super( name );
			this.bw = bw;
			this.type = type;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			bw.setWarpVisGridType( type );
		}
	}

	public static class SetWarpVisTypeAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 7370813069619338918L;

		private BigWarp< ? > bw;
		private BigWarpViewerFrame p;
		private BigWarp.WarpVisType type;

		public SetWarpVisTypeAction( final BigWarp.WarpVisType type, final BigWarp< ? > bw )
		{
			this( type, bw, null );
		}

		public SetWarpVisTypeAction( final BigWarp.WarpVisType type, final BigWarp< ? > bw, BigWarpViewerFrame p )
		{
			super( getName( type, p ));
			this.bw = bw;
			this.p = p;
			this.type = type;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			if( p == null )
				bw.setWarpVisMode( type, p, true );
			else
				bw.setWarpVisMode( type, p, false );
		}

		public static String getName( final BigWarp.WarpVisType type, BigWarpViewerFrame p )
		{
			if( p == null )
				return String.format( SET_WARPTYPE_VIS, type.name() );
			else if( p.isMoving() )
				return String.format( SET_WARPTYPE_VIS_P, type.name() );
			else
				return String.format( SET_WARPTYPE_VIS_Q, type.name() );
		}
	}

	public static class TableSelectionAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = -4647679094757721276L;

		private final JTable table;
		private final int selection;

		public TableSelectionAction( final String name, JTable table, int selection )
		{
			super( name );
			this.table = table;
			this.selection = selection;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			if ( selection < 0 || selection >= table.getRowCount() )
				table.removeRowSelectionInterval( 0, table.getRowCount() - 1 );
			else
				table.setRowSelectionInterval( selection, selection );
		}
	}

	public static class SetBookmarkAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = -4060308986781809606L;
		BigWarp< ? > bw;

		public SetBookmarkAction( final BigWarp< ? > bw )
		{
			super( SET_BOOKMARK );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			if ( bw.getViewerFrameP().isActive() )
				bw.bookmarkEditorP.initSetBookmark();
			else if ( bw.getViewerFrameQ().isActive() )
				bw.bookmarkEditorQ.initSetBookmark();
		}

	}

	public static class GoToBookmarkAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 8777199828772379323L;
		BigWarp< ? > bw;

		public GoToBookmarkAction( final BigWarp< ? > bw )
		{
			super( GO_TO_BOOKMARK );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			bw.goToBookmark();
		}
	}

	public static class GoToBookmarkRotationAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = -6169895035295179820L;
		BigWarp< ? > bw;

		public GoToBookmarkRotationAction( final BigWarp< ? > bw )
		{
			super( GO_TO_BOOKMARK_ROTATION );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			if ( bw.getViewerFrameP().isActive() )
				bw.bookmarkEditorP.initGoToBookmarkRotation();
			else if ( bw.getViewerFrameP().isActive() )
				bw.bookmarkEditorQ.initGoToBookmarkRotation();
		}
	}

	public static class SaveSettingsAction extends AbstractNamedAction
	{
		BigWarp< ? > bw;
		public SaveSettingsAction( final BigWarp< ? > bw )
		{
			super( SAVE_SETTINGS );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			bw.saveSettings();
		}

		private static final long serialVersionUID = 1L;
	}

	public static class SaveProjectAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = -965388576691467002L;
		BigWarp< ? > bw;
		public SaveProjectAction( final BigWarp< ? > bw )
		{
			super( SAVE_PROJECT );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			bw.saveProject();
		}
	}

	public static class LoadSettingsAction extends AbstractNamedAction
	{
		BigWarp< ? > bw;
		public LoadSettingsAction( final BigWarp< ? > bw )
		{
			super( LOAD_SETTINGS );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			bw.loadSettings();
		}

		private static final long serialVersionUID = 1L;
	}

	public static class LoadProjectAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 1793182816804229398L;
		BigWarp< ? > bw;
		public LoadProjectAction( final BigWarp< ? > bw )
		{
			super( LOAD_PROJECT );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			bw.loadProject();
		}
	}

	public static class WarpToSelectedAction extends AbstractNamedAction
	{
		final BigWarp< ? > bw;

		public WarpToSelectedAction( final BigWarp< ? > bw )
		{
			super( JUMP_TO_SELECTED_POINT );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			final int[] selectedRows =  bw.getLandmarkPanel().getJTable().getSelectedRows();

			int row = 0;
			if( selectedRows.length > 0 )
				row = selectedRows[ 0 ];

			if( bw.getViewerFrameP().isActive() )
				bw.jumpToLandmark( row, bw.getViewerFrameP().getViewerPanel() );
			else
				bw.jumpToLandmark( row, bw.getViewerFrameQ().getViewerPanel() );
		}

		private static final long serialVersionUID = 5233843444920094805L;
	}

	public static class JumpToNearest extends AbstractNamedAction
	{
		final BigWarp< ? > bw;
		public JumpToNearest( final BigWarp< ? > bw )
		{
			super( JUMP_TO_NEAREST_POINT );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			if( bw.getViewerFrameP().isActive() )
				bw.jumpToNearestLandmark( bw.getViewerFrameP().getViewerPanel() );
			else
				bw.jumpToNearestLandmark( bw.getViewerFrameQ().getViewerPanel() );
		}
		private static final long serialVersionUID = 3244181492305479433L;
	}

	public static class JumpToNextAction extends AbstractNamedAction
	{
		final BigWarp< ? > bw;
		final int inc;

		public JumpToNextAction( final BigWarp< ? > bw, boolean fwd )
		{
			super( String.format( JUMP_TO_NEXT_POINT, fwd) );
			this.bw = bw;
			if( fwd )
				inc = 1;
			else
				inc = -1;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			if ( bw.landmarkModel.getRowCount() < 1 )
			{
				bw.message.showMessage( "No landmarks found." );
				return;
			}

			final int[] selectedRows =  bw.getLandmarkPanel().getJTable().getSelectedRows();

			int row = 0;
			if( selectedRows.length > 0 )
				row = selectedRows[ selectedRows.length - 1 ];

			row = row + inc; // increment to get the *next* row

			// wrap to start if necessary
			if( row >= bw.getLandmarkPanel().getTableModel().getRowCount() )
				row = 0;
			else if( row < 0 )
				row = bw.getLandmarkPanel().getTableModel().getRowCount() - 1;

			// select new row
			bw.getLandmarkPanel().getJTable().setRowSelectionInterval( row, row );

			if( bw.getViewerFrameP().isActive() )
			{
				bw.jumpToLandmark( row, bw.getViewerFrameP().getViewerPanel() );
			}
			else
			{
				bw.jumpToLandmark( row, bw.getViewerFrameQ().getViewerPanel() );
			}
		}
		private static final long serialVersionUID = 8515568118251877405L;
	}

	public static class LoadLandmarksAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = -5405137757290988030L;
		BigWarp< ? > bw;
		public LoadLandmarksAction( final BigWarp< ? > bw )
		{
			super( LOAD_LANDMARKS );
			this.bw = bw;
		}
		@Override
		public void actionPerformed( ActionEvent e )
		{
			System.out.println("load landmarks");
			bw.loadLandmarks();
		}
	}

	public static class QuickSaveLandmarksAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = -4761309639234262643L;
		BigWarp< ? > bw;
		public QuickSaveLandmarksAction( final BigWarp< ? > bw )
		{
			super( QUICK_SAVE_LANDMARKS );
			this.bw = bw;
		}
		@Override
		public void actionPerformed( ActionEvent e )
		{
			bw.quickSaveLandmarks();
		}
	}

	public static class SaveLandmarksAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 7897687176745034315L;
		BigWarp< ? > bw;
		public SaveLandmarksAction( final BigWarp< ? > bw )
		{
			super( SAVE_LANDMARKS );
			this.bw = bw;
		}
		@Override
		public void actionPerformed( ActionEvent e )
		{
			bw.saveLandmarks();
		}
	}

	public static class ExportImagePlusAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = -8109832912959931917L;
		BigWarp< ? > bw;
		public ExportImagePlusAction( final BigWarp< ? > bw )
		{
			super( EXPORT_IP );
			this.bw = bw;
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			bw.exportAsImagePlus( false );
		}
	}

	public static class ExportWarpAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 4626378501415886468L;
		BigWarp< ? > bw;
		public ExportWarpAction( final BigWarp< ? > bw )
		{
			super( EXPORT_WARP );
			this.bw = bw;
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			bw.exportWarpField();
		}
	}

	public static class ExportAffineAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 9190515918045510236L;
		BigWarp< ? > bw;
		public ExportAffineAction( final BigWarp< ? > bw )
		{
			super( EXPORT_AFFINE );
			this.bw = bw;
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			bw.getBwTransform().printAffine();
		}
	}

	@Deprecated
	public static class SaveWarpedAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 4965249994677649713L;

		BigWarp< ? > bw;
		public SaveWarpedAction( final BigWarp< ? > bw )
		{
			super( SAVE_WARPED );
			this.bw = bw;
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			bw.saveMovingImageToFile();
		}
	}

	public static class SaveWarpedXmlAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = -5437508072904256758L;

		BigWarp< ? > bw;
		public SaveWarpedXmlAction( final BigWarp< ? > bw )
		{
			super( SAVE_WARPED_XML );
			this.bw = bw;
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			bw.saveMovingImageXml();
		}
	}

	public static class LandmarkGridDialogAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 1L;
		BigWarp< ? > bw;

		public LandmarkGridDialogAction( final BigWarp< ? > bw )
		{
			super( LANDMARK_GRID_DIALOG );
			this.bw = bw;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			System.out.println( "LandmarkGridGenerator.fillFromDialog( bw )" );
			LandmarkGridGenerator.fillFromDialog( bw );
		}
	}

	public static class MaskSizeEdit extends AbstractNamedAction
	{
		private static final long serialVersionUID = -7918625162439713732L;
		private final BigWarp< ? > bw;

		public MaskSizeEdit( final BigWarp< ? > bw )
		{
			super( MASK_SIZE_EDIT );
			this.bw = bw;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			if( bw.maskSourceMouseListenerQ != null )
				bw.maskSourceMouseListenerQ.toggleActive();
		}
	}

	public static class MaskVisToggle extends AbstractNamedAction
	{
		private static final long serialVersionUID = 493457851797644046L;
		private final BigWarp< ? > bw;

		public MaskVisToggle( final BigWarp< ? > bw )
		{
			super( MASK_VIS_TOGGLE );
			this.bw = bw;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			bw.getViewerFrameQ().getViewerPanel().getMaskOverlay().toggleVisible();
		}
	}

	public static class MaskImport extends AbstractNamedAction
	{
		private static final long serialVersionUID = 493457851797644046L;
		private final BigWarp< ? > bw;

		public MaskImport( final BigWarp< ? > bw )
		{
			super( MASK_IMPORT );
			this.bw = bw;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			bw.importTransformMaskSourceDialog();
		}
	}

	public static class MaskRemove extends AbstractNamedAction
	{
		private static final long serialVersionUID = 4103338122650843631L;
		private final BigWarp< ? > bw;

		public MaskRemove( final BigWarp< ? > bw )
		{
			super( MASK_REMOVE );
			this.bw = bw;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			bw.removeMaskSource();
		}
	}

}
