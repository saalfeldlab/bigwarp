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
