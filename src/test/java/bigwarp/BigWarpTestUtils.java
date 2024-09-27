package bigwarp;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.DoubleStream;

import org.bouncycastle.util.Arrays;
import org.junit.Assert;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import bdv.gui.BigWarpViewerOptions;
import bdv.viewer.Source;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.source.SourceInfo;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.StackWriter;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.FinalInterval;
import net.imglib2.RealPoint;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

public class BigWarpTestUtils
{

	/**
	 * Create a 3D image file which is deleted on exit.
	 *
	 * @param title of the temporary image file
	 * @param format of the temporary image file
	 * @return the path to the temporary image file
	 */
	public static String createTemp3DImage( String title, String format )
	{

		final Path tmpImgPath;
		try
		{
			tmpImgPath = Files.createTempFile( title, "." + format );
			//noinspection ResultOfMethodCallIgnored
			tmpImgPath.toFile().delete();
		}
		catch ( final IOException e )
		{
			throw new RuntimeException( e );
		}

		return createTemp3DImage( format, tmpImgPath );
	}

	private static String create3DImage( final String format, final Path tmpImgPath ) throws IOException
	{
		final ImagePlus img3d = NewImage.createByteImage( tmpImgPath.getFileName().toString(), 8, 8, 4, NewImage.FILL_RAMP );
		System.out.println( tmpImgPath.toString());
		IJ.saveAs(img3d, format, tmpImgPath.toString());

		tmpImgPath.toFile().deleteOnExit();
		return tmpImgPath.toString();
	}

	/**
	 * Create a 3D image file at {@code imagePath} which is deleted on exit.
	 *
	 * @param imagePath of the temporary image file
	 * @return the path to the temporary image file
	 */
	public static String createTemp3DImage( final String format, Path imagePath )
	{
		try
		{
			return create3DImage( format, imagePath );
		}
		catch ( final Exception e )
		{
			//noinspection ResultOfMethodCallIgnored
			imagePath.toFile().delete();
			throw new RuntimeException( e );
		}
	}

	private static String create2DImage( final String format, final Path tmpImgPath ) throws IOException
	{
		final ImagePlus img2d = NewImage.createByteImage( tmpImgPath.getFileName().toString(), 8, 8, 1, NewImage.FILL_RAMP );
		System.out.println( tmpImgPath.toString());
		IJ.saveAs(img2d, format, tmpImgPath.toString());
		tmpImgPath.toFile().deleteOnExit();
		return tmpImgPath.toString();
	}

	/**
	 * Create a 2D image file which is deleted on exit.
	 *
	 * @param title of the temporary image file
	 * @param format of the temporary image file
	 * @return the path to the temporary image file
	 */
	public static String createTemp2DImage( String title, String format )
	{

		Path tmpImg = null;
		try
		{
			tmpImg = Files.createTempFile( title, "." + format );
			//noinspection ResultOfMethodCallIgnored
			tmpImg.toFile().delete();
			return create2DImage( format, tmpImg );
		}
		catch ( final Exception e )
		{
			if (tmpImg != null) {
				//noinspection ResultOfMethodCallIgnored
				tmpImg.toFile().delete();
			}
			throw new RuntimeException( e );
		}
	}

	/**
	 * Create a 3D image stack which is deleted on exit.
	 *
	 * @param title of the temporary image stack
	 * @return the path to the temporary image stack
	 */
	public static String createTemp3DImageStack( String title )
	{

		final ImagePlus img3d = NewImage.createByteImage( title, 8, 8, 4, NewImage.FILL_RAMP );

		Path tmpStackDir;
		try
		{

			tmpStackDir = Files.createTempDirectory( title );
			StackWriter.save( img3d ,tmpStackDir.toString() + "/", "format=tiff");
			return tmpStackDir.toString();
		}
		catch ( final IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	public static void assertJsonDiff( final JsonElement expectedJson, final JsonElement actualJson )
	{
		final Gson gson = new Gson();
		//noinspection UnstableApiUsage
		final Type mapType = new TypeToken< Map< String, Object > >()
		{
		}.getType();
		final Map< String, Object > expectedMap = gson.fromJson( expectedJson, mapType );
		final Map< String, Object > actualMap = gson.fromJson( actualJson, mapType );
		final MapDifference< String, Object > difference = Maps.difference( expectedMap, actualMap );
		if ( !difference.areEqual() )
		{
			if ( difference.entriesDiffering().size() > 0 )
			{
				difference.entriesDiffering().forEach( ( key, value ) -> {
					final Object left = value.leftValue();
					final Object right = value.rightValue();

					if ( left instanceof Map && right instanceof Map )
					{
						final Map< ?, ? > leftMap = ( Map< ?, ? > ) value.leftValue();
						final Map< ?, ? > rightMap = ( Map< ?, ? > ) value.rightValue();
						removeEqualKeys( leftMap, rightMap );
					}
				} );
			}
			Assert.fail( difference.toString() );
		}

	}

	private static void removeEqualKeys( Map< ?, ? > left, Map< ?, ? > right )
	{
		final Object[] leftVals = left.values().toArray();
		final boolean hasChildren = leftVals.length > 0;
		final boolean childrenIsMap = hasChildren && leftVals[ 0 ] instanceof Map< ?, ? >;

		if ( childrenIsMap )
		{
			/* recurse*/
			left.keySet().forEach( key -> {
				final Map< ?, ? > innerLeft = ( Map< ?, ? > ) left.get( key );
				final Map< ?, ? > innerRight = ( Map< ?, ? > ) right.get( key );
				removeEqualKeys( innerLeft, innerRight );
			} );
		}
		else
		{
			final ArrayList< Object > keysToRemove = new ArrayList<>();
			left.forEach( ( checkKey, value1 ) -> {
				if ( right.containsKey( checkKey ) && right.get( checkKey ).equals( value1 ) )
				{
					keysToRemove.add( checkKey );
				}
			} );
			keysToRemove.forEach( keyToRemove -> {
				left.remove( keyToRemove );
				right.remove( keyToRemove );
			} );
		}

	}

	private static String prettyPrint( StringWriter json )
	{
		return prettyPrint( json.toString() );
	}

	private static String prettyPrint( String json )
	{
		final JsonElement parse = JsonParser.parseString( json );
		final JsonObject asJsonObject = parse.getAsJsonObject();

		return prettyPrint( asJsonObject );
	}

	public static String prettyPrint( JsonObject json )
	{

		return new GsonBuilder().setPrettyPrinting().create().toJson( json );
	}

	static < T extends NativeType<T> >BigWarp< T > createBigWarp(boolean... moving ) throws SpimDataException, URISyntaxException, IOException
	{
		return createBigWarp( null, moving );
	}

	static < T extends NativeType<T> > BigWarp< T > createBigWarp(String sourcePath,   boolean... moving ) throws SpimDataException, URISyntaxException, IOException
	{
		final BigWarpData< T > data = BigWarpInit.initData();
		if (sourcePath != null) {
			createTemp3DImage( "tif", Paths.get(sourcePath) );
		}

		final String tmpPath = sourcePath != null ? sourcePath : createTemp3DImage( "img", "tif" );

		for ( int i = 0; i < moving.length; i++ )
		{
			final LinkedHashMap< Source< T >, SourceInfo > sources = BigWarpInit.createSources( data, tmpPath, i, moving[ i ] );
			BigWarpInit.add( data, sources );
		}
		final BigWarpViewerOptions opts = BigWarpViewerOptions.options( false );
		return new BigWarp<>( data, opts, null );
	}

	public static ImagePlus generateImagePlus( final String title )
	{
		final FunctionRandomAccessible< UnsignedByteType > fimg = new FunctionRandomAccessible<>(
				3,
				( l, v ) -> v.setOne(),
				UnsignedByteType::new );
		return ImageJFunctions.wrap( Views.interval( fimg, new FinalInterval( 32, 32, 1 ) ), title );
	}

	public static ImagePlus generateImagePlus3d(final String title,
			final long[] size,
			final long[] pos,
			final int value,
			final double[] resolution,
			final double[] offset) {

		final ArrayImg<UnsignedByteType, ByteArray> img = ArrayImgs.unsignedBytes(size);
		img.getAt(pos).set(value);

		final ImagePlus imp = ImageJFunctions.wrap(img, title);
		imp.setDimensions(1, (int)size[2], 1);

		imp.getCalibration().pixelWidth = resolution[0];
		imp.getCalibration().pixelHeight = resolution[1];
		imp.getCalibration().pixelDepth = resolution[2];

		imp.getCalibration().xOrigin = offset[0];
		imp.getCalibration().yOrigin = offset[1];
		imp.getCalibration().zOrigin = offset[2];

		return imp;
	}

	public static class TestImagePlusBuilder {

		String title = "img";
		long[] size = new long[]{32, 16, 8};
		long[] pos = new long[]{16, 8, 4};
		int value = 1;
		double[] resolution = new double[]{4, 3, 2};
		double[] offset = new double[]{0, 0, 0};

		public ImagePlus build() {

			return generateImagePlus3d(title, size, pos, value, resolution, offset);
		}

		public TestImagePlusBuilder title(String title) {

			this.title = title;
			return this;
		}

		public TestImagePlusBuilder size(long[] size) {

			this.size = size;
			return this;
		}

		public TestImagePlusBuilder position(long[] pos) {

			this.pos = pos;
			return this;
		}

		public TestImagePlusBuilder position(int value) {

			this.value = value;
			return this;
		}

		public TestImagePlusBuilder resolution(double[] resolution) {

			this.resolution = resolution;
			return this;
		}

		public TestImagePlusBuilder offset(double[] offset) {

			this.offset = offset;
			return this;
		}

	}

	public static LandmarkTableModel identityLandmarks(int nd) {

		final long[] min = new long[nd];
		Arrays.fill(min, -1);

		final long[] max = new long[nd];
		Arrays.fill(max, 1);

		final double[] ones = DoubleStream.generate(() -> 1.0).limit(nd).toArray();
		return landmarks(new IntervalIterator(min, max), new Scale(ones));
	}

	public static LandmarkTableModel landmarks(final IntervalIterator it, RealTransform tform) {

		final int nd = it.numDimensions();
		final LandmarkTableModel ltm = new LandmarkTableModel(nd);

		final RealPoint pt = new RealPoint(nd);
		while (it.hasNext()) {
			it.fwd();
			tform.apply(it, pt);
			ltm.add(it.positionAsDoubleArray(), pt.positionAsDoubleArray());
		}

		return ltm;
	}

}
