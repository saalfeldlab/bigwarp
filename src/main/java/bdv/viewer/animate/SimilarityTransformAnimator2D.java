/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2021 Howard Hughes Medical Institute.
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
package bdv.viewer.animate;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import bigwarp.util.BigWarpUtils;
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

		return transform;
	}

	
	public static void main( String[] args )
	{
		AffineTransform3D transformStart = new AffineTransform3D();
		AffineTransform3D transformEnd = new AffineTransform3D();

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
		
//		transformStart.set(
//				0.215745, -0.118358, 0.000000, 148.068400,
//				0.118358, 0.215745, 0.000000,6.756092,
//				0.000000, 0.000000, 1.000000, 0.000000	
//				);
		
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
//		transformEnd.set(
//				0.240841, -0.050498, 0.000000, 143.633126,
//				0.050498, 0.240841, 0.000000, -36.598098,
//				0.000000, 0.000000, 1.000000, 0.000000 );


//		transformStart.set(
//				46.79004235433043, 8.253851140080304, 0.0, -29488.28971568279, 
//				8.253851140080304, -46.79004235433043, 0.0, -17451.486096219247, 
//				0.0, 0.0, 1.0, 0.0 );
//
//		transformEnd.set(
//				69.62311603249404, 13.389060775479622, 0.0, -43841.48828818949, 
//				13.389060775479622, -69.62311603249404, 0.0, -26799.92317573986, 
//				0.0, 0.0, 1.0, 0.0);


		transformStart.scale( 2 );
		transformStart.rotate( 2, Math.PI / 4 );
		transformStart.translate( -1, 5, 0 );
//		BigWarpUtils.permuteXY( transformStart );
		BigWarpUtils.flipX( transformStart );

		transformEnd.scale( 3 );
		transformEnd.rotate( 2, -Math.PI / 6 );
		transformEnd.translate( 10, -6, 0 );
//		BigWarpUtils.permuteXY( transformEnd );
		BigWarpUtils.flipX( transformEnd );
		

//		double cX = 295.0;
//		double cY = 174.5;
//		double cX = 0.0;
//		double cY = 0.5;
		double cX = 683.0;
		double cY = -256.0;

		System.out.println( transformStart );
		System.out.println( "   det orig: " + BigWarpUtils.det( transformStart ));
		BigWarpUtils.ensurePositiveDeterminant( transformStart );
		System.out.println( "   det after correction: " + BigWarpUtils.det( transformStart ));
		System.out.println( transformStart );

		System.out.println( " " );
		System.out.println( transformEnd );
		System.out.println( "   det orig: " + BigWarpUtils.det( transformEnd ));
		BigWarpUtils.ensurePositiveDeterminant( transformEnd );
		System.out.println( "   det after correction: " + BigWarpUtils.det( transformEnd ));
		System.out.println( transformEnd );
	
		SimilarityTransformAnimator2D anim = new SimilarityTransformAnimator2D( transformStart, transformEnd, cX, cY, 300 );
		AffineTransform3D compXfm = anim.get( 1.0 );
		System.out.println( " " );
		System.out.println( "compXfm: " );
		System.out.println( compXfm );
		System.out.println( "  det: " + BigWarpUtils.det( compXfm ));

//		System.out.println("\ncompXfm:");
//		Rotation2DHelpers.printXfm( compXfm );

		
//		// handedness exps
//		AffineTransform3D id = new AffineTransform3D();
//		Rotation2DHelpers.printXfm( id );
//		System.out.println( "  det: " + Rotation2DHelpers.determinant2d( id ));
//		System.out.println( "" );
//
//		AffineTransform3D flipX = id.copy();
//		flipX.set(-1, 0, 0);
//		Rotation2DHelpers.printXfm( flipX );
//		System.out.println( "  det: " + Rotation2DHelpers.determinant2d( flipX ));
//		System.out.println( "" );
//
//		AffineTransform3D flipY = id.copy();
//		flipY.set(-1, 1, 1);
//		Rotation2DHelpers.printXfm( flipY );
//		System.out.println( "  det: " + Rotation2DHelpers.determinant2d( flipY ));
//		System.out.println( "" );


		System.out.println("done");
		System.exit( 0 );
	}
}
