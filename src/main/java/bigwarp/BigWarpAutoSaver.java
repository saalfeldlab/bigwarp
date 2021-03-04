package bigwarp;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

import bigwarp.landmarks.LandmarkTableModel;

/**
 * Saves bigwarp landmarks to a file periodically, 
 * but only if modification has occured since the last save.
 * 
 * @author John Bogovic
 *
 */
public class BigWarpAutoSaver 
{
	private final BigWarp<?> bw;

	final Timer timer;

	final AutoSave saveTask;

	final long period;

	public BigWarpAutoSaver( final BigWarp<?> bw, final long period )
	{
		this.bw = bw;
		this.period = period;
		bw.autoSaver = this;
		timer = new Timer();
		saveTask = new AutoSave();
		timer.schedule( saveTask, period, period );
	}

	public long getPeriod()
	{
		return period;
	}

	public void stop()
	{
		saveTask.cancel();
		timer.cancel();
	}

	private class AutoSave extends TimerTask 
	{
		@Override
		public void run()
		{
			LandmarkTableModel ltm = bw.getLandmarkPanel().getTableModel();
			if( ltm.isModifiedSinceSave() )
			{
//				try
//				{
					bw.autoSaveLandmarks();
//					ltm.save( fileSupplier.get() );
//				}
//				catch ( IOException e ) { e.printStackTrace(); }
			}
		}
	}
	
	public static void setAutosaveOptions( final BigWarp<?> bw, final long period, final String autoSavePath )
	{
		bw.setAutosaveFolder( new File( autoSavePath ) );

		if( period > 0 )
		{
			bw.warpVisDialog.doAutoSaveBox.setSelected( true );
			int periodMinutes = ( int ) ( period / 60000 );
			bw.warpVisDialog.autoSavePeriodSpinner.setValue( periodMinutes );
			if( bw.autoSaver != null )
				bw.autoSaver.stop();

			new BigWarpAutoSaver( bw, period );
			bw.warpVisDialog.repaint();
		}
		else
		{
			bw.warpVisDialog.doAutoSaveBox.setSelected( false );
			bw.warpVisDialog.repaint();
		}
	}
}
