package bigwarp.transforms;

import mpicbg.models.InvertibleCoordinateTransform;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.realtransform.InvertibleRealTransform;

public class WrappedCoordinateTransform implements InvertibleRealTransform
{
	private final InvertibleCoordinateTransform ct;
	private final InvertibleCoordinateTransform ct_inv;
	private final int nd;

	public WrappedCoordinateTransform( InvertibleCoordinateTransform ct, int nd )
	{
        this.nd = nd;
		this.ct = ct;
		this.ct_inv = ct.createInverse();
	}

	public InvertibleCoordinateTransform getTransform()
    {
		return ct;
	}

	@Override
	public void apply(double[] src, double[] tgt)
    {
		double[] res = ct.apply( src );
        System.arraycopy( res, 0, tgt, 0, res.length );
	}

	@Override
	public void apply( RealLocalizable src, RealPositionable tgt )
    {
        double[] srcpt = new double[ src.numDimensions() ];
        src.localize( srcpt );

        double[] res = ct.apply( srcpt );
        tgt.setPosition( res );
	}

	@Override
	public int numSourceDimensions()
    {
		return nd;
	}

	@Override
	public int numTargetDimensions()
    {
		return nd; 
	}

	@Override
	public void applyInverse( double[] src, double[] tgt )
    {
	    double[] res = ct_inv.apply( tgt );
        System.arraycopy( res, 0, src, 0, res.length );    
	}

	@Override
	public void applyInverse( RealPositionable src, RealLocalizable tgt )
    {
        double[] tgtpt = new double[ tgt.numDimensions() ];
        tgt.localize( tgtpt );
        
        double[] res = ct_inv.apply( tgtpt );
        src.setPosition( res );
	}

	@Override
	public WrappedCoordinateTransform copy()
    {
		return new WrappedCoordinateTransform( ct, nd );
	}

	@Override
	public WrappedCoordinateTransform inverse()
    {
		return new WrappedCoordinateTransform( ct_inv, nd );
	}

}
