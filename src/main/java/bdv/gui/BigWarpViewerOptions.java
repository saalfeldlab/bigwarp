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
package bdv.gui;

import bdv.TransformEventHandler2D;
import bdv.TransformEventHandler3D;

import bdv.TransformEventHandlerFactory;
import org.scijava.ui.behaviour.io.InputTriggerConfig;

import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import bdv.viewer.animate.MessageOverlayAnimator;

public class BigWarpViewerOptions extends ViewerOptions
{
	public final boolean is2d;

	private final BwValues bwValues = new BwValues();

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

	@Override
	public BigWarpViewerOptions inputTriggerConfig( final InputTriggerConfig c )
	{
		super.inputTriggerConfig( c );
		return this;
	}

	@Override
	public ViewerOptions transformEventHandlerFactory( final TransformEventHandlerFactory f )
	{
		super.transformEventHandlerFactory( f );
		return this;
	}

	public BwValues getValues()
	{
		return bwValues;
	}

	/**
	 * Set width of {@link ViewerPanel} canvas.
	 *
	 * @param width of the viewer canvas
	 * @param height of the viewer canvas
	 * @return the new options
	 */
	public BigWarpViewerOptions size( final int width, final int height )
	{
		super.width( width );
		super.height( height );
		return this;
	}

	public static BigWarpViewerOptions options( final boolean is2d )
	{
		BigWarpViewerOptions out = new BigWarpViewerOptions( is2d );
		out.transformEventHandlerFactory( is2d
				? TransformEventHandler2D::new
				: TransformEventHandler3D::new );
		return out;
	}

	public BigWarpViewerOptions copy()
	{
		BigWarpViewerOptions out = new BigWarpViewerOptions( is2d );
		out.
				width( values.getWidth() ).
				height( values.getHeight() ).
				screenScales( values.getScreenScales() ).
				targetRenderNanos( values.getTargetRenderNanos() ).
				numRenderingThreads( values.getNumRenderingThreads() ).
				numSourceGroups( values.getNumSourceGroups() ).
				useVolatileIfAvailable( values.isUseVolatileIfAvailable() ).
				msgOverlay( values.getMsgOverlay() ).
				transformEventHandlerFactory( values.getTransformEventHandlerFactory() ).
				accumulateProjectorFactory( values.getAccumulateProjectorFactory() ).
				inputTriggerConfig( values.getInputTriggerConfig() );
		return out;
	}

	public static class BwValues
	{
		private BigWarpMessageAnimator messageAnimator;

		private MessageOverlayAnimator msgOverlayP;

		private MessageOverlayAnimator msgOverlayQ;

		public BwValues()
		{
			super();
			messageAnimator = new BigWarpMessageAnimator( 1500, 0.01, 0.1 );
			msgOverlayP = messageAnimator.msgAnimatorP;
			msgOverlayQ = messageAnimator.msgAnimatorQ;
		}

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
