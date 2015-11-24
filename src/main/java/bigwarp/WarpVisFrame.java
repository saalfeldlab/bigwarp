package bigwarp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bdv.viewer.BigWarpViewerSettings;
import bigwarp.source.GridSource;

public class WarpVisFrame extends JDialog 
{
	private static final long serialVersionUID = 7561228647761694686L;

	private final BigWarp bw;
	private final BigWarpViewerSettings settings;
	
	protected ButtonGroup visTypeGroup;
	protected JRadioButton setWarpVisOffButton;
	protected JRadioButton setWarpGridButton;
	protected JRadioButton setWarpMagButton;
	
	protected JLabel noOptionsLabel;
	
	// landmark point options
	protected final JButton landmarkColorButton;
	private final JColorChooser colorChooser;
	protected final JSlider landmarkSizeSlider;
	
	// warp magnitude
	protected ButtonGroup warpMagButtons;
	protected JRadioButton warpMagAffineButton;
	protected JRadioButton warpMagSimilarityButton;
	protected JRadioButton warpMagRigidButton;
	
	// grid spacing
	protected ButtonGroup warpGridButtons;
	protected JRadioButton warpGridLineButton;
	protected JRadioButton warpGridModButton;
	protected JSlider gridSpacingSlider;
	protected JSlider gridWidthSlider;
	protected Component	 bigSpace;
	protected Component	 smallSpace;
	protected JLabel gridSpacingLabel;
	protected JLabel gridWidthLabel;
	
	public static final int minGridSpacing = 5;
	public static final int maxGridSpacing = 400;
	public static final int defaultGridSpacing = 100;
	
	public static final int minGridWidth = 1;
	public static final int maxGridWidth = 50;
	public static final int defaultGridWidth = 5;
	
	public WarpVisFrame( final Frame owner, final BigWarp bw )
	{
		super( owner, "big warp options", false );
		this.bw = bw;
		this.settings = bw.viewerSettings;
		
		final Container content = getContentPane();
		
		setSize( 500, 400 );
		
		JPanel landmarkPointOptionsPanel = new JPanel();
		landmarkPointOptionsPanel.setLayout( new BoxLayout( landmarkPointOptionsPanel, BoxLayout.X_AXIS ));
		
		
		landmarkColorButton = new JButton( new ColorIcon( settings.getSpotColor() ) );
		colorChooser = new JColorChooser();
		
		landmarkSizeSlider = new JSlider();
		landmarkSizeSlider.setValue( (int)settings.getSpotSize() );
		landmarkSizeSlider.setMinimum( 1 );
		landmarkSizeSlider.setMaximum( 20 );
		
		landmarkPointOptionsPanel.add( landmarkColorButton );
		landmarkPointOptionsPanel.add( landmarkSizeSlider );
		landmarkPointOptionsPanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"landmark size & color" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );
		
		// 
		JPanel visTypePanel = new JPanel();
		visTypePanel.setLayout(  new BoxLayout( visTypePanel, BoxLayout.Y_AXIS) );
		visTypePanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"warp display" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );

		
		JPanel typeOptionPanel = new JPanel();
		typeOptionPanel.setLayout(  new BoxLayout( typeOptionPanel, BoxLayout.Y_AXIS) );
		typeOptionPanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"options" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );
		
		// label indicating that there are no options to be had
		noOptionsLabel = new JLabel( "None" );
		
		// buttons choosing if and how the warp should be visualized
		visTypeGroup = new ButtonGroup();
		setWarpVisOffButton = new JRadioButton( "Off" );
		setWarpGridButton = new JRadioButton( "Grid" );
		setWarpMagButton = new JRadioButton( "Magnitude" );
		
		visTypeGroup.add( setWarpVisOffButton );
		visTypeGroup.add( setWarpGridButton );
		visTypeGroup.add( setWarpMagButton );
		
		visTypePanel.add( setWarpVisOffButton );
		visTypePanel.add( setWarpGridButton );
		visTypePanel.add( setWarpMagButton );
		
		
		// buttons for warp magnitude options
		warpMagAffineButton = new JRadioButton( "Affine baseline" );
		warpMagSimilarityButton = new JRadioButton("Similarity baseline");
		warpMagRigidButton = new JRadioButton("Rigid baseline");

		warpMagButtons = new ButtonGroup();
		warpMagButtons.add( warpMagAffineButton );
		warpMagButtons.add( warpMagSimilarityButton );
		warpMagButtons.add( warpMagRigidButton );
		
		// buttons for warp grid options 
		warpGridLineButton = new JRadioButton( "Line grid " );
		warpGridModButton  = new JRadioButton( "Modulo grid" );
		
		warpGridButtons = new ButtonGroup();
		warpGridButtons.add( warpGridLineButton );
		warpGridButtons.add( warpGridModButton );
		
		gridSpacingSlider = new JSlider( JSlider.HORIZONTAL, minGridSpacing, maxGridSpacing, defaultGridSpacing );
		gridWidthSlider = new JSlider( JSlider.HORIZONTAL, minGridWidth, maxGridWidth, defaultGridWidth );
		// label the sliders
		gridSpacingLabel = new JLabel("Grid Spacing", JLabel.CENTER);
		gridWidthLabel = new JLabel("Grid Width", JLabel.CENTER);
		gridSpacingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		gridWidthLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		bigSpace = Box.createVerticalStrut( 20 );
		smallSpace = Box.createVerticalStrut( 10 );
		
		typeOptionPanel.add( warpMagAffineButton );
		typeOptionPanel.add( warpMagSimilarityButton );
		typeOptionPanel.add( warpMagRigidButton );
		
		typeOptionPanel.add( warpGridLineButton );
		typeOptionPanel.add( warpGridModButton );
		typeOptionPanel.add( bigSpace );
		typeOptionPanel.add( gridSpacingLabel );
		typeOptionPanel.add( gridSpacingSlider );
		typeOptionPanel.add( smallSpace );
		typeOptionPanel.add( gridWidthLabel );
		typeOptionPanel.add( gridWidthSlider );
		
		typeOptionPanel.add( noOptionsLabel );
		
		content.add( landmarkPointOptionsPanel, BorderLayout.NORTH );
		content.add( visTypePanel, BorderLayout.WEST );
		content.add( typeOptionPanel, BorderLayout.EAST );
		
		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
		
		addListeners();
		updateOptions();
	}
	
	public void setActions()
	{
		final ActionMap actionMap = bw.getViewerFrameP().getKeybindings().getConcatenatedActionMap();

		landmarkColorButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				colorChooser.setColor( settings.getSpotColor());
				final JDialog d = JColorChooser.createDialog( landmarkColorButton, "Choose a color", true, colorChooser, new ActionListener()
				{
					@Override
					public void actionPerformed( final ActionEvent arg0 )
					{
						final Color c = colorChooser.getColor();
						if (c != null)
						{
							landmarkColorButton.setIcon( new ColorIcon( c ) );
							bw.setSpotColor( c );
						}
					}
				}, null );
				d.setVisible( true );
			}
		} );
		
		landmarkSizeSlider.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				if( e.getSource() != landmarkSizeSlider ) return;
				
				settings.setSpotSize( landmarkSizeSlider.getValue() );
				bw.viewerP.requestRepaint();
				bw.viewerQ.requestRepaint();
			}
		});
		
		setWarpVisOffButton.setAction( 
				actionMap.get( String.format( BigWarpActions.SET_WARPTYPE_VIS, BigWarp.WarpVisType.NONE )));
		setWarpGridButton.setAction( 
				actionMap.get( String.format( BigWarpActions.SET_WARPTYPE_VIS, BigWarp.WarpVisType.GRID )));
		setWarpMagButton.setAction( 
				actionMap.get( String.format( BigWarpActions.SET_WARPTYPE_VIS, BigWarp.WarpVisType.WARPMAG )));
		
		setWarpVisOffButton.setText("Off");
		setWarpGridButton.setText("Grid");
		setWarpMagButton.setText("Magnitude");
		
		warpMagAffineButton.setAction( 
				actionMap.get( String.format( BigWarpActions.WARPMAG_BASE, bw.baseXfmList[ 0 ].getClass().getName() )));
		warpMagSimilarityButton .setAction( 
				actionMap.get( String.format( BigWarpActions.WARPMAG_BASE, bw.baseXfmList[ 1 ].getClass().getName() ) ));
		warpMagRigidButton.setAction( 
				actionMap.get( String.format( BigWarpActions.WARPMAG_BASE, bw.baseXfmList[ 2 ].getClass().getName() ) ));
		
		warpMagAffineButton.setText("Affine");
		warpMagSimilarityButton.setText("Similarity");
		warpMagRigidButton.setText("Rigid");
		
		warpGridLineButton.setAction( 
				actionMap.get( String.format( BigWarpActions.WARPVISGRID, GridSource.GRID_TYPE.LINE )));
		warpGridModButton.setAction( 
				actionMap.get( String.format( BigWarpActions.WARPVISGRID, GridSource.GRID_TYPE.MOD )));
		
		warpGridLineButton.setText( "Line" );
		warpGridModButton.setText( "Modulo" );
		
		// turn on the default values
		setWarpVisOffButton.doClick();
		warpMagAffineButton.doClick();
		warpGridLineButton.doClick();
	}
	
	public void addListeners()
	{
		MyChangeListener mylistener = new MyChangeListener();
		setWarpVisOffButton.addChangeListener( mylistener );
		setWarpGridButton.addChangeListener( mylistener );
		setWarpMagButton.addChangeListener( mylistener );
		
		gridSpacingSlider.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				if( e.getSource() != gridSpacingSlider ) return;
				
				WarpVisFrame.this.bw.setWarpGridSpacing( gridSpacingSlider.getValue() );
			}
		});
		
		gridWidthSlider.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				if( e.getSource() != gridWidthSlider ) return;
				
				WarpVisFrame.this.bw.setWarpGridWidth( gridWidthSlider.getValue() );
			}
		});
	}
	
	public class MyChangeListener implements ChangeListener
	{
		@Override
		public void stateChanged( ChangeEvent e )
		{
			WarpVisFrame.this.updateOptions();
		}
	}
	
	private void setGridOptionsVisibility( boolean isVisible )
	{
		// disable all options
		Enumeration< AbstractButton > elems = warpGridButtons.getElements();
		while( elems.hasMoreElements())
			elems.nextElement().setVisible( isVisible ); 

		gridSpacingSlider.setVisible( isVisible );
		gridWidthSlider.setVisible( isVisible );
		bigSpace.setVisible( isVisible );
		gridSpacingLabel.setVisible( isVisible );
		gridWidthLabel.setVisible( isVisible );
	}
	
	private void setMagOptionsVisibility( boolean isVisible )
	{
		// disable all options
		Enumeration< AbstractButton > elems = warpMagButtons.getElements();
		while( elems.hasMoreElements())
			elems.nextElement().setVisible( isVisible ); 
	}
	
	public synchronized void updateOptions()
	{
		if( setWarpVisOffButton.isSelected() )
		{
			noOptionsLabel.setVisible( true );
			setGridOptionsVisibility( false );
			setMagOptionsVisibility( false );
		}
		else if( setWarpGridButton.isSelected() )
		{
			noOptionsLabel.setVisible( false );
			setGridOptionsVisibility( true );
			setMagOptionsVisibility( false );
		}
		else if( setWarpMagButton.isSelected() )
		{
			noOptionsLabel.setVisible( false );
			setGridOptionsVisibility( false );
			setMagOptionsVisibility( true );
		}
		else
		{
//			System.out.println(" UHOH ");
		}
		pack();
	}
	
	private static class ColorIcon implements Icon
	{
		private final int size = 16;

		private final Color color;

		public ColorIcon( final Color color )
		{
			this.color = color;
		}

		@Override
		public void paintIcon( final Component c, final Graphics g, final int x, final int y )
		{
			final Graphics2D g2d = ( Graphics2D ) g;
			g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
			g2d.setColor( color );
			g2d.fillOval( x, y, size, size );
		}

		@Override
		public int getIconWidth()
		{
			return size;
		}

		@Override
		public int getIconHeight()
		{
			return size;
		}
	}
}
