package bigwarp.transforms;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.Common;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.graph.TransformGraph;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.AffineCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.CoordinateTransformAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.DisplacementFieldCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.IdentityCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.InvertibleCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.ScaleCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.SequenceCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.TranslationCoordinateTransform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.ScaleGet;
import net.imglib2.realtransform.TranslationGet;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;


public class NgffTransformations
{

	public static void main( final String[] args ) throws Exception
	{
		// detect transformations
		final String loc = "/home/john/Desktop/dfield.n5";
		final N5URI uri = new N5URI(loc);

//		final CoordinateTransform<?>[] cts = detectTransforms(loc);
//		System.out.println(Arrays.toString(cts));

		System.out.println(detectTransforms(loc));

//		System.out.println( uri );
//		System.out.println( uri.getURI() );
//
//		final String grp = ( uri.getGroupPath() != null ) ? uri.getGroupPath() : "";
//		System.out.println( grp );
//
//		final String attr = ( uri.getAttributePath() != null ) ? uri.getAttributePath() : "";
//		System.out.println( attr );

//		final N5Reader n5 = new N5Factory().gsonBuilder( gsonBuilder() ).openReader( uri.getContainerPath() );
//		final JsonObject json = n5.getAttribute(grp, attr, JsonObject.class);
//		final String ver = n5.getAttribute(grp, "n5", String.class);
//		final JsonElement jcts = n5.getAttribute(grp, "coordinateTransformations", JsonElement.class);
//		final JsonElement jct = n5.getAttribute(grp, "coordinateTransformations[0]", JsonElement.class);
//		final CoordinateTransform<?> ct = n5.getAttribute(grp, "coordinateTransformations[0]", CoordinateTransform.class);
//		final CoordinateTransform<?>[] cts = n5.getAttribute(grp, "coordinateTransformations", CoordinateTransform[].class);

//		System.out.println("");
//		System.out.println(json);
//		System.out.println("");
//		System.out.println(ver);
//		System.out.println("");
//		System.out.println(jcts);
//		System.out.println("");
//		System.out.println(jct);
//		System.out.println("");
//		System.out.println(ct);
//		System.out.println(ct.getType());
//		System.out.println("");
//		System.out.println(Arrays.toString(cts));


//		openTransformN5( url );


		// full
//		final String bijPath = "/home/john/projects/ngff/dfieldTest/jrc18_example.n5?/#/coordinateTransformations[0]";

		// no dataset
//		final String bijPath = "/home/john/projects/ngff/dfieldTest/jrc18_example.n5#/coordinateTransformations[0]";

		// no dataset no attribute
//		final String bijPath = "/home/john/projects/ngff/dfieldTest/jrc18_example.n5";
//
//		final N5URI url = new N5URI( bijPath );
//		System.out.println( url.getGroupPath());
//		System.out.println( url.getAttributePath());
//
//		final Pair< NgffCoordinateTransformation< ? >, N5Reader > bijN5 = openTransformN5( bijPath );
//		System.out.println( bijN5.getA() );
//		final InvertibleRealTransform bij = openInvertible( bijPath );
//		System.out.println( bij );


//		final String path = "/home/john/projects/ngff/dfieldTest/jrc18_example.n5";
//		final N5URL url = new N5URL( path );
//		System.out.println( url.getAttribute() );
//		System.out.println( url.getDataset());


//		final String bijPath = "/home/john/projects/ngff/dfieldTest/jrc18_example.n5?/#/coordinateTransformations[0]";
//		final N5URL url = new N5URL( bijPath );
//		final N5Reader n5 = new N5Factory().gsonBuilder( gsonBuilder() ).openReader( url.getLocation() );
//		final CoordinateTransformation ct = n5.getAttribute( url.getDataset(), url.getAttribute(), CoordinateTransformation.class );
//		System.out.println( ct );
//
//		final NgffCoordinateTransformation< ? > nct = NgffCoordinateTransformation.create( ct );
//		RealTransform tform = nct.getTransform( n5 );
//		System.out.println( tform );




//		final String basePath = "/home/john/projects/ngff/dfieldTest/jrc18_example.n5";
//		final String csPath = "?dfield#coordinateSystems/[0]";
//		final String namePath = "?dfield#coordinateSystems/[0]/name";
//		final String dimPath = "?dfield#/dimensions";
//
//		final N5URL baseUrl = new N5URL( basePath );
//		final N5URL nameUrl = baseUrl.getRelative( namePath );
//		final N5URL csUrl = baseUrl.getRelative( csPath );
//		final N5URL dimUrl = baseUrl.getRelative( dimPath );
//
//		final N5Reader n5 = new N5Factory().gsonBuilder( gsonBuilder() ).openReader( baseUrl.getLocation() );
//		final String name = n5.getAttribute( nameUrl.getDataset(), nameUrl.getAttribute(), String.class );
//		final CoordinateSystem cs = n5.getAttribute( csUrl.getDataset(), csUrl.getAttribute(), CoordinateSystem.class );
//		final long[] dims = n5.getAttribute( dimUrl.getDataset(), dimUrl.getAttribute(), long[].class );
//
//		System.out.println( name );
//		System.out.println( cs );
//		System.out.println( cs.getAxes()[0].getName() );
//		System.out.println( Arrays.toString( dims ) );




////		final String path = "/home/john/projects/ngff/dfieldTest/dfield.n5";
//		final String path = "/home/john/projects/ngff/dfieldTest/jrc18_example.n5";
//
//		final String dataset = "/";
////		final String dataset = "coordinateTransformations";
////		final String dataset = "/dfield";
//
//		final N5FSReader n5 = new N5FSReader( path, gsonBuilder() );
//
////		RealTransform dfieldTform = open( n5, dataset );
////		System.out.println( dfieldTform );
//
////		RealTransform dfieldTform = open( n5, dataset );
////		System.out.println( dfieldTform );
//
//		TransformGraph g = openGraph( n5, dataset );
//		g.printSummary();
//		RealTransform fwdXfm = g.path( "jrc18F", "fcwb" ).get().totalTransform( n5, g );
//		RealTransform invXfm = g.path( "fcwb", "jrc18F" ).get().totalTransform( n5, g );
//		System.out.println( fwdXfm );
//		System.out.println( invXfm );


//		ArrayImg< IntType, IntArray > img = ArrayImgs.ints( 2, 3, 4, 5 );
//
//		int[] p = vectorAxisLastNgff( n5, dataset );
//		System.out.println( Arrays.toString( p ));
//		System.out.println( "" );
//
//		IntervalView< IntType > imgP = N5DisplacementField.permute( img, p );
//		System.out.println( Intervals.toString( imgP ));


//		try
//		{
////			AffineGet p2p = N5DisplacementField.openPixelToPhysicalNgff( n5, "transform", true );
////			System.out.println( p2p );
//
////			int[] indexes = new int[] {1, 2, 3 };
////			AffineGet sp2p = TransformUtils.subAffine( p2p, indexes );
////			System.out.println( sp2p );
//		}
//		catch ( Exception e )
//		{
//			e.printStackTrace();
//		}

	}

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

	public static InvertibleRealTransform openInvertible(final String url) {

		final Pair<CoordinateTransform<?>, N5Reader> pair = openTransformN5(url);
		final CoordinateTransform<?> ct = pair.getA();
		if (ct instanceof InvertibleCoordinateTransform)
			return ((InvertibleCoordinateTransform<?>)ct).getInvertibleTransform(pair.getB());
		else
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
		N5URI uri;
		try {
			uri = new N5URI(url);
		} catch (final URISyntaxException e) {
			return null;
		}

		final String grp = ( uri.getGroupPath() != null ) ? uri.getGroupPath() : "";
		final String attr = ( uri.getAttributePath() != null && !uri.getAttributePath().equals("/")) ? uri.getAttributePath() : "coordinateTransformations";

		final N5Reader n5;
		try {

			n5 = new N5Factory().gsonBuilder(gsonBuilder()).openReader(uri.getContainerPath());
			final CoordinateTransform<?>[] cts = n5.getAttribute(grp, attr, CoordinateTransform[].class);

			if (cts != null && cts.length > 0)
				try {
					return N5URI.from(uri.getContainerPath(), grp, "coordinateTransformations[0]").toString();
				} catch (final URISyntaxException e) {}

		} catch (final N5Exception e) {}

		return null;
	}

	public static Pair<CoordinateTransform<?>,N5Reader> openTransformN5( final String url )
	{
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
				final N5Reader n5 = new N5Factory().gsonBuilder( gsonBuilder() ).openReader( loc );
				final String dataset = n5url.getGroupPath() != null ? n5url.getGroupPath() : "/";
				final String attribute = n5url.getAttributePath() != null ? n5url.getAttributePath() : "coordinateTransformations/[0]";

				final CoordinateTransform<?> ct = n5.getAttribute(dataset, attribute, CoordinateTransform.class);
				final CoordinateTransform<?> nct = CoordinateTransform.create(ct);
				return new ValuePair<>( nct, n5 );
			}
		}
		catch ( URISyntaxException | N5Exception e )
		{
			e.printStackTrace();
			return null;
		}
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

		final Gson gson = gsonBuilder().create();
		final JsonElement elem = gson.fromJson( string, JsonElement.class );
//		System.out.println( elem );

//		final CoordinateTransformation ct = gson.fromJson( elem.getAsJsonArray().get( 0 ), CoordinateTransformation.class );
		final CoordinateTransform<?> ct = gson.fromJson( elem, CoordinateTransform.class );
//		System.out.println( ct );

		final CoordinateTransform< ? > nct = CoordinateTransform.create( ct );
		return nct;
//		final RealTransform tform = nct.getTransform( null );
//		System.out.println( tform );
//
//		return tform;
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

//		final DisplacementFieldCoordinateTransform ngffDfield = new DisplacementFieldCoordinateTransform( "", dataset, "linear" );
//		return ngffDfield;
//		N5DisplacementField.addCoordinateTransformations( n5, "/", ngffDfield );
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
		n5.setAttribute(groupPath, CoordinateTransform.KEY, ctsOut);
	}

	public static final <T extends NativeType<T> & RealType<T>> DisplacementFieldCoordinateTransform<?> saveDisplacementFieldNgff(
			final N5Writer n5Writer,
			final String dataset,
			final String metadataDataset,
			final CoordinateSystem inputCoordinates,
			final CoordinateSystem outputCoordinates,
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
		catch ( final InterruptedException e )
		{
			e.printStackTrace();
		}
		catch ( final ExecutionException e )
		{
			e.printStackTrace();
		}

		final String vecFieldCsName =  inputCoordinates.getName() + "_dfield";
		final CoordinateSystem[] cs = new CoordinateSystem[] {
				createVectorFieldCoordinateSystem( vecFieldCsName, inputCoordinates ) };
		n5Writer.setAttribute(dataset, CoordinateSystem.KEY, cs);

		final CoordinateTransform[] ct = new CoordinateTransform[] {
				createTransformation( "", spacing, offset, dataset, cs[0] ) };
		n5Writer.setAttribute(dataset, CoordinateTransform.KEY, ct );

		return new DisplacementFieldCoordinateTransform( "", dataset, "linear" );
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

	public static CoordinateSystem createVectorFieldCoordinateSystem(final String name, final CoordinateSystem input) {

		final Axis[] vecAxes = new Axis[input.getAxes().length + 1];
		vecAxes[0] = new Axis("d", "displacement", null, true);
		for (int i = 1; i < vecAxes.length; i++)
			vecAxes[i] = input.getAxes()[i - 1];

		return new CoordinateSystem(name, vecAxes);
	}


	/**
	 * returns null if no permutation needed
	 *
	 * @param cs
	 * @return a permutation if needed
	 * @throws Exception
	 */
	public static final int[] vectorAxisLastNgff( final CoordinateSystem cs ) throws Exception {

		final Axis[] axes = cs.getAxes();
		final int n = axes.length;

		if ( axes[ n - 1 ].getType().equals( Axis.DISPLACEMENT))
			return null;
		else
		{
			int vecDim = -1;
//			for( int i = 0; i < n; i++ )
//				{
//					vecDim = i;
//					break;
//				}
//
//			if( vecDim < 0 )
//				return null;

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

	/**
	 * @throws Exception the exception
	 */
	public static final int[] vectorAxisLastNgff(
			final N5Reader n5, final String dataset ) throws Exception {

		// TODO move to somewhere more central
		final TransformGraph g = Common.openGraph( n5, dataset );

		// TODO need to be smarter about which coordinate system to get
		final CoordinateSystem cs = g.getCoordinateSystems().getCollection().iterator().next();
		return vectorAxisLastNgff( cs );
	}

	public static GsonBuilder gsonBuilder() {

		final GsonBuilder gb = new GsonBuilder();
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
