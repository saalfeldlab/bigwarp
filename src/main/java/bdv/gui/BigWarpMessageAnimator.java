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
