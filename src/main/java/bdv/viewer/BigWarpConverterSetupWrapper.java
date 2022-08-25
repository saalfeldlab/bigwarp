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
package bdv.viewer;

import net.imglib2.type.numeric.ARGBType;
import bdv.tools.brightness.ConverterSetup;
import bigwarp.BigWarp;
import org.scijava.listeners.Listeners;

@Deprecated
public class BigWarpConverterSetupWrapper implements ConverterSetup {

	protected ConverterSetup cs;
	protected BigWarp bw;

	public BigWarpConverterSetupWrapper( BigWarp bw, ConverterSetup cs )
	{
		this.bw = bw;
		this.cs = cs;
	}

	public ConverterSetup getSourceConverterSetup(){
		return cs;
	}

	@Override
	public Listeners< SetupChangeListener > setupChangeListeners()
	{
		return cs.setupChangeListeners();
	}

	@Override
	public int getSetupId() {
		return cs.getSetupId();
	}


	@Override
	public void setColor(ARGBType color) {
		cs.setColor(color);
		bw.getViewerFrameP().getViewerPanel().requestRepaint();
		bw.getViewerFrameQ().getViewerPanel().requestRepaint();
	}


	@Override
	public ARGBType getColor() {
		return cs.getColor();
	}

	@Override
	public void setDisplayRange( double min, double max )
	{
		cs.setDisplayRange(min, max);
		bw.getViewerFrameP().getViewerPanel().requestRepaint();
		bw.getViewerFrameQ().getViewerPanel().requestRepaint();

	}

	@Override
	public boolean supportsColor()
	{
		return cs.supportsColor();
	}

	@Override
	public double getDisplayRangeMin()
	{
		return cs.getDisplayRangeMin();
	}

	@Override
	public double getDisplayRangeMax()
	{
		return cs.getDisplayRangeMax();
	}
}
