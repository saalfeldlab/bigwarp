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
package bdv.gui;

import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import bdv.viewer.animate.MessageOverlayAnimator;
import org.scijava.ui.behaviour.io.InputTriggerConfig;

public class BigWarpViewerOptions extends ViewerOptions
{
	private final BwValues bwValues = new BwValues();

	public BigWarpMessageAnimator getMessageAnimator()
	{
		return bwValues.messageAnimator;
	}

	public static BigWarpViewerOptions options()
	{
		return new BigWarpViewerOptions();
	}

	@Override
	public BigWarpViewerOptions inputTriggerConfig( final InputTriggerConfig c )
	{
		super.inputTriggerConfig( c );
		return this;
	}

	@Override
	public BigWarpViewerOptions is2D( final boolean is2D )
	{
		super.is2D( is2D );
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

	public BigWarpViewerOptions copy()
	{
		BigWarpViewerOptions out = new BigWarpViewerOptions();
		out.
				width( values.getWidth() ).
				height( values.getHeight() ).
				screenScales( values.getScreenScales() ).
				targetRenderNanos( values.getTargetRenderNanos() ).
				numRenderingThreads( values.getNumRenderingThreads() ).
				numSourceGroups( values.getNumSourceGroups() ).
				useVolatileIfAvailable( values.isUseVolatileIfAvailable() ).
				msgOverlay( values.getMsgOverlay() ).
				is2D( values.is2D() ).
				transformEventHandlerFactory( values.getTransformEventHandlerFactory() ).
				accumulateProjectorFactory( values.getAccumulateProjectorFactory() ).
				inputTriggerConfig( values.getInputTriggerConfig() ).
				shareKeyPressedEvents( values.getKeyPressedManager() ).
				keymapManager( values.getKeymapManager() ).
				appearanceManager( values.getAppearanceManager() );
		return out;
	}

	public ViewerOptions getViewerOptions( final boolean isMoving )
	{
		return copy().msgOverlay( isMoving
				? getMessageAnimator().getAnimatorMoving()
				: getMessageAnimator().getAnimatorFixed() );
	}

	public static class BwValues
	{
		private BigWarpMessageAnimator messageAnimator = new BigWarpMessageAnimator( 1500, 0.01, 0.1 );
	}
}
