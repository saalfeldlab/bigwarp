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
package bigwarp.landmarks.actions;

import java.util.Arrays;

import jitk.spline.XfmUtils;
import bigwarp.landmarks.LandmarkTableModel;

public class AddPointEdit extends AbstractPointEdit
{
	private static final long serialVersionUID = 3080160649652516963L;
	
	private final int index;
	private final double[] newpt;
	private final boolean isMoving;

	public AddPointEdit( final LandmarkTableModel ltm, final int index, final double[] pt, final boolean isMoving )
	{
		super( ltm );
		this.index = index;
		this.newpt = Arrays.copyOf( pt, pt.length );
		this.isMoving = isMoving;
	}

	@Override
	public void undo()
	{
		ltm.deleteRowHelper( index );
	}

	@Override
	public void redo()
	{
		ltm.pointEdit( index, newpt, true, isMoving, null, false );
	}

	public String toString()
	{
		String s = "AddPointEdit\n";
		s += "newpt: " + XfmUtils.printArray( this.newpt ) + "\n";
		return s;
	}

}
