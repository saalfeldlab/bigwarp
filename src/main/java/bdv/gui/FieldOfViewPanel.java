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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.formdev.flatlaf.util.UIScale;

import bdv.ij.ApplyBigwarpPlugin;
import bigwarp.BigWarpData;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
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
	private JTextField unitField;
	private JLabel minLabel;
	private JLabel sizeLabel;
	private JLabel spacingLabel;

	/**
	 * When true, the panel additionally exposes the landmark point filter and the
	 * bounding box estimation options, and offers the {@code MOVING} and
	 * {@code UNION_TARGET_MOVING} reference options.
	 */
	private final boolean showAdvanced;

	private JLabel pointFilterLabel;
	private JTextField pointFilterField;
	private JComboBox<String> bboxMethodComboBox;
	private JSpinner bboxSamplesSpinner;

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
		this( data, ltm, bwTransform, unit, textFieldWidth, initMin, initSpacing, initPixsize, false );
	}

	public FieldOfViewPanel( final BigWarpData<?> data, final LandmarkTableModel ltm, final BigWarpTransform bwTransform, final String unit, final int textFieldWidth,
			final double[] initMin, final double[] initSpacing, final long[] initPixsize, final boolean showAdvanced )
	{
		super();

		this.showAdvanced = showAdvanced;
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

	/**
	 * Sets the {@link BigWarpData} whose sources the reference options resolve against.
	 * <p>
	 * Callers that change the data after construction should follow this with
	 * {@link #updateFieldsFromReference()}.
	 *
	 * @param data the bigwarp data
	 */
	public void setData( final BigWarpData<?> data )
	{
		this.data = data;
	}

	/**
	 * Sets the landmarks used to resolve the landmark-based reference options.
	 *
	 * @param ltm the landmark table model
	 */
	public void setLandmarks( final LandmarkTableModel ltm )
	{
		this.ltm = ltm;
	}

	/**
	 * Sets the transformation used to estimate the warped moving field of view.
	 *
	 * @param bwTransform the transformation
	 */
	public void setTransform( final BigWarpTransform bwTransform )
	{
		this.bwTransform = bwTransform;
	}

	/**
	 * @return the regular expression filtering landmark points by name, or the empty
	 *         string when this panel was built without the advanced options
	 */
	public String getPointFilter()
	{
		return pointFilterField == null ? "" : pointFilterField.getText();
	}

	/**
	 * @return the selected reference option, or {@code SPECIFIED} when there is no
	 *         reference dropdown
	 */
	public String getReference()
	{
		if ( referenceComboBox == null )
			return ApplyBigwarpPlugin.SPECIFIED;

		final String selected = ( String ) referenceComboBox.getSelectedItem();
		return selected == null ? ApplyBigwarpPlugin.SPECIFIED : selected;
	}

	/**
	 * @return the name of the selected bounding box estimation method
	 */
	public String getBoundingBoxMethod()
	{
		if ( bboxMethodComboBox == null )
			return BoundingBoxEstimation.Method.FACES.toString();

		return ( String ) bboxMethodComboBox.getSelectedItem();
	}

	/**
	 * @return the number of samples per dimension used by the bounding box estimator
	 */
	public int getBoundingBoxSamples()
	{
		return bboxSamplesSpinner == null ? 5 : ( Integer ) bboxSamplesSpinner.getValue();
	}

	/**
	 * @return the bounding box estimator described by the current options
	 */
	public BoundingBoxEstimation getBoundingBoxEstimation()
	{
		if ( bboxMethodComboBox == null )
			return new BoundingBoxEstimation();

		return new BoundingBoxEstimation(
				BoundingBoxEstimation.Method.valueOf( ( String ) bboxMethodComboBox.getSelectedItem() ),
				( Integer ) bboxSamplesSpinner.getValue() );
	}

	public double[] getMin()
	{
		return min;
	}

	/**
	 * Sets the minimum value, does not trigger callbacks.
	 *
	 * @param newMin the new min val
	 */
	public void setMin( final double[] newMin )
	{
		final int N = newMin.length > min.length ? min.length : newMin.length;
		for ( int i = 0; i < N; i++ )
		{
			minFields[ i ].setValue( new Double( newMin[ i ] ), false );
			min[ i ] = newMin[ i ];
		}
	}

	public double[] getSpacing()
	{
		return spacing;
	}

	/**
	 * Sets the spacing, does not trigger callbacks
	 *
	 * @param newSpacing the new spacing
	 */
	public void setSpacing( final double[] newSpacing )
	{
		final int N = newSpacing.length > spacing.length ? spacing.length : newSpacing.length;
		for ( int i = 0; i < N; i++ )
		{
			spacingFields[ i ].setValue( new Double( newSpacing[ i ] ), false );
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

	/**
	 * Sets the pixel size, does not trigger callbacks.
	 *
	 * @param newSize the new discrete image size (in pixels)
	 */
	public void setPixelSize( final long[] newSize )
	{
		final int N = newSize.length > pixSize.length ? pixSize.length : newSize.length;
		for ( int i = 0; i < N; i++ )
		{
			pixelFields[i].setValue( new Long(newSize[i]), false );
			pixSize[ i ] = newSize[ i ];
		}
	}

	public String getUnit()
	{
		return unitField.getText();
	}

	public void setUnit( String unit )
	{
		unitField.setText( unit );
		updateUnitLabels();
	}

	/**
	 * Refreshes every column header that mentions the unit.
	 * <p>
	 * The labels are created after the listener that calls this, so tolerate nulls.
	 */
	private void updateUnitLabels()
	{
		final String u = unitField.getText();
		if ( minLabel != null )
			minLabel.setText( String.format( "min (%s)", u ) );

		if ( sizeLabel != null )
			sizeLabel.setText( String.format( "size (%s)", u ) );

		if ( spacingLabel != null )
			spacingLabel.setText( String.format( "spacing (%s/px)", u ) );
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

		/*
		 * Row 0 always holds the units field, row 1 the column headers, so the value
		 * fields always start at row 2 - whether or not a reference dropdown exists.
		 */
		final int j = 2;
		if( data != null )
		{
			add( new JLabel( "reference:" ), gbc );

			/*
			 * Deliberately not offering MOVING or UNION_TARGET_MOVING:
			 * ApplyBigwarpPlugin.getPixelInterval has no MOVING branch at all and returns
			 * null for it, and its UNION_TARGET_MOVING branch computes both halves of the
			 * union from the moving source (see ApplyBigwarpPlugin:487), so it ignores the
			 * target. Both would look like working options while doing nothing useful.
			 */
			final String[] fovOpts = new String[]{
					ApplyBigwarpPlugin.SPECIFIED,
					ApplyBigwarpPlugin.TARGET,
					ApplyBigwarpPlugin.MOVING_WARPED,
					ApplyBigwarpPlugin.LANDMARK_POINTS };
			referenceComboBox = new JComboBox<>( fovOpts );
			referenceComboBox.setSelectedItem( ApplyBigwarpPlugin.MOVING_WARPED );
			referenceComboBox.addActionListener( e -> {
				updatePointFilterEnabled();
				updateFieldsFromReference();
			});

			gbc.gridx = 1;
			add( referenceComboBox, gbc );
		}
		else if( IJ.getInstance() != null )
		{
			add( new JLabel( "reference:" ), gbc );

			final String[] impTitles = getImagePlusTitles();
			int numImp = 0;
			if( impTitles != null )
				numImp = impTitles.length;

			final String[] fovOpts = new String[ numImp + 1 ];
			fovOpts[ 0 ] = ApplyBigwarpPlugin.SPECIFIED;
			for( int i = 0; i < numImp; i++ )
				fovOpts[ i + 1 ] = impTitles[ i ];

			referenceComboBox = new JComboBox<>( fovOpts );
			if( impTitles.length > 0 )
				referenceComboBox.setSelectedIndex( 1 );

			referenceComboBox.addActionListener( e -> {
				updateFieldsFromImageJReference();
			});

			gbc.gridx = 1;
			add( referenceComboBox, gbc );
		}

		final JLabel unitLabel = new JLabel( "units:" );
		gbc.gridx = 2;
		gbc.anchor = GridBagConstraints.LINE_END;
		add( unitLabel, gbc );

		gbc.gridx = 3;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;
		unitField = new JTextField("pixel");
		unitField.getDocument().addDocumentListener( new DocumentListener() {

			@Override
			public void changedUpdate( DocumentEvent e ) { }

			@Override
			public void insertUpdate( DocumentEvent e )
			{
				updateUnitLabels();
			}

			@Override
			public void removeUpdate( DocumentEvent e )
			{
				updateUnitLabels();
			}
		});
		add( unitField, gbc );

		minLabel = new JLabel( String.format("min (%s)", unit ));
		sizeLabel = new JLabel( String.format( "size (%s)", unit ) );
		spacingLabel = new JLabel( String.format( "spacing (%s/px)", unit ));
		final JLabel pixelLabel = new JLabel( "size (px)" );

		gbc.gridy = 1;
		gbc.gridx = 1;
		add( minLabel, gbc );
		gbc.gridx = 2;
		add( sizeLabel, gbc );
		gbc.gridx = 3;
		add( spacingLabel, gbc );
		gbc.gridx = 4;
		add( pixelLabel, gbc );

		// axis labels, on the same rows as the value fields below
		final String[] axisNames = new String[] { "x", "y", "z" };
		gbc.gridx = 0;
		gbc.anchor = GridBagConstraints.LINE_END;
		for( int i = 0; i < ndims; i++ )
		{
			gbc.gridy = i + j;
			add( new JLabel( i < axisNames.length ? axisNames[ i ] : "d" + i ), gbc );
		}

		/*
		 * TODO investigate focus traversal, see:
		 * https://docs.oracle.com/javase/tutorial/uiswing/misc/focus.html#customFocusTraversal
		 */

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
			gbc.gridy = i + j;

			gbc.gridx = 1;
			minFields[ i ] = new ImprovedFormattedTextField( decimalFormat );
			minFields[ i ].setPreferredSize( textFieldSize );
			minFields[ i ].setHorizontalAlignment( JTextField.RIGHT );
			minFields[ i ].setValueNoCallback( new Double( initMin[i]) );
			add( minFields[ i ], gbc );

			gbc.gridx = 2;
			sizeFields[ i ] = new ImprovedFormattedTextField( decimalFormat );
			sizeFields[ i ].setPreferredSize( textFieldSize );
			sizeFields[ i ].setHorizontalAlignment( JTextField.RIGHT );
			sizeFields[ i ].setValueNoCallback( new Double( initSpacing[ i ] * initPixsize[ i ] ) );
			add( sizeFields[ i ], gbc );

			gbc.gridx = 3;
			spacingFields[ i ] = new ImprovedFormattedTextField( decimalFormat );
			spacingFields[ i ].setPreferredSize( textFieldSize );
			spacingFields[ i ].setHorizontalAlignment( JTextField.RIGHT );
			spacingFields[ i ].setValueNoCallback( new Double( initSpacing[ i ] ) );

			add( spacingFields[ i ], gbc );

			gbc.gridx = 4;
			pixelFields[ i ] = new ImprovedFormattedTextField( NumberFormat.getIntegerInstance());
			pixelFields[ i ].setPreferredSize( textFieldSize );
			pixelFields[ i ].setHorizontalAlignment( JTextField.RIGHT );
			pixelFields[ i ].setValueNoCallback( new Long( initPixsize[ i ] ) );
			add( pixelFields[ i ], gbc );

			// set callbacks for fields
			// what fields update others when modified
			final int idx = i;
			minFields[ i ].setCallback( () -> {
				try {
					min[ idx ] = Double.parseDouble( minFields[ idx ].getText() );
				} catch (final NumberFormatException e) {}
			});

			sizeFields[ i ].setCallback( () -> {
				try {
					size[ idx ] = Double.parseDouble( sizeFields[ idx ].getText() );
					updatePixelsFromSize( idx );
				} catch (final NumberFormatException e) {}
			});

			spacingFields[ i ].setCallback( () -> {
				try {
					spacing[ idx ] = Double.parseDouble( spacingFields[ idx ].getText() );
					updatePixelsFromSpacing( idx );
//					updateSize( idx );
				} catch (final NumberFormatException e) {}
			});

			pixelFields[ i ].setCallback( () -> {
//				System.out.println("pix sz callback");
				try {
					pixSize[ idx ] = Long.parseLong( pixelFields[ idx ].getText() );
					updateSize( idx );
				} catch (final NumberFormatException e) {}
//				updateSpacingFromPixels( idx ); // an alternative update
			});
		}

		if( showAdvanced )
			addAdvancedOptions( gbc, j + ndims );
	}

	/**
	 * Adds the landmark point filter and bounding box estimation controls, starting at
	 * the given row.
	 *
	 * @param gbc the constraints, mutated
	 * @param row the first free row below the value fields
	 */
	private void addAdvancedOptions( final GridBagConstraints gbc, final int row )
	{
		gbc.gridy = row;
		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.LINE_END;
		pointFilterLabel = new JLabel( "point filter:" );
		pointFilterLabel.setToolTipText( POINT_FILTER_HELP_TEXT );
		add( pointFilterLabel, gbc );

		gbc.gridx = 1;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		pointFilterField = new JTextField();
		pointFilterField.setToolTipText( POINT_FILTER_HELP_TEXT );
		add( pointFilterField, gbc );

		gbc.gridwidth = 1;
		gbc.gridx = 3;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.LINE_END;
		final JLabel bboxLabel = new JLabel( "bounding box:" );
		bboxLabel.setToolTipText( BBOX_HELP_TEXT );
		add( bboxLabel, gbc );

		gbc.gridx = 4;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		bboxMethodComboBox = new JComboBox<>( new String[] {
				BoundingBoxEstimation.Method.CORNERS.toString(),
				BoundingBoxEstimation.Method.FACES.toString(),
				BoundingBoxEstimation.Method.VOLUME.toString() } );
		bboxMethodComboBox.setSelectedItem( BoundingBoxEstimation.Method.FACES.toString() );
		bboxMethodComboBox.setToolTipText( BBOX_HELP_TEXT );
		bboxMethodComboBox.addActionListener( e -> updateFieldsFromReference() );
		add( bboxMethodComboBox, gbc );

		gbc.gridy = row + 1;
		gbc.gridx = 3;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.LINE_END;
		final JLabel samplesLabel = new JLabel( "samples per dim:" );
		samplesLabel.setToolTipText( BBOX_SAMPLES_HELP_TEXT );
		add( samplesLabel, gbc );

		gbc.gridx = 4;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		bboxSamplesSpinner = new JSpinner( new SpinnerNumberModel( 5, 1, 9999, 1 ) );
		bboxSamplesSpinner.setToolTipText( BBOX_SAMPLES_HELP_TEXT );
		bboxSamplesSpinner.addChangeListener( e -> updateFieldsFromReference() );
		add( bboxSamplesSpinner, gbc );

		// the filter only means anything for the landmark-based reference
		pointFilterField.getDocument().addDocumentListener( new DocumentListener() {

			@Override
			public void changedUpdate( DocumentEvent e ) { }

			@Override
			public void insertUpdate( DocumentEvent e )
			{
				updateFieldsFromReference();
			}

			@Override
			public void removeUpdate( DocumentEvent e )
			{
				updateFieldsFromReference();
			}
		} );

		updatePointFilterEnabled();
	}

	/**
	 * The point filter only affects the landmark-based reference options, so gray it out
	 * otherwise rather than letting it look load-bearing.
	 */
	private void updatePointFilterEnabled()
	{
		if ( pointFilterField == null || referenceComboBox == null )
			return;

		final Object selected = referenceComboBox.getSelectedItem();
		final boolean enabled = ApplyBigwarpPlugin.LANDMARK_POINTS.equals( selected );
		pointFilterField.setEnabled( enabled );
		pointFilterLabel.setEnabled( enabled );
	}

	protected void updatePixelsFromSize( int i )
	{
		pixSize[ i ] = ( long ) Math.ceil( size[ i ] / spacing[ i ] );
		SwingUtilities.invokeLater( () -> { pixelFields[ i ].setValueNoCallback( Long.valueOf( pixSize[ i ] ) ); });
	}

	protected void updatePixelsFromSpacing( int i )
	{
		pixSize[ i ] = ( long ) Math.floor( size[ i ] / spacing[ i ] );
		SwingUtilities.invokeLater( () -> { pixelFields[ i ].setValueNoCallback( new Long( pixSize[ i ] ) ); });
	}

	protected void updateSpacingFromPixels( int i )
	{
		spacing[ i ] = size[ i ] / pixSize[ i ];
		SwingUtilities.invokeLater( () -> { spacingFields[ i ].setValueNoCallback( new Double( spacing[ i ] ) ); });
	}

	protected void updateSize( int i )
	{
		size[ i ] = spacing[ i ] * pixSize[ i ];
		SwingUtilities.invokeLater( () -> { sizeFields[ i ].setValueNoCallback( new Double( size[ i ] ) ); });
	}

	public void updateFieldsFromReference()
	{
		if ( data == null || bwTransform == null || referenceComboBox == null )
			return;

		final String referenceOption = ( String ) referenceComboBox.getSelectedItem();
		if ( referenceOption.equals( ApplyBigwarpPlugin.SPECIFIED ) )
			return;

		final double[] res = resolutionForReference( referenceOption );
		if ( res != null )
			setSpacing( res );

		final String filter = getPointFilter();
		final BoundingBoxEstimation bboxEst = getBoundingBoxEstimation();

		final List< Interval > itvl = ApplyBigwarpPlugin.getPixelInterval( data, ltm, bwTransform.getTransformation( false ),
				referenceOption, filter, bboxEst, null, null, getSpacing() );

		// nothing usable to show, e.g. no landmark matched the filter
		if ( itvl == null || itvl.isEmpty() )
			return;

		final double[] offset = ApplyBigwarpPlugin.getPhysicalOffset(
				referenceOption,
				null,
				ltm,
				bwTransform.getTransformation( true ),
				filter,
				bboxEst,
				getSpacing(),
				data.getMovingSource(0).getSpimSource(),
				data.numTargetSources() > 0 ? data.getTargetSource(0).getSpimSource() : null);

		setPixelSize( itvl.get( 0 ).dimensionsAsLongArray() );
		setMin( offset );
		setUnit( ApplyBigwarpPlugin.getUnit( data, referenceOption ));

		for ( int i = 0; i < size.length; i++ )
			updateSize( i );
	}

	/**
	 * Resolves the reference option to an output resolution.
	 * <p>
	 * {@link ApplyBigwarpPlugin#getResolution(BigWarpData, String, double[])} only knows
	 * {@code TARGET}, {@code MOVING} and {@code SPECIFIED}, and returns null otherwise.
	 * The warped moving image has no resolution of its own, so fall back to the moving
	 * image's rather than silently leaving a stale spacing in the fields.
	 * {@code LANDMARK_POINTS} genuinely has none, so there keep whatever the user typed.
	 *
	 * @param referenceOption the reference option
	 * @return the resolution, or null to leave the current spacing alone
	 */
	private double[] resolutionForReference( final String referenceOption )
	{
		if ( referenceOption.equals( ApplyBigwarpPlugin.MOVING_WARPED ) )
			return ApplyBigwarpPlugin.getResolution( data, ApplyBigwarpPlugin.MOVING, null );

		return ApplyBigwarpPlugin.getResolution( data, referenceOption, null );
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

	public void updateFieldsFromImageJReference()
	{
		if( referenceComboBox == null )
			return;

		final String referenceOption = (String)referenceComboBox.getSelectedItem();
		if( referenceOption == null || referenceOption.equals( ApplyBigwarpPlugin.SPECIFIED ))
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

			setUnit( refImp.getCalibration().getUnit() );

			// the setters above deliberately skip callbacks, so bring the physical size
			// column in line with the new spacing and pixel counts
			for ( int i = 0; i < size.length; i++ )
				updateSize( i );
		}
	}

	private static final String POINT_FILTER_HELP_TEXT = "Regular expression matching the names of the landmark points "
			+ "used to estimate the field of view. Only applies to the \"" + ApplyBigwarpPlugin.LANDMARK_POINTS + "\" reference.";

	private static final String BBOX_HELP_TEXT = "How the bounding box of the warped image is estimated. \"Corners\" is "
			+ "fastest but can be too small for strongly nonlinear transformations; \"Volume\" is most conservative.";

	private static final String BBOX_SAMPLES_HELP_TEXT = "Number of samples per dimension used by the \"Faces\" and "
			+ "\"Volume\" bounding box estimators.";

//	public static void main( String[] args ) throws IOException, URISyntaxException, SpimDataException
//	{
//		final JFrame frame = new JFrame( "fov" );
//		BigWarpData data = new BigWarpData();
//
//		int id = 0;
//		BigWarpInit.add( data, BigWarpInit.createSources( data, "/home/john/tmp/mri-stack_mm.tif", id++, true ) );
//		BigWarpInit.add( data, BigWarpInit.createSources( data, "/home/john/tmp/mri-stack_mm.tif", id++, false ) );
//
////		LandmarkTableModel ltm = LandmarkTableModel.loadFromCsv( new File("/home/john/tmp/mri-stack-landmarks.csv"), false );
//		LandmarkTableModel ltm = LandmarkTableModel.loadFromCsv( new File("/home/john/tmp/mri-stack-mm-landmarks.csv"), false );
//		BigWarpTransform bwTransform = new BigWarpTransform( ltm, BigWarpTransform.TPS );
//
//		final FieldOfViewPanel panel = new FieldOfViewPanel( data, ltm, bwTransform, "mm", 150,
//				new double[] { 0, 0, 0 }, new double[] { 1, 1, 1 }, new long[] { 300, 200, 100 } );
//
//		frame.add( panel );
//		frame.pack();
//		frame.setVisible( true );
//	}

}
