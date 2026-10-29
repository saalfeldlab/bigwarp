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
package bigwarp.transforms;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.codec.BlockCodecInfo;
import org.janelia.saalfeldlab.n5.codec.DatasetCodecInfo;
//import org.janelia.saalfeldlab.n5.codec.dataset.CastValueCodecInfo.Rounding;
//import org.janelia.saalfeldlab.n5.codec.dataset.ScaleOffsetCastCodecInfo;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffSceneParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffSceneWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.scene.NgffScene;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.Common;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.graph.TransformGraph;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.AbstractAffineCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.AffineCoordinateTransformAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.CoordinateFieldCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.CoordinateTransformAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.DisplacementFieldCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.FieldTransformParsing;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.IdentityCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.InverseCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.InvertibleAffineCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.ParameterBinding;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.InvertibleCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.ParametrizedTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.ReferencedCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.ScaleCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.SequenceCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.TranslationCoordinateTransform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import bdv.gui.ExportDisplacementFieldFrame.DTYPE;
import bigwarp.BigWarpInit;
import bigwarp.source.SourceInfo;
import bigwarp.transforms.metadata.N5TransformMetadata;
import bigwarp.transforms.metadata.N5TransformMetadataParser;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
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
	public static final String inverseSuffix = "<inverse>";

	/**
	 * An attribute path naming one transform of an array, e.g. "coordinateTransformations[1]"
	 * gives the key "coordinateTransformations" and index 1.
	 */
	private static final Pattern INDEXED_ATTRIBUTE = Pattern.compile( "^/?(.+)\\[(\\d+)\\]$" );

	/**
	 * The dataset a coordinate or displacement field array is written to, relative to
	 * its multiscales group. Fields are single-resolution, so there is only ever one.
	 */
	public static final String FIELD_SCALE_DATASET = "s0";

	public static void main( String[] args ) {

		final InvertibleAffineCoordinateTransform ct = InvertibleAffineCoordinateTransform.options()
				.name("name").input("inspace").output("outspace")
				.affine(TransformUtils.affineToMatrix(new AffineTransform3D()))
				.build();

		final N5Writer n5 = new N5Factory()
				.options( opts -> { opts.gsonBuilder(gsonBuilder()); })
				.openWriter("/home/john/tests/bw-tform-graph/fly-templates.n5");
		final String dset = "transforms/test";

		addCoordinateTransformations(n5, dset, ct);
	}
	
	public enum TransformField {
		DISPLACEMENT, COORDINATE
	};

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

	public static Pair<String,Boolean> isInverse(String uri){

		if(uri.endsWith(inverseSuffix)) {
			return new ValuePair<>(uri.substring(0, uri.length() - inverseSuffix.length()), true);
		}
		return new ValuePair<>(uri, false);
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
			// TODO generalize input to use path
			final String nrmInput = N5URI.normalizeGroupPath(ct.getInput().getName());
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
			// TODO generalize input to use path
			final String nrmInput = N5URI.normalizeGroupPath(ct.getInput().getName());
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
			final N5Reader n5 = new N5Factory().gsonBuilder(BigWarpInit.gsonBuilder(true)).openReader(loc);
			return openTransformN5(n5, url);
		} catch (final URISyntaxException e) {}

		return null;
	}

	public static Pair<CoordinateTransform<?>,N5Reader> openTransformN5(final N5Reader n5, final String fullUrl ) {

		if( fullUrl == null )
			return null;

		try
		{
			final Pair<String, Boolean> urlInverse = NgffTransformations.isInverse(fullUrl);
			final String url = urlInverse.getA();
			final boolean isInverse = urlInverse.getB();

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
//					final SpacesTransforms st = SpacesTransforms.deserializeSingle(n5, dataset, "ome", attribute);

					// the attribute path selects the transform: its key and index into that array.
					// An empty attribute searches the default keys and takes the first transform.
					final N5TransformMetadataParser parser;
					final int index;
					final Matcher m = INDEXED_ATTRIBUTE.matcher( attribute == null ? "" : attribute );
					if( m.matches() ) {
						parser = new N5TransformMetadataParser( m.group( 1 ) );
						index = Integer.parseInt( m.group( 2 ) );
					} else if( attribute == null || attribute.isEmpty() || attribute.equals( "/" ) ) {
						parser = new N5TransformMetadataParser();
						index = 0;
					} else {
						parser = new N5TransformMetadataParser( attribute.replaceFirst( "^/", "" ) );
						index = 0;
					}

					final Optional<N5TransformMetadata> tformMeta = parser.parseMetadata(n5, dataset);
					if( tformMeta.isEmpty() || index >= tformMeta.get().getTransforms().length )
						return null;

					CoordinateTransform<?> ct = tformMeta.get().getTransforms()[index];
					if( isInverse ) {

						if(ct instanceof InvertibleCoordinateTransform) {
							ct = new InverseCoordinateTransform((InvertibleCoordinateTransform)ct);
						}
						else
							System.err.println("Inverse of non-invertible transform requested: " + fullUrl +
									"\nReturning transform directly.");
					}

					// resolve parameter paths against this group, including those of
					// transforms nested in a sequence or inverse
					ParameterBinding.bind(ct, n5, dataset);
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
			((ParametrizedTransform)ct).resolveAbsolutePath(groupPath);
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
		// TODO add source and dest coordinate systems

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

		final Gson gson = BigWarpInit.gsonBuilder(true).create();
		final JsonElement elem = gson.fromJson( string, JsonElement.class );

		final CoordinateTransform<?> ct = gson.fromJson( elem, CoordinateTransform.class );
		if( ct != null )
		{
			final CoordinateTransform< ? > nct = ct.copy();
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
		return save( n5, dataset, dfield, inName, outName, spacing, offset, unit, blockSize, compression,
				DTYPE.FLOAT, Double.MAX_VALUE, nThreads );
	}

	/**
	 * As {@link #save(N5Writer, String, RandomAccessibleInterval, String, String, double[], double[], String, int[], Compression, int)},
	 * but stores the field quantized when {@code dtype} is an integral type.
	 *
	 * @param dtype
	 *            the type the field is stored as; {@link DTYPE#BYTE} and
	 *            {@link DTYPE#SHORT} quantize, the floating point types do not
	 * @param maxError
	 *            the maximum permitted magnitude of the quantization error, in
	 *            physical units; ignored for the floating point types
	 */
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
			final DTYPE dtype,
			final double maxError,
			final int nThreads )
	{
		final String[] axisNames = ( spacing.length == 2 ) ? new String[] { "x", "y" } : new String[] { "x", "y", "z"};
		final CoordinateSystem inputCoordinates = new CoordinateSystem( inName, Axis.space( unit, axisNames ) );
		final CoordinateSystem outputCoordinates = new CoordinateSystem( outName, Axis.space( unit, axisNames ) );

		// the vector dimension is not one of the field's spatial dimensions
		final DatasetCodecInfo[] codecs = quantizationCodecs( dtype, maxError, dfield.numDimensions() - 1 );

		final ThreadPoolExecutor threadPool = new ThreadPoolExecutor( nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>()	);
		final DisplacementFieldCoordinateTransform<?> ngffDfield = saveDisplacementFieldNgff( n5, dataset, "/", inputCoordinates, outputCoordinates,
				dfield, spacing, offset, blockSize, compression, codecs, threadPool );

		// N5Utils.save( ..., ExecutorService ) returns before its submitted block-write
		// tasks complete, so the pool must be joined here; otherwise the field array is
		// incompletely written when this method returns (missing blocks read back as
		// zeros), which is especially likely with a single-thread pool. See
		// bigwarp.dfield.N5UtilsExecutorSaveTest.
		threadPool.shutdown();
		try
		{
			threadPool.awaitTermination( Long.MAX_VALUE, TimeUnit.MILLISECONDS );
		}
		catch ( final InterruptedException e )
		{
			Thread.currentThread().interrupt();
			throw new RuntimeException( "Interrupted while writing displacement field to " + dataset, e );
		}

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

	/**
	 * The path with which a source is referenced from the scene at the given location.
	 * <p>
	 * This is the source's path relative to the scene's group if the source is NGFF
	 * (i.e. its coordinate system is known) and lies under the scene's group.
	 * Otherwise, it is the source's absolute location.
	 *
	 * @param info the source
	 * @param sceneContainer the container the scene is written to
	 * @param sceneGroup the group in that container the scene is written to
	 * @return the path, or null if the source has no location
	 */
	public static String scenePath( final SourceInfo info, final String sceneContainer, final String sceneGroup )
	{
		final String uri = info.getUri();
		if( uri == null )
			return null;

		if( info.getCoordinateSystem() == null )
			return uri;

		try
		{
			final N5URI src = new N5URI( N5URI.encodeAsUri( uri ).toString() );
			final URI srcContainer = normalizeContainer( src.getContainerPath() );
			final URI sceneCont = normalizeContainer( sceneContainer );
			if( srcContainer == null || sceneCont == null
					|| !Objects.equals( srcContainer.getScheme(), sceneCont.getScheme() )
					|| !Objects.equals( srcContainer.getAuthority(), sceneCont.getAuthority() ) )
				return uri;

			final String srcFull = joinPath( srcContainer.getPath(), normalizeGroup( src.getGroupPath() ) );
			final String sceneFull = joinPath( sceneCont.getPath(), normalizeGroup( sceneGroup ) );

			if( srcFull.startsWith( sceneFull + "/" ) )
				return srcFull.substring( sceneFull.length() + 1 );
		}
		catch( final URISyntaxException | RuntimeException e )
		{
			// not parseable, treat it as an opaque location
		}
		return uri;
	}

	private static final Pattern URI_SCHEME = Pattern.compile( "^[a-zA-Z][a-zA-Z0-9+.-]*:.*" );

	/**
	 * @return an absolute URI with "." and ".." resolved and no trailing slash
	 */
	private static URI normalizeContainer( String container )
	{
		if( container == null )
			return null;

		// drop a leading storage format, e.g. "zarr:"
		container = container.replaceFirst( "^(n5|zarr|hdf5):(?=[a-zA-Z][a-zA-Z0-9+.-]*:)", "" );
		try
		{
			final URI uri;
			if( URI_SCHEME.matcher( container ).matches() && !container.matches( "^[a-zA-Z]:[\\\\/].*" ) )
				uri = N5URI.encodeAsUri( container ).normalize();
			else
				uri = Paths.get( container ).toAbsolutePath().normalize().toUri();

			final String path = uri.getPath() == null ? "" : uri.getPath().replaceAll( "/+$", "" );
			return new URI( uri.getScheme(), uri.getAuthority(), path, null, null );
		}
		catch( final URISyntaxException | RuntimeException e )
		{
			return null;
		}
	}

	/**
	 * @return the group path with "." and ".." resolved, no leading and no trailing slash
	 */
	private static String normalizeGroup( final String group )
	{
		final ArrayList< String > parts = new ArrayList<>();
		if( group != null )
		{
			for( final String part : group.split( "/" ) )
			{
				if( part.isEmpty() || part.equals( "." ) )
					continue;
				if( part.equals( ".." ) )
				{
					if( !parts.isEmpty() )
						parts.remove( parts.size() - 1 );
				}
				else
					parts.add( part );
			}
		}
		return String.join( "/", parts );
	}

	private static String joinPath( final String containerPath, final String group )
	{
		return group.isEmpty() ? containerPath : containerPath + "/" + group;
	}

	/**
	 * A reference to the given source for use as the input or output of a scene transform.
	 * The name is the coordinate system of an NGFF source, and is empty for any other source.
	 *
	 * @param info the source
	 * @param sceneContainer the container the scene is written to
	 * @param sceneGroup the group in that container the scene is written to
	 * @return the reference as a json object with fields "path" and "name"
	 */
	public static JsonObject sceneReference( final SourceInfo info, final String sceneContainer, final String sceneGroup )
	{
		final CoordinateSystem cs = info.getCoordinateSystem();
		final JsonObject ref = new JsonObject();
		ref.addProperty( "path", scenePath( info, sceneContainer, sceneGroup ) );
		ref.addProperty( "name", cs == null ? "" : cs.getName() );
		return ref;
	}

	/**
	 * Serializes a transform for a scene, with the given references as its input and output.
	 * The references of transforms nested inside it are left alone.
	 *
	 * @param ct the transform
	 * @param input the input reference
	 * @param output the output reference
	 * @param reverse true if parameters are stored in reversed order (zarr)
	 * @return the transform as json
	 */
	public static JsonObject sceneTransform( final CoordinateTransform< ? > ct, final JsonObject input, final JsonObject output, final boolean reverse )
	{
		final Gson gson = new OmeNgffSceneWriter( reverse ).gsonBuilder().create();
		final JsonObject json = gson.toJsonTree( ct, CoordinateTransform.class ).getAsJsonObject();
		json.add( "input", input );
		json.add( "output", output );
		return json;
	}

	/**
	 * Adds transforms to the scene at the given group, creating the scene if necessary.
	 * An existing transform with the same input, output and name is replaced, otherwise
	 * the new transform is appended. Everything else in the scene is left as is.
	 *
	 * @param n5 the writer
	 * @param group the group
	 * @param transforms the transforms, as produced by
	 *        {@link #sceneTransform(CoordinateTransform, JsonObject, JsonObject, boolean)}
	 */
	public static void addScene( final N5Writer n5, final String group, final JsonObject... transforms )
	{
		JsonObject scene = n5.getAttribute( group, NgffScene.SCENE_KEY, JsonObject.class );
		if( scene == null )
		{
			scene = new JsonObject();
			scene.add( "coordinateSystems", new JsonArray() );
		}

		final JsonArray cts = scene.has( "coordinateTransformations" ) && scene.get( "coordinateTransformations" ).isJsonArray()
				? scene.getAsJsonArray( "coordinateTransformations" )
				: new JsonArray();

		for( final JsonObject t : transforms )
		{
			int match = -1;
			for( int i = 0; i < cts.size(); i++ )
			{
				if( !cts.get( i ).isJsonObject() )
					continue;
				final JsonObject e = cts.get( i ).getAsJsonObject();
				if( Objects.equals( e.get( "input" ), t.get( "input" ) )
						&& Objects.equals( e.get( "output" ), t.get( "output" ) )
						&& Objects.equals( e.get( "name" ), t.get( "name" ) ) )
				{
					match = i;
					break;
				}
			}
			if( match >= 0 )
				cts.set( match, t );
			else
				cts.add( t );
		}
		scene.add( "coordinateTransformations", cts );

		if( !n5.exists( group ) )
			n5.createGroup( group );

		n5.setAttribute( group, NgffScene.SCENE_KEY, scene );
		if( n5.getAttribute( group, OmeNgffSceneParser.VERSION_KEY, String.class ) == null )
			n5.setAttribute( group, OmeNgffSceneParser.VERSION_KEY, OmeNgffSceneParser.VERSION );
	}

	/**
	 * Returns a message saying why a scene can not be written for the given sources, or null if it can.
	 *
	 * @param mvg the moving source
	 * @param tgt the target source
	 * @return the message or null
	 */
	public static String sceneRefusal( final SourceInfo mvg, final SourceInfo tgt )
	{
		if( mvg == null || tgt == null )
			return "NGFF scene not written: need both a moving and a target source";

		if( mvg.getUri() == null || tgt.getUri() == null )
			return "NGFF scene not written: a source has no location (was it saved?)";

		final CoordinateSystem mcs = mvg.getCoordinateSystem();
		final CoordinateSystem tcs = tgt.getCoordinateSystem();
		final String mname = mcs == null ? "" : mcs.getName();
		final String tname = tcs == null ? "" : tcs.getName();
		if( mvg.getUri().equals( tgt.getUri() ) && mname.equals( tname ) )
			return "NGFF scene not written: moving and target are the same image and coordinate system";

		return null;
	}

	public static final <T extends NativeType<T> & RealType<T>> InvertibleAffineCoordinateTransform saveAffine(
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

		final InvertibleAffineCoordinateTransform ct = InvertibleAffineCoordinateTransform.options()
				.path(metapath)
				.input(inputCoordinates != null ? inputCoordinates.getName() : null)
				.output(outputCoordinates != null ? outputCoordinates.getName() : null)
				.build();

		addCoordinateTransformations(n5Writer, metadataDataset, ct);

		return ct;
	}

	public static final <T extends NativeType<T> & RealType<T>> InvertibleAffineCoordinateTransform saveAffine(
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

		final InvertibleAffineCoordinateTransform ct = InvertibleAffineCoordinateTransform.options()
				.path(metapath)
				.input(inputCoordinates != null ? inputCoordinates.getName() : null)
				.output(outputCoordinates != null ? outputCoordinates.getName() : null)
				.build();

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

		return saveDisplacementFieldNgff( n5Writer, dataset, metadataDataset, inputCoordinates, outputCoordinates,
				field, spacing, offset, blockSize, compression, null, exec );
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
			final DatasetCodecInfo[] datasetCodecs,
			ExecutorService exec ) {

		return (DisplacementFieldCoordinateTransform<?>)saveFieldNgff( n5Writer, dataset, metadataDataset, inputCoordinates, outputCoordinates,
				TransformField.DISPLACEMENT, field, spacing, offset, blockSize, compression, datasetCodecs, exec );
	}

	/**
	 * The dataset codecs that store a displacement field quantized to an integral
	 * type, or {@code null} if the requested type is not quantized.
	 * <p>
	 * The scale matches {@code N5DisplacementField.saveQuantized}: to hold the error
	 * of the whole vector below {@code maxError}, each of the {@code nd} coordinates
	 * is quantized with a step of {@code m = 2 * sqrt(maxError^2 / nd)}, so the value
	 * stored is {@code round(displacement / m)}. Writing that as a
	 * {@code scale_offset_cast} codec rather than as an attribute keeps the array
	 * readable by anything that understands the codec, which an NGFF reader does and
	 * the {@code quantization_multiplier} attribute is not.
	 *
	 * @param dtype
	 *            the type to store the field as
	 * @param maxError
	 *            the maximum permitted magnitude of the quantization error
	 * @param nd
	 *            the number of spatial dimensions of the field
	 * @return the codecs, or null if {@code dtype} is a floating point type
	 */
	public static DatasetCodecInfo[] quantizationCodecs( final DTYPE dtype, final double maxError, final int nd ) {

		final DataType targetType;
		switch ( dtype )
		{
		case BYTE:
			targetType = DataType.INT8;
			break;
		case SHORT:
			targetType = DataType.INT16;
			break;
		default:
			return null;
		}

		final double m = 2 * Math.sqrt( maxError * maxError / nd );

		throw new N5Exception("unsupported quantization codec");
		// clamp bounds are left to their defaults: the full range of the target type
//		return new DatasetCodecInfo[] {
//				new ScaleOffsetCastCodecInfo( targetType, 1.0 / m, 0.0, null, null, Rounding.NEAREST_AWAY ) };
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

		return saveFieldNgff(n5Writer, dataset, metadataDataset, inputCoordinates, outputCoordinates, type,
				dfield, spacing, offset, blockSize, compression, null, exec);
	}

	/**
	 * As {@link #saveFieldNgff(N5Writer, String, String, CoordinateSystem, CoordinateSystem, TransformField, RandomAccessibleInterval, double[], double[], int[], Compression, ExecutorService)},
	 * but writes the field array through a chain of dataset ("array -&gt; array") codecs.
	 * <p>
	 * The field is still built and passed in at its real-valued type; the codecs
	 * describe how its blocks are transformed on the way to storage and back. This is
	 * how a quantized field is written: a
	 * {@code org.janelia.saalfeldlab.n5.codec.dataset.ScaleOffsetCastCodecInfo} narrows
	 * the displacements to an integral type on encode and restores them on decode, so
	 * readers still see real values.
	 *
	 * @param datasetCodecs
	 *            the dataset codecs, in encode order; may be null or empty, in which
	 *            case the field is written at its own data type
	 */
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
			final DatasetCodecInfo[] datasetCodecs,
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

		// the ome-zarr spec for displacement and position field transforms 
		// reverses the indexing order of coordinates relative to what we store for imglib2
		// as a result, we need to reverse the indexes of the vector components
		// when that is the case.
		final boolean reverse = OmeNgffMetadataParser.storesDimensionsReversed( n5Writer );
		final RandomAccessibleInterval<T> dfieldToSave = reverse ? FieldTransformParsing.reverseCoordinates(dfield) : dfield;

		// the field group holds the multiscales metadata; the array is its first (and
		// only) resolution level, as for any other ome-zarr multiscales image. A zarr
		// node is either a group or an array, so the two cannot share one node.
		final String arrayDataset = dataset + "/" + FIELD_SCALE_DATASET;
		n5Writer.createGroup( dataset );
		try
		{
			if ( datasetCodecs == null || datasetCodecs.length == 0 )
				N5Utils.save(dfieldToSave, n5Writer, arrayDataset, vecBlkSz, compression, exec);
			else
				save(dfieldToSave, n5Writer, arrayDataset, vecBlkSz, compression, datasetCodecs, exec);
		}
		catch ( final InterruptedException | ExecutionException e )
		{
			e.printStackTrace();
		}

		final String vecFieldCsName =  inputCoordinates.getName();
		final CoordinateSystem[] cs = new CoordinateSystem[] {
				createVectorFieldCoordinateSystem( vecFieldCsName, inputCoordinates, type ) };

		// the dataset transform's input is the array, referenced by its path relative
		// to the multiscales group
		final CoordinateTransform[] ct = new CoordinateTransform[] {
				createTransformation( "", spacing, offset, FIELD_SCALE_DATASET, cs[0] ) };

		// write the fields's coordinate system and transform in an OME-Zarr multiscales
		writeFieldMultiscales( n5Writer, dataset, cs, ct );

		if( type.equals( TransformField.DISPLACEMENT ))
			return DisplacementFieldCoordinateTransform.<T>options()
					.name( "" ).path( dataset ).interpolation( "linear" )
					.input( inputCoordinates.getName() ).output( outputCoordinates.getName() )
					.build();
		else
			return CoordinateFieldCoordinateTransform.<T>options()
					.name( "" ).path( dataset ).interpolation( "linear" )
					.input( inputCoordinates.getName() ).output( outputCoordinates.getName() )
					.build();
	}

	/**
	 * Writes {@code source} to a new dataset whose attributes carry the given dataset
	 * codecs.
	 * <p>
	 * The codecs have to be part of the dataset's attributes rather than applied to
	 * the data on the way in: they are what tells a reader how to decode the stored
	 * blocks. That is why this creates the dataset explicitly instead of letting
	 * {@code N5Utils.save} derive the attributes from the source alone. The dataset's
	 * own data type stays that of {@code source} - the type a reader gets back - while
	 * the codecs describe what is actually stored.
	 */
	private static <T extends NativeType<T>> void save(
			final RandomAccessibleInterval<T> source,
			final N5Writer n5,
			final String dataset,
			final int[] blockSize,
			final Compression compression,
			final DatasetCodecInfo[] datasetCodecs,
			final ExecutorService exec) throws InterruptedException, ExecutionException {

		final DatasetAttributes attributes = new DatasetAttributes(
				source.dimensionsAsLongArray(),
				blockSize,
				N5Utils.dataType(source.getType()),
				(BlockCodecInfo)null,
				datasetCodecs,
				compression);

		final DatasetAttributes writtenAttributes = n5.createDataset(dataset, attributes);
		final long[] gridOffset = new long[source.numDimensions()];

		N5Utils.saveBlock(source, n5, dataset, writtenAttributes, gridOffset, exec);
	}

	/**
	 * Writes NGFF multiscales metadata that
	 * holds the field's coordinate system(s) and its single dataset's
	 * pixel-&gt;physical transform(s). The one dataset entry references the field array
	 * as the child {@link #FIELD_SCALE_DATASET}, the same layout any other ome-zarr
	 * multiscales image uses.
	 * <p>
	 * The coordinate systems and transforms are passed in imglib2 (x,y,z) order;
	 * the reverse-aware {@link OmeNgffMetadataParser} reverses them for zarr.
	 *
	 * @param n5Writer the writer
	 * @param dataset the multiscales group holding the field array
	 * @param coordinateSystems the field coordinate system(s)
	 * @param datasetTransforms the array's pixel-&gt;physical transform(s)
	 */
	private static void writeFieldMultiscales(final N5Writer n5Writer, final String dataset,
			final CoordinateSystem[] coordinateSystems, final CoordinateTransform<?>[] datasetTransforms) {

		final OmeNgffMultiScaleMetadata.OmeNgffDataset dset = new OmeNgffMultiScaleMetadata.OmeNgffDataset();
		dset.path = FIELD_SCALE_DATASET;
		dset.coordinateTransformations = datasetTransforms;

		final int nd = coordinateSystems[0].getAxes().length;
		final OmeNgffMultiScaleMetadata ms = new OmeNgffMultiScaleMetadata(
				nd, "", null, null, "0.6", null,
				new OmeNgffMultiScaleMetadata.OmeNgffDataset[] { dset },
				coordinateSystems, null, null, null,
				new NgffSingleScaleAxesMetadata[ 0 ] );

		final OmeNgffMetadata meta = new OmeNgffMetadata( "", new OmeNgffMultiScaleMetadata[] { ms } );
		try {
			new OmeNgffMetadataParser( n5Writer ).writeMetadata( meta, n5Writer, dataset );
		} catch ( final Exception e ) {
			e.printStackTrace();
		}
	}


	/**
	 * Builds the pixel-&gt;physical transform for a multiscales dataset entry.
	 * <p>
	 * The spec requires a transform under {@code multiscales > datasets} to name its
	 * input by {@code path} - the array it applies to - and its output by {@code name},
	 * the coordinate system that array maps into.
	 *
	 * @param name the transform's name
	 * @param scale the pixel spacing
	 * @param offset the pixel offset
	 * @param dataset the array's path, relative to the multiscales group
	 * @param output the coordinate system the array maps into
	 */
	public static CoordinateTransform<?> createTransformation(final String name,
			final double[] scale, final double[] offset,
			final String dataset, final CoordinateSystem output) {

		final OmeNgffReference in = new OmeNgffReference(null, dataset);
		final OmeNgffReference out = new OmeNgffReference(output.getName(), null);

		CoordinateTransform<?> ct;
		if ((scale != null || allOnes(scale)) && (offset != null || allZeros(offset))) {
			ct = SequenceCoordinateTransform.options()
					.name(name).input(in).output(out)
					.transformations(
							new ScaleCoordinateTransform(prepend(1, scale)),
							new TranslationCoordinateTransform(prepend(0, offset)))
					.build();
		} else if (offset != null || !allZeros(offset))
			ct = TranslationCoordinateTransform.options()
					.name(name).input(in).output(out)
					.translation(prepend(0, offset))
					.build();
		else if (scale != null || !allOnes(scale))
			ct = ScaleCoordinateTransform.options()
					.name(name).input(in).output(out)
					.scale(prepend(1, scale))
					.build();
		else
			ct = IdentityCoordinateTransform.options()
					.name(name).input(in).output(out)
					.build();

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
		gb.registerTypeHierarchyAdapter(AbstractAffineCoordinateTransform.class, new AffineCoordinateTransformAdapter());
		gb.registerTypeAdapter(CoordinateTransform.class, new CoordinateTransformAdapter());
		return gb;
	}

	public static CoordinateTransform<?> createAffine(AffineGet transform) {

		if (transform instanceof TranslationGet) {
			return new TranslationCoordinateTransform(((TranslationGet)transform).getTranslationCopy());
		} else if (transform instanceof ScaleGet) {
			return new ScaleCoordinateTransform(((ScaleGet)transform).getScaleCopy());
		} else {
			return new InvertibleAffineCoordinateTransform(TransformUtils.affineToMatrix(transform));
		}
	}

}
