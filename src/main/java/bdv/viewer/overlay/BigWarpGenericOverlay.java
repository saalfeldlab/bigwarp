package bdv.viewer.overlay;

import java.awt.Graphics2D;

import bdv.viewer.BigWarpViewerPanel;


public abstract class BigWarpGenericOverlay<T>
{
	protected BigWarpViewerPanel viewer;
	
	protected T obj;

	protected boolean visible;
	
	public BigWarpGenericOverlay( final BigWarpViewerPanel viewer, T obj )
	{
		this.viewer = viewer;
		this.obj = obj;
	}

	public void set( final T obj )
	{
		this.obj = obj;
	}
	
	public void setVisible( final boolean visible )
	{
		this.visible = visible;
	}
	
	public boolean isVisible()
	{
		return visible;
	}

	public abstract void paint( final Graphics2D g );
}
