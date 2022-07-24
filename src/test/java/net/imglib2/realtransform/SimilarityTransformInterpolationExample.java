package net.imglib2.realtransform;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bdv.gui.TransformTypeSelectDialog; 
import bdv.img.remote.AffineTransform3DJsonSerializer;
import bdv.util.Affine3DHelpers;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.animate.AbstractTransformAnimator;
import bdv.viewer.animate.SimilarityTransformAnimator;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.source.PlateauSphericalMaskRealRandomAccessible;
import bigwarp.transforms.BigWarpTransform;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible; 
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.iterator.RealIntervalIterator;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.ConstantUtils;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.Views;


public class SimilarityTransformInterpolationExample {

	public static void main(String[] args) {
		show( args[0] );
//		exp();
	}

	public static void exp() {

		double[] zero = new double[]{0, 0, 0};
		double[] center = new double[]{252, 76, 70};
		double[] centerOfRotation = new double[] { 252, 150, 70 };
		AffineTransform3D offset = new AffineTransform3D();
		offset.translate(center);

		AffineTransform3D dy20 = new AffineTransform3D();
		dy20.translate(new double[]{0, 300, 0});

		AffineTransform3D transform = new AffineTransform3D();
		transform.rotate(2, Math.PI);
		transform.preConcatenate(offset);
		transform.concatenate(offset.inverse());
		System.out.println(transform);
		System.out.println( Affine3DHelpers.toString(transform));
		
		SimilarityTransformInterpolator interpolatorOtherC = new SimilarityTransformInterpolator( transform, centerOfRotation ); 
		AffineTransform3D z = interpolatorOtherC.get(0);
		AffineTransform3D o = interpolatorOtherC.get(1);
		System.out.println(  "" );
		System.out.println( Affine3DHelpers.toString( z ));
		System.out.println(  "" );
		System.out.println( Affine3DHelpers.toString( o ));
		
	}
	
	public static void show( String imgFile ) {

		ImagePlus imp = IJ.openImage( imgFile );
		Img<UnsignedByteType> imgBase = ImageJFunctions.wrapByte(imp);

		Img<UnsignedByteType> img = imgBase;
		// RandomAccessibleInterval<UnsignedByteType> img = Views.translateInverse( imgBase, 252, 76, 70 );

		AffineTransform3D identity = new AffineTransform3D();
		FinalInterval bigItvl = Intervals.createMinMax(-1000, -1000, -1000, 1000, 1000, 1000);

		double[] zero = new double[]{0, 0, 0};
		double[] center = new double[]{252, 76, 70};
		double[] centerOfRotation = new double[] { 252, 150, 70 };
		AffineTransform3D offset = new AffineTransform3D();
		offset.translate(center);

		AffineTransform3D dy20 = new AffineTransform3D();
		dy20.translate(new double[]{0, 300, 0});

		AffineTransform3D transform = new AffineTransform3D();
		transform.rotate(2, Math.PI);
		transform.preConcatenate(offset);
		transform.concatenate(offset.inverse());
		System.out.println(transform);
		
//      AffineTransform3D transform = new AffineTransform3D();
//      transform.rotate( 2, Math.PI );
//      transform.preConcatenate( dy20 );
//      System.out.println( transform );

		SimilarityTransformAnimator interpolator = new SimilarityTransformAnimator( new AffineTransform3D(), transform, 0, 0, 1); 
		SimilarityTransformInterpolator interpolatorC = new SimilarityTransformInterpolator( transform, center ); 
		SimilarityTransformInterpolator interpolatorOtherC = new SimilarityTransformInterpolator( transform, centerOfRotation ); 
		
		BdvOptions opts = BdvOptions.options();
		BdvStackSource<UnsignedByteType> bdv = makeTimeStack( img, bigItvl, interpolator, "orig", opts );
		opts = opts.addTo(bdv);
		makeTimeStack( img, bigItvl, interpolatorC, "center", opts );
		makeTimeStack( img, bigItvl, interpolatorOtherC, "other C", opts );

	}
	
	public static BdvStackSource<UnsignedByteType> makeTimeStack( RandomAccessibleInterval<UnsignedByteType> img, Interval interval, AbstractTransformAnimator interpolator, String name, BdvOptions opts )
	{

		double del = 0.01;
		List<RandomAccessibleInterval<UnsignedByteType>> stack = new ArrayList<>();
		for (double t = 0.0; t < (1.0 + del); t += del)
		{
			AffineRandomAccessible<UnsignedByteType, AffineGet> rimg = RealViews.affine(
					Views.interpolate(Views.extendZero(img), new NearestNeighborInterpolatorFactory<>()),
					interpolator.get(t));

			stack.add(Views.interval(Views.raster(rimg), interval));
		}

		RandomAccessibleInterval<UnsignedByteType> stackImg = Views.stack(stack);
		return BdvFunctions.show(stackImg, name, opts );
	}

}
