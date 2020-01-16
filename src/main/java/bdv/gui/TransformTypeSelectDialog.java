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
	
	private final BigWarp< ? > bw;
	private String transformType;

	private final JRadioButton tpsButton;
	private final JRadioButton affineButton;
	private final JRadioButton similarityButton;
	private final JRadioButton rotationButton;
	private final JRadioButton translationButton;

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
		affineButton = new JRadioButton( AFFINE );
		similarityButton = new JRadioButton( SIMILARITY );
		rotationButton = new JRadioButton( ROTATION );
		translationButton = new JRadioButton( TRANSLATION );
		
		ButtonGroup group = new ButtonGroup();
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
		
		JPanel radioPanel = new JPanel( new GridLayout(0, 1));
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

		add( radioPanel, BorderLayout.LINE_START );
		pack();
	}

	private void updateButtonGroup()
	{
		switch( transformType )
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

	public void setTransformType( String transformType )
	{
		this.transformType = transformType;
		updateButtonGroup();
		this.validate();
		this.repaint();
	}
}
