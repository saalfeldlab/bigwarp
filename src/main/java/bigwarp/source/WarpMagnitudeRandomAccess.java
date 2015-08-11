package bigwarp.source;

import mpicbg.models.CoordinateTransform;
import net.imglib2.AbstractRealLocalizable;
import net.imglib2.Localizable;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccess;
import net.imglib2.type.numeric.RealType;

public class WarpMagnitudeRandomAccess< T extends RealType<T>> extends AbstractRealLocalizable implements RealRandomAccess< T >
{

	CoordinateTransform warp;
	CoordinateTransform base; 
	
	double[] pt;
	T value;
	
	protected WarpMagnitudeRandomAccess( double[] dimensions )
	{
		this( dimensions, null, null, null );
	}
	
	protected WarpMagnitudeRandomAccess( double[] dimensions, T value, CoordinateTransform warp, CoordinateTransform base )
	{
		super( dimensions );
		this.warp = warp;
		this.base = base;
		this.value = value;
		
		pt = new double[ dimensions.length ];
	}

	@Override
	public T get() 
	{
		this.localize( pt );
		
		double[] warpRes = warp.apply( pt );
		double[] baseRes = base.apply( pt );
		
		double dist = 0.0;
		for( int i = 0; i < this.numDimensions(); i++ )
			dist += ( warpRes[ i ] - baseRes[ i ] ) * ( warpRes[ i ] - baseRes[ i ] );  
		
		dist = Math.sqrt( dist );
		value.setReal( dist );
		
		return value;
	}

	public RealRandomAccess<T> copy() 
	{
		return new WarpMagnitudeRandomAccess< T >( position.clone(), value.copy(), warp, base );
	}

	public RealRandomAccess<T> copyRandomAccess() 
	{
		return copy();
	}

	@Override
	public void fwd( final int d )
	{
		++position[ d ];
	}

	@Override
	public void bck( final int d )
	{
		--position[ d ];
	}

	@Override
	public void move( final int distance, final int d )
	{
		position[ d ] += distance;
	}

	@Override
	public void move( final long distance, final int d )
	{
		position[ d ] += distance;
	}

	@Override
	public void move( final Localizable localizable )
	{
		for ( int d = 0; d < n; ++d )
		{
			final int distance = localizable.getIntPosition( d );
			position[ d ] += distance;
		}
	}

	@Override
	public void move( final int[] distance )
	{
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] += distance[ d ];
		}
	}

	@Override
	public void move( final long[] distance )
	{
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] += distance[ d ];
		}
	}

	@Override
	public void setPosition( final Localizable localizable )
	{
		localizable.localize( position );
	}

	@Override
	public void setPosition( final int[] pos )
	{
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] = pos[ d ];
		}
	}

	@Override
	public void setPosition( final long[] pos )
	{
		for ( int d = 0; d < n; ++d )
		{
			final int p = ( int ) pos[ d ];
			position[ d ] = p;
		}
	}

	@Override
	public void setPosition( final int pos, final int d )
	{
		position[ d ] = pos;
	}

	@Override
	public void setPosition( final long pos, final int d )
	{
		position[ d ] = ( int ) pos;
	}

	@Override
	public void move(float distance, int d) {
		position[ d ] += distance;
	}

	@Override
	public void move(double distance, int d) {
		position[ d ] += distance;
	}

	@Override
	public void move(RealLocalizable localizable) {
		for ( int d = 0; d < n; ++d )
		{
			final double distance = localizable.getDoublePosition( d );
			position[ d ] += distance;
		}
	}

	@Override
	public void move(float[] distance) {
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] += distance[ d ];
		}
	}

	@Override
	public void move(double[] distance) {
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] += distance[ d ];
		}
	}

	@Override
	public void setPosition(RealLocalizable localizable) {
		for ( int d = 0; d < n; ++d )
		{
			final double pos = localizable.getDoublePosition( d );
			position[ d ] = pos;
		}
	}

	@Override
	public void setPosition(float[] p) {
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] = p[ d ];
		}
	}

	@Override
	public void setPosition(double[] p) {
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] = p[ d ];
		}	
	}

	@Override
	public void setPosition(float p, int d) {
		position[ d ] = p;
	}

	@Override
	public void setPosition(double p, int d) {
		position[ d ] = p;
	}

	@Override
	public RealRandomAccess<T> copyRealRandomAccess() {
		// TODO Auto-generated method stub
		return null;
	}

	
}
