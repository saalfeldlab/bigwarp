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
package bdv.viewer;

import bdv.cache.CacheControl;
import bdv.gui.BigWarpViewerOptions;
import bdv.img.WarpedSource;
import bdv.util.Affine3DHelpers;
import bdv.util.Prefs;
import bdv.viewer.animate.RotationAnimator;
import bdv.viewer.animate.SimilarityTransformAnimator3D;
import bdv.viewer.overlay.BigWarpMaskSphereOverlay;
import bigwarp.BigWarpData;
import bigwarp.util.Rotation2DHelpers;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Translation3D;

public class BigWarpViewerPanel extends ViewerPanel
{
	private static final long serialVersionUID = 7706602964307210070L;

	public static final int MOVING_GROUP_INDEX = 0;

	public static final int TARGET_GROUP_INDEX = 1;

	protected final BigWarpData<?> bwData;

	protected BigWarpViewerSettings viewerSettings;

	protected BigWarpOverlay overlay;

	protected BigWarpDragOverlay dragOverlay;

	protected BigWarpMaskSphereOverlay maskOverlay;

	protected boolean isMoving;

	protected boolean updateOnDrag;

	protected boolean transformEnabled = true;

	protected int ndims;

	protected boolean boxOverlayVisible = true;

	protected boolean textOverlayVisible = true;

	protected ArrayList< AffineTransform3D > orthoTransforms;

	// root two over two
	public static final double R2o2 = Math.sqrt( 2 ) / 2;

	ViewerOptions options;

	public BigWarpViewerPanel( final BigWarpData bwData , final BigWarpViewerSettings viewerSettings, final CacheControl cache, boolean isMoving )
	{
		this( bwData, viewerSettings, cache, BigWarpViewerOptions.options(), isMoving );
	}

	public BigWarpViewerPanel( final BigWarpData bwData, final BigWarpViewerSettings viewerSettings, final CacheControl cache, final BigWarpViewerOptions optional, boolean isMoving )
	{
		// TODO compiler complains if the first argument is 'final BigWarpData<?> bwData'
		super( bwData.sources, 1, cache, optional.getViewerOptions( isMoving ) );
		this.bwData = bwData;
		this.viewerSettings = viewerSettings;
		this.isMoving = isMoving;
		this.updateOnDrag = !isMoving; // update on drag only for the fixed
										// image by default
		getDisplay().overlays().add( g -> {
			if ( null != overlay ) {
				overlay.setViewerState( state() );
				overlay.paint( ( Graphics2D ) g );
			}
			if ( dragOverlay != null ) {
				//dragOverlay.setViewerState( state );
				dragOverlay.paint( ( Graphics2D ) g );
			}
		} );

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
			for ( int i : bwData.movingSourceIndexList )
				moving.add( state.getSources().get( i ) );
			final List< SourceAndConverter< ? > > target = new ArrayList<>();
			for ( int i : bwData.targetSourceIndexList )
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
//		return !isMoving || ( ( WarpedSource< ? > ) ( state().getSources().get( bwData.movingSourceIndexList.get( 0 ) ).getSpimSource() ) ).isTransformed();
		return !isMoving || ( ( WarpedSource< ? > ) ( ( bwData.getMovingSource( 0 )).getSpimSource() ) ).isTransformed();
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

	public void addOverlay( OverlayRenderer overlay )
	{
		super.getDisplay().overlays().add( overlay );
	}

	public BigWarpDragOverlay getDragOverlay(){
		return dragOverlay;
	}

	public BigWarpMaskSphereOverlay getMaskOverlay()
	{
		return maskOverlay;
	}

	public void setMaskOverlay( final BigWarpMaskSphereOverlay maskOverlay )
	{
		this.maskOverlay = maskOverlay;
		addOverlay( maskOverlay );
	}

	public boolean getIsMoving()
	{
		return isMoving;
	}

	public void setNumDim( int ndim )
	{
		this.ndims = ndim;
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
		setTransformAnimator( new RotationAnimator(transform, centerX, centerY, qNew, 300 ) );
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

		if( ndims == 2 )
		{
			// if 2d, make sure the viewer transform change doesn't change the z-slice shown
			final double[] tmp = new double[3];
			startXfm.applyInverse(tmp, tmp);
			final double zstart = tmp[2];

			Arrays.fill(tmp, 0);
			destinationXfm.applyInverse(tmp, tmp);
			final Translation3D t = new Translation3D( 0, 0, (tmp[2] - zstart) );
			destinationXfm.concatenate(t);
		}

		setTransformAnimator( new SimilarityTransformAnimator3D( startXfm, destinationXfm, centerX, centerY, millis/2 ) );
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

	@Override
	public void drawOverlays( final Graphics g )
	{
		// remember Prefs settings
		final boolean prefsShowTextOverlay = Prefs.showTextOverlay();
		final boolean prefsShowMultibox = Prefs.showMultibox();

		Prefs.showTextOverlay( textOverlayVisible );
		Prefs.showMultibox( boxOverlayVisible );
		super.drawOverlays( g );

		// restore Prefs settings
		Prefs.showTextOverlay( prefsShowTextOverlay );
		Prefs.showMultibox( prefsShowMultibox );
	}
}
