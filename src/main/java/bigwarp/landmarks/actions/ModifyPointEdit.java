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
package bigwarp.landmarks.actions;

import java.util.Arrays;

import jitk.spline.XfmUtils;
import bigwarp.landmarks.LandmarkTableModel;

public class ModifyPointEdit extends AbstractPointEdit
{
	private static final long serialVersionUID = 6962786164691889547L;
	
	private final int index;
	private final double[] oldpt;
	private final double[] newpt;
	private final boolean isMoving;

	public ModifyPointEdit( final LandmarkTableModel ltm, final int index,
			final double[] oldpt, double[] newpt,
			final boolean isMoving )
	{
		super( ltm );
		this.index = index;
		this.isMoving = isMoving;

		this.oldpt = Arrays.copyOf( oldpt, oldpt.length );
		this.newpt = Arrays.copyOf( newpt, newpt.length );
	}
	
	@Override
	public void undo()
	{
		ltm.pointEdit( index, oldpt, false, isMoving, null, false );
	}

	@Override
	public void redo()
	{
		ltm.pointEdit( index, newpt, false, isMoving, null, false );
	}

	public String toString()
	{
		String s = "ModifyPointEdit\n";
		s += "oldpt: " + XfmUtils.printArray( this.oldpt ) + "\n";
		s += "newpt: " + XfmUtils.printArray( this.newpt ) + "\n";
		return s;
	}
}
