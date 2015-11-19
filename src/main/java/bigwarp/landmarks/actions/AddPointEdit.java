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
	
	private final double[] warpedPos;

	public AddPointEdit( final LandmarkTableModel ltm, final int index, final double[] pt, final double[] warpedPos, final boolean isMoving )
	{
		super( ltm );
		this.index = index;
		this.newpt = Arrays.copyOf( pt, pt.length );
		this.warpedPos = warpedPos;
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
		ltm.pointEdit( index, newpt, true, isMoving, warpedPos, false );
	}
	
	@Override
	public void postProcessUndo()
	{
		if( ltm.getRowCount() > index && isMoving && warpedPos != null )
			ltm.updateWarpedPoint( index, warpedPos );
	}
	
	@Override
	public void postProcessRedo()
	{
		// do nothing
	}

	public String toString()
	{
		String s = "AddPointEdit\n";
		s += "newpt: " + XfmUtils.printArray( this.newpt ) + "\n";
		return s;
	}

}
