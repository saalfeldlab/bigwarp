package bigwarp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import bdv.BigDataViewer;
import bdv.SpimSource;
import bdv.VolatileSpimSource;
import bdv.img.RenamableSource;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp.BigWarpData;
import bigwarp.loader.ImagePlusLoader;
import bigwarp.loader.Loader;
import bigwarp.loader.XMLLoader;
import ij.ImagePlus;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.display.ARGBARGBColorConverter;
import net.imglib2.display.ARGBtoRandomARGBColorConverter;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.display.ScaledARGBConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.util.Util;

public class BigWarpInit
{

	private static String createSetupName( final BasicViewSetup setup )
	{
		if ( setup.hasName() )
			return setup.getName();

		String name = "";

		final Angle angle = setup.getAttribute( Angle.class );
		if ( angle != null )
			name += ( name.isEmpty() ? "" : " " ) + "a " + angle.getName();

		final Channel channel = setup.getAttribute( Channel.class );
		if ( channel != null )
			name += ( name.isEmpty() ? "" : " " ) + "c " + channel.getName();

		return name;
	}

	public static void initSetupsARGBTypeRandom( final AbstractSpimData< ? > spimData, final ARGBType type, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< ? > > sources )
	{
		if ( spimData.getSequenceDescription().getImgLoader() instanceof WrapBasicImgLoader )
		{
			initSetupsARGBTypeNonVolatile( spimData, type, converterSetups, sources );
			return;
		}

		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
		{

			final int setupId = setup.getId();

			final String setupName = createSetupName( setup );
			final VolatileSpimSource< ARGBType, VolatileARGBType > vs = new VolatileSpimSource< ARGBType, VolatileARGBType >( spimData, setupId, setupName );
			final SpimSource< ARGBType > s = vs.nonVolatile();

			// Decorate each source with an extra transformation, that can be
			// edited manually in this viewer.
			final TransformedSource< VolatileARGBType > tvs = new TransformedSource< VolatileARGBType >( vs );
			final TransformedSource< ARGBType > ts = new TransformedSource< ARGBType >( s, tvs );

			final SourceAndConverter< ARGBType > soc;
			final SourceAndConverter< VolatileARGBType > vsoc;
			final ConverterSetup converterSetup;

			final ARGBtoRandomARGBColorConverter.ToGray converter = new ARGBtoRandomARGBColorConverter.ToGray( 0, 255 );
			final ARGBtoRandomARGBColorConverter.VolatileToGray vconverter = new ARGBtoRandomARGBColorConverter.VolatileToGray( 0, 255 );

			converterSetup = new RealARGBColorConverterSetup( setupId, converter, vconverter );
			vsoc = new SourceAndConverter< VolatileARGBType >( tvs, vconverter );
			soc = new SourceAndConverter< ARGBType >( ts, converter, vsoc );

			converterSetups.add( converterSetup );

			sources.add( soc );
		}
	}

	public static void initSetupsARGBType( final AbstractSpimData< ? > spimData, final ARGBType type, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< ? > > sources )
	{
		initSetupsARGBType( spimData, type, converterSetups, sources, true );
	}

	public static void initSetupsARGBType( final AbstractSpimData< ? > spimData, final ARGBType type, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< ? > > sources, final boolean grayConversion )
	{
		if ( spimData.getSequenceDescription().getImgLoader() instanceof WrapBasicImgLoader )
		{
			initSetupsARGBTypeNonVolatile( spimData, type, converterSetups, sources );
			return;
		}

		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
		{

			final int setupId = setup.getId();

			final String setupName = createSetupName( setup );
			final VolatileSpimSource< ARGBType, VolatileARGBType > vs = new VolatileSpimSource< ARGBType, VolatileARGBType >( spimData, setupId, setupName );
			final SpimSource< ARGBType > s = vs.nonVolatile();

			// Decorate each source with an extra transformation, that can be
			// edited manually in this viewer.
			final TransformedSource< VolatileARGBType > tvs = new TransformedSource< VolatileARGBType >( vs );
			final TransformedSource< ARGBType > ts = new TransformedSource< ARGBType >( s, tvs );

			final SourceAndConverter< ARGBType > soc;
			final SourceAndConverter< VolatileARGBType > vsoc;
			final ConverterSetup converterSetup;
			if ( grayConversion )
			{
				final ARGBARGBColorConverter.ToGray converter = new ARGBARGBColorConverter.ToGray( 0, 255 );
				final ARGBARGBColorConverter.VolatileToGray vconverter = new ARGBARGBColorConverter.VolatileToGray( 0, 255 );
//				converter = new ScaledARGBConverter.ARGB( 0, 255 );
//				vconverter = new ScaledARGBConverter.VolatileARGB( 0, 255 );

				converterSetup = new RealARGBColorConverterSetup( setupId, converter, vconverter );
				vsoc = new SourceAndConverter< VolatileARGBType >( tvs, vconverter );
				soc = new SourceAndConverter< ARGBType >( ts, converter, vsoc );

				converterSetups.add( converterSetup );
			}
			else
			{
				final Converter< VolatileARGBType, ARGBType > vconverter = new Converter< VolatileARGBType, ARGBType >()
				{
					@Override
					public void convert( final VolatileARGBType input, final ARGBType output )
					{
						output.set( input.get() );
					}
				};
				final Converter< ARGBType, ARGBType > converter = new Converter< ARGBType, ARGBType >()
				{
					@Override
					public void convert( final ARGBType input, final ARGBType output )
					{
						output.set( input.get() );
					}
				};

				vsoc = new SourceAndConverter< VolatileARGBType >( tvs, vconverter );
				soc = new SourceAndConverter< ARGBType >( ts, converter, vsoc );

			}

			sources.add( soc );
		}
	}

	private static void initSetupsARGBTypeNonVolatile( final AbstractSpimData< ? > spimData, final ARGBType type, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< ? > > sources )
	{
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
		{
			final ScaledARGBConverter.ARGB converter = new ScaledARGBConverter.ARGB( 0, 255 );

			final int setupId = setup.getId();
			final String setupName = createSetupName( setup );
			final SpimSource< ARGBType > s = new SpimSource< ARGBType >( spimData, setupId, setupName );

			// Decorate each source with an extra transformation, that can be
			// edited manually in this viewer.
			final TransformedSource< ARGBType > ts = new TransformedSource< ARGBType >( s );
			final SourceAndConverter< ARGBType > soc = new SourceAndConverter< ARGBType >( ts, converter );

			sources.add( soc );
			converterSetups.add( new RealARGBColorConverterSetup( setupId, converter ) );
		}
	}

	public static void initSetups( final AbstractSpimData< ? > spimData, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< ? > > sources )
	{
		BigDataViewer.initSetups( spimData, converterSetups, sources );
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static void initSetup( final Source< ? > src, final int setupId, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< ? > > sources )
	{
		Object type = src.getType();
		if ( RealType.class.isInstance( type ) )
		{
			initSourceReal( ( Source< RealType > ) src, setupId, converterSetups, sources );
		}
		else if ( ARGBType.class.isInstance( type ) )
		{
			initSourceARGB( ( Source< ARGBType > ) src, setupId, converterSetups, sources );
		}
		else
			throw new IllegalArgumentException( "Source of type " + type.getClass() + " no supported." );
	}

	public static void initSourceARGB( final Source< ARGBType > src, final int setupId, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< ? > > sources )
	{
		final SourceAndConverter< ARGBType > soc = new SourceAndConverter<>( src, null );

		final ScaledARGBConverter.VolatileARGB vconverter = new ScaledARGBConverter.VolatileARGB( 0, 255 );
		final ScaledARGBConverter.ARGB converter = new ScaledARGBConverter.ARGB( 0, 255 );

		sources.add( soc );
		converterSetups.add( new RealARGBColorConverterSetup( setupId, converter, vconverter ) );
	}

	public static < T extends RealType< T > > void initSourceReal( final Source< T > src, final int setupId, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< ? > > sources )
	{
		T type = src.getType();
		final double typeMin = Math.max( 0, Math.min( type.getMinValue(), 65535 ) );
		final double typeMax = Math.max( 0, Math.min( type.getMaxValue(), 65535 ) );
		final RealARGBColorConverter< T > converter = new RealARGBColorConverter.Imp1<>( typeMin, typeMax );
		converter.setColor( new ARGBType( 0xffffffff ) );

		final SourceAndConverter< T > soc = new SourceAndConverter<>( src, converter );

		sources.add( soc );
		converterSetups.add( new RealARGBColorConverterSetup( setupId, converter ) );
	}

	public static BigWarpData< ? > createBigWarpData( final AbstractSpimData< ? >[] spimDataPList, final AbstractSpimData< ? >[] spimDataQList )
	{
		return createBigWarpData( spimDataPList, spimDataQList, null );
	}

	/**
	 * Create {@link BigWarpData} from two {@link AbstractSpimData}.
	 *
	 * @param spimDataPList
	 *            array of moving SpimData
	 * @param spimDataQList
	 *            array of fixed SpimData
	 * @param names
	 *            array of source names
	 * @return BigWarpData
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public static BigWarpData< ? > createBigWarpData( final Source< ? >[] movingSourceList, final Source< ? >[] fixedSourceList, String[] names )
	{

		BigWarpData data = initData();

		int setupId = 0;
		for ( Source< ? > mvgSource : movingSourceList )
		{
			add( data, mvgSource, setupId++, 1, true );
		}

		for ( Source< ? > fxdSource : fixedSourceList )
		{
			add( data, fxdSource, setupId, 1, false );
		}

		data.wrapUp();

		if ( names != null ) { return new BigWarpData( wrapSourcesAsRenamable( data.sources, names ), data.converterSetups, data.cache, data.movingSourceIndices, data.targetSourceIndices ); }

		return data;
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static < T > BigWarpData< ? > add( BigWarpData bwdata, Source< T > src, int setupId, int numTimepoints, boolean isMoving )
	{
		addSourceToListsGenericType( src, setupId, numTimepoints, src.getType(), bwdata.converterSetups, bwdata.sources );

		int N = bwdata.sources.size();
		if ( isMoving )
			bwdata.movingSourceIndexList.add( N - 1 );
		else
			bwdata.targetSourceIndexList.add( N - 1 );

		return bwdata;
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public static < T > BigWarpData< T > initData()
	{
		final ArrayList< ConverterSetup > converterSetups = new ArrayList< ConverterSetup >();
		final ArrayList< SourceAndConverter< T > > sources = new ArrayList< SourceAndConverter< T > >();

		return new BigWarpData( sources, converterSetups, null, null, null );
	}

	/**
	 * Add the given {@code source} to the lists of {@code converterSetups}
	 * (using specified {@code setupId}) and {@code sources}. For this, the
	 * {@code source} is wrapped with an appropriate {@link Converter} to
	 * {@link ARGBType} and into a {@link TransformedSource}.
	 *
	 * @param source
	 *            source to add.
	 * @param setupId
	 *            id of the new source for use in {@link SetupAssignments}.
	 * @param numTimepoints
	 *            the number of timepoints of the source.
	 * @param type
	 *            instance of the {@code img} type.
	 * @param converterSetups
	 *            list of {@link ConverterSetup}s to which the source should be
	 *            added.
	 * @param sources
	 *            list of {@link SourceAndConverter}s to which the source should
	 *            be added.
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	private static < T > void addSourceToListsGenericType( final Source< T > source, final int setupId, final int numTimepoints, final T type, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< T > > sources )
	{
		if ( type instanceof RealType )
			addSourceToListsRealType( ( Source ) source, setupId, ( List ) converterSetups, ( List ) sources );
		else if ( type instanceof ARGBType )
			addSourceToListsARGBType( ( Source ) source, setupId, ( List ) converterSetups, ( List ) sources );
		else if ( type instanceof VolatileARGBType )
			addSourceToListsVolatileARGBType( ( Source ) source, setupId, ( List ) converterSetups, ( List ) sources );
		else
			throw new IllegalArgumentException( "Unknown source type. Expected RealType, ARGBType, or VolatileARGBType" );
	}

	/**
	 * Add the given {@code source} to the lists of {@code converterSetups}
	 * (using specified {@code setupId}) and {@code sources}. For this, the
	 * {@code source} is wrapped with a {@link RealARGBColorConverter} and into
	 * a {@link TransformedSource}.
	 *
	 * @param source
	 *            source to add.
	 * @param setupId
	 *            id of the new source for use in {@link SetupAssignments}.
	 * @param converterSetups
	 *            list of {@link ConverterSetup}s to which the source should be
	 *            added.
	 * @param sources
	 *            list of {@link SourceAndConverter}s to which the source should
	 *            be added.
	 */
	private static < T extends RealType< T > > void addSourceToListsRealType( final Source< T > source, final int setupId, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< T > > sources )
	{
		final T type = Util.getTypeFromInterval( source.getSource( 0, 0 ) );
		final double typeMin = Math.max( 0, Math.min( type.getMinValue(), 65535 ) );
		final double typeMax = Math.max( 0, Math.min( type.getMaxValue(), 65535 ) );
		final RealARGBColorConverter< T > converter;
		if ( source.getType() instanceof Volatile )
			converter = new RealARGBColorConverter.Imp0<>( typeMin, typeMax );
		else
			converter = new RealARGBColorConverter.Imp1<>( typeMin, typeMax );
		converter.setColor( new ARGBType( 0xffffffff ) );

		final TransformedSource< T > ts = new TransformedSource<>( source );
		final SourceAndConverter< T > soc = new SourceAndConverter<>( ts, converter );

		final RealARGBColorConverterSetup setup = new RealARGBColorConverterSetup( setupId, converter );

		converterSetups.add( setup );
		sources.add( soc );
	}

	/**
	 * Add the given {@code source} to the lists of {@code converterSetups}
	 * (using specified {@code setupId}) and {@code sources}. For this, the
	 * {@code source} is wrapped with a {@link ScaledARGBConverter.ARGB} and
	 * into a {@link TransformedSource}.
	 *
	 * @param source
	 *            source to add.
	 * @param setupId
	 *            id of the new source for use in {@link SetupAssignments}.
	 * @param converterSetups
	 *            list of {@link ConverterSetup}s to which the source should be
	 *            added.
	 * @param sources
	 *            list of {@link SourceAndConverter}s to which the source should
	 *            be added.
	 */
	private static void addSourceToListsARGBType( final Source< ARGBType > source, final int setupId, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< ARGBType > > sources )
	{
		final TransformedSource< ARGBType > ts = new TransformedSource<>( source );
		final ScaledARGBConverter.ARGB converter = new ScaledARGBConverter.ARGB( 0, 255 );
		final SourceAndConverter< ARGBType > soc = new SourceAndConverter<>( ts, converter );

		final RealARGBColorConverterSetup setup = new RealARGBColorConverterSetup( setupId, converter );

		converterSetups.add( setup );
		sources.add( soc );
	}

	/**
	 * Add the given {@code source} to the lists of {@code converterSetups}
	 * (using specified {@code setupId}) and {@code sources}. For this, the
	 * {@code source} is wrapped with a {@link ScaledARGBConverter.ARGB} and
	 * into a {@link TransformedSource}.
	 *
	 * @param source
	 *            source to add.
	 * @param setupId
	 *            id of the new source for use in {@link SetupAssignments}.
	 * @param converterSetups
	 *            list of {@link ConverterSetup}s to which the source should be
	 *            added.
	 * @param sources
	 *            list of {@link SourceAndConverter}s to which the source should
	 *            be added.
	 */
	private static void addSourceToListsVolatileARGBType( final Source< VolatileARGBType > source, final int setupId, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< VolatileARGBType > > sources )
	{
		final TransformedSource< VolatileARGBType > ts = new TransformedSource<>( source );
		final ScaledARGBConverter.VolatileARGB converter = new ScaledARGBConverter.VolatileARGB( 0, 255 );
		final SourceAndConverter< VolatileARGBType > soc = new SourceAndConverter<>( ts, converter );

		final RealARGBColorConverterSetup setup = new RealARGBColorConverterSetup( setupId, converter );

		converterSetups.add( setup );
		sources.add( soc );
	}

	/**
	 * Create {@link BigWarpData} from two {@link AbstractSpimData}.
	 *
	 * @param spimDataPList
	 *            array of moving SpimData
	 * @param spimDataQList
	 *            array of fixed SpimData
	 * @param names
	 *            array of source names
	 * @return BigWarpData
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static BigWarpData< ? > createBigWarpData( final AbstractSpimData< ? >[] spimDataPList, final AbstractSpimData< ? >[] spimDataQList, String[] names )
	{
		final ArrayList< ConverterSetup > converterSetups = new ArrayList< ConverterSetup >();
		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();

		// TODO this may need improving
		final AbstractSequenceDescription< ?, ?, ? > seqP = spimDataPList[ 0 ].getSequenceDescription();
		final AbstractSequenceDescription< ?, ?, ? > seqQ = spimDataQList[ 0 ].getSequenceDescription();

		int numMovingSources = 0;
		for ( AbstractSpimData< ? > spimDataP : spimDataPList )
		{
			numMovingSources += spimDataP.getSequenceDescription().getViewSetups().size();
			BigWarpInit.initSetups( spimDataP, converterSetups, sources );
		}

		int numTargetSources = 0;
		for ( AbstractSpimData< ? > spimDataQ : spimDataQList )
		{
			numTargetSources += spimDataQ.getSequenceDescription().getViewSetups().size();
			BigWarpInit.initSetups( spimDataQ, converterSetups, sources );
		}

		int[] movingSourceIndices = ImagePlusLoader.range( 0, numMovingSources );
		int[] targetSourceIndices = ImagePlusLoader.range( numMovingSources, numTargetSources );

		if ( names != null )
		{
			return new BigWarpData( wrapSourcesAsRenamable( sources, names ), converterSetups, null, movingSourceIndices, targetSourceIndices );
		}
		else
		{
			return new BigWarpData( sources, converterSetups, null, movingSourceIndices, targetSourceIndices );
		}
	}

	public static ArrayList< SourceAndConverter< ? > > wrapSourcesAsRenamable( final List< SourceAndConverter< ? > > sources, String[] names )
	{
		final ArrayList< SourceAndConverter< ? > > wrappedSource = new ArrayList< SourceAndConverter< ? > >();

		int i = 0;
		for ( final SourceAndConverter< ? > sac : sources )
		{
			SourceAndConverter< ? > renamableSource = wrapSourceAsRenamable( sac );
			if ( names != null )
			{
				( ( RenamableSource< ? > ) renamableSource.getSpimSource() ).setName( names[ i ] );
			}
			wrappedSource.add( renamableSource );
			i++;
		}
		return wrappedSource;
	}

	private static < T > SourceAndConverter< T > wrapSourceAsRenamable( final SourceAndConverter< T > src )
	{
		if ( src.asVolatile() == null )
		{
			return new SourceAndConverter< T >( new RenamableSource< T >( src.getSpimSource() ), src.getConverter(), null );
		}
		else
		{
			return new SourceAndConverter< T >( new RenamableSource< T >( src.getSpimSource() ), src.getConverter(), src.asVolatile() );
		}
	}

	/**
	 * Create {@link BigWarpData} from two {@link AbstractSpimData}.
	 *
	 * @param spimDataP
	 *            array of moving SpimData
	 * @param spimDataQ
	 *            array of fixed SpimData
	 * @return BigWarpData
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static BigWarpData< ? > createBigWarpData( final AbstractSpimData< ? > spimDataP, final AbstractSpimData< ? > spimDataQ )
	{
		final AbstractSequenceDescription< ?, ?, ? > seqP = spimDataP.getSequenceDescription();
		final AbstractSequenceDescription< ?, ?, ? > seqQ = spimDataQ.getSequenceDescription();

		final ArrayList< ConverterSetup > converterSetups = new ArrayList< ConverterSetup >();

		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();
		BigWarpInit.initSetups( spimDataP, converterSetups, sources );

		int numMovingSources = seqP.getViewSetups().size();
		int numTargetSources = seqQ.getViewSetups().size();

		int[] movingSourceIndices = ImagePlusLoader.range( 0, numMovingSources );
		int[] targetSourceIndices = ImagePlusLoader.range( numMovingSources, numTargetSources );

		/* Load the second source */
		BigWarpInit.initSetups( spimDataQ, converterSetups, sources );

		return new BigWarpData( sources, converterSetups, null, movingSourceIndices, targetSourceIndices );
	}

	public static BigWarpData< ? > createBigWarpData( final ImagePlusLoader loaderP, final ImagePlusLoader loaderQ )
	{
		return createBigWarpData( loaderP, loaderQ, null );
	}

	public static BigWarpData< ? > createBigWarpData( final ImagePlusLoader loaderP, final ImagePlusLoader loaderQ, final String[] names )
	{
		/* Load the first source */
		final AbstractSpimData< ? >[] spimDataP = loaderP.loadAll( 0 );
		int numMovingChannels = loaderP.numChannels();

		/* Load the second source, giving each channel a different setupId */
		final AbstractSpimData< ? >[] spimDataQ = loaderQ.loadAll( numMovingChannels );

		BigWarpData< ? > data = createBigWarpData( spimDataP, spimDataQ, names );

		// update channel settings
		loaderP.update( data );
		loaderQ.update( data );

		return data;
	}

	/**
	 * Create {@link BigWarpData} from two {@link Loader Loaders} that generate
	 * {@link AbstractSpimData}.
	 *
	 * @param loaderP
	 *            moving image source loader
	 * @param loaderQ
	 *            fixed image source loader
	 * @return BigWarpData
	 */
	public static BigWarpData< ? > createBigWarpData( final Loader loaderP, final Loader loaderQ )
	{
		return createBigWarpData( loaderP, loaderQ, null );
	}

	/**
	 * Create {@link BigWarpData} from two {@link Loader Loaders} that generate
	 * {@link AbstractSpimData}.
	 *
	 * @param loaderP
	 *            moving image source loader
	 * @param loaderQ
	 *            fixed image source loader
	 * @param names
	 *            list of names
	 * @return BigWarpData
	 */
	public static BigWarpData< ? > createBigWarpData( final Loader loaderP, final Loader loaderQ, final String[] names )
	{
		/* Load the first source */
		final AbstractSpimData< ? >[] spimDataP = loaderP.load();
		final AbstractSpimData< ? >[] spimDataQ = loaderQ.load();

		BigWarpData< ? > data = createBigWarpData( spimDataP, spimDataQ, names );

		if( loaderP instanceof ImagePlusLoader  )
		{
			((ImagePlusLoader)loaderP).update( data );
		}

		if( loaderQ instanceof ImagePlusLoader  )
		{
			((ImagePlusLoader)loaderQ).update( data );
		}

		return data;
	}

	/**
	 * Create {@link BigWarpData} from two XML files.
	 *
	 * @param xmlFilenameP
	 *            moving source XML
	 * @param xmlFilenameQ
	 *            fixed source XML
	 * @return BigWarpData
	 */
	public static BigWarpData< ? > createBigWarpDataFromXML( final String xmlFilenameP, final String xmlFilenameQ )
	{
		File fP = new File( xmlFilenameP );
		File fQ = new File( xmlFilenameQ );
		// TODO: wrong when XMLLoaders return multichannel sources
		return createBigWarpData( new XMLLoader( xmlFilenameP ), new XMLLoader( xmlFilenameQ ), new String[] { fP.getName(), fQ.getName() } );
	}

	/**
	 * Create {@link BigWarpData} from two {@link ImagePlus ImagePluses}.
	 *
	 * @param impP
	 *            moving source ImagePlus
	 * @param impQ
	 *            fixed source ImagePlus
	 * @return BigWarpData
	 */
	public static BigWarpData< ? > createBigWarpDataFromImages( final ImagePlus impP, final ImagePlus impQ )
	{
		String[] names = namesFromImagePluses( impP, impQ );
		return createBigWarpData( new ImagePlusLoader( impP ), new ImagePlusLoader( impQ ), names );
	}

	/**
	 * Create {@link BigWarpData} from two {@link ImagePlus ImagePlus} arrays.
	 *
	 * @param impP
	 *            array of moving sources ImagePlus
	 * @param impQ
	 *            array of fixed sources ImagePlus
	 * @return BigWarpData
	 */
	public static BigWarpData< ? > createBigWarpDataFromImages( final ImagePlus[] impP, final ImagePlus[] impQ )
	{
		return createBigWarpData( new ImagePlusLoader( impP ), new ImagePlusLoader( impQ ) );
	}

	/**
	 * Create {@link BigWarpData} from one {@link ImagePlus ImagePlus} (moving)
	 * and one {@link ImagePlus ImagePlus} array (target).
	 *
	 * @param impP
	 *            moving source ImagePlus
	 * @param impQ
	 *            array of fixed sources ImagePlus
	 * @return BigWarpData
	 */
	public static BigWarpData< ? > createBigWarpDataFromImages( final ImagePlus impP, final ImagePlus[] impQ )
	{
		return createBigWarpData( new ImagePlusLoader( impP ), new ImagePlusLoader( impQ ) );
	}

	/**
	 * Create {@link BigWarpData} from one {@link ImagePlus ImagePlus} array
	 * (moving) and one {@link ImagePlus ImagePlus} (target).
	 *
	 * @param impP
	 *            array of fixed sources ImagePlus
	 * @param impQ
	 *            fixed source ImagePlus
	 * @return BigWarpData
	 */
	public static BigWarpData< ? > createBigWarpDataFromImages( final ImagePlus[] impP, final ImagePlus impQ )
	{
		return createBigWarpData( new ImagePlusLoader( impP ), new ImagePlusLoader( impQ ) );
	}

	/**
	 * Create {@link BigWarpData} from an xml file and an {@link ImagePlus}.
	 *
	 * @param xmlFilenameP
	 *            movingSource XML
	 * @param impQ
	 *            fixed source ImagePlus
	 * @return BigWarpData
	 */
	public static BigWarpData< ? > createBigWarpDataFromXMLImagePlus( final String xmlFilenameP, final ImagePlus impQ )
	{
		return createBigWarpData( new XMLLoader( xmlFilenameP ), new ImagePlusLoader( impQ ) );
	}

	/**
	 * Create {@link BigWarpData} from an xml file and an {@link ImagePlus}
	 * array.
	 *
	 * @param xmlFilenameP
	 *            movingSource XML
	 * @param impQ
	 *            array of fixed sources ImagePlus
	 * @return BigWarpData
	 */
	public static BigWarpData< ? > createBigWarpDataFromXMLImagePlus( final String xmlFilenameP, final ImagePlus[] impQ )
	{
		return createBigWarpData( new XMLLoader( xmlFilenameP ), new ImagePlusLoader( impQ ) );
	}

	/**
	 * Create {@link BigWarpData} from an {@link ImagePlus} and an XML file.
	 *
	 * @param impP
	 *            moving source ImagePlus
	 * @param xmlFilenameQ
	 *            fixed source XML
	 * @return BigWarpData
	 */
	public static BigWarpData< ? > createBigWarpDataFromImagePlusXML( final ImagePlus impP, final String xmlFilenameQ )
	{
		return createBigWarpData( new ImagePlusLoader( impP ), new XMLLoader( xmlFilenameQ ) );
	}

	/**
	 * Create {@link BigWarpData} from an {@link ImagePlus} array and an XML
	 * file.
	 *
	 * @param impP
	 *            array of moving sources ImagePlus
	 * @param xmlFilenameQ
	 *            fixed source XML
	 * @return BigWarpData
	 */
	public static BigWarpData< ? > createBigWarpDataFromImagePlusXML( final ImagePlus[] impP, final String xmlFilenameQ )
	{
		return createBigWarpData( new ImagePlusLoader( impP ), new XMLLoader( xmlFilenameQ ) );
	}

	/**
	 * Create a {@link String} array of names from two {@link ImagePlus}es,
	 * essentially concatenating the results from calling
	 * {@link namesFromImagePlus} with each.
	 *
	 * @param impP
	 *            first image to generate names from
	 * @param impQ
	 *            second image to generate names from
	 * @return String array of names from both images
	 */
	public static String[] namesFromImagePluses( ImagePlus impP, ImagePlus impQ )
	{
		String[] names = new String[ impP.getNChannels() + impQ.getNChannels() ];

		String[] impPnames = namesFromImagePlus( impP );
		String[] impQnames = namesFromImagePlus( impQ );

		int i = 0;
		for ( String name : impPnames )
			names[ i++ ] = name;
		for ( String name : impQnames )
			names[ i++ ] = name;

		return names;
	}

	/**
	 * Create a {@link String} array of names from an {@link ImagePlus}. Each
	 * channel is given its own name, in the format of [title]-[channel #],
	 * unless there is only one channel.
	 *
	 * @param imp
	 *            image to generate names from
	 * @return String array of names
	 */
	public static String[] namesFromImagePlus( ImagePlus imp )
	{
		if ( imp.getNChannels() == 1 )
			return new String[] { imp.getTitle() };
		String[] names = new String[ imp.getNChannels() ];
		for ( int i = 0; i < names.length; ++i )
			names[ i ] = imp.getTitle() + "-" + i;
		return names;
	}

}
