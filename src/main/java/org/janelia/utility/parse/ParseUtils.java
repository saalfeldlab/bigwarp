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
package org.janelia.utility.parse;

import com.beust.jcommander.IStringConverter;

public class ParseUtils
{

	/**
	 * Parses a string of the form "#,#,#,...#" and returns a double array,
	 * with a specified delimiter.
	 * 
	 * @param in the string
	 * @param delimiter the delimiter
	 * @return the double array
	 */
	public static double[] parseDoubleArray( String in, String delimiter )
	{
		String[] ina = in.split( delimiter );
		double[] out = new double[ ina.length ];

		for ( int i = 0; i < ina.length; i++ )
			out[ i ] = Double.parseDouble( ina[ i ] );

		return out;
	}

	/**
	 * Parses a string of the form "#,#,#,...#" and returns a double array
	 * 
	 * @param in the string
	 * @return the double array
	 */
	public static double[] parseDoubleArray( String in )
	{
		return parseDoubleArray( in, "," );
	}

	/**
	 * Parses a string of the form "#,#,#,...#" and returns a double array,
	 * with a specified delimiter.
	 * 
	 * @param in the string
	 * @param delimiter the delimiter
	 * @return the float array
	 */
	public static float[] parseFloatArray( String in, String delimiter )
	{
		String[] ina = in.split( delimiter );
		float[] out = new float[ ina.length ];

		for ( int i = 0; i < ina.length; i++ )
			out[ i ] = Float.parseFloat( ina[ i ] );

		return out;
	}

	/**
	 * Parses a string of the form "#,#,#,...#" and returns a float array
	 * 
	 * @param in the string
	 * @return the float array
	 */
	public static float[] parseFloatArray( String in )
	{
		return parseFloatArray( in, "," );
	}

	/**
	 * Parses a string of the form "#,#,#,...#" and returns an int array
	 * 
	 * @param in the string
	 * @param delimiter the delimiter
	 * @return the int array
	 */
	public static int[] parseIntArray( String in, String delimiter )
	{
		String[] ina = in.split( delimiter );
		int[] out = new int[ ina.length ];

		for ( int i = 0; i < ina.length; i++ )
			out[ i ] = Integer.parseInt( ina[ i ] );

		return out;
	}

	/**
	 * Parses a string of the form "#,#,#,...#" and returns a int array
	 * 
	 * @param in the string
	 * @return the int array
	 */
	public static int[] parseIntArray( String in )
	{
		return parseIntArray( in, "," );
	}

	/**
	 * Parses a string of the form "#,#,#,...#" and returns a long array
	 * 
	 * @param in the string
	 * @param delimiter the delimiter
	 * @return the long array
	 */
	public static long[] parseLongArray( String in, String delimiter )
	{
		String[] ina = in.split( delimiter );
		long[] out = new long[ ina.length ];
		System.out.println( ina.length );
		for ( int i = 0; i < ina.length; i++ )
		{
			System.out.println( ina[i] );
			out[ i ] = Long.parseLong( ina[ i ] );
		}
		return out;
	}

	/**
	 * Parses a string of the form "#,#,#,...#" and returns a long array
	 * 
	 * @param in the string
	 * @return the long array
	 */
	public static long[] parseLongArray( String in )
	{
		return parseLongArray( in, "," );
	}

	public static class DoubleArrayConverter implements IStringConverter< double[] >
	{
		@Override
		public double[] convert( String value )
		{
			return parseDoubleArray( value );
		}
	}

	public static class FloatArrayConverter implements IStringConverter< float[] >
	{
		@Override
		public float[] convert( String value )
		{
			return parseFloatArray( value );
		}
	}

	public static class IntArrayConverter implements IStringConverter< int[] >
	{
		@Override
		public int[] convert( String value )
		{
			return parseIntArray( value );
		}
	}

	public static class LongArrayConverter implements IStringConverter< long[] >
	{
		@Override
		public long[] convert( String value )
		{
			return parseLongArray( value );
		}
	}
}
