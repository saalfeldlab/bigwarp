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
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import bigwarp.BigWarp;
import bigwarp.transforms.BigWarpTransform;

public class TransformTypeSelectDialog extends JDialog
{
	private static final long serialVersionUID = 1L;

	@Deprecated
	public static final String TPS = "Thin Plate Spline";
	@Deprecated
	public static final String MASKEDTPS = "Masked Thin Plate Spline";
	@Deprecated
	public static final String MASKEDSIMTPS = "Masked Similarity + Thin Plate Spline";
	@Deprecated
	public static final String AFFINE = "Affine";
	@Deprecated
	public static final String SIMILARITY = "Similarity";
	@Deprecated
	public static final String ROTATION = "Rotation";
	@Deprecated
	public static final String TRANSLATION = "Translation";

	private final BigWarp< ? > bw;
	private String transformType;

	private final ButtonGroup group;
	private final JRadioButton tpsButton;
	private final JRadioButton affineButton;
	private final JRadioButton similarityButton;
	private final JRadioButton rotationButton;
	private final JRadioButton translationButton;

	private boolean active;

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
		active = true;
		this.setLayout( new BorderLayout() );
		transformType = bw.getTransformType();

		tpsButton = new JRadioButton( BigWarpTransform.TPS );
		affineButton = new JRadioButton( BigWarpTransform.AFFINE );
		similarityButton = new JRadioButton( BigWarpTransform.SIMILARITY );
		rotationButton = new JRadioButton( BigWarpTransform.ROTATION );
		translationButton = new JRadioButton( BigWarpTransform.TRANSLATION );

		group = new ButtonGroup();
		group.add( tpsButton );
		group.add( affineButton );
		group.add( similarityButton );
		group.add( rotationButton );
		group.add( translationButton );

		updateButtonGroup();

		addActionListender( tpsButton );
		addActionListender( affineButton );
		addActionListender( similarityButton );
		addActionListender( rotationButton );
		addActionListender( translationButton );

		final JPanel radioPanel = new JPanel( new GridLayout(0, 1));
		radioPanel.add( tpsButton );
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

		pack();
	}

	private void updateButtonGroup()
	{
		switch( transformType )
		{
		case BigWarpTransform.TPS:
			tpsButton.setSelected( true );
			break;
		case BigWarpTransform.AFFINE:
			affineButton.setSelected( true );
			break;
		case BigWarpTransform.SIMILARITY:
			similarityButton.setSelected( true );
			break;
		case BigWarpTransform.ROTATION:
			rotationButton.setSelected( true );
			break;
		case BigWarpTransform.TRANSLATION:
			translationButton.setSelected( true );
			break;
		}
	}

	public void addActionListender( final JRadioButton button )
	{
		button.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( active )
				{
					final String type = button.getText();
					bw.setTransformType( type );
					bw.updateTransformTypePanel( type );
				}
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
				bw.getTransformPlateauMaskSource().getRandomAccessible().setFalloffShape( button.getText() );
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

	/**
	 * After calling deactivate, updates to this panel won't affect Bigwarp.
	 */
	public void deactivate()
	{
		active = false;
	}

	/**
	 * After calling activate, updates to this panel will affect Bigwarp.
	 */
	public void activate()
	{
		active = true;
	}

}
