package bdv.viewer.overlay;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import bdv.util.Affine3DHelpers;
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.OverlayRenderer;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

public class BigWarpMaskSphereOverlay implements OverlayRenderer
{

	final static protected BasicStroke stroke = new BasicStroke( 1 );
	final protected BigWarpViewerPanel viewer;

	protected double[] radii;
	protected final double[] viewerCoords;
	protected double minWidth = 0.1;

	protected final Color[] colors;
	final protected AffineTransform3D viewerTransform;

	protected double[] center;
	protected int width, height;
	protected boolean is3d;
	protected boolean visible = false;

	public BigWarpMaskSphereOverlay( final BigWarpViewerPanel viewer, boolean is3d )
	{
		this( viewer, new double[]{0,0}, new Color[]{ Color.ORANGE, Color.YELLOW }, is3d );
	}

	public BigWarpMaskSphereOverlay( final BigWarpViewerPanel viewer, final Color[] colors, boolean is3d )
	{
		this( viewer, new double[]{0, 0}, colors, is3d );
	}

	public BigWarpMaskSphereOverlay( final BigWarpViewerPanel viewer, final double[] radii, final Color[] colors, boolean is3d )
	{
		this.viewer = viewer;
		this.radii = radii;
		this.colors = colors;
		viewerCoords = new double[ 3 ];
		center = new double[ 3 ];
		this.is3d = is3d;
		viewerTransform = new AffineTransform3D();
	}

	public double[] getCenter()
	{
		return center;
	}

	public double[] getRadii()
	{
		return radii;
	}

	public void setCenter( final double[] center )
	{
		this.center = center;
	}

	public void setCenter( final RealLocalizable c )
	{
		c.localize( center );
	}

	public void setCenter( final RealPoint center )
	{
		center.localize( this.center );
	}

	public void setRadii( double[] radii )
	{
		this.radii = radii;
	}
	
	public void setInnerRadius( double inner )
	{
		double del = radii[1] - radii[0];
		radii[0] = inner;
		radii[1] = inner + del;
	}

	public void setOuterRadiusDelta( double outerDelta )
	{
		radii[1] = radii[0] + outerDelta;
	}

	public void setColor( final Color color, final int i )
	{
		colors[ i ] = color;
	}

	public void setColors( final Color[] colors )
	{
		System.arraycopy( colors, 0, this.colors, 0, Math.min( colors.length, this.colors.length ) );
	}

	public void setVisible( final boolean visible )
	{
		this.visible = visible;
		viewer.requestRepaint();
	}

	public void toggleVisible()
	{
		setVisible( !visible );
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		if ( visible )
		{
			final Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
			g2d.setComposite( AlphaComposite.SrcOver );

			final double scale;
			viewer.state().getViewerTransform( viewerTransform ); // synchronized
			scale = Affine3DHelpers.extractScale( viewerTransform, 0 );

			viewerTransform.apply( center, viewerCoords );

			final double zv;
			if( is3d )
				zv = viewerCoords[ 2 ];
			else 
				zv = 0;

			final double dz2 = zv * zv;
			for ( int i = 0; i < radii.length; ++i )
			{
				final double rad = radii[i];
				final double scaledRadius = scale * rad;
				final double arad;
				if( is3d )
					arad = Math.sqrt( scaledRadius * scaledRadius - dz2 );
				else
					arad = rad;
				
				final int rarad = (int)Math.round( arad );
				if ( viewerCoords[0] + scaledRadius > 0 && viewerCoords[0] - scaledRadius < width
						&& viewerCoords[1] + scaledRadius > 0 && viewerCoords[1] - scaledRadius < height )
				{
					g2d.setColor( colors[ i ] );
					g2d.setStroke( stroke );
					g2d.drawOval( (int)viewerCoords[0] - rarad, (int)viewerCoords[1] - rarad,
							(2 * rarad + 1), 2 * rarad + 1 );
				}
			}
		}
	}

	@Override
	public void setCanvasSize( final int width, final int height )
	{
		this.width = width;
		this.height = height;
	}

}
