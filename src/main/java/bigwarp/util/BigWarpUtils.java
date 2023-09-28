/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2022 Howard Hughes Medical Institute.
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

import java.awt.Dimension;
import java.util.HashMap;

import bdv.util.Affine3DHelpers;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerState;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;

public class BigWarpUtils
{
	/**
	 * Set a "good" initial viewer transform. The viewer transform is chosen
	 * such that for the first source,
	 * <ul>
	 * <li>the XY plane is aligned with the screen plane,
	 * <li>the <em>z = 0</em> slice is shown,
	 * <li>centered and scaled such that the full <em>dim_x</em> by
	 * <em>dim_y</em> is visible.
	 * </ul>
	 * This calls {@link #initTransform(int, int, boolean, ViewerState)}, using the size
	 * of the viewer's display component.
	 *
	 * @param viewer
	 *            the viewer (containing at least one source) to have its
	 *            transform set.
	 */
	public static void initTransform( final ViewerPanel viewer )
	{
		final Dimension dim = viewer.getDisplay().getSize();
		final ViewerState state = viewer.state();
		final AffineTransform3D viewerTransform = initTransform( dim.width, dim.height, false, state );
		viewer.setCurrentViewerTransform( viewerTransform );
	}

	public static void ensurePositiveZ( final AffineTransform3D xfm )
	{
		xfm.set( Math.abs( xfm.get( 2, 2 )), 2, 2 );
	}

	public static void ensurePositiveDeterminant( final AffineTransform3D xfm )
	{
//		if( det( xfm ) < 0 )
//			permuteXY( xfm );

		if( det( xfm ) < 0 )
			flipX( xfm );
	}
	
	public static double det( final AffineTransform3D xfm )
	{
		return LinAlgHelpers.det3x3(
				xfm.get(0, 0), xfm.get(0, 1), xfm.get(0, 2), 
				xfm.get(1, 0), xfm.get(1, 1), xfm.get(1, 2), 
				xfm.get(2, 0), xfm.get(2, 1), xfm.get(2, 2) );
	}

	public static double dotXy( final AffineTransform3D xfm )
	{
		return  xfm.get(0, 0) * xfm.get( 0, 1 ) +
				xfm.get(1, 0) * xfm.get( 1, 1 ) +
				xfm.get(2, 0) * xfm.get( 2, 1 );
	}

	public static void flipX( final AffineTransform3D xfm )
	{
		for( int i = 0; i < 4; i++ )
			xfm.set( -xfm.get(0, i), 0, i );
	}

	public static void permuteXY( final AffineTransform3D xfm )
	{
		double tmp = 0;
		for( int i = 0; i < 4; i++ )
		{
			tmp = xfm.get( 1, i );
			xfm.set( xfm.get(0, i), 1, i );
			xfm.set( tmp, 0, i );
		}
	}

	/**
	 * Get a "good" initial viewer transform for 2d. for 2d. for 2d. for 2d. for 2d. for 2d. for 2d. for 2d. The viewer transform is chosen
	 * such that for the first source,
	 * <ul>
	 * <li>the XY plane is aligned with the screen plane,
	 * <li>the <em>z = 0</em> slice is shown,
	 * <li>centered and scaled such that the full <em>dim_x</em> by
	 * <em>dim_y</em> is visible.
	 * </ul>
	 *
	 * @param viewerWidth
	 *            width of the viewer display
	 * @param viewerHeight
	 *            height of the viewer display
	 * @param state
	 *            the {@link ViewerState} containing at least one source.
	 * @return proposed initial viewer transform.
	 */
	public static AffineTransform3D initTransform( final int viewerWidth, final int viewerHeight, final boolean zoomedIn, final ViewerState state )
	{
		final int cX = viewerWidth / 2;
		final int cY = viewerHeight / 2;

		final Source< ? > source = state.getCurrentSource().getSpimSource();
		final int timepoint = state.getCurrentTimepoint();
		if ( !source.isPresent( timepoint ) )
			return new AffineTransform3D();

		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( timepoint, 0, sourceTransform );

		final Interval sourceInterval = source.getSource( timepoint, 0 );
		final double sX0 = sourceInterval.min( 0 );
		final double sX1 = sourceInterval.max( 0 );
		final double sY0 = sourceInterval.min( 1 );
		final double sY1 = sourceInterval.max( 1 );
		final double sX = ( sX0 + sX1 + 1 ) / 2;
		final double sY = ( sY0 + sY1 + 1 ) / 2;

		final double[][] m = new double[ 3 ][ 4 ];

		// rotation
		final double[] qSource = new double[ 4 ];
		final double[] qViewer = new double[ 4 ];
		Affine3DHelpers.extractApproximateRotationAffine( sourceTransform, qSource, 2 );
		LinAlgHelpers.quaternionInvert( qSource, qViewer );
		LinAlgHelpers.quaternionToR( qViewer, m );

		// translation
		final double[] centerSource = new double[] { sX, sY, 0 };
		final double[] centerGlobal = new double[ 3 ];
		final double[] translation = new double[ 3 ];
		sourceTransform.apply( centerSource, centerGlobal );
		LinAlgHelpers.quaternionApply( qViewer, centerGlobal, translation );
		LinAlgHelpers.scale( translation, -1, translation );
		LinAlgHelpers.setCol( 3, translation, m );

		final AffineTransform3D viewerTransform = new AffineTransform3D();
		viewerTransform.set( m );

		// scale
		final double[] pSource = new double[] { sX1 + 0.5, sY1 + 0.5, 0.0 };
		final double[] pGlobal = new double[ 3 ];
		final double[] pScreen = new double[ 3 ];
		sourceTransform.apply( pSource, pGlobal );
		viewerTransform.apply( pGlobal, pScreen );
		final double scaleX = cX / pScreen[ 0 ];
		final double scaleY = cY / pScreen[ 1 ];
		final double scale;
		if ( zoomedIn )
			scale = Math.max( scaleX, scaleY );
		else
			scale = Math.min( scaleX, scaleY );
		viewerTransform.scale( scale );
//		viewerTransform.set( 1.0, 2, 2 );

		// window center offset
		viewerTransform.set( viewerTransform.get( 0, 3 ) + cX, 0, 3 );
		viewerTransform.set( viewerTransform.get( 1, 3 ) + cY, 1, 3 );
		return viewerTransform;
	}

	/**
	 * Computes the angle of rotation between the two input quaternions,
	 * returning the result in radians.  Assumes the inputs are unit quaternions.
	 * 
	 * @param q1 first quaternion
	 * @param q2 second quaternion
	 * @return the angle in radians
	 */
	public static double quaternionAngle( double[] q1, double[] q2 )
	{
		double dot = 0;
		for( int i = 0; i < 4; i++ )
			dot += ( q1[ i ] * q2[ i ]);

		return Math.acos( 2 * dot * dot  - 1);
	}
	
//	public static double angleBetween( final AffineTransform3D xfm1, final AffineTransform3D xfm2 )
//	{
//		double[][] tmpMat = new double[ 3 ][ 4 ];
//		double[] q1 = new double[ 4 ];
//		double[] q2 = new double[ 4 ];
//
//		xfm1.toMatrix( tmpMat );
//		LinAlgHelpers.qu
//	
//		normalize( q1 );
//		normalize( q2 );
//
//	}

	public static void normalize( double[] x )
	{
		double magSqr = 0;
		for( int i = 0; i < x.length; i++ )
			magSqr += (x[ i ] * x[ i ]);

		for( int i = 0; i < x.length; i++ )
			x[ i ] /= magSqr;
	}

	public static HashMap<String,String> parseMacroArguments( String input )
	{
		return parseMacroArguments( input, "=", "[", "]" );
	}

	public static HashMap<String,String> parseMacroArguments( String input, String keyDelim, String startDelim, String endDelim )
	{
		final HashMap<String,String> output = new HashMap<>();

		String arguments = input.trim();
		boolean done = false;
		while( !done )
		{
			int i = arguments.indexOf( keyDelim );
			final String key = arguments.substring( 0, i );
			output.put( key, parse( arguments, startDelim, endDelim ));

			i = arguments.indexOf( endDelim );
			if( i < 0 || i +1 > arguments.length() )
				done = true;

			arguments = arguments.substring( i + 1 ).trim();
			if( arguments.length() == 0 )
				done = true;
			else 
				arguments = arguments.substring( 1 ).trim(); // remove comma
		}

		return output;
	}

	public static String parse( String arg, String start, String end )
	{
		final int startIdx = arg.indexOf( start ) + 1;
		final int endIdx = arg.indexOf( end );
		if ( startIdx < 0 || endIdx < 0 )
			return null;

		return arg.substring( startIdx, endIdx );
	}

	/**
	 * Return a 3D {@link AffineGet} from a lower-dimensional (1D or 2D) AffineGet.
	 * The input instance is returned if it is 3D.
	 *
	 * @param affine the input affine.
	 * @return the 3D affine
	 */
	public static AffineGet toAffine3D( final AffineGet affine )
	{
		if( affine.numSourceDimensions() == 3)
			return affine;

		final AffineTransform3D out = new AffineTransform3D();
		final int N = affine.numSourceDimensions();
		for (int i = 0; i < N; i++)
			for (int j = 0; j < N; j++)
				out.set(affine.get(i, j), i, j);

		// translation part
		for (int i = 0; i < N; i++)
			out.set(affine.get(i, N+1), i, 3);

		return out;
	}

//	/**
//	 * Computes the angle of rotation between the two input quaternions,
//	 * returning the result in degrees.  Assumes the inputs are unit quaternions.
//	 *
//	 * @param q1 first quaternion
//	 * @param q2 second quaternion
//	 * @return the angle in degrees
//	 */
//	public static double quaternionAngleD( double[] q1, double q2 )
//	{
//
//	}
}
