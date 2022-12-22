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
package bdv.ij;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.JFrame;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;
import org.janelia.saalfeldlab.n5.blosc.BloscCompression;
import org.janelia.saalfeldlab.n5.ij.N5Exporter;
import org.janelia.saalfeldlab.n5.ij.N5Factory;
import org.janelia.saalfeldlab.n5.imglib2.N5DisplacementField;
import org.janelia.saalfeldlab.n5.metadata.omengff.NgffAffineTransformation;
import org.janelia.saalfeldlab.n5.metadata.omengff.NgffDisplacementsTransformation;
import org.janelia.saalfeldlab.n5.metadata.omengff.NgffSequenceTransformation;

import bdv.gui.ExportDisplacementFieldFrame;
import bigwarp.BigWarp;
import bigwarp.BigWarpData;
import bigwarp.BigWarpExporter;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.source.SourceInfo;
import bigwarp.transforms.BigWarpTransform;
import bigwarp.transforms.NgffTransformations;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.FloatImagePlus;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.parallel.TaskExecutors;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.DisplacementFieldTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.InvertibleRealTransformSequence;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import net.imglib2.view.composite.CompositeIntervalView;
import net.imglib2.view.composite.GenericComposite;
import ome.ngff.transformations.CoordinateTransformation;

/**
 * ImageJ plugin to convert the thin plate spline to a deformation field.
 *
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public class BigWarpToDeformationFieldPlugIn implements PlugIn
{
	public static enum INVERSE_OPTIONS { FORWARD, INVERSE, BOTH };

	public static final String[] compressionOptions = new String[] {
				N5Exporter.RAW_COMPRESSION,
				N5Exporter.GZIP_COMPRESSION,
				N5Exporter.LZ4_COMPRESSION,
				N5Exporter.XZ_COMPRESSION,
				N5Exporter.BLOSC_COMPRESSION };

	public static final String flattenOption = "Flat";
	public static final String sequenceOption = "Sequence";

	public static void main( final String[] args )
	{
		new ImageJ();

		new Recorder();
		Recorder.record = true;

//		IJ.run("Boats (356K)");
//		ImagePlus imp = IJ.openImage( "/groups/saalfeld/home/bogovicj/tmp/mri-stack.tif" );
//		ImagePlus imp = IJ.openImage( "/home/john/tmp/mri-stack.tif" );
		ImagePlus imp = IJ.openImage( "/home/john/tmp/mri-stack_mm.tif" );
		imp.show();

		new BigWarpToDeformationFieldPlugIn().run( null );
	}

	public <T> void runFromBigWarpInstance( final BigWarp<?> bw )
	{
//		System.out.println( "run from instance." );
//		ImageJ ij = IJ.getInstance();
//		if ( ij == null )
//			return;
//
		ExportDisplacementFieldFrame.createAndShow( bw );
	}

//	/**
//	 * @deprecated not necessary access thedesired source this way anymore, use {@link #runFromBigWarpInstance(LandmarkTableModel, SourceAndConverter)}
//	 * 	on the result of {@link bigwarp.BigWarpData#getTargetSource(int)}
//	 */
//	@Deprecated
//	public <T> void runFromBigWarpInstanceOld(
//			final BigWarpData<?> data,
//			final LandmarkTableModel landmarkModel,
//			final List<SourceAndConverter<T>> sources,
//			final List<Integer> targetSourceIndexList )
//	{
//		runFromBigWarpInstanceOld( data, landmarkModel, data.getTargetSource( 0 ) );
//	}

//	public <T> void runFromBigWarpInstanceOld(
//			final BigWarpData<?> data, final LandmarkTableModel landmarkModel, final SourceAndConverter< T > sourceAndConverter )
//	{
//		System.out.println( "run from instance." );
//		ImageJ ij = IJ.getInstance();
//		if ( ij == null )
//			return;
//
//		final DeformationFieldExportParameters params = DeformationFieldExportParameters.fromDialog( false, false );
//		if( params == null )
//			return;
//
//		final RandomAccessibleInterval< ? > tgtInterval = sourceAndConverter.getSpimSource().getSource( 0, 0 );
//
//		int ndims = landmarkModel.getNumdims();
//		long[] dims = tgtInterval.dimensionsAsLongArray();
//
//		double[] spacing = new double[ 3 ];
//		double[] offset = new double[ 3 ];
//		String unit = "pix";
//		VoxelDimensions voxelDim = sourceAndConverter.getSpimSource().getVoxelDimensions();
//		voxelDim.dimensions( spacing );
//
//		if( params.spacing != null )
//			spacing = params.spacing;
//
//		if( params.offset != null )
//			offset = params.offset;
//
//		if( params.size != null )
//			dims = params.size;
//
//		if( params.n5Base.isEmpty() )
//		{
////			toImagePlus( data, landmarkModel, null, params.ignoreAffine, params.flatten(), params.virtual, dims, spacing, params.nThreads );
//			if ( params.inverseOption.equals( INVERSE_OPTIONS.BOTH.toString() ) )
//			{
//				toImagePlus( data, landmarkModel, null, params.ignoreAffine, params.flatten(), false, params.virtual, params.size, params.spacing, params.nThreads );
//				toImagePlus( data, landmarkModel, null, params.ignoreAffine, params.flatten(), true, params.virtual, params.size, params.spacing, params.nThreads );
//			}
//			else
//			{
//				final boolean inverse = params.inverseOption.equals( INVERSE_OPTIONS.INVERSE.toString() );
//				toImagePlus( data, landmarkModel, null, params.ignoreAffine, params.flatten(), inverse, params.virtual, params.size, params.spacing, params.nThreads );
//			}
//		}
//		else
//		{
//			try
//			{
//				final boolean inverse = params.inverseOption.equals( INVERSE_OPTIONS.INVERSE.toString() );
//				writeN5( params.n5Base, params.n5Dataset, landmarkModel, null, data, dims, spacing, offset, unit, params.blockSize, params.compression, params.nThreads, params.ignoreAffine, params.flatten(), inverse );
//			}
//			catch ( IOException e )
//			{
//				e.printStackTrace();
//			}
//		}
//	}
	
	public static void runFromParameters( final DeformationFieldExportParameters params, final BigWarpData<?> data, final LandmarkTableModel landmarkModel, final BigWarpTransform bwTransform )
	{
		String unit = "pixel";
		LandmarkTableModel ltm;
		if( landmarkModel == null )
		{
			// load
			try
			{
				ltm = LandmarkTableModel.loadFromCsv( new File( params.landmarkPath ), false );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
				return;
			}
		}
		else
			ltm = landmarkModel;

		int ndims = ltm.getNumdims();
		double[] spacing = new double[ ndims ];
		double[] offset = new double[ ndims ];
		long[] dims = new long[ ndims ];
		if ( params.spacing != null )
			spacing = params.spacing;

		if ( params.offset != null )
			offset = params.offset;

		if ( params.size != null )
			dims = params.size;

		if ( params.n5Base.isEmpty() )
		{
			if ( params.inverseOption.equals( INVERSE_OPTIONS.BOTH.toString() ) )
			{
				toImagePlus( data, ltm, bwTransform, params.ignoreAffine, params.flatten(), false, params.virtual, params.size, params.spacing, params.nThreads );
				toImagePlus( data, ltm, bwTransform, params.ignoreAffine, params.flatten(), true, params.virtual, params.size, params.spacing, params.nThreads );
			}
			else
			{
				final boolean inverse = params.inverseOption.equals( INVERSE_OPTIONS.INVERSE.toString() );
				toImagePlus( data, ltm, bwTransform, params.ignoreAffine, params.flatten(), inverse, params.virtual, params.size, params.spacing, params.nThreads );
			}
		}
		else
		{
			try
			{
				if ( params.inverseOption.equals( INVERSE_OPTIONS.BOTH.toString() ) )
				{
					writeN5( params.n5Base, params.n5Dataset + "/dfield", ltm, bwTransform, data, dims, spacing, offset, unit, params.blockSize, params.compression, params.nThreads, params.ignoreAffine, params.flatten(), false );
					writeN5( params.n5Base, params.n5Dataset + "/invdfield", ltm, bwTransform, data, dims, spacing, offset, unit, params.blockSize, params.compression, params.nThreads, params.ignoreAffine, params.flatten(), true );
				}
				else
				{
					final boolean inverse = params.inverseOption.equals( INVERSE_OPTIONS.INVERSE.toString() );
					writeN5( params.n5Base, params.n5Dataset, ltm, bwTransform, data, dims, spacing, offset, unit, params.blockSize, params.compression, params.nThreads, params.ignoreAffine, params.flatten(), inverse );
				}
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	public void run( final String args )
	{
		if ( IJ.versionLessThan( "1.40" ) )
			return;

		final String macroOptions = Macro.getOptions();
		String options = args;
		if( options == null || options.isEmpty())
			options = macroOptions;

		final boolean isMacro = (options != null && !options.isEmpty());

		if( isMacro )
			ExportDisplacementFieldFrame.runMacro( macroOptions );
		else
			ExportDisplacementFieldFrame.createAndShow();
	}

	public void runBackup( final String arg )
	{
		if ( IJ.versionLessThan( "1.40" ) )
			return;

		DeformationFieldExportParameters params = DeformationFieldExportParameters.fromDialog( true, true );
		int nd = params.size.length;

		String unit = "pixel";
		// load
		LandmarkTableModel ltm = new LandmarkTableModel( nd );
		try
		{
			ltm.load( new File( params.landmarkPath ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return;
		}

		if( params.n5Base.isEmpty() )
		{
			if ( params.inverseOption.equals( INVERSE_OPTIONS.BOTH.toString() ) )
			{
				toImagePlus( null, ltm, null, params.ignoreAffine, params.flatten(), false, params.virtual, params.size, params.spacing, params.nThreads );
				toImagePlus( null, ltm, null, params.ignoreAffine, params.flatten(), true, params.virtual, params.size, params.spacing, params.nThreads );
			}
			else
			{
				final boolean inverse = params.inverseOption.equals( INVERSE_OPTIONS.INVERSE.toString() );
				toImagePlus( null, ltm, null, params.ignoreAffine, params.flatten(), inverse, params.virtual, params.size, params.spacing, params.nThreads );
			}
		}
		else
		{
			try
			{
				if ( params.inverseOption.equals( INVERSE_OPTIONS.BOTH.toString() ) )
				{
					writeN5( params.n5Base, params.n5Dataset + "/dfield",    ltm, null, null, params.size, params.spacing, params.offset, unit, params.blockSize, params.compression, params.nThreads, params.ignoreAffine, params.flatten(), false );
					writeN5( params.n5Base, params.n5Dataset + "/invdfield", ltm, null, null, params.size, params.spacing, params.offset, unit, params.blockSize, params.compression, params.nThreads, params.ignoreAffine, params.flatten(), true );
				}
				else
				{
					final boolean inverse = params.inverseOption.equals( INVERSE_OPTIONS.INVERSE.toString() );
					writeN5( params.n5Base, params.n5Dataset, ltm, null, null, params.size, params.spacing, params.offset, unit, params.blockSize, params.compression, params.nThreads, params.ignoreAffine, params.flatten(), inverse );
				}
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
	}
	public static ImagePlus toImagePlus(
			final BigWarpData<?> data,
			final LandmarkTableModel ltm,
			final BigWarpTransform bwTransform,
			final boolean ignoreAffine,
			final boolean flatten,
			final boolean inverse,
			final boolean virtual,
			final long[] dims,
			final double[] spacing,
			final int nThreads )
	{
		final double[] offset = new double[ spacing.length ];
		return toImagePlus( data, ltm, bwTransform, ignoreAffine, flatten, inverse, virtual, dims, spacing, offset, nThreads );
	}

	/**
	 * 
	 * @param data the {@link BigWarpData}
	 * @param ltm the {@link LandmarkTableModel}
	 * @param bwTransform the {@link BigWarpTransform}
	 * @param ignoreAffine omit the affine part of the transformation
	 * @param flatten include any fixed transformation
	 * @param inverse output the inverse transformation
	 * @param virtual output of virtual image
	 * @param dims the dimensions of the output {@link ImagePlus}'s spatial dimensions
	 * @param spacing the pixel spacing of the output field
	 * @param offset the physical offset (origin) of the output field
	 * @param nThreads number of threads for copying
	 * @return the image plus representing the displacement field
	 */
	public static ImagePlus toImagePlus(
			final BigWarpData<?> data,
			final LandmarkTableModel ltm,
			final BigWarpTransform bwTransform,
			final boolean ignoreAffine,
			final boolean flatten,
			final boolean inverse,
			final boolean virtual,
			final long[] dims,
			final double[] spacing,
			final double[] offset,
			final int nThreads )
	{
		BigWarpTransform bwXfm;
		if( bwTransform == null )
			bwXfm = new BigWarpTransform( ltm, BigWarpTransform.TPS );
		else
			bwXfm = bwTransform;

		final InvertibleRealTransform fwdTransform = getTransformation( data, bwXfm, flatten );
		final InvertibleRealTransform startingTransform = inverse ? fwdTransform.inverse() : fwdTransform;

		int nd = ltm.getNumdims();
		long[] ipDims = null;
		if ( nd == 2 )
		{
			ipDims = new long[ 4 ];
			ipDims[ 0 ] = dims[ 0 ];
			ipDims[ 1 ] = dims[ 1 ];
			ipDims[ 2 ] = 2;
			ipDims[ 3 ] = 1;
		}
		else if ( nd == 3 )
		{
			ipDims = new long[ 4 ];
			ipDims[ 0 ] = dims[ 0 ];
			ipDims[ 1 ] = dims[ 1 ];
			ipDims[ 2 ] = 3;
			ipDims[ 3 ] = dims[ 2 ];
		}
		else
			return null;

		final RandomAccessibleInterval< DoubleType > dfieldVirt = DisplacementFieldTransform.createDisplacementField( startingTransform, new FinalInterval( dims ), spacing, offset );

		ImagePlus dfieldIp;
		if( virtual )
		{
			final RealFloatConverter<DoubleType> conv = new RealFloatConverter<>();
			final RandomAccessibleInterval< FloatType > dfieldF = Views.moveAxis(
						Converters.convert2( dfieldVirt, conv, FloatType::new ),
						0, 2 );
			dfieldIp = ImageJFunctions.wrap( dfieldF, "" ); // title gets set below
		}
		else
		{
			final FloatImagePlus< FloatType > dfield = ImagePlusImgs.floats( ipDims );

			// make the "vector" axis the first dimension
			RandomAccessibleInterval< FloatType > dfieldImpPerm = Views.moveAxis( dfield, 2, 0 );
			LoopBuilder.setImages( dfieldVirt, dfieldImpPerm ).multiThreaded( TaskExecutors.fixedThreadPool( nThreads ) ).forEachPixel( (x,y) -> { y.setReal(x.get()); });
			dfieldIp = dfield.getImagePlus();
		}

		String title = "bigwarp dfield";
		if ( ignoreAffine )
			title += " (no affine)";

		dfieldIp.setTitle( title );

		dfieldIp.getCalibration().pixelWidth = spacing[ 0 ];
		dfieldIp.getCalibration().pixelHeight = spacing[ 1 ];

		dfieldIp.getCalibration().xOrigin = offset[ 0 ];
		dfieldIp.getCalibration().yOrigin = offset[ 1 ];

		if( spacing.length > 2 )
		{
			dfieldIp.getCalibration().pixelDepth = spacing[ 2 ];
			dfieldIp.getCalibration().zOrigin = offset[ 2 ];
		}

		dfieldIp.show();
		return dfieldIp;
	}

	/**
	 * Returns the transformation to be converted to a displacement field.
	 * 
	 * @param data the bigwarp data storing the fixed transformations
	 * @param transform the current transformation
	 * @param concat concatenate the current with fixed transformation
	 * @return the transformation
	 */
	protected static InvertibleRealTransform getTransformation( final BigWarpData<?> data, final BigWarpTransform transform, final boolean concat )
	{
		InvertibleRealTransform tps = transform.getTransformation( false );
		if( data == null || !concat )
			return tps;

		InvertibleRealTransform preTransform = null;
		for ( Entry< Integer, SourceInfo > entry : data.sourceInfos.entrySet() )
		{
			if ( entry.getValue().getTransform() != null )
			{
				RealTransform tform = entry.getValue().getTransform();
				if( tform instanceof InvertibleRealTransform )
				{
					preTransform = ( InvertibleRealTransform ) tform;
				}
				else
				{
					WrappedIterativeInvertibleRealTransform< RealTransform > ixfm = new WrappedIterativeInvertibleRealTransform<>( tform );
					// use same parameters as the passed transform, hopefully that works
					// alternatively, I could wrap the sequence in the iterative invertible transform - there are tradeoffs here
					// TODO consider the tradeoffs
					ixfm.getOptimzer().setMaxIters( transform.getInverseMaxIterations() );
					ixfm.getOptimzer().setTolerance( transform.getInverseTolerance() );
					preTransform = ixfm;
				}
				break;
			}
		}

		final InvertibleRealTransform startingTransform;
		if ( preTransform != null )
		{
			final InvertibleRealTransformSequence seq = new InvertibleRealTransformSequence();
			seq.add( tps );
			seq.add( preTransform );
			startingTransform = seq;
		}
		else
			startingTransform = tps;

		return startingTransform;
	}

	public static void writeN5( 
			final String n5BasePath, 
			final LandmarkTableModel ltm,
			final BigWarpTransform bwTransform,
			final BigWarpData<?> data,
			final long[] dims,
			final double[] spacing,
			final double[] offset,
			final String unit,
			final int[] spatialBlockSize,
			final Compression compression,
			final int nThreads,
			final boolean flatten,
			final boolean inverse ) throws IOException 
	{
		writeN5( n5BasePath, N5DisplacementField.FORWARD_ATTR, ltm, bwTransform, data, dims, spacing, offset, unit, spatialBlockSize, compression, nThreads, flatten, inverse );
	}

	public static void writeN5(
			final String n5BasePath,
			final String n5Dataset,
			final LandmarkTableModel ltm,
			final BigWarpTransform bwTransform,
			final BigWarpData<?> data,
			final long[] dims,
			final double[] spacing,
			final double[] offset,
			final String unit,
			final int[] spatialBlockSize,
			final Compression compression,
			final int nThreads,
			final boolean flatten,
			final boolean inverse ) throws IOException
	{
		writeN5( n5BasePath, n5Dataset, ltm, bwTransform, data, dims, spacing, offset, unit, spatialBlockSize, compression, nThreads, false, flatten, inverse );
	}

	public static void writeN5(
			final String n5BasePath,
			final String n5Dataset,
			final LandmarkTableModel ltm,
			final BigWarpTransform bwTransform,
			final BigWarpData<?> data,
			final long[] dims,
			final double[] spacing,
			final double[] offset,
			final String unit,
			final int[] spatialBlockSizeArg,
			final Compression compression,
			final int nThreads,
			final boolean splitAffine,
			final boolean flatten,
			final boolean inverse ) throws IOException
	{
		final String dataset = ( n5Dataset == null || n5Dataset.isEmpty() ) ? N5DisplacementField.FORWARD_ATTR : n5Dataset;

		final String mvgSpaceName = data != null ? data.getMovingSource( 0 ).getSpimSource().getName() : "moving";
		final String tgtSpaceName = data != null ? data.getTargetSource( 0 ).getSpimSource().getName() : "target";
		final String inputSpace;
		final String outputSpace;
		if( inverse )
		{
			inputSpace = mvgSpaceName;
			outputSpace = tgtSpaceName;
		}
		else
		{
			inputSpace = tgtSpaceName;
			outputSpace = mvgSpaceName;
		}

		final BigWarpTransform bwXfm;
		if( bwTransform == null )
			bwXfm = new BigWarpTransform( ltm, BigWarpTransform.TPS );
		else
			bwXfm = bwTransform;

//		final RealTransform tpsTotal = bwXfm.getTransformation( false );
		final InvertibleRealTransform fwdTransform = getTransformation( data, bwXfm, flatten );
		final InvertibleRealTransform totalTransform = inverse ? fwdTransform.inverse() : fwdTransform;

		AffineGet affine = null;
		if ( splitAffine )
		{
			final double[][] affineArray = bwXfm.affinePartOfTpsHC();
			if ( affineArray.length == 2 )
			{
				final AffineTransform2D affine2d = new AffineTransform2D();
				affine2d.set( affineArray );
				affine = affine2d;
			}
			else
			{
				final AffineTransform3D affine3d = new AffineTransform3D();
				affine3d.set( affineArray );
				affine = affine3d;
			}
		}

		int[] spatialBlockSize = fillBlockSize( spatialBlockSizeArg, ltm.getNumdims() );
		int[] blockSize = new int[ spatialBlockSize.length + 1 ];
		blockSize[ 0 ] = spatialBlockSize.length;
		System.arraycopy( spatialBlockSize, 0, blockSize, 1, spatialBlockSize.length );

		final N5Writer n5 = new N5Factory().openWriter( n5BasePath );
//		N5DisplacementField.save( n5, dataset, affine, dfield, spacing, blockSize, compression );

		final RandomAccessibleInterval< DoubleType > dfield;
		if( affine != null )
		{
			// the affine part
			NgffAffineTransformation ngffAffine = new NgffAffineTransformation( affine.getRowPackedCopy() );

			// displacement field (with the affine removed)
			RealTransformSequence totalNoAffine = new RealTransformSequence();
			totalNoAffine.add( totalTransform );
			totalNoAffine.add( affine.inverse() );
			dfield = DisplacementFieldTransform.createDisplacementField( totalNoAffine, new FinalInterval( dims ), spacing );
			NgffDisplacementsTransformation dfieldTform = NgffTransformations.save( n5, dataset, dfield, inputSpace, outputSpace, spacing, offset, unit, blockSize, compression, nThreads );

			// the total transform
			NgffSequenceTransformation totalTform = new NgffSequenceTransformation( inputSpace, outputSpace,
					new CoordinateTransformation[]{ dfieldTform, ngffAffine  });

			N5DisplacementField.addCoordinateTransformations( n5, "/", totalTform );
		}
		else
		{
			dfield = DisplacementFieldTransform.createDisplacementField( totalTransform, new FinalInterval( dims ), spacing );
			NgffDisplacementsTransformation ngffTform = NgffTransformations.save( n5, dataset, dfield, inputSpace, outputSpace, spacing, offset, unit, blockSize, compression, nThreads );
			N5DisplacementField.addCoordinateTransformations( n5, "/", ngffTform );
		}

		n5.close();
	}

	private static int[] fillBlockSize(int[] blockSize, int N)
	{
		if( blockSize.length >= N )
			return blockSize;
		else
		{
			final int[] out = new int[ N ];
			int j = blockSize.length - 1;
			for ( int i = 0; i < N; i++ )
			{
				if ( i < blockSize.length )
					out[ i ] = blockSize[ i ];
				else
					out[ i ] = blockSize[ j ];
			}
			return out;
		}
	}

	private static RealTransform getTpsAffineToggle( BigWarpTransform bwXfm, boolean splitAffine )
	{
		if ( splitAffine )
		{
			final ThinPlateR2LogRSplineKernelTransform tps = bwXfm.getTpsBase();
			return new ThinplateSplineTransform(
					new ThinPlateR2LogRSplineKernelTransform( tps.getSourceLandmarks(), null, null, tps.getKnotWeights() ) );
		}
		else
			return bwXfm.getTransformation( false );
	}

	/**
	 * @param tps
	 *
	 * @return
	 *
	 * @deprecated Use {@link BigWarpTransform} method instead
	 */
	@Deprecated()
	public static AffineGet toAffine( final ThinPlateR2LogRSplineKernelTransform tps )
	{
		double[] affineFlat = toFlatAffine( tps );
		if( affineFlat.length == 6 )
		{
			final AffineTransform2D affine = new AffineTransform2D();
			affine.set( affineFlat );
			return affine;
		}
		else if( affineFlat.length == 12 )
		{
			final AffineTransform3D affine = new AffineTransform3D();
			affine.set( affineFlat );
			return affine;
		}
		else
			return null;
	}

	public static double[] toFlatAffine( final ThinPlateR2LogRSplineKernelTransform tps )
	{
		// move this method somewhere more central?

		final double[][] tpsAffine = tps.getAffine();
		final double[] translation = tps.getTranslation();

		double[] affine;
		if( tps.getNumDims() == 2)
		{
			affine = new double[ 6 ];

			affine[ 0 ] = 1 + tpsAffine[ 0 ][ 0 ];
			affine[ 1 ] = tpsAffine[ 0 ][ 1 ];
			affine[ 2 ] = translation[ 0 ];

			affine[ 3 ] = tpsAffine[ 1 ][ 0 ];
			affine[ 4 ] = 1 + tpsAffine[ 1 ][ 1 ];
			affine[ 5 ] = translation[ 1 ];
		}
		else
		{
			affine = new double[ 12 ];

			affine[ 0 ] = 1 + tpsAffine[ 0 ][ 0 ];
			affine[ 1 ] = tpsAffine[ 0 ][ 1 ];
			affine[ 2 ] = tpsAffine[ 0 ][ 2 ];
			affine[ 3 ] = translation[ 0 ];

			affine[ 4 ] = tpsAffine[ 1 ][ 0 ];
			affine[ 5 ] = 1 + tpsAffine[ 1 ][ 1 ];
			affine[ 6 ] = tpsAffine[ 1 ][ 2 ];
			affine[ 7 ] = translation[ 1 ];

			affine[ 8 ] = tpsAffine[ 2 ][ 0 ];
			affine[ 9 ] = tpsAffine[ 2 ][ 1 ];
			affine[ 10 ] = 1 + tpsAffine[ 2 ][ 2 ];
			affine[ 11 ] = translation[ 2 ];
		}
		return affine;
	}

	public static long[] dimensionsFromImagePlus( final ImagePlus ref_imp )
	{
		long[] dims;
		if( ref_imp.getNSlices() < 2 )
		{
			dims = new long[ 2 ];
			dims[ 0 ] = ref_imp.getWidth();
			dims[ 1 ] = ref_imp.getHeight();
		} 
		else
		{
			dims = new long[ 3 ];
			dims[ 0 ] = ref_imp.getWidth();
			dims[ 1 ] = ref_imp.getHeight();
			dims[ 2 ] = ref_imp.getNSlices();
		}
		return dims;
	}

	@Deprecated
	public static FloatImagePlus< FloatType > convertToDeformationField(
			final long[] dims,
			final RealTransform transform,
			final AffineGet pixToPhysical,
			final int nThreads)
	{
		FloatImagePlus< FloatType > deformationField = ImagePlusImgs.floats( dims );

		RandomAccessibleInterval<FloatType> dfieldPermuted = deformationField;
		if( dims.length == 4 )
			dfieldPermuted = Views.permute( deformationField, 2, 3 );

		if( nThreads <= 1 )
			fromRealTransform( transform, pixToPhysical, dfieldPermuted );
		else
			fromRealTransform( transform, pixToPhysical, dfieldPermuted, nThreads );
		
		return deformationField;
	}
	
	public static boolean areTransformsTheSame( RealTransform xfm1, RealTransform xfm2, Interval itvl, final double EPS )
	{

		double[] pArray = new double[ 3 ];
		double[] qArray = new double[ 3 ];
		RealPoint p = RealPoint.wrap( pArray );
		RealPoint q = RealPoint.wrap( qArray );

		IntervalIterator c = new IntervalIterator( itvl );
		while ( c.hasNext() )
		{
			c.fwd();
			xfm1.apply( c, p );
			xfm2.apply( c, q );

			for( int d = 0; d < itvl.numDimensions(); d++ )
				if( Math.abs( p.getDoublePosition( d ) - q.getDoublePosition( d )) > EPS )
					return false;
		}
		return true;
	}
	
	/**
	 * Converts a {@link RealTransform} into a deformation field.
	 * 
	 * Writes the result into the passed {@link RandomAccessibleInterval}. If
	 * the transform has N source dimensions, then the deformation field must
	 * have at least N+1 dimensions where the last dimensions of of length at
	 * least N.  
	 * 
	 * A DeformationField creating with the resulting {@link RandomAccessibleInterval}
	 * will give the same results as the transform inside its Interval.
	 * 
     * @param <T> the type of the deformation field
	 * @param transform
	 *            the {@link RealTransform} to convert
	 * @param pixelToPhysical
	 * 			  the transform from coordinates in the {@link RandomAccessibleInterval} to
	 * 		      physical units
	 * @param deformationField
	 *            the {@link RandomAccessibleInterval} into which the
	 *            displacement field will be written
	 */
	@Deprecated
	public static < T extends RealType< T > > void fromRealTransform( 
			final RealTransform transform, 
			final AffineGet pixelToPhysical,
			final RandomAccessibleInterval< T > deformationField )
	{
		assert deformationField.numDimensions() == ( transform.numSourceDimensions() + 1 );
		assert deformationField.dimension( deformationField.numDimensions() - 1 ) >= transform.numSourceDimensions();

		int N = transform.numSourceDimensions();
		RealPoint p = new RealPoint( transform.numTargetDimensions() );
		RealPoint q = new RealPoint( transform.numTargetDimensions() );

		CompositeIntervalView< T, ? extends GenericComposite< T > > col = Views.collapse( deformationField );
		Cursor< ? extends GenericComposite< T > > c = Views.flatIterable( col ).cursor();
		while ( c.hasNext() )
		{
			GenericComposite< T > displacementVector = c.next();

			// transform the location of the cursor
			// and store the displacement
			pixelToPhysical.apply( c, p );
			transform.apply( p, q );

			for ( int i = 0; i < N; i++ )
				displacementVector.get( i ).setReal( q.getDoublePosition( i ) - p.getDoublePosition( i ) ); 
		}
	}
	
	/**
	 * Converts a {@link RealTransform} into a deformation field.
	 * 
	 * Writes the result into the passed {@link RandomAccessibleInterval}. If
	 * the transform has N source dimensions, then the deformation field must
	 * have at least N+1 dimensions where the last dimensions of of length at
	 * least N.  
	 * 
	 * A DeformationField creating with the resulting {@link RandomAccessibleInterval}
	 * will give the same results as the transform inside its Interval.
	 * 
     * @param <T> the type of the deformation field
	 * @param transform
	 *            the {@link RealTransform} to convert
	 * @param pixelToPhysical
	 * 			  the transform from coordinates in the {@link RandomAccessibleInterval} to
	 * 		      physical units
	 * @param deformationField
	 *            the {@link RandomAccessibleInterval} into which the
	 *            displacement field will be written
	 * @param nThreads
	 *            the number of threads
	 */
	@Deprecated
	public static < T extends RealType< T > > void fromRealTransform( final RealTransform transform, 
			final AffineGet pixelToPhysical,
			final RandomAccessibleInterval< T > deformationField,
			int nThreads)
	{
		assert deformationField.numDimensions() == ( transform.numSourceDimensions() + 1 );
		assert deformationField.dimension( deformationField.numDimensions() - 1 ) >= transform.numSourceDimensions();

		final int ndims = transform.numSourceDimensions();
		final long[] splitPoints = new long[ nThreads + 1 ];
		long N;
		final int dim2split;
		if( ndims == 2 ) 
		{
			N = deformationField.dimension( 1 );
			dim2split = 1;
		}
		else
		{
			N = deformationField.dimension( 2 );
			dim2split = 2;
		}

		long del = ( N / nThreads );
		splitPoints[ 0 ] = 0;
		splitPoints[ nThreads ] = deformationField.dimension( dim2split );
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

			final RealTransform transformCopy = transform.copy();
			final RealTransform toPhysicalCopy = pixelToPhysical.copy();
			
			jobs.add( new Callable<Boolean>()
			{
				@Override
				public Boolean call()
				{
					try
					{
						RealPoint p = new RealPoint( transform.numTargetDimensions() );
						RealPoint q = new RealPoint( transform.numTargetDimensions() );

						final FinalInterval subItvl = BigWarpExporter.getSubInterval( deformationField, dim2split, start, end );
						CompositeIntervalView< T, ? extends GenericComposite< T > > col = Views.collapse( deformationField );
						final IntervalView< ? extends GenericComposite< T > > subTgt = Views.interval( col, subItvl );

						Cursor< ? extends GenericComposite< T > > c = Views.flatIterable( subTgt ).cursor();
						while ( c.hasNext() )
						{
							GenericComposite< T > displacementVector = c.next();

							// transform the location of the cursor
							// and store the displacement
							toPhysicalCopy.apply( c, p );
							transformCopy.apply( p, q );

							for ( int i = 0; i < ndims; i++ )
								displacementVector.get( i ).setReal( q.getDoublePosition( i ) - p.getDoublePosition( i ) ); 
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
			for( Future<Boolean> f : futures )
					f.get();

			threadPool.shutdown(); // wait for all jobs to finish
		}
		catch ( InterruptedException e1 )
		{
			e1.printStackTrace();
		}
		catch ( ExecutionException e )
		{
			e.printStackTrace();
		}
	}

	public static Compression getCompression( final String compressionArg )
	{
		switch (compressionArg) {
		case N5Exporter.GZIP_COMPRESSION:
			return new GzipCompression();
		case N5Exporter.LZ4_COMPRESSION:
			return new Lz4Compression();
		case N5Exporter.XZ_COMPRESSION:
			return new XzCompression();
		case N5Exporter.RAW_COMPRESSION:
			return new RawCompression();
		case N5Exporter.BLOSC_COMPRESSION:
			return new BloscCompression();
		default:
			return new RawCompression();
		}
	}
	
	/**
	 * A helper that stores the parameters for export
	 * and can prompt the user for these parameters.
	 * 
	 */
	public static class DeformationFieldExportParameters 
	{
		public final String landmarkPath;
		public final boolean ignoreAffine;
		public final String option;
		public final String inverseOption;
		public final boolean virtual;
		public final int nThreads;

		public final long[] size;
		public final double[] spacing;
		public final double[] offset;
		public final String unit;

		public final String n5Base;
		public final String n5Dataset;
		public final Compression compression;
		public final int[] blockSize;

		public DeformationFieldExportParameters(
				final String landmarkPath,
				final boolean ignoreAffine,
				final String option,
				final String inverseOption,
				final boolean virtual,
				final int nThreads,
				final long[] size,
				final double[] spacing,
				final double[] offset,
				final String unit,
				final String n5Base,
				final String n5Dataset,
				final int[] blockSize, 
				final Compression compression )
		{
			this.landmarkPath = landmarkPath;
			this.ignoreAffine = ignoreAffine;
			this.option = option;
			this.inverseOption = inverseOption;
			this.virtual = virtual;
			this.nThreads = nThreads;

			this.size = size;
			this.spacing = spacing;
			this.offset = offset;
			this.unit = unit;

			this.n5Base = n5Base;
			this.n5Dataset = n5Dataset;
			this.blockSize  = blockSize;
			this.compression = compression;
		}

		public boolean flatten()
		{
			return option.equals( flattenOption );
		}

		public static DeformationFieldExportParameters fromDialog( 
				final boolean promptLandmarks,
				final boolean promptReference )
		{
			final GenericDialogPlus gd = new GenericDialogPlus( "BigWarp to Deformation" );
			gd.addMessage( "Deformation field export:" );
			if( promptLandmarks )
			{
				gd.addFileField( "landmarks_image_file", "" );
			}

			gd.addCheckbox( "Split affine part", false );

			final String[] choices = new String[] { flattenOption, sequenceOption };
			gd.addChoice( "type", choices, flattenOption );

			final String[] invChoices = new String[] { INVERSE_OPTIONS.FORWARD.toString(), INVERSE_OPTIONS.INVERSE.toString(), INVERSE_OPTIONS.BOTH.toString() };
			gd.addChoice( "direction", invChoices, INVERSE_OPTIONS.FORWARD.toString() );

			gd.addCheckbox( "virtual", false );
			gd.addNumericField( "threads", 1, 0 );
			gd.addMessage( "Size and spacing" );

			final int[] ids = WindowManager.getIDList();
			if( promptReference )
			{
				final String[] titles = new String[ ids.length + 1 ];
				for ( int i = 0; i < ids.length; ++i )
				{
					titles[ i ] = ( WindowManager.getImage( ids[ i ] ) ).getTitle();
				}
				titles[ ids.length ] = "None";

				final String current = WindowManager.getCurrentImage().getTitle();
				gd.addChoice( "reference_image", titles, current );
			}
			gd.addStringField( "output size", "");
			gd.addStringField( "output spacing", "");
			gd.addStringField( "output offset", "");
			gd.addStringField( "output unit", "pixel");

			gd.addMessage( "Leave n5 path empty to export as ImagePlus" );
			gd.addDirectoryOrFileField( "n5 root path", "" );
			gd.addStringField( "n5 dataset", N5DisplacementField.FORWARD_ATTR );
			gd.addStringField( "n5 block size", "32,32,32");
			gd.addChoice( "n5 compression", compressionOptions, N5Exporter.GZIP_COMPRESSION );
			gd.showDialog();

			if ( gd.wasCanceled() )
				return null;

			String landmarkPath = null;
			if( promptLandmarks )
				landmarkPath = gd.getNextString();

			final boolean ignoreAffine = gd.getNextBoolean();
			final String option = gd.getNextChoice();
			final String direction = gd.getNextChoice();
			final boolean virtual = gd.getNextBoolean();
			final int nThreads = ( int ) gd.getNextNumber();

			ImagePlus ref_imp = null;
			if( promptReference )
			{
				final int idx = ids[ gd.getNextChoiceIndex() ] ;
				if( idx < ids.length )
					ref_imp = WindowManager.getImage( idx );
			}

			final String sizeString = gd.getNextString();
			final String spacingString = gd.getNextString();
			final String offsetString = gd.getNextString();
			final String unitString = gd.getNextString();

			final String n5Base = gd.getNextString();
			final String n5Dataset = gd.getNextString();
			final String n5BlockSizeString = gd.getNextString();
			final String n5CompressionString = gd.getNextChoice();

			final Compression compression = getCompression( n5CompressionString );
			final int[] blockSize = n5BlockSizeString.isEmpty() ? null : 
				Arrays.stream( n5BlockSizeString.split( "," ) ).mapToInt( Integer::parseInt ).toArray();

			final long[] size;
			final double[] spacing;
			final double[] offset;
			String unit = "pixel";
			if( ref_imp == null )
			{
				if( !sizeString.isEmpty())
					size = Arrays.stream( sizeString.split( "," ) ).mapToLong( Long::parseLong ).toArray();
				else
					size = null;

				if( !spacingString.isEmpty() )
					spacing = Arrays.stream( spacingString.split( "," ) ).mapToDouble( Double::parseDouble ).toArray();
				else
					spacing = null;

				if( !offsetString.isEmpty() )
					offset = Arrays.stream( offsetString.split( "," ) ).mapToDouble( Double::parseDouble ).toArray();
				else
					offset = null;

				if( !unitString.isEmpty() )
					unit = unitString;
			}
			else
			{
				int nd = 2;
				if ( ref_imp.getNSlices() > 1 )
					nd = 3;

				// account for physical units of reference image
				spacing = new double[ nd ];
				offset = new double[ nd ];
				spacing[ 0 ] = ref_imp.getCalibration().pixelWidth;
				spacing[ 1 ] = ref_imp.getCalibration().pixelHeight;

				offset[ 0 ] = ref_imp.getCalibration().xOrigin;
				offset[ 1 ] = ref_imp.getCalibration().yOrigin;

				if ( nd > 2 )
				{
					spacing[ 2 ] = ref_imp.getCalibration().pixelDepth;
					offset[ 2 ] = ref_imp.getCalibration().zOrigin;
				}

				size = BigWarpToDeformationFieldPlugIn.dimensionsFromImagePlus( ref_imp );
			}

			return new DeformationFieldExportParameters( 
					landmarkPath,
					ignoreAffine,
					option,
					direction,
					virtual,
					nThreads,
					size,
					spacing,
					offset,
					unit,
					n5Base,
					n5Dataset,
					blockSize,
					compression );
		}
		
		public JFrame makeDialog()
		{
			ExportDisplacementFieldFrame frame = new ExportDisplacementFieldFrame( null );
			return frame;
		}
		
	}

}

