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
package bigwarp.scripts;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.ij.N5ScalePyramidExporter;
import org.janelia.saalfeldlab.n5.universe.N5Factory;

import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.scripts.CompareUtils.TransformType;
import bigwarp.source.JacobianDeterminantRandomAccess;
import bigwarp.transforms.BigWarpTransform;
import bigwarp.transforms.NgffTransformations;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import net.imglib2.realtransform.InvertibleWrapped2DTransformAs3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.inverse.DifferentiableRealTransform;
import net.imglib2.realtransform.inverse.RealTransformFiniteDerivatives;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Writes the jacobian determinant of a transformation, sampled on a grid, to a
 * tif or an N5 / Zarr container.
 * <p>
 * The transformation is either solved from a BigWarp landmark csv or opened
 * from a url ({@link NgffTransformations#open(String)}, for example a
 * displacement field). It is used as is: a BigWarp transformation maps target
 * to moving, so the determinant is that of the target to moving map, sampled in
 * target space, as BigWarp's "Jacobian determinant" overlay shows it. Values
 * below one mean the target is locally smaller than the moving image, and
 * values at or below zero mean the transformation folds.
 * <p>
 * Thin plate splines are differentiated exactly. Every other transformation is
 * differentiated with one-sided finite differences, with a step of
 * <code>--step</code> physical units (default: a tenth of the smallest output
 * spacing).
 * <p>
 * The output grid is the grid of <code>--reference</code>, or the physical
 * interval <code>--min</code> to <code>--max</code>, sampled at
 * <code>--spacing</code>, exactly as for {@link RenderTimeVarying}. Without a
 * reference image the spacing defaults to one.
 */
@Command( name = "jacobian-determinant", mixinStandardHelpOptions = true, version = "jacobian-determinant 1.0",
		description = "Writes the jacobian determinant of a transformation, sampled on a grid, to a tif or an N5 / Zarr container." )
public class JacobianDeterminant implements Callable< Integer >
{
	@Parameters( index = "0", arity = "0..1", paramLabel = "LANDMARKS",
			description = "Landmark csv file the transformation is solved from. Give this or --transform-url." )
	private File landmarks;

	@Option( names = { "--transform-type", "-t" }, defaultValue = "TPS",
			description = "Transformation type solved from the landmarks, one of: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})." )
	private TransformType transformType;

	@Option( names = { "--transform-url" },
			description = "Url of a stored transformation, e.g. an NGFF displacement field. Give this or LANDMARKS." )
	private String transformUrl;

	@Option( names = { "--reference", "-r" },
			description = "Image whose grid (size, spacing, origin) the determinant is sampled on, unless --min and --max are given." )
	private File reference;

	@Option( names = { "--min" }, split = ",",
			description = "Output field of view min, physical units, comma separated. Requires --max." )
	private double[] fovMin;

	@Option( names = { "--max" }, split = ",",
			description = "Output field of view max, physical units, comma separated. Requires --min." )
	private double[] fovMax;

	@Option( names = { "--spacing" }, split = ",",
			description = "Output pixel spacing, physical units, comma separated; one value is used for every dimension "
					+ "(default: the spacing of the reference image if given, else 1)." )
	private double[] fovSpacing;

	@Option( names = { "--output", "-o" }, required = true,
			description = "Output: a .tif or .tiff file, otherwise an N5 / Zarr container root (the format is chosen by its extension)." )
	private String output;

	@Option( names = { "--dataset" }, defaultValue = "jacobian",
			description = "Dataset in the N5 / Zarr container (default: ${DEFAULT-VALUE})." )
	private String dataset;

	@Option( names = { "--chunk-size" }, defaultValue = "64",
			description = "N5 / Zarr chunk size, comma separated; the last value is repeated as needed (default: ${DEFAULT-VALUE})." )
	private String chunkSize;

	@Option( names = { "--compression" }, defaultValue = N5ScalePyramidExporter.GZIP_COMPRESSION,
			description = "N5 / Zarr compression, one of: gzip, raw, lz4, xz, blosc, zstd (default: ${DEFAULT-VALUE})." )
	private String compression;

	@Option( names = { "--overwrite" },
			description = "Replace the N5 / Zarr dataset if it exists." )
	private boolean overwrite;

	@Option( names = { "--step" },
			description = "Finite difference step, physical units (default: a tenth of the smallest output spacing). "
					+ "Not used for thin plate splines." )
	private Double step;

	@Option( names = { "--threads" },
			description = "Number of threads (default: the number of processors)." )
	private Integer threads;

	public static void main( final String[] args )
	{
		// exit explicitly: ImageJ / AWT classes on the classpath may have
		// started non-daemon threads that would otherwise keep the jvm alive
		System.exit( new CommandLine( new JacobianDeterminant() ).execute( args ) );
	}

	@Override
	public Integer call()
	{
		if ( ( landmarks == null ) == ( transformUrl == null ) )
		{
			System.err.println( "error: give exactly one of LANDMARKS or --transform-url" );
			return 1;
		}

		final int nThreads = threads != null ? threads : Runtime.getRuntime().availableProcessors();
		if ( nThreads < 1 )
		{
			System.err.println( "error: --threads must be at least 1" );
			return 1;
		}

		if ( ( fovMin == null ) != ( fovMax == null ) )
		{
			System.err.println( "error: --min and --max must be given together" );
			return 1;
		}

		if ( reference == null && fovMin == null )
		{
			System.err.println( "error: give --reference, or --min and --max, to define the output grid" );
			return 1;
		}

		if ( step != null && !( step > 0 && !Double.isInfinite( step ) ) )
		{
			System.err.println( "error: --step must be positive" );
			return 1;
		}

		// the transformation
		final RealTransform transform;
		final String source;
		if ( landmarks != null )
		{
			final LandmarkTableModel ltm;
			try
			{
				ltm = CompareUtils.loadLandmarks( landmarks );
			}
			catch ( final Exception e )
			{
				System.err.println( "error: " + e.getMessage() );
				return 1;
			}

			final BigWarpTransform bwt = new BigWarpTransform( ltm, transformType.bigwarpName() );
			if ( !CompareUtils.checkEnoughLandmarks( ltm, landmarks, CompareUtils.minimumLandmarks( bwt, ltm.getNumdims() ) ) )
				return 1;

			// force3D must be false so that 2d landmarks yield a 2d transformation
			transform = bwt.getTransformation( false );
			source = String.format( "%s (%s, %d active landmarks)", landmarks, transformType, ltm.getActiveRowCount() );
		}
		else
		{
			try
			{
				transform = NgffTransformations.open( transformUrl );
			}
			catch ( final Exception e )
			{
				System.err.println( "error: could not open transformation " + transformUrl + ": " + e.getMessage() );
				return 1;
			}
			if ( transform == null )
			{
				System.err.println( "error: could not open transformation " + transformUrl );
				return 1;
			}
			source = transformUrl;
		}

		final int nd = transform.numSourceDimensions();
		if ( nd != 2 && nd != 3 )
		{
			System.err.println( String.format( "error: only 2d and 3d transformations are supported, got %dd", nd ) );
			return 1;
		}
		if ( transform.numTargetDimensions() != nd )
		{
			System.err.println( String.format( "error: the transformation maps %dd to %dd, but must map nd to nd",
					nd, transform.numTargetDimensions() ) );
			return 1;
		}

		if ( !checkLength( "--min", fovMin, nd ) || !checkLength( "--max", fovMax, nd ) )
			return 1;

		if ( fovSpacing != null && fovSpacing.length != 1 && fovSpacing.length != nd )
		{
			System.err.println( String.format( "error: the transformation is %dd, but --spacing has %d values (give 1 or %d)",
					nd, fovSpacing.length, nd ) );
			return 1;
		}

		// the output grid
		final ImagePlus referenceImp = reference != null ? open( reference ) : null;
		if ( reference != null && referenceImp == null )
			return 1;

		if ( referenceImp != null && nd == 2 && referenceImp.getNSlices() > 1 )
		{
			System.err.println( "error: the transformation is 2d, but the reference image has more than one z slice" );
			return 1;
		}

		final OutputGrid grid;
		try
		{
			grid = referenceImp != null
					? OutputGrid.create( referenceImp, nd, fovMin, fovMax, fovSpacing )
					: OutputGrid.create( nd, fovMin, fovMax, fovSpacing );
		}
		catch ( final IllegalArgumentException e )
		{
			System.err.println( "error: " + e.getMessage() );
			return 1;
		}

		final double usedStep = step != null ? step : 0.1 * Arrays.stream( grid.spacing ).min().getAsDouble();
		final DifferentiableRealTransform differentiable = toDifferentiable( transform, usedStep );
		final boolean exact = !( differentiable instanceof RealTransformFiniteDerivatives );

		System.out.println( String.format( "transform:   %s", source ) );
		System.out.println( String.format( "derivatives: %s", exact ? "exact" : String.format( "finite differences, step %s", usedStep ) ) );
		System.out.println( String.format( "output grid: %s, spacing %s, origin %s (%s)", Arrays.toString( grid.size ),
				Arrays.toString( grid.spacing ), Arrays.toString( grid.origin ),
				fovMin != null ? "from --min / --max" : "from " + reference ) );
		System.out.println( String.format( "threads:     %d", nThreads ) );

		final float[][] planes;
		final ExecutorService exec = Executors.newFixedThreadPool( nThreads );
		try
		{
			final long start = System.currentTimeMillis();
			planes = compute( differentiable, grid, exec, nThreads );
			System.out.println( String.format( "computed in %.1f s", ( System.currentTimeMillis() - start ) / 1000.0 ) );
		}
		catch ( final InterruptedException | ExecutionException e )
		{
			System.err.println( "error: computing the jacobian determinant failed: " + e );
			return 1;
		}
		finally
		{
			exec.shutdown();
		}

		printStats( planes );

		final int w = (int) grid.size[ 0 ];
		final int h = (int) grid.size[ 1 ];
		final ImageStack stack = new ImageStack( w, h );
		for ( final float[] plane : planes )
			stack.addSlice( new FloatProcessor( w, h, plane ) );

		final ImagePlus out = new ImagePlus( "jacobian determinant", stack );
		grid.calibrate( out, referenceImp );

		return isTif( output ) ? writeTif( out ) : writeN5( out, nThreads );
	}

	/**
	 * A differentiable version of the transformation. Differentiable
	 * transformations, thin plate splines among them, are used as they are,
	 * after unwrapping the wrappers BigWarp puts around them. Any other is
	 * differentiated with finite differences.
	 *
	 * @param transform the transformation
	 * @param step the finite difference step
	 * @return a differentiable transformation
	 */
	static DifferentiableRealTransform toDifferentiable( final RealTransform transform, final double step )
	{
		if ( transform instanceof DifferentiableRealTransform )
			return (DifferentiableRealTransform) transform;
		else if ( transform instanceof WrappedIterativeInvertibleRealTransform )
			return toDifferentiable( ( (WrappedIterativeInvertibleRealTransform< ? >) transform ).getTransform(), step );
		else if ( transform instanceof InvertibleWrapped2DTransformAs3D )
			return toDifferentiable( ( (InvertibleWrapped2DTransformAs3D) transform ).getTransform(), step );

		final RealTransformFiniteDerivatives fd = new RealTransformFiniteDerivatives( transform );
		fd.setStep( step );
		return fd;
	}

	/**
	 * The jacobian determinant of the transformation at every point of the
	 * grid, computed in parallel over bands of rows. Each task differentiates
	 * its own copy of the transformation, since the jacobian is written to
	 * scratch space the transformation owns.
	 *
	 * @param transform the transformation
	 * @param grid the grid
	 * @param exec the executor
	 * @param nThreads the number of threads
	 * @return one plane per z (a single plane in 2d), x fastest
	 */
	static float[][] compute( final DifferentiableRealTransform transform, final OutputGrid grid,
			final ExecutorService exec, final int nThreads ) throws InterruptedException, ExecutionException
	{
		final int nd = grid.size.length;
		final int w = (int) grid.size[ 0 ];
		final int h = (int) grid.size[ 1 ];
		final int depth = nd == 2 ? 1 : (int) grid.size[ 2 ];
		final int nBands = Math.max( 1, Math.min( h, 4 * nThreads ) );

		final float[][] planes = new float[ depth ][ w * h ];
		final List< Future< ? > > futures = new ArrayList<>();
		for ( int z = 0; z < depth; z++ )
		{
			final int zi = z;
			for ( int b = 0; b < nBands; b++ )
			{
				final int y0 = (int) ( ( (long) h * b ) / nBands );
				final int y1 = (int) ( ( (long) h * ( b + 1 ) ) / nBands );
				if ( y1 <= y0 )
					continue;

				futures.add( exec.submit( () -> {
					final DifferentiableRealTransform t = transform.copy();
					final double[] x = new double[ nd ];
					if ( nd == 3 )
						x[ 2 ] = zi * grid.spacing[ 2 ] + grid.origin[ 2 ];

					final float[] plane = planes[ zi ];
					for ( int y = y0; y < y1; y++ )
					{
						x[ 1 ] = y * grid.spacing[ 1 ] + grid.origin[ 1 ];
						for ( int xi = 0; xi < w; xi++ )
						{
							x[ 0 ] = xi * grid.spacing[ 0 ] + grid.origin[ 0 ];
							plane[ y * w + xi ] = (float) JacobianDeterminantRandomAccess.determinant( t.jacobian( x ) );
						}
					}
				} ) );
			}
		}
		for ( final Future< ? > f : futures )
			f.get();

		return planes;
	}

	private static void printStats( final float[][] planes )
	{
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		double sum = 0;
		long n = 0;
		long folded = 0;
		long nonFinite = 0;
		for ( final float[] plane : planes )
			for ( final float v : plane )
			{
				if ( !Float.isFinite( v ) )
				{
					nonFinite++;
					continue;
				}
				min = Math.min( min, v );
				max = Math.max( max, v );
				sum += v;
				n++;
				if ( v <= 0 )
					folded++;
			}

		if ( n == 0 )
		{
			System.out.println( "determinant: no finite values" );
			return;
		}
		System.out.println( String.format( "determinant: min %s, max %s, mean %s", CompareUtils.format( min, 6 ),
				CompareUtils.format( max, 6 ), CompareUtils.format( sum / n, 6 ) ) );
		System.out.println( String.format( "folded:      %d of %d pixels (%.4f%%) have a determinant <= 0", folded, n,
				100.0 * folded / n ) );
		if ( nonFinite > 0 )
			System.out.println( String.format( "non-finite:  %d pixels", nonFinite ) );
	}

	private int writeTif( final ImagePlus out )
	{
		final File f = new File( output );
		final File parent = f.getAbsoluteFile().getParentFile();
		if ( parent != null )
			parent.mkdirs();

		if ( !IJ.saveAsTiff( out, f.getAbsolutePath() ) )
		{
			System.err.println( "error: could not write " + output );
			return 1;
		}
		System.out.println( "wrote " + output );
		return 0;
	}

	private int writeN5( final ImagePlus out, final int nThreads )
	{
		// the exporter opens a dialog rather than overwriting without asking,
		// and does not report failure, so check before and after
		final boolean exists = datasetExists();
		if ( exists && !overwrite )
		{
			System.err.println( String.format( "error: %s already exists in %s, use --overwrite to replace it", dataset, output ) );
			return 1;
		}

		final String chunks = expandChunkSize( chunkSize, out.getNSlices() > 1 ? 3 : 2 );
		if ( chunks == null )
		{
			System.err.println( "error: --chunk-size must be positive integers, comma separated, got " + chunkSize );
			return 1;
		}

		final String metadataStyle = output.toLowerCase( Locale.ROOT ).contains( ".zarr" )
				? N5Importer.MetadataOmeZarrKey : N5Importer.MetadataN5ViewerKey;

		final N5ScalePyramidExporter exporter = new N5ScalePyramidExporter( out, output, dataset,
				N5ScalePyramidExporter.AUTO_FORMAT, chunks, false, N5ScalePyramidExporter.NONE, metadataStyle, compression );
		exporter.setOverwrite( overwrite );
		exporter.setNumThreads( nThreads );
		exporter.run();

		if ( !datasetExists() )
		{
			System.err.println( String.format( "error: could not write %s to %s", dataset, output ) );
			return 1;
		}
		System.out.println( String.format( "wrote %s in %s (%s metadata)", dataset, output, metadataStyle ) );
		return 0;
	}

	/**
	 * The chunk size with one value per dimension, the last value repeated as
	 * needed. The exporter does not repeat a single value for z itself: given
	 * "64" for a 3d image it writes chunks one slice deep.
	 *
	 * @param arg the comma separated chunk size
	 * @param nd the number of dimensions
	 * @return the chunk size, or null if arg is not positive integers
	 */
	static String expandChunkSize( final String arg, final int nd )
	{
		final String[] parts = arg.split( "," );
		final int[] sizes = new int[ Math.max( nd, parts.length ) ];
		try
		{
			for ( int i = 0; i < parts.length; i++ )
				sizes[ i ] = Integer.parseInt( parts[ i ].trim() );
		}
		catch ( final NumberFormatException e )
		{
			return null;
		}
		for ( int i = parts.length; i < sizes.length; i++ )
			sizes[ i ] = sizes[ parts.length - 1 ];

		if ( Arrays.stream( sizes ).anyMatch( s -> s < 1 ) )
			return null;

		return Arrays.stream( sizes ).mapToObj( Integer::toString ).collect( Collectors.joining( "," ) );
	}

	private boolean datasetExists()
	{
		try ( final N5Reader n5 = new N5Factory().openReader( output ) )
		{
			return n5.exists( dataset );
		}
		catch ( final Exception e )
		{
			// the container does not exist yet
			return false;
		}
	}

	private static boolean isTif( final String path )
	{
		final String lower = path.toLowerCase( Locale.ROOT );
		return lower.endsWith( ".tif" ) || lower.endsWith( ".tiff" );
	}

	private static ImagePlus open( final File f )
	{
		if ( !f.exists() )
		{
			System.err.println( "error: no such file: " + f );
			return null;
		}
		final ImagePlus imp = IJ.openImage( f.getAbsolutePath() );
		if ( imp == null )
			System.err.println( "error: could not open image: " + f );

		return imp;
	}

	private static boolean checkLength( final String name, final double[] values, final int nd )
	{
		if ( values == null || values.length == nd )
			return true;

		System.err.println( String.format( "error: the transformation is %dd, but %s has %d values", nd, name, values.length ) );
		return false;
	}
}
