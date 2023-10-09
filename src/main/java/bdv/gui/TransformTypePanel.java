package bdv.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import bigwarp.BigWarp;
import bigwarp.transforms.BigWarpTransform;

public class TransformTypePanel extends JPanel
{
	private static final long serialVersionUID = 3285885870885172257L;

	private static final String TRANSFORM_TYPE_HELP_TEXT = "Select the type of transformation.";

	public static final String[] TRANSFORM_TYPE_STRINGS = new String[] {
				BigWarpTransform.TPS, BigWarpTransform.AFFINE,
				BigWarpTransform.SIMILARITY, BigWarpTransform.ROTATION, BigWarpTransform.TRANSLATION };

	private final BigWarp< ? > bw;

	private final JLabel transformTypeLabel;
	private final JComboBox< String > transformTypeDropdown;

	private boolean active;

	public TransformTypePanel( BigWarp<?> bw )
	{
		super( new GridBagLayout() );
		this.bw = bw;
		active = true;

		setBorder( BorderFactory.createCompoundBorder(
		BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
		BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(
						BorderFactory.createEtchedBorder(),
						"Transformation options" ),
				BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );

		transformTypeLabel = new JLabel( "Transform type");
		transformTypeLabel.setToolTipText( TRANSFORM_TYPE_HELP_TEXT );
		transformTypeDropdown = new JComboBox<>( TRANSFORM_TYPE_STRINGS );
		getTransformTypeDropdown().setToolTipText( TRANSFORM_TYPE_HELP_TEXT );
		getTransformTypeDropdown().addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e )
			{
				if( active )
				{
					final String type = (String)transformTypeDropdown.getSelectedItem();
					bw.setTransformType( type );
					bw.updateTransformTypeDialog( type );
				}
			}
		});

		// layout
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets( 5, 5, 5, 5 );
		add( transformTypeLabel, gbc );

		gbc.gridx = 2;
		gbc.weightx = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		add( getTransformTypeDropdown(), gbc );	

	}

	public JComboBox< String > getTransformTypeDropdown()
	{
		return transformTypeDropdown;
	}

	public void setType( String type )
	{
		transformTypeDropdown.setSelectedItem( type );
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
