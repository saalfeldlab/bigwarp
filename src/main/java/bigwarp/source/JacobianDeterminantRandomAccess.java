package bigwarp.source;


import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import bigwarp.source.JacobianRandomAccess.JacobianRandomAccessible;
import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.inverse.DifferentiableRealTransform;
import net.imglib2.type.numeric.RealType;

public class JacobianDeterminantRandomAccess< T extends RealType<T>> extends RealPoint implements RealRandomAccess<T>
{

	final JacobianRandomAccess<T> jacobian;
	
	final DenseMatrix64F mtx;
	
	final T value;
	
	//final double[] tensorPosition;

	public JacobianDeterminantRandomAccess( final JacobianRandomAccess<T> jacobian )
	{
		super( jacobian.numSpatialDimensions );
		this.jacobian = jacobian;
		mtx = new DenseMatrix64F( this.numDimensions(), this.numDimensions() );
		value = jacobian.value.copy();
	}

	@Override
	public T get() 
	{
		
		int n = this.numDimensions();
		jacobian.setPosition( this );
		
		int k = 0;
		for( int i = 0; i < n; i++ )
			for( int j = 0; j < n; j++ )
			{
				jacobian.setPosition( k, this.numDimensions() );
				mtx.set( i, j, jacobian.get().getRealDouble() );
				k++;
			}

		value.setReal( CommonOps.det( mtx ) );
		return value;
	}
	
	@Override
	public JacobianDeterminantRandomAccess< T > copy()
	{
		return new JacobianDeterminantRandomAccess<T>( jacobian.copy() );
	}

	@Override
	public JacobianDeterminantRandomAccess< T > copyRealRandomAccess()
	{
		return new JacobianDeterminantRandomAccess<T>( jacobian.copy() );
	}

	public static <T extends RealType<T>> JacobianDeterminantRandomAccessible<T> createJacobianDeterminant(
			T type, DifferentiableRealTransform transform )
	{
		JacobianRandomAccessible< T > jacimg = JacobianRandomAccess.create( transform.numTargetDimensions(), type, transform );
		JacobianDeterminantRandomAccess<T> jacdimg = 
				new JacobianDeterminantRandomAccess<>( jacimg.realRandomAccess() );

		return new JacobianDeterminantRandomAccessible<T>( jacdimg );
	}
	
	public static class JacobianDeterminantRandomAccessible<T extends RealType<T>> implements RealRandomAccessible<T>
	{
		final private JacobianDeterminantRandomAccess<T> access;

		public JacobianDeterminantRandomAccessible( final JacobianDeterminantRandomAccess<T> access )
		{
			this.access = access;
		}

		@Override
		public int numDimensions()
		{
			return access.numDimensions();
		}

		@Override
		public JacobianDeterminantRandomAccess< T > realRandomAccess()
		{
			return access;
		}

		@Override
		public JacobianDeterminantRandomAccess< T > realRandomAccess( RealInterval interval )
		{
			return access;
		}
	}

}
