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
package bdv.viewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.Arrays;

import javax.swing.JTable;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import bdv.gui.BigWarpLandmarkPanel;
import bigwarp.landmarks.LandmarkTableModel;

public class BigWarpOverlay {

	private BigWarpViewerPanel viewer;

	private BigWarpLandmarkPanel landmarkPanel;

	protected RealTransform estimatedXfm;

	protected boolean isTransformed = false;

	private int hoveredIndex;

	protected final boolean isMoving;

	protected final double[] spot;

	protected final double[] viewerCoords;
	/** The transform for the viewer current viewpoint. */
	private final AffineTransform3D transform = new AffineTransform3D();


	public BigWarpOverlay( final BigWarpViewerPanel viewer, BigWarpLandmarkPanel landmarkPanel )
	{
		this.viewer = viewer;
		setLandmarkPanel(landmarkPanel);
		isMoving = viewer.getIsMoving();

		spot = new double[ 3 ];
		viewerCoords = new double[ 3 ];
	}

	public int getHoveredIndex()
	{
		return hoveredIndex;
	}

	public void setHoveredIndex( int hoveredIndex )
	{
		this.hoveredIndex = hoveredIndex;
	}

	public void setLandmarkPanel( final BigWarpLandmarkPanel landmarkPanel )
	{
		this.landmarkPanel = landmarkPanel;
	}

	public void paint( final Graphics2D g )
	{

		/*
		 * Draw spots.
		 */
		if ( viewer.getSettings().areLandmarksVisible() )
		{

			final LandmarkTableModel landmarkModel = landmarkPanel.getTableModel();
			final JTable table = landmarkPanel.getJTable();
			final boolean is3d = landmarkPanel.numDimensions() == 3;

			// Save graphic device original settings
			final Composite originalComposite = g.getComposite();
			final Stroke originalStroke = g.getStroke();
			final Color originalColor = g.getColor();

			// get selected points
			final int[] selectedRows = table.getSelectedRows();
			Arrays.sort( selectedRows );
			final boolean[] isSelected = new boolean[ landmarkModel.getRowCount() ];
			for( final int i : selectedRows )
				isSelected[ i ] = true;


			final BasicStroke hlStroke = new BasicStroke( (int)viewer.getSettings().strokeWeight );
			// final BasicStroke selStroke = new BasicStroke( (int)viewer.getSettings().strokeWeight / 2 );

			final double radiusRatio = ( Double ) viewer.getSettings().get(
					BigWarpViewerSettings.KEY_SPOT_RADIUS_RATIO );

			final double radius = viewer.getSettings().getSpotSize();

			Stroke stroke;
			stroke = BigWarpViewerSettings.NORMAL_STROKE;

			FontMetrics fm = null;
			Font font = null;
			int fonthgt = 0;
			Color textBoxColorHl = null;
			Color textBoxColor = null;

			final double strokeW = viewer.getSettings().strokeWeight;
			final Color color  = viewer.getSettings().getSpotColor();
			final Color inactiveColor  = viewer.getSettings().getSpotColor();

			if ( viewer.getSettings().areNamesVisible() )
			{
				font = g.getFont().deriveFont( viewer.getSettings().fontSize );
				g.setFont( font );
				fm = g.getFontMetrics( g.getFont() );
				fonthgt = fm.getHeight();

				textBoxColor = Color.BLACK;
				textBoxColorHl = new Color( color.getRed(), color.getGreen(), color.getBlue(), 255 );
				textBoxColor = new Color( color.getRed(), color.getGreen(), color.getBlue(), 128 );

			}

			final int nRows = landmarkModel.getRowCount();
			for( int index = 0; index < nRows; index++ )
			{

				if ( landmarkModel.isActive( index ) )
					g.setColor( color );
				else
					g.setColor( inactiveColor );

				g.setStroke( stroke );

				landmarkModel.copyPointSafe(spot, index, isMoving);

				// if the viewer is moving but transformed, render the points
				// at the location of the warped point ( if it exists ),
				// otherwise, take the fixed point
				boolean copySuccess = false;
				if( isMoving ) {
					if( viewer.isInFixedImageSpace() ) {
						if( landmarkModel.isWarped(index))
							copySuccess = landmarkModel.copyWarpedPointSafe(spot, index);
						else
							copySuccess = landmarkModel.copyTargetPointSafe(spot, index);
					}
					else
						copySuccess = landmarkModel.copyMovingPointSafe(spot, index);
				}
				else {
					// fixed space
					copySuccess = landmarkModel.copyTargetPointSafe(spot, index);
				}

				// if this point is not set, don't render it.
				if ( !copySuccess || Double.isInfinite( spot[ 0 ] ) )
					continue;

				transform.apply( spot, viewerCoords );

				final double rad = radius * radiusRatio;
				final double zv = viewerCoords[ 2 ];
				final double dz2 = zv * zv;

				if ( !is3d || dz2 < rad * rad )
				{
					final double arad;
					if( is3d )
						arad = Math.sqrt( rad * rad - dz2 );
					else
						arad = rad;

					final double srad = arad + strokeW;

					// vary size
					g.fillOval( ( int ) ( viewerCoords[ 0 ] - arad ),
								( int ) ( viewerCoords[ 1 ] - arad ),
								( int ) ( 2 * arad + 1 ), ( int ) ( 2 * arad + 1) );

					if( isSelected[ index ] )
					{
						g.setStroke( hlStroke );
						g.setColor( Color.WHITE );
						g.drawOval( ( int ) ( viewerCoords[ 0 ] - arad - 2),
								( int ) ( viewerCoords[ 1 ] - arad - 2 ),
								( int ) ( 2 * arad + 4 ), ( int ) ( 2 * arad + 4 ) );

					}

					if( hoveredIndex == index )
					{
						g.setStroke( hlStroke );
						g.setColor( new Color( color.getRed(), color.getGreen(), color.getBlue(), 128 ));
						g.drawOval( ( int ) ( viewerCoords[ 0 ] - srad - 2 ),
								( int ) ( viewerCoords[ 1 ] - srad - 2 ),
								( int ) ( 2 * srad + 4 ), ( int ) ( 2 * srad + 4 ) );

						g.setColor( color );
						g.setStroke( stroke );
					}

					if ( viewer.getSettings().areNamesVisible() )
					{
						final int tx = ( int ) ( viewerCoords[ 0 ] + arad + 5 );
						final int ty = ( int ) viewerCoords[ 1 ];

						final String name = landmarkModel.getNames().get(index);
						final int strwidth = fm.stringWidth( name );

						if( hoveredIndex == index )
							g.setColor( textBoxColorHl );
						else
							g.setColor( textBoxColor );

						g.fillRect( tx - 1, ty - fonthgt + 2, strwidth + 2, fonthgt);

						g.setColor( Color.BLACK );
						g.setFont( font );
						g.drawString( name, tx, ty );

					}
				}
			}

			// Restore graphic device original settings
			g.setComposite( originalComposite );
			g.setStroke( originalStroke );
			g.setColor( originalColor );
		}

	}


	/**
	 * Update data to show in the overlay.
	 * @param state the viewer state
	 */
	public void setViewerState( final ViewerState state )
	{
		/*
		 * Collect current view.
		 */
		state.getViewerTransform( transform );
	}

	public void setEstimatedTransform( final RealTransform estimatedXfm )
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

