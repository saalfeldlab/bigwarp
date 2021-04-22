/**
 *
 */
package bigwarp.spark;

import java.io.IOException;
import java.util.HashMap;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.ij.N5Factory;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;

/**
 * Manage named singleton instances.
 *
 * Useful for sharing instances of objects between threads.
 *
 * Our use case for this is to share cached N5 images between
 * threads of a Spark cluster running on the same machine.
 *
 * Example:
 * <pre>
 * </pre>
 *
 * @author Stephan Saalfeld
 *
 */
public class Singleton {

	@FunctionalInterface
	public static interface ThrowingSupplier<T, E extends Exception> {

	    public T get() throws E;
	}

	private Singleton() {}

	static private HashMap<String, Object> singletons = new HashMap<>();

	public static synchronized Object remove(final String key) {

		return singletons.remove(key);
	}

	public static synchronized <T, E extends Exception> T get(
			final String key,
			final ThrowingSupplier<T, E> supplier) throws E {

		final T t = (T)singletons.get(key);
		if (t == null) {
			final T s = supplier.get();
			singletons.put(key, s);
			return s;
		} else return t;
	}

	public static synchronized void clear() {

		singletons.clear();
	}

	public static void main(final String... args) throws IOException {

		run(args);
	}


	public static <T extends NativeType<T>> void run(final String... args) throws IOException {

		final String url = "https://janelia-cosem.s3.amazonaws.com/jrc_hela-2/jrc_hela-2.n5";
		final String dataset = "/em/fibsem-uint16/s4";
		final N5Reader n5 = Singleton.get(
				url,
				() -> new N5Factory().openReader(url));

		final RandomAccessibleInterval<T> img = Singleton.get(
				url + ":" + dataset,
				() -> (RandomAccessibleInterval<T>)N5Utils.open(n5, dataset));
	}
}
