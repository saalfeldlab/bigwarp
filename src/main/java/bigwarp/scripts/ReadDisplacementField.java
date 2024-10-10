package bigwarp.scripts;

import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5DisplacementField;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.TransformUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import bigwarp.transforms.NgffTransformations;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultAxisType;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

@Plugin(type = Command.class, menuPath = "Plugins>Transform>Read Displacement Field")
public class ReadDisplacementField  implements Callable<Void>, Command {

	public static final String NATIVE = "NATIVE";
	public static final String[] AXIS_LABELS = new String[] { "x", "y", "z" };

	@Parameter
	private UIService ui;

	@Parameter
	private DatasetService ds;

	@Parameter
	private LogService log;

	@Parameter
	private String n5Root;

	@Parameter
	private String n5Dataset;

	@Parameter(label = "Thread count", required = true, min = "1", max = "999")
	private int nThreads = 1;

	private CalibratedAxis[] axes;

	private Axis[] csAxes;

	@Override
	public void run() {

		call();
	}

	@Override
	public Void call() {

		process();
		return null;
	}

	public <T extends RealType<T> & NativeType<T>> void process() {

		final RandomAccessibleInterval<T> dfieldRai = readDataAndMetadata();
		final Dataset dataset = ds.create(dfieldRai);
		dataset.setName(n5Dataset);
		dataset.setAxes(axes);
		ui.show(dataset);
	}

	private <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> readDataAndMetadata() {

		try (N5Reader n5 = new N5Factory().gsonBuilder(NgffTransformations.gsonBuilder()).openReader(n5Root)) {

			if( isN5Field(n5, n5Dataset))
				return readN5(n5, n5Dataset);
			else if( isNgffField(n5, n5Dataset))
				return readNgff(n5, n5Dataset);

		} catch (Exception e) {
			e.printStackTrace();
		}
		throw new N5Exception("Could not read displacement field from: " + n5Root + "  " + n5Dataset);
	}

	private boolean isN5Field(final N5Reader n5, final String n5Dataset) {

		return n5.getAttribute(n5Dataset, N5DisplacementField.SPACING_ATTR, double[].class) != null;
	}

	@SuppressWarnings("unchecked")
	private  <Q extends RealType<Q> & NativeType<Q>, T extends RealType<T>> RandomAccessibleInterval<T> readN5(final N5Reader n5, final String n5Dataset) {

		createAxesN5(n5, n5Dataset);

		DataType type = n5.getDatasetAttributes(n5Dataset).getDataType();
		boolean isQuantized = !(type == DataType.FLOAT64 || type == DataType.FLOAT32);

		final RandomAccessibleInterval<T> img;
		if (isQuantized)
			img = N5DisplacementField.openQuantized(n5, n5Dataset, (Q)N5Utils.type(type), (T)new FloatType());
		else
			img = (RandomAccessibleInterval<T>)N5DisplacementField.openRaw(n5, n5Dataset, new FloatType());

		final int nd = img.numDimensions();
		final RandomAccessibleInterval<T> imgp;
		if (nd == 4)
			imgp = Views.moveAxis(img, nd - 2, nd - 1);
		else if (nd == 3)
			imgp = img;
		else
			throw new N5Exception("Dataset must be 3D or 4D, but had " + nd + " dimensions");

		return imgp;
	}

	@SuppressWarnings("unchecked")
	private <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> readNgff(final N5Reader n5, final String n5Dataset) {

		createAxesNgff(n5, n5Dataset);
		final CachedCellImg<?, ?> img = N5Utils.open(n5, n5Dataset);
		final int nd = img.numDimensions();

		// need to permute the axes because we permute the image below
		final int[] p = IntStream.range(0, nd).toArray();
		p[0] = 2;
		p[2] = 0;

		AxisUtils.permute(axes, axes, p);
		return (RandomAccessibleInterval<T>)Views.moveAxis(img, 0, 2);
	}

	private CalibratedAxis[] createAxesN5(final N5Reader n5, final String n5Dataset) {

		final DatasetAttributes dsetAttrs = n5.getDatasetAttributes(n5Dataset);
		if (dsetAttrs == null)
			throw new N5Exception("No dataset at" + n5Dataset);

		final double[] spacing = n5.getAttribute(n5Dataset, N5DisplacementField.SPACING_ATTR, double[].class);
		final double[] offset = n5.getAttribute(n5Dataset, N5DisplacementField.OFFSET_ATTR, double[].class);

		final int nd = dsetAttrs.getNumDimensions();
		// last axis always hold vector dimension
		axes = new CalibratedAxis[nd];
		int j = 0;
		for (int i = 0; i < nd; i++) {
			if (i == 2)
				axes[i] = new DefaultLinearAxis(new DefaultAxisType("v", false), "px");
			else {
				axes[i] = new DefaultLinearAxis(new DefaultAxisType(AXIS_LABELS[j], true), "px", spacing[j], offset[j]);
				j++;
			}
		}

		return axes;
	}

	private boolean isNgffField(final N5Reader n5, final String n5Dataset) {

		return n5.getAttribute(n5Dataset, CoordinateTransform.KEY, CoordinateTransform[].class) != null;
	}

	private CalibratedAxis[] createAxesNgff(final N5Reader n5, final String n5Dataset) {

		final CoordinateTransform<?>[] cts = n5.getAttribute(n5Dataset, CoordinateTransform.KEY, CoordinateTransform[].class);
		final CoordinateSystem[] css = n5.getAttribute(n5Dataset, CoordinateSystem.KEY, CoordinateSystem[].class);
		csAxes = css[0].getAxes();

		final AffineGet affine = TransformUtils.toAffine(cts[0], csAxes.length);

		final int nd = csAxes.length;
		axes = new CalibratedAxis[nd];

		for (int i = 0; i < nd; i++) {
			final Axis csAxis = csAxes[i];
			if (csAxis.getType().equals(Axis.DISPLACEMENT)) {
				axes[i] = new DefaultLinearAxis(new DefaultAxisType("v", false), "px");
			} else {
				axes[i] = new DefaultLinearAxis(
						new DefaultAxisType(csAxis.getName(), true),
						csAxis.getUnit(),
						affine.get(i, i),
						affine.get(i, nd));
			}
		}

		return axes;
	}

}