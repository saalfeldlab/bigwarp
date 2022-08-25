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

import bdv.img.imagestack.ImageStackImageLoader;
import bdv.img.virtualstack.VirtualStackImageLoader;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp.BigWarpData;
import ij.ImagePlus;
import ij.process.LUT;
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
import net.imglib2.type.numeric.ARGBType;

/**
 *
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public class ImagePlusLoader implements Loader
{
	final private ImagePlus[] impList;

	final private int numSources;

	private int index; // keep track of the setupIds given to each source

	private boolean is3d;
	private boolean isMultiChannel;
	private boolean[] isComposite;

	private HashMap< Integer, ColorSettings > settingsMap;

	private final String[] names;

	public ImagePlusLoader( final ImagePlus imp )
	{
		this( new ImagePlus[]{ imp } );
	}

	public ImagePlusLoader( final ImagePlus[] impList )
	{
		this.impList = impList;
		int nc = 0;
		isComposite = new boolean[ impList.length ];

		for( ImagePlus ip : impList )
		{
			nc += ip.getNChannels();
		}

		names = new String[ nc ];
		int k = 0;
		for( ImagePlus ip : impList )
		{
			for( int i = 0; i < ip.getNChannels(); i++ )
			{
				names[ k++ ] = ip.getTitle() + String.format("_ch-%d", i );
			}
		}

		numSources = nc;
		settingsMap = new HashMap<>();
	}

	/*
	 * Use numSources 
	 */
	@Deprecated
	public int numChannels()
	{
		return numSources;
	}

	@Override
	public int numSources()
	{
		return numSources;
	}

	@Override
	public String name( final int i )
	{
		assert( i < numSources );

		return names[ i ];
	}

	public boolean is3d()
	{
		return is3d;
	}

	public boolean isMultiChannel()
	{
		return isMultiChannel;
	}

	public HashMap< Integer, ColorSettings > getSetupSettings()
	{
		return settingsMap;
	}

	public void update( final BigWarpData< ? > data )
	{
		for( Integer key : settingsMap.keySet() )
		{
			SourceAndConverter<?> sac = data.sources.get( key.intValue() );
			data.setupSettings.put( key, settingsMap.get( key ) );
			data.sourceColorSettings.put( sac, settingsMap.get( key ));
		}
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public SpimDataMinimal[] load()
	{
		return loadAll( 0 );
	}

	public SpimDataMinimal[] loadAll( int startid )
	{
		SpimDataMinimal[] out = new SpimDataMinimal[ impList.length ];
		index = startid;
		for( int i = 0; i < impList.length; i++ )
		{
			out[ i ] = load( index, impList[ i ] );
			index += impList[ i ].getNChannels();
		}
		return out;
	}

	public static double sanitizeCalibration( double in, String dim )
	{
		if( Double.isNaN( in ) || Double.isInfinite( in ))
		{
			System.err.println("WARNING: Check image calibration. dimension " + dim + " was " + in + " changing to 1.0");
			return 1.0;
		}
		else
			return in;
	}

	public SpimDataMinimal load( final int setupIdOffset, ImagePlus imp )
	{
		// get calibration and image size
		final double pw;
		final double ph;
		final double pd;

		pw = sanitizeCalibration( imp.getCalibration().pixelWidth, "x" );
		ph = sanitizeCalibration( imp.getCalibration().pixelHeight, "y" );
		pd = sanitizeCalibration( imp.getCalibration().pixelDepth, "z" );

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
				imgLoader = VirtualStackImageLoader.createUnsignedByteInstance( imp, setupIdOffset );
				break;
			case ImagePlus.GRAY16:
				imgLoader = VirtualStackImageLoader.createUnsignedShortInstance( imp, setupIdOffset );
				break;
			case ImagePlus.GRAY32:
				imgLoader = VirtualStackImageLoader.createFloatInstance( imp, setupIdOffset );
				break;
			case ImagePlus.COLOR_RGB:
			default:
				imgLoader = VirtualStackImageLoader.createARGBInstance( imp, setupIdOffset );
				break;
			}
		}
		else
		{
			switch ( imp.getType() )
			{
			case ImagePlus.GRAY8:
				imgLoader = ImageStackImageLoader.createUnsignedByteInstance( imp, setupIdOffset );
				break;
			case ImagePlus.GRAY16:
				imgLoader = ImageStackImageLoader.createUnsignedShortInstance( imp, setupIdOffset );
				break;
			case ImagePlus.GRAY32:
				imgLoader = ImageStackImageLoader.createFloatInstance( imp, setupIdOffset );
				break;
			case ImagePlus.COLOR_RGB:
			default:
				imgLoader = ImageStackImageLoader.createARGBInstance( imp, setupIdOffset );
				break;
			}
		}
		final File basePath = new File( "." );

		// create setups from channels
		final HashMap< Integer, BasicViewSetup > setups = new HashMap< Integer, BasicViewSetup >( numSetups );
		for ( int s = 0; s < numSetups; ++s )
		{
			final int id = setupIdOffset + s;
			final BasicViewSetup setup = new BasicViewSetup( setupIdOffset + s, String.format( "%s channel %d", imp.getTitle(), id + 1 ), size, voxelSize );
			setup.setAttribute( new Channel( id + 1 ) );
			setups.put( id, setup );

			settingsMap.put( id, ColorSettings.fromImagePlus( imp, id, s ));
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
				registrations.add( new ViewRegistration( t, setupIdOffset + s, sourceTransform ) );

		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( new TimePoints( timepoints ), setups, imgLoader, null );

		SpimDataMinimal spimData = new SpimDataMinimal( basePath, seq, new ViewRegistrations( registrations ) );
		WrapBasicImgLoader.wrapImgLoaderIfNecessary( spimData );
//		if ( WrapBasicImgLoader.wrapImgLoaderIfNecessary( spimData ) )
//			System.err.println( "WARNING:\nOpening <SpimData> dataset that is not suited for interactive browsing.\nConsider resaving as HDF5 for better performance." );

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

	public static class ColorSettings
	{
		public final int converterSetupIndex; // index into the ConverterSetups list that this corresponds to
		public final double min;
		public final double max;
		public final ARGBType color;

		public ColorSettings( int converterSetupIndex, double min, double max, ARGBType color)
		{
			this.converterSetupIndex = converterSetupIndex;
			this.min = min;
			this.max = max;
			this.color = color;
		}

		/**
		 * @deprecated
		 */
		public void updateSetup( final SetupAssignments setups )
		{
			updateSetup( setups.getConverterSetups().get( converterSetupIndex ) );
		}

		public void updateSetup( final ConverterSetup setup )
		{
			setup.setDisplayRange( min, max );
			if( color != null )
				setup.setColor( color );
		}

		public static ColorSettings fromImagePlus( final ImagePlus imp, int converterSetupIndex, int channelOffset )
		{
			double min = imp.getDisplayRangeMin();
			double max = imp.getDisplayRangeMax();

			ARGBType color = null;
			LUT[] luts = imp.getLuts();

			// see
			// https://forum.image.sc/t/issue-using-big-warp-on-tiff-file/31163
			if ( luts !=null && channelOffset < luts.length )
			{
				color = new ARGBType( luts[ channelOffset ].getRGB( 255 ) );
				min = luts[ channelOffset ].min;
				max = luts[ channelOffset ].max;
			}

			return new ColorSettings( converterSetupIndex, min, max, color );
		}
	}
}
