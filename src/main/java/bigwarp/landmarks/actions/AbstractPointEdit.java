package bigwarp.landmarks.actions;

import javax.swing.undo.AbstractUndoableEdit;

import bigwarp.landmarks.LandmarkTableModel;

public abstract class AbstractPointEdit extends AbstractUndoableEdit
{
	private static final long serialVersionUID = 6129026885209095156L;

	protected final LandmarkTableModel ltm;

	public AbstractPointEdit( LandmarkTableModel ltm )
	{
		this.ltm = ltm;
	}

	public abstract void undo();

	public abstract void redo();

	public abstract String toString();
}