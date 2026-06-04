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
package bigwarp.util;

import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;

public class Rotation2DHelpers 
{
	public static final double PI  = Math.PI;

	public static final double PIo2  = Math.PI / 2;

	public static final double PIo2t3  = Math.PI * 1.5;

	public static final double PIt2  = Math.PI * 2;

	public static final double sqrt2  = Math.sqrt( 2 );
	
	public static final double eps = 0.001;


	public static void rotateByBasisRow( 
			final AffineTransform3D transform,
			final int row, 
			final boolean clockwise, 
			final double[] tmpRow )
	{
		if( clockwise )
			rotateByBasisRowCW( transform, row, tmpRow );
		else
			rotateByBasisRowCCW( transform, row, tmpRow );
	}

	public static void rotateByBasisRowCCW( 
			final AffineTransform3D transform,
			final int row,
			final double[] tmpRow)
	{
		double angle = angle2d( transform, row );
		double scale = rowMag2d( transform, row );

		if ( angle >= -eps && angle < (PIo2-eps) )
		{
			tmpRow[ 0 ] = 0;
			tmpRow[ 1 ] = scale;
		}
		else if( angle >= (PIo2-eps) && angle < (PI-eps) )
		{
			tmpRow[ 0 ] = -scale;
			tmpRow[ 1 ] = 0;
		}
		else if( angle >= (PI-eps) && angle < (PIo2t3-eps) )
		{
			tmpRow[ 0 ] = 0;
			tmpRow[ 1 ] = -scale;
		}
		else
		{
			tmpRow[ 0 ] = scale;
			tmpRow[ 1 ] = 0;
		}
	}
	
	public static void rotateByBasisRowCW( 
			final AffineTransform3D transform,
			final int row, 
			final double[] tmpRow )
	{
		double angle = angle2d( transform, row );
		if( angle > PIt2 )
			angle -= PIt2;

		double scale = rowMag2d( transform, row );

		if ( angle >= eps && angle < (PIo2+eps) )
		{
			tmpRow[ 0 ] = scale;
			tmpRow[ 1 ] = 0;
		}
		else if( angle >= (PIo2+eps) && angle < (PI+eps) )
		{
			tmpRow[ 0 ] = 0;
			tmpRow[ 1 ] = scale;
		}
		else if( angle >= (PI+eps) && angle < (PIo2t3+eps) )
		{
			tmpRow[ 0 ] = -scale;
			tmpRow[ 1 ] = 0;
		}
		else
		{
			tmpRow[ 0 ] = 0;
			tmpRow[ 1 ] = -scale;
		}
	}

	public static AffineTransform3D targetViewerTransform2d( 
			final AffineTransform3D currentView,
			final boolean clockwise )
	{
		AffineTransform3D newView = currentView.copy();
		double[] xrow = new double[ 2 ];
		double[] yrow = new double[ 2 ];
		rotateByBasisRow( newView, 0, clockwise, xrow );
		rotateByBasisRow( newView, 1, clockwise, yrow );

//		System.out.println( "old view " + currentView );
//		System.out.println( "new view: "  + 
//				xrow[ 0 ] + " " +  xrow[ 1 ] + " " + currentView.get(0, 2) + " " + currentView.get(0, 3) + " " + 
//				yrow[ 0 ] + " " + yrow[ 1 ] + " " + currentView.get(1, 2) + " " + currentView.get(1, 3) + " " + 
//				currentView.get(2, 0) + " " + currentView.get(2, 1) + " " + currentView.get(2, 2) + " " + currentView.get(2, 3) );

		newView.set( 
				xrow[ 0 ], xrow[ 1 ], currentView.get(0, 2), currentView.get(0, 3), 
				yrow[ 0 ], yrow[ 1 ], currentView.get(1, 2), currentView.get(1, 3), 
				currentView.get(2, 0), currentView.get(2, 1), currentView.get(2, 2), currentView.get(2, 3) );
		

		return newView;
	}

	public static AffineTransform3D targetViewerTransform2dOld(
			final AffineTransform3D currentView,
			final boolean clockwise )
	{
		double angle = angle2d( currentView, 0 );
		double angleDiff = angle % ( PI / 2 );

		AffineTransform3D newView = currentView.copy();
		if( clockwise )
			newView.rotate( 2, (angleDiff * PI));
		else
			newView.rotate( 2, angleDiff );

		return newView;
	}

	public static double angle2d( final AffineTransform3D xfm, final int r )
	{
		return Rotation2DHelpers.getAngle( xfm.get(r, 0), xfm.get(r, 1));
	}
	
	public static void compareCos( final AffineTransform3D xfm, double trueAngle )
	{
		double cosAngle = cosAngle2d( xfm );
		double trueCos  = Math.cos( trueAngle );
		System.out.println( "estimated cos: " + cosAngle );
		System.out.println( "true cos     : " + trueCos );
		System.out.println( "diff         : " + (cosAngle-trueCos) );
	}

	/**
	 * Return the determinant of the top-left 2x2 transformation matrix.
	 * 
	 * @param xfm a 3d transform
	 * @return the determinant
	 */
	public static double determinant2d( final AffineTransform3D xfm )
	{
		return xfm.get(0, 0) * xfm.get(1, 1) 
				- xfm.get(1, 0) * xfm.get(0, 1);
	}
	
	public static double cosAngle2d( final AffineTransform3D xfm )
	{
		return xfm.get(0, 0) / rowMag2d( xfm, 0 );
	}
	
	public static double rowMag2d( final AffineTransform3D xfm, int r )
	{
		return Math.sqrt( 
					xfm.get(r, 0) * xfm.get(r, 0) + 
					xfm.get(r, 1) * xfm.get(r, 1));
	}

	/**
	 * Return the determinant of the 2d transform
	 * 
	 * @param xfm a 3d transform
	 * @return the determinant
	 */
	public static double determinant2d( final AffineTransform2D xfm )
	{
		return xfm.get(0, 0) * xfm.get(1, 1) 
				- xfm.get(1, 0) * xfm.get(0, 1);
	}

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
			
		}
		else
		{
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
	
	public static boolean isSimilarity( final AffineTransform3D xfm )
	{
		double[] p = new double[]{ 1, 0, 0 };
		double[] q = new double[ 3 ];

		xfm.apply(p, q);
		double sx = q[0]*q[0] + q[1]*q[1];

		p[ 0 ] = 0; p[ 1 ] = 1;
		xfm.apply(p, q);
		double sy = q[0]*q[0] + q[1]*q[1];

		return sx == sy;
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
	 * Returns the angle in radians of the given polar coordinates 
	 * correcting Math.atan2 output to a pattern I can digest. 
	 * Adjusting so that 0 is 3 o'clock, PI+PI/2 is 12 o'clock, 
	 * PI is 9 o'clock, and PI/2 is 6 o'clock 
	 * 
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

	/**
	 * Left handed coordinate system.
	 * 
	 * @param theta angle
	 * @return rotation matrix
	 */
	public static double[][] rotationToMatrixLeftHanded( double theta )
	{
		double cos = Math.cos( theta );
		double sin = Math.sin( theta );
		
		double[][] transform = new double[][]{
				{ sin,  cos,  0.0 }, 
				{ cos, -sin,  0.0 },
				{ 0.0,  0.0,  1.0 }};
		
		return transform;
	}

	public static double[][] rotationToMatrix( double theta )
	{
		return rotationToMatrix( theta, false );
	}
	
	public static double[][] rotationToMatrix( double theta, boolean isZFlipped )
	{
		double cos = Math.cos( theta );
		double sin = Math.sin( theta );
		double[][] transform ;
		if( isZFlipped )
			transform = new double[][]{
				{ cos, -sin,  0.0 },
				{ sin,  cos,  0.0 }, 
				{ 0.0,  0.0,  1.0 }};
		else
			transform = new double[][]{
				{ cos, -sin,  0.0 },
				{ sin,  cos,  0.0 }, 
				{ 0.0,  0.0,  -1.0 }};

		return transform;
	}

	public static AffineTransform3D rotationToTransform( double theta, AffineTransform3D transform )
	{
		double cos = Math.cos( theta );
		double sin = Math.sin( theta );
		
		transform.set( 	cos, -sin,  0.0,  0.0, 
						sin,  cos,  0.0,  0.0, 
						0.0,  0.0,  1.0,  0.0);
		
		return transform;
	}
	
	public static AffineTransform3D rotationToTransform( double theta )
	{
		AffineTransform3D transform = new AffineTransform3D();
		return rotationToTransform( theta, transform );
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
				transform.get(2,0), transform.get(2,1),transform.get(2,2), transform.get(2,3) ) + "\n");
	}

	public static void main( String[] args )
	{
		AffineTransform3D id = new AffineTransform3D();

		AffineTransform3D flipZ = new AffineTransform3D();
		flipZ.set(-1, 2, 2);

//		// print angles as a fraction of pi
//		System.out.println( getAngle( sqrt2, sqrt2 ) / PI );
//
//		System.out.println( getAngle( -sqrt2, sqrt2 ) / PI );
//
//		System.out.println( getAngle( -sqrt2, -sqrt2 ) / PI );
//
//		System.out.println( getAngle( sqrt2, -sqrt2 ) / PI );
//
//		System.out.println( " " );
//		for( double x = 0.25; x < 2; x += 0.5 )
//		{
//			System.out.println(  x % 0.5 );
//		}


		double pi3 = Math.PI / 3;
		double pi4 = Math.PI / 4;
		double pi6 = Math.PI / 6;
		
		double iniAngle = -pi4;
		System.out.println( "iniAngle: " + iniAngle );

//		AffineTransform3D xfm = id.copy();
//		xfm.scale( 2 );
//		xfm.rotate( 2, iniAngle );
//		System.out.println( xfm );
		
		AffineTransform3D newView = new AffineTransform3D();
		AffineTransform3D xfm = new AffineTransform3D();

//		xfm.set(
//				-0.6434782608695611, 2.8576175242525584E-16, 0.0, 554.3498071312583, 
//				-2.8576175242525584E-16, -0.6434782608695611, 0.0, 358.8994722287988, 
//				0.0, 0.0, 0.6434782608695607, 0.0 );
//		newView = targetViewerTransform2d( xfm, true);
//
//		xfm.set(0.5041820549449209, 0.0, 0.0, 117.41157229406889, 0.0, 0.5041820549449209, 0.0, -70.83911667235193, 0.0, 0.0, 0.5041820549449209, 0.0);
//		newView = targetViewerTransform2d( xfm, true);
		
//		xfm.set(-
//				4.137404315536347, -1.3446564025493126, 0.0, 2909.4473014898595,
//				-1.3446564025493126, 4.137404315536347, 0.0, 2121.1164640177226,
//				-0.0, 0.0, -4.350427026297034, -0.0);

		xfm.set(7.713925195605986E-15, 34.740430636495, -0.0, 9158.97224991679, 34.740430636495, -7.713925195605986E-15, 0.0, -23549.535244807274, 0.0, 0.0, -34.74043063649499, 0.0);

		System.out.println( "xfm angle 0 : " + angle2d( xfm, 0 ) / PI );
		System.out.println( "xfm angle 1 : " + angle2d( xfm, 1 ) / PI );

		newView = targetViewerTransform2d( xfm, true );
		System.out.println( "newView : " + newView );
		System.out.println( "newView angle 0 : " + angle2d( newView, 0 ) / PI );
		System.out.println( "newView angle 1 : " + angle2d( newView, 1 ) / PI );
		

		
//		boolean isCW = true;
//		newView = targetViewerTransform2d( xfm, isCW );
//
//		xfm.rotate( 2, PIo2 );
//		newView = targetViewerTransform2d( xfm, isCW );
//
//		xfm.rotate( 2, PIo2 );
//		newView = targetViewerTransform2d( xfm, isCW );
//
//		xfm.rotate( 2, PIo2 );
//		newView = targetViewerTransform2d( xfm, isCW );
		
		

//		double[] x = new double[]{ 1, 0, 0 };
//		double[] res = new double[ 3 ]; 
//		xfm.apply(x, res);
//		System.out.println( " ");
//		System.out.println( " x unit vec goes to: " + Arrays.toString( res ));
//		System.out.println( " res len: " + LinAlgHelpers.length( res ));


//		AffineTransform3D newView = targetViewerTransform2d( xfm, true);
//		System.out.println( "newView: " + newView );
//		System.out.println( "newView angle 0 : " + angle2d( newView, 0 ) / PI );
//		System.out.println( "newView angle 1 : " + angle2d( newView, 1 ) / PI );


//		AffineTransform3D testXfm = id.copy();
//		testXfm.rotate( 2, iniAngle );
//		testXfm.rotate( 2, -iniAngle );
//		System.out.println( "\ntestXfm: " + testXfm );

//
//		/* 
//		 * 2d sub rotations
//		 */
////		AffineTransform3D rot4 = id.copy();
////		rot4.rotate( 2, pi4 );
////		compareCos( rot4, pi4 );
//
//		for( double angle = 0; angle <= PI + 0.0001; angle+= pi6 )
//		{
//			AffineTransform3D rot = id.copy();
//			rot.rotate( 2, -angle );
//			System.out.println( rot );
//			compareCos( rot, -angle );
//			System.out.println( " " );
//		}


//		double[] qId = new double[ 4 ];
//		double[] q1 = new double[ 4 ];
//		double[][] mat = new double[ 3 ][ 4 ];
//
//		id.toMatrix( mat );
//		LinAlgHelpers.quaternionFromR( mat, qId );
//
//		rot4.toMatrix( mat );
//		LinAlgHelpers.quaternionFromR( mat, q1 );




//		double idAngle = Rotation2DHelpers.extractRotation( id );
//		System.out.println( idAngle );
//		double flipZAngle = Rotation2DHelpers.extractRotation( flipZ );
//		System.out.println( flipZAngle );
		

//		AffineTransform3D rotId = id.copy();
//		rotId.rotate(2, Math.PI);
//
//		AffineTransform3D rotFz = flipZ.copy();
//		rotFz.rotate(2, Math.PI/2);
//
//		double a1 = Rotation2DHelpers.extractRotation( rotId );
//		System.out.println( a1 );
//		double a2 = Rotation2DHelpers.extractRotation( rotFz );
//		System.out.println( a2 );


		System.out.println( "done" );
	}
	
}
