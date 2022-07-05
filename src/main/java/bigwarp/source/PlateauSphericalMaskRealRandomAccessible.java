package bigwarp.source;

import java.util.function.BiConsumer;

import bdv.gui.MaskedSourceEditorMouseListener;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
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
	private PlateauFunction pfun;
	private FunctionRealRandomAccessible< DoubleType > rra;
	
	private double plateauR;
	private double plateauR2;

	private double sqrSigma;
	private double invSqrSigma;

	private RealPoint center;

	private static final double EPS = 1e-6;

	public PlateauSphericalMaskRealRandomAccessible( int n, RealPoint center )
	{
		this.center = center;
		pfun = new PlateauFunction();
		rra = new FunctionRealRandomAccessible<>( 3, pfun, DoubleType::new );

		setRadius( 8.0 );
		setSigma ( 10.0 );
	}
	
	public static void main( String[] args )
	{
		long S = 50;
		RealPoint pt = new RealPoint( new double[]{ S, S, S } );

		PlateauSphericalMaskRealRandomAccessible img = new PlateauSphericalMaskRealRandomAccessible( 3, pt );
		Interval interval = Intervals.createMinSize( 0, 0, 0, 2*S, 2*S, 2*S );

//		BdvOptions options = BdvOptions.options().screenScales( new double[] { 1 } );
		BdvOptions options = BdvOptions.options();
		BdvStackSource< DoubleType > bdv = BdvFunctions.show( img.rra, interval, "img", options );
		bdv.getBdvHandle().getSetupAssignments().getMinMaxGroups().get( 0 ).setRange( 0, 1 );
		
//		InputActionBindings kb = bdv.getBdvHandle().getKeybindings();
//		System.out.println( kb );
//		kb.removeActionMap( "navigation" );
//		kb.removeInputMap( "navigation" );

//		bdv.getBdvHandle().getTriggerbindings().removeInputTriggerMap( "block_transform" );

		final MaskedSourceEditorMouseListener ml = new MaskedSourceEditorMouseListener( 3, null, bdv.getBdvHandle().getViewerPanel() );
		ml.setMask( img );
//		bdv.getBdvHandle().getViewerPanel().getDisplay().addMouseListener( ml );

	}

	public void setRadius( double r )
	{
		plateauR = r;
		plateauR2 = plateauR * plateauR ;
	}

	public void setSquaredRadius( double r2 )
	{
		plateauR2 = r2;
	}

	public void setSigma( double sigma )
	{
		sqrSigma = sigma * sigma;

		if( sqrSigma <= 0  )
			sqrSigma = EPS;

		invSqrSigma = 1.0 / sqrSigma;
	}

	public void setSquaredSigma( double squaredSigma )
	{
		sqrSigma = squaredSigma;
		if( sqrSigma <= 0  )
			sqrSigma = EPS;

		invSqrSigma = 1.0 / squaredSigma;
	}

	public void incSquaredSigma( double increment )
	{
		sqrSigma += increment;

		if( sqrSigma <= 0  )
			sqrSigma = EPS;

		invSqrSigma = 1.0 / sqrSigma;
	}

	public void setCenter( RealLocalizable p )
	{
		center.setPosition( p );
	}

	public RealPoint getCenter()
	{
		return center;
	}

	public class PlateauFunction implements BiConsumer< RealLocalizable, DoubleType > {

		@Override
		public void accept( RealLocalizable x, DoubleType v )
		{
			v.setZero();
			double r2 = squaredDistance( x, center );
			if( r2 <= plateauR2 )
				v.setOne();
			else
			{
				double t = (r2 - plateauR2);
				// TODO sample exp function and interpolate to speed up
				v.set( Math.exp( -0.5 * t * invSqrSigma ) );
//				v.set( Math.cos( t * 0.5  + 0.5 ));
//				v.set( 1 / t );
			}
		}
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

}
