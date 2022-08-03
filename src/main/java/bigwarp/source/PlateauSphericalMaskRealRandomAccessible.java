package bigwarp.source;

import java.util.function.BiConsumer;

import org.jdom2.Element;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import mpicbg.spim.data.XmlHelpers;
import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;

public class PlateauSphericalMaskRealRandomAccessible implements RealRandomAccessible< DoubleType >
{
	private BiConsumer< RealLocalizable, DoubleType > pfun;
	private FunctionRealRandomAccessible< DoubleType > rra;

	private int nd;
	
	private double plateauR;
	private double plateauR2;

	private double sigma;
	private double sqrSigma;
	private double invSqrSigma;

	private RealPoint center;

	private static final double EPS = 1e-6;
	private static final double PIon2 = Math.PI / 2.0;
	private static final double PI = Math.PI;

	public static enum FalloffType { GAUSSIAN, COSINE };

	public PlateauSphericalMaskRealRandomAccessible( int n, RealPoint center )
	{
		this.nd = n;
		this.center = center;
		pfun = new CosineFalloff();
		update();

		setRadius( 8.0 );
		setSigma ( 10.0 );
	}
	
	public static void main( String[] args )
	{
		long S = 50;
		double[] center = new double[] { S, S, S };
		RealPoint pt = RealPoint.wrap( center );

		PlateauSphericalMaskRealRandomAccessible img = new PlateauSphericalMaskRealRandomAccessible( 3, pt );
		img.setRadius( 10 );
		img.setSigma( 10 );
		Interval interval = Intervals.createMinSize( 0, 0, 0, 2*S, 2*S, 2*S );
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
//		ml.setMask( img );
////		bdv.getBdvHandle().getViewerPanel().getDisplay().addMouseListener( ml );
//
		double x = 50;
		RealRandomAccess< DoubleType > access = img.realRandomAccess();
		access.setPosition( center );
		while( x < 100 )
		{
			access.move( 1, 0 );
			System.out.println( x + "," + access.get().getRealDouble());

			x = access.getDoublePosition( 0 );
		}

//		x = 70;
//		access.setPosition( new double[] { x, 50, 50 } );
//		System.out.println( x + "," + access.get().getRealDouble());
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
	}

	public void setSquaredRadius( double r2 )
	{
		plateauR2 = r2;
		plateauR = Math.sqrt( plateauR2 );
	}

	public double getSquaredSigma()
	{
		return sqrSigma;
	}

	public void setSigma( double sigma )
	{
		this.sigma = sigma;
		sqrSigma = sigma * sigma;

		if( sqrSigma <= 0  )
			sqrSigma = EPS;

		invSqrSigma = 1.0 / sqrSigma;
	}

	public void setSquaredSigma( double squaredSigma )
	{
		sqrSigma = squaredSigma;
		sigma = Math.sqrt( sqrSigma );
		if( sqrSigma <= 0  )
			sqrSigma = EPS;

		invSqrSigma = 1.0 / squaredSigma;
	}

	public void incSquaredSigma( double increment )
	{
		sqrSigma += increment;
		sigma = Math.sqrt( sqrSigma );

		if( sqrSigma <= 0  )
			sqrSigma = EPS;

		invSqrSigma = 1.0 / sqrSigma;
	}
//
//	public void mulSquaredSigma( double factor )
//	{
//		sqrSigma *= factor;
//		sigma = Math.sqrt( sqrSigma );
//
//		if( sqrSigma <= 0  )
//			sqrSigma = EPS;
//
//		invSqrSigma = 1.0 / sqrSigma;
//	}

	public void setCenter( RealLocalizable p )
	{
		center.setPosition( p );
	}

	public void setCenter( double[] p )
	{
		center.setPosition( p );
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
				v.set( Math.exp( -0.5 * t * t * invSqrSigma ) );
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
			else if ( r > plateauR + 2 * sigma )
				v.setZero();
			else
			{
//				final double t = (r2 - plateauR2);
//				final double r = Math.sqrt( r2 );
				final double t = (r - plateauR);
				double val = 0.5 + 0.5 * Math.cos( t * PIon2 / sigma );
				v.set( val );
			}
		}
	}

}
