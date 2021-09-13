/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2021 Howard Hughes Medical Institute.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
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
