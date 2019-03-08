package bdv.gui;

import bdv.BehaviourTransformEventHandler3D;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import bdv.viewer.animate.MessageOverlayAnimator;
import bigwarp.BehaviourTransformEventHandler2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformEventHandlerFactory;

public class BigWarpViewerOptions extends ViewerOptions
{
	public final boolean is2d;

	private TransformEventHandlerFactory< AffineTransform3D > factory;
	
	private final BwValues bwValues = new BwValues();
	
	public TransformEventHandlerFactory< AffineTransform3D > getTransformEventHandlerFactory()
	{
		return factory;
	}

	public BigWarpViewerOptions( final boolean is2d )
	{
		this.is2d = is2d;
	}

	public BigWarpMessageAnimator getMessageAnimator()
	{
		return bwValues.messageAnimator;
	}
	
	public static BigWarpViewerOptions options()
	{
		return options( false );
	}
	
	public BwValues getValues()
	{
		return bwValues;
	}
	
	/**
	 * Set width of {@link ViewerPanel} canvas.
	 */
	public BigWarpViewerOptions size( final int width, final int height )
	{
		bwValues.width = width;
		bwValues.height = height;
		return this;
	}
	
	public static BigWarpViewerOptions options( final boolean is2d )
	{
		BigWarpViewerOptions out = new BigWarpViewerOptions( is2d );
		if ( is2d )
		{
			out.factory = BehaviourTransformEventHandler2D.factory();
		}
		else
		{
			out.factory = BehaviourTransformEventHandler3D.factory();
		}
		return out;
	}
	
	public static class BwValues extends Values
	{
		private BigWarpMessageAnimator messageAnimator;
		
		private MessageOverlayAnimator msgOverlayP; 

		private MessageOverlayAnimator msgOverlayQ; 
		
		private int width;
		
		private int height;

		public BwValues()
		{
			super();
			messageAnimator = new BigWarpMessageAnimator( 1500, 0.01, 0.1 );
			msgOverlayP = messageAnimator.msgAnimatorP;
			msgOverlayQ = messageAnimator.msgAnimatorQ;
		}
		
		public int getWidth()
		{
			return width;
		}

		public int getHeight()
		{
			return height;
		}

		@Override
		public MessageOverlayAnimator getMsgOverlay()
		{
			return msgOverlayQ;
		}

		public MessageOverlayAnimator getMsgOverlayMoving()
		{
			return msgOverlayP;
		}
	}
}
