package net.imglib2.display;

import java.util.HashMap;
import java.util.HashSet;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.volatiles.VolatileARGBType;

public abstract class ARGBtoRandomARGBColorConverter<R> implements ColorConverter, Converter< R, ARGBType >
{
	protected double min = 0;

	protected double max = 1;

	protected final ARGBType color = new ARGBType( ARGBType.rgba( 0, 0, 0, 255 ) );

	protected int A;

	protected double scaleR = 1.0;

	protected double scaleG = 1.0;

	protected double scaleB = 1.0;
	
	protected int black = 0;
	
	protected HashMap<Integer,Integer> map;
	protected HashSet<Integer> colorset;
	
	public ARGBtoRandomARGBColorConverter( final double min, final double max )
	{
		this.min = min;
		this.max = max;
		map = new HashMap<Integer,Integer>(); 
		colorset = new HashSet<Integer>();
		
		update();
	}

	@Override
	public ARGBType getColor()
	{
		return color.copy();
	}

	@Override
	/**
	 * Sets the BACKGROUND color only.
	 */
	public void setColor( final ARGBType c )
	{
		color.set( c );
		update();
	}

	@Override
	public boolean supportsColor()
	{
		return true;
	}

	@Override
	public double getMin()
	{
		return min;
	}

	@Override
	public double getMax()
	{
		return max;
	}

	@Override
	public void setMax( final double max )
	{
		this.max = max;
		update();
	}

	@Override
	public void setMin( final double min )
	{
		this.min = min;
		update();
	}

	private void update()
	{
		final double scale = 1.0 / ( max - min );
		final int value = color.get();
		A = ARGBType.alpha( value );
//		scaleR = ARGBType.red( value ) * scale;
//		scaleG = ARGBType.green( value ) * scale;
//		scaleB = ARGBType.blue( value ) * scale;
		black = 0;
		
		map.clear();
		map.put( black, value );
		colorset.add( value );
	}
	
	int convertColor( final int color )
	{
		final int a = ARGBType.alpha( color );
		int r = ARGBType.red( color );
		int g = ARGBType.green( color );
		int b = ARGBType.blue( color );
		
		
//		final int v = Math.min( 255, Math.max( 0, (int)( scaleR * r + scaleG * g + scaleB * b ) / 3 ) );
		final int v = ( r + 256 * g + 512 * b );
		
		int argb = -1;
		if( !map.containsKey( v ))
		{
			map.put( v, newColor() );
		}
		
		if( argb >= 0 )
			return argb;
		else
			return map.get( v );
	}
	
	public int newColor(){
		int N = 1000;
		int i = 0;
		int argb = 0;
		while( i < N )
		{
			argb = ARGBType.rgba( 
					(int)Math.min( 255, 255 * Math.random() ),
					(int)Math.min( 255, 255 * Math.random() ),
					(int)Math.min( 255, 255 * Math.random() ),
					128 );
			
			if( !colorset.contains( argb ))
			{
				return argb;
			}
			
			i++;
		}
		return argb;
	}

	/**
	 * A converter from a ARGB to ARGB that initially converts the input color to grayscale, 
	 * then scales the resulting grayscale value with the set color. 
	 * <p>
	 * This can be useful if a grayscale image is imported as ARGB and one wants to change
	 * the hue for visualization / overlay.
	 * 
	 * @author John Bogovic
	 *
	 */
	public static class ToGray extends ARGBtoRandomARGBColorConverter<ARGBType>
	{
		public ToGray( final double min, final double max )
		{
			super( min, max );
		}

		@Override
		public void convert( final ARGBType input, final ARGBType output )
		{
			output.set( convertColor( input.get() ));
		}
	}
	
	public static class VolatileToGray extends ARGBtoRandomARGBColorConverter<VolatileARGBType>
	{
		public VolatileToGray( final double min, final double max )
		{
			super( min, max );
		}

		@Override
		public void convert( final VolatileARGBType input, final ARGBType output )
		{
			output.set( convertColor( input.get().get() ));
		}
	}
}
