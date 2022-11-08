package bigwarp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

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
import bigwarp.loader.ImagePlusLoader.ColorSettings;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.InvertibleWrapped2DTransformAs3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.Wrapped2DTransformAs3D;

public class BigWarpData< T >
{
	// TODO JOHN CHECK ME
	public List< SourceAndConverter< T > > sources;

	public List<RealTransform> transforms;

	public final List< ConverterSetup > converterSetups;

	public final CacheControl cache;

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

		if( transforms != null )
			this.transforms = transforms;
		else
		{
			// fill the initial transform list with nulls
			this.transforms = new ArrayList<>();
			IntStream.range( 0, sources.size() ).forEach(  i -> this.transforms.add( null ));
		}

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

	public void addSource( Source<T> src, boolean isMoving )
	{
		addSource( src, isMoving );
	}

	public void addSource( Source<T> src, boolean isMoving, RealTransform transform )
	{
		// find an unused id
		int id = 0;
		for( ConverterSetup cs : converterSetups )
		{
			if( id == cs.getSetupId() )
				id++;
		}
		BigWarpInit.add( this, src, id, 0, isMoving, transform );
	}

	public void addSourceAndConverter( SourceAndConverter<T> sac, boolean isMoving )
	{
		addSourceAndConverter( sac, isMoving, null );
	}

	public void addSourceAndConverter( SourceAndConverter<T> sac, boolean isMoving, RealTransform transform )
	{
		sources.add( sac );
		isMovingMap.put( sac, isMoving );
		transforms.add( transform ); // transform may be null

		if( isMoving )
			movingSourceIndexList.add( sources.size() - 1 );
		else
			targetSourceIndexList.add( sources.size() - 1 );
	}

	public void setTransform( int i, RealTransform transform )
	{
		transforms.set( i, transform );
	}

	public void applyTransformations()
	{
		int i = 0;
		for ( final SourceAndConverter<T> sac : sources )
		{
			if ( transforms.get( i ) != null )
			{
				SourceAndConverter<T> newSac = inheritConverter(
						applyFixedTransform( sac.getSpimSource(), transforms.get( i )),
						sac );

				sourceColorSettings.put( newSac, sourceColorSettings.get( sac ));
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
			TransformedSource<?> tsrc;

			/*
			 * UNSURE WHAT TO DO IN THIS CASE
			 */
//			if( (src instanceof WarpedSource ))
//			{
//				Source< ? > wsrc = ( ( WarpedSource< ? > ) src ).getWrappedSource();
//			}

			final AffineTransform3D affine3d;
			if( transform instanceof AffineTransform3D )
				affine3d = ( AffineTransform3D ) transform;
			else
			{
				affine3d = new AffineTransform3D();
				affine3d.preConcatenate( ( AffineGet ) transform );
			}

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
			WarpedSource<?> wsrc;
			if( !(src instanceof WarpedSource ))
				wsrc = new WarpedSource( src, src.getName() );
			else
				wsrc = (WarpedSource<?>)src;

			wsrc.updateTransform( tform );
			wsrc.setIsTransformed( true );
			return ( Source< T > ) wsrc;
		}
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