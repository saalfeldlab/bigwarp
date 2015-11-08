package bdv.viewer.animate;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import bigwarp.util.Rotation2DHelpers;

public class SimilarityTransformAnimator2D extends AbstractTransformAnimator
{
	private final double startAngle;
	private final double totalAngle;
	
	private final double cosInvAngleStart;
	private final double sinInvAngleStart;
	
	private final double cosInvAngleEnd;
	private final double sinInvAngleEnd;
	
	private final double scaleStart;

	private final double scaleEnd;

	private final double scaleDiff;

	private final double[] xg0Start;

	private final double[] xg0Diff;

	
	public SimilarityTransformAnimator2D( final AffineTransform3D transformStart, final AffineTransform3D transformEnd, final double cX, final double cY, final long duration )
	{
		super( duration );
		
		startAngle = Rotation2DHelpers.extractRotation( transformStart );
		final double endAngle = Rotation2DHelpers.extractRotation( transformEnd );
		
		scaleStart = Rotation2DHelpers.extractScale( transformStart, 0 );
		scaleEnd   = Rotation2DHelpers.extractScale( transformEnd, 0 );
		scaleDiff = scaleEnd - scaleStart;
		
		totalAngle = Rotation2DHelpers.shorterAngleBetweenRotations( startAngle, endAngle );
		
		cosInvAngleStart = Math.cos( -startAngle );
		sinInvAngleStart = Math.sin( -startAngle );
		
		cosInvAngleEnd = Math.cos( -endAngle );
		sinInvAngleEnd = Math.sin( -endAngle );
		
		final double[] tStart = new double[ 3 ];
		final double[] tEnd = new double[ 3 ];
		for ( int d = 0; d < 3; ++d )
		{
			tStart[ d ] = transformStart.get( d, 3 ) / scaleStart;
			tEnd[ d ] = transformEnd.get( d, 3 ) / scaleEnd;
		}
		
		xg0Start = new double[3];
		final double[] xg0End = new double[3];
		xg0Diff = new double[3];
		
		// inverse rotate scaled starting 
		Rotation2DHelpers.cosSinMult( tStart, xg0Start, cosInvAngleStart, sinInvAngleStart );
		LinAlgHelpers.scale( xg0Start, -1, xg0Start );
			
		Rotation2DHelpers.cosSinMult( tEnd, xg0End, cosInvAngleEnd, sinInvAngleEnd );
		LinAlgHelpers.scale( xg0End, -1, xg0End );
		
		LinAlgHelpers.subtract( xg0End, xg0Start, xg0Diff );
		
	}

	@Override
	public AffineTransform3D get( final double t )
	{

		final double scaleCurrent = scaleStart + t * scaleDiff;
		final double angleCurrent = startAngle + t * totalAngle;
		
		final double[] xg0Current = new double[ 3 ];
		final double[] tCurrent = new double[ 3 ];
		LinAlgHelpers.scale( xg0Diff, -( t * scaleEnd / scaleCurrent ), xg0Current );
		for ( int r = 0; r < 3; ++r )
			xg0Current[ r ] -= xg0Start[ r ];
		
		double[][] Rcurrent = Rotation2DHelpers.rotationToMatrix( angleCurrent );
		LinAlgHelpers.mult( Rcurrent, xg0Current, tCurrent );
		

		final double[][] m = new double[ 3 ][ 4 ];
		for ( int r = 0; r < 3; ++r )
		{
			for ( int c = 0; c < 3; ++c )
				m[ r ][ c ] = scaleCurrent * Rcurrent[ r ][ c ];
			m[ r ][ 3 ] = scaleCurrent * tCurrent[ r ];
		}
		
		final AffineTransform3D transform = new AffineTransform3D();
		transform.set( m );
		transform.set( 1.0, 2, 2 );
		
		return transform;
	}

	
	public static void main( String[] args )
	{
		AffineTransform3D transformStart = new AffineTransform3D();
//		transformStart.set(
//					0.223200, 0.000000, 0.000000, 121.987597,
//					0.000000, 0.223200, 0.000000,  53.168668,
//					0.000000, 0.000000, 1.000000,   0.000000 );
		
//		transformStart.set( 
//				0.242694, 0.040669, 0.000000, 76.287040,
//				-0.040669, 0.242694, 0.000000, 88.073513,
//				0.000000, 0.000000, 1.000000, 0.000000);
		
//		transformStart.set( 
//				0.2, 0.0, 0.0, 10.0,
//				0.0, 0.2, 0.0, -5.0,
//				0.0, 0.0, 1.0, 0.0);
		
		transformStart.set(
				0.215745, -0.118358, 0.000000, 148.068400,
				0.118358, 0.215745, 0.000000,6.756092,
				0.000000, 0.000000, 1.000000, 0.000000	
				);
		
		AffineTransform3D transformEnd = new AffineTransform3D();
//		transformEnd.set(
//				0.337591, 0.000000, 0.000000,  26.952555,
//				0.000000, 0.337591, 0.000000, 121.168796,
//				0.000000, 0.000000, 1.000000,   0.000000 );
//		transformEnd.set(
//				0.223200, 0.000000, 0.000000, 121.987597,
//				0.000000, 0.223200, 0.000000,  53.168668,
//				0.000000, 0.000000, 1.000000,   0.000000 );
//		transformEnd.set(
//				0.4, 0.000000, 0.000000, -100,
//				0.000000, 0.4, 0.000000,  50,
//				0.000000, 0.000000, 1.000000,   0.000000 );
//		transformEnd.set(
//				0.0, -0.4, 0.000000, -100,
//				0.4,  0.0, 0.000000,  50,
//				0.000000, 0.000000, 1.000000,   0.000000 );
		transformEnd.set(
				0.240841, -0.050498, 0.000000, 143.633126,
				0.050498, 0.240841, 0.000000, -36.598098,
				0.000000, 0.000000, 1.000000, 0.000000 );
		
		double cX = 295.0;
		double cY = 174.5;
//		double cX = 0.0;
//		double cY = 0.5;
		
		SimilarityTransformAnimator2D anim = new SimilarityTransformAnimator2D( transformStart, transformEnd, cX, cY, 300 );
		

		AffineTransform3D compXfm = anim.get( 1.0 );
		System.out.println("\ncompXfm:");
		Rotation2DHelpers.printXfm( compXfm );
		
		System.out.println("done");
		System.exit( 0 );
	}
}
