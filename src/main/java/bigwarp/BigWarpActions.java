package bigwarp;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.TableCellEditor;
import javax.swing.undo.UndoManager;

import mpicbg.models.AbstractModel;
import bdv.gui.BigWarpViewerFrame;
import bdv.tools.ToggleDialogAction;
import bdv.util.AbstractNamedAction;
import bdv.util.AbstractNamedAction.NamedActionAdder;
import bdv.util.KeyProperties;
import bdv.util.KeyProperties.KeyStrokeAdder;
import bdv.viewer.InputActionBindings;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.source.GridSource;

public class BigWarpActions
{
	public static final String TOGGLE_LANDMARK_MODE  = "toggle landmark mode";
	public static final String TOGGLE_POINTS_VISIBLE  = "toggle points visible";
	public static final String TOGGLE_POINT_NAMES_VISIBLE  = "toggle point names visible";
	public static final String TOGGLE_MOVING_IMAGE_DISPLAY = "toggle moving image display";
	public static final String ESTIMATE_WARP = "estimate warp";
	public static final String TOGGLE_ALWAYS_ESTIMATE_WARP = "toggle always estimate warp";
	
//	public static final String TOGGLE_WARP_VIS = "toggle warp vis";
//	public static final String TOGGLE_WARPMAG_VIS_P = "toggle warp magnitude p";
//	public static final String TOGGLE_WARPMAG_VIS_Q = "toggle warp magnitude q";
	
	public static final String SHOW_WARPTYPE_DIALOG = "show warp vis dialog" ;
	public static final String SET_WARPTYPE_VIS = "set warp vis type %s" ;
	public static final String SET_WARPTYPE_VIS_P = "p " + SET_WARPTYPE_VIS;
	public static final String SET_WARPTYPE_VIS_Q = "q " + SET_WARPTYPE_VIS;
	
	public static final String WARPMAG_BASE = "set warpmag base %s";
	public static final String WARPVISGRID = "set warp vis grid %s";
	public static final String WARPVISDIALOG = "warp vis dialog";
	
	public static final String RESET_VIEWER = "reset active viewer";
	public static final String ALIGN_VIEW_TRANSFORMS = "align view transforms %s";
	public static final String BRIGHTNESS_SETTINGS = "brightness settings";
	public static final String VISIBILITY_AND_GROUPING = "visibility and grouping";
	public static final String SHOW_HELP = "help";
	public static final String CROP = "crop";
	public static final String SAVE_SETTINGS = "save settings";
	public static final String LOAD_SETTINGS = "load settings";

	public static final String UNDO = "undo";
	public static final String REDO = "redo";
	
	public static final String DEBUG = "debug";
	public static final String GARBAGE_COLLECTION = "garbage collection";

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
		inputActionBindings.addActionMap( "bw", createActionMap( bdv ) );
		inputActionBindings.addInputMap( "bw", createInputMap( keyProperties ) );
	}
	
	public static void installLandmarkPanelActionBindings(
			final InputActionBindings inputActionBindings,
			final BigWarp bw,
			final JTable landmarkTable,
			final KeyProperties keyProperties )
	{
		final InputMap inputMap = new InputMap();
		final KeyStrokeAdder map = keyProperties.adder( inputMap );

		map.put( TOGGLE_LANDMARK_MODE, "SPACE" );
		map.put( SHOW_HELP, "F1", "H" );
		
		final ActionMap actionMap = new ActionMap();
		final NamedActionAdder actionAdder = new NamedActionAdder( actionMap );
		actionAdder.put( new ToggleDialogAction( SHOW_HELP, bw.helpDialog ) );
		actionAdder.put( new ToggleLandmarkModeAction( TOGGLE_LANDMARK_MODE, bw ));
		
		inputActionBindings.addInputMap( "w", inputMap );
		inputActionBindings.addActionMap( "bw", actionMap );
		
		TableCellEditor celled = landmarkTable.getCellEditor( 0, 1 );
		Component c = celled.getTableCellEditorComponent(landmarkTable, new Boolean(true), true, 0, 1 );
		
		InputMap parentInputMap = ((JCheckBox)c).getInputMap().getParent();
		parentInputMap.clear();
		KeyStroke enterDownKS = KeyStroke.getKeyStroke("pressed ENTER" );
		KeyStroke enterUpKS = KeyStroke.getKeyStroke("released ENTER" );
		
		parentInputMap.put( enterDownKS, "pressed" );
		parentInputMap.put(   enterUpKS, "released" );
	}
	
	public static InputMap createInputMap( final KeyProperties keyProperties )
	{
		final InputMap inputMap = new InputMap();
		final KeyStrokeAdder map = keyProperties.adder( inputMap );

		map.put( SHOW_WARPTYPE_DIALOG, "G" );
		map.put( VISIBILITY_AND_GROUPING, "F6" );
		map.put( TOGGLE_LANDMARK_MODE, "SPACE" );
		map.put( BRIGHTNESS_SETTINGS, "S" );
		map.put( SHOW_HELP, "F1", "H" );

		map.put( TOGGLE_ALWAYS_ESTIMATE_WARP, "F3" );
		map.put( TOGGLE_POINTS_VISIBLE, "V" );
		map.put( TOGGLE_POINT_NAMES_VISIBLE, "N" );
		map.put( TOGGLE_MOVING_IMAGE_DISPLAY, "T" );
		map.put( ESTIMATE_WARP, "C" );
		
		map.put(RESET_VIEWER, "R");
		map.put( String.format( ALIGN_VIEW_TRANSFORMS, AlignViewerPanelAction.TYPE.OTHER_TO_ACTIVE ), "Q" );
		map.put( String.format( ALIGN_VIEW_TRANSFORMS, AlignViewerPanelAction.TYPE.ACTIVE_TO_OTHER ), "W" );
		
		map.put( UNDO, "control Z" );
		map.put( REDO, "control Y" );
		
		map.put( GARBAGE_COLLECTION, "F9" );
		map.put( DEBUG, "F8" );
		
		return inputMap;
	}

	public static ActionMap createActionMap( final BigWarp bw )
	{
		final ActionMap actionMap = new ActionMap();
		final NamedActionAdder map = new NamedActionAdder( actionMap );

		map.put( new ToggleDialogAction( VISIBILITY_AND_GROUPING, bw.activeSourcesDialog ) );
		map.put( new ToggleDialogAction( SHOW_WARPTYPE_DIALOG, bw.warpVisDialog ) );
		map.put( new ToggleLandmarkModeAction( TOGGLE_LANDMARK_MODE, bw ));
		
//		map.put( new SetWarpVisTypeAction( TOGGLE_WARP_VIS, bw ));
//		map.put( new SetWarpVisTypeAction( TOGGLE_WARPMAG_VIS_P, bw, bw.getViewerFrameP() ));
//		map.put( new SetWarpVisTypeAction( TOGGLE_WARPMAG_VIS_Q, bw, bw.getViewerFrameQ() ));
		
		for( BigWarp.WarpVisType t: BigWarp.WarpVisType.values())
		{
			map.put( new SetWarpVisTypeAction( t, bw ));
			map.put( new SetWarpVisTypeAction( t, bw, bw.getViewerFrameP() ));
			map.put( new SetWarpVisTypeAction( t, bw, bw.getViewerFrameQ() ));
		}
		
		map.put( new ToggleDialogAction( BRIGHTNESS_SETTINGS, bw.brightnessDialog ) );
		map.put( new ToggleDialogAction( SHOW_HELP, bw.helpDialog ) );

		map.put( new ToggleAlwaysEstimateTransformAction( TOGGLE_ALWAYS_ESTIMATE_WARP, bw ));
		map.put( new TogglePointsVisibleAction( TOGGLE_POINTS_VISIBLE, bw ));
		map.put( new TogglePointNameVisibleAction( TOGGLE_POINT_NAMES_VISIBLE, bw ));
		map.put( new ToggleMovingImageDisplayAction( TOGGLE_MOVING_IMAGE_DISPLAY, bw ));
		map.put( new EstimateWarpAction( ESTIMATE_WARP, bw ));

		map.put( new ResetActiveViewerAction( bw ));
		map.put( new AlignViewerPanelAction( bw, AlignViewerPanelAction.TYPE.ACTIVE_TO_OTHER ) );
		map.put( new AlignViewerPanelAction( bw, AlignViewerPanelAction.TYPE.OTHER_TO_ACTIVE ) );
		
		for( int i = 0; i < bw.baseXfmList.length; i++ ){
			AbstractModel<?> xfm = bw.baseXfmList[ i ];
			map.put( new SetWarpMagBaseAction( String.format( WARPMAG_BASE, xfm.getClass().getName()), bw, i ));
		}
		
		for( GridSource.GRID_TYPE t : GridSource.GRID_TYPE.values())
			map.put( new SetWarpVisGridTypeAction( String.format( WARPVISGRID, t.name()), bw, t ));
		
		map.put( new UndoRedoAction( UNDO, bw ) );
		map.put( new UndoRedoAction( REDO, bw ) );
		
		map.put( new GarbageCollectionAction( GARBAGE_COLLECTION ) );
		map.put( new DebugAction( DEBUG, bw ) );
			
		return actionMap;
	}

	private BigWarpActions(){}

	public static class UndoRedoAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = -5413579107763110117L;

		private BigWarp bw;
		private boolean isRedo;

		public UndoRedoAction( final String name, BigWarp bw )
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
//			UndoManager manager = bw.getLandmarkPanel().getTableModel().getUndoManager();
			
//			if( isRedo )
//				System.out.println( "can redo: " + manager.canRedo() );
//			else
//				System.out.println( "can undo: " + manager.canUndo() );
			
//			if( isRedo && manager.canRedo() ){
			try { 
				
				if( isRedo ) {
					bw.getLandmarkPanel().getTableModel().getUndoManager().preProcessRedo();
					bw.getLandmarkPanel().getTableModel().getUndoManager().redo();
					bw.getViewerFrameP().getViewerPanel().showMessage( "Redo" );
					bw.getViewerFrameQ().getViewerPanel().showMessage( "Redo" );
				}else{ 
					bw.getLandmarkPanel().getTableModel().getUndoManager().preProcessUndo();
					//			} else if( manager.canUndo() ) {
//					bw.getLandmarkPanel().getTableModel().getUndoManager().
					bw.getLandmarkPanel().getTableModel().getUndoManager().undo();
					bw.getViewerFrameP().getViewerPanel().showMessage( "Undo" );
					bw.getViewerFrameQ().getViewerPanel().showMessage( "Undo" );
				}
			}catch( Exception ex ){
				System.err.println( " Undo / redo error " );
				//ex.printStackTrace();
			}
			//System.out.println( "  can redo: " + manager.canRedo() );
			//System.out.println( "  can undo: " + manager.canUndo() );

			this.bw.restimateTransformation();

			// if there's something to do after resestimation, then do it now 
			if( isRedo ) 
			{
//				bw.getLandmarkPanel().getTableModel().getUndoManager().postProcess();
			}
			else
			{
				bw.getLandmarkPanel().getTableModel().getUndoManager().postProcess();
			}
			// repaint
			this.bw.getLandmarkPanel().repaint();

		}
	}

	public static class ToggleAlwaysEstimateTransformAction extends AbstractNamedAction 
	{
		private static final long serialVersionUID = 2909830484701853577L;

		private BigWarp bw;

		public ToggleAlwaysEstimateTransformAction( final String name, final BigWarp bw )
		{
			super( name );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			bw.toggleUpdateWarpOnChange();
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
	
	public static class DebugAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 7408679512565343805L;

		private BigWarp bw;

		public DebugAction( final String name, final BigWarp bw )
		{
			super( name );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			System.out.println( "Debug" );

			LandmarkTableModel ltm = this.bw.getLandmarkPanel().getTableModel();
			// ltm.printState();
			// ltm.validateTransformPoints();

			// System.out.println( ltm.getChangedSinceWarp() );
			// System.out.println( ltm.getWarpedPoints() );
			ltm.printWarpedPoints();

			System.out.println( " " );
		}
	}

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
	
	public static class EstimateWarpAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = -210012348709096037L;
		
		private BigWarp bw;
		
		public EstimateWarpAction( final String name, final BigWarp bw )
		{
			super( name );
			this.bw = bw;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			if( bw.isInLandmarkMode())
				bw.restimateTransformation();
		}
	}
	
	public static class ToggleMovingImageDisplayAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 6495981071796613953L;
		
		private BigWarp bw;
		
		public ToggleMovingImageDisplayAction( final String name, final BigWarp bw )
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
		
		private BigWarp bw;
		
		public TogglePointNameVisibleAction( final String name, final BigWarp bw )
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
	
	public static class TogglePointsVisibleAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 8747830204501341125L;
		private BigWarp bw;
		
		public TogglePointsVisibleAction( final String name, final BigWarp bw )
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
		
		private BigWarp bw;
		
		public ResetActiveViewerAction( final BigWarp bw )
		{
			super( String.format( RESET_VIEWER ) );
			this.bw = bw;
		}
		
		public void actionPerformed( ActionEvent e )
		{
			bw.resetView();
		}
	}
	
	public static class AlignViewerPanelAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = -7023242695323421450L;
		
		public enum TYPE { ACTIVE_TO_OTHER, OTHER_TO_ACTIVE };
		
		private BigWarp bw;
		private TYPE type;
		
		public AlignViewerPanelAction( final BigWarp bw, TYPE type )
		{
			super( String.format( ALIGN_VIEW_TRANSFORMS, type ) );
			this.bw = bw;
			this.type = type;
		}
		
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
		
		private BigWarp bw;
		private int i;
		
		public SetWarpMagBaseAction( final String name, final BigWarp bw, int i )
		{
			super( name );
			this.bw = bw;
			this.i = i;
		}

		@Override
		public void actionPerformed( ActionEvent e )
		{
			bw.setWarpMagBaseline( i );
		}
	}
	
	public static class SetWarpVisGridTypeAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 7370813069619338918L;
		
		private final BigWarp bw;
		private final GridSource.GRID_TYPE type;
		
		public SetWarpVisGridTypeAction( final String name, final BigWarp bw, final GridSource.GRID_TYPE type )
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
		
		private BigWarp bw;
		private BigWarpViewerFrame p;
		private BigWarp.WarpVisType type;
		
		public SetWarpVisTypeAction( final BigWarp.WarpVisType type, final BigWarp bw )
		{
			this( type, bw, null );
		}
		
		public SetWarpVisTypeAction( final BigWarp.WarpVisType type, final BigWarp bw, BigWarpViewerFrame p )
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
	
}
