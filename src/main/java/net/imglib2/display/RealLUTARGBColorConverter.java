package net.imglib2.display;

import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

public class RealLUTARGBColorConverter< R extends RealType< ? > > extends RealARGBColorConverter< R >
{
	protected int[] lut;

	protected double scale;

	public RealLUTARGBColorConverter( final double min, final double max, final int[] lut )
	{
		super( min, max );
		this.lut = lut;
		update();
	}

	@Override
	public boolean supportsColor()
	{
		return false;
	}

	private void update()
	{
		scale = (lut.length - 1) / (max - min);
		black = lut[ 0 ];
	}

	@Override
	public void convert( final R input, final ARGBType output )
	{
		final int v = (int) Math.round( scale * (input.getRealDouble() - min) );
		if ( v < 0 )
		{
			output.set( black );
		} else if ( v > lut.length - 1 )
		{
			output.set( lut[ lut.length - 1 ] );
		} else
		{
			final int index = (int) Math.round( v );
			output.set( lut[ index ] );
		}
	}
}
