package bigwarp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import bdv.cache.CacheControl;
import bdv.gui.BigWarpViewerFrame;
import bdv.tools.InitializeViewerState;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.util.Bounds;
import bdv.viewer.ConverterSetups;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SynchronizedViewerState;
import bdv.viewer.VisibilityAndGrouping;
import bigwarp.loader.ImagePlusLoader.ColorSettings;

public class BigWarpData< T >
{
	// TODO JOHN CHECK ME
	public List< SourceAndConverter< T > > sources;

	public final List< ConverterSetup > converterSetups;

	public final CacheControl cache;

//	public int[] movingSourceIndices;
//
//	public int[] targetSourceIndices;

	public final List< Integer > movingSourceIndexList;

	public final List< Integer > targetSourceIndexList;
	
	public final HashMap< SourceAndConverter<T>, Boolean > isMovingMap;

	public final HashMap< Integer, ColorSettings > setupSettings;

	public final HashMap< SourceAndConverter<?>, ColorSettings > sourceColorSettings;

	public BigWarpData( final List< SourceAndConverter< T > > sources, final List< ConverterSetup > converterSetups, 
			final CacheControl cache, 
			final int[] movingIndexes, 
			final int[] targetIndexes )
	{
		this( sources, converterSetups, cache,
				listOf( movingIndexes ),
				listOf( targetIndexes ));
	}
	
	public BigWarpData( final List< SourceAndConverter< T > > sources, final List< ConverterSetup > converterSetups, 
			final CacheControl cache )
	{
		this.sources = sources;
		this.converterSetups = converterSetups;

		this.movingSourceIndexList = new ArrayList<>();
		this.targetSourceIndexList = new ArrayList<>();
		isMovingMap = new HashMap<>();

		if ( cache == null )
			this.cache = new CacheControl.Dummy();
		else
			this.cache = cache;

		setupSettings = new HashMap<>();
		sourceColorSettings = new HashMap<>();
	}

	public BigWarpData( final List< SourceAndConverter< T > > sources, final List< ConverterSetup > converterSetups, 
			final CacheControl cache, 
			final List<Integer> movingIndexes, 
			final List<Integer> targetIndexes )
	{
		this.sources = sources;
		this.converterSetups = converterSetups;
//		this.movingSourceIndices = movingSourceIndices;
//		this.targetSourceIndices = targetSourceIndices;

		this.movingSourceIndexList = movingIndexes;
		this.targetSourceIndexList = targetIndexes;
		isMovingMap = new HashMap<>();
		updateIsMovingMap();

		if ( cache == null )
			this.cache = new CacheControl.Dummy();
		else
			this.cache = cache;

		setupSettings = new HashMap<>();
		sourceColorSettings = new HashMap<>();
	}
	
	private void updateIsMovingMap()
	{
		isMovingMap.clear();
		for( Integer i : movingSourceIndexList )
			isMovingMap.put( sources.get( i ), true );

		for( Integer i : targetSourceIndexList )
			isMovingMap.put( sources.get( i ), false );	
	}

	private static ArrayList<Integer> listOf( int[] x )
	{
		final ArrayList< Integer > out = new ArrayList<Integer>();
		for( int i : x )
			out.add( i );

		return out;
	}

	public int numMovingSources()
	{
		return movingSourceIndexList.size();
	}

	public SourceAndConverter< T > getMovingSource( int i )
	{
		return sources.get( movingSourceIndexList.get( i ) );
	}

	public int numTargetSources()
	{
		return targetSourceIndexList.size();
	}

	public SourceAndConverter< T > getTargetSource( int i )
	{
		return sources.get( targetSourceIndexList.get( i ) );
	}
	
	public List<ConverterSetup> getMovingConverterSetups()
	{
		final ArrayList<ConverterSetup> out = new ArrayList<>();
		for( int i : movingSourceIndexList )
			out.add( converterSetups.get( i ) );

		return out;
	}
	
	public List<ConverterSetup> getTargetConverterSetups()
	{
		final ArrayList<ConverterSetup> out = new ArrayList<>();
		for( int i : targetSourceIndexList )
			out.add( converterSetups.get( i ) );

		return out;
	}

	public boolean isMoving( SourceAndConverter<?> sac ) {
		if( !isMovingMap.containsKey( sac ))
			return false;
		else
			return isMovingMap.get( sac );
	}

	public void wrapUp()
	{
//		movingSourceIndices = movingSourceIndexList.stream().mapToInt( x -> x ).toArray();
//		targetSourceIndices = targetSourceIndexList.stream().mapToInt( x -> x ).toArray();
//
//		Arrays.sort( movingSourceIndices );
//		Arrays.sort( targetSourceIndices );
		Collections.sort( movingSourceIndexList );
		Collections.sort( targetSourceIndexList );
	}

	/**
	 * @deprecated
	 */
	public void transferChannelSettings( final SetupAssignments setupAssignments, final VisibilityAndGrouping visibility )
	{
		for( Integer key : setupSettings.keySet() )
			setupSettings.get( key ).updateSetup( setupAssignments );
	}

	public void transferChannelSettings( final BigWarpViewerFrame viewer )
	{
		SynchronizedViewerState state = viewer.getViewerPanel().state();
		ConverterSetups setups = viewer.getConverterSetups();
		synchronized ( state )
		{
			for ( SourceAndConverter< ? > sac : state.getSources() )
			{
				if ( sourceColorSettings.containsKey( sac ) )
				{
					if ( sourceColorSettings.get( sac ) == null )
						continue;

					System.out.println( setups.getConverterSetup( sac ) );
					sourceColorSettings.get( sac ).updateSetup( setups.getConverterSetup( sac ) );
				}
				else
				{
					final int timepoint = state.getCurrentTimepoint();	
					final Bounds bounds = InitializeViewerState.estimateSourceRange( sac.getSpimSource(), timepoint, 0.001, 0.999 );
					ConverterSetup cs = setups.getConverterSetup(sac);
					if( cs != null )
						cs.setDisplayRange( bounds.getMinBound(), bounds.getMaxBound() );
				}
			}
		}
	}
}