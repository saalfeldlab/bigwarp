/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2022 Howard Hughes Medical Institute.
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
package net.imglib2.realtransform;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;

public class InvertibleWrapped2DIntermediate3D implements InvertibleRealTransform
{
	public InvertibleRealTransform transform;

	public double[] tmp;

	public InvertibleWrapped2DIntermediate3D( final InvertibleRealTransform transform )
	{
		this.transform = transform;
		tmp = new double[ 3 ];
	}

	@Override
	public int numSourceDimensions()
	{
		return 2;
	}

	@Override
	public int numTargetDimensions()
	{
		return 2;
	}

	public InvertibleRealTransform getTransform()
	{
		return transform;
	}

	@Override
	public void apply(double[] source, double[] target)
	{
		tmp[ 0 ] = target[ 0 ];
		tmp[ 1 ] = target[ 1 ];
		transform.apply( tmp, tmp );
		source[ 0 ] = tmp[ 0 ];
		source[ 1 ] = tmp[ 1 ];
	}

	@Override
	public void apply(RealLocalizable source, RealPositionable target)
	{
		tmp[ 0 ] = source.getDoublePosition( 0 );
		tmp[ 1 ] = source.getDoublePosition( 1 );
		transform.apply( tmp, tmp );
		target.setPosition( tmp[ 0 ], 0 );
		target.setPosition( tmp[ 1 ], 1 );
	}

	@Override
	public void applyInverse( double[] source, double[] target )
	{
		tmp[ 0 ] = target[ 0 ];
		tmp[ 1 ] = target[ 1 ];
		transform.applyInverse( tmp, tmp );
		source[ 0 ] = tmp[ 0 ];
		source[ 1 ] = tmp[ 1 ];
	}

	@Override
	public void applyInverse( RealPositionable source, RealLocalizable target )
	{
		tmp[ 0 ] = target.getDoublePosition( 0 );
		tmp[ 1 ] = target.getDoublePosition( 1 );
		transform.applyInverse( tmp, tmp );
		source.setPosition( tmp[ 0 ], 0 );
		source.setPosition( tmp[ 1 ], 1 );
	}

	@Override
	public InvertibleWrapped2DIntermediate3D copy()
	{
		return new InvertibleWrapped2DIntermediate3D( transform.copy() );
	}

	@Override
	public InvertibleRealTransform inverse()
	{
		return new InvertibleWrapped2DIntermediate3D( transform.inverse() );
	}

}
