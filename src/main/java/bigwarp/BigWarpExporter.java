package bigwarp;

import bdv.viewer.Interpolation;
import ij.ImagePlus;

public interface BigWarpExporter <T>
{

	public abstract ImagePlus exportMovingImagePlus( final boolean isVirtual );

	public void setInterp( Interpolation interp );
}
