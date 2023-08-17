package bigwarp.transforms;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5DisplacementField;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformationAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

public class NgffTransformations
{

	public static void main( final String[] args ) throws Exception
	{
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

//	public static TransformGraph openGraph( final N5Reader n5 )
//	{
//		return openGraph( n5, "/" );
//	}
//
//	public static TransformGraph openGraph( final N5Reader n5, final String dataset )
//	{
//		return new TransformGraph( n5, dataset );
//	}
//
//	public static RealTransform open( final N5Reader n5, final String dataset )
//	{
//		// TODO error handling
//		return openGraph( n5, dataset ).getTransforms().get( 0 ).getTransform( n5 );
//	}
//
//	public static RealTransform open( final N5Reader n5, final String dataset, final String name )
//	{
//		// TODO error handling
//		return openGraph( n5, dataset ).getTransform( name ).get().getTransform( n5 );
//	}
//
	public static < T extends RealTransform> T open( final N5Reader n5, final String dataset, final String input, final String output )
	{
		// TODO error handling
		final TransformGraph g = openGraph( n5, dataset );
		return (T)g.path( input, output ).get().totalTransform( n5, g );
	}

	public static RealTransform open( final String url )
	{
		final Pair< NgffCoordinateTransformation< ? >, N5Reader > pair = openTransformN5( url );
		return pair.getA().getTransform( pair.getB() );
	}

	public static InvertibleRealTransform openInvertible( final String url )
	{
		final Pair< NgffCoordinateTransformation< ? >, N5Reader > pair = openTransformN5( url );
		return pair.getA().getInvertibleTransform( pair.getB() );
	}

	public static Pair<NgffCoordinateTransformation<?>,N5Reader> openTransformN5( final String url )
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

				final CoordinateTransformation ct = n5.getAttribute( dataset, attribute, CoordinateTransformation.class );
				final NgffCoordinateTransformation< ? > nct = NgffCoordinateTransformation.create( ct );
				return new ValuePair<>( nct, n5 );
			}
		}
		catch ( URISyntaxException | N5Exception e )
		{
			e.printStackTrace();
			return null;
		}
	}

	public static NgffCoordinateTransformation openJson( final String url )
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
		final CoordinateTransformation ct = gson.fromJson( elem, CoordinateTransformation.class );
//		System.out.println( ct );

		final CoordinateTransformation< ? > nct = CoordinateTransformation.create( ct );
		return nct;
//		final RealTransform tform = nct.getTransform( null );
//		System.out.println( tform );
//
//		return tform;
	}

	public static void save( final String jsonFile, final CoordinateTransformation<?> transform )
	{
		final GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter(CoordinateTransformation.class, new CoordinateTransformationAdapter() );
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

	public static < T extends NativeType< T > & RealType< T > > NgffDisplacementsTransformation save(
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
			final int nThreads ) throws IOException
	{
		final String[] axisNames = ( spacing.length == 2 ) ? new String[] { "x", "y" } : new String[] { "x", "y", "z"};
		final CoordinateSystem inputCoordinates = new CoordinateSystem( inName, Axis.space( unit, axisNames ) );
		final CoordinateSystem outputCoordinates = new CoordinateSystem( outName, Axis.space( unit, axisNames ) );

		final ThreadPoolExecutor threadPool = new ThreadPoolExecutor( nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>()	);
		N5DisplacementField.saveDisplacementFieldNgff( n5, dataset, "/", inputCoordinates, outputCoordinates,
				dfield, spacing, offset, blockSize, compression, threadPool );

		final NgffDisplacementsTransformation ngffDfield = new NgffDisplacementsTransformation( dataset, "linear" );
		return ngffDfield;
//		N5DisplacementField.addCoordinateTransformations( n5, "/", ngffDfield );
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

		if ( axes[ n - 1 ].getType().equals( Axis.DISPLACEMENT_TYPE ))
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
				if ( axes[ i ].getType().equals( Axis.DISPLACEMENT_TYPE ))
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
		final TransformGraph g = openGraph( n5, dataset );

		// TODO need to be smarter about which coordinate system to get
		final CoordinateSystem cs = g.getCoordinateSystems().iterator().next();
		return vectorAxisLastNgff( cs );
	}

	public static GsonBuilder gsonBuilder()
	{
		final GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter(CoordinateTransformation.class, new CoordinateTransformationAdapter() );
		return gb;
	}

}
