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
package bigwarp;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

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
	transient private final BigWarp<?> bw;

	transient final Timer timer;

	transient final AutoSave saveTask;

	final long period;

	@SerializedName("location")
	@JsonAdapter( BigwarpSettings.FileAdapter.class )
	protected File autoSaveDirectory;


	public BigWarpAutoSaver( final BigWarp<?> bw, final long period )
	{
		this.bw = bw;
		this.period = period;
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
			final LandmarkTableModel ltm = bw.getLandmarkPanel().getTableModel();
			if( ltm.isModifiedSinceSave() )
			{
				bw.autoSaveLandmarks();
			}
		}
	}

	public static void setAutosaveOptions( final BigWarp<?> bw, final long period, final String autoSavePath )
	{

		if( period > 0 )
		{
			bw.warpVisDialog.getAutoSaveOptionsPanel().getDoAutoSaveBox().setSelected( true );
			final int periodMinutes = ( int ) ( period / 60000 );
			bw.warpVisDialog.getAutoSaveOptionsPanel().getAutoSavePeriodSpinner().setValue( periodMinutes );
			if( bw.autoSaver != null )
				bw.autoSaver.stop();

			final BigWarpAutoSaver autoSaver = new BigWarpAutoSaver( bw, period );
			bw.setAutoSaver( autoSaver );
			autoSaver.setAutosaveFolder( new File( autoSavePath ) );
		}
		else
		{
			bw.warpVisDialog.getAutoSaveOptionsPanel().getDoAutoSaveBox().setSelected( false );
			bw.warpVisDialog.repaint();
		}
	}

	/**
	 * Set the folder where the results of auto-saving will be stored.
	 *
	 * @param autoSaveFolder
	 * 		the destination folder
	 */
	public void setAutosaveFolder( final File autoSaveFolder )
	{
		boolean exists = autoSaveFolder.exists();
		if ( !exists )
			exists = autoSaveFolder.mkdir();

		if ( exists && autoSaveFolder.isDirectory() )
		{
			autoSaveDirectory = autoSaveFolder;
			bw.warpVisDialog.getAutoSaveOptionsPanel().getAutoSaveFolderText().setText( autoSaveFolder.getAbsolutePath() );
			bw.warpVisDialog.repaint();
		}
	}
}
