package bigwarp.transforms;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.ij.N5Factory;
import org.janelia.saalfeldlab.n5.imglib2.N5DisplacementField;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class SlicerTransformations
{

	public static final String AFFINE_ATTR = "Transformation_Matrix";

	/*
	 * Saves an arbitrary {@link RealTransform} as a deformation field 
	 * into a specified n5 dataset using slicer's convention.
	 *
	 * @param <T> the type parameter
	 * @param n5Writer the n5 writer
	 * @param dataset the dataset path
	 * @param dfield the displacement field
	 * @param blockSize the block size
	 * @param compression the compression type
	 * @param exec the executor service
	 * @throws IOException the exception
	 */
	public static final <T extends NativeType<T> & RealType<T>> void saveDisplacementField(
			final N5Writer n5Writer,
			final String dataset,
			final RandomAccessibleInterval<T> dfield,
			final int[] blockSize,
			final Compression compression,
			ExecutorService exec ) throws IOException {

		int[] vecBlkSz;
		if( blockSize.length >= dfield.numDimensions() )
			vecBlkSz = blockSize; 
		else {
			vecBlkSz = new int[ blockSize.length + 1 ];
			vecBlkSz[ 0 ] = (int)dfield.dimension( 0 );
			for( int i = 1; i < vecBlkSz.length; i++ )
			{
				vecBlkSz[ i ] = blockSize[ i - 1 ];
			}
		}

		final RandomAccessibleInterval< T > dfieldPerm = dxyz2dzyx( dfield );
		try
		{
			N5Utils.save( dfieldPerm, n5Writer, dataset, vecBlkSz, compression, exec );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		catch ( InterruptedException e )
		{
			e.printStackTrace();
		}
		catch ( ExecutionException e )
		{
			e.printStackTrace();
		}
	}

	public static final int[] permXYZ = new int[] { 2, 1, 0 };
	public static final int[] perm = new int[] { 1, 2, 3, 0 };

	public static final <T extends NativeType<T> & RealType<T>> RandomAccessibleInterval<T> dxyz2dzyx( RandomAccessibleInterval<T> df )
	{
		final IntervalView< T > dx = Views.hyperSlice( df, 0, 0 );
		final IntervalView< T > dy = Views.hyperSlice( df, 0, 1 );
		final IntervalView< T > dz = Views.hyperSlice( df, 0, 2 );

		// besides permuting the axes, we also need to re-order the components of the displacement vectors
		final IntervalView< T > dxp = N5DisplacementField.permute( dx, permXYZ );
		final IntervalView< T > dyp = N5DisplacementField.permute( dy, permXYZ );
		final IntervalView< T > dzp = N5DisplacementField.permute( dz, permXYZ );
		final RandomAccessibleInterval< T > dfp = Views.stack( dxp, dyp, dzp );

		return N5DisplacementField.permute( dfp, perm );
	}

	public static final <T extends NativeType<T> & RealType<T>> void saveAffine( final N5Writer n5Writer, final String dataset, final AffineGet affine)
	{
		try
		{
			final double[][] mtx;
			if ( affine instanceof AffineTransform3D )
			{
				AffineTransform3D a3d = ( AffineTransform3D ) affine;
				mtx = new double[ 4 ][ 4 ];
				a3d.toMatrix( mtx );
			}
			else if ( affine instanceof AffineTransform2D )
			{
				AffineTransform2D a2d = ( AffineTransform2D ) affine;
				mtx = new double[ 3 ][ 3 ];
				a2d.toMatrix( mtx );
			}
			else {
				// src and tgt dims always the same for AffineGets
				final int nd = affine.numTargetDimensions();
				mtx = new double[nd][nd];
				for( int i = 0; i < nd; i++ )
					for( int j = 0; j < nd; j++ )
						mtx[i][j] = affine.get( i, j );
			}
			n5Writer.setAttribute( dataset, AFFINE_ATTR, mtx );
		}
		catch ( IOException e ) {}
	}

	public static void main( String[] args ) throws IOException
	{
		double[][] mtx = new double[4][4];
		mtx[0][0] = 2;
		mtx[1][1] = 3;
		mtx[2][2] = 4;
		mtx[3][3] = 1;

		final N5HDF5Writer h5 = new N5Factory().openHDF5Writer( "/home/john/tmp/mri-stack-landmarks-df-slicer.h5" );
		h5.setAttribute( "dfield", AFFINE_ATTR, mtx );
		h5.close();

		System.out.println( "done" );
	}

}
