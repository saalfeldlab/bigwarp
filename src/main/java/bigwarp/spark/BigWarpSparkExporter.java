package bigwarp.spark;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.ij.N5Factory;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.PhysicalMetadata;

import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.basictypeaccess.AccessFlags;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.RandomAccessibleOnRealRandomAccessible;
import net.imglib2.view.Views;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class BigWarpSparkExporter implements Callable< Void >
{

	@Option(
			names = {"-i", "--n5url"},
			required = true,
			description = "N5 URL, e.g. 'https://janelia-cosem.s3.amazonaws.com/jrc_hela-2/jrc_hela-2.n5'")
	private String n5Url = null;

	@Option(
			names = {"-d", "--n5dataset"},
			required = true,
			description = "N5 dataset, e.g. '/em/fibsem-uint16/s4'")
	private String n5Dataset = null;

	@Option(
			names = {"-o", "--n5outurl"},
			required = true,
			description = "N5 output URL, e.g. '/home/saalfeld/tmp/jrc_hela-2.n5'")
	private String n5OutUrl = null;

	@Option(
			names = {"-e", "--n5outdataset"},
			required = true,
			description = "N5 output dataset, e.g. '/em/fibsem-uint16/s4'")
	private String n5OutDataset = null;

	@Option(
			names = { "-l", "--landmarks"},
			required = true,
			description = "BigWarp landmarks csv")
	private String landmarksPath;
	
	@Option(
			names = { "-t", "--transform"},
			required = false,
			description = "Transform type option")
	private String transformTypeOption = "TPS";
	
	@Option(
			names = {"-s", "--output-dimensions"},
			required = true,
			split = ",",
			description = "Output image size (pixels)")
	private long[] outSz;

	@Option(
			names = {"-r", "--output-resolution"},
			required = true,
			split = ",",
			description = "Output image resolution" )
	private double[] outResolution;

	@Option(
			names = {"-f", "--output-offset"},
			required = false,
			split = ",",
			description = "Output image offset (physical)")
	private double[] outOffset = new double[ 3 ];
	
	@Option(
			names = {"--interpolation" },
			required = false,
			description = "Interpolation type (linear, nearest)")
	private String interpTypeOption = "linear";


	public static void main( String[] args )
	{
		new CommandLine(new BigWarpSparkExporter()).execute(args);
	}
	
	public BigWarpSparkExporter() { }

	@Override
	public Void call() throws Exception
	{
		/* create Spark context */
		final SparkConf conf = new SparkConf().setAppName( this.getClass().getName() );
		final JavaSparkContext sc = new JavaSparkContext( conf );

		System.out.println( "sc: " + sc );

		/* get some data about the input */
		final N5Reader n5 = new N5Factory().openReader( n5Url );
		final DatasetAttributes attributes = n5.getDatasetAttributes( n5Dataset );

		/* create the output */
		final N5Writer n5Writer = new N5Factory().openWriter( n5OutUrl );
		n5Writer.createDataset( n5OutDataset, attributes );

		/* create the grid for parallelization */
		final List< long[][] > grid = Grid.create( attributes.getDimensions(), attributes.getBlockSize() );

		/* Sparkify it */
		final JavaRDD< long[][] > rddGrid = sc.parallelize( grid );
		
		run( sc, n5Url, n5Dataset, n5OutUrl, n5OutDataset,
				landmarksPath, transformTypeOption, 
				interpTypeOption, outResolution, outOffset,
				rddGrid );

		return null;
	}
	
	private static final <T extends NativeType<T> & RealType<T>> void run(
			final JavaSparkContext sc,
			final String n5Url,
			final String n5Dataset,
			final String n5OutUrl,
			final String n5OutDataset,
			final String landmarksPath,
			final String transformTypeOption,
			final String interpTypeOption,
			final double[] outResolution,
			final double[] outOffset,
			final JavaRDD<long[][]> rddGrid) throws IOException {


//		final RandomAccessibleInterval<T> img = Singleton.get(
//				n5Url + ":" + n5Dataset,
//				() -> (RandomAccessibleInterval<T>)N5Utils.open(n5, n5Dataset));
		
		
//		final N5TreeNode node = new N5TreeNode( n5Dataset, false );
//		N5DatasetDiscoverer.parseMetadata( n5, node, N5Importer.PARSERS, null );
//
//		AffineTransform3D imgResTransform = null;
//		N5Metadata meta = node.getMetadata();
//		if( meta == null )
//		{
//			System.err.println( "unable to parse metadata for dataset" );
//		}
//
//		if( meta instanceof PhysicalMetadata )
//		{
//			imgResTransform = ((PhysicalMetadata) meta).physicalTransform3d();
//		}
//		else
//		{
//			System.err.println( "metadata for dataset do not specify physical spacing" );
//		}

		rddGrid.foreach( gridBlock -> {

			final N5Reader n5Inner = Singleton.get(
					n5Url + ".reader",
					() -> new N5Factory().openReader(n5Url));

			final RandomAccessibleInterval<T> imgInner = Singleton.get(
					n5Url + ":" + n5Dataset,
					() -> (RandomAccessibleInterval<T>)N5Utils.open(n5Inner, n5Dataset));

			final AffineTransform3D imgPixelToPhysical = Singleton.get(
					n5Url + ":" + n5Dataset + ":physicalTransform",
					() -> { 
						final N5TreeNode lnode = new N5TreeNode( n5Dataset, false );
						N5DatasetDiscoverer.parseMetadata( n5Inner, lnode, N5Importer.PARSERS, null );

						N5Metadata meta = lnode.getMetadata();
						if( meta == null )
						{
							System.err.println( "unable to parse metadata for dataset" );
						}
				
						if( meta instanceof PhysicalMetadata )
						{
							return ((PhysicalMetadata) meta).physicalTransform3d();
						}
						return null;
					});
			
			if( imgPixelToPhysical == null )
			{
				System.err.println( "could not read metadata" );
				return;
			}

			// build total transform
			LandmarkTableModel ltm = new LandmarkTableModel( 3 );
			ltm.load( new File( landmarksPath ));
			final InvertibleRealTransform invXfm = new BigWarpTransform( ltm, transformTypeOption ).getTransformation();

			final AffineTransform3D renderPixelToPhysical = new AffineTransform3D();
			renderPixelToPhysical.concatenate( scaleTransform( outResolution ) );
			renderPixelToPhysical.concatenate( offsetTransform( outOffset ) );

			ExtendedRandomAccessibleInterval< T, RandomAccessibleInterval< T > > imgExt = Views.extendZero( imgInner );
			RealRandomAccessible< T > imgInterp;
			if( interpTypeOption.toLowerCase().equals( "nearest" ))
			{
				imgInterp = Views.interpolate( imgExt, new NLinearInterpolatorFactory<>() );
			}
			else
			{
				imgInterp = Views.interpolate( imgExt, new NearestNeighborInterpolatorFactory<>() );
			}
			
			final RealTransformSequence totalTransform = new RealTransformSequence();
			totalTransform.add( renderPixelToPhysical );
			totalTransform.add( invXfm );
			totalTransform.add( imgPixelToPhysical.inverse() );

			final RealTransformRealRandomAccessible<T,?> imgXfm = new RealTransformRealRandomAccessible< >( imgInterp, totalTransform );
			final RandomAccessibleOnRealRandomAccessible< T > renderedImg = Views.raster( imgXfm );
			final IntervalView< T > block = Views.offsetInterval( renderedImg, gridBlock[ 0 ], gridBlock[1]);

//			/* crop the block of interest */
//			final IntervalView<T> block = Views.offsetInterval(cllcned, gridBlock[0], gridBlock[1]);

			final N5Writer n5Writer = Singleton.get(
					n5OutUrl + ".writer",
					() -> new N5Factory().openWriter(n5OutUrl));

			N5Utils.saveNonEmptyBlock(block, n5Writer, n5OutDataset, gridBlock[2], Util.getTypeFromInterval(block).createVariable());
		});
	}
	
	public static AffineTransform3D scaleTransform( double... res )
	{
		AffineTransform3D resolutionTransform = new AffineTransform3D();
		for( int i = 0; i < res.length; i++ )
			resolutionTransform.set( res[ i ], i, i );

		return resolutionTransform;
	}
	
	public static AffineTransform3D offsetTransform( double... offset )
	{
		AffineTransform3D offsetTransform = new AffineTransform3D();
		for( int i = 0; i < offset.length; i++ )
			offsetTransform.set( offset[ i ], i, 3 );

		return offsetTransform;
	}

}
