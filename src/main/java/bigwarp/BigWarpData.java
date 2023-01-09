package bigwarp;

import bigwarp.source.SourceInfo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import bdv.cache.CacheControl;
import bdv.gui.BigWarpViewerFrame;
import bdv.img.WarpedSource;
import bdv.tools.InitializeViewerState;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.transformation.TransformedSource;
import bdv.util.Bounds;
import bdv.viewer.ConverterSetups;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SynchronizedViewerState;
import bdv.viewer.VisibilityAndGrouping;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.InvertibleWrapped2DTransformAs3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.Wrapped2DTransformAs3D;

public class BigWarpData< T >
{
	public List< SourceAndConverter< T > > sources;

	public final LinkedHashMap< Integer, SourceInfo > sourceInfos = new LinkedHashMap<>();
	public final List< ConverterSetup > converterSetups;

	public final CacheControl cache;

	public BigWarpData()
	{
		this( new ArrayList<>(), new ArrayList<>(), null );
	}

	public BigWarpData( final List< SourceAndConverter< T > > sources, final List< ConverterSetup > converterSetups, 
			final CacheControl cache, 
			final int[] movingIndexes, 
			final int[] targetIndexes )
	{
		this( sources, converterSetups, cache,
				listOf( movingIndexes ),
				listOf( targetIndexes ));
	}

	public BigWarpData( final List< SourceAndConverter< T > > sources,
			final List< ConverterSetup > converterSetups,
			final CacheControl cache )
	{
		this( sources, null, converterSetups, cache );
	}
	
	public BigWarpData( final List< SourceAndConverter< T > > sources,
			final List< RealTransform > transforms,
			final List< ConverterSetup > converterSetups,
			final CacheControl cache )
	{
		this.sources = sources;
		this.converterSetups = converterSetups;

		if ( cache == null )
			this.cache = new CacheControl.Dummy();
		else
			this.cache = cache;
	}

	public BigWarpData( final List< SourceAndConverter< T > > sources, final List< ConverterSetup > converterSetups, 
			final CacheControl cache, 
			final List<Integer> movingIndexes, 
			final List<Integer> targetIndexes )
	{
		this.sources = sources;
		this.converterSetups = converterSetups;

		for ( int i = 0; i < sources.size(); i++ )
		{
			final SourceAndConverter< T > sourceAndConverter = sources.get( i );
			final SourceInfo sourceInfo = new SourceInfo( i, movingIndexes.contains( i ), sourceAndConverter.getSpimSource().getName() );
			sourceInfo.setSourceAndConverter( sourceAndConverter );
			sourceInfos.put( i, sourceInfo );
		}

		if ( cache == null )
			this.cache = new CacheControl.Dummy();
		else
			this.cache = cache;
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
		final AtomicInteger movingCount = new AtomicInteger();
		sourceInfos.forEach( (id, info) -> {
			if (info.isMoving()) movingCount.incrementAndGet();
		} );
		return movingCount.get();
	}

	@SuppressWarnings( "unchecked" )
	public SourceAndConverter< T > getMovingSource( int i )
	{
		int curIdx = 0;
		for ( final Map.Entry< Integer, SourceInfo > idToInfo : sourceInfos.entrySet() )
		{
			final SourceInfo info = idToInfo.getValue();
			if (info.isMoving()) {
				if (curIdx == i) return ( SourceAndConverter< T > ) info.getSourceAndConverter();
				curIdx++;
			}
		}
		return null;
	}

	public int numTargetSources()
	{
		return sourceInfos.size() - numMovingSources();
	}

	public List< Integer > getMovingSourceIndices() {
		final ArrayList<Integer> indices = new ArrayList<>();
		int idx = 0;
		for ( final SourceInfo sourceInfo : sourceInfos.values() )
		{
			if ( sourceInfo.isMoving()){
				indices.add( idx );
			}
			idx++;
		}
		return indices;
	}

	public SourceAndConverter< T > getTargetSource( int i )
	{
		int curIdx = 0;
		for ( final Map.Entry< Integer, SourceInfo > idToInfo : sourceInfos.entrySet() )
		{
			final SourceInfo info = idToInfo.getValue();
			if (!info.isMoving()) {
				if (curIdx == i) return ( SourceAndConverter< T > ) info.getSourceAndConverter();
				curIdx++;
			}
		}
		return null;
	}
	
	public List<ConverterSetup> getMovingConverterSetups()
	{
		final ArrayList<ConverterSetup> out = new ArrayList<>();

		final SourceInfo[] infos = sourceInfos.values().toArray(new SourceInfo[]{} );
		for ( int i = 0; i < infos.length; i++ )
		{
			final SourceInfo info = infos[ i ];
			if (info.isMoving()) {
				out.add( converterSetups.get( i ) );
			}
		}
		return out;
	}
	
	public List<ConverterSetup> getTargetConverterSetups()
	{
		final ArrayList<ConverterSetup> out = new ArrayList<>();

		final SourceInfo[] infos = sourceInfos.values().toArray(new SourceInfo[]{} );
		for ( int i = 0; i < infos.length; i++ )
		{
			final SourceInfo info = infos[ i ];
			if (!info.isMoving()) {
				out.add( converterSetups.get( i ) );
			}
		}
		return out;
	}

	public SourceInfo getSourceInfo( int id )
	{
		return sourceInfos.get( id );
	}

	public SourceInfo getSourceInfo( SourceAndConverter< ? > sac )
	{
		for ( final SourceInfo info : sourceInfos.values() )
		{
			if (info.getSourceAndConverter() == sac) {
				return info;
			}
		}
		return null;
	}

	public boolean isMoving( SourceAndConverter<?> sac ) {
		return getSourceInfo( sac ).isMoving();
	}

	/**
	 * @Deprecated only handled maintaing the moving lists, now a no-op
	 */
	@Deprecated
	public void wrapUp()
	{
	}

	void addSource( Source<T> src, boolean isMoving )
	{
		addSource( src, isMoving, null );
	}

	void addSource( Source<T> src, boolean isMoving, RealTransform transform )
	{
		// find an unused id
		int id = 0;
		for( ConverterSetup cs : converterSetups )
		{
			if( id == cs.getSetupId() )
				id++;
		}
		addSource( id, src, isMoving, transform );
	}

	/**
	 * Adds a {@link Source} with the given id. Does not check that the id is unused.
	 *
	 * @param id the id
	 * @param src the source
	 * @param isMoving if the source is moving
	 * @param transform an optional transformation
	 */
	void addSource( final int id, Source<T> src, boolean isMoving, RealTransform transform )
	{
		BigWarpInit.add( this, src, id, 0, isMoving, transform );
		final SourceInfo sourceInfo = new SourceInfo( id, isMoving, src.getName(), null, transform );
		sourceInfo.setSourceAndConverter( sources.get( sources.size() -1 ) );
		sourceInfos.put( id, sourceInfo);
	}

	int remove( SourceInfo sourceInfo)
	{
		final int idx = sources.indexOf( sourceInfo.getSourceAndConverter() );
		remove( idx );
		return idx;
	}

	void remove( int i )
	{
		SourceAndConverter< T > sac = sources.get( i );
		final int sacId = sourceInfos.entrySet().stream().filter( it -> it.getValue().getSourceAndConverter() == sac ).map( Map.Entry::getKey ).findFirst().get();
		final SourceInfo sourceInfo = sourceInfos.remove( sacId );
		sources.remove( i );
		converterSetups.remove( i  );
	}

	public void applyTransformations()
	{
		int i = 0;
		for ( final SourceAndConverter<T> sac : sources )
		{
			final SourceInfo info = getSourceInfo( sac );
			final RealTransform transform = info.getTransform();
			if ( transform != null )
			{
				SourceAndConverter<T> newSac = inheritConverter(
						applyFixedTransform( sac.getSpimSource(), transform),
						sac );

				info.setSourceAndConverter( newSac );
				sources.set( i, newSac );
			}
			i++;
		}
	}

	public static < T > SourceAndConverter< T > inheritConverter( final Source<T> src, final SourceAndConverter< T > sac )
	{
		if ( sac.asVolatile() == null ) {
			return new SourceAndConverter< T >( src, sac.getConverter(), null );
		}
		else
		{
			System.out.println( "Inherit Converter needs to handle volatile");
//			inheritConverter( src, sac );
			return null;
//			return new SourceAndConverter< T >( src, sac.getConverter(), wrapSourceAsTransformed( src, name + "_vol", ndims ) );
		}
	}

	/**
	 * Returns a source that applies the given {@link RealTransform} to the given {@link Source}.
	 * <p>
	 * The returned source will be a new instance than unless the transform
	 * is a instance of {@link AffineGet} and source is an instance of {@link TransformedSource}.
	 * 
	 * @param <T> the type
	 * @param src the original source
	 * @param transform the transformation 
	 * @return the transformed source
	 */
	public <T> Source<T> applyFixedTransform( final Source<T> src, final RealTransform transform )
	{
		RealTransform tform = transform;
		if( transform.numSourceDimensions() < 3 )
		{
			if( transform instanceof InvertibleRealTransform )
				tform = new Wrapped2DTransformAs3D( transform );
			else
				tform = new InvertibleWrapped2DTransformAs3D( ( InvertibleRealTransform ) transform );
		}

		if( transform instanceof AffineGet )
		{
			// can use TransformedSource
			final AffineTransform3D affine3d;
			if( transform instanceof AffineTransform3D )
				affine3d = ( AffineTransform3D ) transform;
			else
			{
				affine3d = new AffineTransform3D();
				affine3d.preConcatenate( ( AffineGet ) transform );
			}

			// could perhaps try to be clever if its a warped source (?), maybe later
			TransformedSource<?> tsrc;
			if ( src instanceof TransformedSource )
			{
				tsrc = ( TransformedSource ) ( src );
			}
			else
			{
				tsrc = new TransformedSource( src );
			}
			tsrc.setFixedTransform( affine3d );
			return ( Source< T > ) tsrc;
		}
		else
		{
			// need to use WarpedSource
			final WarpedSource<?> wsrc = new WarpedSource( src, src.getName() );
			wsrc.updateTransform( tform );
			wsrc.setIsTransformed( true );
			return ( Source< T > ) wsrc;
		}
	}

	/**
	 * Updates the moving sources' transformation with the transform currently
	 * being edited by BigWarp.
	 * 
	 * @param transform the transformation
	 */
	public void updateEditableTransformation( RealTransform transform )
	{
		for ( final SourceInfo sourceInfo : sourceInfos.values() )
		{
			// could be extra careful and ensure the source is a WarpedSource, but hopefully not necessary
			if ( sourceInfo.isMoving() )
			{
				final SourceAndConverter< ? > sac = sourceInfo.getSourceAndConverter();
				final WarpedSource< ? > wsrc = ( WarpedSource< ? > ) sac.getSpimSource();
				wsrc.updateTransform( transform );
				if ( sac.asVolatile() != null )
					( ( WarpedSource< ? > ) sourceInfo.getSourceAndConverter().asVolatile().getSpimSource() ).updateTransform( transform );

				wsrc.updateTransform( transform );

				/*
				 * There was a time when I had a single WarpedSource manage a RealTransformSequence
				 * instead of a WarpedSource wrapping a different WarpedSource as I'm doing now.
				 * 
				 * But I decided against having a single source because warped sources can toggle their transforms.
				 * That toggling makes sense for the editable transform, but the fixex should be "on" 
				 * always, and therefore be handled by either a TransformedSource or a different
				 * WarpedSource instance.
				 */
			}
		}
	}

	public void transferChannelSettings( final BigWarpViewerFrame viewer )
	{
		final SynchronizedViewerState state = viewer.getViewerPanel().state();
		final ConverterSetups setups = viewer.getConverterSetups();

		for( Entry< Integer, SourceInfo > infoEntry : sourceInfos.entrySet() )
		{
			final int id = infoEntry.getKey();
			final SourceInfo info = infoEntry.getValue();
			final SourceAndConverter< ? > sac = info.getSourceAndConverter();
			final ConverterSetup cs = setups.getConverterSetup( sac );

			if ( info.getColorSettings() == null )
			{
				final int timepoint = state.getCurrentTimepoint();
				final Bounds bounds = InitializeViewerState.estimateSourceRange( sac.getSpimSource(), timepoint, 0.001, 0.999 );
				if( cs != null )
					cs.setDisplayRange( bounds.getMinBound(), bounds.getMaxBound() );
			}
			else
			{
				info.getColorSettings().updateSetup( cs );
			}
		}
	}
}