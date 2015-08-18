package bdv.viewer;

import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import mpicbg.models.CoordinateTransform;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.viewer.state.ViewerState;
import bigwarp.landmarks.LandmarkTableModel;

public class BigWarpOverlay {
	
	/** The viewer state. */
	private ViewerState state;
	
	private BigWarpViewerPanel viewer;
	
	protected LandmarkTableModel landmarkModel;
	
	protected CoordinateTransform estimatedXfm;
	
	protected boolean isTransformed = false;
	
	/** The transform for the viewer current viewpoint. */
	private final AffineTransform3D transform = new AffineTransform3D();
	
	public BigWarpOverlay( final BigWarpViewerPanel viewer, LandmarkTableModel landmarkModel )
	{
		this.viewer = viewer;
		this.landmarkModel = landmarkModel;
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

		/*
		 * Draw spots.
		 */
		if ( viewer.getSettings().areLandmarksVisible() )
		{

			final double radiusRatio = ( Double ) viewer.getSettings().get( 
					BigWarpViewerSettings.KEY_SPOT_RADIUS_RATIO );
			
			final double radius = ( Double ) viewer.getSettings().get( 
					BigWarpViewerSettings.KEY_SPOT_SIZE);

			/*
			 * Compute scale
			 */
			// final double vx = transform.get( 0, 0 );
			// final double vy = transform.get( 1, 0 );
			// final double vz = transform.get( 2, 0 );
			// final double transformScale = Math.sqrt( vx * vx + vy * vy + vz * vz );

			Color color;
			Stroke stroke;
			stroke = BigWarpViewerSettings.NORMAL_STROKE;
			
			boolean is3d = true;
			int offset = 3;
			
			FontMetrics fm = null;
			int fonthgt = 0;
			Color textBoxColor = null;
			if ( viewer.getSettings().areNamesVisible() )
			{
				fm = g.getFontMetrics( g.getFont() );
				fonthgt = fm.getHeight();
				textBoxColor = Color.BLACK;
			}
			
			int index = 0;
			for ( final Double[] spot : landmarkModel.getPoints()) 
			{
				if( spot.length == 4 ){
					is3d = false;
					offset = 2;
				}
				
				if( landmarkModel.isActive(index) ){
					color  = (Color)viewer.getSettings().get( BigWarpViewerSettings.KEY_COLOR );
				}else{
					color  = (Color)viewer.getSettings().get( BigWarpViewerSettings.KEY_INACTIVE_COLOR );
				}              

				g.setColor( color );
				g.setStroke( stroke );

				double x=0.0, y=0.0, z=0.0;
				if( viewer.getIsMoving() && spot[0] != null )
				{
					if( !isTransformed )
					{
						x = spot[0];
						y = spot[1];
						
						if( is3d ) z = spot[2];
					}
					else if( isTransformed && landmarkModel.isWarpedPositionChanged( index ) )
					{
						// in this block, the viewer is displaying a transformed version
						// of the moving image, but the location of the point has changed
						// ( as a result, we can't use the target point ) 
						// so use the stored warped point
						
						x = landmarkModel.getWarpedPoints().get( index )[ 0 ];
						y = landmarkModel.getWarpedPoints().get( index )[ 1 ];
						
						//System.out.println( "rendering warped point" + index  + "   " + x + " " + y );
						
						if( is3d )
							z = landmarkModel.getWarpedPoints().get( index )[ 2 ];
					}
					else if( isTransformed && spot[ 0 ] < Double.MAX_VALUE )
					{
						// if we've provided a moving landmark here ( spot < double max value )
						// and we're displaying a transformed version, then
						// the landmark should be at the target location.
						
						x = spot[ offset ];
						y = spot[ offset + 1];
						
						if( is3d ) 
							z = spot[ offset + 2 ];
					}
					else
					{
						continue;
					}
				}
				else if( !viewer.getIsMoving() && spot[offset] != null )
				{
					x = spot[ offset ];
					y = spot[ offset + 1];
					
					if( is3d ) 
						z = spot[ offset + 2 ];
				}
				else
				{
					continue;
				}

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
								( int ) ( 2 * arad ), ( int ) ( 2 * arad ) );
					
					if ( viewer.getSettings().areNamesVisible() )
					{
						final int tx = ( int ) ( viewerCoords[ 0 ] + arad + 5 );
						final int ty = ( int ) viewerCoords[ 1 ];
						
						String name = landmarkModel.getNames().get(index);
						int strwidth = fm.stringWidth( name );
						
						textBoxColor = new Color( color.getRed(), color.getGreen(), color.getBlue(), 128 );
						
						g.setColor( textBoxColor );
						g.fillRect( tx - 1, ty - fonthgt + 2, strwidth + 2, fonthgt);
						
						g.setColor( Color.BLACK );
						g.drawString( name, tx, ty );
						
					}
				}
				
				index++;
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
	
	public void setEstimatedTransform( ThinPlateR2LogRSplineKernelTransform estimatedXfm )
	{
		this.estimatedXfm = estimatedXfm.deepCopy();
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

