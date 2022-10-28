package bigwarp.overlay;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import bdv.util.Affine3DHelpers;
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.OverlayRenderer;
import net.imglib2.RealRandomAccess;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;

public class DfieldOverlay implements OverlayRenderer
{
	protected final BigWarpViewerPanel viewer;
	
	protected Color negColor;

	protected Color posColor;
	
	protected RealRandomAccess<? extends RealType<?>>[] displacements;
	
	protected boolean visible;

	protected AffineTransform3D viewerTransform;

	protected int width, height;

	public DfieldOverlay( final BigWarpViewerPanel viewer )
	{
		this.viewer = viewer;
		viewerTransform = new AffineTransform3D();
		visible = false;
	}
	
	public void setDisplacementField( RealRandomAccess<? extends RealType<?>>... displacements )
	{
		this.displacements = displacements;
	}
	
	public void setVisible( final boolean visible )
	{
		this.visible = visible;
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
