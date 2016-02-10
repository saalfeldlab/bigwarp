package bdv.viewer;

import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.Arrays;

import javax.swing.JTable;

import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import mpicbg.models.CoordinateTransform;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.gui.BigWarpLandmarkPanel;
import bdv.viewer.state.ViewerState;
import bigwarp.landmarks.LandmarkTableModel;

public class BigWarpOverlay {
	
	/** The viewer state. */
	private ViewerState state;
	
	private BigWarpViewerPanel viewer;
	
	protected JTable table;

	protected LandmarkTableModel landmarkModel;
	
	protected CoordinateTransform estimatedXfm;
	
	protected boolean isTransformed = false;

	private int hoveredIndex;

	protected final boolean isMoving;
	protected final boolean is3d;
	
	/** The transform for the viewer current viewpoint. */
	private final AffineTransform3D transform = new AffineTransform3D();
	
	public BigWarpOverlay( final BigWarpViewerPanel viewer, BigWarpLandmarkPanel landmarkpanel )
	{
		this.viewer = viewer;
		this.table = landmarkpanel.getJTable();
		this.landmarkModel = landmarkpanel.getTableModel();

		if( landmarkModel.getNumdims() == 3 )
			is3d = true;
		else
			is3d = false;

		isMoving = viewer.getIsMoving();
	}


	public int getHoveredIndex()
	{
		return hoveredIndex;
	}


	public void setHoveredIndex( int hoveredIndex )
	{
		this.hoveredIndex = hoveredIndex;
	}


	public void paint( final Graphics2D g ) 
	{
		/*
		 * Collect current view.
		 */
		state.getViewerTransform( transform );

		// Save graphic device original settings
		final Composite originalComposite = g.getComposite();
		final Stroke originalStroke = g.getStroke();
		final Color originalColor = g.getColor();

		// get selected points
		int[] selectedRows = table.getSelectedRows();
		Arrays.sort( selectedRows );
		boolean[] isSelected = new boolean[ landmarkModel.getRowCount() ];
		for( int i : selectedRows )
			isSelected[ i ] = true;

		/*
		 * Draw spots.
		 */
		if ( viewer.getSettings().areLandmarksVisible() )
		{

			final double radiusRatio = ( Double ) viewer.getSettings().get( 
					BigWarpViewerSettings.KEY_SPOT_RADIUS_RATIO );
			
			final double radius = viewer.getSettings().getSpotSize();

			Color color;
			Stroke stroke;
			stroke = BigWarpViewerSettings.NORMAL_STROKE;
			
			FontMetrics fm = null;
			int fonthgt = 0;
			Color textBoxColor = null;
			if ( viewer.getSettings().areNamesVisible() )
			{
				fm = g.getFontMetrics( g.getFont() );
				fonthgt = fm.getHeight();
				textBoxColor = Color.BLACK;
			}
			
			for( int index = 0; index < landmarkModel.getRowCount(); index++ )
			{
				Double[] spot; // = landmarkModel.getPoints().get( index );

				if ( landmarkModel.isActive( index ) )
					color = viewer.getSettings().getSpotColor();
				else
					color = viewer.getSettings().getInactiveSpotColor();

				g.setColor( color );
				g.setStroke( stroke );

				double x = 0.0, y = 0.0, z = 0.0;
				spot = landmarkModel.getPoints( isMoving ).get( index );

				// if this point is not set, don't render it.
				if ( Double.isInfinite( spot[ 0 ] ) )
					continue;

				// if the viewer is moving but transformed, render the points
				// at the location of the warped point ( if it exists ),
				// otherwise, take the fixed point
				if ( isMoving && viewer.isInFixedImageSpace() )
				{
					if ( landmarkModel.isWarped( index ) )
						spot = landmarkModel.getWarpedPoints().get( index );
					else
						spot = landmarkModel.getPoints( false ).get( index );
				}

				// have to do this song and dance because globalCoords should be a length-3 array
				// all the time with z=0 if we're in a 2d
				x = spot[ 0 ];
				y = spot[ 1 ];
				if ( is3d )
					z = spot[ 2 ];

				final double[] globalCoords = new double[] { x, y, z };
				final double[] viewerCoords = new double[ 3 ];
				transform.apply( globalCoords, viewerCoords );

				// final double rad = radius * transformScale * radiusRatio;
				final double rad = radius * radiusRatio;
				final double zv = viewerCoords[ 2 ];
				final double dz2 = zv * zv;

				if ( dz2 < rad * rad )
				{
					final double arad = Math.sqrt( rad * rad - dz2 );
					
					// vary size
					g.fillOval( ( int ) ( viewerCoords[ 0 ] - arad ), 
								( int ) ( viewerCoords[ 1 ] - arad ), 
								( int ) ( 2 * arad + 1 ), ( int ) ( 2 * arad + 1) );
					
					if( isSelected[ index ] )
					{
						g.drawOval( ( int ) ( viewerCoords[ 0 ] - arad - 2 ),
									( int ) ( viewerCoords[ 1 ] - arad - 2 ),
									( int ) ( 2 * arad + 4 ), ( int ) ( 2 * arad + 4 ) );
					}
					else if( hoveredIndex == index )
					{
						g.setColor( new Color( color.getRed(), color.getGreen(), color.getBlue(), 128 ));
						g.drawOval( ( int ) ( viewerCoords[ 0 ] - arad - 2 ),
								( int ) ( viewerCoords[ 1 ] - arad - 2 ),
								( int ) ( 2 * arad + 4 ), ( int ) ( 2 * arad + 4 ) );
						g.setColor( color );
					}

					if ( viewer.getSettings().areNamesVisible() )
					{
						final int tx = ( int ) ( viewerCoords[ 0 ] + arad + 5 );
						final int ty = ( int ) viewerCoords[ 1 ];
						
						String name = landmarkModel.getNames().get(index);
						int strwidth = fm.stringWidth( name );
						
						if( isSelected[ index ] || hoveredIndex == index )
							textBoxColor = new Color( color.getRed(), color.getGreen(), color.getBlue(), 255 );
						else
							textBoxColor = new Color( color.getRed(), color.getGreen(), color.getBlue(), 128 );
						
						g.setColor( textBoxColor );
						g.fillRect( tx - 1, ty - fonthgt + 2, strwidth + 2, fonthgt);
						
						g.setColor( Color.BLACK );
						g.drawString( name, tx, ty );
						
					}
				}
				
			}
		}

		// Restore graphic device original settings
		g.setComposite( originalComposite );
		g.setStroke( originalStroke );
		g.setColor( originalColor );
	}


	/**
	 * Update data to show in the overlay.
	 */
	public void setViewerState( final ViewerState state )
	{
		this.state = state;
	}
	
	public void setEstimatedTransform( final ThinPlateR2LogRSplineKernelTransform estimatedXfm )
	{
		this.estimatedXfm = estimatedXfm;
	}
	
	public boolean getIsTransformed()
	{
		return isTransformed;
	}
	
	public void setIsTransformed( boolean isTransformed )
	{
		this.isTransformed = isTransformed;
	}

}

