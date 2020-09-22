package bigwarp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import bigwarp.util.BigWarpUtils;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

public class BoundingBoxEstimationTest
{
	private FinalRealInterval realInterval2d;

	private AffineTransform2D transform2d;

	private FinalRealInterval realInterval3d;

	private AffineTransform3D transform3d;

	@Before
	public void before()
	{
		realInterval2d = Intervals.createMinMaxReal( -1, -1, 1, 1 );
		realInterval3d = Intervals.createMinMaxReal( -1, -1, -1, 1, 1, 1 );

		transform2d = new AffineTransform2D();
		transform2d.set( 1, 1, 0, -1, 1, 0 );

		transform3d = new AffineTransform3D();
		transform3d.set( 1,  1,  0, 0, 
						-1,  1,  0, 0,
						 0,  0,  1, 0);
	}

	@After
	public void after()
	{
		realInterval2d = null;
		realInterval3d = null;

		transform2d = null;
		transform3d = null;
	}

	@Test
	public void testFaces()
	{
		List< RealInterval > faces2d = BigWarpUtils.getFaces( realInterval2d );
		List< RealInterval > faces3d = BigWarpUtils.getFaces( realInterval3d );

		assertEquals( "num faces 3d", 6, faces3d.size() );
		assertEquals( "num faces 2d", 4, faces2d.size() );

		assertTrue( "fixed faces 2d", areAllFacesCovered( faces2d ));
		assertTrue( "fixed faces 3d", areAllFacesCovered( faces3d ));
	}

	private boolean areAllFacesCovered( List<RealInterval> faces )
	{
		int nd = faces.get( 0 ).numDimensions();
		int nfaces = nd == 2 ? 4 : 6;
		boolean[] fixedFaces = new boolean[ nfaces ];
		Arrays.fill( fixedFaces, false );
		for( RealInterval f : faces )
		{
			for( int d = 0; d < nd; d++ )
			{
				double diff = Math.abs( f.realMax( d ) - f.realMin( d ) );
				int offset = f.realMax( d ) < 0 ? 0 : 1;
				if( diff < 1e-6 )
					fixedFaces[ ( 2 * d ) + offset ] = true;
			}
		}

		for( int i = 0; i < nfaces; i++ )
			if( !fixedFaces[ i ] )
				return false;

		return true;
	}
}
