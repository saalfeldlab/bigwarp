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
package bigwarp.scripts;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import mpicbg.models.Model;
import net.imglib2.realtransform.RealTransform;
import org.janelia.utility.geom.GeomUtils;

/**
 * Shared support for the landmark comparison scripts.
 * <p>
 * {@link CompareLandmarks} holds the transformation type fixed and varies the
 * landmark set; {@link CompareTransforms} holds the landmark set fixed and
 * varies the transformation type. Both load landmark csv files, solve
 * transformations, measure distances between them at sample points, and print
 * the same kind of table, so that machinery lives here.
 *
 */
public class CompareUtils
{

	private CompareUtils()
	{}

	/**
	 * The transformation types that {@link BigWarpTransform} can solve.
	 * <p>
	 * An enum rather than a raw String because {@code BigWarpTransform} does not
	 * validate the type it is given - an unrecognized name yields a null model
	 * and a NullPointerException later on. picocli validates enum values and
	 * lists them in the usage message.
	 */
	public enum TransformType
	{
		TPS( BigWarpTransform.TPS ),
		AFFINE( BigWarpTransform.AFFINE ),
		SIMILARITY( BigWarpTransform.SIMILARITY ),
		ROTATION( BigWarpTransform.ROTATION ),
		TRANSLATION( BigWarpTransform.TRANSLATION );

		private final String bigwarpName;

		private TransformType( final String bigwarpName )
		{
			this.bigwarpName = bigwarpName;
		}

		public String bigwarpName()
		{
			return bigwarpName;
		}
	}

	/**
	 * Loads a landmark csv, inferring its dimensionality from the file.
	 *
	 * @param f the csv file
	 * @return the landmark table
	 * @throws Exception if the file is missing, empty, or not a landmark csv
	 */
	public static LandmarkTableModel loadLandmarks( final File f ) throws Exception
	{
		if ( !f.exists() )
			throw new IllegalArgumentException( "no such file: " + f );

		if ( f.length() == 0 )
			throw new IllegalArgumentException( "file is empty: " + f );

		// loadFromCsv infers ndims from the column count, and returns null
		// if the file has neither 6 (2d) nor 8 (3d) columns
		final LandmarkTableModel ltm = LandmarkTableModel.loadFromCsv( f, false );
		if ( ltm == null )
			throw new IllegalArgumentException(
					"not a landmark csv (expected 6 columns for 2d or 8 for 3d): " + f );

		return ltm;
	}

	/**
	 * The number of landmarks needed to solve the given transformation.
	 *
	 * @param bwt the transform
	 * @param ndims the dimensionality
	 * @return the minimum number of landmarks
	 */
	public static int minimumLandmarks( final BigWarpTransform bwt, final int ndims )
	{
		// getModelType returns null for the thin plate spline, which is not an
		// mpicbg model; a tps needs enough points for its affine part
		final Model< ? > model = bwt.getModelType();
		return model == null ? ndims + 1 : model.getMinNumMatches();
	}

	public static boolean checkEnoughLandmarks( final LandmarkTableModel ltm, final File f, final int minPts )
	{
		final int n = ltm.getActiveRowCount();
		if ( n < minPts )
		{
			// the model solvers swallow NotEnoughDataPointsException and return
			// an unfit model, so this has to be caught before solving
			System.err.println( String.format(
					"error: %s has %d active landmarks but at least %d are needed", f, n, minPts ) );
			return false;
		}
		return true;
	}

	/**
	 * Returns the fixed (target) point of every landmark that constrains the
	 * transformation, that is, every row that is active and paired.
	 * <p>
	 * Inactive rows are excluded because the solvers ignore them, and unpaired
	 * rows because they are stored with infinite placeholder coordinates.
	 *
	 * @param ltm the landmark table
	 * @return the landmarks
	 */
	public static List< Landmark > fixedLandmarks( final LandmarkTableModel ltm )
	{
		final int nd = ltm.getNumdims();
		final List< String > names = ltm.getNames();
		final List< Landmark > out = new ArrayList<>();
		for ( int i = 0; i < ltm.getRowCount(); i++ )
		{
			if ( !ltm.isActive( i ) || ltm.isRowUnpaired( i ) )
				continue;

			// getFixedPoint returns the live array, so copy it
			final Double[] p = ltm.getFixedPoint( i );
			final double[] pos = new double[ nd ];
			for ( int d = 0; d < nd; d++ )
				pos[ d ] = p[ d ];

			out.add( new Landmark( names.get( i ), pos ) );
		}
		return out;
	}

	/**
	 * Returns the moving point of every landmark that constrains the
	 * transformation, using the same filter as {@link #fixedLandmarks}, so that
	 * the two lists are index aligned.
	 *
	 * @param ltm the landmark table
	 * @return the landmarks
	 */
	public static List< Landmark > movingLandmarks( final LandmarkTableModel ltm )
	{
		final int nd = ltm.getNumdims();
		final List< String > names = ltm.getNames();
		final List< Landmark > out = new ArrayList<>();
		for ( int i = 0; i < ltm.getRowCount(); i++ )
		{
			if ( !ltm.isActive( i ) || ltm.isRowUnpaired( i ) )
				continue;

			// getMovingPoint returns the live array, so copy it
			final Double[] p = ltm.getMovingPoint( i );
			final double[] pos = new double[ nd ];
			for ( int d = 0; d < nd; d++ )
				pos[ d ] = p[ d ];

			out.add( new Landmark( names.get( i ), pos ) );
		}
		return out;
	}

	/**
	 * Distances between the two transformations at the given points.
	 *
	 * @param pts the points at which to evaluate
	 * @param tA the first transformation
	 * @param tB the second transformation
	 * @param ndims the dimensionality
	 * @return the distances, in the order of {@code pts}
	 */
	public static double[] distances( final List< Landmark > pts, final RealTransform tA,
			final RealTransform tB, final int ndims )
	{
		final double[] a = new double[ ndims ];
		final double[] b = new double[ ndims ];
		final double[] out = new double[ pts.size() ];
		for ( int i = 0; i < pts.size(); i++ )
		{
			final double[] x = pts.get( i ).position;
			tA.apply( x, a );
			tB.apply( x, b );
			out[ i ] = Math.sqrt( GeomUtils.squaredDistance( a, b ) );
		}
		return out;
	}

	public static double[] concatenate( final double[] x, final double[] y )
	{
		final double[] out = Arrays.copyOf( x, x.length + y.length );
		System.arraycopy( y, 0, out, x.length, y.length );
		return out;
	}

	/**
	 * Summary statistics of a set of distances.
	 *
	 * @param d the distances
	 * @return the statistics
	 */
	public static Stats stats( final double[] d )
	{
		if ( d.length == 0 )
			return new Stats( 0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN );

		// sort a copy so the caller's array keeps its order
		final double[] sorted = d.clone();
		Arrays.sort( sorted );

		double sum = 0;
		double sumSq = 0;
		for ( final double x : d )
		{
			sum += x;
			sumSq += x * x;
		}

		final int n = d.length;
		final double median = ( n % 2 == 1 ) ? sorted[ n / 2 ]
				: 0.5 * ( sorted[ n / 2 - 1 ] + sorted[ n / 2 ] );

		return new Stats( n, sum / n, median, sorted[ 0 ], sorted[ n - 1 ], Math.sqrt( sumSq / n ) );
	}

	public static final String[] SUMMARY_HEADER =
			new String[] { "# group", "n", "mean", "median", "min", "max", "rms" };

	public static final boolean[] SUMMARY_ALIGNMENT =
			new boolean[] { false, true, true, true, true, true, true };

	public static String[] pointHeader( final int ndims )
	{
		final String[] header = new String[ ndims + 3 ];
		header[ 0 ] = "# group";
		header[ 1 ] = "name";
		for ( int d = 0; d < ndims; d++ )
			header[ d + 2 ] = String.valueOf( ( char ) ( 'x' + d ) );

		header[ ndims + 2 ] = "distance";
		return header;
	}

	public static boolean[] pointAlignment( final int ndims )
	{
		// group and name read left to right, the numbers line up on the right
		final boolean[] align = new boolean[ ndims + 3 ];
		Arrays.fill( align, true );
		align[ 0 ] = false;
		align[ 1 ] = false;
		return align;
	}

	public static void addPointRows( final List< String[] > rows, final String group,
			final List< Landmark > pts, final double[] dist, final int digits )
	{
		for ( int i = 0; i < pts.size(); i++ )
		{
			final Landmark p = pts.get( i );
			final String[] row = new String[ p.position.length + 3 ];
			row[ 0 ] = group;
			row[ 1 ] = p.name;
			for ( int d = 0; d < p.position.length; d++ )
				row[ d + 2 ] = format( p.position[ d ], digits );

			row[ p.position.length + 2 ] = format( dist[ i ], digits );
			rows.add( row );
		}
	}

	public static String[] summaryRow( final String group, final Stats s, final int digits )
	{
		return new String[] { group, Integer.toString( s.n ),
				format( s.mean, digits ), format( s.median, digits ),
				format( s.min, digits ), format( s.max, digits ), format( s.rms, digits ) };
	}

	public static String format( final double x, final int digits )
	{
		// Locale.ROOT so that the decimal separator is always a point
		return String.format( Locale.ROOT, "%." + digits + "f", x );
	}

	/**
	 * Prints rows in columns wide enough for their contents.
	 *
	 * @param rows the rows, the first of which is the header
	 * @param rightAlign whether each column is right aligned
	 */
	public static void printTable( final List< String[] > rows, final boolean[] rightAlign )
	{
		final int ncol = rightAlign.length;
		final int[] width = new int[ ncol ];
		for ( final String[] row : rows )
			for ( int c = 0; c < ncol; c++ )
				width[ c ] = Math.max( width[ c ], row[ c ].length() );

		final StringBuilder sb = new StringBuilder();
		for ( final String[] row : rows )
		{
			sb.setLength( 0 );
			for ( int c = 0; c < ncol; c++ )
			{
				if ( c > 0 )
					sb.append( "  " );

				pad( sb, row[ c ], width[ c ], rightAlign[ c ] );
			}

			// the last column may be left aligned, so do not leave it padded
			while ( sb.length() > 0 && sb.charAt( sb.length() - 1 ) == ' ' )
				sb.setLength( sb.length() - 1 );

			System.out.println( sb );
		}
	}

	private static void pad( final StringBuilder sb, final String s, final int width,
			final boolean rightAlign )
	{
		final int n = width - s.length();
		if ( rightAlign )
		{
			for ( int i = 0; i < n; i++ )
				sb.append( ' ' );

			sb.append( s );
		}
		else
		{
			sb.append( s );
			for ( int i = 0; i < n; i++ )
				sb.append( ' ' );
		}
	}

	/**
	 * A named point.
	 */
	public static class Landmark
	{
		public final String name;

		public final double[] position;

		public Landmark( final String name, final double[] position )
		{
			this.name = name;
			this.position = position;
		}
	}

	/**
	 * Summary statistics of a set of distances.
	 */
	public static class Stats
	{
		public final int n;

		public final double mean;

		public final double median;

		public final double min;

		public final double max;

		public final double rms;

		public Stats( final int n, final double mean, final double median,
				final double min, final double max, final double rms )
		{
			this.n = n;
			this.mean = mean;
			this.median = median;
			this.min = min;
			this.max = max;
			this.rms = rms;
		}
	}

}
