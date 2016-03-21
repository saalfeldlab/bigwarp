package bigwarp;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.ui.TransformEventHandler2D;
import net.imglib2.ui.TransformEventHandlerFactory;
import net.imglib2.ui.TransformListener;

public class TransformHandler3DWrapping2D extends MouseAdapter implements KeyListener, TransformEventHandler< AffineTransform3D >,  TransformListener< AffineTransform2D >
{

	final static private TransformEventHandlerFactory< AffineTransform3D > factory = new TransformEventHandlerFactory< AffineTransform3D >()
	{
		@Override
		public TransformEventHandler< AffineTransform3D > create( final TransformListener< AffineTransform3D > transformListener )
		{
			return new TransformHandler3DWrapping2D( transformListener );
		}
	};

	public static TransformEventHandlerFactory< AffineTransform3D > factory()
	{
		return factory;
	}
	
	/**
	 * Whom to notify when the current transform is changed.
	 */
	protected TransformListener< AffineTransform3D > listener;
	
	protected TransformEventHandler2D handler2d;
	
	protected AffineTransform3D affine3d;
	
	public TransformHandler3DWrapping2D( final TransformListener< AffineTransform3D > transformListener )
	{
		init();
		setTransformListener( transformListener );
	}
	
	protected void init()
	{
		affine3d = new AffineTransform3D();
		affine3d.identity();
		handler2d = new TransformEventHandler2D( this );
	}
	
	@Override
	public AffineTransform3D getTransform() 
	{
		synchronized ( affine3d )
		{
			return affine3d.copy();
		}
	}

	@Override
	public void setTransform( AffineTransform3D transform ) 
	{
		synchronized ( affine3d ) 
		{
			affine3d.set( 
					transform.get( 0, 0 ), transform.get( 0, 1 ), 0.0, transform.get( 0, 3 ), 
					transform.get( 1, 0 ), transform.get( 1, 1 ), 0.0, transform.get( 1, 3 ),
					0.0, 0.0, 1.0, 0.0);
			
			AffineTransform2D tmp2dxfm = new AffineTransform2D();
			tmp2dxfm.set( 	transform.get( 0, 0 ), transform.get( 0, 1 ), transform.get( 0, 3 ),
							transform.get( 1, 0 ), transform.get( 1, 1 ), transform.get( 1, 3 ));
			handler2d.setTransform(tmp2dxfm);
		}
	}

	@Override
	public void setCanvasSize(int width, int height, boolean updateTransform) 
	{
		handler2d.setCanvasSize(width, height, updateTransform);
	}

	@Override
	public void setTransformListener(
			TransformListener<AffineTransform3D> transformListener) 
	{
		listener = transformListener;
	}

	@Override
	public String getHelpString() 
	{
		return handler2d.getHelpString();
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		handler2d.keyPressed( arg0 );
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		handler2d.keyReleased( arg0 );
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		handler2d.keyTyped( arg0 );
	}

	@Override
	public void mousePressed( final MouseEvent e )
	{ 
		handler2d.mousePressed( e );
	}
	
	@Override
	public void mouseDragged( final MouseEvent e )
	{
		handler2d.mouseDragged( e );
	}
	
	@Override
	public void mouseWheelMoved( final MouseWheelEvent e )
	{
		handler2d.mouseWheelMoved( e );
	}
	
	private void project( final AffineTransform2D transform )
	{
		synchronized ( affine3d ) 
		{
			affine3d.set( 
					transform.get( 0, 0 ), transform.get( 0, 1 ), 0.0, transform.get( 0, 2 ), 
					transform.get( 1, 0 ), transform.get( 1, 1 ), 0.0, transform.get( 1, 2 ), 
					0.0, 0.0, 1.0, 0.0);
		}
	}
	
	protected void update()
	{
		if ( listener != null )
			listener.transformChanged( affine3d );
	}
	
	@Override
	public void transformChanged(AffineTransform2D transform) 
	{
		project( transform );
		update();
	}
}
