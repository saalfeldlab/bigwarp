package bigwarp;

import java.util.Arrays;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.Common;

import net.imglib2.realtransform.RealTransform;

public class NgffTransformTests {

	public static void main( final String[] args ) throws Exception
	{
//		final String url = "/home/john/projects/bigwarp/projects/jrc18-rot.zarr?/#coordinateTransformations[1]";
//		final Pair<CoordinateTransform<?>, N5Reader> ctN5 = Common.openTransformN5(url);
//		System.out.println( ctN5.getA() );
//
//		final RealTransform tf = ctN5.getA().getTransform(ctN5.getB());
//		System.out.println( tf );

//		final String dfUrl = "/home/john/projects/bigwarp/projects/jrc18-rot.zarr?/#coordinateTransformations[0]";
//		final Pair<CoordinateTransform<?>, N5Reader> ctN5 = Common.openTransformN5(dfUrl);
//		System.out.println( ctN5.getA() );
//
//		final RealTransform df1 = ctN5.getA().getTransform(ctN5.getB());
//		System.out.println( df1 );

		final String tuneUrl = "/home/john/projects/bigwarp/projects/jrc18-rot.zarr?/#coordinateTransformations[2]";
		final RealTransform tune = Common.open(tuneUrl);

		final double[] p = new double[] { 308, 122,  91 };
//		final double[] p = new double[] { 292, 116,  92 };
		final double[] q1 = new double[] {   0,   0,   0};
		final double[] q2 = new double[] {   0,   0,   0};


//		df1.apply(p, q1);
//		System.out.println( Arrays.toString(p) );
//		System.out.println( Arrays.toString(q1) );
//
		tune.apply(p, q2);
		System.out.println( " " );
		System.out.println( Arrays.toString(p) );
		System.out.println( Arrays.toString(q2) );


//		final String loc = "/home/john/projects/bigwarp/projects/jrc18-rot.zarr";
//		final N5Reader n5 = new N5Factory().gsonBuilder( gsonBuilder() ).openReader( loc );
//		final String dataset = "";
//		final String attribute = "coordinateTransformations[1]";
//
//		final CoordinateTransform ct = n5.getAttribute(dataset, attribute, CoordinateTransform.class);
//		System.out.println( ct );



//		final String urlRef = "/home/john/projects/bigwarp/projects/jrc18.zarr?/#coordinateTransformations[5]";
//		final RealTransform tform = open( urlRef );
//		System.out.println( tform );


//		final String lmPath = "/home/john/Desktop/bw-rot-landmarks.csv";
//		final LandmarkTableModel ltm = new LandmarkTableModel(3);
//		ltm.load(new File( lmPath ));
//
//		final BigWarpTransform bwTform = new BigWarpTransform(ltm);
//
//		final InvertibleRealTransform tform = bwTform.getTransformation();
//
//		// tform is deformation then affine
//
//		final InvertibleRealTransform tf = BigWarpToDeformationFieldPlugIn.getTransformation(null, bwTform, false, true);
//		System.out.println( tf );
//
//		final AffineGet affineTps = bwTform.affinePartOfTps();
//		System.out.println( affineTps );
//
//
//		final double[] p = new double[] { 150, 200,  75 };
//		final double[] q1 = new double[] {   0,   0,   0};
//		final double[] q2 = new double[] {   0,   0,   0};


		// this part behaves as expected
//		tform.apply(p, q1);
//		System.out.println( " " );
//		System.out.println( "true: " + Arrays.toString(q1));
//
//
//		final RealTransformSequence seq = new RealTransformSequence();
//		seq.add( tf );
//		seq.add( affineTps );
//		seq.apply(p, q2);
//
//		System.out.println( " " );
//		System.out.println( "othr: " + Arrays.toString(q2));


//		final Pair<CoordinateTransform<?>, N5Reader> ctN5 = NgffTransformations.openTransformN5("/home/john/projects/bigwarp/projects/jrc18-rot.zarr?/#coordinateTransformations[4]");
//		final RealTransform df = ctN5.getA().getTransform(ctN5.getB());
//
//
//		System.out.println( " " );
//		System.out.println( " " );
//		System.out.println( ctN5.getA().getName() );
//
//		tf.apply(p, q1);
//		System.out.println( " " );
//		System.out.println( "true: " + Arrays.toString(q1));
//
//		df.apply(p, q2);
//		System.out.println( " " );
//		System.out.println( "df  : " + Arrays.toString(q2));


////		final String jrc18DfTgt = "/home/john/projects/bigwarp/projects/bw_jrc18Down_tforms.n5?#coordinateTransformations[6]";
//		final String jrc18DfTgt = "/home/john/projects/bigwarp/projects/bw_jrc18Down_tforms.n5?#coordinateTransformations[0]";
////		final RealTransform tform = open(jrc18DfTgt);
//
//		final Pair<CoordinateTransform<?>, N5Reader> ctN5 = openTransformN5(jrc18DfTgt);
//		final CoordinateTransform<?> ct = ctN5.getA();
//		final N5Reader n5 = ctN5.getB();
//
//		System.out.println("ct: " + ct + " " + ct.getType());
//
//		final RealTransform tform = ct.getTransform( n5 );


//		final String p2pUri = "/home/john/Desktop/tforms.n5?/dfield2d#coordinateTransformations[1]";
//		final Pair< CoordinateTransform< ? >, N5Reader > pair = openTransformN5( p2pUri );
//		final CoordinateTransform<?> ct = pair.getA();
//		System.out.println( ct );
//		System.out.println( ct.getType() );
//
//		final N5Reader n5 = pair.getB();
////		final RealTransform tform = ct.getTransform(n5);
////		System.out.println( tform );
//
//
//		final SequenceCoordinateTransform sct = (SequenceCoordinateTransform)ct;
//		final AffineGet affine = sct.asAffine(3);
//		System.out.println( affine.getRowPackedCopy());

//		final String dfUri = "/home/john/Desktop/tforms.n5?/dfield2d#coordinateTransformations[0]";
//		final Pair< CoordinateTransform< ? >, N5Reader > pair = openTransformN5( dfUri );
//		final CoordinateTransform<?> ct = pair.getA();
//		System.out.println( ct );
//
//		final N5Reader n5 = pair.getB();
//		final RealTransform tform = ct.getTransform(n5);
//		System.out.println( tform );


//		final AffineTransform affine = new AffineTransform( 2 );
//		final Translation2D t = new Translation2D( new double[] { 5, 6 } );
//		System.out.println( Arrays.toString(affine.getRowPackedCopy() ));
//		affine.preConcatenate(t);
//		System.out.println( Arrays.toString(affine.getRowPackedCopy() ));



//		// detect transformations
//		final String loc = "/home/john/Desktop/tforms.n5";
//		final N5URI uri = new N5URI(loc);

//		final N5Reader n5 = new N5Factory().gsonBuilder( gsonBuilder() ).openReader( loc );
////		final RealTransform tform = findFieldTransformFirst( n5, "/dfield2d" );
//		final RealTransform tform = findFieldTransformStrict( n5, "/dfield2d", "boats.tif_dfield" );


//		// detect transformations
//		final String loc = "/home/john/Desktop/tforms.n5";
//		final N5URI uri = new N5URI(loc);
//
//		// dfield 2d path
//		final String dfUri = "/home/john/Desktop/tforms.n5?/dfield2d#coordinateTransformations[0]";
////		final String dfUri = "/home/john/Desktop/tforms.n5?/#coordinateTransformations[2]";
////		final String dfPartUri = "/home/john/Desktop/tforms.n5";
//
////		final RealTransform tform = open(dfUri);
////		System.out.println(tform);
//
//		final Pair< CoordinateTransform< ? >, N5Reader > pair = openTransformN5( dfUri );
//		final CoordinateTransform<?> ct = pair.getA();
//		System.out.println( ct );
//
//		final RealTransform tform = ct.getTransform( pair.getB());
//		System.out.println( tform );


//		final String s = detectTransforms(dfUri);
//		System.out.println( "full uri: " + s );
//		System.out.println("inferred full uri: " + detectTransforms(dfPartUri));


//		final CoordinateTransform<?>[] cts = detectTransforms(loc);
//		System.out.println(Arrays.toString(cts));

//		System.out.println(detectTransforms(loc));

//		System.out.println( uri );
//		System.out.println( uri.getURI() );
//
//		final String grp = ( uri.getGroupPath() != null ) ? uri.getGroupPath() : "";
//		System.out.println( grp );
//
//		final String attr = ( uri.getAttributePath() != null ) ? uri.getAttributePath() : "";
//		System.out.println( attr );

//		final N5Reader n5 = new N5Factory().gsonBuilder( gsonBuilder() ).openReader( uri.getContainerPath() );
//		final JsonObject json = n5.getAttribute(grp, attr, JsonObject.class);
//		final String ver = n5.getAttribute(grp, "n5", String.class);
//		final JsonElement jcts = n5.getAttribute(grp, "coordinateTransformations", JsonElement.class);
//		final JsonElement jct = n5.getAttribute(grp, "coordinateTransformations[0]", JsonElement.class);
//		final CoordinateTransform<?> ct = n5.getAttribute(grp, "coordinateTransformations[0]", CoordinateTransform.class);
//		final CoordinateTransform<?>[] cts = n5.getAttribute(grp, "coordinateTransformations", CoordinateTransform[].class);

//		System.out.println("");
//		System.out.println(json);
//		System.out.println("");
//		System.out.println(ver);
//		System.out.println("");
//		System.out.println(jcts);
//		System.out.println("");
//		System.out.println(jct);
//		System.out.println("");
//		System.out.println(ct);
//		System.out.println(ct.getType());
//		System.out.println("");
//		System.out.println(Arrays.toString(cts));


//		openTransformN5( url );


		// full
//		final String bijPath = "/home/john/projects/ngff/dfieldTest/jrc18_example.n5?/#/coordinateTransformations[0]";

		// no dataset
//		final String bijPath = "/home/john/projects/ngff/dfieldTest/jrc18_example.n5#/coordinateTransformations[0]";

		// no dataset no attribute
//		final String bijPath = "/home/john/projects/ngff/dfieldTest/jrc18_example.n5";
//
//		final N5URI url = new N5URI( bijPath );
//		System.out.println( url.getGroupPath());
//		System.out.println( url.getAttributePath());
//
//		final Pair< NgffCoordinateTransformation< ? >, N5Reader > bijN5 = openTransformN5( bijPath );
//		System.out.println( bijN5.getA() );
//		final InvertibleRealTransform bij = openInvertible( bijPath );
//		System.out.println( bij );


//		final String path = "/home/john/projects/ngff/dfieldTest/jrc18_example.n5";
//		final N5URL url = new N5URL( path );
//		System.out.println( url.getAttribute() );
//		System.out.println( url.getDataset());


//		final String bijPath = "/home/john/projects/ngff/dfieldTest/jrc18_example.n5?/#/coordinateTransformations[0]";
//		final N5URL url = new N5URL( bijPath );
//		final N5Reader n5 = new N5Factory().gsonBuilder( gsonBuilder() ).openReader( url.getLocation() );
//		final CoordinateTransformation ct = n5.getAttribute( url.getDataset(), url.getAttribute(), CoordinateTransformation.class );
//		System.out.println( ct );
//
//		final NgffCoordinateTransformation< ? > nct = NgffCoordinateTransformation.create( ct );
//		RealTransform tform = nct.getTransform( n5 );
//		System.out.println( tform );




//		final String basePath = "/home/john/projects/ngff/dfieldTest/jrc18_example.n5";
//		final String csPath = "?dfield#coordinateSystems/[0]";
//		final String namePath = "?dfield#coordinateSystems/[0]/name";
//		final String dimPath = "?dfield#/dimensions";
//
//		final N5URL baseUrl = new N5URL( basePath );
//		final N5URL nameUrl = baseUrl.getRelative( namePath );
//		final N5URL csUrl = baseUrl.getRelative( csPath );
//		final N5URL dimUrl = baseUrl.getRelative( dimPath );
//
//		final N5Reader n5 = new N5Factory().gsonBuilder( gsonBuilder() ).openReader( baseUrl.getLocation() );
//		final String name = n5.getAttribute( nameUrl.getDataset(), nameUrl.getAttribute(), String.class );
//		final CoordinateSystem cs = n5.getAttribute( csUrl.getDataset(), csUrl.getAttribute(), CoordinateSystem.class );
//		final long[] dims = n5.getAttribute( dimUrl.getDataset(), dimUrl.getAttribute(), long[].class );
//
//		System.out.println( name );
//		System.out.println( cs );
//		System.out.println( cs.getAxes()[0].getName() );
//		System.out.println( Arrays.toString( dims ) );




////		final String path = "/home/john/projects/ngff/dfieldTest/dfield.n5";
//		final String path = "/home/john/projects/ngff/dfieldTest/jrc18_example.n5";
//
//		final String dataset = "/";
////		final String dataset = "coordinateTransformations";
////		final String dataset = "/dfield";
//
//		final N5FSReader n5 = new N5FSReader( path, gsonBuilder() );
//
////		RealTransform dfieldTform = open( n5, dataset );
////		System.out.println( dfieldTform );
//
////		RealTransform dfieldTform = open( n5, dataset );
////		System.out.println( dfieldTform );
//
//		TransformGraph g = openGraph( n5, dataset );
//		g.printSummary();
//		RealTransform fwdXfm = g.path( "jrc18F", "fcwb" ).get().totalTransform( n5, g );
//		RealTransform invXfm = g.path( "fcwb", "jrc18F" ).get().totalTransform( n5, g );
//		System.out.println( fwdXfm );
//		System.out.println( invXfm );


//		ArrayImg< IntType, IntArray > img = ArrayImgs.ints( 2, 3, 4, 5 );
//
//		int[] p = vectorAxisLastNgff( n5, dataset );
//		System.out.println( Arrays.toString( p ));
//		System.out.println( "" );
//
//		IntervalView< IntType > imgP = N5DisplacementField.permute( img, p );
//		System.out.println( Intervals.toString( imgP ));


//		try
//		{
////			AffineGet p2p = N5DisplacementField.openPixelToPhysicalNgff( n5, "transform", true );
////			System.out.println( p2p );
//
////			int[] indexes = new int[] {1, 2, 3 };
////			AffineGet sp2p = TransformUtils.subAffine( p2p, indexes );
////			System.out.println( sp2p );
//		}
//		catch ( Exception e )
//		{
//			e.printStackTrace();
//		}

	}

}
