/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2025 Howard Hughes Medical Institute.
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

import bdv.viewer.animate.AbstractTransformAnimator;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.ConstantUtils;

public class InterpolatedTimeVaryingTransformation< T extends AbstractTransformAnimator > implements TimeVaryingTransformation
{
	private RealTransform a;
	private RealTransform b;

	public InterpolatedTimeVaryingTransformation( RealTransform a, RealTransform b )
	{
		this.a = a;
		this.b = b;
	}

	public RealTransform get( double t )
	{
		return new SpatiallyInterpolatedRealTransform<>( a, b, ConstantUtils.constantRealRandomAccessible( new DoubleType( t ), a.numSourceDimensions() ) );
	}

}
