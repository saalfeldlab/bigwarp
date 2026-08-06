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
package bigwarp.apply;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;
import org.junit.Test;

import bdv.gui.ApplyTransformFrame;
import bdv.ij.ApplyBigwarpPlugin;
import bdv.ij.ApplyBigwarpTransformPlugIn;
import bdv.ij.ApplyBigwarpTransformPlugIn.ApplyTransformParameters;
import bigwarp.BigWarpData;
import bigwarp.BigWarpInit;
import bigwarp.BigWarpTestUtils;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import ij.ImagePlus;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;

/**
 * Tests the parameter-driven entry point of {@link ApplyBigwarpTransformPlugIn}.
 * <p>
 * Deliberately uses a non-unit voxel spacing: with spacing 1 the physical and pixel
 * offsets coincide, so the tests in {@link BigWarpApplyTests} cannot tell whether the
 * offset flowing into the exporter is in physical or pixel units.
 */
public class ApplyTransformParametersTest
{
	private static final double EPS = 1e-6;

	private static final double[] RESOLUTION = new double[] { 4, 3, 2 };

	@Test
	public void testApplyAtNonUnitResolution()
	{
		final long[] pt = new long[] { 16, 8, 4 };
		final double[] ptPhysical = physical( pt, RESOLUTION );

		final ImagePlus mvg = new BigWarpTestUtils.TestImagePlusBuilder().title( "mvg" )
				.resolution( RESOLUTION )
				.position( pt ).build();

		// output starts at the non-zero voxel, so it should land at output pixel (0,0,0)
		final ApplyTransformParameters params = params( new long[] { 2, 2, 2 }, RESOLUTION, ptPhysical,
				ApplyBigwarpTransformPlugIn.DEST_IMAGEJ, "", "" );

		final List<ImagePlus> resList = run( mvg, params );

		assertNotNull( "no result", resList );
		assertEquals( 1, resList.size() );

		final ImagePlus result = resList.get( 0 );
		assertResolutionsEqual( RESOLUTION, result );
		assertOriginsEqual( ptPhysical, result );

		final Img<UnsignedByteType> img = ImageJFunctions.wrapByte( result );
		assertArrayEquals( "result image the wrong size", new long[] { 2, 2, 2 },
				Intervals.dimensionsAsLongArray( img ) );

		assertEquals( "non-zero voxel at the output origin", 1, img.getAt( 0, 0, 0 ).get() );
		assertEquals( 0, img.getAt( 1, 1, 1 ).get() );
	}

	/**
	 * Halving the spacing should double the pixel count needed to cover the same physical
	 * extent, and leave the physical origin where it was.
	 */
	@Test
	public void testApplyAtFinerResolution()
	{
		final long[] pt = new long[] { 16, 8, 4 };
		final double[] ptPhysical = physical( pt, RESOLUTION );

		final ImagePlus mvg = new BigWarpTestUtils.TestImagePlusBuilder().title( "mvg" )
				.resolution( RESOLUTION )
				.position( pt ).build();

		final double[] finer = new double[] { RESOLUTION[ 0 ] / 2, RESOLUTION[ 1 ] / 2, RESOLUTION[ 2 ] / 2 };
		final ApplyTransformParameters params = params( new long[] { 4, 4, 4 }, finer, ptPhysical,
				ApplyBigwarpTransformPlugIn.DEST_IMAGEJ, "", "" );

		final List<ImagePlus> resList = run( mvg, params );
		assertEquals( 1, resList.size() );

		final ImagePlus result = resList.get( 0 );
		assertResolutionsEqual( finer, result );
		assertOriginsEqual( ptPhysical, result );

		final Img<UnsignedByteType> img = ImageJFunctions.wrapByte( result );
		assertArrayEquals( new long[] { 4, 4, 4 }, Intervals.dimensionsAsLongArray( img ) );
		assertEquals( "non-zero voxel at the output origin", 1, img.getAt( 0, 0, 0 ).get() );
	}

	/**
	 * The ome-ngff scale and translation written for a container must be the spacing and
	 * physical min the dialog showed.
	 * <p>
	 * This is what the {@code runN5Export} overload taking a physical offset is for: the
	 * older overload derives the offset with
	 * {@link ApplyBigwarpPlugin#getPixelOffset(String, double[], double[], net.imglib2.Interval)},
	 * which returns pixel units, so the translation came out divided by the spacing.
	 */
	@Test
	public void testWriteZarrMetadata() throws IOException
	{
		final long[] pt = new long[] { 16, 8, 4 };
		final double[] ptPhysical = physical( pt, RESOLUTION );

		final ImagePlus mvg = new BigWarpTestUtils.TestImagePlusBuilder().title( "mvg" )
				.resolution( RESOLUTION )
				.position( pt ).build();

		final File tmpDir = Files.createTempDirectory( "bw-apply-test-" ).toFile();
		final String root = new File( tmpDir, "out.zarr" ).getAbsolutePath();
		final String dataset = "warped";

		final ApplyTransformParameters params = new ApplyTransformParameters(
				"", "", "", "", "", "", "", BigWarpTransform.AFFINE,
				new long[] { 2, 2, 2 }, RESOLUTION, ptPhysical, "um",
				ApplyBigwarpPlugin.SPECIFIED, "", "FACES", 5,
				ApplyBigwarpTransformPlugIn.INTERP_NEAREST, false, 1,
				ApplyBigwarpTransformPlugIn.DEST_N5, root, dataset, "32", "raw" );

		assertNull( "container writes return no images", run( mvg, params ) );

		try (final N5Reader n5 = new N5Factory().openReader( root ))
		{
			assertTrue( "dataset was not written", n5.datasetExists( dataset + "/s0" ) );

			final Optional<OmeNgffMetadata> parsed = new OmeNgffMetadataParser().parseMetadata( n5, dataset );
			assertTrue( "no ome-ngff metadata", parsed.isPresent() );

			final NgffSingleScaleAxesMetadata s0 = parsed.get().multiscales[ 0 ].getChildrenMetadata()[ 0 ];

			assertArrayEquals( "scale must be the output spacing", RESOLUTION, s0.getScale(), EPS );
			assertArrayEquals( "translation must be the physical min, not min/spacing",
					ptPhysical, s0.getTranslation(), EPS );
		}
		finally
		{
			deleteRecursively( tmpDir );
		}
	}

	private static void deleteRecursively( final File f )
	{
		final File[] children = f.listFiles();
		if ( children != null )
			for ( final File c : children )
				deleteRecursively( c );

		f.delete();
	}

	@Test
	public void testMacroRoundTrip()
	{
		final ApplyTransformParameters params = new ApplyTransformParameters(
				"/tmp/some landmarks.csv",
				"mvg.tif",
				"/tmp/moving.zarr",
				"some/moving dataset",
				"tgt.tif",
				"/tmp/target.n5",
				"target/dataset",
				BigWarpTransform.SIMILARITY,
				new long[] { 11, 22, 33 },
				new double[] { 4, 3, 2 },
				new double[] { -1.5, 0.25, 7 },
				"um",
				ApplyBigwarpPlugin.MOVING_WARPED,
				"bbox.*",
				"VOLUME",
				7,
				ApplyBigwarpTransformPlugIn.INTERP_NEAREST,
				true,
				3,
				ApplyBigwarpTransformPlugIn.DEST_N5,
				"/tmp/out dir.zarr",
				"warped/s0",
				"32,32,16",
				"gzip" );

		final String options = ApplyTransformFrame.toMacroOptions( params, true );
		final ApplyTransformParameters rt = ApplyTransformFrame.parseMacroOptions( options );

		assertEquals( "landmarks", params.landmarkPath, rt.landmarkPath );
		assertEquals( "moving image", params.movingImage, rt.movingImage );
		assertEquals( "moving root", params.movingRoot, rt.movingRoot );
		assertEquals( "moving dataset", params.movingDataset, rt.movingDataset );
		assertEquals( "target image", params.targetImage, rt.targetImage );
		assertEquals( "target root", params.targetRoot, rt.targetRoot );
		assertEquals( "target dataset", params.targetDataset, rt.targetDataset );
		assertEquals( "transform type", params.transformType, rt.transformType );

		assertArrayEquals( "pixel size", params.pixSize, rt.pixSize );
		assertArrayEquals( "spacing", params.spacing, rt.spacing, EPS );
		assertArrayEquals( "min", params.min, rt.min, EPS );
		assertEquals( "unit", params.unit, rt.unit );

		assertEquals( "fov reference", params.fovReference, rt.fovReference );
		assertEquals( "point filter", params.pointFilter, rt.pointFilter );
		assertEquals( "bbox method", params.bboxMethod, rt.bboxMethod );
		assertEquals( "bbox samples", params.bboxSamples, rt.bboxSamples );

		assertEquals( "interpolation", params.interpolation, rt.interpolation );
		assertEquals( "virtual", params.virtual, rt.virtual );
		assertEquals( "threads", params.nThreads, rt.nThreads );

		assertEquals( "destination", params.destination, rt.destination );
		assertEquals( "path", params.path, rt.path );
		assertEquals( "dataset", params.dataset, rt.dataset );
		assertEquals( "block size", params.blockSizeString, rt.blockSizeString );
		assertEquals( "compression", params.compressionString, rt.compressionString );
	}

	/**
	 * An unchecked box must not read back as checked, and an unset optional string must not
	 * become the literal text of some other option.
	 */
	@Test
	public void testMacroRoundTripDefaults()
	{
		final ApplyTransformParameters params = new ApplyTransformParameters(
				"", "", "", "", "", "", "", BigWarpTransform.TPS,
				new long[] { 8, 8, 8 },
				new double[] { 1, 1, 1 },
				new double[] { 0, 0, 0 },
				"pixel",
				ApplyBigwarpPlugin.TARGET,
				"",
				"FACES",
				5,
				ApplyBigwarpTransformPlugIn.INTERP_LINEAR,
				false,
				1,
				ApplyBigwarpTransformPlugIn.DEST_IMAGEJ,
				"", "", "", "" );

		final String options = ApplyTransformFrame.toMacroOptions( params, true );
		final ApplyTransformParameters rt = ApplyTransformFrame.parseMacroOptions( options );

		assertFalse( "virtual must stay unset", rt.virtual );
		assertEquals( "point filter must stay empty", "", rt.pointFilter );
		assertEquals( "unit", "pixel", rt.unit );
		assertEquals( "destination", ApplyBigwarpTransformPlugIn.DEST_IMAGEJ, rt.destination );
		assertEquals( "threads", 1, rt.nThreads );
		assertArrayEquals( "pixel size", params.pixSize, rt.pixSize );
	}

	/**
	 * When opened from a BigWarp instance the dialog owns no inputs, so it records none -
	 * but everything describing the output must still survive the trip.
	 */
	@Test
	public void testMacroRoundTripWithoutInputs()
	{
		final ApplyTransformParameters params = params( new long[] { 5, 6, 7 },
				new double[] { 4, 3, 2 }, new double[] { 1, 2, 3 },
				ApplyBigwarpTransformPlugIn.DEST_FILE, "/tmp/out.tif", "" );

		final String options = ApplyTransformFrame.toMacroOptions( params, false );
		assertFalse( "inputs must not be recorded", options.contains( "landmarks=" ) );
		assertTrue( "output path must be recorded", options.contains( "path=" ) );

		final ApplyTransformParameters rt = ApplyTransformFrame.parseMacroOptions( options );
		assertArrayEquals( "pixel size", params.pixSize, rt.pixSize );
		assertArrayEquals( "spacing", params.spacing, rt.spacing, EPS );
		assertArrayEquals( "min", params.min, rt.min, EPS );
		assertEquals( "destination", params.destination, rt.destination );
		assertEquals( "path", params.path, rt.path );
	}

	private static List<ImagePlus> run( final ImagePlus mvg, final ApplyTransformParameters params )
	{
		final ImagePlus tgt = null;
		final BigWarpData<?> bwData = BigWarpInit.createBigWarpDataFromImages( mvg, tgt );
		bwData.wrapMovingSources();

		final LandmarkTableModel ltm = BigWarpTestUtils.identityLandmarks( 3 );
		final BigWarpTransform bwTransform = new BigWarpTransform( ltm, BigWarpTransform.AFFINE );

		// show = false, wait = true so the render finishes before we assert on it
		return ApplyBigwarpTransformPlugIn.runFromParameters( params, bwData, ltm, bwTransform, false, true );
	}

	private static ApplyTransformParameters params( final long[] pixSize, final double[] spacing,
			final double[] min, final String destination, final String path, final String dataset )
	{
		return new ApplyTransformParameters(
				"", "", "", "", "", "", "", BigWarpTransform.AFFINE,
				pixSize, spacing, min, "pixel",
				ApplyBigwarpPlugin.SPECIFIED, "", "FACES", 5,
				ApplyBigwarpTransformPlugIn.INTERP_NEAREST,
				false, 1,
				destination, path, dataset, "", "" );
	}

	private static double[] physical( final long[] pixel, final double[] resolution )
	{
		return IntStream.range( 0, pixel.length ).mapToDouble( i -> pixel[ i ] * resolution[ i ] ).toArray();
	}

	private static void assertResolutionsEqual( final double[] expected, final ImagePlus actual )
	{
		assertEquals( "width", expected[ 0 ], actual.getCalibration().pixelWidth, EPS );
		assertEquals( "height", expected[ 1 ], actual.getCalibration().pixelHeight, EPS );
		assertEquals( "depth", expected[ 2 ], actual.getCalibration().pixelDepth, EPS );
	}

	private static void assertOriginsEqual( final double[] expected, final ImagePlus actual )
	{
		assertEquals( "origin x", expected[ 0 ], actual.getCalibration().xOrigin, EPS );
		assertEquals( "origin y", expected[ 1 ], actual.getCalibration().yOrigin, EPS );
		assertEquals( "origin z", expected[ 2 ], actual.getCalibration().zOrigin, EPS );
	}
}
