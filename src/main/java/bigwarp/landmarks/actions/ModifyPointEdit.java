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
	
	//private final boolean  isWarped;
	private final double[] newWarpedPos;
	private final double[] oldWarpedPos;

	public ModifyPointEdit( final LandmarkTableModel ltm, final int index, 
			final double[] oldpt, double[] newpt, 
			final double[] oldwarped, final double[] warped, 
			final boolean isMoving )
	{
		super(ltm);
		this.index = index;
		this.isMoving = isMoving;
		
		this.oldpt = Arrays.copyOf( oldpt, oldpt.length );
		this.newpt = Arrays.copyOf( newpt, newpt.length );

		//isWarped = true;
		oldWarpedPos = oldwarped;
		newWarpedPos = warped;
	}
	
	@Override
	public void undo()
	{
		ltm.pointEdit( index, oldpt, false, isMoving, oldWarpedPos, false );
	}

	@Override
	public void redo()
	{
		ltm.pointEdit( index, newpt, false, isMoving, newWarpedPos, false );
	}
	
	@Override
	public void postProcessUndo()
	{
		// if this edits the fixed image, AND it was changed to be "empty"
		if( (!isMoving && Arrays.equals( ltm.getPendingPoint(), oldpt ) && !ltm.isActive( index )) ||
			 isMoving && oldWarpedPos != null )
		{
			ltm.updateWarpedPoint( index, oldWarpedPos );
		}
		else if( isMoving && newWarpedPos != null  )
		{
			ltm.updateWarpedPoint( index, newWarpedPos );
		}
	}
	
	@Override
	public void postProcessRedo()
	{
		// do nothing
	}

	public String toString()
	{
		String s = "ModifyPointEdit\n";
		s += "oldpt: " + XfmUtils.printArray( this.oldpt ) + "\n";
		s += "newpt: " + XfmUtils.printArray( this.newpt ) + "\n";
		return s;
	}
}
