package bigwarp.transforms;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.Common;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.TransformGraph;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.AffineCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.AffineCoordinateTransformAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateFieldCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransformAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.DisplacementFieldCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.IdentityCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.InvertibleCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.ParametrizedTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.ReferencedCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.ScaleCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.SequenceCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.TranslationCoordinateTransform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import bigwarp.BigWarpInit;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.ScaleGet;
import net.imglib2.realtransform.TranslationGet;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;


public class NgffTransformations
{

	public enum TransformField {
		DISPLACEMENT, COORDINATE
	};

	@SuppressWarnings("unchecked")
	public static < T extends RealTransform> T open( final N5Reader n5, final String dataset, final String input, final String output )
	{
		// TODO error handling
		final TransformGraph g = Common.openGraph( n5, dataset );
		return (T)g.path( input, output ).get().totalTransform( n5, g );
	}

	public static RealTransform open( final String url )
	{
		final Pair< CoordinateTransform< ? >, N5Reader > pair = openTransformN5( url );
		return pair.getA().getTransform( pair.getB() );
	}

	public static RealTransform open( final N5Reader n5, final String url )
	{
		final Pair< CoordinateTransform< ? >, N5Reader > pair = openTransformN5( n5, url );
		return pair.getA().getTransform( pair.getB() );
	}

	public static InvertibleRealTransform openInvertible(final String url) {

		final Pair<CoordinateTransform<?>, N5Reader> pair = openTransformN5(url);
		final CoordinateTransform<?> ct = pair.getA();
		if (ct instanceof InvertibleCoordinateTransform)
			return ((InvertibleCoordinateTransform<?>)ct).getInvertibleTransform(pair.getB());
		else
			return null;
	}

	public static RealTransform findFieldTransformFirst(final N5Reader n5, final String group) {

		final String normGrp = N5URI.normalizeGroupPath(group);
		final CoordinateTransform<?>[] transforms = n5.getAttribute(group, CoordinateTransform.KEY, CoordinateTransform[].class);
		if (transforms == null)
			return null;

		if( transforms.length == 1 )
		{
			// TODO 
		}

		boolean found = false;
		for (final CoordinateTransform<?> ct : transforms) {
			System.out.println(ct);
			final String nrmInput = N5URI.normalizeGroupPath(ct.getInput());
			if (nrmInput.equals(normGrp)) {
				found = true;
			}

		}

		return null;
	}

	public static RealTransform findFieldTransformStrict(final N5Reader n5, final String group, final String output ) {

		final String normGrp = N5URI.normalizeGroupPath(group);

		final CoordinateTransform<?>[] transforms = n5.getAttribute(group, CoordinateTransform.KEY, CoordinateTransform[].class);
		if (transforms == null)
			return null;

		final boolean found = false;
		for (final CoordinateTransform<?> ct : transforms) {
			System.out.println(ct);
			final String nrmInput = N5URI.normalizeGroupPath(ct.getInput());
			if (nrmInput.equals(normGrp) && ct.getOutput().equals(output) ) {
				return ct.getTransform(n5);
			}
		}
		return null;
	}

	/**
	 * Finds a candidate transformation in the n5 attributes at the given url and returns the
	 * complete URI for that transformation if found, otherwise null.
	 *
	 * @param url the base url
	 * @return the complete n5uri for a transformation, or null
	 */
	public static String detectTransforms( final String url )
	{
		// detect transformations
		final N5URI uri;
		try {
			uri = new N5URI(url.trim());
		} catch (final URISyntaxException e) {
			return null;
		}

		if( isValidTransformUri( url ))
			return url;

		if (!uri.getAttributePath().equals("coordinateTransformations[0]")) {

			String defaultUri;
			try {
				defaultUri = N5URI.from(uri.getContainerPath(), uri.getGroupPath(), "coordinateTransformations[0]").toString();
				if( isValidTransformUri( defaultUri ))
					return defaultUri;
			} catch (final URISyntaxException e) { }
		}

//		final N5Reader n5 = new N5Factory().gsonBuilder(gsonBuilder()).openReader(uri.getContainerPath());
//		final CoordinateTransform<?>[] cts = n5.getAttribute(grp, attr, CoordinateTransform[].class);
//
//		if (cts != null && cts.length > 0)
//			try {
//				return N5URI.from(uri.getContainerPath(), grp, "coordinateTransformations[0]").toString();
//			} catch (final URISyntaxException e) {}


		return null;
	}

	private static boolean isValidTransformUri(final String uri) {

		final Pair<CoordinateTransform<?>, N5Reader> out = openTransformN5(uri.trim());
		if (out != null && out.getA() != null)
			return true;

		return false;
	}

	public static Pair<CoordinateTransform<?>,N5Reader> openTransformN5( final String url ) {

		try {
			final N5URI n5url = new N5URI(url);
			final String loc = n5url.getContainerPath();
			final N5Reader n5 = new N5Factory().gsonBuilder(BigWarpInit.gsonBuilder()).openReader(loc);
			return openTransformN5(n5, url);
		} catch (final URISyntaxException e) {}

		return null;
	}

	public static Pair<CoordinateTransform<?>,N5Reader> openTransformN5( final N5Reader n5, final String url )
	{
		if( url == null )
			return null;

		try
		{
			final N5URI n5url = new N5URI( url );
			final String loc = n5url.getContainerPath();
			if( loc.endsWith( ".json" ))
			{
				return new ValuePair<>( openJson( url ), null);
			}
			else
			{
				final String dataset = n5url.getGroupPath() != null ? n5url.getGroupPath() : "/";

				final String attribute = n5url.getAttributePath();
				try {
					final CoordinateTransform<?> ct = n5.getAttribute(dataset, attribute, CoordinateTransform.class);
					resolveAbsolutePath(ct, dataset);
					return new ValuePair<>( ct, n5 );
				} catch (N5Exception | ClassCastException e) {}

				try {
					return openReference( url, n5, dataset, attribute ); // try to open a reference
				} catch( N5Exception | ClassCastException e ) {}
			}
		}
		catch ( final URISyntaxException e ) { }

		return null;
	}

	@SuppressWarnings("rawtypes")
	public static void resolveAbsolutePath( CoordinateTransform<?> ct, final String groupPath )
	{
		if( ct instanceof ParametrizedTransform )
			((ParametrizedTransform)ct).resolveAbsoluePath(groupPath);
	}

	public static Pair<CoordinateTransform<?>,N5Reader> openReference( final String url, final N5Reader n5, final String dataset, final String attribute) {

		final ReferencedCoordinateTransform ref = n5.getAttribute(dataset, attribute, ReferencedCoordinateTransform.class);
		if( ref == null )
			return null;
		else if( url != null && url.equals( ref.getUrl() ))
			return null; // avoid self-reference
		else
			return openTransformN5( ref.getUrl());
	}

	public static CoordinateTransform<?> openJson( final String url )
	{
		final Path path = Paths.get( url );
		String string;
		try
		{
			string = new String(Files.readAllBytes(path));
		}
		catch ( final IOException e )
		{
			return null;
		}

		final Gson gson = BigWarpInit.gsonBuilder().create();
		final JsonElement elem = gson.fromJson( string, JsonElement.class );

		final CoordinateTransform<?> ct = gson.fromJson( elem, CoordinateTransform.class );
		if( ct != null )
		{
			final CoordinateTransform< ? > nct = CoordinateTransform.create( ct );
			return nct;
		} else if( elem.isJsonObject()) {
			// TODO figure out what should be returned here
			final String refUrl = elem.getAsJsonObject().get("url").getAsString();
			if( url.equals( refUrl ))
				return null; //avoid self-reference
			else
				return openTransformN5( refUrl ).getA();
		}
		return null;
	}

	public static void save( final String jsonFile, final CoordinateTransform<?> transform )
	{
		final GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter(CoordinateTransform.class, new CoordinateTransformAdapter() );
		final Gson gson = gb.create();
		try( FileWriter writer = new FileWriter( jsonFile ))
		{
			gson.toJson( transform, writer );
			writer.close();
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
	}

	public static < T extends NativeType< T > & RealType< T > > DisplacementFieldCoordinateTransform<?> save(
			final N5Writer n5,
			final String dataset,
			final RandomAccessibleInterval< T > dfield,
			final String inName,
			final String outName,
			final double[] spacing,
			final double[] offset,
			final String unit,
			final int[] blockSize,
			final Compression compression,
			final int nThreads )
	{
		final String[] axisNames = ( spacing.length == 2 ) ? new String[] { "x", "y" } : new String[] { "x", "y", "z"};
		final CoordinateSystem inputCoordinates = new CoordinateSystem( inName, Axis.space( unit, axisNames ) );
		final CoordinateSystem outputCoordinates = new CoordinateSystem( outName, Axis.space( unit, axisNames ) );

		final ThreadPoolExecutor threadPool = new ThreadPoolExecutor( nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>()	);
		final DisplacementFieldCoordinateTransform<?> ngffDfield = saveDisplacementFieldNgff( n5, dataset, "/", inputCoordinates, outputCoordinates,
				dfield, spacing, offset, blockSize, compression, threadPool );

		return ngffDfield;
	}

	public static void addCoordinateTransformations( final N5Writer n5, final String groupPath, final CoordinateTransform<?> transform ) {

		final CoordinateTransform<?>[] cts = n5.getAttribute(groupPath, CoordinateTransform.KEY, CoordinateTransform[].class);
		final CoordinateTransform<?>[] ctsOut;
		if (cts == null)
			ctsOut = new CoordinateTransform[] { transform };
		else
		{
			ctsOut = new CoordinateTransform[cts.length + 1];
			System.arraycopy(cts, 0, ctsOut, 0, cts.length);
			ctsOut[ ctsOut.length - 1 ] = transform;
		}

		if( !n5.exists(groupPath))
			n5.createGroup(groupPath);

		n5.setAttribute(groupPath, CoordinateTransform.KEY, ctsOut);
	}

	public static final <T extends NativeType<T> & RealType<T>> AffineCoordinateTransform saveAffine(
			final N5Writer n5Writer,
			final String dataset,
			final String metadataDataset,
			final CoordinateSystem inputCoordinates,
			final CoordinateSystem outputCoordinates,
			final AffineGet affine,
			final boolean flat,
			final Compression compression ) {

		if( flat )
			return saveAffine( n5Writer, dataset, metadataDataset, inputCoordinates, outputCoordinates, affine.getRowPackedCopy(), compression );

		final int rows = affine.numTargetDimensions();
		final int columns = affine.numSourceDimensions() + 1;

		// the matrix is stored row major (columns are contiguous in memory)
		final int[] blockSize = new int[]{columns, rows};
		final ArrayImg<DoubleType, DoubleArray> data = ArrayImgs.doubles(affine.getRowPackedCopy(), columns, rows);
		N5Utils.save(data, n5Writer, dataset, blockSize, compression);

		// TODO make this more robust
		final String metapath;
		if (metadataDataset.equals(dataset))
			metapath = ".";
		else
			metapath = metadataDataset;

		final AffineCoordinateTransform ct = new AffineCoordinateTransform( null, metapath,
				inputCoordinates != null ? inputCoordinates.getName() : null,
				outputCoordinates != null ? outputCoordinates.getName() : null);

		addCoordinateTransformations(n5Writer, metadataDataset, ct);

		return ct;
	}

	public static final <T extends NativeType<T> & RealType<T>> AffineCoordinateTransform saveAffine(
			final N5Writer n5Writer,
			final String dataset,
			final String metadataDataset,
			final CoordinateSystem inputCoordinates,
			final CoordinateSystem outputCoordinates,
			final double[] affineParameters,
			final Compression compression ) {

		final int[] blockSize = new int[] { affineParameters.length };
		final ArrayImg<DoubleType, DoubleArray> data = ArrayImgs.doubles(affineParameters, affineParameters.length);
		N5Utils.save(data, n5Writer, dataset, blockSize, compression);

		// TODO make this more robust
		final String metapath;
		if (metadataDataset.equals(dataset))
			metapath = ".";
		else
			metapath = metadataDataset;

		final AffineCoordinateTransform ct = new AffineCoordinateTransform( null, metapath,
				inputCoordinates != null ? inputCoordinates.getName() : null,
				outputCoordinates != null ? outputCoordinates.getName() : null);

		addCoordinateTransformations(n5Writer, metadataDataset, ct);

		return ct;
	}

	public static final <T extends NativeType<T> & RealType<T>> DisplacementFieldCoordinateTransform<?> saveDisplacementFieldNgff(
			final N5Writer n5Writer,
			final String dataset,
			final String metadataDataset,
			final CoordinateSystem inputCoordinates,
			final CoordinateSystem outputCoordinates,
			final RandomAccessibleInterval<T> field,
			final double[] spacing,
			final double[] offset,
			final int[] blockSize,
			final Compression compression,
			ExecutorService exec ) {

		return (DisplacementFieldCoordinateTransform<?>)saveFieldNgff( n5Writer, dataset, metadataDataset, inputCoordinates, outputCoordinates,
				TransformField.DISPLACEMENT, field, spacing, offset, blockSize, compression, exec );
	}

	public static final <T extends NativeType<T> & RealType<T>> CoordinateFieldCoordinateTransform<?> savePositionFieldNgff(
			final N5Writer n5Writer,
			final String dataset,
			final String metadataDataset,
			final CoordinateSystem inputCoordinates,
			final CoordinateSystem outputCoordinates,
			final RandomAccessibleInterval<T> field,
			final double[] spacing,
			final double[] offset,
			final int[] blockSize,
			final Compression compression,
			ExecutorService exec ) {

		return (CoordinateFieldCoordinateTransform<?>)saveFieldNgff( n5Writer, dataset, metadataDataset, inputCoordinates, outputCoordinates,
				TransformField.COORDINATE, field, spacing, offset, blockSize, compression, exec );
	}

	public static final <T extends NativeType<T> & RealType<T>> CoordinateTransform<?> saveFieldNgff(
			final N5Writer n5Writer,
			final String dataset,
			final String metadataDataset,
			final CoordinateSystem inputCoordinates,
			final CoordinateSystem outputCoordinates,
			final TransformField type,
			final RandomAccessibleInterval<T> dfield,
			final double[] spacing,
			final double[] offset,
			final int[] blockSize,
			final Compression compression,
			ExecutorService exec ) {

		int[] vecBlkSz;
		if( blockSize.length >= dfield.numDimensions() )
			vecBlkSz = blockSize;
		else {
			vecBlkSz = new int[ blockSize.length + 1 ];
			vecBlkSz[ 0 ] = (int)dfield.dimension( 0 );
			for( int i = 1; i < vecBlkSz.length; i++ )
			{
				vecBlkSz[ i ] = blockSize[ i - 1 ];
			}
		}

		try
		{
			N5Utils.save(dfield, n5Writer, dataset, vecBlkSz, compression, exec);
		}
		catch ( final InterruptedException | ExecutionException e )
		{
			e.printStackTrace();
		}

		final String vecFieldCsName =  inputCoordinates.getName();
		final CoordinateSystem[] cs = new CoordinateSystem[] {
				createVectorFieldCoordinateSystem( vecFieldCsName, inputCoordinates, type ) };
		n5Writer.setAttribute(dataset, CoordinateSystem.KEY, cs);

		final CoordinateTransform[] ct = new CoordinateTransform[] {
				createTransformation( "", spacing, offset, dataset, cs[0] ) };
		n5Writer.setAttribute(dataset, CoordinateTransform.KEY, ct );

		if( type.equals( TransformField.DISPLACEMENT ))
			return new DisplacementFieldCoordinateTransform<T>( "", dataset, "linear",
					inputCoordinates.getName(), outputCoordinates.getName() );
		else
			return new CoordinateFieldCoordinateTransform<T>( "", dataset, "linear",
					inputCoordinates.getName(), outputCoordinates.getName() );
	}


	public static CoordinateTransform<?> createTransformation(final String name,
			final double[] scale, final double[] offset,
			final String dataset, final CoordinateSystem output) {

		CoordinateTransform<?> ct;
		if ((scale != null || allOnes(scale)) && (offset != null || allZeros(offset))) {
			ct = new SequenceCoordinateTransform(name, dataset, output.getName(),
					new CoordinateTransform[]{
							new ScaleCoordinateTransform(prepend(1, scale)),
							new TranslationCoordinateTransform(prepend(0, offset))});
		} else if (offset != null || !allZeros(offset))
			ct = new TranslationCoordinateTransform(name, dataset, output.getName(), prepend(0, offset));
		else if (scale != null || !allOnes(scale))
			ct = new ScaleCoordinateTransform(name, dataset, output.getName(), prepend(1, scale));
		else
			ct = new IdentityCoordinateTransform(name, dataset, output.getName());

		return ct;
	}

	private static boolean allZeros(final double[] x) {

		for (int i = 0; i < x.length; i++)
			if (x[i] != 0.0)
				return false;

		return true;
	}

	private static boolean allOnes(final double[] x) {

		for (int i = 0; i < x.length; i++)
			if (x[i] != 1.0)
				return false;

		return true;
	}

	private static double[] prepend(double val, double[] array) {

		final double[] out = new double[array.length + 1];
		out[0] = val;
		for (int i = 1; i < out.length; i++) {
			out[i] = array[i - 1];
		}
		return out;
	}

	public static CoordinateSystem createVectorFieldCoordinateSystem(final String name, final CoordinateSystem input, TransformField type ) {

		final Axis[] vecAxes = new Axis[input.getAxes().length + 1];
		if( type.equals(TransformField.DISPLACEMENT))
			vecAxes[0] = new Axis("displacement", "d", null, true);
		else
			vecAxes[0] = new Axis("coordinate", "c", null, true);

		for (int i = 1; i < vecAxes.length; i++)
			vecAxes[i] = input.getAxes()[i - 1];

		return new CoordinateSystem(name, vecAxes);
	}


	/**
	 * returns null if no permutation needed
	 *
	 * @param cs a coordinate system
	 * @return a permutation if needed
	 */
	public static final int[] vectorAxisLastNgff( final CoordinateSystem cs ) {

		final Axis[] axes = cs.getAxes();
		final int n = axes.length;

		if ( axes[ n - 1 ].getType().equals( Axis.DISPLACEMENT))
			return null;
		else
		{
			int vecDim = -1;
			final int[] permutation = new int[ n ];

			int k = 0;
			for( int i = 0; i < n; i++ )
			{
				if ( axes[ i ].getType().equals( Axis.DISPLACEMENT))
					vecDim = i;
				else
					permutation[i] = k++;
			}

			// did not find a matching axis
			if( vecDim < 0 )
				return null;

			permutation[vecDim] = n-1;
			return permutation;
		}
	}

	public static final int[] vectorAxisLastNgff(
			final N5Reader n5, final String dataset ) throws Exception {

		// TODO move to somewhere more central
		final TransformGraph g = Common.openGraph( n5, dataset );

		// TODO need to be smarter about which coordinate system to get
		final CoordinateSystem cs = g.getCoordinateSystems().getCollection().iterator().next();
		return vectorAxisLastNgff( cs );
	}

	@Deprecated
	public static GsonBuilder gsonBuilder() {

		final GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter(AffineCoordinateTransform.class, new AffineCoordinateTransformAdapter());
		gb.registerTypeAdapter(CoordinateTransform.class, new CoordinateTransformAdapter());
		return gb;
	}

	public static CoordinateTransform<?> createAffine(AffineGet transform) {

		if (transform instanceof TranslationGet) {
			return new TranslationCoordinateTransform(((TranslationGet)transform).getTranslationCopy());
		} else if (transform instanceof ScaleGet) {
			return new ScaleCoordinateTransform(((ScaleGet)transform).getScaleCopy());
		} else {
			return new AffineCoordinateTransform(transform.getRowPackedCopy());
		}
	}

}
