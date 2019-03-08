package bdv.gui;

import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.animate.MessageOverlayAnimator;

public class BigWarpMessageAnimator
{

	protected MessageOverlayAnimator msgAnimatorP;

	protected MessageOverlayAnimator msgAnimatorQ;

	private BigWarpViewerPanel viewerP;

	private BigWarpViewerPanel viewerQ;

	
	public BigWarpMessageAnimator( long duration, double fadeIn, double fadeOut,
			BigWarpViewerPanel viewerP, 
			BigWarpViewerPanel viewerQ )
	{
		this( duration, fadeIn, fadeOut );
		setViewers( viewerP, viewerQ );
	}

	public BigWarpMessageAnimator( long duration, double fadeIn, double fadeOut )
	{
		msgAnimatorP = new MessageOverlayAnimator( duration, fadeIn, fadeOut );
		msgAnimatorQ = new MessageOverlayAnimator( duration, fadeIn, fadeOut );
	}
	
	public void setViewers( BigWarpViewerPanel viewerP, BigWarpViewerPanel viewerQ )
	{
		this.viewerP = viewerP;
		this.viewerQ = viewerQ;
	}
	
	public void showMessageMoving( String message )
	{
		viewerP.showMessage( message );
	}

	public void showMessageFixed( String message )
	{
		viewerQ.showMessage( message );
	}

	public void showMessage( String message )
	{
		viewerP.showMessage( message );
		viewerQ.showMessage( message );
	}
	
	public MessageOverlayAnimator getAnimatorMoving()
	{
		return msgAnimatorP;
	}

	public MessageOverlayAnimator getAnimatorFixed()
	{
		return msgAnimatorQ;
	}
}
