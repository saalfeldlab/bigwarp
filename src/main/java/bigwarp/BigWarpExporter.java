package bigwarp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import bdv.img.WarpedSource;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.Views;

public abstract class BigWarpExporter <T>
{
	final protected ArrayList< SourceAndConverter< ? >> sources;

	final protected int[] movingSourceIndexList;

	final protected int[] targetSourceIndexList;
	
	protected AffineTransform3D pixelRenderToPhysical;
	
	protected AffineTransform3D resolutionTransform;
	
	protected AffineTransform3D offsetTransform;
	
	protected Interval outputInterval;

	protected Interpolation interp;
	
	protected boolean isVirtual = false;
	
	protected int nThreads = 1;
	
	public abstract ImagePlus export();

	public BigWarpExporter(
			final ArrayList< SourceAndConverter< ? >> sources,
			final int[] movingSourceIndexList,
			final int[] targetSourceIndexList,
			final Interpolation interp )
	{
		this.sources = sources;
		this.movingSourceIndexList = movingSourceIndexList;
		this.targetSourceIndexList = targetSourceIndexList;
		this.setInterp( interp );
		
		pixelRenderToPhysical = new AffineTransform3D();
		resolutionTransform = new AffineTransform3D();
		offsetTransform = new AffineTransform3D();
	}

	public void setInterp( Interpolation interp )
	{
		this.interp = interp;
	}
	
	public void setVirtual( final boolean isVirtual )
	{
		this.isVirtual = isVirtual;
	}
	
	public void setNumThreads( final int nThreads )
	{
		this.nThreads = nThreads;
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
		
//		System.out.println( " " );
//		System.out.println( "resolutionTransform   : " + resolutionTransform );
//		System.out.println( "offsetTransform       : " + offsetTransform );
//		System.out.println( "pixelRenderToPhysical : " + pixelRenderToPhysical );
	}

	public void setInterval( final Interval outputInterval )
	{
		this.outputInterval = outputInterval;
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

	public static < T extends NumericType<T> > RandomAccessibleInterval<T> copyToImageStack( 
			final RandomAccessible< T > raible,
			final Interval itvl,
			final ImgFactory<T> factory,
			final int nThreads )
	{
		// create the image plus image
		Img< T > target = factory.create( itvl );
		return copyToImageStack( raible, itvl, target, nThreads );
	}

	public static < T extends NumericType<T> > RandomAccessibleInterval<T> copyToImageStack( 
			final RandomAccessible< T > ra,
			final Interval itvl,
			final RandomAccessibleInterval<T> target,
			final int nThreads )
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
//		System.out.println( "dim2split: " + dim2split );
//		System.out.println( "split points: " + XfmUtils.printArray( splitPoints ));

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
						final Cursor< T > c = subTgt.cursor();
						final RandomAccess< T > ra = raible.randomAccess();
						while ( c.hasNext() )
						{
							c.fwd();
							ra.setPosition( c );
							c.get().set( ra.get() );
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
			List< Future< Boolean > > futures = threadPool.invokeAll( jobs );
			threadPool.shutdown(); // wait for all jobs to finish

		}
		catch ( InterruptedException e1 )
		{
			e1.printStackTrace();
		}

		IJ.showProgress( 1.1 );
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
	
	public static FinalInterval estimateBounds( RealTransform xfm, Interval interval )
	{
		System.out.println( "estimateBounds" );
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
			System.out.println( "pt " + Arrays.toString( pt ));

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

}
