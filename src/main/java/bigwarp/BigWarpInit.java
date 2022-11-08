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
package bigwarp;

import ij.IJ;
import ij.plugin.FolderOpener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import ij.io.FileInfo;
import java.util.Objects;
import net.imglib2.util.ValuePair;
import org.janelia.saalfeldlab.n5.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.ij.N5Factory;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.MultiscaleMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5GenericSingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5SingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5ViewerMultiscaleMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.SpatialMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.imagej.ImagePlusLegacyMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.imagej.N5ImagePlusMetadata;

import bdv.BigDataViewer;
import bdv.img.BwRandomAccessibleIntervalSource;
import bdv.img.RenamableSource;
import bdv.spimdata.SpimDataMinimal;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.transformation.TransformedSource;
import bdv.util.RandomAccessibleIntervalMipmapSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp.BigWarpData;
import bigwarp.loader.ImagePlusLoader;
import bigwarp.loader.Loader;
import bigwarp.loader.XMLLoader;
import ij.ImagePlus;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.display.ScaledARGBConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;

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

//	public static void initSetupsARGBTypeRandom( final AbstractSpimData< ? > spimData, final ARGBType type, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< ? > > sources )
//	{
//		if ( spimData.getSequenceDescription().getImgLoader() instanceof WrapBasicImgLoader )
//		{
//			initSetupsARGBTypeNonVolatile( spimData, type, converterSetups, sources );
//			return;
//		}
//
//		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
//		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
//		{
//
//			final int setupId = setup.getId();
//
//			final String setupName = createSetupName( setup );
//			final VolatileSpimSource< ARGBType, VolatileARGBType > vs = new VolatileSpimSource< ARGBType, VolatileARGBType >( spimData, setupId, setupName );
//			final SpimSource< ARGBType > s = vs.nonVolatile();
//
//			// Decorate each source with an extra transformation, that can be
//			// edited manually in this viewer.
//			final TransformedSource< VolatileARGBType > tvs = new TransformedSource< VolatileARGBType >( vs );
//			final TransformedSource< ARGBType > ts = new TransformedSource< ARGBType >( s, tvs );
//
//			final SourceAndConverter< ARGBType > soc;
//			final SourceAndConverter< VolatileARGBType > vsoc;
//			final ConverterSetup converterSetup;
//
//			final ARGBtoRandomARGBColorConverter.ToGray converter = new ARGBtoRandomARGBColorConverter.ToGray( 0, 255 );
//			final ARGBtoRandomARGBColorConverter.VolatileToGray vconverter = new ARGBtoRandomARGBColorConverter.VolatileToGray( 0, 255 );
//
//			converterSetup = new RealARGBColorConverterSetup( setupId, converter, vconverter );
//			vsoc = new SourceAndConverter< VolatileARGBType >( tvs, vconverter );
//			soc = new SourceAndConverter< ARGBType >( ts, converter, vsoc );
//
//			converterSetups.add( converterSetup );
//
//			sources.add( soc );
//		}
//	}

//	public static void initSetupsARGBType( final AbstractSpimData< ? > spimData, final ARGBType type, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< ? > > sources )
//	{
//		initSetupsARGBType( spimData, type, converterSetups, sources, true );
//	}

//	public static void initSetupsARGBType( final AbstractSpimData< ? > spimData, final ARGBType type, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< ? > > sources, final boolean grayConversion )
//	{
//		if ( spimData.getSequenceDescription().getImgLoader() instanceof WrapBasicImgLoader )
//		{
//			initSetupsARGBTypeNonVolatile( spimData, type, converterSetups, sources );
//			return;
//		}
//
//		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
//		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
//		{
//
//			final int setupId = setup.getId();
//
//			final String setupName = createSetupName( setup );
//			final VolatileSpimSource< ARGBType, VolatileARGBType > vs = new VolatileSpimSource< ARGBType, VolatileARGBType >( spimData, setupId, setupName );
//			final SpimSource< ARGBType > s = vs.nonVolatile();
//
//			// Decorate each source with an extra transformation, that can be
//			// edited manually in this viewer.
//			final TransformedSource< VolatileARGBType > tvs = new TransformedSource< VolatileARGBType >( vs );
//			final TransformedSource< ARGBType > ts = new TransformedSource< ARGBType >( s, tvs );
//
//			final SourceAndConverter< ARGBType > soc;
//			final SourceAndConverter< VolatileARGBType > vsoc;
//			final ConverterSetup converterSetup;
//			if ( grayConversion )
//			{
//				final ARGBARGBColorConverter.ToGray converter = new ARGBARGBColorConverter.ToGray( 0, 255 );
//				final ARGBARGBColorConverter.VolatileToGray vconverter = new ARGBARGBColorConverter.VolatileToGray( 0, 255 );
////				converter = new ScaledARGBConverter.ARGB( 0, 255 );
////				vconverter = new ScaledARGBConverter.VolatileARGB( 0, 255 );
//
//				converterSetup = new RealARGBColorConverterSetup( setupId, converter, vconverter );
//				vsoc = new SourceAndConverter< VolatileARGBType >( tvs, vconverter );
//				soc = new SourceAndConverter< ARGBType >( ts, converter, vsoc );
//
//				converterSetups.add( converterSetup );
//			}
//			else
//			{
//				final Converter< VolatileARGBType, ARGBType > vconverter = new Converter< VolatileARGBType, ARGBType >()
//				{
//					@Override
//					public void convert( final VolatileARGBType input, final ARGBType output )
//					{
//						output.set( input.get() );
//					}
//				};
//				final Converter< ARGBType, ARGBType > converter = new Converter< ARGBType, ARGBType >()
//				{
//					@Override
//					public void convert( final ARGBType input, final ARGBType output )
//					{
//						output.set( input.get() );
//					}
//				};
//
//				vsoc = new SourceAndConverter< VolatileARGBType >( tvs, vconverter );
//				soc = new SourceAndConverter< ARGBType >( ts, converter, vsoc );
//
//			}
//
//			sources.add( soc );
//		}
//	}

//	private static void initSetupsARGBTypeNonVolatile( final AbstractSpimData< ? > spimData, final ARGBType type, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< ? > > sources )
//	{
//		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
//		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
//		{
//			final ScaledARGBConverter.ARGB converter = new ScaledARGBConverter.ARGB( 0, 255 );
//
//			final int setupId = setup.getId();
//			final String setupName = createSetupName( setup );
//			final SpimSource< ARGBType > s = new SpimSource< ARGBType >( spimData, setupId, setupName );
//
//			// Decorate each source with an extra transformation, that can be
//			// edited manually in this viewer.
//			final TransformedSource< ARGBType > ts = new TransformedSource< ARGBType >( s );
//			final SourceAndConverter< ARGBType > soc = new SourceAndConverter< ARGBType >( ts, converter );
//
//			sources.add( soc );
//			converterSetups.add( new RealARGBColorConverterSetup( setupId, converter ) );
//		}
//	}

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
		final RealARGBColorConverter< T > converter = RealARGBColorConverter.create( type, typeMin, typeMax );
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
	 * @param movingSourceList
	 *            array of moving SpimData
	 * @param fixedSourceList
	 *            array of fixed SpimData
	 * @param names
	 *            array of source names
	 * @return BigWarpData the data
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public static BigWarpData< ? > createBigWarpData( final Source< ? >[] movingSourceList, final Source< ? >[] fixedSourceList, String[] names )
	{
		BigWarpData data = initData();

		int setupId = 0;
		// moving
		for ( Source< ? > mvgSource : movingSourceList )
		{
			add( data, mvgSource, setupId++, 1, true );
		}

		// target
		for ( Source< ? > fxdSource : fixedSourceList )
		{
			add( data, fxdSource, setupId, 1, false );
		}

		data.wrapUp();

		if ( names != null ) { return new BigWarpData( wrapSourcesAsRenamable( data.sources, names ), data.converterSetups, data.cache, data.movingSourceIndices, data.targetSourceIndices ); }

		return data;
	}

	@SuppressWarnings( { "rawtypes" } )
	public static < T > int add( BigWarpData bwdata, ImagePlus ip, int setupId, int numTimepoints, boolean isMoving )
	{
		ImagePlusLoader loader = new ImagePlusLoader( ip );
		SpimDataMinimal[] dataList = loader.loadAll( setupId );
		for ( SpimDataMinimal data : dataList )
		{
			add( bwdata, data, setupId, numTimepoints, isMoving );
			setupId++;
		}
		loader.update(bwdata);
		return loader.numSources();
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static < T > BigWarpData< ? > add( BigWarpData bwdata, Source< T > src, int setupId, int numTimepoints, boolean isMoving )
	{
		addSourceToListsGenericType( src, setupId, bwdata.converterSetups, bwdata.sources );

		int N = bwdata.sources.size();
		if ( isMoving )
			bwdata.movingSourceIndexList.add( N - 1 );
		else
			bwdata.targetSourceIndexList.add( N - 1 );

		return bwdata;
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static < T > BigWarpData< ? > add( BigWarpData bwdata, AbstractSpimData< ? > data, int baseId, int numTimepoints, boolean isMoving )
	{
		final List<SourceAndConverter<?>> tmpSources = new ArrayList<>();
		final List<ConverterSetup> tmpConverterSetups = new ArrayList<>();
		initSetups( data, tmpConverterSetups, tmpSources );

		int setupId = baseId;
		for( SourceAndConverter sac : tmpSources )
			add( bwdata, sac.getSpimSource(), setupId++, numTimepoints, isMoving );

//		int N = bwdata.sources.size();
//		final ArrayList<Integer > idxList;
//		if ( isMoving )
//			idxList = bwdata.movingSourceIndexList;
//		else
//			idxList = bwdata.targetSourceIndexList;
//
//		for( int i = startSize; i < N; i++ )
//			idxList.add( i );
//
		return bwdata;
	}


	private static String schemeSpecificPartWithoutQuery( URI uri )
	{
		return uri.getSchemeSpecificPart().replaceAll( "\\?" + uri.getQuery(), "" ).replaceAll( "//", "" );
	}

	public static Source< ? > add( final BigWarpData< ? > bwData, String uri,  int setupId, boolean isMoving ) throws URISyntaxException, IOException, SpimDataException
	{
		final URI tmpUri = new URI( "TMP", uri, null );
		String encodedUriString = tmpUri.getRawSchemeSpecificPart();
		encodedUriString = encodedUriString.replaceAll( "%23", "#" );
		URI firstUri = new URI( encodedUriString );
		final int prevSacCount = bwData.sources.size();
		Source< ? > source = null;
		if ( firstUri.isOpaque() )
		{
			URI secondUri = new URI( firstUri.getSchemeSpecificPart() );
			final String firstScheme = firstUri.getScheme().toLowerCase();
			final String secondScheme = secondUri.getScheme();
			final String secondSchemeSpecificMinusQuery = schemeSpecificPartWithoutQuery( secondUri );
			final boolean dontIncludeScheme = secondScheme == null || Objects.equals( secondScheme, "" ) || Objects.equals( secondScheme.toLowerCase(), "file" );
			final String secondSchemeAndPath = dontIncludeScheme ? secondSchemeSpecificMinusQuery : secondScheme + "://" + secondSchemeSpecificMinusQuery;
			final String datasetQuery = secondUri.getQuery();
			final String dataset = datasetQuery == null ? "/" : datasetQuery;
			final N5Reader n5reader;
			switch ( firstScheme )
			{
			case "n5":
				n5reader = new N5Factory().openReader( secondSchemeAndPath );
				break;
			case "zarr":
				n5reader = new N5ZarrReader( secondSchemeAndPath );
				break;
			case "h5":
			case "hdf5":
			case "hdf":
				n5reader = new N5HDF5Reader( secondSchemeAndPath );
				break;
			default:
				throw new URISyntaxException( firstScheme, "Unsupported Top Level Protocol" );
			}

			source = loadN5Source( n5reader, dataset );
			add( bwData, source, setupId, 0, isMoving);
		}
		else
		{
			firstUri = new URI( encodedUriString.replaceAll( "%23", "#" ) );
			final String firstSchemeSpecificPartMinusQuery = schemeSpecificPartWithoutQuery( firstUri );
			final boolean skipScheme = firstUri.getScheme() == null
					|| firstUri.getScheme().trim().isEmpty()
					|| firstUri.getScheme().trim().equalsIgnoreCase( "n5" )
					|| firstUri.getScheme().trim().equalsIgnoreCase( "file" );
			final String firstSchemeAndPath = skipScheme ? firstSchemeSpecificPartMinusQuery : firstUri.getScheme() + "://" + firstSchemeSpecificPartMinusQuery;
			try
			{
				final N5Reader n5reader = new N5Factory().openReader( firstSchemeAndPath );
				final String datasetQuery = firstUri.getQuery();
				final String dataset = datasetQuery == null ? "/" : datasetQuery;
				source = loadN5Source( n5reader, dataset );
				add( bwData, source, setupId, 0, isMoving);
			}
			catch ( Exception ignored )
			{
			}
			if (source == null) {
				if ( firstSchemeAndPath.trim().toLowerCase().endsWith( ".xml" ) )
				{
					addToData( bwData, isMoving, setupId, firstSchemeAndPath, firstUri.getQuery() );
					return bwData.sources.get( bwData.sources.size() - 1 ).getSpimSource();
				}
				else
				{
					final ImagePlus ijp;
					try
					{

						if ( new File( uri ).isDirectory() )
						{
							ijp = FolderOpener.open( uri );
						}
						else
						{
							ijp = IJ.openImage( uri );
						}
					}
					catch ( Exception e )
					{
						return null;
					}
					add( bwData, ijp, setupId, 0, isMoving);
					source = bwData.sources.get( bwData.sources.size() - 1 ).getSpimSource();
				}
			}

		}

		/* override any already set urls with the uri we used to load this source. */
		if (source != null) {
			final int postSacCount = bwData.sources.size();
			final List< ? extends SourceAndConverter< ? > > addedSacs = bwData.sources.subList( prevSacCount, postSacCount );
			bwData.urls.put( setupId, new ValuePair<>( () -> uri, new ArrayList<>(addedSacs) ) );
		}
		return source;
	}

	public static SpimData addToData( final BigWarpData<?> bwdata,
			final boolean isMoving, final int setupId, final String rootPath, final String dataset )
	{
		if( rootPath.endsWith( "xml" ))
		{
			SpimData spimData;
			try
			{
				spimData = new XmlIoSpimData().load( rootPath );
				add( bwdata, spimData, setupId, 0, isMoving );

				if( isMoving )
					return spimData;
			}
			catch ( SpimDataException e ) { e.printStackTrace(); }
			return null;
		}
		else
		{
			BigWarpInit.add( bwdata, loadN5Source( rootPath, dataset ), setupId, 0, isMoving );
			return null;
		}
	}

	public static Source<?> loadN5Source( final String n5Root, final String n5Dataset )
	{
		final N5Reader n5;
		try
		{
			n5 = new N5Factory().openReader( n5Root );
		}
		catch ( IOException e ) { 
			e.printStackTrace();
			return null;
		}
		return loadN5Source( n5, n5Dataset );
	}
	public static Source<?> loadN5Source( final N5Reader n5, final String n5Dataset )
	{
		final N5MetadataParser<?>[] PARSERS = new N5MetadataParser[]{
			new ImagePlusLegacyMetadataParser(),
			new N5CosemMetadataParser(),
			new N5SingleScaleMetadataParser(),
			new CanonicalMetadataParser(),
			new N5GenericSingleScaleMetadataParser()
		};

		final N5MetadataParser<?>[] GROUP_PARSERS = new N5MetadataParser[]{
			new N5CosemMultiScaleMetadata.CosemMultiScaleParser(),
			new N5ViewerMultiscaleMetadataParser(),
			new CanonicalMetadataParser(),
		};

		N5Metadata meta = null;
		try
		{
			final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer( n5, 
					N5DatasetDiscoverer.fromParsers(PARSERS), 
					N5DatasetDiscoverer.fromParsers(GROUP_PARSERS) );

			final N5TreeNode node = discoverer.discoverAndParseRecursive( n5Dataset );
			meta = node.getMetadata();
		}
		catch ( IOException e )
		{}

		if( meta instanceof MultiscaleMetadata )
		{
			return openAsSourceMulti( n5, (MultiscaleMetadata<?>)meta, true );
		}
		else
		{
			return openAsSource( n5, meta, true );
		}
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static <T extends N5Metadata > Source<?> openAsSource( final N5Reader n5, final T meta, final boolean isVolatile )
	{
		final RandomAccessibleInterval imageRaw;
		final RandomAccessibleInterval image;
		try
		{
			if( isVolatile )
				imageRaw = to3d( N5Utils.openVolatile( n5, meta.getPath() ));
			else
				imageRaw = to3d( N5Utils.open( n5, meta.getPath() ));

			if( meta instanceof N5ImagePlusMetadata 
					&& ((N5ImagePlusMetadata)meta).getType() == ImagePlus.COLOR_RGB
					&& Util.getTypeFromInterval( imageRaw ) instanceof UnsignedIntType )
			{
				image = toColor( imageRaw );
			}
			else
				image = imageRaw;

			if( meta instanceof SpatialMetadata )
			{
				final String unit = ((SpatialMetadata)meta).unit();
				final AffineTransform3D srcXfm = ((SpatialMetadata)meta).spatialTransform3d();
				final FinalVoxelDimensions voxelDims = new FinalVoxelDimensions( unit, 
						new double[]{ srcXfm.get( 0, 0 ), srcXfm.get( 1, 1 ), srcXfm.get( 2, 2 ) });

				return new BwRandomAccessibleIntervalSource( image, (NumericType ) Util.getTypeFromInterval( image ),
						srcXfm, meta.getPath(), voxelDims );
			}
			else
				return new BwRandomAccessibleIntervalSource( image, ( NumericType ) Util.getTypeFromInterval( image ),
						new AffineTransform3D(), meta.getPath() );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		return null;
	}

	public static Source<?> openAsSourceMulti( final N5Reader n5, final MultiscaleMetadata<?> multiMeta, final boolean isVolatile )
	{
		final String[] paths = multiMeta.getPaths();
		final AffineTransform3D[] transforms = multiMeta.spatialTransforms3d();
		final String unit = multiMeta.units()[0];

		@SuppressWarnings( "rawtypes" )
		final RandomAccessibleInterval[] images = new RandomAccessibleInterval[paths.length];
		final double[][] mipmapScales = new double[ images.length ][ 3 ];
		for ( int s = 0; s < images.length; ++s )
		{
			try
			{
				if( isVolatile )
					images[ s ] = to3d( N5Utils.openVolatile( n5, paths[s] ));
				else
					images[ s ] = to3d( N5Utils.open( n5, paths[s] ));
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}

			mipmapScales[ s ][ 0 ] = transforms[ s ].get( 0, 0 );
			mipmapScales[ s ][ 1 ] = transforms[ s ].get( 1, 1 );
			mipmapScales[ s ][ 2 ] = transforms[ s ].get( 2, 2 );
		}

		@SuppressWarnings( { "unchecked", "rawtypes" } )
		final RandomAccessibleIntervalMipmapSource source = new RandomAccessibleIntervalMipmapSource( 
				images, 
				(NumericType ) Util.getTypeFromInterval(images[0]),
				mipmapScales,
				new mpicbg.spim.data.sequence.FinalVoxelDimensions( unit, mipmapScales[0]),
				new AffineTransform3D(),
				multiMeta.getPaths()[0] + "_group" );

		return source;
	}

	private static RandomAccessibleInterval<?> to3d( RandomAccessibleInterval<?> img )
	{
		if( img.numDimensions() == 2 )
			return Views.addDimension( img, 0, 0 );
		else
			return img;
	}

	private static RandomAccessibleInterval<ARGBType> toColor( RandomAccessibleInterval<UnsignedIntType> img )
	{
		return Converters.convertRAI( img,
				new Converter<UnsignedIntType,ARGBType>()
				{
					@Override
					public void convert( UnsignedIntType input, ARGBType output )
					{
						output.set( input.getInt() );
					}
				},
				new ARGBType() );
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
	 * @param converterSetups
	 *            list of {@link ConverterSetup}s to which the source should be
	 *            added.
	 * @param sources
	 *            list of {@link SourceAndConverter}s to which the source should
	 *            be added.
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	private static < T > void addSourceToListsGenericType(
			final Source< T > source,
			final int setupId,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< T > > sources )
	{
		final T type = source.getType();
		if ( type instanceof RealType || type instanceof ARGBType || type instanceof VolatileARGBType )
			addSourceToListsNumericType( ( Source ) source, setupId, converterSetups, ( List ) sources );
		else
			throw new IllegalArgumentException( "Unknown source type. Expected RealType, ARGBType, or VolatileARGBType" );
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
	 * @param converterSetups
	 *            list of {@link ConverterSetup}s to which the source should be
	 *            added.
	 * @param sources
	 *            list of {@link SourceAndConverter}s to which the source should
	 *            be added.
	 */
	private static < T extends NumericType< T > > void addSourceToListsNumericType(
			final Source< T > source,
			final int setupId,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< T > > sources )
	{
		final T type = source.getType();
		final SourceAndConverter< T > soc = BigDataViewer.wrapWithTransformedSource(
				new SourceAndConverter<>( source, BigDataViewer.createConverterToARGB( type ) ) );
		converterSetups.add( BigDataViewer.createConverterSetup( soc, setupId ) );
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

		int numMovingSources = 0;
		for ( AbstractSpimData< ? > spimDataP : spimDataPList )
		{
			numMovingSources += spimDataP.getSequenceDescription().getViewSetups().size();
			BigDataViewer.initSetups( spimDataP, converterSetups, sources );
		}

		int numTargetSources = 0;
		for ( AbstractSpimData< ? > spimDataQ : spimDataQList )
		{
			numTargetSources += spimDataQ.getSequenceDescription().getViewSetups().size();
			BigDataViewer.initSetups( spimDataQ, converterSetups, sources );
		}

		int[] movingSourceIndices = ImagePlusLoader.range( 0, numMovingSources );
		int[] targetSourceIndices = ImagePlusLoader.range( numMovingSources, numTargetSources );

		if ( names != null && names.length == sources.size() )
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

	public static BigWarpData< ? > createBigWarpData( final ImagePlusLoader loaderP, final ImagePlusLoader loaderQ, 
			final String[] names )
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
	 * @param namesIn
	 *            list of names
	 * @return BigWarpData
	 */
	public static BigWarpData< ? > createBigWarpData( final Loader loaderP, final Loader loaderQ, final String[] namesIn )
	{
		/* Load the moving sources */
		final AbstractSpimData< ? >[] spimDataP;
		if( loaderP instanceof ImagePlusLoader  )
			spimDataP = loaderP.load();
		else 
			spimDataP = loaderP.load();

		/* Load the fixed sources */
		final AbstractSpimData< ? >[] spimDataQ;
		if( loaderQ instanceof ImagePlusLoader  )
			spimDataQ = ((ImagePlusLoader)loaderQ).loadAll( spimDataP.length );
		else
			spimDataQ = loaderQ.load();

		int N = loaderP.numSources() + loaderQ.numSources();

		String[] names;
		if( namesIn == null || namesIn.length != N )
		{
			names = new String[ N ];
			int j = 0;
			for( int i = 0; i < loaderP.numSources(); i++ )
				names[ j++ ] = loaderP.name( i );

			for( int i = 0; i < loaderQ.numSources(); i++ )
				names[ j++ ] = loaderQ.name( i );
		}
		else
			names = namesIn;

		BigWarpData< ? > data = createBigWarpData( spimDataP, spimDataQ, names );

		if( loaderP instanceof ImagePlusLoader  )
			((ImagePlusLoader)loaderP).update( data );

		if( loaderQ instanceof ImagePlusLoader  )
			((ImagePlusLoader)loaderQ).update( data );

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
		return createBigWarpData( new XMLLoader( xmlFilenameP ), new XMLLoader( xmlFilenameQ ), null );
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
		return createBigWarpData( new ImagePlusLoader( impP ), new ImagePlusLoader( impQ ), null );
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
