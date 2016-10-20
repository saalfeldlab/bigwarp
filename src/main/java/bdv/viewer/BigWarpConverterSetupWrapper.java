package bdv.viewer;

import net.imglib2.type.numeric.ARGBType;
import bdv.tools.brightness.ConverterSetup;
import bigwarp.BigWarp;

public class BigWarpConverterSetupWrapper implements ConverterSetup {

	protected ConverterSetup cs;
	protected BigWarp bw;
	
	public BigWarpConverterSetupWrapper( BigWarp bw, ConverterSetup cs )
	{
		this.bw = bw;
		this.cs = cs;
	}
	
	public ConverterSetup getSourceConverterSetup(){
		return cs;
	}
	
	@Override
	public int getSetupId() {
		return cs.getSetupId();
	}


	@Override
	public void setColor(ARGBType color) {
		cs.setColor(color);
		bw.getViewerFrameP().getViewerPanel().requestRepaint();
		bw.getViewerFrameQ().getViewerPanel().requestRepaint();
	}


	@Override
	public ARGBType getColor() {
		return cs.getColor();
	}

	@Override
	public void setDisplayRange( double min, double max )
	{
		cs.setDisplayRange(min, max);
		bw.getViewerFrameP().getViewerPanel().requestRepaint();
		bw.getViewerFrameQ().getViewerPanel().requestRepaint();
		
	}

	@Override
	public boolean supportsColor()
	{
		return cs.supportsColor();
	}

	@Override
	public double getDisplayRangeMin()
	{
		return cs.getDisplayRangeMin();
	}

	@Override
	public double getDisplayRangeMax()
	{
		return cs.getDisplayRangeMax();
	}

	@Override
	public void setViewer( RequestRepaint arg0 )
	{
		// do nothing
	}

}
