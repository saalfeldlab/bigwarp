package bigwarp.landmarks.actions;

import javax.swing.undo.UndoManager;

public class LandmarkUndoManager extends UndoManager
{
	private static final long serialVersionUID = -2126698066459744227L;

	private AbstractPointEdit thisEdit;

	public void preProcessUndo()
	{
		thisEdit = (AbstractPointEdit)this.editToBeUndone();
	}

	public void preProcessRedo()
	{
		thisEdit = (AbstractPointEdit)this.editToBeRedone();
	}

	public void postProcess()
	{
		if( thisEdit != null)
			thisEdit.postProcessUndo();
	}
}