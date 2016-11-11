package bdv.img;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;

public class RenamableSource< T > implements Source< T >
{
	private final Source<T> src;
	private String name;
	
	public RenamableSource( Source<T> src )
	{
		this.src = src;
		this.name = src.getName();
	}

	public RenamableSource( Source<T> src, String name )
	{
		this.src = src;
		this.name = name;
	}

	@Override
	public boolean isPresent( int t )
	{
		return src.isPresent( t );
	}

	@Override
	public RandomAccessibleInterval< T > getSource( int t, int level )
	{
		return src.getSource( t, level );
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( int t, int level, Interpolation method )
	{
		return src.getInterpolatedSource( t, level, method );
	}

	@Override
	public void getSourceTransform( int t, int level, AffineTransform3D transform )
	{
		src.getSourceTransform( t, level, transform );
	}

	@Override
	public T getType()
	{
		return src.getType();
	}

	public void setName( String name )
	{
		this.name = name;
	}
	
	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return src.getVoxelDimensions();
	}

	@Override
	public int getNumMipmapLevels()
	{
		return src.getNumMipmapLevels();
	}

}
