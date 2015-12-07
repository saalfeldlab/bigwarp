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
