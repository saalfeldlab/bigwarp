/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2016 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imglib2.ui;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import net.imglib2.realtransform.AffineTransform3D;


/**
 * A {@link TransformEventHandler} that changes an {@link AffineTransform2D} in
 * response to mouse and keyboard events.
 * 
 * @author Stephan Saalfeld
 * @author Tobias Pietzsch
 */
public class TransformEventHandler2Dto3D extends MouseAdapter implements KeyListener, TransformEventHandler< AffineTransform3D >
{
	final static private TransformEventHandlerFactory< AffineTransform3D > factory = new TransformEventHandlerFactory< AffineTransform3D >()
	{
		@Override
		public TransformEventHandler< AffineTransform3D > create( final TransformListener< AffineTransform3D > transformListener )
		{
			return new TransformEventHandler2Dto3D( transformListener );
		}
	};

	public static TransformEventHandlerFactory< AffineTransform3D > factory()
	{
		return factory;
	}

	/**
	 * Current source to screen transform.
	 */
	final protected AffineTransform3D affine = new AffineTransform3D();

	/**
	 * Whom to notify when the {@link #affine current transform} is changed.
	 */
	protected TransformListener< AffineTransform3D > listener;

	/**
	 * Copy of {@link #affine current transform} when mouse dragging started.
	 */
	final protected AffineTransform3D affineDragStart = new AffineTransform3D();

	/**
	 * Coordinates where mouse dragging started.
	 */
	protected double oX, oY;

	/**
	 * The screen size of the canvas (the component displaying the image and
	 * generating mouse events).
	 */
	protected int canvasW = 1, canvasH = 1;

	/**
	 * Screen coordinates to keep centered while zooming or rotating with the
	 * keyboard. For example set these to
	 * <em>(screen-width/2, screen-height/2)</em>
	 */
	protected int centerX = 0, centerY = 0;

	public TransformEventHandler2Dto3D( final TransformListener< AffineTransform3D > listener )
	{
		this.listener = listener;
	}

	@Override
	public AffineTransform3D getTransform()
	{
		synchronized ( affine )
		{
			return affine.copy();
		}
	}

	@Override
	public void setTransform( final AffineTransform3D transform )
	{
		synchronized ( affine )
		{
			affine.set( transform );
		}
	}

	@Override
	public void setCanvasSize( final int width, final int height, final boolean updateTransform )
	{
		if ( updateTransform )
		{
			synchronized ( affine )
			{
				affine.set( affine.get( 0, 3 ) - canvasW / 2, 0, 3 );
				affine.set( affine.get( 1, 3 ) - canvasH / 2, 1, 3 );
				affine.scale( ( double ) width / canvasW );
				affine.set( affine.get( 0, 3 ) + width / 2, 0, 3 );
				affine.set( affine.get( 1, 3 ) + height / 2, 1, 3 );
				update();
			}
		}
		canvasW = width;
		canvasH = height;
		centerX = width / 2;
		centerY = height / 2;
	}

	@Override
	public void setTransformListener( final TransformListener< AffineTransform3D > transformListener )
	{
		listener = transformListener;
	}

	@Override
	public String getHelpString()
	{
		return helpString;
	}

	/**
	 * notifies {@link #listener} that the current transform changed.
	 */
	protected void update()
	{
		if ( listener != null )
			listener.transformChanged( affine );
	}

	/**
	 * One step of rotation (radian).
	 */
	final private static double step = Math.PI / 180;

	final private static String NL = System.getProperty( "line.separator" );

	final private static String helpString =
			"Mouse control:" + NL + " " + NL +
					"rotate the image by left-click and dragging the image in the canvas, " + NL +
					"move the image by middle-or-right-click and dragging the image in the canvas, " + NL +
					"zoom in and out using the mouse-wheel." + NL + " " + NL +
					"Key control:" + NL + " " + NL +
					"CURSOR LEFT - Rotate clockwise." + NL +
					"CURSOR RIGHT - Rotate counter-clockwise." + NL +
					"CURSOR UP - Zoom in." + NL +
					"CURSOR DOWN - Zoom out." + NL +
					"SHIFT - Rotate and zoom 10x faster." + NL +
					"CTRL - Rotate and zoom 10x slower.";

	/**
	 * Return rotate/translate/scale speed resulting from modifier keys.
	 * 
	 * Normal speed is 1. SHIFT is faster (10). CTRL is slower (0.1).
	 * 
	 * @param modifiers
	 * @return speed resulting from modifier keys.
	 */
	private static double keyModfiedSpeed( final int modifiers )
	{
		if ( ( modifiers & InputEvent.SHIFT_DOWN_MASK ) != 0 )
			return 10;
		else if ( ( modifiers & InputEvent.CTRL_DOWN_MASK ) != 0 )
			return 0.1;
		else
			return 1;
	}

	@Override
	public void mousePressed( final MouseEvent e )
	{
		synchronized ( affine )
		{
			oX = e.getX();
			oY = e.getY();
			affineDragStart.set( affine );
		}
	}

	@Override
	public void mouseDragged( final MouseEvent e )
	{
		synchronized ( affine )
		{
			final int modifiers = e.getModifiersEx();

			if ( ( modifiers & InputEvent.BUTTON1_DOWN_MASK ) != 0 ) // rotate
			{
				affine.set( affineDragStart );

				final double dX = e.getX() - centerX;
				final double dY = e.getY() - centerY;
				final double odX = oX - centerX;
				final double odY = oY - centerY;
				final double theta = Math.atan2( dY, dX ) - Math.atan2( odY, odX );

				rotate( theta );
			}
			else if ( ( modifiers & ( InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK ) ) != 0 ) // translate
			{
				affine.set( affineDragStart );

				final double dX = oX - e.getX();
				final double dY = oY - e.getY();

				affine.set( affine.get( 0, 3 ) - dX, 0, 3 );
				affine.set( affine.get( 1, 3 ) - dY, 1, 3 );
			}

			update();
		}
	}

	/**
	 * Scale by factor s. Keep screen coordinates (x, y) fixed.
	 */
	private void scale( final double s, final double x, final double y )
	{
		// center shift
		affine.set( affine.get( 0, 3 ) - x, 0, 3 );
		affine.set( affine.get( 1, 3 ) - y, 1, 3 );

		// scale
		affine.scale( s );

		// center un-shift
		affine.set( affine.get( 0, 3 ) + x, 0, 3 );
		affine.set( affine.get( 1, 3 ) + y, 1, 3 );
	}

	/**
	 * Rotate by d radians. Keep screen coordinates ({@link #centerX},
	 * {@link #centerY}) fixed.
	 */
	private void rotate( final double d )
	{
		// center shift
		affine.set( affine.get( 0, 3 ) - centerX, 0, 3 );
		affine.set( affine.get( 1, 3 ) - centerY, 1, 3 );

		// rotate
		affine.rotate( 2, d );

		// center un-shift
		affine.set( affine.get( 0, 3 ) + centerX, 0, 3 );
		affine.set( affine.get( 1, 3 ) + centerY, 1, 3 );
	}

	@Override
	public void mouseWheelMoved( final MouseWheelEvent e )
	{
		synchronized ( affine )
		{
			final int modifiers = e.getModifiersEx();
			final double v = keyModfiedSpeed( modifiers );
			final int s = e.getWheelRotation();

			final double dScale = 1.0 + 0.05 * v;
			if ( s > 0 )
				scale( 1.0 / dScale, e.getX(), e.getY() );
			else
				scale( dScale, e.getX(), e.getY() );

			update();
		}
	}

	@Override
	public void keyPressed( final KeyEvent e )
	{
		synchronized ( affine )
		{
			final double v = keyModfiedSpeed( e.getModifiersEx() );
			if ( e.getKeyCode() == KeyEvent.VK_LEFT )
			{
				rotate( step * v );
				update();
			}
			else if ( e.getKeyCode() == KeyEvent.VK_RIGHT )
			{
				rotate( step * -v );
				update();
			}
			if ( e.getKeyCode() == KeyEvent.VK_UP )
			{
				final double dScale = 1.0 + 0.1 * v;
				scale( dScale, centerX, centerY );
				update();
			}
			else if ( e.getKeyCode() == KeyEvent.VK_DOWN )
			{
				final double dScale = 1.0 + 0.1 * v;
				scale( 1.0 / dScale, centerX, centerY );
				update();
			}
		}
	}

	@Override
	public void keyTyped( final KeyEvent e )
	{}

	@Override
	public void keyReleased( final KeyEvent e )
	{}
}
