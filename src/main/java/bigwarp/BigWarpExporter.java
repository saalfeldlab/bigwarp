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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bdv.export.ProgressWriter;
import bdv.export.ProgressWriterConsole;
import bdv.img.WarpedSource;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.models.AffineModel2D;
import mpicbg.models.AffineModel3D;
import mpicbg.models.Model;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.Views;

public abstract class BigWarpExporter <T>
{
	final protected List< SourceAndConverter< T >> sources;

	private List< ImagePlus > outputList;

	protected final BigWarpData<T> bwData;

	protected AffineTransform3D pixelRenderToPhysical;

	protected AffineTransform3D resolutionTransform;

	protected AffineTransform3D offsetTransform;

	protected boolean singleChannelNoStack = false;

	protected Interval outputInterval;

	protected Interpolation interp;

	protected boolean isVirtual = false;

	protected int nThreads = 1;

	protected ExportThread exportThread;

	protected String nameSuffix = "";

	protected String unit = "pixel";

	protected ProgressWriter progress;

	public enum ParallelizationPolicy {
		SLICE, ITER
	}

	public ParallelizationPolicy policy = ParallelizationPolicy.ITER;

	private ImagePlus result;

	private boolean showResult = true;

	protected static Logger logger = LoggerFactory.getLogger( BigWarpExporter.class );

	private String exportPath;

	public BigWarpExporter(
			BigWarpData<T> bwData,
			final List< ConverterSetup > convSetups,
			final Interpolation interp,
			final ProgressWriter progress )
	{
		this.bwData = bwData;
		this.sources = new ArrayList<SourceAndConverter<T>>();
		for( final SourceAndConverter<T> sac : bwData.sources )
		{
			Source<T> srcCopy = null;
			final Source<T> src = sac.getSpimSource();
			if( src instanceof WarpedSource )
			{
				final WarpedSource<T> ws = (WarpedSource<T>)( sac.getSpimSource() );
				final WarpedSource<T> wsCopy = new WarpedSource<>( ws.getWrappedSource(), ws.getName() ) ;

				if( ws.getTransform() != null )
				{
					wsCopy.updateTransform( ws.getTransform().copy() );
					wsCopy.setIsTransformed( true );
				}
				srcCopy = wsCopy;
			}
			else
				srcCopy = src;

			final SourceAndConverter<T> copy = new SourceAndConverter<>( srcCopy, sac.getConverter() );
			sources.add( copy );
		}

		if( progress == null )
			this.progress = new ProgressWriterConsole();
		else
			this.progress = progress;

		this.setInterp( interp );

		pixelRenderToPhysical = new AffineTransform3D();
		resolutionTransform = new AffineTransform3D();
		offsetTransform = new AffineTransform3D();

		try {
			unit = bwData.getTargetSource( 0 ).getSpimSource().getVoxelDimensions().unit();
		} catch( final Exception e ) {
			// if something goes wrong use the units of this source
			unit = bwData.getMovingSource(0).getSpimSource().getVoxelDimensions().unit();
		}
	}

	public abstract RandomAccessibleInterval<?> exportRai(Source<?> src);

	public abstract RandomAccessibleInterval<?> exportRai();

	public abstract ImagePlus export();

	public abstract boolean isRGB();

	public void showResult( final boolean showResult )
	{
		this.showResult = showResult;
	}

	public void setExportPath( final String exportPath )
	{
		this.exportPath = exportPath;
	}

	public void setOutputList( final List<ImagePlus> outputList )
	{
		this.outputList = outputList;
	}

	public void setUnit( final String unit )
	{
		this.unit = unit;
	}

	public void setInterp( Interpolation interp )
	{
		this.interp = interp;
	}

	public void setVirtual( final boolean isVirtual )
	{
		this.isVirtual = isVirtual;
	}

	public void setParallelizationPolicy( ParallelizationPolicy policy )
	{
		this.policy = policy;
	}

	public void setNumThreads( final int nThreads )
	{
		this.nThreads = nThreads;
	}

	public void setNameSuffix( final String suffix )
	{
		this.nameSuffix = suffix;
	}

	public void setRenderResolution( double... res )
	{
		for( int i = 0; i < res.length; i++ )
			resolutionTransform.set( res[ i ], i, i );
	}

	public void setSingleChannelNoStack( boolean singleChannelNoStack )
	{
		this.singleChannelNoStack = singleChannelNoStack;
	}

	/**
	 * Set the offset of the output field of view in physical units.
	 *
	 * @param offset the offset in pixel units.
	 */
	public void setOffset( double... offset )
	{
		System.out.println("setOffset: " + Arrays.toString(offset));
		for( int i = 0; i < offset.length; i++ )
			offsetTransform.set( offset[ i ], i, 3 );
	}

	/**
	 * Generate the transform from output pixel space to physical space.
	 *
	 * Call this after setRenderResolution and setOffset.
	 */
	public void buildTotalRenderTransform()
	{
		pixelRenderToPhysical.identity();
		pixelRenderToPhysical.concatenate( offsetTransform );
		pixelRenderToPhysical.concatenate( resolutionTransform );
	}

	public void setInterval( final Interval outputInterval )
	{
		this.outputInterval = outputInterval;
	}

	@SuppressWarnings("hiding")
	public <T> RandomAccessibleInterval<T> exportSource( SourceAndConverter<T> src )
	{
		final RealRandomAccessible< T > raiRaw = src.getSpimSource().getInterpolatedSource( 0, 0, interp );

		// apply the transformations
		final AffineRandomAccessible< T, AffineGet > rai = RealViews.affine(
				raiRaw, pixelRenderToPhysical.inverse() );

		return Views.interval( Views.raster( rai ), outputInterval );
	}

	public static void updateBrightnessContrast(
			final ImagePlus imp,
			final List<ConverterSetup> convSetups,
			final int[] indexList )
	{
		assert( imp.getNChannels() == indexList.length );

		for( int i = 0; i < indexList.length; i++ )
		{
			final ConverterSetup setup = convSetups.get( indexList[ i ] );
			final double rngmin = setup.getDisplayRangeMin();
			final double rngmax = setup.getDisplayRangeMax();

			imp.setC( i + 1 ); // ImagePlus.setC is one-indexed
			imp.setDisplayRange( rngmin, rngmax );
			imp.updateAndDraw();
		}
	}

	public static void updateBrightnessContrast(
			final ImagePlus imp,
			final List<ConverterSetup> convSetups)
	{
		for( int i = 0; i < convSetups.size(); i++ )
		{
			final ConverterSetup setup = convSetups.get( i );
			final double rngmin = setup.getDisplayRangeMin();
			final double rngmax = setup.getDisplayRangeMax();

			imp.setC( i + 1 ); // ImagePlus.setC is one-indexed
			imp.setDisplayRange( rngmin, rngmax );
			imp.updateAndDraw();
		}
	}

	public static void updateBrightnessContrast(
			final ImagePlus imp,
			final BigWarpData<?> bwdata,
			final int[] indexList )
	{
		assert( imp.getNChannels() == indexList.length );

		for( int i = 0; i < indexList.length; i++ )
		{
			final ConverterSetup setup = bwdata.converterSetups.get( indexList[ i ] );
			final double rngmin = setup.getDisplayRangeMin();
			final double rngmax = setup.getDisplayRangeMax();

			imp.setC( i + 1 ); // ImagePlus.setC is one-indexed
			imp.setDisplayRange( rngmin, rngmax );
			imp.updateAndDraw();
		}
	}

	public FinalInterval destinationIntervalFromLandmarks( ArrayList<Double[]> pts, boolean isMoving )
	{
		final int nd = pts.get( 0 ).length;
		final long[] min = new long[ nd ];
		final long[] max = new long[ nd ];

		Arrays.fill( min, Long.MAX_VALUE );
		Arrays.fill( max, Long.MIN_VALUE );

		for( final Double[] pt : pts )
		{
			for( int d = 0; d < nd; d++ )
			{
				if( pt[ d ] > max [ d ] )
					max[ d ] = (long)Math.ceil( pt[ d ]);

				if( pt[ d ] < min [ d ] )
					min[ d ] = (long)Math.floor( pt[ d ]);
			}
		}
		return new FinalInterval( min, max );
	}

	public static FinalInterval getSubInterval( Interval interval, int d, long start, long end )
	{
		final int nd = interval.numDimensions();
		final long[] min = new long[ nd ];
		final long[] max = new long[ nd ];
		for( int i = 0; i < nd; i++ )
		{
			if( i == d )
			{
				min[ i ] = start;
				max[ i ] = end - 1;
			}
			else
			{
				min[ i ] = interval.min( i );
				max[ i ] = interval.max( i );
			}
		}
		return new FinalInterval( min, max );
	}

	public < T extends NumericType<T> > RandomAccessibleInterval<T> copyToImageStack(
			final RandomAccessible< T > raible,
			final Interval itvl,
			final ImgFactory<T> factory,
			final int nThreads )
	{
		final Img< T > target = factory.create( itvl );
		if( policy == ParallelizationPolicy.ITER )
			return copyToImageStackIterOrder( raible, itvl, target, nThreads, progress );
		else
			return copyToImageStackBySlice( raible, itvl, target, nThreads, progress );
	}

	public static < T extends NumericType<T> > RandomAccessibleInterval<T> copyToImageStackBySlice(
			final RandomAccessible< T > raible,
			final Interval itvl,
			final ImgFactory<T> factory,
			final int nThreads,
			final ProgressWriter progress )
	{
		// create the image plus image
		final Img< T > target = factory.create( itvl );
		return copyToImageStackBySlice( raible, itvl, target, nThreads, progress );
	}

	public static < T extends NumericType<T> > RandomAccessibleInterval<T> copyToImageStackBySlice(
			final RandomAccessible< T > ra,
			final Interval itvl,
			final RandomAccessibleInterval<T> target,
			final int nThreads,
			final ProgressWriter progress )
	{
		// TODO I wish I didn't have to do this inside this method
		final MixedTransformView< T > raible = Views.permute( ra, 2, 3 );

		// what dimension should we split across?
		final int nd = raible.numDimensions();
		int tmp = nd - 1;
		while( tmp >= 0 )
		{
			if( target.dimension( tmp ) > 1 )
				break;
			else
				tmp--;
		}
		final int dim2split = tmp;

		final long[] splitPoints = new long[ nThreads + 1 ];
		final long N = target.dimension( dim2split );
		final long del = ( long )( N / nThreads );
		splitPoints[ 0 ] = 0;
		splitPoints[ nThreads ] = target.dimension( dim2split );
		for( int i = 1; i < nThreads; i++ )
		{
			splitPoints[ i ] = splitPoints[ i - 1 ] + del;
		}

		final ExecutorService threadPool = Executors.newFixedThreadPool( nThreads );

		final LinkedList<Callable<Boolean>> jobs = new LinkedList<Callable<Boolean>>();
		for( int i = 0; i < nThreads; i++ )
		{
			final long start = splitPoints[ i ];
			final long end   = splitPoints[ i+1 ];

			jobs.add( new Callable<Boolean>()
			{
				@Override
				public Boolean call()
				{
					try
					{
						final FinalInterval subItvl = getSubInterval( target, dim2split, start, end );
						final IntervalView< T > subTgt = Views.interval( target, subItvl );
						final long N = Intervals.numElements(subTgt);
						final Cursor< T > c = subTgt.cursor();
						final RandomAccess< T > ra = raible.randomAccess();
						long j = 0;
						while ( c.hasNext() )
						{
							c.fwd();
							ra.setPosition( c );
							c.get().set( ra.get() );

							if( start == 0  && j % 100000 == 0 )
							{
								final double ratio = 1.0 * j / N;
								progress.setProgress( ratio );
							}
							j++;
						}
						return true;
					}
					catch( final Exception e )
					{
						e.printStackTrace();
					}
					return false;
				}
			});
		}
		try
		{
			threadPool.invokeAll( jobs );
			threadPool.shutdown(); // wait for all jobs to finish

		}
		catch ( final InterruptedException e1 )
		{
			e1.printStackTrace();
		}

		progress.setProgress(1.0);
		return target;
	}

	public static < T extends NumericType<T> > RandomAccessibleInterval<T> copyToImageStackIterOrder(
			final RandomAccessible< T > raible,
			final Interval itvl,
			final ImgFactory<T> factory,
			final int nThreads,
			final ProgressWriter progress )
	{
		// create the image plus image
		final Img< T > target = factory.create( itvl );
		return copyToImageStackIterOrder( raible, itvl, target, nThreads, progress );
	}

	public static < T extends NumericType<T> > RandomAccessibleInterval<T> copyToImageStackIterOrder(
			final RandomAccessible< T > ra,
			final Interval itvl,
			final RandomAccessibleInterval<T> target,
			final int nThreads,
			final ProgressWriter progress )
	{
		progress.setProgress(0.0);
		// TODO I wish I didn't have to do this inside this method..
		// 	Maybe I don't have to, and should do it where I call this instead?
		final MixedTransformView< T > raible = Views.permute( ra, 2, 3 );

		final ExecutorService threadPool = Executors.newFixedThreadPool( nThreads );

		final LinkedList<Callable<Boolean>> jobs = new LinkedList<Callable<Boolean>>();
		for( int i = 0; i < nThreads; i++ )
		{

			final int offset = i;
			jobs.add( new Callable<Boolean>()
			{
				@Override
				public Boolean call()
				{
					try
					{
						final IterableInterval<T> it = Views.flatIterable( target );
						final RandomAccess< T > access = raible.randomAccess();

						final long N = it.size();
						final Cursor< T > c = it.cursor();
						c.jumpFwd( 1 + offset );
						for( long j = offset; j < N; j += nThreads )
						{
							access.setPosition( c );
							c.get().set( access.get() );
							c.jumpFwd( nThreads );

							if( offset == 0  && j % (nThreads * 100000) == 0 )
							{
								final double ratio = 1.0 * j / N;
								progress.setProgress( ratio );
							}
						}

						return true;
					}
					catch( final Exception e )
					{
						e.printStackTrace();
					}
					return false;
				}
			});
		}
		try
		{
			threadPool.invokeAll( jobs );
			threadPool.shutdown(); // wait for all jobs to finish

		}
		catch ( final InterruptedException e1 )
		{
			e1.printStackTrace();
		}

		progress.setProgress(1.0);
		return target;
	}

	public static FinalInterval transformRealInterval( RealTransform xfm, RealInterval interval )
	{
		final int nd = interval.numDimensions();
		final double[] pt = new double[ nd ];
		final double[] ptxfm = new double[ nd ];

		final long[] min = new long[ nd ];
		final long[] max = new long[ nd ];

		// transform min
		for( int d = 0; d < nd; d++ )
			pt[ d ] = interval.realMin( d );

		xfm.apply( pt, ptxfm );
		copyToLongFloor( ptxfm, min );

		// transform max
		for( int d = 0; d < nd; d++ )
			pt[ d ] = interval.realMax( d );

		xfm.apply( pt, ptxfm );
		copyToLongCeil( ptxfm, max );

		return new FinalInterval( min, max );
	}

	public static FinalInterval transformIntervalMinMax( RealTransform xfm, Interval interval )
	{
		final int nd = interval.numDimensions();
		final double[] pt = new double[ nd ];
		final double[] ptxfm = new double[ nd ];

		final long[] min = new long[ nd ];
		final long[] max = new long[ nd ];

		// transform min
		for( int d = 0; d < nd; d++ )
			pt[ d ] = interval.min( d );

		xfm.apply( pt, ptxfm );
		copyToLongFloor( ptxfm, min );


		// transform max

		for( int d = 0; d < nd; d++ )
		{
			pt[ d ] = interval.max( d );
		}

		xfm.apply( pt, ptxfm );
		copyToLongCeil( ptxfm, max );

		return new FinalInterval( min, max );
	}

	/**
	 * Only works for 2D or 3D.
	 *
	 * @param affine variable in which to store the result
	 * @param xfm the transform
	 * @param interval the interval
	 */
	public static void estimateAffineFromCorners( AffineTransform3D affine, RealTransform xfm, Interval interval )
	{
		if( xfm == null )
			return;

		final int nd = interval.numDimensions();
		int N;
		Model model;
		if ( nd == 2 )
		{
			N = 4;
			model = new AffineModel2D();
		}
		else
		{
			N = 8;
			model = new AffineModel3D();
		}

		final double[][] mvgPts = new double[ nd ][ N ];
		final double[][] tgtPts = new double[ nd ][ N ];

		final double[] w = new double[ N ];
		Arrays.fill( w, 1.0 );

		final double[] pt = new double[ nd ];
		final double[] ptxfm = new double[ nd ];

		final long[] unitInterval = new long[ nd ];
		Arrays.fill( unitInterval, 2 );

		int i = 0;
		final IntervalIterator it = new IntervalIterator( unitInterval );
		while( it.hasNext() )
		{
			it.fwd();
			for( int d = 0; d < nd; d++ )
			{
				if( it.getLongPosition( d ) == 0 )
					pt[ d ] = interval.min( d );
				else
					pt[ d ] = interval.max( d );
			}

			xfm.apply( pt, ptxfm );

			for( int d = 0; d < nd; d++ )
			{
				mvgPts[ d ][ i ] = pt[ d ];
				tgtPts[ d ][ i ] = ptxfm[ d ];
			}

			i++;
		}

		try
		{
			model.fit( mvgPts, tgtPts, w );
		}
		catch ( final Exception e )
		{
			affine.identity();
			return;
		}

		if ( nd == 2 )
		{
			final double[] mat = new double[ 6 ];
			((AffineModel2D)model).toArray( mat );
			affine.set( mat );
		}
		else
		{
			final double[] mat = new double[ 12 ];
			((AffineModel3D)model).getMatrix( mat );
			affine.set( mat );
		}
	}

	public static FinalInterval estimateBounds( RealTransform xfm, Interval interval )
	{
		if( xfm == null )
			return new FinalInterval(
							Intervals.minAsLongArray(interval),
							Intervals.maxAsLongArray(interval) );

		final int nd = interval.numDimensions();
		final double[] pt = new double[ nd ];
		final double[] ptxfm = new double[ nd ];

		final long[] min = new long[ nd ];
		final long[] max = new long[ nd ];
		Arrays.fill( min, Long.MAX_VALUE );
		Arrays.fill( max, Long.MIN_VALUE );

		final long[] unitInterval = new long[ nd ];
		Arrays.fill( unitInterval, 2 );

		final IntervalIterator it = new IntervalIterator( unitInterval );
		while( it.hasNext() )
		{
			it.fwd();
			for( int d = 0; d < nd; d++ )
			{
				if( it.getLongPosition( d ) == 0 )
					pt[ d ] = interval.min( d );
				else
					pt[ d ] = interval.max( d );
			}

			xfm.apply( pt, ptxfm );

			for( int d = 0; d < nd; d++ )
			{
				final long lo = (long)Math.floor( ptxfm[d] );
				final long hi = (long)Math.ceil( ptxfm[d] );

				if( lo < min[ d ])
					min[ d ] = lo;

				if( hi > max[ d ])
					max[ d ] = hi;
			}
		}
		return new FinalInterval( min, max );
	}

	public static void copyToLongFloor( final double[] src, final long[] dst )
	{
		for( int d = 0; d < src.length; d++ )
			dst[ d ] = (long)Math.floor( src[d] );
	}

	public static void copyToLongCeil( final double[] src, final long[] dst )
	{
		for( int d = 0; d < src.length; d++ )
			dst[ d ] = (long)Math.floor( src[d] );
	}

	public ImagePlus exportAsynch( final boolean wait )
	{
		return exportAsynch( wait, true );
	}

	public ImagePlus exportAsynch( final boolean wait, final boolean show )
	{
		exportThread = new ExportThread( this, show );
		exportThread.start();
		if( wait )
			try
			{
				exportThread.join();
			}
			catch ( final InterruptedException e )
			{
				e.printStackTrace();
			}

		return result;
	}

	public ImagePlus getResult()
	{
		return result;
	}

	public static class ExportThread extends Thread
	{
		final BigWarpExporter<?> exporter;

		final boolean show;

		public ExportThread(BigWarpExporter<?> exporter, final boolean show )
		{
			this.exporter = exporter;
			this.show = show;
		}

		@Override
		public void run()
		{
			try {
				// long startTime = System.currentTimeMillis();
				exporter.result = exporter.export();
				// long endTime = System.currentTimeMillis();
				// System.out.println("export took " + (endTime - startTime) + "ms");

				if( show )
					exporter.result.show();

				if( exporter.outputList != null )
					exporter.outputList.add( exporter.result );

				if (exporter.result != null && exporter.showResult && show )
				{
					if( !exporter.isRGB() )
						BigWarpExporter.updateBrightnessContrast( exporter.result, exporter.bwData.getMovingConverterSetups() );

				}

				if( exporter.exportPath != null && !exporter.exportPath.isEmpty())
				{
					try{
						IJ.save( exporter.result, exporter.exportPath );
					}
					catch( final Exception e )
					{
						IJ.showMessage( "Failed to write : " + exporter.exportPath );
					}
				}

			}
			catch (final RejectedExecutionException e)
			{
				// this happens when the rendering threadpool
				// is killed before the painter thread.
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> BigWarpExporter<?> getExporter(
			final BigWarpData<T> bwData,
			final  SourceAndConverter< T > source,
			final Interpolation interp,
			final ProgressWriter progressWriter )
	{
		final Object baseType = source.getSpimSource().getType();
		if( baseType instanceof RealType )
			return new BigWarpRealExporter( bwData, bwData.converterSetups, interp, (RealType)baseType, progressWriter);
		else if ( ARGBType.class.isInstance( baseType ) )
		{
			return new BigWarpARGBExporter( (BigWarpData<ARGBType>)bwData, bwData.converterSetups, interp, progressWriter );
		}
		else
		{
			System.err.println( "Can't export type " + baseType.getClass() );
			return null;
		}
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public static <T> BigWarpExporter<?> getExporter(
			final BigWarpData<T> bwData,
			final List< SourceAndConverter< T >> transformedSources,
			final Interpolation interp,
			final ProgressWriter progressWriter )
	{
		final List<Integer> movingSourceIndexList = bwData.getMovingSourceIndices();

		//TODO Caleb: Consider a method that just takes a list of all moving sources
		if ( BigWarpRealExporter.isTypeListFullyConsistent( transformedSources, movingSourceIndexList ) )
		{
			final Object baseType = transformedSources.get( movingSourceIndexList.get( 0 ) ).getSpimSource().getType();
			if( baseType instanceof RealType )
				return new BigWarpRealExporter( bwData, bwData.converterSetups, interp, (RealType)baseType, progressWriter);
			else if ( ARGBType.class.isInstance( baseType ) )
			{
				return new BigWarpARGBExporter( (BigWarpData<ARGBType>)bwData, bwData.converterSetups, interp, progressWriter );
			}
			else
			{
				System.err.println( "Can't export type " + baseType.getClass() );
			}
		}
		return null;
	}


}
