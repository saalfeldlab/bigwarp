package bigwarp.source;

import java.util.List;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;

import bdv.viewer.overlay.BigWarpMaskSphereOverlay;
import org.jdom2.Element;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import mpicbg.spim.data.XmlHelpers;
import net.imglib2.*;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;
import org.jdom2.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class PlateauSphericalMaskRealRandomAccessible implements RealRandomAccessible< DoubleType >
{
	transient private BiConsumer< RealLocalizable, DoubleType > pfun;
	transient private FunctionRealRandomAccessible< DoubleType > rra;
	transient private List<BigWarpMaskSphereOverlay> overlays;

	private FalloffShape fallOffShape;

	transient private int nd;

	transient private double plateauR;
	@SerializedName(SQUARED_RADIUS)
	private double plateauR2;

	transient private double sigma;

	@SerializedName( SQUARED_SIGMA )
	private double sqrSigma;
	transient private double invSqrSigma;
	transient private double gaussInvSqrSigma;

	@JsonAdapter( RealPointToDoubleArray.class )
	private RealPoint center;

	private static final double EPS = 1e-6;
	private static final double PIon2 = Math.PI / 2.0;
	private static final double PI = Math.PI;

	public static enum FalloffShape
	{
		COSINE( it -> new CosineFalloff( it ) ),
		GAUSSIAN( it -> new GaussianFalloff( it ) ),
		LINEAR( it -> new LinearFalloff( it ) );

		private final Function< PlateauSphericalMaskRealRandomAccessible, BiConsumer< RealLocalizable, DoubleType > > fallOffProvider;

		FalloffShape( Function< PlateauSphericalMaskRealRandomAccessible, BiConsumer< RealLocalizable, DoubleType > > fallOffProvider )
		{
			this.fallOffProvider = fallOffProvider;
		}

		public BiConsumer< RealLocalizable, DoubleType > createFalloffFunction( PlateauSphericalMaskRealRandomAccessible mask )
		{
			return fallOffProvider.apply( mask );
		}

	}

	public PlateauSphericalMaskRealRandomAccessible( RealPoint center )
	{
		this.nd = center.numDimensions();
		this.center = center;
		fallOffShape = FalloffShape.COSINE;
		pfun = fallOffShape.createFalloffFunction( this );
		update();

		setRadius( 8.0 );
		setSigma ( 10.0 );
	}

	public void setOverlays( final List< BigWarpMaskSphereOverlay > overlays ) {
		this.overlays = overlays;
		if (overlays != null) {
			overlays.stream().forEach(o -> {
				o.setCenter(center);
				o.setInnerRadius(plateauR);
				o.setOuterRadiusDelta(sigma);
			});
		}
	}

	public static void main( String[] args )
	{
		long S = 50;
		double[] center = new double[] { S, S, S };
		RealPoint pt = RealPoint.wrap( center );

		PlateauSphericalMaskRealRandomAccessible img = new PlateauSphericalMaskRealRandomAccessible( pt );
		img.setRadius( 10 );
		img.setSigma( 10 );
		Interval interval = Intervals.createMinSize( 0, 0, 0, 2 * S, 2 * S, 2 * S );
//
////		BdvOptions options = BdvOptions.options().screenScales( new double[] { 1 } );
//		BdvOptions options = BdvOptions.options();
//		BdvStackSource< DoubleType > bdv = BdvFunctions.show( img.rra, interval, "img", options );
//		bdv.getBdvHandle().getSetupAssignments().getMinMaxGroups().get( 0 ).setRange( 0, 1 );
//
////		InputActionBindings kb = bdv.getBdvHandle().getKeybindings();
////		System.out.println( kb );
////		kb.removeActionMap( "navigation" );
////		kb.removeInputMap( "navigation" );
//
////		bdv.getBdvHandle().getTriggerbindings().removeInputTriggerMap( "block_transform" );
//
//		final MaskedSourceEditorMouseListener ml = new MaskedSourceEditorMouseListener( 3, null, bdv.getBdvHandle().getViewerPanel() );
//		ml.setMaskInterpolationType( img );
////		bdv.getBdvHandle().getViewerPanel().getDisplay().addMouseListener( ml );
//
		double x = 50;
		RealRandomAccess< DoubleType > access = img.realRandomAccess();
		access.setPosition( center );
		while ( x < 100 )
		{
			access.move( 1, 0 );
			System.out.println( x + "," + access.get().getRealDouble());

			x = access.getDoublePosition( 0 );
		}
	}

	private void update()
	{
		rra = new FunctionRealRandomAccessible<>( nd, getFalloffFunction(), DoubleType::new );
	}

	private BiConsumer< RealLocalizable, DoubleType > getFalloffFunction()
	{
		if ( pfun == null )
		{
			pfun = fallOffShape.createFalloffFunction( this );
		}
		return pfun;
	}

	public FalloffShape getFallOffShape()
	{
		return fallOffShape;
	}

	public void setFalloffShape( String type )
	{
		setFalloffShape( FalloffShape.valueOf( type ) );
	}

	public void setFalloffShape( FalloffShape shape )
	{

		fallOffShape = shape;
		pfun = shape.createFalloffFunction( this );
		update();
	}

	public double getSquaredRadius()
	{
		return plateauR2;
	}

	public void setRadius( double r )
	{
		plateauR = r;
		plateauR2 = plateauR * plateauR ;
		if ( overlays != null )
			overlays.stream().forEach( o -> o.setInnerRadius( plateauR ) );
	}

	public void setSquaredRadius( double r2 )
	{
		plateauR2 = r2;
		plateauR = Math.sqrt( plateauR2 );
		if ( overlays != null )
			overlays.stream().forEach( o -> o.setInnerRadius( plateauR ) );
	}

	public double getSquaredSigma()
	{
		return sqrSigma;
	}

	public double getSigma()
	{
		return sigma;
	}

	public void setSigma( double sigma )
	{
		this.sigma = sigma < 0 ? -sigma : sigma;
		sqrSigma = this.sigma * this.sigma;

		if( sqrSigma <= 0  )
			sqrSigma = EPS;

		invSqrSigma = 1.0 / sqrSigma;
		updateGaussSigma();

		if ( overlays != null )
			overlays.stream().forEach( o -> o.setOuterRadiusDelta( this.sigma ));
	}

	public void setSquaredSigma( double squaredSigma )
	{
		setSigma( Math.sqrt( sqrSigma ));
	}

	public void incSquaredSigma( double increment )
	{
		sqrSigma += increment;
		sigma = Math.sqrt( sqrSigma );

		if( sqrSigma <= 0  )
			sqrSigma = EPS;

		invSqrSigma = 1.0 / sqrSigma;
		updateGaussSigma();
		if ( overlays != null )
			overlays.stream().forEach( o -> o.setOuterRadiusDelta( sigma ));
	}

	private void updateGaussSigma()
	{
		final double gsig = gaussSigma( sigma );
		gaussInvSqrSigma = 1.0 / ( gsig * gsig );
	}

	public void setCenter( RealLocalizable p )
	{
		center.setPosition( p );
		if ( overlays != null )
			overlays.stream().forEach( o -> o.setCenter( p ) );
	}

	public void setCenter( double[] p )
	{
		center.setPosition( p );
		if ( overlays != null )
			overlays.stream().forEach( o -> o.setCenter( p ) );
	}

	public RealPoint getCenter()
	{
		return center;
	}


	final public static double squaredDistance( final RealLocalizable position1, final RealLocalizable position2 )
	{
		double dist = 0;

		final int n = position1.numDimensions();
		for ( int d = 0; d < n; ++d )
		{
			final double pos = position2.getDoublePosition( d ) - position1.getDoublePosition( d );

			dist += pos * pos;
		}

		return dist;
	}

	@Override
	public int numDimensions()
	{
		return rra.numDimensions();
	}

	@Override
	public RealRandomAccess< DoubleType > realRandomAccess()
	{
		return rra.realRandomAccess();
	}

	@Override
	public RealRandomAccess< DoubleType > realRandomAccess( RealInterval interval )
	{
		return rra.realRandomAccess( interval );
	}

	public Element toXml()
	{
		final Element maskSettings = new Element( "transform-mask" );

		final Element type = new Element( "type" );
		type.setText( "plateau-spherical" );
		maskSettings.addContent( type );

		final Element c = XmlHelpers.doubleArrayElement( "center", center.positionAsDoubleArray() );
		maskSettings.addContent( c );

		final Element p = new Element( "parameters" );
		p.addContent( XmlHelpers.doubleElement( "squaredRadius", plateauR2 ) );
		p.addContent( XmlHelpers.doubleElement( "squaredSigma", sqrSigma ) );

		maskSettings.addContent( p );

		return maskSettings;
	}

	public void fromXml( Element elem )
	{
		setCenter( XmlHelpers.getDoubleArray( elem, "center" ) );

		final Element p = elem.getChild( "parameters" );
		setSquaredRadius( XmlHelpers.getDouble( p, "squaredRadius" ));
		setSquaredSigma( XmlHelpers.getDouble( p, "squaredSigma" ));
	}

	@Deprecated
	public void fromJson( JsonObject json )
	{
		final JsonArray c = json.get("center").getAsJsonArray();
		final double[] center = new double[ c.size() ];
		for( int i = 0; i < c.size(); i++ )
			center[i] = c.get( i ).getAsDouble();

		setCenter( center );
		setSquaredRadius(  json.get("squaredRadius").getAsDouble() );
		setSquaredSigma(  json.get("squaredSigma").getAsDouble() );
	}

	/**
	 * Re.turns the sigma that makes a Gaussian shape most like a cosine with period T.
	 * <p>
	 * see https://gist.github.com/bogovicj/d212b236868c76798edfd11150b2c9a0
	 *
	 * @param T the cosine period
	 * @return
	 */
	public static double gaussSigma( double T )
	{
		return ( 0.40535876907923957 * T + 0.03706937 );
	}

	public static class GaussianFalloff implements BiConsumer< RealLocalizable, DoubleType >
	{

		private final PlateauSphericalMaskRealRandomAccessible mask;

		public GaussianFalloff( PlateauSphericalMaskRealRandomAccessible mask )
		{
			this.mask = mask;
		}

		@Override
		public void accept( RealLocalizable x, DoubleType v )
		{
			v.setZero();
			double r2 = squaredDistance( x, mask.center );
			if ( r2 <= mask.plateauR2 )
				v.setOne();
			else
			{
				final double r = Math.sqrt( r2 );
//				final double t = (r2 - plateauR2);
				final double t = ( r - mask.plateauR );
				// TODO sample exp function and interpolate to speed up
				v.set( Math.exp( -0.5 * t * t * mask.gaussInvSqrSigma ) );
//				v.set( Math.cos( t * 0.5  + 0.5 ));
//				v.set( 1 / t );
			}
		}
	}

	public static class CosineFalloff implements BiConsumer< RealLocalizable, DoubleType >
	{

		private final PlateauSphericalMaskRealRandomAccessible mask;

		public CosineFalloff( PlateauSphericalMaskRealRandomAccessible mask )
		{
			this.mask = mask;
		}

		@Override
		public void accept( RealLocalizable x, DoubleType v )
		{
			final double r2 = squaredDistance( x, mask.center );
			final double r = Math.sqrt( r2 );
			if ( r2 <= mask.plateauR2 )
				v.setOne();
			else if ( r >= mask.plateauR + mask.sigma )
				v.setZero();
			else
			{
//				final double t = (r2 - plateauR2);
//				final double r = Math.sqrt( r2 );
				final double t = ( r - mask.plateauR );
				double val = 0.5 + 0.5 * Math.cos( t * PI / mask.sigma );
				v.set( val );
			}
		}
	}

	public static class LinearFalloff implements BiConsumer< RealLocalizable, DoubleType >
	{

		private final PlateauSphericalMaskRealRandomAccessible mask;

		public LinearFalloff( PlateauSphericalMaskRealRandomAccessible mask )
		{
			this.mask = mask;
		}

		@Override
		public void accept( RealLocalizable x, DoubleType v )
		{
			v.setZero();
			final double r2 = squaredDistance( x, mask.center );
			final double d2 = mask.plateauR + mask.sigma;
			if ( r2 <= mask.plateauR2 )
				v.setOne();
			else if ( r2 >= d2 * d2 )
				v.setZero();
			else
			{
				final double r = Math.sqrt( r2 );
				v.set( 1 - ( r - mask.plateauR ) / mask.sigma );
			}
		}
	}

	public static final String FALLOFF_SHAPE = "falloffShape";

	public static final String CENTER = "center";

	public static final String SQUARED_RADIUS = "squaredRadius";

	public static final String SQUARED_SIGMA = "squaredSigma";

	public static class RealPointToDoubleArray extends TypeAdapter< RealPoint >
	{

		@Override
		public void write( final JsonWriter out, final RealPoint value ) throws IOException
		{
			final JsonWriter jsonWriter = out.beginArray();
			for ( int i = 0; i < value.numDimensions(); i++ )
			{
				jsonWriter.value( value.getDoublePosition( i ) );
			}
			jsonWriter.endArray();
		}

		@Override
		public RealPoint read( final JsonReader in ) throws IOException
		{
			in.beginArray();
			ArrayList< Double > pos = new ArrayList<>();
			while ( in.hasNext() )
			{
				pos.add( in.nextDouble() );
			}
			in.endArray();
			return new RealPoint( pos.stream().mapToDouble( it -> it ).toArray() );
		}
	}

}
