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
package bigwarp.source;

import java.util.Arrays;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bigwarp.BigWarp.BigWarpData;
import bigwarp.landmarks.LandmarkTableModel;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.inverse.DifferentiableRealTransform;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class JacobianDeterminantSource< T extends RealType< T >> implements Source< T >
{
	protected final String name;
	
	protected final BigWarpData<?> sourceData;
	
	protected final Interval interval;
	
	protected final JacobianDeterminantRandomAccess.JacobianDeterminantRandomAccessibleInterval< T > jacDetImg;
	
	protected final VoxelDimensions voxDims;

	protected T type;
	
	public JacobianDeterminantSource( String name, BigWarpData<?> data, T t  )
	{
		this.name = name;
		this.type = t;

		sourceData = data;

		//RandomAccessibleInterval<?> fixedsrc = sourceData.sources.get( 1 ).getSpimSource().getSource( 0, 0 );
//		interval = sourceData.sources.get( sourceData.targetSourceIndices[ 0 ] ).getSpimSource().getSource( 0, 0 );
		interval = sourceData.getTargetSource( 0 ).getSpimSource().getSource( 0, 0 );

//		VoxelDimensions srcVoxDims = sourceData.sources.get( sourceData.targetSourceIndices[ 0 ] ).getSpimSource().getVoxelDimensions();
		VoxelDimensions srcVoxDims = sourceData.getTargetSource( 0 ).getSpimSource().getVoxelDimensions();
		String unit = "pix";
		if( srcVoxDims != null )
			unit = srcVoxDims.unit();

		voxDims = new FinalVoxelDimensions( unit, 1.0, 1.0, 1.0 );

		jacDetImg = new JacobianDeterminantRandomAccess.JacobianDeterminantRandomAccessibleInterval< T >( interval, t, null );
	}
	
	public double getMax( LandmarkTableModel lm )
	{
		double maxVal = 0.0;
		
		int ndims = lm.getNumdims();
		double[] pt = new double[ ndims ];
		
		for( Double[] movingPt : lm.getPoints( true ) )
		{
			for( int d = 0; d < ndims; d++ )
				pt[ d ] = movingPt[ d ];
			
			jacDetImg.ra.setPosition(  pt );
			double val = jacDetImg.ra.get().getRealDouble();
			
			if( val > maxVal )
				maxVal = val;
		}
		return maxVal;
	}
	
	public void setTransform( final DifferentiableRealTransform transform )
	{
		jacDetImg.setTransform( transform );
	}
	
	public void debug( double[] pt )
	{
		RealRandomAccess<T> rra = jacDetImg.realRandomAccess();
		
		rra.setPosition( pt );
		
		System.out.println("at : " + Arrays.toString( pt ) );
		System.out.println( "get val: " + rra.get());

//		double[] warpRes = new double[ jacDetImg.ra.warp.numTargetDimensions() ]; 
//		jacDetImg.ra.warp.apply( pt, warpRes );
//
//		System.out.println( "base res: " + baseRes[0] + " " + baseRes[1]);
//		System.out.println( "warp res: " + warpRes[0] + " " + warpRes[1]);
		
	}

	public double[] minMax()
	{
		double[] minmax = new double[ 2 ];
		minmax[ 0 ] = Double.MAX_VALUE;
		minmax[ 1 ] = Double.MIN_VALUE;
		
		Cursor<T> curs = Views.iterable( this.getSource( 0,0 ) ).cursor();
		
		while( curs.hasNext() )
		{
			double val = curs.next().getRealDouble();
			if( val < minmax[ 0 ])
				minmax[ 0 ] = val;
			else if( val > minmax[ 1 ])
				minmax[ 1 ] = val;
			
		}
		return minmax;
	}
	
	@Override
	public boolean isPresent( int t )
	{
		return ( t == 0 );
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
		return jacDetImg;
	}

	@Override
	public void getSourceTransform( int t, int level, AffineTransform3D transform )
	{
		transform.identity();
	}

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
		return voxDims;
	}

	@Override
	public int getNumMipmapLevels() 
	{
		return 1;
	}
	
}
