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
package bdv.gui;

import bigwarp.BigWarp;
import bigwarp.BigWarpAutoSaver;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class AutosaveOptionsPanel extends JPanel
{
	private static final long serialVersionUID = 2449704984531905538L;

	private final BigWarp< ? > bw;

	private final SpinnerNumberModel savePeriodModel;
	private final JSpinner autoSavePeriodSpinner;
	private final JCheckBox doAutoSaveBox;
	private final JTextField autoSaveFolderText;

	private int lastAutoSaveFreq = 5;
	private boolean updating = false;

	public AutosaveOptionsPanel( final BigWarp< ? > bw, final Container content )
	{
		super( new GridBagLayout() );
		this.bw = bw;

		doAutoSaveBox = new JCheckBox( "Auto-save landmarks" );

		final JLabel autoSavePeriodLabel = new JLabel( "Frequency (minutes)" );
		autoSavePeriodSpinner = new JSpinner();
		savePeriodModel = new SpinnerNumberModel( 0, 0, 0, 1 );
		getAutoSavePeriodSpinner().setModel( savePeriodModel );
		((DefaultEditor)getAutoSavePeriodSpinner().getEditor()).getTextField().setEditable( false );
		((DefaultEditor)getAutoSavePeriodSpinner().getEditor()).getTextField().setEnabled( false );
		getAutoSavePeriodSpinner().addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				if ( getDoAutoSaveBox().isSelected() && !updating )
				{
					long periodMillis = ((Integer) savePeriodModel.getValue()).longValue() * 60000;
					BigWarpAutoSaver autoSaver = bw.getAutoSaver();
					if ( autoSaver != null )
						autoSaver.stop();

					bw.setAutoSaver( new BigWarpAutoSaver( bw, periodMillis ));
				}
			}
		} );

		getDoAutoSaveBox().addItemListener( new ItemListener()
		{
			@Override
			public void itemStateChanged( ItemEvent e )
			{
				bw.stopAutosave();
				if ( getDoAutoSaveBox().isSelected() )
				{
					updating = true;
					((DefaultEditor)getAutoSavePeriodSpinner().getEditor()).getTextField().setEditable( true );
					((DefaultEditor)getAutoSavePeriodSpinner().getEditor()).getTextField().setEnabled( true );
					savePeriodModel.setMinimum( 1 );
					savePeriodModel.setMaximum( 5000 );
					savePeriodModel.setValue( lastAutoSaveFreq );

					long periodMillis = ((Integer) savePeriodModel.getValue()).longValue() * 60000;
					bw.setAutoSaver( new BigWarpAutoSaver( bw, periodMillis ));
					updating = false;
				}
				else
				{
					lastAutoSaveFreq =  ((Integer) savePeriodModel.getValue());
					savePeriodModel.setMinimum( 0 );
					savePeriodModel.setMaximum( 0 );
					savePeriodModel.setValue( 0 );
					((DefaultEditor)getAutoSavePeriodSpinner().getEditor()).getTextField().setEditable( false );
					((DefaultEditor)getAutoSavePeriodSpinner().getEditor()).getTextField().setEnabled( false );
				}
			}
		} );

		final JLabel destDirLabel = new JLabel( "Directory" );
		final File startingFolder = bw.getBigwarpSettingsFolder();
		autoSaveFolderText = new JTextField();
		getAutoSaveFolderText().setText( startingFolder.getAbsolutePath() );

		final JButton browseBtn = new JButton( "Browse" );
		browseBtn.addActionListener( e -> {

			final JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
			fileChooser.setCurrentDirectory( startingFolder );

			final int ret = fileChooser.showOpenDialog( content );
			if ( ret == JFileChooser.APPROVE_OPTION )
			{
				final File folder = fileChooser.getSelectedFile();
				getAutoSaveFolderText().setText( folder.getAbsolutePath() );
				bw.getAutoSaver().setAutosaveFolder( folder );
			}
		} );

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 1.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets( 5, 5, 5, 5 );

		add( getDoAutoSaveBox(), gbc );

		gbc.weightx = 1.0;
		gbc.gridx = 2;
		gbc.gridwidth = 1;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.LINE_END;
		add( autoSavePeriodLabel, gbc );

		gbc.gridx = 3;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add( getAutoSavePeriodSpinner(), gbc );

		gbc.gridy = 2;
		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.NONE;
		add( destDirLabel, gbc );

		gbc.gridy = 2;
		gbc.gridx = 1;
		gbc.gridwidth = 3;
		gbc.weightx = 1.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add( getAutoSaveFolderText(), gbc );

		gbc.gridx = 4;
		gbc.weightx = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		add( browseBtn, gbc );
	}

	public JSpinner getAutoSavePeriodSpinner()
	{
		return autoSavePeriodSpinner;
	}

	public JCheckBox getDoAutoSaveBox()
	{
		return doAutoSaveBox;
	}

	public JTextField getAutoSaveFolderText()
	{
		return autoSaveFolderText;
	}

}
