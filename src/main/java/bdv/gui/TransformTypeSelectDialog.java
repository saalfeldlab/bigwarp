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
package bdv.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bigwarp.BigWarp;
import bigwarp.source.PlateauSphericalMaskRealRandomAccessible;
import bigwarp.source.PlateauSphericalMaskRealRandomAccessible.FalloffType;;

public class TransformTypeSelectDialog extends JDialog
{
	private static final long serialVersionUID = 1L;
	
	public static final String TPS = "Thin Plate Spline";
	public static final String MASKEDTPS = "Masked Thin Plate Spline";
	public static final String MASKEDSIMTPS = "Masked Similarity + Thin Plate Spline";
	public static final String AFFINE = "Affine";
	public static final String SIMILARITY = "Similarity";
	public static final String ROTATION = "Rotation";
	public static final String TRANSLATION = "Translation";

	private final BigWarp< ? > bw;
	private String transformType;

	private final ButtonGroup group;
	private final JRadioButton tpsButton;
	private final JRadioButton maskedTpsButton;
	private final JRadioButton maskedSimTpsButton;
	private final JRadioButton affineButton;
	private final JRadioButton similarityButton;
	private final JRadioButton rotationButton;
	private final JRadioButton translationButton;

	private final JCheckBox autoEstimateMaskButton;

	private final ButtonGroup falloffGroup;
	private final JRadioButton gaussFalloffButton;
	private final JRadioButton cosFalloffButton;

	/**
	 * Instantiates and displays a JFrame that enables
	 * the selection of the transformation type.
	 * 
	 * @param owner the parent frame
	 * @param bw a bigwarp instance
	 */
	public TransformTypeSelectDialog( final Frame owner, final BigWarp< ? > bw )
	{
		super( owner, "Transform Type select", false );

		this.bw = bw;
		this.setLayout( new BorderLayout() );
		transformType = bw.getTransformType();

		tpsButton = new JRadioButton( TPS );
		maskedTpsButton = new JRadioButton( MASKEDTPS );
		maskedSimTpsButton = new JRadioButton( MASKEDSIMTPS );
		affineButton = new JRadioButton( AFFINE );
		similarityButton = new JRadioButton( SIMILARITY );
		rotationButton = new JRadioButton( ROTATION );
		translationButton = new JRadioButton( TRANSLATION );
		
		group = new ButtonGroup();
		group.add( tpsButton );
		group.add( maskedTpsButton );
		group.add( maskedSimTpsButton );
		group.add( affineButton );
		group.add( similarityButton );
		group.add( rotationButton );
		group.add( translationButton );

		updateButtonGroup();

		addActionListender( tpsButton );
		addActionListender( maskedTpsButton );
		addActionListender( maskedSimTpsButton );
		addActionListender( affineButton );
		addActionListender( similarityButton );
		addActionListender( rotationButton );
		addActionListender( translationButton );

		JPanel radioPanel = new JPanel( new GridLayout(0, 1));
		radioPanel.add( tpsButton );
		radioPanel.add( maskedTpsButton );
		radioPanel.add( maskedSimTpsButton );
		radioPanel.add( affineButton );
		radioPanel.add( similarityButton );
		radioPanel.add( rotationButton );
		radioPanel.add( translationButton );
		
		radioPanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"Transform type" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );

		add( radioPanel, BorderLayout.PAGE_START );


		// panel containing options for transforms and mask
		JPanel optionsPanel = new JPanel( new GridLayout(0, 1));

		optionsPanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"options" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );

		autoEstimateMaskButton = new JCheckBox( "Auto-estimate mask");
		optionsPanel.add( autoEstimateMaskButton );

		cosFalloffButton = new JRadioButton( FalloffType.COSINE.toString() );
		gaussFalloffButton = new JRadioButton( FalloffType.GAUSSIAN.toString() );
		cosFalloffButton.setSelected( true );

		addFalloffActionListender( cosFalloffButton );
		addFalloffActionListender( gaussFalloffButton );

		falloffGroup = new ButtonGroup();
		falloffGroup.add( cosFalloffButton );
		falloffGroup.add( gaussFalloffButton );

		optionsPanel.add( new JLabel( "Mask falloff shape" ) );
		optionsPanel.add( cosFalloffButton );
		optionsPanel.add( gaussFalloffButton );

		add( optionsPanel, BorderLayout.PAGE_END );

		pack();
		addListeners();
		updateOptions();
	}

	private void addListeners()
	{
		maskedTpsButton.addChangeListener( new ChangeListener() {
			@Override
			public void stateChanged( ChangeEvent e ) { updateOptions(); }
		});

	}

	private synchronized void updateOptions()
	{
		autoEstimateMaskButton.setVisible( maskedTpsButton.isSelected() );
		cosFalloffButton.setVisible( maskedTpsButton.isSelected() );
		gaussFalloffButton.setVisible( maskedTpsButton.isSelected() );
		pack();
	}

	private void updateButtonGroup()
	{
		switch( transformType )
		{
		case TPS:
			tpsButton.setSelected( true );
			break;
		case MASKEDTPS:
			maskedTpsButton.setSelected( true );
			break;
		case MASKEDSIMTPS:
			maskedSimTpsButton.setSelected( true );
			break;
		case AFFINE:
			affineButton.setSelected( true );
			break;
		case SIMILARITY:
			similarityButton.setSelected( true );
			break;
		case ROTATION:
			rotationButton.setSelected( true );
			break;
		case TRANSLATION:
			translationButton.setSelected( true );
			break;
		}
	}

	public void addActionListender( final JRadioButton button )
	{
		button.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				bw.setTransformType( button.getText() );
			}
		});
	}

	public void addFalloffActionListender( final JRadioButton button )
	{
		button.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				bw.getTpsMaskSource().getRandomAccessible().setType( button.getText() );
				bw.getViewerFrameP().getViewerPanel().requestRepaint();
				bw.getViewerFrameQ().getViewerPanel().requestRepaint();
			}
		} );
	}

	public void setTransformType( String transformType )
	{
		this.transformType = transformType;
		updateButtonGroup();
		this.validate();
		this.repaint();
	}

	public boolean autoEstimateMask()
	{
		return autoEstimateMaskButton.isSelected();
	}
}
