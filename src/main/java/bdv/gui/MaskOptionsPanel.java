package bdv.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bigwarp.BigWarp;
import bigwarp.BigWarpAutoSaver;
import bigwarp.source.PlateauSphericalMaskRealRandomAccessible.FalloffType;
import bigwarp.source.PlateauSphericalMaskSource;
import bigwarp.transforms.BigWarpTransform;

public class MaskOptionsPanel extends JPanel
{
	private static final long serialVersionUID = -8614381106547838575L;

	private static final String FALLOFF_HELP_TEXT = "Controls the shape of the mask in the transition region.";
	private static final String AUTO_ESTIMATE_HELP_TEXT = "If selected, the mask location and size will be dynamically updated as you add landmarks.";
	private static final String SHOW_MASK_OVERLAY_HELP_TEXT = "Toggles the visibility of the mask overlay";
	private static final String INTERPOLATION_HELP_TEXT = "Controls whether a mask is applied to the transformation and how the transformation changes "
			+ "in the mask transition region.\n"
			+ "If your transformation has lots of rotation, try selecting \"ROTATION\" or \"SIMILARITY\".";

	public static final String[] maskTypes = new String[] { 
			"NONE",
			BigWarpTransform.MASK_INTERP,
			BigWarpTransform.ROT_MASK_INTERP,
			BigWarpTransform.SIM_MASK_INTERP,
	};

	private final BigWarp< ? > bw;

	private final JCheckBox autoEstimateMaskButton;
	private final JCheckBox showMaskOverlayButton;

	private final JLabel falloffTypeLabel;
	private final JComboBox< FalloffType > falloffTypeDropdown;

	private final JLabel maskTypeLabel;
	private final JComboBox< String > maskTypeDropdown;

	private ActionListener falloffListener;

	public MaskOptionsPanel( BigWarp<?> bw )
	{
		super( new GridBagLayout() );
		this.bw = bw;

		setBorder( BorderFactory.createCompoundBorder(
		BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
		BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(
						BorderFactory.createEtchedBorder(),
						"Mask options" ),
				BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );
		
		autoEstimateMaskButton = new JCheckBox( "Auto-estimate mask");
		autoEstimateMaskButton.setToolTipText( AUTO_ESTIMATE_HELP_TEXT );
		autoEstimateMaskButton.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				if( autoEstimateMaskButton.isSelected() )
					bw.autoEstimateMask();
			}
		} );

		showMaskOverlayButton = new JCheckBox( "Show mask overlay", true );
		showMaskOverlayButton.setToolTipText( SHOW_MASK_OVERLAY_HELP_TEXT );
		showMaskOverlayButton.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				bw.setMaskOverlayVisibility( showMaskOverlayButton.isSelected() && isMask() );
			}
		} );

		falloffTypeLabel = new JLabel( "Mask falloff");
		falloffTypeLabel.setToolTipText( FALLOFF_HELP_TEXT );
		falloffTypeDropdown = new JComboBox<>( FalloffType.values() );
		falloffTypeDropdown.setToolTipText( FALLOFF_HELP_TEXT );

		maskTypeLabel = new JLabel( "Mask interpolation");
		maskTypeLabel.setToolTipText( INTERPOLATION_HELP_TEXT );
		maskTypeDropdown = new JComboBox<>( maskTypes );
		maskTypeDropdown.setToolTipText( INTERPOLATION_HELP_TEXT );
		maskTypeDropdown.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e )
			{
				bw.setMaskOverlayVisibility( autoEstimateMaskButton.isSelected() && isMask() );
				bw.getBwTransform().setMask( maskTypeDropdown.getItemAt( maskTypeDropdown.getSelectedIndex() ) );
				bw.restimateTransformation();
				bw.getViewerFrameP().getViewerPanel().requestRepaint();
				bw.getViewerFrameQ().getViewerPanel().requestRepaint();
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
		add( maskTypeLabel, gbc );	

		gbc.gridx = 2;
		gbc.weightx = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		add( maskTypeDropdown, gbc );	

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.LINE_END;
		add( falloffTypeLabel, gbc );	

		gbc.gridx = 2;
		gbc.anchor = GridBagConstraints.LINE_START;
		add( falloffTypeDropdown, gbc );	

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.anchor = GridBagConstraints.LINE_END;
		add( autoEstimateMaskButton, gbc );

		gbc.gridx = 2;
		gbc.gridy = 2;
		gbc.anchor = GridBagConstraints.LINE_START;
		add( showMaskOverlayButton, gbc );
	}

	public void setMask( PlateauSphericalMaskSource maskSource )
	{
		if( falloffListener == null )
			falloffTypeDropdown.removeActionListener( falloffListener );

		falloffTypeDropdown.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e )
			{
				maskSource.getRandomAccessible().setType( (FalloffType)falloffTypeDropdown.getSelectedItem() );
				bw.getViewerFrameP().getViewerPanel().requestRepaint();
				bw.getViewerFrameQ().getViewerPanel().requestRepaint();	
			}
		});

	}

	public JCheckBox getAutoEstimateMaskButton()
	{
		return autoEstimateMaskButton;
	}

	public JCheckBox getShowMaskOverlayButton()
	{
		return showMaskOverlayButton;
	}

	public JComboBox< FalloffType > getFalloffTypeDropdown()
	{
		return falloffTypeDropdown;
	}

	/**
	 * @return true if a mask is applied to the transformation
	 */
	public boolean isMask()
	{
		return maskTypeDropdown.getSelectedIndex() > 0;
	}

}
