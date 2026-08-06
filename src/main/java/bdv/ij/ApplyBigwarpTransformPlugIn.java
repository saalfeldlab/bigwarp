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
package bdv.ij;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import org.janelia.saalfeldlab.n5.Compression;

import bdv.export.ProgressWriter;
import bdv.gui.ApplyTransformFrame;
import bdv.ij.ApplyBigwarpPlugin.WriteDestinationOptions;
import bdv.ij.util.ProgressWriterIJ;
import bdv.img.WarpedSource;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import bigwarp.BigWarpData;
import bigwarp.BigWarpInit;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.plugin.PlugIn;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.realtransform.InvertibleRealTransform;

/**
 * ImageJ plugin that applies a BigWarp transformation to images, using an interactive
 * dialog in which the output field of view and resolution are shown and edited directly.
 * <p>
 * This is an alternative front end for {@link ApplyBigwarpPlugin}, which remains available
 * and unchanged. Where that plugin asks for a field-of-view option plus nine numeric
 * fields whose meaning depends on the option chosen, {@link ApplyTransformFrame} resolves
 * the chosen reference to concrete numbers and lets the user edit them, so what will be
 * rendered is always visible.
 *
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 */
public class ApplyBigwarpTransformPlugIn implements PlugIn
{
	// output destinations
	public static final String DEST_IMAGEJ = "Image window";
	public static final String DEST_FILE = "Image file";
	public static final String DEST_N5 = "N5/Zarr/HDF5";

	public static final String[] destinationOptions = new String[] { DEST_IMAGEJ, DEST_FILE, DEST_N5 };

	// interpolation
	public static final String INTERP_NEAREST = "Nearest Neighbor";
	public static final String INTERP_LINEAR = "Linear";

	public static final String[] interpolationOptions = new String[] { INTERP_NEAREST, INTERP_LINEAR };

	/** Sentinel meaning "no image selected", matching the existing plugin's dialog. */
	public static final String NO_IMAGE = "<None>";

	public <T> void runFromBigWarpInstance( final BigWarp<?> bw )
	{
		ApplyTransformFrame.createAndShow( bw );
	}

	@Override
	public void run( final String args )
	{
		if ( IJ.versionLessThan( "1.40" ) )
			return;

		final String macroOptions = Macro.getOptions();
		String options = args;
		if ( options == null || options.isEmpty() )
			options = macroOptions;

		final boolean isMacro = ( options != null && !options.isEmpty() );

		if ( isMacro )
			ApplyTransformFrame.runMacro( options );
		else
			ApplyTransformFrame.createAndShow();
	}

	/**
	 * Converts the interpolation option string to an {@link Interpolation}.
	 *
	 * @param interpolation the option string
	 * @return the interpolation, defaulting to linear
	 */
	public static Interpolation getInterpolation( final String interpolation )
	{
		return INTERP_NEAREST.equals( interpolation ) ? Interpolation.NEARESTNEIGHBOR : Interpolation.NLINEAR;
	}

	/**
	 * Applies the transformation and writes the result, as described by the parameters.
	 * <p>
	 * Every entry point - dialog, macro, and in-BigWarp menu item - funnels through here.
	 * The {@code data}, {@code ltm} and {@code bwTransform} arguments may be null, in
	 * which case they are built from the paths and options in {@code params}.
	 *
	 * @param params the export parameters
	 * @param dataIn the bigwarp data, or null to build it from {@code params}
	 * @param ltmIn the landmarks, or null to load them from {@code params}
	 * @param bwTransformIn the transformation, or null to build it from {@code params}
	 * @return the rendered images, or null when written to a container
	 */
	public static List<ImagePlus> runFromParameters( final ApplyTransformParameters params, final BigWarpData<?> dataIn,
			final LandmarkTableModel ltmIn, final BigWarpTransform bwTransformIn )
	{
		return runFromParameters( params, dataIn, ltmIn, bwTransformIn, true, false );
	}

	/**
	 * Applies the transformation and writes the result, as described by the parameters.
	 *
	 * @param params the export parameters
	 * @param dataIn the bigwarp data, or null to build it from {@code params}
	 * @param ltmIn the landmarks, or null to load them from {@code params}
	 * @param bwTransformIn the transformation, or null to build it from {@code params}
	 * @param show whether to show the result in an image window
	 * @param wait whether to block until rendering finishes
	 * @return the rendered images, or null when written to a container
	 */
	public static List<ImagePlus> runFromParameters( final ApplyTransformParameters params, final BigWarpData<?> dataIn,
			final LandmarkTableModel ltmIn, final BigWarpTransform bwTransformIn, final boolean show, final boolean wait )
	{
		BigWarpData<?> data = dataIn;
		LandmarkTableModel ltm = ltmIn;

		if ( data == null )
		{
			data = buildData( params, true );
			if ( data == null )
				return null;
		}

		if ( ltm == null )
		{
			ltm = loadLandmarks( params, BigWarp.detectNumDims( data.sources ), true );
			if ( ltm == null )
				return null;
		}

		BigWarpTransform bwTransform = bwTransformIn;
		if ( bwTransform == null )
			bwTransform = new BigWarpTransform( ltm, params.transformType );

		if ( params.pixSize == null || params.spacing == null || params.min == null )
		{
			IJ.showMessage( "Must specify the output field of view." );
			return null;
		}

		// bind the wildcard once, so data and data.sources agree on their element type
		return runExport( params, data, bwTransform, show, wait );
	}

	private static <T> List<ImagePlus> runExport( final ApplyTransformParameters params, final BigWarpData<T> data,
			final BigWarpTransform bwTransform, final boolean show, final boolean wait )
	{
		applyTransformToMovingSources( data, bwTransform.getTransformation() );

		/*
		 * The dialog has already resolved the field of view to concrete numbers, so pass
		 * them straight through rather than going back through the field-of-view option
		 * strings: ApplyBigwarpPlugin.getPhysicalOffset and getPixelOffset disagree about
		 * whether the offset they return is in pixel or physical units, and
		 * BigWarpExporter.setOffset wants physical.
		 */
		final Interval outputInterval = new FinalInterval( params.pixSize );
		final double[] offsetPhysical = params.min;
		final double[] resolution = params.spacing;

		final Interpolation interp = getInterpolation( params.interpolation );
		final ProgressWriter progressWriter = new ProgressWriterIJ();

		if ( DEST_N5.equals( params.destination ) )
		{
			runN5( data, params, outputInterval, interp, offsetPhysical, resolution, progressWriter );
			return null;
		}

		final boolean toFile = DEST_FILE.equals( params.destination );
		// an empty n5Dataset means BigWarpExporter writes with IJ.save instead
		final WriteDestinationOptions writeOpts = new WriteDestinationOptions(
				toFile ? params.path : "", "", params.getBlockSize(), params.getCompression() );

		return ApplyBigwarpPlugin.runExport( data, data.sources, params.fovReference,
				Collections.singletonList( outputInterval ), Collections.<String>emptyList(), interp,
				offsetPhysical, resolution, params.virtual, params.nThreads,
				progressWriter, show && !toFile, wait, writeOpts );
	}

	private static <T> void runN5( final BigWarpData<T> data, final ApplyTransformParameters params,
			final Interval outputInterval, final Interpolation interp, final double[] offsetPhysical,
			final double[] resolution, final ProgressWriter progressWriter )
	{
		if ( params.dataset == null || params.dataset.isEmpty() )
		{
			IJ.showMessage( "Must specify a dataset for n5/zarr/hdf5 output." );
			return;
		}

		final WriteDestinationOptions writeOpts = new WriteDestinationOptions( params.path, params.dataset,
				params.getBlockSize(), params.getCompression() );

		/*
		 * runN5Export renders a single source, so warn rather than silently dropping the
		 * others. This matches ApplyBigwarpPlugin, which also exports moving source 0 only.
		 */
		if ( data.numMovingSources() > 1 )
			IJ.log( "Writing only the first of " + data.numMovingSources()
					+ " moving sources; use the image window destination to render all of them." );

		final SourceAndConverter<T> src = data.getMovingSource( 0 );
		ApplyBigwarpPlugin.runN5Export( data, src, outputInterval, interp, offsetPhysical, resolution,
				params.unit, progressWriter, writeOpts, Executors.newFixedThreadPool( params.nThreads ) );
	}

	/**
	 * Points the moving sources at the transformation, unless they are already transformed
	 * (which is the case when running from a BigWarp instance).
	 * <p>
	 * Mirrors {@link ApplyBigwarpPlugin#apply}.
	 *
	 * @param data the bigwarp data
	 * @param invXfm the transformation
	 */
	private static <T> void applyTransformToMovingSources( final BigWarpData<T> data, final InvertibleRealTransform invXfm )
	{
		final int numChannels = data.numMovingSources();
		for ( int i = 0; i < numChannels; i++ )
		{
			final SourceAndConverter<T> movingSource = data.getMovingSource( i );
			final WarpedSource<?> ws = ( ( WarpedSource<?> ) ( movingSource.getSpimSource() ) );
			if ( ws.getTransform() == null && invXfm != null )
			{
				ws.updateTransform( invXfm );
				ws.setIsTransformed( true );
			}
		}
	}

	/**
	 * Builds the {@link BigWarpData} without reporting problems to the user.
	 * <p>
	 * For callers that build data speculatively - the dialog does this as the user edits
	 * the input fields - where an incomplete selection is expected rather than an error.
	 *
	 * @param params the parameters
	 * @return the data, or null if it could not be built
	 */
	public static BigWarpData<?> buildDataQuietly( final ApplyTransformParameters params )
	{
		return buildData( params, false );
	}

	/**
	 * Loads the landmarks without reporting problems to the user.
	 *
	 * @param params the parameters
	 * @param nd the dimensionality
	 * @return the landmarks, or null if they could not be loaded
	 */
	public static LandmarkTableModel loadLandmarksQuietly( final ApplyTransformParameters params, final int nd )
	{
		return loadLandmarks( params, nd, false );
	}

	/**
	 * Builds the {@link BigWarpData} from the image selections in the parameters.
	 * <p>
	 * Mirrors the source-loading sequence in {@link ApplyBigwarpPlugin#run(String)}.
	 *
	 * @param params the parameters
	 * @param verbose whether to show a message when a source cannot be read
	 * @return the data, or null if the moving image could not be read
	 */
	private static BigWarpData<?> buildData( final ApplyTransformParameters params, final boolean verbose )
	{
		final BigWarpData<?> data = BigWarpInit.initData();
		int id = 0;

		final ImagePlus movingIp = getImagePlus( params.movingImage );
		if ( movingIp != null )
		{
			id += BigWarpInit.add( data, movingIp, id, 0, true );
		}
		else if ( params.movingDataset != null && !params.movingDataset.isEmpty() )
		{
			BigWarpInit.addToData( data, true, id, params.movingRoot, params.movingDataset );
			id++;
		}
		else if ( params.movingRoot != null && !params.movingRoot.isEmpty() )
		{
			try
			{
				id += BigWarpInit.add( data, IJ.openImage( params.movingRoot ), id, 0, true );
			}
			catch ( final Exception e )
			{
				if ( verbose )
					IJ.showMessage( "could not read from file: " + params.movingRoot );

				return null;
			}
		}
		else
		{
			if ( verbose )
				IJ.showMessage( "Must provide a moving image." );

			return null;
		}

		final ImagePlus targetIp = getImagePlus( params.targetImage );
		if ( targetIp != null )
		{
			id += BigWarpInit.add( data, targetIp, id, 0, false );
		}
		else if ( params.targetDataset != null && !params.targetDataset.isEmpty() )
		{
			BigWarpInit.addToData( data, false, id, params.targetRoot, params.targetDataset );
			id++;
		}
		else if ( params.targetRoot != null && !params.targetRoot.isEmpty() )
		{
			try
			{
				id += BigWarpInit.add( data, IJ.openImage( params.targetRoot ), id, 0, false );
			}
			catch ( final Exception e )
			{
				// we're allowed not to have a target image
			}
		}

		final int nd = BigWarp.detectNumDims( data.sources );
		BigWarp.wrapMovingSources( nd, data );
		return data;
	}

	/**
	 * @param title an ImagePlus window title, or null / empty / {@link #NO_IMAGE}
	 * @return the image with that title, or null
	 */
	private static ImagePlus getImagePlus( final String title )
	{
		if ( title == null || title.isEmpty() || title.equals( NO_IMAGE ) || IJ.getInstance() == null )
			return null;

		return WindowManager.getImage( title );
	}

	private static LandmarkTableModel loadLandmarks( final ApplyTransformParameters params, final int nd,
			final boolean verbose )
	{
		if ( params.landmarkPath == null || params.landmarkPath.isEmpty() )
		{
			if ( verbose )
				IJ.showMessage( "Must provide landmark file." );

			return null;
		}

		final LandmarkTableModel ltm = new LandmarkTableModel( nd );
		try
		{
			ltm.load( new File( params.landmarkPath ) );
		}
		catch ( final IOException e )
		{
			if ( verbose )
			{
				e.printStackTrace();
				IJ.showMessage( "Could not read landmarks from: " + params.landmarkPath );
			}
			return null;
		}
		return ltm;
	}

	/**
	 * Everything needed to render and write a transformed image.
	 * <p>
	 * Immutable, and buildable without any Swing, so it can be constructed from the
	 * dialog, from macro options, or directly from a script or test.
	 */
	public static class ApplyTransformParameters
	{
		// inputs (only used when not running from a BigWarp instance)
		public final String landmarkPath;
		public final String movingImage;
		public final String movingRoot;
		public final String movingDataset;
		public final String targetImage;
		public final String targetRoot;
		public final String targetDataset;
		public final String transformType;

		// field of view, as resolved by the dialog
		public final long[] pixSize;
		public final double[] spacing;
		public final double[] min;
		public final String unit;

		/** The reference the field of view was resolved from; informational only. */
		public final String fovReference;
		public final String pointFilter;
		public final String bboxMethod;
		public final int bboxSamples;

		// output
		public final String interpolation;
		public final boolean virtual;
		public final int nThreads;

		public final String destination;
		public final String path;
		public final String dataset;
		public final String blockSizeString;
		public final String compressionString;

		public ApplyTransformParameters(
				final String landmarkPath,
				final String movingImage,
				final String movingRoot,
				final String movingDataset,
				final String targetImage,
				final String targetRoot,
				final String targetDataset,
				final String transformType,
				final long[] pixSize,
				final double[] spacing,
				final double[] min,
				final String unit,
				final String fovReference,
				final String pointFilter,
				final String bboxMethod,
				final int bboxSamples,
				final String interpolation,
				final boolean virtual,
				final int nThreads,
				final String destination,
				final String path,
				final String dataset,
				final String blockSizeString,
				final String compressionString )
		{
			this.landmarkPath = landmarkPath;
			this.movingImage = movingImage;
			this.movingRoot = movingRoot;
			this.movingDataset = movingDataset;
			this.targetImage = targetImage;
			this.targetRoot = targetRoot;
			this.targetDataset = targetDataset;
			this.transformType = transformType;

			this.pixSize = pixSize;
			this.spacing = spacing;
			this.min = min;
			this.unit = unit;

			this.fovReference = fovReference;
			this.pointFilter = pointFilter;
			this.bboxMethod = bboxMethod;
			this.bboxSamples = bboxSamples;

			this.interpolation = interpolation;
			this.virtual = virtual;
			this.nThreads = nThreads;

			this.destination = destination;
			this.path = path;
			this.dataset = dataset;
			this.blockSizeString = blockSizeString;
			this.compressionString = compressionString;
		}

		public int[] getBlockSize()
		{
			if ( blockSizeString == null || blockSizeString.isEmpty() )
				return null;

			return ApplyBigwarpPlugin.parseBlockSize( blockSizeString, pixSize == null ? 3 : pixSize.length );
		}

		public Compression getCompression()
		{
			return ApplyBigwarpPlugin.getCompression( compressionString == null ? "" : compressionString );
		}
	}
}
