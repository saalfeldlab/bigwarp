/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2025 Howard Hughes Medical Institute.
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
package bigwarp.transforms;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5DisplacementField;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5Factory;

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
			final ExecutorService exec ) {

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

		/* Converts [3,X,Y,Z] to [X,Y,Z,3] displacement field */
		final RandomAccessibleInterval< T > dfieldPerm = Views.moveAxis( dfield, 0, 3 );
		try
		{
			N5Utils.save( dfieldPerm, n5Writer, dataset, vecBlkSz, compression, exec );
		}
		catch ( final N5Exception e )
		{
			e.printStackTrace();
		}
		catch ( final InterruptedException e )
		{
			e.printStackTrace();
		}
		catch ( final ExecutionException e )
		{
			e.printStackTrace();
		}
	}

	public static final int[] permXYZ = new int[] { 2, 1, 0 };
	public static final int[] perm = new int[] { 1, 2, 3, 0 };

	/**
	 * Converts a [3,X,Y,Z] displacement field to a [3,Z,Y,X] displacement field
	 *
	 * @param <T> the type
	 * @param df the displacement field
	 * @return a permuted displacement field
	 */
	public static final <T extends NativeType<T> & RealType<T>> RandomAccessibleInterval<T> dxyz2dzyx( final RandomAccessibleInterval<T> df )
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
				final AffineTransform3D a3d = ( AffineTransform3D ) affine;
				mtx = new double[ 4 ][ 4 ];
				a3d.toMatrix( mtx );
			}
			else if ( affine instanceof AffineTransform2D )
			{
				final AffineTransform2D a2d = ( AffineTransform2D ) affine;
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
		catch ( final N5Exception e ) {}
	}

}
