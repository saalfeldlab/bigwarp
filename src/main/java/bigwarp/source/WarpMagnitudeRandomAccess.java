package bigwarp.source;

import mpicbg.models.AbstractModel;
import mpicbg.models.CoordinateTransform;
import net.imglib2.AbstractRealLocalizable;
import net.imglib2.Localizable;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccess;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.RealType;

public class WarpMagnitudeRandomAccess< T extends RealType<T>> extends AbstractRealLocalizable implements RealRandomAccess< T >
{

	RealTransform warp;
	RealTransform baseline;
	
	T value;
	
	final double[] warpRes;
	final double[] baseRes;

	protected WarpMagnitudeRandomAccess( double[] dimensions )
	{
		this( dimensions, null, null, null );
	}
	
	protected WarpMagnitudeRandomAccess( double[] dimensions, T value, RealTransform warp, RealTransform baseline )
	{
		super( dimensions.length );
		if( warp != null)
		{
			this.warp = warp.copy();
		}

		if( baseline != null )
		{
			this.baseline = baseline.copy();
		}
		this.value = value;
		warpRes = new double[ numDimensions() ]; 
		baseRes = new double[ numDimensions() ]; 
	}

	@Override
	public T get() 
	{
		T out = value.copy();
		if( warp == null || baseline == null )
		{
			out.setZero();
			return out;
		}
					
		double[] mypt = new double[ this.numDimensions() ];
		this.localize( mypt );

		// apply the warp
		warp.apply( mypt, warpRes );

		// apply the baseline transform
		baseline.apply( mypt, baseRes );

		double dist = 0.0;
		for( int d = 0; d < warpRes.length; d++ )
			dist += ( warpRes[ d ] - baseRes[ d ] ) * ( warpRes[ d ] - baseRes[ d ] );  

		out.setReal( Math.sqrt( dist ));

		return out;
	}
	
	private boolean withinRad( double[] pt1, double[] pt2, double rad )
	{
		double radSquared = rad * rad;
		double distSquared = 0.0;
		for( int d = 0; d < pt2.length; d++ )
			distSquared += ( pt1[ d ] - pt2[ d ] ) * ( pt1[ d ] - pt2[ d ] );
		
		return distSquared  < radSquared;
	}

	public RealRandomAccess<T> copy() 
	{
		return new WarpMagnitudeRandomAccess< T >( new double[ position.length ], value.copy(), 
				warp, baseline );
	}

	public RealRandomAccess<T> copyRandomAccess() 
	{
		return copy();
	}
	
	@Override
	public RealRandomAccess<T> copyRealRandomAccess() 
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

}
