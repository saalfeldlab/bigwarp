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

import bigwarp.landmarks.LandmarkTableModel;

public class DeleteRowEdit extends AbstractPointEdit
{
	private static final long serialVersionUID = -3624020789748090982L;

	private final int index;
	private final double[] movingPt;
	private final double[] targetPt;
	
	public DeleteRowEdit( final LandmarkTableModel ltm, final int index )
	{
		super( ltm );
		this.index = index;
		
		movingPt = LandmarkTableModel.toPrimitive( ltm.getPoints( true ).get( index ) );
		targetPt = LandmarkTableModel.toPrimitive( ltm.getPoints( false ).get( index ) );
	}

	@Override
	public void undo()
	{
		ltm.pointEdit( index, movingPt, true, true, null, false );
		ltm.pointEdit( index, targetPt, false, false, null, false );
	}

	@Override
	public void redo()
	{
		ltm.deleteRowHelper( index );
	}

	@Override
	public String toString()
	{
		return "DeletePointEdit " + index;
	}
}
