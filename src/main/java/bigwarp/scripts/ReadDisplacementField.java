package bigwarp.scripts;

import java.util.concurrent.Callable;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5DisplacementField;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultAxisType;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.ShortType;

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

	@Parameter(label = "ResultType type", style = "listBox", choices = { "FLOAT64", "FLOAT32", "NATIVE" })
	private String resultType;

	@Parameter(label = "Thread count", required = true, min = "1", max = "999")
	private int nThreads = 1;

	private CalibratedAxis[] axes;

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

	@SuppressWarnings("unchecked")
	private <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> readDataAndMetadata() {

		try (N5Reader n5 = new N5Factory().openReader(n5Root)) {
			createAxes(n5, n5Dataset);
			final RandomAccessibleInterval<T> img;
			if( resultType.equals(NATIVE))
				img = N5DisplacementField.openRaw(n5, n5Dataset, (T)getRawZero(n5, n5Dataset));
			else
				img = N5DisplacementField.openRaw(n5, n5Dataset, (T)getTargetType());

			final int nd = img.numDimensions();
			final RandomAccessibleInterval<T> imgp;
			if( nd == 4 )
				imgp = Views.moveAxis(img, nd-2, nd-1);
			else if( nd == 3 )
				imgp = img;
			else
				throw new N5Exception("Dataset must be 3D or 4D, but had " + nd + " dimensions");

			return imgp;

		}catch( Exception e ) {
			e.printStackTrace();
		}
		throw new N5Exception("Could not read displacement field from: " + n5Root + "  " + n5Dataset);
	}

	private CalibratedAxis[] createAxes(N5Reader n5, final String n5Dataset) {
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

	@SuppressWarnings({ "incomplete-switch", "unchecked" })
	private static <T extends RealType<T> & NativeType<T>> T getRawZero( N5Reader n5, final String n5Dataset ) {

		// The types enumerated here are the only allowed types for displacement fields
		final DatasetAttributes dsetAttrs = n5.getDatasetAttributes(n5Dataset);
		if( dsetAttrs == null)
			throw new N5Exception("No dataset at" + n5Dataset);

		switch( dsetAttrs.getDataType() ) {
		case INT8:
			return (T)new ByteType();
		case INT16:
			return (T)new ShortType();
		case FLOAT32:
			return (T)new FloatType();
		case FLOAT64:
			return (T)new DoubleType();
		}
		throw new N5Exception("Unexpected type: " + dsetAttrs.getDataType());
	}


	@SuppressWarnings("unchecked")
	private <T extends RealType<T> & NativeType<T>> T getTargetType() {
		switch (resultType) {
		case WriteDisplacementField.FLOAT32:
				return (T)new FloatType();
		case WriteDisplacementField.FLOAT64:
				return (T)new DoubleType();
		}
		return null;
	}


}
