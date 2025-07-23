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
package bdv.ui.convertersetupeditor;

import bdv.util.BoundedRange;
import bigwarp.BigWarp;

public class MaskBoundsRangePanel extends BoundedRangePanel {

	private static final long serialVersionUID = -4065636040399818543L;

	public MaskBoundsRangePanel(BigWarp<?> bw ) {
		this(bw, new BoundedRange(0, 1, 0, 1));
	}

	public MaskBoundsRangePanel( final BigWarp<?> bw, final BoundedRange range ) {
		super( range );
		setConsistent( true );
		setup( bw );
	}

	protected void setup( BigWarp<?> bw ) {
		changeListeners().add(new ChangeListener() {
			@Override
			public void boundedRangeChanged() {
				bw.getBwTransform().setMaskIntensityBounds( getRange().getMin(), getRange().getMax());
			}
		});
	}

}
