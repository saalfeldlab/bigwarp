package bigwarp;

import java.util.ArrayList;
import java.util.List;

import bdv.BigDataViewer;
import bdv.SpimSource;
import bdv.VolatileSpimSource;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.transformation.TransformedSource;
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
import net.imglib2.converter.Converter;
import net.imglib2.display.ARGBARGBColorConverter;
import net.imglib2.display.ARGBtoRandomARGBColorConverter;
import net.imglib2.display.ScaledARGBConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.volatiles.VolatileARGBType;

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

	public static void initSetupsARGBTypeRandom(
			final AbstractSpimData< ? > spimData,
			final ARGBType type,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< ? > > sources)
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


	public static void initSetupsARGBType(
			final AbstractSpimData< ? > spimData,
			final ARGBType type,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< ? > > sources )
	{
		initSetupsARGBType( spimData, type, converterSetups, sources, true );
	}


	public static void initSetupsARGBType(
			final AbstractSpimData< ? > spimData,
			final ARGBType type,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< ? > > sources,
			final boolean grayConversion )
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
			if( grayConversion )
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
							}};
				final Converter< ARGBType, ARGBType > converter = new Converter< ARGBType, ARGBType >()
							{
								@Override
								public void convert( final ARGBType input, final ARGBType output )
								{
									output.set( input.get() );
								}};

				vsoc = new SourceAndConverter< VolatileARGBType >( tvs, vconverter );
				soc = new SourceAndConverter< ARGBType >( ts, converter, vsoc );

			}

			sources.add( soc );
		}
	}

	private static void initSetupsARGBTypeNonVolatile(
			final AbstractSpimData< ? > spimData,
			final ARGBType type,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< ? > > sources )
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

	public static void initSetups(
			final AbstractSpimData< ? > spimData,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< ? > > sources )
	{
		BigDataViewer.initSetups(spimData, converterSetups, sources);
	}

	/**
	 * Create {@link BigWarpData} from two {@link AbstractSpimData}.
	 *
	 * @param spimDataP
	 * @param spimDataQ
	 * @return
	 */
	public static BigWarpData createBigWarpData( final AbstractSpimData< ? >[] spimDataPList, final AbstractSpimData< ? >[] spimDataQList )
	{
		final ArrayList< ConverterSetup > converterSetups = new ArrayList< ConverterSetup >();
		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();

		// TODO this may need improving
		final AbstractSequenceDescription< ?, ?, ? > seqP = spimDataPList[ 0 ].getSequenceDescription();
		final AbstractSequenceDescription< ?, ?, ? > seqQ = spimDataQList[ 0 ].getSequenceDescription();

		int numMovingSources = 0;
		for( AbstractSpimData< ? > spimDataP : spimDataPList )
		{
			numMovingSources += spimDataP.getSequenceDescription().getViewSetups().size();
			BigWarpInit.initSetups( spimDataP, converterSetups, sources );
		}

		int numTargetSources = 0;
		for( AbstractSpimData< ? > spimDataQ : spimDataQList )
		{
			numTargetSources += spimDataQ.getSequenceDescription().getViewSetups().size();
			BigWarpInit.initSetups( spimDataQ, converterSetups, sources );
		}

		int[] movingSourceIndices = ImagePlusLoader.range( 0, numMovingSources );
		int[] targetSourceIndices = ImagePlusLoader.range( numMovingSources, numTargetSources );

		return new BigWarpData( sources, seqP, seqQ, converterSetups, movingSourceIndices, targetSourceIndices );
	}

	/**
	 * Create {@link BigWarpData} from two {@link AbstractSpimData}.
	 *
	 * @param spimDataP
	 * @param spimDataQ
	 * @return
	 */
	public static BigWarpData createBigWarpData( final AbstractSpimData< ? > spimDataP, final AbstractSpimData< ? > spimDataQ )
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

		return new BigWarpData( sources, seqP, seqQ, converterSetups, movingSourceIndices, targetSourceIndices );
	}

	public static BigWarpData createBigWarpData(
			final ImagePlusLoader loaderP,
			final ImagePlusLoader loaderQ )
	{
		/* Load the first source */
		final AbstractSpimData< ? >[] spimDataP = loaderP.loadAll( 0 );
		int numMovingChannels = loaderP.numChannels();

		/*
		 * Load the second source, giving each channel a different setupId
		 */
		final AbstractSpimData< ? >[] spimDataQ = loaderQ.loadAll( numMovingChannels );

		return createBigWarpData( spimDataP, spimDataQ );
	}
	
	/**
	 * Create {@link BigWarpData} from two {@link Loader Loaders} that generate {@link AbstractSpimData}.
	 *
	 * @param loaderP
	 * @param loaderQ
	 * @return
	 */
	public static BigWarpData createBigWarpData(
			final Loader loaderP,
			final Loader loaderQ )
	{
		/* Load the first source */
		final AbstractSpimData< ? >[] spimDataP = loaderP.load();
		final AbstractSpimData< ? >[] spimDataQ = loaderQ.load();

		return createBigWarpData( spimDataP, spimDataQ );
	}

	/**
	 * Create {@link BigWarpData} from two XML files.
	 *
	 * @param xmlFilenameP
	 * @param xmlFilenameQ
	 * @return
	 */
	public static BigWarpData createBigWarpDataFromXML( final String xmlFilenameP, final String xmlFilenameQ )
	{
		return createBigWarpData( new XMLLoader( xmlFilenameP ), new XMLLoader( xmlFilenameQ ) );
	}

	/**
	 * Create {@link BigWarpData} from two {@link ImagePlus ImagePluses}.
	 *
	 * @param impP
	 * @param impQ
	 * @return
	 */
	public static BigWarpData createBigWarpDataFromImages( final ImagePlus impP, final ImagePlus impQ )
	{
		return createBigWarpData( new ImagePlusLoader( impP ), new ImagePlusLoader( impQ ) );
	}

	/**
	 * Create {@link BigWarpData} from two {@link ImagePlus ImagePlus} arrays.
	 *
	 * @param impP
	 * @param impQ
	 * @return
	 */
	public static BigWarpData createBigWarpDataFromImages( final ImagePlus[] impP, final ImagePlus[] impQ )
	{
		return createBigWarpData( new ImagePlusLoader( impP ), new ImagePlusLoader( impQ ) );
	}

	/**
	 * Create {@link BigWarpData} from one {@link ImagePlus ImagePlus} (moving) and one {@link ImagePlus ImagePlus} array (target).
	 *
	 * @param impP
	 * @param impQ
	 * @return
	 */
	public static BigWarpData createBigWarpDataFromImages( final ImagePlus impP, final ImagePlus[] impQ )
	{
		return createBigWarpData( new ImagePlusLoader( impP ), new ImagePlusLoader( impQ ) );
	}

	/**
	 * Create {@link BigWarpData} from one {@link ImagePlus ImagePlus} array (moving) and one {@link ImagePlus ImagePlus} (target).
	 *
	 * @param impP
	 * @param impQ
	 * @return
	 */
	public static BigWarpData createBigWarpDataFromImages( final ImagePlus[] impP, final ImagePlus impQ )
	{
		return createBigWarpData( new ImagePlusLoader( impP ), new ImagePlusLoader( impQ ) );
	}

	/**
	 * Create {@link BigWarpData} from an xml file and an {@link ImagePlus}.
	 *
	 * @param xmlFilenameP
	 * @param impQ
	 * @return
	 */
	public static BigWarpData createBigWarpDataFromXMLImagePlus( final String xmlFilenameP, final ImagePlus impQ )
	{
		return createBigWarpData( new XMLLoader( xmlFilenameP ), new ImagePlusLoader( impQ ) );
	}

	/**
	 * Create {@link BigWarpData} from an xml file and an {@link ImagePlus} array.
	 *
	 * @param xmlFilenameP
	 * @param impQ
	 * @return
	 */
	public static BigWarpData createBigWarpDataFromXMLImagePlus( final String xmlFilenameP, final ImagePlus[] impQ )
	{
		return createBigWarpData( new XMLLoader( xmlFilenameP ), new ImagePlusLoader( impQ ) );
	}

	/**
	 * Create {@link BigWarpData} from an {@link ImagePlus} and an XML file.
	 *
	 * @param impP
	 * @param xmlFilenameQ
	 * @return
	 */
	public static BigWarpData createBigWarpDataFromImagePlusXML( final ImagePlus impP, final String xmlFilenameQ )
	{
		return createBigWarpData( new ImagePlusLoader( impP ), new XMLLoader( xmlFilenameQ ) );
	}

	/**
	 * Create {@link BigWarpData} from an {@link ImagePlus} array and an XML file.
	 *
	 * @param impP
	 * @param xmlFilenameQ
	 * @return
	 */
	public static BigWarpData createBigWarpDataFromImagePlusXML( final ImagePlus[] impP, final String xmlFilenameQ )
	{
		return createBigWarpData( new ImagePlusLoader( impP ), new XMLLoader( xmlFilenameQ ) );
	}
}
