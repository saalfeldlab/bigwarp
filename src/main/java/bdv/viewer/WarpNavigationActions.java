package bdv.viewer;

import java.awt.event.ActionEvent;

import java.util.ArrayList;
import java.util.List;
import javax.swing.ActionMap;
import javax.swing.InputMap;

import org.scijava.ui.behaviour.KeyStrokeAdder;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.InputActionBindings;

import bdv.gui.BigWarpViewerFrame;
import bdv.viewer.ViewerPanel.AlignPlane;

public class WarpNavigationActions extends Actions
{

	public static final String TOGGLE_INTERPOLATION = "toggle interpolation";
	public static final String TOGGLE_FUSED_MODE = "toggle fused mode";
	public static final String TOGGLE_GROUPING = "toggle grouping";
	public static final String SET_CURRENT_SOURCE = "set current source %d";
	public static final String TOGGLE_SOURCE_VISIBILITY = "toggle source visibility %d";
	public static final String ALIGN_PLANE = "align %s plane";
	public static final String ROTATE_PLANE = "rotate %s";
	public static final String DISPLAY_XFMS = "display transforms";
	public static final String EXPAND_CARDS = "expand and focus cards panel";
	public static final String COLLAPSE_CARDS = "collapse cards panel";

	public static final String[] EXPAND_CARDS_KEYS         = new String[] { "P" };
	public static final String[] COLLAPSE_CARDS_KEYS       = new String[] { "shift P", "shift ESCAPE" };
	
	public static enum rotationDirections2d { CLOCKWISE, COUNTERCLOCKWISE }; 

	public WarpNavigationActions( final KeyStrokeAdder.Factory keyConfig )
	{
		super( keyConfig, new String[] { "bw_warpNav" } );
	}	
	
	/**
	 * Create navigation actions and install them in the specified
	 * {@link InputActionBindings}.
	 *
	 * @param inputActionBindings
	 *            {@link InputMap} and {@link ActionMap} are installed here.
	 * @param frame
	 *            Navigation actions are targeted at this {@link BigWarpViewerFrame}.
	 * @param keyProperties
	 *            user-defined key-bindings.
	 * @param is2d does this bigwarp instance work on 2d images 
	 */
	public static void installActionBindings(
			final InputActionBindings inputActionBindings,
			final BigWarpViewerFrame frame,
			final KeyStrokeAdder.Factory keyProperties,
			final boolean is2d )
	{
		final ActionMap actionMap = createActionMap( frame );
		final InputMap inputMap = createInputMap( keyProperties, is2d );

		Actions actions = new Actions( inputMap, actionMap, keyProperties, "navigation" );
//		actions.runnableAction( viewer::expandAndFocusCardPanel, EXPAND_CARDS, EXPAND_CARDS_KEYS );
//		actions.runnableAction( viewer::collapseCardPanel, COLLAPSE_CARDS, COLLAPSE_CARDS_KEYS );
		actions.install( inputActionBindings, "navigation" );
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

		map.put( DISPLAY_XFMS, "shift V" );
		
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

		map.put( EXPAND_CARDS, EXPAND_CARDS_KEYS );
		map.put( COLLAPSE_CARDS, COLLAPSE_CARDS_KEYS );

		return inputMap;
	}

	public static ActionMap createActionMap( final BigWarpViewerFrame frame )
	{
		return createActionMap( frame, 10 );
	}

	public static ActionMap createActionMap( final BigWarpViewerFrame frame, final int numSourceKeys )
	{
		final ActionMap actionMap = new ActionMap();
		addToActionMap( actionMap, frame, numSourceKeys );
		return actionMap;
	}

	public static void addToActionMap( final ActionMap actionMap, final BigWarpViewerFrame frame, final int numSourceKeys )
	{
		final BigWarpViewerPanel viewer = frame.getViewerPanel();

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

		new ExpandCardAction( frame ).put( actionMap );
		new CollapseCardAction( frame ).put( actionMap );
	}

	private static abstract class NavigationAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 1607667614920666360L;

		protected final ViewerPanel viewer;

		public NavigationAction( final String name, final ViewerPanel viewer )
		{
			super( name );
			this.viewer = viewer;
		}
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
			final ViewerState state = viewer.state();
			final DisplayMode mode = state.getDisplayMode();
			state.setDisplayMode( mode.withFused( !mode.hasFused() ) );
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
			final ViewerState state = viewer.state();
			final DisplayMode mode = state.getDisplayMode();
			state.setDisplayMode( mode.withGrouping( !mode.hasGrouping() ) );
		}

		private static final long serialVersionUID = 1L;
	}

	public static class SetCurrentSourceOrGroupAction extends NavigationAction
	{
		private final int index;

		public SetCurrentSourceOrGroupAction( final ViewerPanel viewer, final int index )
		{
			super( String.format( SET_CURRENT_SOURCE, index ), viewer );
			this.index = index;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			final ViewerState state = viewer.state();
			synchronized ( state )
			{
				if ( state.getDisplayMode().hasGrouping() )
				{
					final List< SourceGroup > groups = state.getGroups();
					if ( index >= 0 && index < groups.size() )
					{
						final SourceGroup group = groups.get( index );
						state.setCurrentGroup( group );
						final List< SourceAndConverter< ? > > sources = new ArrayList<>( state.getSourcesInGroup( group ) );
						if ( !sources.isEmpty() )
						{
							sources.sort( state.sourceOrder() );
							state.setCurrentSource( sources.get( 0 ) );
						}
					}
				}
				else
				{
					final List< SourceAndConverter< ? > > sources = state.getSources();
					if ( index >= 0 && index < sources.size() )
						state.setCurrentSource( sources.get( index ) );
				}
			}
		}

		private static final long serialVersionUID = 1L;
	}

	public static class ToggleSourceOrGroupVisibilityAction extends NavigationAction
	{
		private final int index;

		public ToggleSourceOrGroupVisibilityAction( final ViewerPanel viewer, final int index )
		{
			super( String.format( TOGGLE_SOURCE_VISIBILITY, index ), viewer );
			this.index = index;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			final ViewerState state = viewer.state();
			synchronized ( state )
			{
				if ( state.getDisplayMode().hasGrouping() )
				{
					final List< SourceGroup > groups = state.getGroups();
					if ( index >= 0 && index < groups.size() )
					{
						final SourceGroup group = groups.get( index );
						state.setGroupActive( group, !state.isGroupActive( group ) );
					}
				}
				else
				{
					final List< SourceAndConverter< ? > > sources = state.getSources();
					if ( index >= 0 && index < sources.size() )
					{
						final SourceAndConverter< ? > source = sources.get( index );
						state.setSourceActive( source, !state.isSourceActive( source ) );
					}
				}
			}
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
		private static final long serialVersionUID = -5868085804210873492L;

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
	}

	public static class CollapseCardAction extends NavigationAction
	{
		private static final long serialVersionUID = -2337698189753056286L;

		public BigWarpViewerFrame frame;

		public CollapseCardAction( BigWarpViewerFrame frame )
		{
			super( COLLAPSE_CARDS, frame.getViewerPanel() );
			this.frame = frame;
			System.out.println("card action name: " + name());
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			System.out.println("card action " + name() );
			frame.collapseCardPanel();
		}
	}

	public static class ExpandCardAction extends NavigationAction
	{
		private static final long serialVersionUID = -4267852269622298980L;

		public BigWarpViewerFrame frame;

		public ExpandCardAction( BigWarpViewerFrame frame )
		{
			super( EXPAND_CARDS, frame.getViewerPanel() );
			this.frame = frame;
			System.out.println("card action name: " + name());
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			System.out.println("card action " + name() );
			frame.expandAndFocusCardPanel();
		}
	}

	public static class CardAction extends NavigationAction
	{
		private static final long serialVersionUID = -8949725696799894130L;

		public BigWarpViewerFrame frame;

		public CardAction( BigWarpViewerFrame frame, final String name )
		{
			super( name, frame.getViewerPanel() );
			this.frame = frame;
			System.out.println("card action name: " + name());
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			System.out.println("card action " + name() );
			if( name().equals( EXPAND_CARDS ) )
				frame.expandAndFocusCardPanel();
			else
				frame.collapseCardPanel();
		}
	}
	
}
