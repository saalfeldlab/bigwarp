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
