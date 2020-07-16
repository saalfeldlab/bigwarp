package bigwarp;

import ij.IJ;
import ij.ImagePlus;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.InvertibleRealTransform;

import java.io.File;

public class TransformPoints2DTest {

    public static void main(String... args) throws Exception {

        ImagePlus impBlobs = IJ.openImage("https://imagej.nih.gov/ij/images/blobs.gif");

        BigWarp.BigWarpData bwData = BigWarpInit.createBigWarpDataFromImages( impBlobs, impBlobs );

        BigWarp bigWarp = new BigWarp(bwData, "2D points transform", null);
//        bigWarp.getLandmarkPanel().getTableModel().load( new File( "src/test/resources/landmarks2d-blobs.csv" ));
        bigWarp.loadLandmarks( "src/test/resources/landmarks2d-blobs.csv" );

        bigWarp.toggleMovingImageDisplay();
        
        // Making a realpoint, and transform it

        double xTest = 100;
        double yTest = 100;

        RealPoint pt2D = new RealPoint(2);
        pt2D.setPosition(xTest,0);
        pt2D.setPosition(yTest, 1);

        double[] ptCoords2D = new double[2];
        ptCoords2D[0]=pt2D.getDoublePosition(0);
        ptCoords2D[1]=pt2D.getDoublePosition(1);

        // ------------------- Deprecated transform computation
        // -------- FWD
        ThinPlateR2LogRSplineKernelTransform ltmTransform = bigWarp.getTransform();
        double[] ptTransformDeprecatedFwd = ltmTransform.apply(ptCoords2D);
        System.out.println("Fwd Deprecated \t x:"+ptTransformDeprecatedFwd[0]+" \t y:"+ptTransformDeprecatedFwd[1]);



        // -------- BWD
        double[] ptTransformDeprecatedBwd = bigWarp.getTransform().inverse(ptCoords2D,0.001);
        System.out.println("Bwd Deprecated \t x:"+ptTransformDeprecatedBwd[0]+" \t y:"+ptTransformDeprecatedBwd[1]);

        // ------------------ New API :
        // -------- FWD
        // 2D point input : doesn't work... : java.lang.ArrayIndexOutOfBoundsException: 2
        /*RealPoint fwdTransformedPt2D = new RealPoint(2);
        bigWarp.getTransformation()
                .apply(pt2D, fwdTransformedPt2D); // uncomment to see the exception
        System.out.println("Fwd 2D \t x:"+fwdTransformedPt2D.getDoublePosition(0)+" \t y:"+fwdTransformedPt2D.getDoublePosition(1));
         */

        // But maybe the inputs need to be 3D... ?... let's try 3D input with Z = 0
        RealPoint pt3D = new RealPoint(3);
        pt3D.setPosition(xTest, 0);
        pt3D.setPosition(yTest, 1);
        pt3D.setPosition(0, 2);

        InvertibleRealTransform bwTransform = bigWarp.getTransformation();
        RealPoint fwdTransformedPt3D = new RealPoint(3);
        bwTransform.apply(pt3D, fwdTransformedPt3D);

        System.out.println("Fwd 3D \t x:"+fwdTransformedPt3D.getDoublePosition(0)+" \t y:"+fwdTransformedPt3D.getDoublePosition(1));
        // Why are the numbers so different from the result we get in the deprecated computation ? Who's right who's wrong

        // -------- BWD
        // 2D point input : doesn't work... : java.lang.ArrayIndexOutOfBoundsException: 2
        /*RealPoint bwdTransformedPt2D = new RealPoint(2);
        bigWarp.getTransformation()
                .inverse()
                .apply(pt2D, bwdTransformedPt2D); // uncomment to see the exception
        System.out.println("Bwd 2D \t x:"+bwdTransformedPt2D.getDoublePosition(0)+" \t y:"+bwdTransformedPt2D.getDoublePosition(1));
         */

        // But maybe the inputs need to be 3D... ?... let's try 3D input with Z = 0
        RealPoint bwdTransformedPt3D = new RealPoint(3);
        bigWarp.getTransformation()
                .inverse()
                .apply(pt3D, bwdTransformedPt3D);

        // Nope : inverse do not work in 3D either : Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException

        System.out.println("Fwd \t x:"+bwdTransformedPt3D.getDoublePosition(0)+" \t y:"+bwdTransformedPt3D.getDoublePosition(1));
        // Why are the numbers so different from the result we get in the deprecated computation ? Who's right who's wrong

        // Using 3d points *should* have worked, this is a bug in a dependency.
        // I'll fix and PR
 
        // This works
		RealPoint bwdTransformedPt2D = new RealPoint( 2 );
		bigWarp.unwrap2d( bigWarp.getTransformation() )
				.inverse()
				.apply(pt2D, bwdTransformedPt2D); // uncomment to see the exception
		System.out.println("Bwd unwrapped 2D \t x:"+bwdTransformedPt2D.getDoublePosition(0)+" \t y:"+bwdTransformedPt2D.getDoublePosition(1));

    }
}
