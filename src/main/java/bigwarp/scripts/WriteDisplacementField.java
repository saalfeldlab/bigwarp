package bigwarp.scripts;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.ij.N5ScalePyramidExporter;
import org.janelia.saalfeldlab.n5.imglib2.N5DisplacementField;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.view.Views;

@Plugin(type = Command.class, menuPath = "Plugins>Transform>Write Displacement Field")
public class WriteDisplacementField  implements Callable<Void>, Command {

	// Output types
	public static final String INT8 = "INT8";
	public static final String INT16 = "INT16";
	public static final String FLOAT32 = "FLOAT32";
	public static final String FLOAT64 = "FLOAT64";

	@Parameter
	private UIService ui;

	@Parameter
	private LogService log;

	@Parameter
	private String n5Root;

	@Parameter
	private String n5Dataset;

	@Parameter
	private Dataset dataset;

	@Parameter(label = "Chunk size", description = "The size of chunks. Comma separated, for example: \"64,32,16\".\n "
			+
			"ImageJ's axis order is X,Y,C,Z,T. The chunk size must be specified in this order.\n" +
			"You must skip any axis whose size is 1, e.g. a 2D time-series without channels\n" +
			"may have a chunk size of 1024,1024,1 (X,Y,T).\n" +
			"You may provide fewer values than the data dimension. In that case, the size will\n" +
			"be expanded to necessary size with the last value, for example \"64\", will expand\n" +
			"to \"64,64,64\" for 3D data.")
	private String chunkSizeArg;

	@Parameter(label = "Compression", style = "listBox", choices = {
			N5ScalePyramidExporter.GZIP_COMPRESSION,
			N5ScalePyramidExporter.RAW_COMPRESSION,
			N5ScalePyramidExporter.LZ4_COMPRESSION,
			N5ScalePyramidExporter.XZ_COMPRESSION,
			N5ScalePyramidExporter.BLOSC_COMPRESSION,
			N5ScalePyramidExporter.ZSTD_COMPRESSION})
	private String compressionArg = N5ScalePyramidExporter.GZIP_COMPRESSION;

	@Parameter(label = "Output type", style = "listBox", choices = {
			"FLOAT64", "FLOAT32", "INT16", "INT8"
	})
	private String outputType;

	@Parameter(label = "Thread count", required = true, min = "1", max = "999")
	private int nThreads = 1;

	private int nd = -1;
	private int vectorDim = -1;
	private int vectorSize = -1;

	@SuppressWarnings({ "unchecked" })
	public <T extends RealType<T> & NativeType<T>, S extends RealType<S> & NativeType<S>, Q extends NativeType<Q> & IntegerType<Q>> void process() {
		final AffineGet affine = null;
		final Compression compression = N5ScalePyramidExporter.getCompression(compressionArg);

		nd = dataset.numDimensions() - 1;
		final long[] spatialDims = new long[nd];
		final double[] offset = new double[nd];
		final double[] spacing = new double[nd];
		Arrays.fill(spacing, 1.0);

		int j = 0;
		for (int i = 0; i < dataset.numDimensions(); i++) {

			if (dataset.axis(i).type().isSpatial()) {
				spatialDims[j] = dataset.dimension(i);
				offset[j] = dataset.axis(i).calibratedValue(0.0);
				spacing[j++] = dataset.averageScale(i);
			} else {
				vectorDim = i;
				vectorSize = (int) dataset.dimension(i);
			}
		}
		
		validateAndWarn();

		final int[] chunkSizeSpatial = N5ScalePyramidExporter.parseBlockSize(chunkSizeArg, spatialDims);
		final int[] chunkSize = IntStream.concat(
				IntStream.of(vectorSize),
				Arrays.stream(chunkSizeSpatial)).toArray();

		final RandomAccessibleInterval<T> vectorAxisFirst = (RandomAccessibleInterval<T>) Views.moveAxis( (RandomAccessibleInterval<T>)dataset, vectorDim, 0);
		try (N5Writer n5 = new N5Factory().openWriter(n5Root)) {

			if (outputType.equals(FLOAT32) || outputType.equals(FLOAT64)) {
				final RandomAccessibleInterval<S> converted = convertIfNecessary(vectorAxisFirst, (S)getTargetType());
				N5DisplacementField.save(n5, n5Dataset, affine, converted, spacing, offset, chunkSize, compression);
			}
			else {
				final Q quantizedType = (Q)getTargetType();
				N5DisplacementField.save(n5, n5Dataset, affine, vectorAxisFirst, spacing, offset, chunkSize, compression, quantizedType, 1e-6);
			}

		} catch (Exception e) {
			System.err.println("Failed to write displacement field at " + n5Root);
			e.printStackTrace();
		}

	}

	@SuppressWarnings("unchecked")
	private <T extends RealType<T> & NativeType<T>> T getTargetType() {
		switch (outputType) {
		case FLOAT32:
				return (T)new FloatType();
		case FLOAT64:
				return (T)new DoubleType();
		case INT16:
			return (T)new ShortType();
		case INT8:
			return (T)new ByteType();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T extends RealType<T> & NativeType<T>, S extends RealType<S> & NativeType<S>> RandomAccessibleInterval<S> convertIfNecessary(
			final RandomAccessibleInterval<T> dfield, final S targetType) {

		if (dfield.getType().getClass().equals(targetType.getClass()))
			return (RandomAccessibleInterval<S>) dfield;

		final Converter<T, S> conv = new Converter<T, S>() {

			@Override
			public void convert(T input, S output) {

				output.setReal(input.getRealDouble());
			}
		};
		return Converters.convertRAI(dfield, conv, targetType);
	}

	private void validateAndWarn() {

		if (vectorSize != nd) {
			ui.showDialog(String.format("Error: channel dimension size (%d) must match dimensionality (%d). Exiting.",
					vectorSize, nd));
			return;
		}
	}

	@Override
	public void run() {

		call();
	}

	@Override
	public Void call() {

		process();
		return null;
	}

}
