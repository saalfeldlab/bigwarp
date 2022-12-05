package bdv.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class FieldOfViewPanel extends JPanel
{
	private static final long serialVersionUID = 1719652204751351335L;

	private static final int DEFAULT_OUTER_PAD = 8;
	private static final int DEFAULT_BUTTON_PAD = 3;
	private static final int DEFAULT_MID_PAD = 5;

	private String unit;
	private int ndims;
	private int textFieldWidth;

	private double[] initMin;
	private double[] initSpacing;
	private long[] initPixsize;

	private double[] min;
	private double[] spacing;
	private double[] size;
	private long[] pixSize;

	private ImprovedFormattedTextField[] minFields;
	private ImprovedFormattedTextField[] sizeFields;
	private ImprovedFormattedTextField[] spacingFields;
	private ImprovedFormattedTextField[] pixelFields;

	public FieldOfViewPanel( final int ndims, final String unit, final int textFieldWidth, final double[] initMin, final double[] initSpacing, final long[] initPixsize )
	{
		super();

		// only support 2 or 3 dimensional fields of view
		// defending for ndims > 3 but not ndims < 2
		this.ndims = ndims > 3 ? 3 : ndims;
		this.unit = unit;
		this.textFieldWidth = textFieldWidth;

		this.initMin = initMin;
		this.initSpacing = initSpacing;
		this.initPixsize = initPixsize;

		this.min = new double[ ndims ];
		System.arraycopy( initMin, 0, min, 0, ndims );

		this.spacing = new double[ ndims ];
		System.arraycopy( initSpacing, 0, spacing, 0, ndims );

		this.pixSize = new long[ ndims ];
		System.arraycopy( initPixsize, 0, pixSize, 0, ndims );

		this.size = new double[ ndims ];
		for ( int i = 0; i < ndims; i++ )
			size[ i ] = spacing[ i ] * pixSize[ i ];

		create();
	}

	public double[] getMin()
	{
		return min;
	}

	public double[] getSpacing()
	{
		return spacing;
	}

	public double[] getPhysicalSize()
	{
		return size;
	}

	public long[] getPixelSize()
	{
		return pixSize;
	}

	public void create()
	{
		setLayout(new GridBagLayout());	

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets( DEFAULT_OUTER_PAD, DEFAULT_OUTER_PAD, DEFAULT_MID_PAD, DEFAULT_BUTTON_PAD );

		JLabel minLabel = new JLabel( "min" );
		JLabel sizeLabel = new JLabel( String.format( "size (%s)", unit ) );
		JLabel spacingLabel = new JLabel( "spacing" );
		JLabel pixelLabel = new JLabel( "size (pixels)" );

		gbc.gridx = 1;
		add( minLabel, gbc );
		gbc.gridx = 2;
		add( sizeLabel, gbc );
		gbc.gridx = 3;
		add( spacingLabel, gbc );
		gbc.gridx = 4;
		add( pixelLabel, gbc );

		final JLabel yLabel = new JLabel("y");

		gbc.gridx = 0;
		gbc.gridy = 1;

		final JLabel xLabel = new JLabel("x");
		add( xLabel, gbc );

		gbc.gridy = 2;
		add( yLabel, gbc );
		
		if( ndims >= 3 )
		{
			gbc.gridy = 3;
			JLabel zLabel = new JLabel("z");
			add( zLabel, gbc );
		}

		minFields = new ImprovedFormattedTextField[ ndims ];
		sizeFields = new ImprovedFormattedTextField[ ndims ];
		spacingFields = new ImprovedFormattedTextField[ ndims ];
		pixelFields = new ImprovedFormattedTextField[ ndims ];
		gbc.fill = GridBagConstraints.HORIZONTAL;

		final Dimension textFieldSize = new Dimension( textFieldWidth, 20 );
		final DecimalFormat decimalFormat = new DecimalFormat(); 
		decimalFormat.setMaximumFractionDigits( 8 );

		// add fields
		for( int i = 0; i < ndims; i++ )
		{
			final int idx = i;
			gbc.gridy = i + 1;
			
			gbc.gridx = 1;
			minFields[ i ] = new ImprovedFormattedTextField( decimalFormat );
			minFields[ i ].setPreferredSize( textFieldSize );
			minFields[ i ].setHorizontalAlignment( JTextField.RIGHT );
			minFields[ i ].setValue( new Double( initMin[i]) );
			add( minFields[ i ], gbc );

			gbc.gridx = 2;
			sizeFields[ i ] = new ImprovedFormattedTextField( decimalFormat );
			sizeFields[ i ].setPreferredSize( textFieldSize );
			sizeFields[ i ].setHorizontalAlignment( JTextField.RIGHT );
			sizeFields[ i ].setValue( new Double( initSpacing[ i ] * initPixsize[ i ] ) );
			sizeFields[ i ].addActionListener( a -> 
			{
				size[ idx ] = Double.parseDouble( sizeFields[ idx ].getText() );
				updatePixelsFromSize( idx );
			});
			add( sizeFields[ i ], gbc );

			gbc.gridx = 3;
			spacingFields[ i ] = new ImprovedFormattedTextField( decimalFormat );
			spacingFields[ i ].setPreferredSize( textFieldSize );
			spacingFields[ i ].setHorizontalAlignment( JTextField.RIGHT );
			spacingFields[ i ].setValue( new Double( initSpacing[ i ] ) );
			spacingFields[ i ].addActionListener( a -> 
			{
				spacing[ idx ] = Double.parseDouble( spacingFields[ idx ].getText() );
				updatePixelsFromSpacing( idx );
			});

			add( spacingFields[ i ], gbc );

			gbc.gridx = 4;
			pixelFields[ i ] = new ImprovedFormattedTextField( NumberFormat.getIntegerInstance() );
			pixelFields[ i ].setPreferredSize( textFieldSize );
			pixelFields[ i ].setHorizontalAlignment( JTextField.RIGHT );
			pixelFields[ i ].setValue( new Long( initPixsize[ i ] ) );
			pixelFields[ i ].addActionListener( a -> 
			{
				pixSize[ idx ] = Long.parseLong( pixelFields[ idx ].getText() );
				updateSpacingFromPixels( idx );
			});
			add( pixelFields[ i ], gbc );
		}
	}

	protected void updatePixelsFromSize( int i )
	{
		pixSize[ i ] = (long)Math.ceil( size[ i ] / spacing[ i ] );
		pixelFields[ i ].setValue( new Double( pixSize[ i ] ));
	}

	protected void updatePixelsFromSpacing( int i )
	{
		pixSize[ i ] = (long)Math.floor( size[ i ] / spacing[ i ] );
		pixelFields[ i ].setValue( new Long( pixSize[ i ] ));
	}

	protected void updateSpacingFromPixels( int i )
	{
		spacing[ i ] = size[ i ] / pixSize[ i ];
		spacingFields[ i ].setValue( new Double( spacing[ i ] ));
	}

	public static void main( String[] args )
	{
		final JFrame frame = new JFrame( "fov" );
		final FieldOfViewPanel panel = new FieldOfViewPanel( 3, "mm", 150, new double[] { 0, 0, 0 }, new double[] { 1, 1, 1 }, new long[] { 300, 200, 100 } );

		frame.add( panel );
		frame.pack();
		frame.setVisible( true );
	}

}
