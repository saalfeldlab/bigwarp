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

import javax.swing.undo.AbstractUndoableEdit;

import bigwarp.landmarks.LandmarkTableModel;

public abstract class AbstractPointEdit extends AbstractUndoableEdit {

	private static final long serialVersionUID = 6129026885209095156L;

	protected final LandmarkTableModel ltm;

	public AbstractPointEdit(LandmarkTableModel ltm) {

		this.ltm = ltm;
	}

	public abstract void undo();

	public abstract void redo();

	public abstract String toString();
}
