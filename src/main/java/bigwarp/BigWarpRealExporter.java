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
package bigwarp;

import java.util.ArrayList;
import java.util.List;

import bdv.export.ProgressWriter;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class BigWarpRealExporter< T extends RealType< T > & NativeType< T >  > extends BigWarpExporter<T>
{

	final private T baseType;

	public BigWarpRealExporter(
			final BigWarpData<T> bwData,
			final List< ConverterSetup > convSetups,
			final Interpolation interp,
			final T baseType,
			final boolean needConversion,
			final ProgressWriter progress )
	{
		super( bwData, convSetups, interp, progress );
		this.baseType = baseType;
	}

	public BigWarpRealExporter(
			final BigWarpData<T> bwData,
			final List< ConverterSetup > convSetups,
			final Interpolation interp,
			final T baseType,
			final ProgressWriter progress )
	{
		this( bwData, convSetups, interp, baseType, false, progress );
	}

	/**
	 * Returns true if moving image sources are all of the same type.
	 *
	 * @param sources the sources
	 * @param <T> the type
	 * @param movingSourceIndexList list of indexes for moving sources
	 * @return true if all moving sources are of the same type
	 */
	public static <T> boolean isTypeListFullyConsistent( List< SourceAndConverter< T >> sources, int[] movingSourceIndexList )
	{
		final Object baseType = sources.get( movingSourceIndexList[ 0 ] ).getSpimSource().getType();

		for ( int i = 1; i < movingSourceIndexList.length; i++ )
		{
			final int idx = movingSourceIndexList[ i ];
			final Object type = sources.get( idx ).getSpimSource().getType();

			if ( !baseType.getClass().equals( type.getClass() ) )
				return false;
		}

		return true;
	}

	/**
	 * Returns true if moving image sources are all of the same type.
	 *
	 * @param sources the sources
	 * @param <T> the type
	 * @param movingSourceIndexList list of indexes for moving sources
	 * @return true if all moving sources are of the same type
	 */
	public static <T> boolean isTypeListFullyConsistent( List< SourceAndConverter< T >> sources, List<Integer> movingSourceIndexList )
	{
		final Object baseType = sources.get( movingSourceIndexList.get( 0 ) ).getSpimSource().getType();

		for ( int i = 1; i < movingSourceIndexList.size(); i++ )
		{
			final int idx = movingSourceIndexList.get( i );
			final Object type = sources.get( idx ).getSpimSource().getType();

			if ( !baseType.getClass().equals( type.getClass() ) )
				return false;
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public RandomAccessibleInterval< T > exportRai()
	{
		final ArrayList< RandomAccessibleInterval< T > > raiList = new ArrayList< RandomAccessibleInterval< T > >();

		final int numChannels;
		if( singleChannelNoStack )
			numChannels = 1;
		else
			numChannels = bwData.numMovingSources();

		for ( int i = 0; i < numChannels; i++ )
			raiList.add( (RandomAccessibleInterval<T>)exportRai( bwData.getMovingSourceForExport( i ).getSpimSource()));

		if( singleChannelNoStack )
			return raiList.get(0);
		else {
			return Views.stack( raiList );
		}
	}

	@Override
	public RandomAccessibleInterval<?> exportRai(Source<?> src) {

		buildTotalRenderTransform();
		final AffineTransform3D srcXfm = new AffineTransform3D();
		src.getSourceTransform(0, 0, srcXfm);

		// in pixel space
		@SuppressWarnings("unchecked")
		final RealRandomAccessible<T> raiRaw = (RealRandomAccessible<T>)src.getInterpolatedSource(0, 0, interp);

		// the transform from world to new pixel coordinates
		final AffineTransform3D pixelToPhysical = pixelRenderToPhysical.copy().inverse();

		// but first need to transform from original pixel to world coordinates
		pixelToPhysical.concatenate(srcXfm);

		// apply the transformations
		final AffineRandomAccessible<T, AffineGet> rai = RealViews.affine(raiRaw, pixelToPhysical);

		return Views.interval(Views.raster(rai), outputInterval);

	}

	@Override
	public boolean isRGB()
	{
		return false;
	}

	@Override
	public ImagePlus export()
	{
		final int numChannels = bwData.numMovingSources();
		final RandomAccessibleInterval< T > raiStack = exportRai();

		final VoxelDimensions voxdim = new FinalVoxelDimensions( unit,
				resolutionTransform.get( 0, 0 ),
				resolutionTransform.get( 1, 1 ),
				resolutionTransform.get( 2, 2 ));

		ImagePlus ip = null;
		if ( isVirtual )
		{
			ip = ImageJFunctions.wrap(
					Views.translateInverse( raiStack, Intervals.minAsLongArray( raiStack )),
					"warped_moving_image" );

			ip.setDimensions( numChannels, (int)raiStack.dimension( 2 ), 1 );
		}
		else if( nThreads == 1 )
		{
			ip = copyToImageStack( raiStack, raiStack, progress );
		}
		else
		{
			final ImagePlusImgFactory< T > factory = new ImagePlusImgFactory< T >( baseType );

			if ( outputInterval.numDimensions() == 3 )
			{
				// A bit of hacking to make slices the 4th dimension and
				// channels the 3rd since that's how ImagePlusImgFactory does it
				final long[] dimensions = new long[ 4 ];
				dimensions[ 0 ] = outputInterval.dimension( 0 );	// x
				dimensions[ 1 ] = outputInterval.dimension( 1 );	// y
				dimensions[ 2 ] = numChannels; 						// c
				dimensions[ 3 ] = outputInterval.dimension( 2 ); 	// z
				final FinalInterval destIntervalPerm = new FinalInterval( dimensions );
				final RandomAccessibleInterval< T > img = copyToImageStack(
						raiStack,
						destIntervalPerm, factory, nThreads );
				ip = ((ImagePlusImg<T,?>)img).getImagePlus();
			}
			else if ( outputInterval.numDimensions() == 2 )
			{
				final long[] dimensions = new long[ 4 ];
				dimensions[ 0 ] = outputInterval.dimension( 0 );	// x
				dimensions[ 1 ] = outputInterval.dimension( 1 );	// y
				dimensions[ 2 ] = numChannels; 						// c
				dimensions[ 3 ] = 1; 								// z
				final FinalInterval destIntervalPerm = new FinalInterval( dimensions );
				final RandomAccessibleInterval< T > img = copyToImageStack(
						Views.addDimension( Views.extendMirrorDouble( raiStack )),
						destIntervalPerm, factory, nThreads );
				ip = ((ImagePlusImg<T,?>)img).getImagePlus();
			}
		}

		ip.getCalibration().pixelWidth = voxdim.dimension( 0 );
		ip.getCalibration().pixelHeight = voxdim.dimension( 1 );
		ip.getCalibration().pixelDepth = voxdim.dimension( 2 );
		ip.getCalibration().setUnit( voxdim.unit() );

		if( offsetTransform != null )
		{
			ip.getCalibration().xOrigin = offsetTransform.get( 0, 3 );
			ip.getCalibration().yOrigin = offsetTransform.get( 1, 3 );
			ip.getCalibration().zOrigin = offsetTransform.get( 2, 3 );
		}

		ip.setTitle( bwData.getMovingSource( 0 ).getSpimSource().getName() + nameSuffix );

		return ip;
	}

	public static < T extends NumericType< T > & NativeType< T > > ImagePlus copyToImageStack( final RandomAccessible< T > rai, final Interval itvl,
			ProgressWriter progress )
	{
		// A bit of hacking to make slices the 4th dimension and channels the 3rd
		// since that's how ImagePlusImgFactory does it
		RandomAccessible< T > raip;
		if ( rai.numDimensions() > 3 )
			raip = Views.permute( rai, 2, 3 );
		else
			raip = rai;

		final long[] dimensions = new long[ itvl.numDimensions() ];
		for( int d = 0; d < itvl.numDimensions(); d++ )
		{
			if ( d == 2 && itvl.numDimensions() > 3 )
				dimensions[ d ] = itvl.dimension( 3 );
			else if ( d == 3 && itvl.numDimensions() > 3 )
				dimensions[ d ] = itvl.dimension( 2 );
			else
				dimensions[ d ] = itvl.dimension( d );
		}

		// create the image plus image
		final T t = rai.randomAccess().get();

		final ImagePlusImgFactory< T > factory = new ImagePlusImgFactory< T >( t );
		final ImagePlusImg< T, ? > target = factory.create( dimensions );

		final long[] dims = new long[ target.numDimensions() ];
		target.dimensions( dims );

		final long N = Intervals.numElements(itvl);
		final Cursor< T > c = target.cursor();
		final RandomAccess< T > ra = raip.randomAccess();
		double k = 0;
		while ( c.hasNext() )
		{
			c.fwd();
			ra.setPosition( c );
			c.get().set( ra.get() );

			if ( k % 10000 == 0 )
				progress.setProgress( k / N );

			k++;
		}

		progress.setProgress( 1.0 );
		try
		{
			return target.getImagePlus();
		}
		catch ( final ImgLibException e )
		{
			e.printStackTrace();
		}

		return null;
	}

	public static < T extends NumericType< T > & NativeType< T > > ImagePlus copyToImageStack( final RealRandomAccessible< T > rai, final Interval itvl )
	{
		final long[] dimensions = new long[ itvl.numDimensions() ];
		itvl.dimensions( dimensions );

		// create the image plus image
		final T t = rai.realRandomAccess().get();
		final ImagePlusImgFactory< T > factory = new ImagePlusImgFactory< T >( t );
		final ImagePlusImg< T, ? > target = factory.create( itvl );

		double k = 0;
		final long N = dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ];

		final Cursor< T > c = target.cursor();
		final RealRandomAccess< T > ra = rai.realRandomAccess();
		while ( c.hasNext() )
		{
			c.fwd();
			ra.setPosition( c );
			c.get().set( ra.get() );

			if ( k % 10000 == 0 )
			{
				IJ.showProgress( k / N );
			}
			k++;
		}

		IJ.showProgress( 1.1 );
		try
		{
			return target.getImagePlus();
		}
		catch ( final ImgLibException e )
		{
			e.printStackTrace();
		}

		return null;
	}

}
