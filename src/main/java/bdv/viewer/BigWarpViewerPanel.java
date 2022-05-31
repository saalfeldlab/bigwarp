/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2021 Howard Hughes Medical Institute.
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
package bdv.viewer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import bdv.cache.CacheControl;
import bdv.gui.BigWarpViewerOptions;
import bdv.img.WarpedSource;
import bdv.util.Affine3DHelpers;
import bdv.util.Prefs;
import bdv.viewer.animate.MessageOverlayAnimator;
import bdv.viewer.animate.OverlayAnimator;
import bdv.viewer.animate.RotationAnimator;
import bdv.viewer.animate.SimilarityTransformAnimator3D;
import bdv.viewer.overlay.BigWarpGenericOverlay;
import bigwarp.util.Rotation2DHelpers;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

public class BigWarpViewerPanel extends ViewerPanel
{
	private static final long serialVersionUID = 7706602964307210070L;

	public static final int MOVING_GROUP_INDEX = 0;

	public static final int TARGET_GROUP_INDEX = 1;

	protected List< SourceAndConverter< ? > > sources;

	protected BigWarpViewerSettings viewerSettings;

	protected BigWarpOverlay overlay;

	protected BigWarpDragOverlay dragOverlay;

	protected ArrayList<BigWarpGenericOverlay<?>> otherOverlays;

	protected final MessageOverlayAnimator message;

	protected boolean isMoving;

	protected boolean updateOnDrag;

	protected boolean transformEnabled = true;

	protected int ndims;

	final protected int[] movingSourceIndexList;

	final protected int[] targetSourceIndexList;

	protected boolean boxOverlayVisible = true;

	protected boolean textOverlayVisible = true;
	
	protected ArrayList< AffineTransform3D > orthoTransforms;

	// root two over two
	public static final double R2o2 = Math.sqrt( 2 ) / 2;

	ViewerOptions options;

	public BigWarpViewerPanel( final List< SourceAndConverter< ? > > sources, final BigWarpViewerSettings viewerSettings, final CacheControl cache, boolean isMoving,
			int[] movingSourceIndexList, int[] targetSourceIndexList )
	{
		this( sources, viewerSettings, cache, BigWarpViewerOptions.options(), isMoving, movingSourceIndexList, targetSourceIndexList );
	}
	
	public BigWarpViewerPanel( final List< SourceAndConverter< ? > > sources, final BigWarpViewerSettings viewerSettings, final CacheControl cache, final BigWarpViewerOptions optional, boolean isMoving, 
			int[] movingSourceIndexList, int[] targetSourceIndexList  )
	{
		super( sources, 1, cache, optional );
		this.sources = sources;
		this.viewerSettings = viewerSettings;
		options = optional;
		this.isMoving = isMoving;
		this.updateOnDrag = !isMoving; // update on drag only for the fixed
										// image by default
		this.movingSourceIndexList = movingSourceIndexList;
		this.targetSourceIndexList = targetSourceIndexList;

		if( isMoving )
			message = optional.getValues().getMsgOverlayMoving();
		else
			message = optional.getValues().getMsgOverlay();

		otherOverlays = new ArrayList<>();  
		overlayAnimators.add( message );
		updateGrouping();
	}

	@Override
	public ViewerOptions.Values getOptionValues()
	{
		return options.values;
	}

	public void precomputeRotations2d( final AffineTransform3D initialViewTransform )
	{
		orthoTransforms = new ArrayList<>();
		AffineTransform3D rot = new AffineTransform3D();
		rot.rotate( 2, -Math.PI / 2 );

		AffineTransform3D xfm = initialViewTransform;
		orthoTransforms.add( xfm );
		for( int i = 1; i < 4; i++ )
		{
			AffineTransform3D newXfm = xfm.copy();
			newXfm.rotate( 2, -Math.PI/2);
			orthoTransforms.add( newXfm );
			xfm = newXfm;
		}
	}

	public void toggleTextOverlayVisible()
	{
		textOverlayVisible = !textOverlayVisible;
	}

	public void toggleBoxOverlayVisible()
	{
		boxOverlayVisible = !boxOverlayVisible;
	}
	
	@Override
	public void showMessage( final String msg )
	{
		message.add( msg );
		display.repaint();
	}

	public void setHoveredIndex( int index )
	{
		if( index != overlay.getHoveredIndex() )
		{
			overlay.setHoveredIndex( index );

			// repaint
			if ( null != overlay ) {
				overlay.setViewerState( state() );
			}
		}
	}

	/**
	 * Makes the first group contain all the moving images and the second group
	 * contain all the fixed images
	 * 
	 * @return the number sources in the moving group
	 */
	public int updateGrouping()
	{
		final SynchronizedViewerState state = state();
		synchronized ( state )
		{
			// TODO: work backwards to find out whether movingSourceIndexList
			//  and targetSourceIndexList are required, or whether a
			//  List<SourceAndConverter<?>> can be used directly
			final List< SourceAndConverter< ? > > moving = new ArrayList<>();
			for ( int i : movingSourceIndexList )
				moving.add( state.getSources().get( i ) );
			final List< SourceAndConverter< ? > > target = new ArrayList<>();
			for ( int i : targetSourceIndexList )
				target.add( state.getSources().get( i ) );

			state.clearGroups();

			final SourceGroup movingGroup = new SourceGroup();
			state.addGroup( movingGroup );
			state.setGroupName( movingGroup, "moving images" );
			state.addSourcesToGroup( moving, movingGroup );

			final SourceGroup targetGroup = new SourceGroup();
			state.addGroup( targetGroup );
			state.setGroupName( targetGroup, "fixed images" );
			state.addSourcesToGroup( target, targetGroup );

			// make only moving and target image groups active in fused mode
			state.setGroupActive( movingGroup, true );
			state.setGroupActive( targetGroup, true );

			// only turn grouping enabled by default if there are multiple moving or
			// target images
			if ( moving.size() > 1 || target.size() > 1 )
				state.setDisplayMode( state.getDisplayMode().withGrouping( true ) );

			state.setCurrentGroup( isMoving ? movingGroup : targetGroup );

			// TODO: This does not what the javadoc says. Change javadoc? Return void.
			return state.getGroups().size();
		}
	}

	public boolean isInFixedImageSpace()
	{
		return !isMoving || ( ( WarpedSource< ? > ) ( sources.get( movingSourceIndexList[ 0 ] ).getSpimSource() ) ).isTransformed();
	}

	public boolean doUpdateOnDrag()
	{
		return updateOnDrag;
	}

	public void setUpdateOnDrag( boolean updateOnDrag )
	{
		this.updateOnDrag = updateOnDrag;
	}

	public void toggleUpdateOnDrag()
	{
		setUpdateOnDrag( !updateOnDrag );
		if( updateOnDrag )
			showMessage( "Update on drag" );
		else
			showMessage( "No update on drag" );
	}

	public void addOverlay( BigWarpOverlay overlay ){
		this.overlay = overlay;
	}
	
	public BigWarpOverlay getOverlay( ){
		return overlay;
	}

	public void addDragOverlay( BigWarpDragOverlay dragOverlay ){
		this.dragOverlay = dragOverlay;
	}

	public BigWarpDragOverlay getDragOverlay(){
		return dragOverlay;
	}
	
	public boolean getIsMoving()
	{
		return isMoving;
	}
	
	public void setNumDim( int ndim )
	{
		this.ndims = ndim;
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		boolean requiresRepaint = false;
		if( boxOverlayVisible )
		{
			multiBoxOverlayRenderer.setViewerState( state() );
			multiBoxOverlayRenderer.updateVirtualScreenSize( display.getWidth(), display.getHeight() );
			multiBoxOverlayRenderer.paint( ( Graphics2D ) g );
			requiresRepaint = multiBoxOverlayRenderer.isHighlightInProgress();
		}

		if( textOverlayVisible )
		{
			sourceInfoOverlayRenderer.setViewerState( state() );
			sourceInfoOverlayRenderer.paint( ( Graphics2D ) g );
		}

		if ( Prefs.showScaleBar() )
		{
			scaleBarOverlayRenderer.setViewerState( state() );
			scaleBarOverlayRenderer.paint( ( Graphics2D ) g );
		}

		if( textOverlayVisible )
		{
			final RealPoint gPos = new RealPoint( 3 );
			getGlobalMouseCoordinates( gPos );

			final String mousePosGlobalString;
			if ( ndims == 2 )
				mousePosGlobalString = String.format( "(%6.1f,%6.1f)",
						gPos.getDoublePosition( 0 ), gPos.getDoublePosition( 1 ) );
			else
				mousePosGlobalString = String.format( "(%6.1f,%6.1f,%6.1f)",
						gPos.getDoublePosition( 0 ), gPos.getDoublePosition( 1 ),
						gPos.getDoublePosition( 2 ) );

			g.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
			g.setColor( Color.white );
			int actual_width = g.getFontMetrics().stringWidth( mousePosGlobalString );
			g.drawString( 
					mousePosGlobalString,
					(int) g.getClipBounds().getWidth() - actual_width - 10, 28 );
		}

		final long currentTimeMillis = System.currentTimeMillis();
		final ArrayList< OverlayAnimator > overlayAnimatorsToRemove = new ArrayList< OverlayAnimator >();
		for ( final OverlayAnimator animator : overlayAnimators )
		{
			animator.paint( ( Graphics2D ) g, currentTimeMillis );
			requiresRepaint |= animator.requiresRepaint();
			if ( animator.isComplete() )
				overlayAnimatorsToRemove.add( animator );
		}
		overlayAnimators.removeAll( overlayAnimatorsToRemove );

        for( final BigWarpGenericOverlay< ? >  thisoverlay : otherOverlays )                                                                                                                    
        {
            if( thisoverlay.isVisible() )
            {
                thisoverlay.paint( ( Graphics2D ) g );
            }
        }

		if ( requiresRepaint )
			display.repaint();
		
		if ( null != overlay ) {
			overlay.setViewerState( state() );
			overlay.paint( ( Graphics2D ) g );
		}
		
		if ( dragOverlay != null ) {
			//dragOverlay.setViewerState( state );
			dragOverlay.paint( ( Graphics2D ) g );
		}
	}
	
	public BigWarpViewerSettings getSettings()
	{
		return viewerSettings;
	}

	public void displayViewerTransforms()
	{
		System.out.println( state().getViewerTransform() );
	}

	public synchronized void rotateView2d( boolean isClockwise )
	{
		if ( !transformEnabled )
			return;

		final AffineTransform3D transform = state().getViewerTransform();

		double centerX;
		double centerY;
		if ( mouseCoordinates.isMouseInsidePanel() )
		{
			centerX = mouseCoordinates.getX();
			centerY = mouseCoordinates.getY();
		}
		else
		{
			centerY = getHeight() / 2.0;
			centerX = getWidth() / 2.0;
		}

		AffineTransform3D newTransform = null;
		for( int i = 0; i < 4; i++ )
		{
			try
			{
				newTransform = Rotation2DHelpers.targetViewerTransform2d( transform , isClockwise );
				break;
			}
			catch(Exception e)
			{
				if( isClockwise )
					transform.rotate( 2, -0.1 );
				else
					transform.rotate( 2, 0.1 );
			}
		}

		double[] qNew = new double[ 4 ];
		Affine3DHelpers.extractRotation( newTransform, qNew );
		currentAnimator = new RotationAnimator(transform, centerX, centerY, qNew, 300 );
		currentAnimator.setTime( System.currentTimeMillis() );
		requestRepaint();
	}

	@Override
	public synchronized void align( AlignPlane plane )
	{
		if ( !transformEnabled )
			return;

		super.align( plane );
	}

    public synchronized void animateTransformation( final AffineTransform3D destinationXfm, int millis )
    {
		if ( !transformEnabled )
			return;

		final AffineTransform3D startXfm = state().getViewerTransform();

		double centerX;
		double centerY;
		if ( mouseCoordinates.isMouseInsidePanel() )
		{
			centerX = mouseCoordinates.getX();
			centerY = mouseCoordinates.getY();
		}
		else
		{
			centerY = getHeight() / 2.0;
			centerX = getWidth() / 2.0;
		}
 
		// TODO fixes to transform handlers mean the 3d animator works.
		// 2d similar animator is still broken though.
//		if( ndims == 2 )
//			currentAnimator = new SimilarityTransformAnimator2D( startXfm, destinationXfm, centerX, centerY, millis );
//		else
//			currentAnimator = new SimilarityTransformAnimator3D( startXfm, destinationXfm, centerX, centerY, millis/2 );

		currentAnimator = new SimilarityTransformAnimator3D( startXfm, destinationXfm, centerX, centerY, millis/2 );

		currentAnimator.setTime( System.currentTimeMillis() );
		requestRepaint();
    }
    
    public void animateTransformation( AffineTransform3D destinationXfm )
    {
    	animateTransformation( destinationXfm, 300 );
    }
    
    public synchronized void setTransformEnabled( boolean enabled )
    {
    	transformEnabled = enabled;
    }
    
    public boolean getTransformEnabled()
    {
    	return transformEnabled;
    }

    public void addGenericOverlay( final BigWarpGenericOverlay< ? > overlay )
    {
        otherOverlays.add( overlay );                                                                                                                                                           
    }

}
