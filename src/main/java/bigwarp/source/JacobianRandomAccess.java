package bigwarp.source;


import java.util.Arrays;

import net.imglib2.AbstractRealLocalizable;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.inverse.DifferentiableRealTransform;
import net.imglib2.type.numeric.RealType;

public class JacobianRandomAccess< T extends RealType<T>> extends AbstractRealLocalizable implements RealRandomAccess< T >
{

	final DifferentiableRealTransform xfm;
	final T value;

	final int numSpatialDimensions;
	final int tensorDimension;
	final AffineTransform currentJacobian;

	final double[] realPos;
	
	boolean recompute = true;
	
	public JacobianRandomAccess( long[] dimensions )
	{
		// should assume identity transformation
		this( dimensions, null, null );
	}
	
	public JacobianRandomAccess( long[] dimensions, T value, DifferentiableRealTransform xfm )
	{
		this( dimensions.length, value, xfm );
	}

	public JacobianRandomAccess( int nsd, T value, DifferentiableRealTransform xfm )
	{
		super( nsd + 1 );
		this.numSpatialDimensions = nsd;

		// the non-spatial dimension in which the jacobian values can be accessed
		tensorDimension = nsd; 

		this.value = value;
		this.xfm = xfm;
		currentJacobian = new AffineTransform( nsd );
		realPos = new double[ nsd ];

	}
	
	public static <T extends RealType<T>> JacobianRandomAccessible<T> create( 
			int nsd, T type, DifferentiableRealTransform transform )
	{
		return new JacobianRandomAccessible<T>( 
				new JacobianRandomAccess<T>( nsd, type, transform ));
	}

	public static <T extends RealType<T>> RealRandomAccessible<T> create( 
			Interval spatialInterval, T type, DifferentiableRealTransform transform )
	{
		int nsd = spatialInterval.numDimensions();
			
		long[] min = new long[ nsd + 1 ];
		long[] max = new long[ nsd + 1 ];
		
		int ntd = nsd * nsd; // number of tensor elements
		for( int d = 0; d < nsd + 1; d++ )
		{
			if( d < nsd )
			{
				min[ d ] = spatialInterval.min( d );
				max[ d ] = spatialInterval.max( d );
			}
			else
			{
				min[ d ] = 0;
				max[ d ] = ntd - 1;
			}
		}

		System.out.println( "total jac interval min: " + Arrays.toString( min ));
		System.out.println( "total jac interval max: " + Arrays.toString( max ));

		JacobianRandomAccessible<T> raj = new JacobianRandomAccessible<T>( 
				new JacobianRandomAccess<T>( nsd, type, transform ));
		
		return raj;
	}



	@Override
	public T get() 
	{
		if( recompute  && xfm != null ) 
		{
			// recompute jacobian
			localizeSpatial( realPos );
			AffineTransform newJacobian = xfm.jacobian( realPos );
			currentJacobian.set( newJacobian );
		}

		// Force integer position for the tensor dimension
		int tensorP = (int)Math.round( position[ numSpatialDimensions ]);

		int column = tensorP % numSpatialDimensions;
		int row = ( tensorP - column ) / numSpatialDimensions;

		value.setReal( currentJacobian.get( row, column ) );
		return value;
	}
	
	public void localizeSpatial( double[] p )
	{
		for( int d = 0; d < numSpatialDimensions; d++ )
			p[ d ] = getDoublePosition( d );
	}
	
	public JacobianRandomAccess<T> copy() 
	{
		return new JacobianRandomAccess< T >( new long[ position.length ], value.copy(), xfm );
	}

	public JacobianRandomAccess<T> copyRandomAccess() 
	{
		return copy();
	}

	@Override
	public void fwd( final int d )
	{
		++position[ d ];
		updateRecomputeDistance( 1, d );
	}

	@Override
	public void bck( final int d )
	{
		--position[ d ];
		updateRecomputeDistance( -1, d );
	}

	@Override
	public void move( final int distance, final int d )
	{
		position[ d ] += distance;
		updateRecomputeDistance( distance, d );
	}

	@Override
	public void move( final long distance, final int d )
	{
		position[ d ] += distance;
		updateRecomputeDistance( distance, d );
	}

	@Override
	public void move( final Localizable localizable )
	{
		for ( int d = 0; d < n; ++d )
		{
			final int distance = localizable.getIntPosition( d );
			position[ d ] += distance;
			updateRecomputeDistance( localizable.getDoublePosition( d ), d );
		}
	}

	@Override
	public void move( final int[] distance )
	{
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] += distance[ d ];
			updateRecomputeDistance( distance[ d ], d );
		}
	}

	@Override
	public void move( final long[] distance )
	{
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] += distance[ d ];
			updateRecomputeDistance( distance[ d ], d );

		}
	}

	@Override
	public void setPosition( final Localizable localizable )
	{
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] = localizable.getIntPosition( d );
			updateRecompute( localizable.getIntPosition( d ), d );
		}
	}

	@Override
	public void setPosition( final int[] pos )
	{
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] = pos[ d ];
			updateRecompute( pos[d ], d );
		}
	}

	@Override
	public void setPosition( final long[] pos )
	{
		for ( int d = 0; d < n; ++d )
		{
			position[ d ] = pos[ d ];
			updateRecompute( pos[d ], d );
		}
	}

	@Override
	public void move( float distance, int d )
	{
		position[ d ] += distance;
		recompute = recompute || ( d != tensorDimension );
	}

	@Override
	public void move( double distance, int d )
	{
		position[ d ] += distance;
		recompute = recompute || ( d != tensorDimension );
	}

	@Override
	public void move( RealLocalizable distance )
	{
		for ( int d = 0; d < n; ++d )
		{
			final double dist_dim = distance.getDoublePosition( d );
			position[ d ] += dist_dim;
			
			updateRecomputeDistance( distance.getDoublePosition( d ), d );

		}
	}

	@Override
	public void move( float[] distance )
	{
		for ( int d = 0; d < n; ++d )
		{
			float dist_dim = distance[ d ];
			position[ d ] += dist_dim;
			
		}
	}

	@Override
	public void move( double[] distance )
	{
		for ( int d = 0; d < n; ++d )
		{
			double dist_dim = distance[ d ];
			position[ d ] += dist_dim;
			
			updateRecomputeDistance( distance[ d ], d );
		}
	}

	@Override
	public void setPosition( RealLocalizable pos )
	{
		for ( int d = 0; d < pos.numDimensions(); ++d )
		{
			updateRecompute( pos.getDoublePosition( d ), d );
			position[ d ] = pos.getDoublePosition( d );
		}
		
	}

	@Override
	public void setPosition( float[] pos )
	{
		for ( int d = 0; d < pos.length; ++d )
		{
			updateRecompute( pos[d ], d );
			position[ d ] = pos[ d ];
		}
	}

	@Override
	public void setPosition( double[] pos )
	{
		for ( int d = 0; d < pos.length; ++d )
		{
			updateRecompute( pos[ d ], d );
			position[ d ] = pos[ d ];
		}
		
	}

	@Override
	public void setPosition( float pos, int d )
	{
		updateRecompute( pos, d );
		position[ d ] = pos;
		
	}

	@Override
	public void setPosition( double pos, int d )
	{
		updateRecompute( pos, d );
		position[ d ] = pos;
		
	}

	@Override
	public void setPosition( final int pos, final int d )
	{
		position[ d ] = pos;
		updateRecompute( pos, d );
	}

	@Override
	public void setPosition( final long pos, final int d )
	{
		position[ d ] = ( int ) pos;
		updateRecompute( pos, d );
	}
	
	public void updateRecompute( double pos, int d )
	{
		if( !recompute && d != tensorDimension && pos != position[ d ] )
			recompute = true;
	}

	public void updateRecomputeDistance( double distance, int d )
	{
		if( !recompute && d != tensorDimension && distance != 0.0 )
			recompute = true;
	}

	@Override
	public JacobianRandomAccess< T > copyRealRandomAccess()
	{
		return new JacobianRandomAccess< T >( this.numSpatialDimensions, value.copy(), xfm );
	}

	
	public static class JacobianRandomAccessible<T extends RealType<T>> implements RealRandomAccessible<T>
	{
		final private JacobianRandomAccess<T> access;

		public JacobianRandomAccessible( final JacobianRandomAccess<T> access )
		{
			this.access = access;
		}

		@Override
		public int numDimensions()
		{
			return access.numDimensions();
		}

		@Override
		public JacobianRandomAccess< T > realRandomAccess()
		{
			return access;
		}

		@Override
		public JacobianRandomAccess< T > realRandomAccess( RealInterval interval )
		{
			return access;
		}
	}

}
