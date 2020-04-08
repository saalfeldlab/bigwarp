package bdv.viewer.animate;

import bigwarp.util.Rotation2DHelpers;
import net.imglib2.realtransform.AffineTransform3D;

public class RotationAnimator2D extends AbstractTransformAnimator
{
	private final AffineTransform3D transformStart;

	private final double cX, cY;
	
	private final double totalAngle;

	private final AffineTransform3D transform;

	public RotationAnimator2D( final AffineTransform3D transformStart, final double viewerCenterX, final double viewerCenterY, AffineTransform3D target, final long duration )
	{
		super( duration );

		this.transformStart = transformStart;
		cX = viewerCenterX;
		cY = viewerCenterY;

		final double angleStart = Rotation2DHelpers.extractRotation( transformStart );
		final double angleEnd	= Rotation2DHelpers.extractRotation( target );

		totalAngle = Rotation2DHelpers.shorterAngleBetweenRotations( angleStart, angleEnd );
		transform = new AffineTransform3D();
	}
	
	@Override
	public AffineTransform3D get( final double t )
	{
		transform.set( transformStart );

		transform.translate( -cX, -cY, 0 );
		transform.rotate( 2, totalAngle * t );
		transform.translate( cX, cY, 0 );

		return transform;
	}
	
	/*
	 * AN OLD TEST
	 */
//	public static void main( String[] args )
//	{
//		// test me
//		double[] pt = new double[]{1.0, 0.0, 0.0};
//		double[] res = new double[ 3 ];
//		
//		AffineTransform3D xfmPI   = rotationToTransform( PI );
//		AffineTransform3D xfmPIo2 = rotationToTransform( PI/2 );
//		AffineTransform3D xfmPIo4 = rotationToTransform( PI/4 );
//		AffineTransform3D xfm3PIo4 = rotationToTransform( PI + PI/2 );
//		AffineTransform3D xfmmPIo4 = rotationToTransform( PI + PI/2  + PI/4);
//		
//		
////		double iniAngle = PI;
//		
//		for (double iniAngle = 0.0; iniAngle <= 2 * PI; iniAngle += PI / 8) 
//		{
//			AffineTransform3D test = rotationToTransform(iniAngle);
//			print(test);
//			
//			double unwrapAngle = ( iniAngle <= PI ) ? iniAngle : iniAngle - 2*PI;
//			
//			System.out.println("\nangle: " + extractRotation(test) / PI
//					+ "  vs  " + (unwrapAngle / PI));
//
//			test.apply(pt, res);
//			System.out.println("\n(1,0)- > (" + res[0] + ", " + res[1] + " )");
//			System.out.println("\n\n\n\n");
//		}
//
//		System.out.println("done");
//	}
	
}
