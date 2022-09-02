package net.imglib2.realtransform;

import java.io.IOException;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import bdv.util.Affine3DHelpers;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.animate.AbstractTransformAnimator;
import bdv.viewer.animate.SimilarityModel3D;
import bdv.viewer.animate.SimilarityTransformAnimator;
import bigwarp.BigwarpSettings;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.source.PlateauSphericalMaskRealRandomAccessible;
import bigwarp.source.PlateauSphericalMaskSource;
import bigwarp.transforms.BigWarpTransform;
import bigwarp.transforms.ModelTransformSolver;
import bigwarp.transforms.TpsTransformSolver;
import bigwarp.transforms.WrappedCoordinateTransform;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.models.AbstractAffineModel3D;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.MaskedSimilarityTransform.Interpolators;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;


public class SimilarityTransformInterpolationExample {

	public static void main(String[] args) {
//		show( args[0] );
//		exp();
//		showSeq( args[0], args[1] );
		expSeq( args[0], args[1] );
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

	public static void expSeq( String imgFile, String jsonFile ) {
		ImagePlus imp = IJ.openImage( imgFile );
		Img<UnsignedByteType> imgBase = ImageJFunctions.wrapByte(imp);
		Scale3D toPhysical = new Scale3D(
				imp.getCalibration().pixelWidth,
				imp.getCalibration().pixelHeight,
				imp.getCalibration().pixelDepth);

		Img<UnsignedByteType> img = imgBase;
		// RandomAccessibleInterval<UnsignedByteType> img = Views.translateInverse( imgBase, 252, 76, 70 );

		AffineTransform3D identity = new AffineTransform3D();
		//TODO Caleb: John, what interval should we be using here?
		FinalInterval bigItvl = Intervals.createMinMax(0, -200, 0, 1000, 1000, 150);
		
		final Path path = Paths.get( jsonFile );
		final OpenOption[] options = new OpenOption[]{StandardOpenOption.READ};
		Reader reader;
		JsonObject json = null;
		try
		{
			reader = Channels.newReader(FileChannel.open(path, options), StandardCharsets.UTF_8.name());
			json = BigwarpSettings.gson.fromJson( reader, JsonObject.class );
		} catch ( IOException e )
		{
			e.printStackTrace();
			return;
		}
		
		LandmarkTableModel ltm = new LandmarkTableModel( 3 );
		if( json.has( "landmarks" ))
			ltm.fromJson( json );



		final JsonObject maskJson = json.getAsJsonObject( "mask" );
		PlateauSphericalMaskRealRandomAccessible mask = BigwarpSettings.gson.fromJson( maskJson, PlateauSphericalMaskRealRandomAccessible.class);


		PlateauSphericalMaskSource tpsMask = PlateauSphericalMaskSource.build( mask, bigItvl );
		PlateauSphericalMaskRealRandomAccessible lambda = tpsMask.getRandomAccessible();
		
		// build transformations
		final double[][] mvgPts;
		final double[][] tgtPts;

		final int numActive = ltm.numActive();
		mvgPts = new double[ 3 ][ numActive ];
		tgtPts = new double[ 3 ][ numActive ];
		ltm.copyLandmarks( mvgPts, tgtPts ); // synchronized

		WrappedCoordinateTransform sim = new ModelTransformSolver( new SimilarityModel3D() ).solve(mvgPts, tgtPts);
		final AffineTransform3D transform = new AffineTransform3D();
		BigWarpTransform.affine3d( (AbstractAffineModel3D)sim.getTransform(), transform );

		final double[] center = new double[3];
		lambda.getCenter().localize( center );
		System.out.println( "center: " + Arrays.toString( center ));

		// masked similarity
		final MaskedSimilarityTransform msim = new MaskedSimilarityTransform( transform, lambda, center );
		final MaskedSimilarityTransform msimInv = new MaskedSimilarityTransform( transform.inverse(), lambda, center, Interpolators.SIMILARITY, true );
		final WrappedIterativeInvertibleRealTransform<?> tpsXfm = new TpsTransformSolver().solve( ltm );

		final Scale3D id = new Scale3D(1,1,1);
		final SpatiallyInterpolatedRealTransform tpsLmsim = new SpatiallyInterpolatedRealTransform( tpsXfm, transform.inverse(), lambda );
		final SpatiallyInterpolatedRealTransform idLmsimI = new SpatiallyInterpolatedRealTransform( id, transform, lambda );
		final SpatiallyInterpolatedRealTransform msimILid = new SpatiallyInterpolatedRealTransform( id, transform, lambda );
		
		final double[] x = new double[]{358.1, 171.0, 85.7 };

		double lam = lambda.getAt( x ).getRealDouble();
		System.out.println( "    x: " + Arrays.toString( x ));
		System.out.println( "    l: " + lam );

		
		final double[] tx = new double[3];
		final double[] sx = new double[3];
		final double[] msx = new double[3];
		
		tpsXfm.apply( x, tx );
		transform.inverse().apply( x, sx );
		msim.apply( x, msx );

		System.out.println( "  t(x): " + Arrays.toString( tx ));
		System.out.println( "  s(x): " + Arrays.toString( sx ));
		System.out.println( " ms(x): " + Arrays.toString( msx ));

		double ltx =  lambda.getAt( tx ).getRealDouble();
		double lsx =  lambda.getAt( sx ).getRealDouble();
		double lmsx =  lambda.getAt( msx ).getRealDouble();
		System.out.println( "  lam(t(x))  : " + ltx );
		System.out.println( "  lam(s(x))  : " + lsx );
		System.out.println( "  lam( ms(x)): " + lmsx );
		System.out.println( "  " );

		final double[] msxMinv = new double[3];
		msimInv.apply( msx, msxMinv );
		System.out.println( " minv(ms(x)): " + Arrays.toString( msxMinv ));
		
		/**
		 * Conclusion:
		 * msimInv ( msim ( x )) is almost the identity
		 * good
		 */

		
		double[] tlmsx  = new double[3];
		tpsLmsim.apply( x, tlmsx );
		
		System.out.println( " " );
		System.out.println( " tlms(x): " + Arrays.toString( tlmsx ));
		
		double[] msi_tlmsx  = new double[3];
		msimInv.apply( tlmsx, msi_tlmsx );
		System.out.println( " msi(tlms(x)): " + Arrays.toString( msi_tlmsx ));
		
		/*
		 * CONCLUSION
		 * msi_tlmsx is approximately where we want it to be
		 * 
		 */

		final RealTransformSequence xfm = new RealTransformSequence();
		xfm.add( tpsLmsim );
		xfm.add( msimInv );
		
		double[] y = new double[3];
		xfm.apply( x, y );
		System.out.println( " f(x): " + Arrays.toString( y ));
	}
	
	public static void showSeq( String imgFile, String jsonFile ) {
		ImagePlus imp = IJ.openImage( imgFile );
		Img<UnsignedByteType> imgBase = ImageJFunctions.wrapByte(imp);
		Scale3D toPhysical = new Scale3D(
				imp.getCalibration().pixelWidth,
				imp.getCalibration().pixelHeight,
				imp.getCalibration().pixelDepth);

		Img<UnsignedByteType> img = imgBase;
		// RandomAccessibleInterval<UnsignedByteType> img = Views.translateInverse( imgBase, 252, 76, 70 );

		AffineTransform3D identity = new AffineTransform3D();
		FinalInterval bigItvl = Intervals.createMinMax(0, -200, 0, 1000, 1000, 150);
		
		Gson gson = new Gson();
		final Path path = Paths.get( jsonFile );
		final OpenOption[] options = new OpenOption[]{StandardOpenOption.READ};
		Reader reader;
		JsonObject json = null;
		try
		{
			reader = Channels.newReader(FileChannel.open(path, options), StandardCharsets.UTF_8.name());
			json = gson.fromJson( reader, JsonObject.class );
		} catch ( IOException e )
		{
			e.printStackTrace();
			return;
		}
		
		LandmarkTableModel ltm = new LandmarkTableModel( 3 );
		if( json.has( "landmarks" ))
			ltm.fromJson( json );


		final JsonObject mask = json.getAsJsonObject( "mask" );
		PlateauSphericalMaskRealRandomAccessible maskRA = BigwarpSettings.gson.fromJson( mask, PlateauSphericalMaskRealRandomAccessible.class);


		PlateauSphericalMaskSource tpsMask = PlateauSphericalMaskSource.build( maskRA, bigItvl );
		PlateauSphericalMaskRealRandomAccessible lambda = tpsMask.getRandomAccessible();
		
		
		// build transformations
		final double[][] mvgPts;
		final double[][] tgtPts;

		final int numActive = ltm.numActive();
		mvgPts = new double[ 3 ][ numActive ];
		tgtPts = new double[ 3 ][ numActive ];
		ltm.copyLandmarks( mvgPts, tgtPts ); // synchronized

		WrappedCoordinateTransform sim = new ModelTransformSolver( new SimilarityModel3D() ).solve(mvgPts, tgtPts);
		final AffineTransform3D transform = new AffineTransform3D();
		BigWarpTransform.affine3d( (AbstractAffineModel3D)sim.getTransform(), transform );

		final double[] center = new double[3];
		lambda.getCenter().localize( center );
		System.out.println( "center: " + Arrays.toString( center ));

		// masked similarity
//		final MaskedSimilarityTransform xfm = new MaskedSimilarityTransform( transform, lambda, center );
		final WrappedIterativeInvertibleRealTransform<?> tpsXfm = new TpsTransformSolver().solve( ltm );

		final Scale3D id = new Scale3D(1,1,1);
		final SpatiallyInterpolatedRealTransform first = new SpatiallyInterpolatedRealTransform( tpsXfm, transform.inverse(), lambda );
		final SpatiallyInterpolatedRealTransform second = new SpatiallyInterpolatedRealTransform( id, transform, lambda );
		
		

//		final RealTransformSequence xfm = new RealTransformSequence();
//		xfm.add( first );
//		xfm.add( second );

		final InterpolatedTimeVaryingTransformation sim2tps = new InterpolatedTimeVaryingTransformation( tpsXfm, transform );
//		final InterpolatedTimeVaryingTransformation id2simi = new InterpolatedTimeVaryingTransformation( id, transform.inverse() );
		SimilarityTransformInterpolator id2simi = new SimilarityTransformInterpolator( transform, center );

//		SimilarityTransformAnimator intebcwrpolator = new SimilarityTransformAnimator( new AffineTransform3D(), transform, 0, 0, 1); 
//		SimilarityTransformInterpolator interpolatorC = new SimilarityTransformInterpolator( transform, center ); 
//		SimilarityTransformInterpolator interpolatorOtherC = new SimilarityTransformInterpolator( transform, centerOfRotation ); 


//		NearestNeighborInterpolatorFactory pixInterp = new NearestNeighborInterpolatorFactory();
		NLinearInterpolatorFactory pixInterp = new NLinearInterpolatorFactory();
		RealRandomAccessible< UnsignedByteType > rimg = RealViews.affine(
				Views.interpolate(Views.extendZero(img), pixInterp),
				toPhysical);

		BdvOptions opts = BdvOptions.options();
		BdvStackSource<UnsignedByteType> bdv = makeTimeStack( rimg, bigItvl, sim2tps, "sim 2 tps", opts );
		opts = opts.addTo(bdv);
		makeTimeStack( rimg, bigItvl, id2simi, "id 2 simi", opts );
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

		RealRandomAccessible< UnsignedByteType > rimg = Views.interpolate(Views.extendZero(img), new NearestNeighborInterpolatorFactory<>());
		
		BdvOptions opts = BdvOptions.options();
		BdvStackSource<UnsignedByteType> bdv = makeTimeStack( rimg, bigItvl, interpolator, "orig", opts );
		opts = opts.addTo(bdv);
		makeTimeStack( rimg, bigItvl, interpolatorC, "center", opts );
		makeTimeStack( rimg, bigItvl, interpolatorOtherC, "other C", opts );

	}
	
	public static BdvStackSource<UnsignedByteType> makeTimeStack( RealRandomAccessible<UnsignedByteType> img, Interval interval, AbstractTransformAnimator interpolator, String name, BdvOptions opts )
	{
		return makeTimeStack( img, interval, new AnimatorTimeVaryingTransformation( interpolator ), name, opts );
	}

	public static BdvStackSource<UnsignedByteType> makeTimeStack( RealRandomAccessible<UnsignedByteType> rimg, Interval interval, TimeVaryingTransformation interpolator, String name, BdvOptions opts )
	{

		double del = 0.01;
		List<RandomAccessibleInterval<UnsignedByteType>> stack = new ArrayList<>();
		for (double t = 0.0; t < (1.0 + del); t += del)
		{
			RealRandomAccessible<UnsignedByteType> xfmimg = new RealTransformRandomAccessible< >( 
					rimg, interpolator.get(t));

			stack.add(Views.interval(Views.raster(xfmimg), interval));
		}

		RandomAccessibleInterval<UnsignedByteType> stackImg = Views.stack(stack);
		return BdvFunctions.show(stackImg, name, opts );
	}

}
