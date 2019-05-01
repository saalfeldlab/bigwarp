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

public class TransformTypeSelectDialog extends JDialog
{
	private static final long serialVersionUID = 1L;
	
	public static final String TPS = "Thin Plate Spline";
	public static final String AFFINE = "Affine";
	public static final String SIMILARITY = "Similarity";
	public static final String ROTATION = "Rotation";
	public static final String TRANSLATION = "Translation";
	
	private final BigWarp bw;

	/**
	 * Instantiates and displays a JFrame that enables
	 * the selection of the transformation type.
	 * 
	 * 
	 * @param owner the parent frame
	 */
	public TransformTypeSelectDialog( final Frame owner, final BigWarp bw, final String startType )
	{
		super( owner, "Transform Type select", false );

		this.bw = bw;
		this.setLayout( new BorderLayout() );
		
		JRadioButton tpsButton = new JRadioButton( TPS );
		JRadioButton affineButton = new JRadioButton( AFFINE );
		JRadioButton similarityButton = new JRadioButton( SIMILARITY );
		JRadioButton rotationButton = new JRadioButton( ROTATION );
		JRadioButton translationButton = new JRadioButton( TRANSLATION );
		
		ButtonGroup group = new ButtonGroup();
		group.add( tpsButton );
		group.add( affineButton );
		group.add( similarityButton );
		group.add( rotationButton );
		group.add( translationButton );

		switch( startType )
		{
		case TPS:
			tpsButton.setSelected( true );
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

		addActionListender( tpsButton );
		addActionListender( affineButton );
		addActionListender( similarityButton );
		addActionListender( rotationButton );
		addActionListender( translationButton );
		
		JPanel radioPanel = new JPanel( new GridLayout(0, 1));
		radioPanel.add(tpsButton);
		radioPanel.add(affineButton);
		radioPanel.add(similarityButton);
		radioPanel.add(rotationButton);
		radioPanel.add(translationButton);
		
		radioPanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"Transform type" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );

		add( radioPanel, BorderLayout.LINE_START );
		pack();
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
}
