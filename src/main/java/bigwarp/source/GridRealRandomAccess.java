package bigwarp.source;

import bigwarp.source.GridSource.GRID_TYPE;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import mpicbg.models.CoordinateTransform;
import net.imglib2.AbstractRealLocalizable;
import net.imglib2.Localizable;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccess;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;


public class GridRealRandomAccess< T extends RealType<T>> extends AbstractRealLocalizable implements RealRandomAccess< T >
{

	protected CoordinateTransform warp;

	private T value;
	private GRID_TYPE method = GRID_TYPE.MOD;

	private double gridSpacing = 20;
	private double gridWidth = 2.0;
	
	private boolean is2d = false;
	
	protected GridRealRandomAccess( double[] dimensions )
	{
		this( dimensions, null, null );
	}

	protected GridRealRandomAccess( double[] dimensions, T value, CoordinateTransform warp )
	{
		this( dimensions, value, warp, GRID_TYPE.MOD );
	}

	protected GridRealRandomAccess( double[] dimensions, T value, CoordinateTransform warp, GRID_TYPE method )
	{
		super( dimensions.length );
		this.value = value;
		this.warp = warp;
		this.method = method;
		is2d = ( dimensions[2] == 0 );
	}
	
	
	public static void main( String[] args )
	{
		GridRealRandomAccess<DoubleType> ra = new GridRealRandomAccess<DoubleType>( new double[]{ 100.0, 100.0}, new DoubleType(), null );
		ra.setMethod( GRID_TYPE.LINE );
		
		for( double x = 2.1; x < 5.0; x+= 1.0 ) for( double y = 0.0; y < 5.0; y+= 0.5 )
		{
			ra.setPosition( new double[]{x,y});
			System.out.println("("+x+","+y+") : " + ra.get() );
		}
		
		
	}
	

	public void setMethod( GRID_TYPE method )
	{
		this.method = method;
	}

	@Override
	public T get() 
	{
		double[] mypt = new double[ this.numDimensions() ];
		this.localize( mypt );
		
		switch( method )
		{
		case LINE:
			return getLine( mypt );
		default:
			return getMod( mypt );
		}
	}

	private T getLine( double[] pt )
	{
		double[] warpRes;
		if( warp != null )
			warpRes = warp.apply( pt );
		else
			warpRes = pt;

		boolean ongrid = false;
		
		int nd = warpRes.length;
		if( is2d )
			nd = 2;
		
		for( int d = 0; d < nd; d++ )
		{
			double tmp = warpRes[ d ] % gridSpacing;
			if( tmp < 0 ) tmp *= -1;

			if( tmp <= gridWidth )
			{
				ongrid = true;
				break;
			}
		}

		T out = value.copy();
		if( ongrid )
			out.setReal(  255.0 );
		else
			out.setZero();

		return out;
	}

	private T getMod( double[] pt )
	{
		double[] warpRes;
		if( warp != null )
			warpRes = warp.apply( pt );
		else
			warpRes = pt;
		
		double val = 0.0;
		for( int d = 0; d < warpRes.length; d++ )
		{
			double tmp = warpRes[ d ] % gridSpacing;
			if( tmp < 0 ) tmp *= -1;

			val += tmp;
		}
		T out = value.copy();
		out.setReal( val );
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

	@Override
	public RealRandomAccess<T> copyRealRandomAccess()
	{
		return copy();
	}
	
	public RealRandomAccess<T> copy() 
	{
		if( warp == null )
		{
			return new GridRealRandomAccess< T >( new double[ position.length ], value.copy(), 
					null, this.method  );
		}
		else
		{
			return new GridRealRandomAccess< T >( new double[ position.length ], value.copy(), 
					((ThinPlateR2LogRSplineKernelTransform)warp).deepCopy(), this.method  );
		}

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

}
