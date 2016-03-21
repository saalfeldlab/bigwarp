package bigwarp.util;

import net.imglib2.realtransform.AffineTransform3D;

public class Rotation2DHelpers 
{
	
	/**
	 * Returns the shorter angle between the starting and ending angle.
	 * @param angleStart starting angle
	 * @param angleEnd ending angle
	 * @return the angle
	 */
	public static double shorterAngleBetweenRotations( double angleStart, double angleEnd )
	{
		double tmpAngle = angleEnd - angleStart;
		if( tmpAngle > Math.PI )
		{
			return tmpAngle - 2 * Math.PI;
		}
		else if( tmpAngle < -Math.PI )
		{
			return 2 * Math.PI + tmpAngle;
			
		}else{
			return tmpAngle;
		}
	}
	
	public static double extractScale( final AffineTransform3D transform, final int axis )
	{
		double sqSum = 0;
		final int c = axis;
		for ( int r = 0; r < 2; ++r )
		{
			final double x = transform.get( r, c );
			sqSum += x * x;
		}
		return Math.sqrt( sqSum );
	}
	
	public static double extractRotation( AffineTransform3D xfm )
	{
		double x = xfm.get( 0, 0 );
		double y = xfm.get( 0, 1 );
		
//		double theta  = Math.atan2( y, x );
		double theta  = Math.atan2( -y, x );
//		double theta = getAngle( y, x );
		
		return theta;
	}

	/**
	 * Taken from http://albert.rierol.net/java_tricks.html
	 * 
	 * @param x x coordinate
	 * @param y y coordinate
	 * @return the angle
	 */
	public static double getAngle(double x, double y) {
		// calculate angle
		double a = Math.atan2(x, y);
		// fix too large angles (beats me why are they ever generated)
		if (a > 2 * Math.PI) {
			a = a - 2 * Math.PI;
		}
		// fix atan2 output scheme to match my mental scheme
		if (a >= 0.0 && a <= Math.PI/2) {
			a = Math.PI/2 - a;
		} else if (a < 0 && a >= -Math.PI) {
			a = Math.PI/2 -a;
		} else if (a > Math.PI/2 && a <= Math.PI) {
			a = Math.PI + Math.PI + Math.PI/2 - a;
		}
		// return
		return a;
	}
	
	public static void angleMult( double[] pt, double[] res, double angle )
	{
		double cos = Math.cos( angle );
		double sin = Math.sin( angle );
		cosSinMult( pt, res, cos, sin );
	}
	
	public static void cosSinMult( double[] pt, double[] res, double cosAngle, double sinAngle )
	{
		res[ 0 ] = cosAngle * pt[ 0 ] - sinAngle * pt[ 1 ];
		res[ 1 ] = sinAngle * pt[ 0 ] + cosAngle * pt[ 1 ];
	}
	
	public static double[][] rotationToMatrix( double theta )
	{
		double cos = Math.cos( theta );
		double sin = Math.sin( theta );
		
		double[][] transform = new double[][]{
				{ cos, -sin,  0.0 },
				{ sin,  cos,  0.0 }, 
				{ 0.0,  0.0,  1.0 }};
		
		return transform;
	}
	
	public static AffineTransform3D rotationToTransform( double theta )
	{
		AffineTransform3D transform = new AffineTransform3D();
		double cos = Math.cos( theta );
		double sin = Math.sin( theta );
		
		transform.set( 	cos, -sin,  0.0,  0.0, 
						sin,  cos,  0.0,  0.0, 
						0.0,  0.0,  1.0,  0.0);
		
		return transform;
	}
	
	public static void printXfm2d( AffineTransform3D xfm )
	{
		System.out.println( String.format( "%02f %02f" , xfm.get(0,0), xfm.get(0,1) ));
		System.out.println( String.format( "%02f %02f" , xfm.get(1,0), xfm.get(1,1) ));
	}
	

	public static void printXfm( AffineTransform3D transform )
	{
		System.out.println( String.format( "%02f, %02f, %02f, %02f," , 
				transform.get(0,0), transform.get(0,1),transform.get(0,2), transform.get(0,3) ));
		System.out.println( String.format( "%02f, %02f, %02f, %02f," , 
				transform.get(1,0), transform.get(1,1),transform.get(1,2), transform.get(1,3) ));
		System.out.println( String.format( "%02f, %02f, %02f, %02f" , 
				transform.get(2,0), transform.get(2,1),transform.get(2,2), transform.get(2,3) ));
	}
	
}
