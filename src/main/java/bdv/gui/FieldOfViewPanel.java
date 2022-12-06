package bdv.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.formdev.flatlaf.util.UIScale;

import bdv.ij.ApplyBigwarpPlugin;
import bigwarp.BigWarpData;
import bigwarp.BigWarpInit;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.Interval;
import net.imglib2.realtransform.BoundingBoxEstimation;

public class FieldOfViewPanel extends JPanel
{
	private static final long serialVersionUID = 1719652204751351335L;

	private static final int DEFAULT_OUTER_PAD = 8;
	private static final int DEFAULT_BUTTON_PAD = 3;
	private static final int DEFAULT_MID_PAD = 5;

	private BigWarpData<?> data;
	private LandmarkTableModel ltm;
	private BigWarpTransform bwTransform;

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

	private JComboBox<String> referenceComboBox;

	private ImprovedFormattedTextField[] minFields;
	private ImprovedFormattedTextField[] sizeFields;
	private ImprovedFormattedTextField[] spacingFields;
	private ImprovedFormattedTextField[] pixelFields;

	public FieldOfViewPanel( final LandmarkTableModel ltm, final BigWarpTransform bwTransform, final String unit, final int textFieldWidth,
			final double[] initMin, final double[] initSpacing, final long[] initPixsize )
	{
		this( null, ltm, bwTransform, unit, textFieldWidth, initMin, initSpacing, initPixsize );
	}

	public FieldOfViewPanel( final BigWarpData<?> data, final LandmarkTableModel ltm, final BigWarpTransform bwTransform, final String unit, final int textFieldWidth,
			final double[] initMin, final double[] initSpacing, final long[] initPixsize )
	{
		super();

		this.data = data;
		this.ltm = ltm;
		this.bwTransform = bwTransform;

		this.ndims = ltm != null ? ltm.getNumdims() : 3;
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

	public void setMin( final double[] newMin )
	{
		final int N = newMin.length > min.length ? min.length : newMin.length;
		for ( int i = 0; i < N; i++ )
		{
			minFields[ i ].setValue( new Double( newMin[ i ] ) );
			min[ i ] = newMin[ i ];
		}
	}

	public double[] getSpacing()
	{
		return spacing;
	}

	public void setSpacing( final double[] newSpacing )
	{
		final int N = newSpacing.length > spacing.length ? spacing.length : newSpacing.length;
		for ( int i = 0; i < N; i++ )
		{
			spacingFields[ i ].setValue( new Double( newSpacing[ i ] ) );
			spacing[ i ] = newSpacing[ i ];
		}
	}

	public double[] getPhysicalSize()
	{
		return size;
	}

	public long[] getPixelSize()
	{
		return pixSize;
	}

	public void setPixelSize( final long[] newSize )
	{
		final int N = newSize.length > pixSize.length ? pixSize.length : newSize.length;
		for ( int i = 0; i < N; i++ )
		{
			pixelFields[ i ].setValue( new Long( newSize[ i ]));
			pixSize[ i ] = newSize[ i ];
		}
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

		int j = 1;
		if( data != null )
		{
			add( new JLabel( "reference:" ), gbc );

			final String[] fovOpts = new String[]{
					ApplyBigwarpPlugin.SPECIFIED,
					ApplyBigwarpPlugin.TARGET,
					ApplyBigwarpPlugin.MOVING_WARPED,
					ApplyBigwarpPlugin.LANDMARK_POINTS };
			referenceComboBox = new JComboBox<>( fovOpts );
			referenceComboBox.addActionListener( e -> {
				updateFieldsFromReference();
			});

			gbc.gridx = 1;
			add( referenceComboBox, gbc );

			j = 2;
			gbc.gridy = 1;
		}
		else if( IJ.getInstance() != null )
		{
			add( new JLabel( "reference:" ), gbc );

			String[] impTitles = getImagePlusTitles();
			int numImp = 0;
			if( impTitles != null )
				numImp = impTitles.length;

			final String[] fovOpts = new String[ numImp + 1 ];
			fovOpts[ 0 ] = ApplyBigwarpPlugin.SPECIFIED;
			for( int i = 0; i < numImp; i++ )
				fovOpts[ i + 1 ] = impTitles[ i ];

			referenceComboBox = new JComboBox<>( fovOpts );
			referenceComboBox.addActionListener( e -> {
				updateFieldsFromImageJReference();
			});

			gbc.gridx = 1;
			add( referenceComboBox, gbc );

			j = 2;
			gbc.gridy = 1;
		}

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
		gbc.gridy++;
		gbc.anchor = GridBagConstraints.LINE_END;

		final JLabel xLabel = new JLabel("x");
		add( xLabel, gbc );

		gbc.gridy++;
		add( yLabel, gbc );
		
		if( ndims >= 3 )
		{
			gbc.gridy++;
			JLabel zLabel = new JLabel("z");
			add( zLabel, gbc );
		}

		minFields = new ImprovedFormattedTextField[ ndims ];
		sizeFields = new ImprovedFormattedTextField[ ndims ];
		spacingFields = new ImprovedFormattedTextField[ ndims ];
		pixelFields = new ImprovedFormattedTextField[ ndims ];
		gbc.fill = GridBagConstraints.HORIZONTAL;

		final int textWidthScaled = UIScale.scale( textFieldWidth );
		final int textHeight = UIScale.scale( 20 );
		final Dimension textFieldSize = new Dimension( textWidthScaled, textHeight );
		final DecimalFormat decimalFormat = new DecimalFormat(); 
		decimalFormat.setMaximumFractionDigits( 8 );

		// add fields
		for( int i = 0; i < ndims; i++ )
		{
			final int idx = i;
			gbc.gridy = i + j;

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
		pixSize[ i ] = ( long ) Math.ceil( size[ i ] / spacing[ i ] );
		pixelFields[ i ].setValue( new Double( pixSize[ i ] ) );
	}

	protected void updatePixelsFromSpacing( int i )
	{
		pixSize[ i ] = ( long ) Math.floor( size[ i ] / spacing[ i ] );
		pixelFields[ i ].setValue( new Long( pixSize[ i ] ) );
	}

	protected void updateSpacingFromPixels( int i )
	{
		spacing[ i ] = size[ i ] / pixSize[ i ];
		spacingFields[ i ].setValue( new Double( spacing[ i ] ) );
	}

	protected void updateSize( int i )
	{
		size[ i ] = spacing[ i ] * pixSize[ i ];
		sizeFields[ i ].setValue( new Double( size[ i ] ) );
	}

	protected void updateFieldsFromReference()
	{
		if ( data == null || bwTransform == null )
			return;

		final String referenceOption = ( String ) referenceComboBox.getSelectedItem();
		if ( referenceOption.equals( ApplyBigwarpPlugin.SPECIFIED ) )
			return;

		final double[] res = ApplyBigwarpPlugin.getResolution( data, referenceOption, null );
		if ( res != null )
			setSpacing( res );

		final List< Interval > itvl = ApplyBigwarpPlugin.getPixelInterval( data, ltm, bwTransform.getTransformation( false ),
				referenceOption, "", new BoundingBoxEstimation(), null, null, getSpacing() );

		final double[] offset = ApplyBigwarpPlugin.getPixelOffset( referenceOption, null, res, itvl.get( 0 ) );

		setPixelSize( itvl.get( 0 ).dimensionsAsLongArray() );
		setMin( offset );

		for ( int i = 0; i < size.length; i++ )
			updateSize( i );
	}

	protected String[] getImagePlusTitles()
	{
		if( IJ.getInstance() != null )
		{
			return WindowManager.getImageTitles();
		}
		else
			return null;
	}

	protected void updateFieldsFromImageJReference()
	{
		final String referenceOption = (String)referenceComboBox.getSelectedItem();
		if( referenceOption.equals( ApplyBigwarpPlugin.SPECIFIED ))
			return;

		if( IJ.getInstance() != null )
		{
			final ImagePlus refImp = WindowManager.getImage( (String)referenceComboBox.getSelectedItem() );
			setSpacing( new double[] { 
					refImp.getCalibration().pixelWidth,
					refImp.getCalibration().pixelHeight,
					refImp.getCalibration().pixelDepth,
			});

			setMin( new double[] { 
					refImp.getCalibration().xOrigin,
					refImp.getCalibration().yOrigin,
					refImp.getCalibration().zOrigin,
			});

			setPixelSize( new long[] { 
					refImp.getWidth(),
					refImp.getHeight(),
					refImp.getNSlices()
			});
		}
	}

	public static void main( String[] args ) throws IOException, URISyntaxException, SpimDataException
	{
		final JFrame frame = new JFrame( "fov" );
		BigWarpData data = new BigWarpData();

		int id = 0;
		BigWarpInit.add( data, BigWarpInit.createSources( data, "/home/john/tmp/mri-stack_mm.tif", id++, true ) );
		BigWarpInit.add( data, BigWarpInit.createSources( data, "/home/john/tmp/mri-stack_mm.tif", id++, false ) );

//		LandmarkTableModel ltm = LandmarkTableModel.loadFromCsv( new File("/home/john/tmp/mri-stack-landmarks.csv"), false );
		LandmarkTableModel ltm = LandmarkTableModel.loadFromCsv( new File("/home/john/tmp/mri-stack-mm-landmarks.csv"), false );
		BigWarpTransform bwTransform = new BigWarpTransform( ltm, BigWarpTransform.TPS );

		final FieldOfViewPanel panel = new FieldOfViewPanel( data, ltm, bwTransform, "mm", 150,
				new double[] { 0, 0, 0 }, new double[] { 1, 1, 1 }, new long[] { 300, 200, 100 } );

		frame.add( panel );
		frame.pack();
		frame.setVisible( true );
	}

}
