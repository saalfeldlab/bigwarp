package bigwarp.source;

import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import mpicbg.models.CoordinateTransform;
import net.imglib2.AbstractLocalizable;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.RealType;

public class GridRandomAccess< T extends RealType<T>> extends AbstractLocalizable implements RandomAccess< T >
{

	CoordinateTransform warp;
	T value;
	
	protected GridRandomAccess( long[] dimensions )
	{
		this( dimensions, null, null );
	}
	
	protected GridRandomAccess( long[] dimensions, T value, CoordinateTransform warp )
	{
		super( dimensions.length );
		this.value = value;
	}

	@Override
	public T get() 
	{
		
		double[] mypt = new double[ this.numDimensions() ];
		this.localize( mypt );
		
		double[] warpRes = warp.apply( mypt );
		 
		T out = value.copy();
		
//		if( position[ 0 ] % 20 == 0 || 
//		    position[ 1 ] % 20 == 0  )
//			out.setReal( 255.0 );
//		else
//			out.setReal( 0.0 );
		
		out.setReal( warpRes[ 0 ] % 50 + 
					 warpRes[ 1 ] % 50);
			
//		// FOR DEBUG
//		out.setReal( position[ 0 ] / 10 );
		
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

	public RandomAccess<T> copy() 
	{
		return new GridRandomAccess< T >( new long[ position.length ], value.copy(), 
				((ThinPlateR2LogRSplineKernelTransform)warp) );
	}

	public RandomAccess<T> copyRandomAccess() 
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

}
