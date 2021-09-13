/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2021 Howard Hughes Medical Institute.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package bdv.img;

import bdv.util.RandomAccessibleIntervalSource;
import mpicbg.spim.data.sequence.DefaultVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

/**
 * A {@link RandomAccessibleIntervalSource} with set-able {@link VoxelDimensions}.
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
