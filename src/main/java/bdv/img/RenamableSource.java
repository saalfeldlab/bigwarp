/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2022 Howard Hughes Medical Institute.
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
	
	public Source<T> getWrappedSource()
	{
		return src;
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
	public synchronized void getSourceTransform( int t, int level, AffineTransform3D transform )
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
