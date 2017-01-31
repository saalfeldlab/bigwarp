/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package bigwarp.loader;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import bdv.img.virtualstack.VirtualStackImageLoader;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.WrapBasicImgLoader;
import ij.ImagePlus;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;

/**
 *
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public class ImagePlusLoader implements Loader
{
	final private ImagePlus[] impList;
	final private int numChannels;
	private int index; // keep track of the setupIds given to each source

	private boolean is3d;
	private boolean isMultiChannel;

	public ImagePlusLoader( final ImagePlus imp )
	{
		this( new ImagePlus[]{ imp } );
	}

	public ImagePlusLoader( final ImagePlus[] impList )
	{
		this.impList = impList;
		int nc = 0;
		for( ImagePlus ip : impList )
			nc += ip.getNChannels();

		numChannels = nc;
	}

	public int numChannels()
	{
		return numChannels;
	}

	public boolean is3d()
	{
		return is3d;
	}

	public boolean isMultiChannel()
	{
		return isMultiChannel;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public SpimDataMinimal[] load()
	{
		return loadAll( 255 );
	}

	public SpimDataMinimal[] loadAll( int startid )
	{
		SpimDataMinimal[] out = new SpimDataMinimal[ impList.length ];
		index = startid;
		for( int i = 0; i < impList.length; i++ )
		{
			out[ i ] = load( range( index, impList[ i ].getNChannels() ), impList[ i ] );
			index += impList[ i ].getNChannels();
		}
		return out;
	}

	public SpimDataMinimal load( int[] ids, ImagePlus imp )
	{
		// get calibration and image size
		final double pw;
		final double ph;
		final double pd;

		pw = imp.getCalibration().pixelWidth;
		ph = imp.getCalibration().pixelHeight;
		pd = imp.getCalibration().pixelDepth;

		String punit = imp.getCalibration().getUnit();
		if ( punit == null || punit.isEmpty() )
			punit = "px";
		final FinalVoxelDimensions voxelSize = new FinalVoxelDimensions( punit, pw, ph, pd );
		final int w = imp.getWidth();
		final int h = imp.getHeight();
		final int d = imp.getNSlices();
		final int numTimepoints = imp.getNFrames();
		final int numSetups = imp.getNChannels();
		final FinalDimensions size = new FinalDimensions( new int[] { w, h, d } );

		is3d = ( d > 1 );
		isMultiChannel = ( numSetups > 1 );

		// create ImgLoader wrapping the image
		final BasicImgLoader imgLoader;
		if ( imp.getStack().isVirtual() )
		{
			switch ( imp.getType() )
			{
			case ImagePlus.GRAY8:
				imgLoader = VirtualStackImageLoader.createUnsignedByteInstance( imp );
				break;
			case ImagePlus.GRAY16:
				imgLoader = VirtualStackImageLoader.createUnsignedShortInstance( imp );
				break;
			case ImagePlus.GRAY32:
				imgLoader = VirtualStackImageLoader.createFloatInstance( imp );
				break;
			case ImagePlus.COLOR_RGB:
			default:
				imgLoader = VirtualStackImageLoader.createARGBInstance( imp );
				break;
			}
		}
		else
		{
			switch ( imp.getType() )
			{
			case ImagePlus.GRAY8:
				imgLoader = BigWarpImageStackImageLoader.createUnsignedByteInstance( imp, ids );
				break;
			case ImagePlus.GRAY16:
				imgLoader = BigWarpImageStackImageLoader.createUnsignedShortInstance( imp, ids );
				break;
			case ImagePlus.GRAY32:
				imgLoader = BigWarpImageStackImageLoader.createFloatInstance( imp, ids );
				break;
			case ImagePlus.COLOR_RGB:
			default:
				imgLoader = BigWarpImageStackImageLoader.createARGBInstance( imp, ids );
				break;
			}
		}
		final File basePath = new File( "." );

		// create setups from channels
		final HashMap< Integer, BasicViewSetup > setups = new HashMap< Integer, BasicViewSetup >( numSetups );
		for ( int s = 0; s < numSetups; ++s )
		{
			final BasicViewSetup setup = new BasicViewSetup( ids[ s ], String.format( "channel %d", ids[ s ] + 1 ), size, voxelSize );
			setup.setAttribute( new Channel( ids[ s ] + 1 ) );
			setups.put( ids[ s ], setup );
		}

		// create timepoints
		final ArrayList< TimePoint > timepoints = new ArrayList< TimePoint >( numTimepoints );
		for ( int t = 0; t < numTimepoints; ++t )
			timepoints.add( new TimePoint( t ) );

		// create ViewRegistrations from the images calibration
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceTransform.set( pw, 0, 0, 0, 0, ph, 0, 0, 0, 0, pd, 0 );
		final ArrayList< ViewRegistration > registrations = new ArrayList< ViewRegistration >();
		for ( int t = 0; t < numTimepoints; ++t )
			for ( int s = 0; s < numSetups; ++s )
				registrations.add( new ViewRegistration( t, ids[ s ], sourceTransform ) );

		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( new TimePoints( timepoints ), setups, imgLoader, null );

		SpimDataMinimal spimData = new SpimDataMinimal( basePath, seq, new ViewRegistrations( registrations ) );
		if ( WrapBasicImgLoader.wrapImgLoaderIfNecessary( spimData ) )
			System.err.println( "WARNING:\nOpening <SpimData> dataset that is not suited for interactive browsing.\nConsider resaving as HDF5 for better performance." );

		return spimData;
	}

	public static int[] range( int start, int length )
	{
		int[] out = new int[ length ];
		for ( int i = 0; i < length; i++ )
			out[ i ] = start + i;

		return out;
	}

	public static int[] value( int value, int length )
	{
		int[] out = new int[ length ];
		Arrays.fill( out, value );
		return out;
	}
}
