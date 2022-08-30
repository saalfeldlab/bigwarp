package bigwarp.source;

import java.util.List;
import java.util.function.BiConsumer;

import org.jdom2.Element;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import bdv.viewer.overlay.BigWarpMaskSphereOverlay;
import mpicbg.spim.data.XmlHelpers;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.type.numeric.real.DoubleType;

public class PlateauSphericalMaskRealRandomAccessible implements RealRandomAccessible< DoubleType >
{
	private BiConsumer< RealLocalizable, DoubleType > pfun;
	private FunctionRealRandomAccessible< DoubleType > rra;
	private List<BigWarpMaskSphereOverlay> overlays;

	private int nd;
	
	private double plateauR;
	private double plateauR2;

	private double sigma;
	private double sqrSigma;
	private double invSqrSigma;
	private double gaussInvSqrSigma;

	private RealPoint center;

	private static final double EPS = 1e-6;
	private static final double PIon2 = Math.PI / 2.0;
	private static final double PI = Math.PI;

	public static enum FalloffType { COSINE, GAUSSIAN, LINEAR };

	public PlateauSphericalMaskRealRandomAccessible( int n, RealPoint center )
	{
		this.nd = n;
		this.center = center;
		pfun = new CosineFalloff();
		update();

		setRadius( 8.0 );
		setSigma ( 10.0 );
	}

	public void setOverlays( final List< BigWarpMaskSphereOverlay > overlays )
	{
		this.overlays = overlays;
		if ( overlays != null )
		{
			overlays.stream().forEach( o -> { 
				o.setCenter( center ) ;
				o.setInnerRadius( plateauR );
				o.setOuterRadiusDelta( sigma );
			});
		}
	}

	private void update()
	{
		rra = new FunctionRealRandomAccessible<>( nd, pfun, DoubleType::new );
	}

	public void setType( String type )
	{
		setType( FalloffType.valueOf( type ));
	}

	public void setType( FalloffType type )
	{
		switch (type) {
		case GAUSSIAN:
			pfun = new GaussianFalloff();
			update();
			break;
		case COSINE:
			pfun = new CosineFalloff();
			update();
			break;
		case LINEAR:
			pfun = new LinearFalloff();
			update();
			break;
		default:
			break;
		}
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
		this.sigma = sigma;
		sqrSigma = sigma * sigma;

		if( sqrSigma <= 0  )
			sqrSigma = EPS;

		invSqrSigma = 1.0 / sqrSigma;
		updateGaussSigma();

		if ( overlays != null )
			overlays.stream().forEach( o -> o.setOuterRadiusDelta( sigma ));
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

	public class GaussianFalloff implements BiConsumer< RealLocalizable, DoubleType > {

		@Override
		public void accept( RealLocalizable x, DoubleType v )
		{
			v.setZero();
			double r2 = squaredDistance( x, center );
			if( r2 <= plateauR2 )
				v.setOne();
			else
			{
				final double r = Math.sqrt( r2 );
//				final double t = (r2 - plateauR2);
				final double t = (r - plateauR);
				// TODO sample exp function and interpolate to speed up
				v.set( Math.exp( -0.5 * t * t * gaussInvSqrSigma ) );
//				v.set( Math.cos( t * 0.5  + 0.5 ));
//				v.set( 1 / t );
			}
		}
	}

	public class CosineFalloff implements BiConsumer< RealLocalizable, DoubleType > {

		@Override
		public void accept( RealLocalizable x, DoubleType v )
		{
			final double r2 = squaredDistance( x, center );
			final double r = Math.sqrt( r2 );
			if( r2 <= plateauR2 )
				v.setOne();
			else if ( r >= plateauR + sigma )
				v.setZero();
			else
			{
//				final double t = (r2 - plateauR2);
//				final double r = Math.sqrt( r2 );
				final double t = (r - plateauR);
				double val = 0.5 + 0.5 * Math.cos( t * PI / sigma );
				v.set( val );
			}
		}
	}

	public class LinearFalloff implements BiConsumer< RealLocalizable, DoubleType > {

		@Override
		public void accept( RealLocalizable x, DoubleType v )
		{
			v.setZero();
			final double r2 = squaredDistance( x, center );
			final double d2 = plateauR + sigma;
			if( r2 <= plateauR2 )
				v.setOne();
			else if( r2 >= d2 * d2 )
				v.setZero();
			else
			{
				final double r = Math.sqrt( r2 );
				v.set( 1 - (r - plateauR) /  sigma );
			}
		}
	}

}
