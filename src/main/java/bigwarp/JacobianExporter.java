package bigwarp;


import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.source.JacobianRandomAccess;
import ij.ImagePlus;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.inverse.DifferentiableRealTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class JacobianExporter < T extends RealType< T > & NativeType< T > > extends BigWarpRealExporter< T >
{

	final DifferentiableRealTransform xfm;

	boolean doDeterminant = true;

	public JacobianExporter( final T baseType, final DifferentiableRealTransform xfm )
	{
		super( baseType );
		this.xfm = xfm;
	}
	
	public void setDeterminant( final boolean doDeterminant )
	{
		this.doDeterminant = doDeterminant;
	}

	public ImagePlus export()
	{
		return null;
	}
	
	public RandomAccessibleInterval< T > exportRai()
	{
		int nsd = xfm.numTargetDimensions();
		
		buildTotalRenderTransform();
		
		VoxelDimensions voxdim = new FinalVoxelDimensions( "um",
				resolutionTransform.get( 0, 0 ),
				resolutionTransform.get( 1, 1 ),
				resolutionTransform.get( 2, 2 ));


		final RealRandomAccessible< T > raiRaw;
		if( doDeterminant )
			raiRaw = JacobianRandomAccess.create( nsd, baseType, xfm );


		final RealRandomAccess<T> ra;


//		// Lets try typing
//		// apply the transformations
//		final AffineRandomAccessible< T, AffineGet > rai = RealViews.affine( 
//				raiRaw, pixelRenderToPhysical.inverse() );
//
//		System.out.println( "outputInterval: " + Util.printInterval( outputInterval ));
//		raiList.add( Views.interval( Views.raster( rai ), outputInterval ) );
//
//		RandomAccessibleInterval< T > raiStack = Views.stack( raiList );
//
//		return raiStack;
		
		return null;
	}
	
	public static <T extends RealType<T>> RandomAccessibleInterval<T> apply(
			final ImagePlus reference,
			final LandmarkTableModel landmarks,
			final String fieldOfViewOption,
			final String fieldOfViewPointFilter,
			final String resolutionOption,
			final double[] resolutionSpec,
			final double[] fovSpec,
			final double[] offsetSpec,
			final boolean isVirtual)
	{
		return null;
	}

}
