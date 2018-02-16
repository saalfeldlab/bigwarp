package bdv.tools.transformation;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import org.scijava.ui.behaviour.util.InputActionBindings;

import bdv.gui.BigWarpViewerFrame;
import bdv.img.RenamableSource;
import bdv.img.WarpedSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.state.SourceGroup;
import bdv.viewer.state.ViewerState;
import bigwarp.BigWarp;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;

public class BigwarpAffineEditor  implements TransformListener< AffineTransform3D >
{
	private boolean active = false;

	private final InputActionBindings bindings;

	private ViewerPanel viewer;
	
	private final BigWarp bw;

	private final AffineTransform3D frozenTransform;

	private final AffineTransform3D liveTransform;

	private final ArrayList< SourceAndConverter< ? > > sourcesToModify;

	private final ArrayList< SourceAndConverter< ? > > sourcesToFix;

	private final ActionMap actionMap;

	private final InputMap inputMap;

	public BigwarpAffineEditor( final BigWarp bw, final ViewerPanel viewer, final InputActionBindings inputActionBindings )
	{
		this.bw = bw;
		this.viewer = null;
		
		bindings = inputActionBindings;
		frozenTransform = new AffineTransform3D();
		liveTransform = new AffineTransform3D();
		sourcesToModify = new ArrayList<>();
		sourcesToFix = new ArrayList<>();

		final KeyStroke abortKey = KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 );
		final Action abortAction = new AbstractAction( "abort manual transformation" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				abort();
			}

			private static final long serialVersionUID = 1L;
		};
		final KeyStroke resetKey = KeyStroke.getKeyStroke( KeyEvent.VK_R, 0 );
		final Action resetAction = new AbstractAction( "reset manual transformation" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				reset();
			}

			private static final long serialVersionUID = 1L;
		};
		actionMap = new ActionMap();
		inputMap = new InputMap();
		actionMap.put( "abort manual transformation", abortAction );
		inputMap.put( abortKey, "abort manual transformation" );
		actionMap.put( "reset manual transformation", resetAction );
		inputMap.put( resetKey, "reset manual transformation" );
		bindings.addActionMap( "manual transform", actionMap );
	}

	public synchronized void abort()
	{
		if ( active )
		{
			final AffineTransform3D identity = new AffineTransform3D();
			
			for ( final SourceAndConverter< ? > sac : sourcesToModify )
			{
				TransformedSource<?> source = getTransformedSource( sac.getSpimSource() );
				if( source == null )
					continue;

				source.setIncrementalTransform( identity );
			}

			viewer.setCurrentViewerTransform( frozenTransform );
			viewer.showMessage( "aborted manual transform" );
			active = false;
		}
	}

	public synchronized void reset()
	{
		if ( active )
		{
			final AffineTransform3D identity = new AffineTransform3D();
			for ( final SourceAndConverter< ? > sac : sourcesToModify )
			{
				TransformedSource<?> source = getTransformedSource( sac.getSpimSource() );
				if( source == null )
					continue;

				source.setIncrementalTransform( identity );
				source.setFixedTransform( identity );
			}
			for ( final SourceAndConverter< ? > sac : sourcesToFix )
			{
				TransformedSource<?> source = getTransformedSource( sac.getSpimSource() );
				if( source == null )
					continue;

				source.setIncrementalTransform( identity );
			}
			viewer.setCurrentViewerTransform( frozenTransform );
			viewer.showMessage( "reset manual transform" );
		}
	}

	/**
	 * 
	 * @param src some source of unkown type
	 * @return a transformed source that is "wrapped" at some level by the input source
	 */
	public TransformedSource<?> getTransformedSource( Source<?> src )
	{
		if( src instanceof TransformedSource )
			return (TransformedSource<?>) src;
		else if ( src instanceof WarpedSource<?> )
			return getTransformedSource( ((WarpedSource<?>) src).getWrappedSource() );
		else if ( src instanceof RenamableSource<?> )
			return getTransformedSource( ((RenamableSource<?>) src).getWrappedSource() );
		else
			return null;
	}

	public synchronized void toggle()
	{
		BigWarpViewerFrame vp = bw.getViewerFrameP();
		BigWarpViewerFrame vq = bw.getViewerFrameQ();
		
		if ( vp.isActive() )
		{
			viewer = vp.getViewerPanel();
		}
		else if ( vq.isActive() )
		{
			viewer = vq.getViewerPanel();
		}
				
		if ( !active )
		{ // Enter manual edit mode
			final ViewerState state = viewer.getState();
			final List< Integer > indices = new ArrayList<>();
			switch ( state.getDisplayMode() )
			{
			case FUSED:
				indices.add( state.getCurrentSource() );
				break;
			case FUSEDGROUP:
				final SourceGroup group = state.getSourceGroups().get( state.getCurrentGroup() );
				indices.addAll( group.getSourceIds() );
				break;
			default:
				viewer.showMessage( "Can only do manual transformation when in FUSED mode." );
				return;
			}
			state.getViewerTransform( frozenTransform );
			sourcesToModify.clear();
			sourcesToFix.clear();
			

			sourcesToModify.addAll( bw.getMovingSources() );
			sourcesToFix.addAll( bw.getTargetSources() );

			active = true;
			viewer.addTransformListener( this );
			bindings.addInputMap( "manual transform", inputMap );
			viewer.showMessage( "starting manual transform" );
		}
		else
		{ // Exit manual edit mode.
			active = false;
			viewer.removeTransformListener( this );
			bindings.removeInputMap( "manual transform" );
			final AffineTransform3D tmp = new AffineTransform3D();
			
			tmp.identity();
			tmp.preConcatenate( liveTransform );
			bw.setMovingAffine( tmp );
			
			viewer.setCurrentViewerTransform( frozenTransform );
			viewer.showMessage( "fixed manual transform" );
		}
	}

	@Override
	public void transformChanged( final AffineTransform3D transform )
	{
		if ( !active ) { return; }

		liveTransform.set( transform );
		liveTransform.preConcatenate( frozenTransform.inverse() );

		for ( final SourceAndConverter< ? > sac : sourcesToFix )
		{
			TransformedSource<?> source = getTransformedSource( sac.getSpimSource() );
			if( source == null )
				continue;

			source.setIncrementalTransform( liveTransform.inverse() );
		}
	}
}
