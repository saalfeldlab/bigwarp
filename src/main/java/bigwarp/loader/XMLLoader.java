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

import java.util.List;
import java.util.ArrayList;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;

/**
 *
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public class XMLLoader implements Loader
{
	final private String xmlPath;

	private List<String> names;

	private int numSources = 0;

	public XMLLoader( final String xmlPath )
	{
		this.xmlPath = xmlPath;
	}

	@Override
	public String name( final int i )
	{
		assert( i <= numSources );
		return names.get( i );
	}

	@Override
	public int numSources()
	{
		return numSources;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public SpimDataMinimal[] load()
	{
		names = new ArrayList<String>();
		SpimDataMinimal spimData = null;
		try
		{
			spimData = new XmlIoSpimDataMinimal().load( xmlPath );

			numSources = spimData.getSequenceDescription().getViewSetupsOrdered().size();
			for( BasicViewSetup s : spimData.getSequenceDescription().getViewSetupsOrdered())
			{
				names.add( s.getName());
			}

			if ( WrapBasicImgLoader.wrapImgLoaderIfNecessary( spimData ) )
			{
				System.err.println( "WARNING:\nOpening <SpimData> dataset that is not suited for interactive browsing.\nConsider resaving as HDF5 for better performance." );
			}

		}
		catch ( final SpimDataException e )
		{
			e.printStackTrace();
		}

		return new SpimDataMinimal[]{ spimData };
	}
}
