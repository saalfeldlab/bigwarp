package bigwarp.source;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bigwarp.BigWarp.BigWarpData;
import mpicbg.models.CoordinateTransform;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.RandomAccessibleOnRealRandomAccessible;
import net.imglib2.view.Views;

public class WarpMagnitudeSource< T extends RealType< T >> implements Source< T >
{
	protected final String name;

	protected CoordinateTransform warp;
	
	protected CoordinateTransform baseline;
	
	protected final BigWarpData sourceData;
	
	protected final Interval interval;
	
	protected final WarpMagnitudeRandomAccessibleInterval<T> warpMagImg;
	
	public WarpMagnitudeSource( String name, BigWarpData data, T t )
	{
		this.name = name;
		sourceData = data;
		
		// use the interval of the fixed image 
		interval = data.sources.get( 1 ).getSpimSource().getSource( 0, 0 ); 
		warpMagImg = new WarpMagnitudeRandomAccessibleInterval<T>( interval, t, null, null );
	}
	
	public void setWarp( CoordinateTransform warp )
	{
		warpMagImg.ra.warp = warp;
	}
	
	public void setBaseline( CoordinateTransform baseline )
	{
		warpMagImg.ra.base = baseline;
	}
	
	@Override
	public boolean isPresent( int t )
	{
		return ( t == 0 );
	}

	@Override
	public RandomAccessibleInterval<T> getSource( int t, int level ) 
	{
		return Views.interval( Views.raster( warpMagImg ), interval );
	}

	@Override
	public RealRandomAccessible<T> getInterpolatedSource( int t, int level, Interpolation method ) 
			{
		return warpMagImg;
	}

	@Override
	public void getSourceTransform( int t, int level, AffineTransform3D transform )
	{
		sourceData.sources.get( 0 ).getSpimSource().getSourceTransform( t, level, transform );
	}

	@Override
	public AffineTransform3D getSourceTransform(int t, int level)
	{
		return sourceData.sources.get( 0 ).getSpimSource().getSourceTransform( t, level );
	}

	@Override
	public T getType()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return sourceData.seq.getViewSetups().get( 0 ).getVoxelSize();
	}

	@Override
	public int getNumMipmapLevels() 
	{
		return 1;
	}
	
}
