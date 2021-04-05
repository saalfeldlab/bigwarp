package bigwarp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataWriter;

import bdv.export.ProgressWriter;
import bdv.export.ProgressWriterConsole;
import bdv.img.WarpedSource;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.ConverterSetups;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp.BigWarpData;
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
import net.imglib2.type.NativeType;
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

	private List< ConverterSetup > convSetups;

	private List< ImagePlus > outputList;

	final protected int[] movingSourceIndexList;

	final protected int[] targetSourceIndexList;
	
	protected AffineTransform3D pixelRenderToPhysical;
	
	protected AffineTransform3D resolutionTransform;
	
	protected AffineTransform3D offsetTransform;
	
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
	};

	public ParallelizationPolicy policy = ParallelizationPolicy.ITER;

	private ImagePlus result;

	private boolean showResult = true;

	protected static Logger logger = LogManager.getLogger( BigWarpExporter.class.getName() );

	private String exportPath;

	public BigWarpExporter(
			final List< SourceAndConverter< T >> sourcesIn,
			final List< ConverterSetup > convSetups,
			final int[] movingSourceIndexList,
			final int[] targetSourceIndexList,
			final Interpolation interp,
			final ProgressWriter progress )
	{
		this.sources = new ArrayList<SourceAndConverter<T>>();
		this.convSetups = convSetups;
		for( SourceAndConverter<T> sac : sourcesIn )
		{
			Source<T> srcCopy = null;
			Source<T> src = sac.getSpimSource();
			if( src instanceof WarpedSource )
			{
				WarpedSource<T> ws = (WarpedSource<T>)( sac.getSpimSource() );
				WarpedSource<T> wsCopy = new WarpedSource<>( ws.getWrappedSource(), ws.getName() ) ;
	
				if( ws.getTransform() != null )
				{
					wsCopy.updateTransform( ws.getTransform().copy() );
					wsCopy.setIsTransformed( true );
				}
				srcCopy = wsCopy;
			}
			else
				srcCopy = src;
				
			SourceAndConverter<T> copy = new SourceAndConverter<>( srcCopy, sac.getConverter() );
			sources.add( copy );
		}

		this.movingSourceIndexList = movingSourceIndexList;
		this.targetSourceIndexList = targetSourceIndexList;

		if( progress == null )
			this.progress = new ProgressWriterConsole();
		else
			this.progress = progress;

		this.setInterp( interp );
		
		pixelRenderToPhysical = new AffineTransform3D();
		resolutionTransform = new AffineTransform3D();
		offsetTransform = new AffineTransform3D();

		try {
			unit = sources.get( targetSourceIndexList[ 0 ] ).getSpimSource().getVoxelDimensions().unit();
		} catch( Exception e ) {}
	}

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
	
	/**
	 * Set the offset of the output field of view in pixels.
	 * 
	 * @param offset the offset in pixel units.
	 */
	public void setOffset( double... offset )
	{
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
		pixelRenderToPhysical.concatenate( resolutionTransform );
		pixelRenderToPhysical.concatenate( offsetTransform );
	}

	public void setInterval( final Interval outputInterval )
	{
		this.outputInterval = outputInterval;
	}

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
			ConverterSetup setup = convSetups.get( indexList[ i ] );
			double rngmin = setup.getDisplayRangeMin();
			double rngmax = setup.getDisplayRangeMax();

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
			ConverterSetup setup = bwdata.converterSetups.get( indexList[ i ] );
			double rngmin = setup.getDisplayRangeMin();
			double rngmax = setup.getDisplayRangeMax();

			imp.setC( i + 1 ); // ImagePlus.setC is one-indexed
			imp.setDisplayRange( rngmin, rngmax );
			imp.updateAndDraw();
		}
	}

	public FinalInterval destinationIntervalFromLandmarks( ArrayList<Double[]> pts, boolean isMoving )
	{
		int nd = pts.get( 0 ).length;
		long[] min = new long[ nd ];
		long[] max = new long[ nd ];

		Arrays.fill( min, Long.MAX_VALUE );
		Arrays.fill( max, Long.MIN_VALUE );

		for( Double[] pt : pts )
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
		int nd = interval.numDimensions();
		long[] min = new long[ nd ];
		long[] max = new long[ nd ];
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
		Img< T > target = factory.create( itvl );
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
		Img< T > target = factory.create( itvl );
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
		MixedTransformView< T > raible = Views.permute( ra, 2, 3 );

		// what dimension should we split across?
		int nd = raible.numDimensions();
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
		long N = target.dimension( dim2split );
		long del = ( long )( N / nThreads ); 
		splitPoints[ 0 ] = 0;
		splitPoints[ nThreads ] = target.dimension( dim2split );
		for( int i = 1; i < nThreads; i++ )
		{
			splitPoints[ i ] = splitPoints[ i - 1 ] + del;
		}

		ExecutorService threadPool = Executors.newFixedThreadPool( nThreads );

		LinkedList<Callable<Boolean>> jobs = new LinkedList<Callable<Boolean>>();
		for( int i = 0; i < nThreads; i++ )
		{
			final long start = splitPoints[ i ];
			final long end   = splitPoints[ i+1 ];

			jobs.add( new Callable<Boolean>()
			{
				public Boolean call()
				{
					try
					{
						final FinalInterval subItvl = getSubInterval( target, dim2split, start, end );
						final IntervalView< T > subTgt = Views.interval( target, subItvl );
						long N = Intervals.numElements(subTgt);
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
								double ratio = 1.0 * j / N;
								progress.setProgress( ratio ); 
							}
							j++;
						}
						return true;
					}
					catch( Exception e )
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
		catch ( InterruptedException e1 )
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
		Img< T > target = factory.create( itvl );
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
		MixedTransformView< T > raible = Views.permute( ra, 2, 3 );

		ExecutorService threadPool = Executors.newFixedThreadPool( nThreads );

		LinkedList<Callable<Boolean>> jobs = new LinkedList<Callable<Boolean>>();
		for( int i = 0; i < nThreads; i++ )
		{

			final int offset = i;
			jobs.add( new Callable<Boolean>()
			{
				public Boolean call()
				{
					try
					{
						IterableInterval<T> it = Views.flatIterable( target );
						final RandomAccess< T > access = raible.randomAccess();

						long N = it.size();
						final Cursor< T > c = it.cursor();
						c.jumpFwd( 1 + offset );
						for( long j = offset; j < N; j += nThreads )
						{
							access.setPosition( c );
							c.get().set( access.get() );
							c.jumpFwd( nThreads );
							
							if( offset == 0  && j % (nThreads * 100000) == 0 )
							{
								double ratio = 1.0 * j / N;
								progress.setProgress( ratio ); 
							}
						}

						return true;
					}
					catch( Exception e )
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
		catch ( InterruptedException e1 )
		{
			e1.printStackTrace();
		}

		progress.setProgress(1.0);
		return target;
	}
	
	public static FinalInterval transformRealInterval( RealTransform xfm, RealInterval interval )
	{
		int nd = interval.numDimensions();
		double[] pt = new double[ nd ];
		double[] ptxfm = new double[ nd ];

		long[] min = new long[ nd ];
		long[] max = new long[ nd ];

		// transform min		
		for( int d = 0; d < nd; d++ )
			pt[ d ] = interval.realMin( d );
		
		xfm.apply( pt, ptxfm );
		copyToLongFloor( ptxfm, min );


		// transform max
		
		for( int d = 0; d < nd; d++ )
		{
			pt[ d ] = interval.realMax( d );
		}
		
		xfm.apply( pt, ptxfm );
		copyToLongCeil( ptxfm, max );
		
		return new FinalInterval( min, max );
	}
	
	public static FinalInterval transformIntervalMinMax( RealTransform xfm, Interval interval )
	{
		int nd = interval.numDimensions();
		double[] pt = new double[ nd ];
		double[] ptxfm = new double[ nd ];

		long[] min = new long[ nd ];
		long[] max = new long[ nd ];

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

		int nd = interval.numDimensions();
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
		
		double[][] mvgPts = new double[ nd ][ N ];
		double[][] tgtPts = new double[ nd ][ N ];

		double[] w = new double[ N ];
		Arrays.fill( w, 1.0 );

		double[] pt = new double[ nd ];
		double[] ptxfm = new double[ nd ];

		long[] unitInterval = new long[ nd ];
		Arrays.fill( unitInterval, 2 );
		
		int i = 0;
		IntervalIterator it = new IntervalIterator( unitInterval );
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
		catch ( Exception e )
		{
			affine.identity();
			return;
		}
		
		if ( nd == 2 )
		{
			double[] mat = new double[ 6 ];
			((AffineModel2D)model).toArray( mat );
			affine.set( mat );
		}
		else
		{
			double[] mat = new double[ 12 ];
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

		int nd = interval.numDimensions();
		double[] pt = new double[ nd ];
		double[] ptxfm = new double[ nd ];

		long[] min = new long[ nd ];
		long[] max = new long[ nd ];
		Arrays.fill( min, Long.MAX_VALUE );
		Arrays.fill( max, Long.MIN_VALUE );

		long[] unitInterval = new long[ nd ];
		Arrays.fill( unitInterval, 2 );
		
		IntervalIterator it = new IntervalIterator( unitInterval );
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
				long lo = (long)Math.floor( ptxfm[d] );
				long hi = (long)Math.ceil( ptxfm[d] );
				
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
			catch ( InterruptedException e )
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
				long startTime = System.currentTimeMillis();
				exporter.result = exporter.export();
				long endTime = System.currentTimeMillis();

				System.out.println("export took " + (endTime - startTime) + "ms");

				if( exporter.outputList != null )
					exporter.outputList.add( exporter.result );

				if (exporter.result != null && exporter.showResult && show )
				{
					if( !exporter.isRGB() )
						BigWarpExporter.updateBrightnessContrast( exporter.result, exporter.convSetups, exporter.movingSourceIndexList );

					exporter.result.show();
				}

				if( exporter.exportPath != null )
				{
					try{
						IJ.save( exporter.result, exporter.exportPath );
					}
					catch( Exception e )
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

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public static <T> BigWarpExporter<?> getExporter(
			final BigWarpData<T> bwData,
			final List< SourceAndConverter< T >> transformedSources,
			final Interpolation interp,
			final ProgressWriter progressWriter )
	{
		int[] movingSourceIndexList = bwData.movingSourceIndices;
		int[] targetSourceIndexList = bwData.targetSourceIndices;

		if ( BigWarpRealExporter.isTypeListFullyConsistent( transformedSources, movingSourceIndexList ) )
		{
			Object baseType = transformedSources.get( movingSourceIndexList[ 0 ] ).getSpimSource().getType();
			if( baseType instanceof RealType )
				return new BigWarpRealExporter( transformedSources, bwData.converterSetups, movingSourceIndexList, targetSourceIndexList, interp, (RealType)baseType, progressWriter);
			else if ( ARGBType.class.isInstance( baseType ) )
			{
				return new BigWarpARGBExporter( (List)transformedSources, bwData.converterSetups, movingSourceIndexList, targetSourceIndexList, interp, progressWriter );
			}
			else
			{
				System.err.println( "Can't export type " + baseType.getClass() );
			}
		}
		return null;
	}

}
