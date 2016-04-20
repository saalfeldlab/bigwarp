package bigwarp;

import org.scijava.ui.behaviour.io.InputTriggerConfig;

import bdv.BehaviourTransformEventHandler3D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformEventHandlerFactory;
import net.imglib2.ui.TransformListener;

public class BehaviourTransformEventHandler2D extends BehaviourTransformEventHandler3D
{
	
	AffineTransform3D affine2d; // a 3d affine limited to the z-plane

	
	public static TransformEventHandlerFactory< AffineTransform3D > factory()
	{
		return new BehaviourTransformEventHandler3DFactory();
	}

	public static class BehaviourTransformEventHandler2DFactory implements TransformEventHandlerFactory< AffineTransform3D >
	{
		private InputTriggerConfig config = new InputTriggerConfig();

		public void setConfig( final InputTriggerConfig config )
		{
			this.config = config;
		}

		@Override
		public BehaviourTransformEventHandler2D create( final TransformListener< AffineTransform3D > transformListener )
		{
			return new BehaviourTransformEventHandler2D( transformListener, config );
		}
	}
	
	public BehaviourTransformEventHandler2D(
			TransformListener< AffineTransform3D > listener, 
			InputTriggerConfig config )
	{
		super( listener, config );
	}

	@Override
	public AffineTransform3D getTransform()
	{
		AffineTransform3D transform = super.getTransform();
		affine2d.set( 
				transform.get( 0, 0 ), transform.get( 0, 1 ), 0.0, transform.get( 0, 3 ), 
				transform.get( 1, 0 ), transform.get( 1, 1 ), 0.0, transform.get( 1, 3 ),
				0.0, 0.0, 1.0, 0.0); // no z- rotation or translation

		return affine2d.copy();
	}
}
