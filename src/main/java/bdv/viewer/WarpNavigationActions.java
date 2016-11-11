package bdv.viewer;

import java.awt.event.ActionEvent;

import javax.swing.ActionMap;
import javax.swing.InputMap;

import org.scijava.ui.behaviour.KeyStrokeAdder;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.InputActionBindings;

import bdv.viewer.ViewerPanel.AlignPlane;

public class WarpNavigationActions 
{

	public static final String TOGGLE_INTERPOLATION = "toggle interpolation";
	public static final String TOGGLE_FUSED_MODE = "toggle fused mode";
	public static final String TOGGLE_GROUPING = "toggle grouping";
	public static final String SET_CURRENT_SOURCE = "set current source %d";
	public static final String TOGGLE_SOURCE_VISIBILITY = "toggle source visibility %d";
	
	public static final String ALIGN_PLANE = "align %s plane";
	public static final String ROTATE_PLANE = "rotate %s";
	

	public static final String DISPLAY_XFMS = "display transforms";
	
	public static enum rotationDirections2d { CLOCKWISE, COUNTERCLOCKWISE }; 
	
	/**
	 * Create navigation actions and install them in the specified
	 * {@link InputActionBindings}.
	 *
	 * @param inputActionBindings
	 *            {@link InputMap} and {@link ActionMap} are installed here.
	 * @param viewer
	 *            Navigation actions are targeted at this {@link ViewerPanel}.
	 * @param keyProperties
	 *            user-defined key-bindings.
	 * @param is2d does this bigwarp instance work on 2d images 
	 */
	public static void installActionBindings(
			final InputActionBindings inputActionBindings,
			final BigWarpViewerPanel viewer,
			final KeyStrokeAdder.Factory keyProperties,
			final boolean is2d )
	{
		inputActionBindings.addActionMap( "navigation", createActionMap( viewer ) );
		inputActionBindings.addInputMap( "navigation", createInputMap( keyProperties, is2d ) );
		
		viewer.getActionMap().get( "navigation" );
	}

	public static InputMap createInputMap( final KeyStrokeAdder.Factory keyProperties, final boolean is2d )
	{
		final InputMap inputMap = new InputMap();
		final KeyStrokeAdder map = keyProperties.keyStrokeAdder( inputMap );

		map.put( TOGGLE_INTERPOLATION, "I" );
		map.put( TOGGLE_FUSED_MODE, "F" );
		map.put( TOGGLE_GROUPING, "G" );

		final String[] numkeys = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };
		for ( int i = 0; i < numkeys.length; ++i )
		{
			map.put( String.format( SET_CURRENT_SOURCE, i ), numkeys[ i ] );
			map.put( String.format( TOGGLE_SOURCE_VISIBILITY, i ), "shift " + numkeys[ i ] );
		}

		map.put( DISPLAY_XFMS, "shift P" );
		
		if( !is2d )
		{
			map.put( String.format( ALIGN_PLANE, AlignPlane.XY ), "shift Z" );
			map.put( String.format( ALIGN_PLANE, AlignPlane.ZY ), "shift X" );
			map.put( String.format( ALIGN_PLANE, AlignPlane.XZ ), "shift Y", "shift A" );
		}
		else
		{
			map.put( String.format( ROTATE_PLANE, rotationDirections2d.CLOCKWISE.name() ), "shift X" );
			map.put( String.format( ROTATE_PLANE, rotationDirections2d.COUNTERCLOCKWISE.name() ), "shift Z" );
		}
		return inputMap;
	}

	public static ActionMap createActionMap( final BigWarpViewerPanel viewer )
	{
		return createActionMap( viewer, 10 );
	}

	public static ActionMap createActionMap( final BigWarpViewerPanel viewer, final int numSourceKeys )
	{
		final ActionMap actionMap = new ActionMap();
		addToActionMap( actionMap, viewer, numSourceKeys );
		return actionMap;
	}

	public static void addToActionMap( final ActionMap actionMap, final BigWarpViewerPanel viewer, final int numSourceKeys )
	{
		new ToggleInterPolationAction( viewer ).put( actionMap );
		new ToggleFusedModeAction( viewer ).put( actionMap );
		new ToggleGroupingAction( viewer ).put( actionMap );

		for ( int i = 0; i < numSourceKeys; ++i )
		{
			new SetCurrentSourceOrGroupAction( viewer, i ).put( actionMap );
			new ToggleSourceOrGroupVisibilityAction( viewer, i ).put( actionMap );
		}

		for ( final AlignPlane plane : AlignPlane.values() )
			new AlignPlaneAction( viewer, plane ).put( actionMap );

		new RotatePlaneAction( viewer, rotationDirections2d.CLOCKWISE ).put( actionMap ); // clockwise
		new RotatePlaneAction( viewer, rotationDirections2d.COUNTERCLOCKWISE ).put( actionMap ); // counterclockwise

		new DisplayXfmAction( viewer ).put( actionMap );
	}

	private static abstract class NavigationAction extends AbstractNamedAction
	{
		protected final ViewerPanel viewer;

		public NavigationAction( final String name, final ViewerPanel viewer )
		{
			super( name );
			this.viewer = viewer;
		}

		private static final long serialVersionUID = 1L;
	}

	public static class ToggleInterPolationAction extends NavigationAction
	{
		public ToggleInterPolationAction( final ViewerPanel viewer )
		{
			super( TOGGLE_INTERPOLATION, viewer );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			viewer.toggleInterpolation();
		}

		private static final long serialVersionUID = 1L;
	}

	public static class ToggleFusedModeAction extends NavigationAction
	{
		public ToggleFusedModeAction( final ViewerPanel viewer )
		{
			super( TOGGLE_FUSED_MODE, viewer );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			viewer.getVisibilityAndGrouping().setFusedEnabled( !viewer.visibilityAndGrouping.isFusedEnabled() );
		}

		private static final long serialVersionUID = 1L;
	}

	public static class ToggleGroupingAction extends NavigationAction
	{
		public ToggleGroupingAction( final ViewerPanel viewer )
		{
			super( TOGGLE_GROUPING, viewer );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			viewer.getVisibilityAndGrouping().setGroupingEnabled( !viewer.visibilityAndGrouping.isGroupingEnabled() );
		}

		private static final long serialVersionUID = 1L;
	}

	public static class SetCurrentSourceOrGroupAction extends NavigationAction
	{
		private final int sourceIndex;

		public SetCurrentSourceOrGroupAction( final ViewerPanel viewer, final int sourceIndex )
		{
			super( String.format( SET_CURRENT_SOURCE, sourceIndex ), viewer );
			this.sourceIndex = sourceIndex;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			viewer.getVisibilityAndGrouping().setCurrentGroupOrSource( sourceIndex );
		}

		private static final long serialVersionUID = 1L;
	}

	public static class ToggleSourceOrGroupVisibilityAction extends NavigationAction
	{
		private final int sourceIndex;

		public ToggleSourceOrGroupVisibilityAction( final ViewerPanel viewer, final int sourceIndex )
		{
			super( String.format( TOGGLE_SOURCE_VISIBILITY, sourceIndex ), viewer );
			this.sourceIndex = sourceIndex;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			viewer.getVisibilityAndGrouping().toggleActiveGroupOrSource( sourceIndex );
		}

		private static final long serialVersionUID = 1L;
	}
	
	public static class DisplayXfmAction extends NavigationAction
	{
		
		public DisplayXfmAction( final BigWarpViewerPanel viewer )
		{
			super( DISPLAY_XFMS, viewer );
		}

		@Override
		public void actionPerformed( final ActionEvent e ) 
		{
			((BigWarpViewerPanel) viewer).displayViewerTransforms();
		}
		
		private static final long serialVersionUID = 1L;
	}

	public static class RotatePlaneAction extends NavigationAction
	{
		private rotationDirections2d direction;
		
		public RotatePlaneAction( final BigWarpViewerPanel viewer, final rotationDirections2d direction )
		{
			super( String.format( ROTATE_PLANE, direction.name() ), viewer );
			this.direction = direction;
		}

		@Override
		public void actionPerformed( final ActionEvent e ) 
		{
			((BigWarpViewerPanel) viewer).rotateView2d( direction == rotationDirections2d.CLOCKWISE );
		}
		
		private static final long serialVersionUID = 1L;
	}
	
	public static class AlignPlaneAction extends NavigationAction
	{
		private final AlignPlane plane;

		public AlignPlaneAction( final ViewerPanel viewer, final AlignPlane plane )
		{
			super( String.format( ALIGN_PLANE, plane.getName() ), viewer );
			this.plane = plane;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			viewer.align( plane );
		}

		private static final long serialVersionUID = 1L;
	}
	
	
	
	private WarpNavigationActions()
	{}
}
