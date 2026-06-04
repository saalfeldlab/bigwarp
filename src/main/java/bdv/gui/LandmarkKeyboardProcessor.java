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
package bdv.gui;

import java.awt.KeyEventPostProcessor;
import java.awt.event.KeyEvent;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import bigwarp.BigWarp;

public class LandmarkKeyboardProcessor implements KeyEventPostProcessor 
{
	BigWarp bw;
	
	public LandmarkKeyboardProcessor( BigWarp bw )
	{
		this.bw = bw;
	}
	
	@Override
	public boolean postProcessKeyEvent(KeyEvent ke ) {
		// if the event is consumed, ignore it
		if( ke.isConsumed())
			return false;
		
		if( ke.getKeyCode() == KeyEvent.VK_ESCAPE && ke.getID() == KeyEvent.KEY_RELEASED  )
		{
			if( bw.getLandmarkPanel().getTableModel().isPointUpdatePending() )
			{
				bw.getLandmarkPanel().getTableModel().restorePendingUpdate();
			}
		}
		return false;
	}

}
