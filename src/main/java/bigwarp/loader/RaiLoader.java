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
import java.util.List;

import bdv.img.virtualstack.VirtualStackImageLoader;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.WrapBasicImgLoader;
import ij.ImagePlus;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.generic.sequence.TypedBasicImgLoader;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.util.Util;

/**
 *
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public class RaiLoader<T> implements BasicImgLoader, TypedBasicImgLoader< T >, Loader
{
	private List<RandomAccessibleInterval< T >> raiList;
	private HashMap<Integer, BasicSetupImgLoader> setupImageLoaderMap;
	private int[] ids;

	public RaiLoader( final List<RandomAccessibleInterval< T >> raiList, int[] ids )
	{
		this.raiList = raiList;
		this.ids = ids;
		setupImageLoaderMap = new HashMap< Integer, BasicSetupImgLoader >();
		
		for( int i = 0; i < ids.length; i++ )
		{
			setupImageLoaderMap.put( ids[ i ], new MySetupImgLoader( ids[ i ], raiList.get( i )));
		}
	}

	public SpimDataMinimal[] load()
	{
		final File basePath = new File( "." );

		// create setups from channels
		final HashMap< Integer, BasicViewSetup > setups = new HashMap< Integer, BasicViewSetup >( 1 );


		FinalVoxelDimensions voxdims = new FinalVoxelDimensions( "um", 1, 1, 1 );
		for ( int s = 0; s < raiList.size(); ++s )
		{
			final BasicViewSetup setup = new BasicViewSetup( ids[s], String.format( "channel %d", ids[s] ), raiList.get( s ), voxdims );
			setup.setAttribute( new Channel( ids[s] ) );
			setups.put( ids[s], setup );
		}

		// create timepoints
		final ArrayList< TimePoint > timepoints = new ArrayList< TimePoint >( 1 );
		for ( int t = 0; t < 1; ++t )
			timepoints.add( new TimePoint( t ) );

		// create ViewRegistrations from the images calibration
		final AffineTransform3D sourceTransform = new AffineTransform3D();
//		sourceTransform.set( 
//				voxdims.dimension( 0 ), 0, 0, 0, 
//				0, voxdims.dimension( 1 ), 0, 0, 
//				0, 0, voxdims.dimension( 2 ), 0 );

		final ArrayList< ViewRegistration > registrations = new ArrayList< ViewRegistration >();
		for ( int t = 0; t < 1; ++t )
			for ( int s = 0; s < 1; ++s )
				registrations.add( new ViewRegistration( t, ids[s], sourceTransform ) );

		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( 
				new TimePoints( timepoints ), setups, this, null );

		SpimDataMinimal spimData = new SpimDataMinimal( basePath, seq, new ViewRegistrations( registrations ) );
		if ( WrapBasicImgLoader.wrapImgLoaderIfNecessary( spimData ) )
			System.err.println( "WARNING:\nOpening <SpimData> dataset that is not suited for interactive browsing.\nConsider resaving as HDF5 for better performance." );

		return new SpimDataMinimal[]{ spimData };
	}

	@Override
	public BasicSetupImgLoader< T > getSetupImgLoader( int setupId )
	{
		return setupImageLoaderMap.get( setupId );
	}
	
	public class MySetupImgLoader implements BasicSetupImgLoader< T >
	{
		int id;
		RandomAccessibleInterval<T> rai;
		public MySetupImgLoader( int id, RandomAccessibleInterval<T> rai )
		{
			this.id = id;
			this.rai = rai;
		}

		@Override
		public RandomAccessibleInterval< T > getImage( final int timepointId, final ImgLoaderHint... hints )
		{
			return rai;
		}

		@Override
		public T getImageType()
		{
			return Util.getTypeFromInterval( rai );
		}
	}	

}
