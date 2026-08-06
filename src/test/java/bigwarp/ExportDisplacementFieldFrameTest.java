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
package bigwarp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.awt.GraphicsEnvironment;

import org.junit.Assume;
import org.junit.Test;

import bdv.gui.ExportDisplacementFieldFrame;
import bdv.ij.ApplyBigwarpPlugin;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import ij.ImagePlus;

public class ExportDisplacementFieldFrameTest
{
	private static final double EPS = 1e-6;

	private static final double[] MOVING_RESOLUTION = new double[] { 4, 3, 2 };

	/**
	 * {@link ApplyBigwarpPlugin#getResolution(BigWarpData, String, double[])} returns null
	 * for {@code TARGET} when no target source was loaded, which is easy to miss because
	 * every other failure in that method throws instead.
	 */
	@Test
	public void testTargetResolutionIsNullWithoutTargetSource()
	{
		final BigWarpData<?> data = movingOnlyData();

		assertEquals( "should have one moving source", 1, data.numMovingSources() );
		assertEquals( "should have no target source", 0, data.numTargetSources() );

		assertNull( "target resolution must be null when there is no target source",
				ApplyBigwarpPlugin.getResolution( data, ApplyBigwarpPlugin.TARGET, null ) );

		assertNotNull( "moving resolution must still resolve",
				ApplyBigwarpPlugin.getResolution( data, ApplyBigwarpPlugin.MOVING, null ) );
	}

	/**
	 * Opening the export dialog against a moving-only session used to throw a
	 * NullPointerException out of {@code FieldOfViewPanel.setSpacing}, because the null
	 * target resolution above was passed straight in. It should instead fall back to the
	 * moving image's resolution.
	 */
	@Test
	public void testCreateContentWithoutTargetSource()
	{
		Assume.assumeFalse( "needs a display", GraphicsEnvironment.isHeadless() );

		final BigWarpData<?> data = movingOnlyData();
		final LandmarkTableModel ltm = BigWarpTestUtils.identityLandmarks( 3 );
		final BigWarpTransform bwTransform = new BigWarpTransform( ltm, BigWarpTransform.TPS );

		final ExportDisplacementFieldFrame frame = new ExportDisplacementFieldFrame( data, bwTransform, ltm );
		try
		{
			// used to throw
			frame.createContent();

			assertArrayEquals( "spacing should default to the moving resolution",
					MOVING_RESOLUTION, frame.getParams().spacing, EPS );
		}
		finally
		{
			frame.dispose();
		}
	}

	private static BigWarpData<?> movingOnlyData()
	{
		final ImagePlus mvg = new BigWarpTestUtils.TestImagePlusBuilder().title( "mvg" )
				.resolution( MOVING_RESOLUTION )
				.position( new long[] { 16, 8, 4 } ).build();

		final ImagePlus tgt = null;
		final BigWarpData<?> data = BigWarpInit.createBigWarpDataFromImages( mvg, tgt );
		data.wrapMovingSources();
		return data;
	}
}
