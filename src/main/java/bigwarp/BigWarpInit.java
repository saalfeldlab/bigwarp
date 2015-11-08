package bigwarp;

import java.util.List;

import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.converter.TypeIdentity;
import net.imglib2.display.ARGBARGBColorConverter;
import net.imglib2.display.ARGBtoRandomARGBColorConverter;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.display.ScaledARGBConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.volatiles.VolatileARGBType;
import bdv.BigDataViewer;
import bdv.SpimSource;
import bdv.VolatileSpimSource;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;

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
	
	private static < T extends RealType< T >, V extends Volatile< T > & RealType< V > > void initSetupsRealType(
			final AbstractSpimData< ? > spimData,
			final T type,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< ? > > sources )
	{
		System.out.println("setupsRealType");
		
		if ( spimData.getSequenceDescription().getImgLoader() instanceof WrapBasicImgLoader )
		{
			initSetupsRealTypeNonVolatile( spimData, type, converterSetups, sources );
			return;
		}
		final double typeMin = Math.max( 0, Math.min( type.getMinValue(), 65535 ) );
		final double typeMax = Math.max( 0, Math.min( type.getMaxValue(), 65535 ) );
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
		{
			final RealARGBColorConverter< V > vconverter = new RealARGBColorConverter.Imp0< V >( typeMin, typeMax );
			vconverter.setColor( new ARGBType( 0xffffffff ) );
			final RealARGBColorConverter< T > converter = new RealARGBColorConverter.Imp1< T >( typeMin, typeMax );
			converter.setColor( new ARGBType( 0xffffffff ) );

			final int setupId = setup.getId();
			final String setupName = createSetupName( setup );
			final VolatileSpimSource< T, V > vs = new VolatileSpimSource< T, V >( spimData, setupId, setupName );
			final SpimSource< T > s = vs.nonVolatile();

			// Decorate each source with an extra transformation, that can be
			// edited manually in this viewer.
			final TransformedSource< V > tvs = new TransformedSource< V >( vs );
			final TransformedSource< T > ts = new TransformedSource< T >( s, tvs );

			final SourceAndConverter< V > vsoc = new SourceAndConverter< V >( tvs, vconverter );
			final SourceAndConverter< T > soc = new SourceAndConverter< T >( ts, converter, vsoc );

			sources.add( soc );
			converterSetups.add( new RealARGBColorConverterSetup( setupId, converter, vconverter ) );
		}
	}

	private static < T extends RealType< T > > void initSetupsRealTypeNonVolatile(
			final AbstractSpimData< ? > spimData,
			final T type,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< ? > > sources )
	{
		System.out.println("setupsRealType NV");
		
		final double typeMin = type.getMinValue();
		final double typeMax = type.getMaxValue();
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
		{
			final RealARGBColorConverter< T > converter = new RealARGBColorConverter.Imp1< T >( typeMin, typeMax );
			converter.setColor( new ARGBType( 0xffffffff ) );

			final int setupId = setup.getId();
			final String setupName = createSetupName( setup );
			final SpimSource< T > s = new SpimSource< T >( spimData, setupId, setupName );

			// Decorate each source with an extra transformation, that can be
			// edited manually in this viewer.
			final TransformedSource< T > ts = new TransformedSource< T >( s );
			final SourceAndConverter< T > soc = new SourceAndConverter< T >( ts, converter );

			sources.add( soc );
			converterSetups.add( new RealARGBColorConverterSetup( setupId, converter ) );
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
				Converter< VolatileARGBType, ARGBType > vconverter = new Converter< VolatileARGBType, ARGBType >()
						{
							@Override
							public void convert( final VolatileARGBType input, final ARGBType output )
							{
								output.set( input.get() );
							}};
				Converter< ARGBType, ARGBType > converter = new Converter< ARGBType, ARGBType >()
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
		System.out.println("setupsARGBType NV");
		
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
	
//	@SuppressWarnings( { "unchecked", "rawtypes" } )
//	public static void initSetups(
//			final AbstractSpimData< ? > spimData,
//			final List< ConverterSetup > converterSetups,
//			final List< SourceAndConverter< ? > > sources )
//	{
//		final Object type = spimData.getSequenceDescription().getImgLoader().getImageType();
//		if ( RealType.class.isInstance( type ) )
//			initSetupsRealType( spimData, ( RealType ) type, converterSetups, sources );
//		else if ( ARGBType.class.isInstance( type ) )
//			initSetupsARGBType( spimData, ( ARGBType ) type, converterSetups, sources );
//		else
//			throw new IllegalArgumentException( "ImgLoader of type " + type.getClass() + " not supported." );
//	}
	
}
