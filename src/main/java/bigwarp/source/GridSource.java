package bigwarp.source;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bigwarp.BigWarp.BigWarpData;
import mpicbg.models.CoordinateTransform;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class GridSource< T extends RealType< T >> implements Source< T >
{
	
	public enum GRID_TYPE { MOD, LINE };
	
	protected final String name;
	
	protected final BigWarpData sourceData;
	
	protected final Interval interval;
	
//	protected final GridRandomAccessibleInterval<T> gridImg;
	protected final GridRealRandomAccessibleRealInterval<T> gridImg;
	
	protected T type;
	
	public GridSource( String name, BigWarpData data, T t, CoordinateTransform warp  )
	{
		this.name = name;
		this.type = t.copy();
		sourceData = data;
		
		RandomAccessibleInterval<?> fixedsrc = data.sources.get( 1 ).getSpimSource().getSource( 0, 0 );
		interval = fixedsrc;
		
		gridImg = new GridRealRandomAccessibleRealInterval<T>( interval, t, warp );
	}
	
	public void setGridSpacing( double spacing )
	{
		gridImg.ra.setGridSpacing( spacing );
	}
	
	public void setGridWidth( double width )
	{
		gridImg.ra.setGridWidth( width );
	}
	
//	public void debug( long[] pt )
//	{
////		RandomAccess<T> rra = gridImg.randomAccess();
////		RealRandomAccess<T> rra = gridImg.randomAccess();
//		
//		rra.setPosition( pt );
//		
//		System.out.println("at ( 0 0 0 ): ");
//		System.out.println( "get val: " + rra.get());
//		
//	}
	
	public void setWarp( CoordinateTransform warp )
	{
		gridImg.ra.warp = warp;
	}
	
	@Override
	public boolean isPresent( int t )
	{
		return ( t == 0 );
	}
	
	public void setMethod( GRID_TYPE method )
	{
		gridImg.ra.setMethod( method );
	}

	@Override
	public RandomAccessibleInterval<T> getSource( int t, int level ) 
	{
		return Views.interval( Views.raster( 
				getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR ) ), 
				interval );
	}

	@Override
	public RealRandomAccessible<T> getInterpolatedSource( int t, int level, Interpolation method ) 
	{
		return gridImg;
	}

	@Override
	public void getSourceTransform( int t, int level, AffineTransform3D transform )
	{
		sourceData.sources.get( 0 ).getSpimSource().getSourceTransform( t, level, transform );
	}
	
//	@Override
//	public RealRandomAccessible<T> getInterpolatedSource( int t, int level, Interpolation method ) 
//	{
//		return Views.interpolate( getSource( t, level ), new NearestNeighborInterpolatorFactory<T>() );
//	}
//
//	@Override
//	public void getSourceTransform( int t, int level, AffineTransform3D transform )
//	{
//		sourceData.sources.get( 0 ).getSpimSource().getSourceTransform( t, level, transform );
//	}

	@Override
	public T getType()
	{
		return type;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return sourceData.seqQ.getViewSetups().get( 0 ).getVoxelSize();
	}

	@Override
	public int getNumMipmapLevels() 
	{
		return 1;
	}
	
}
