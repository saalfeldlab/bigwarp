package bdv.img;

import bdv.util.RandomAccessibleIntervalSource;
import mpicbg.spim.data.sequence.DefaultVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

/**
 * A {@link RandomAccessibleIntervalSource} with set-able {@link VoxelDimension}. 
 * 
 * @author John Bogovic
 *
 * @param <T>
 */
public class BwRandomAccessibleIntervalSource< T extends NumericType< T > > extends RandomAccessibleIntervalSource<T>
{
	final VoxelDimensions voxelDims;

	public BwRandomAccessibleIntervalSource( final RandomAccessibleInterval<T> img,
			final T type,
			final AffineTransform3D sourceTransform,
			final String name )
	{
		this( img, type, sourceTransform, name, new DefaultVoxelDimensions( img.numDimensions() ) );
	}

	public BwRandomAccessibleIntervalSource( final RandomAccessibleInterval<T> img,
			final T type,
			final AffineTransform3D sourceTransform,
			final String name,
			final VoxelDimensions voxelDims )
	{
		super( img, type, sourceTransform, name );
		this.voxelDims = voxelDims;
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return voxelDims;
	}
}
