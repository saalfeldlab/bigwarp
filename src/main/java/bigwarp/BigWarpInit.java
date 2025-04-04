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

import static bdv.BigDataViewer.createConverterToARGB;
import static bdv.BigDataViewer.wrapWithTransformedSource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.bdv.N5Viewer;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.N5ViewerMultichannelMetadata;
import org.janelia.saalfeldlab.n5.metadata.imagej.ImagePlusLegacyMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.imagej.N5ImagePlusMetadata;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.janelia.saalfeldlab.n5.universe.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.MultiscaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5GenericSingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5ViewerMultiscaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.TransformUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.InverseCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.OmeNgffV06MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.OmeNgffV06MultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.OmeNgffV06MultiScaleMetadataAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.OmeNgffV06MultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfigAdapter;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;

import com.google.gson.GsonBuilder;

import bdv.BigDataViewer;
import bdv.cache.SharedQueue;
import bdv.img.BwRandomAccessibleIntervalSource;
import bdv.img.RenamableSource;
import bdv.img.WarpedSource;
import bdv.spimdata.SpimDataMinimal;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvOptions;
import bdv.util.RandomAccessibleIntervalMipmapSource;
import bdv.util.RandomAccessibleIntervalMipmapSource4D;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.util.VolatileSource;
import bdv.util.volatiles.VolatileTypeMatcher;
import bdv.util.volatiles.VolatileViews;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bigwarp.loader.ImagePlusLoader;
import bigwarp.loader.Loader;
import bigwarp.loader.XMLLoader;
import bigwarp.source.SourceInfo;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.plugin.FolderOpener;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.Volatile;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.display.ScaledARGBConverter;
import net.imglib2.display.ScaledARGBConverter.ARGB;
import net.imglib2.display.ScaledARGBConverter.VolatileARGB;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.BoundingBoxEstimation;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.InvertibleRealTransformSequence;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.realtransform.Scale;
import net.imglib2.realtransform.StackedRealTransform;
import net.imglib2.realtransform.Translation;
import net.imglib2.realtransform.Wrapped2DTransformAs3D;
import net.imglib2.realtransform.inverse.RealTransformFiniteDerivatives;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class BigWarpInit {

	public static final N5MetadataParser<?>[] PARSERS = new N5MetadataParser[]{
			new N5CosemMetadataParser(),
			new N5SingleScaleMetadataParser(),
			new CanonicalMetadataParser(),
			new ImagePlusLegacyMetadataParser(),
			new N5GenericSingleScaleMetadataParser()
	};

	public static final N5MetadataParser<?>[] GROUP_PARSERS = new N5MetadataParser[]{
			new OmeNgffV06MetadataParser(),
			new OmeNgffMetadataParser(),
			new N5CosemMultiScaleMetadata.CosemMultiScaleParser(),
			new N5ViewerMultiscaleMetadataParser(),
			new CanonicalMetadataParser(),
			new N5ViewerMultichannelMetadata.N5ViewerMultichannelMetadataParser()
	};

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

	public static void initSetups( final AbstractSpimData< ? > spimData, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< ? > > sources )
	{
		BigDataViewer.initSetups( spimData, converterSetups, sources );
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static void initSetup( final Source< ? > src, final int setupId, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< ? > > sources )
	{
		final Object type = src.getType();
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

		ValuePair<ARGB, VolatileARGB> converters = initColorConverterARGB();
		final ScaledARGBConverter.ARGB converter = converters.getA();
		final ScaledARGBConverter.VolatileARGB vconverter = converters.getB();

		sources.add( soc );
		converterSetups.add( new RealARGBColorConverterSetup( setupId, converter, vconverter ) );
	}

	public static < T extends RealType< T > > void initSourceReal( final Source< T > src, final int setupId, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< ? > > sources )
	{
		final T type = src.getType();
		final RealARGBColorConverter< T > converter = initColorConverterReal( type );

		final SourceAndConverter< T > soc = new SourceAndConverter<>( src, converter );

		sources.add( soc );
		converterSetups.add( new RealARGBColorConverterSetup( setupId, converter ) );
	}

	private static <T extends RealType<T>> RealARGBColorConverter<T> initColorConverterReal(T type) {

		final double typeMin = Math.max(0, Math.min(type.getMinValue(), 65535));
		final double typeMax = Math.max(0, Math.min(type.getMaxValue(), 65535));
		final RealARGBColorConverter<T> converter = RealARGBColorConverter.create(type, typeMin, typeMax);
		converter.setColor(new ARGBType(0xffffffff));
		return converter;
	}

	private static ValuePair<ScaledARGBConverter.ARGB, ScaledARGBConverter.VolatileARGB> initColorConverterARGB() {

		final ScaledARGBConverter.VolatileARGB vconverter = new ScaledARGBConverter.VolatileARGB(0, 255);
		final ScaledARGBConverter.ARGB converter = new ScaledARGBConverter.ARGB(0, 255);;
		return new ValuePair<>(converter, vconverter);
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
	public static <T> BigWarpData< ? > createBigWarpData( final Source< ? >[] movingSourceList, final Source< ? >[] fixedSourceList, final String[] names )
	{
		final BigWarpData data = initData();
		int setupId = 0;

		// moving
		for (final Source<?> mvgSource : movingSourceList)
			add(data, createSources(data, (Source<T>) mvgSource, setupId++, true));

		// target
		for (final Source<?> fxdSource : fixedSourceList)
			add(data, createSources(data, (Source<T>) fxdSource, setupId++, false));

		// set names
		final ArrayList wrappedSources = wrapSourcesAsRenamable( data.sources, names );
		final AtomicInteger sourceInfoIdx = new AtomicInteger();
		data.sources = wrappedSources;

		final BigWarpData< ? > typedData = data;
		typedData.sourceInfos.forEach( ( id, info ) -> {
			info.setSourceAndConverter( typedData.sources.get( sourceInfoIdx.getAndIncrement() ) );
		} );

		return data;
	}

	/**
	 * Add images from an {@link ImagePlus} as sources for BigWarp. Each channel will be added as its own source.
	 *
	 * @param <T> the type
	 * @param bwdata the bigwarp data
	 * @param ip an ImagePlus
	 * @param setupId id
	 * @param numTimepoints the number of timepoints
	 * @param isMoving true for moving sources
	 * @return the number of sources
	 *
	 * @deprecated Use {@code createSources(BigWarpData,ImagePlus,int,int,boolean)} instead, and pass output to
	 *             {@code add(BigWarpData,LinkedHashMap)}
	 */
	@Deprecated
	public static < T > int add( BigWarpData< T > bwdata, ImagePlus ip, int setupId, int numTimepoints, boolean isMoving )
	{
		final LinkedHashMap< Source< T >, SourceInfo > sources = createSources( bwdata, ip, setupId, numTimepoints, isMoving );
		add( bwdata, sources );
		return sources.size();
	}

	public static < T > LinkedHashMap< Source< T >, SourceInfo > createSources( BigWarpData< T > bwdata, ImagePlus ip, int setupId, int numTimepoints, boolean isMoving )
	{
		final ImagePlusLoader loader = new ImagePlusLoader( ip );
		final SpimDataMinimal[] dataList = loader.loadAll( setupId );

		final LinkedHashMap< Source< T >, SourceInfo > sourceInfoMap = new LinkedHashMap<>();
		for ( final SpimDataMinimal data : dataList )
		{
			final LinkedHashMap< Source< T >, SourceInfo > map = createSources( bwdata, data, setupId, isMoving );
			sourceInfoMap.putAll( map );
			setupId += map.values().stream().map( SourceInfo::getId ).max( Integer::compare ).orElseGet( () -> 0 );
		}

		sourceInfoMap.forEach( ( sac, state ) -> {
			loader.update( state );
			state.setUriSupplier( () -> {
				final FileInfo originalFileInfo = ip.getOriginalFileInfo();
				if ( originalFileInfo != null )
				{
					final String url = originalFileInfo.url;
					if ( url != null && !url.isEmpty() )
					{
						return url;
					}
					else
					{
						return originalFileInfo.getFilePath();
					}
				}
				return null;
			} );
		} );

		for ( final Map.Entry< Source< T >, SourceInfo > sourceSourceInfoEntry : sourceInfoMap.entrySet() )
		{
			sourceSourceInfoEntry.getValue().setSerializable( true );
			/* Always break after the first */
			break;
		}
		return sourceInfoMap;
	}

	/**
	 * Initialize BigWarp.
	 * 
	 * @param <T> the type
	 * @param bwdata
	 *            a BigWarpData instance
	 * @param src
	 *            the Source to add
	 * @param setupId
	 *            the id to assign to the source
	 * @param numTimepoints
	 *            number of time points this source has
	 * @param isMoving
	 *            true if this is a moving source
	 * @return a {@link BigWarpData} instance
	 *
	 * @deprecated Use the output from one of the
	 *             {{@link #createSources(BigWarpData, String, int, boolean)}}
	 *             to call {{@link #add(BigWarpData, LinkedHashMap)}} instead
	 */
	@Deprecated
	public static < T > BigWarpData< T > add( BigWarpData< T > bwdata, Source< T > src, int setupId, int numTimepoints, boolean isMoving )
	{
		return add( bwdata, createSources( bwdata, src, setupId, isMoving));
	}

	public static < T > BigWarpData< T > add( BigWarpData< T > bwdata, Source< T > source, SourceInfo sourceInfo )
	{
		return add( bwdata, source, sourceInfo );
	}

	public static < T > BigWarpData< T > add( BigWarpData< T > bwdata, Source< T > source, SourceInfo sourceInfo, RealTransform transform, Supplier<String> transformUriSupplier )
	{
		final LinkedHashMap< Source< T >, SourceInfo > sourceToInfo = new LinkedHashMap<>();
		sourceToInfo.put( source, sourceInfo );
		return add( bwdata, sourceToInfo, transform, transformUriSupplier );
	}

	public static < T > BigWarpData< T > add( BigWarpData< T > bwdata, LinkedHashMap< Source< T >, SourceInfo > sources )
	{
		add( bwdata, sources, null, null );
		return bwdata;
	}

	/**
	 * Initialize BigWarp.
	 *
	 * @param <T> the type
	 * @param bwdata
	 *            a BigWarpData instance
	 * @param src
	 *            the Source to add
	 * @param setupId
	 *            the id to assign to the source
	 * @param numTimepoints
	 *            number of time points this source has
	 * @param isMoving
	 *            true if this is a moving source
	 * @param transform
	 *            a fixed transform to apply to this source
	 * @return a {@link BigWarpData} instance
	 *
	 * @deprecated Use the output from one of the
	 *             {{@code createSources(BigWarpData, String, int, boolean)}} to
	 *             call {{@code add(BigWarpData, LinkedHashMap, RealTransform)}}
	 *             instead
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Deprecated
	public static < T > BigWarpData< T > add( BigWarpData bwdata, Source< T > src, int setupId, int numTimepoints, boolean isMoving, RealTransform transform )
	{
		final LinkedHashMap<Source<T>, SourceInfo> info = createSources(bwdata, src, numTimepoints, isMoving);
		add( bwdata, info, transform, null );
		return bwdata;
	}

	public static < T > BigWarpData< T > add( BigWarpData< T > bwdata, LinkedHashMap< Source< T >, SourceInfo > sources, RealTransform transform, Supplier<String> transformUriSupplier )
	{
		for( Entry<Source<T>, SourceInfo> entry : sources.entrySet() ) {

			final Source<T> source = entry.getKey();
			final SourceInfo info = entry.getValue();

			// some initializers set the SourceAndConverter, some do not
			if( info.getSourceAndConverter() == null ) {
				addSourceToListsGenericType( source, info.getId(), bwdata.converterSetups, bwdata.sources );
				final SourceAndConverter< T > addedSource = bwdata.sources.get( bwdata.sources.size() - 1 );
				info.setSourceAndConverter( addedSource );
			}

			if ( transform != null )
				info.setTransform( transform, transformUriSupplier );

			bwdata.sourceInfos.put( info.getId(), info );
		}
		return bwdata;
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public static < T > LinkedHashMap< Source< T >, SourceInfo > createSources( BigWarpData bwdata, Dataset data, int baseId, final boolean isMoving )
	{
		boolean first = true;
		final LinkedHashMap< Source< T >, SourceInfo > sourceInfoMap = new LinkedHashMap<>();

		final AffineTransform3D res = datasetResolution( data );
		final long nc = data.getChannels();
		boolean hasZ = false;

		final CalibratedAxis[] axes = new CalibratedAxis[ data.numDimensions() ];
		data.axes( axes );
		for (int i = 0; i < data.numDimensions(); i++) {
			if (axes[i].type().equals(Axes.Z))
			{
				hasZ = true;
				break;
			}
		}

		if ( nc > 1 )
		{
			int channelIdx = -1;
			for ( int i = 0; i < data.numDimensions(); i++ )
			{
				if ( axes[ i ].type().equals( Axes.CHANNEL ) )
				{
					channelIdx = i;
					break;
				}
			}

			for ( int c = 0; c < nc; c++ )
			{
				final IntervalView<RealType<?>> channelRaw = Views.hyperSlice( data, channelIdx, c );
				final IntervalView<RealType<?>> channel = hasZ ? channelRaw : Views.addDimension( channelRaw, 0, 0 );

				final RandomAccessibleIntervalSource source = new RandomAccessibleIntervalSource( channel, data.getType(), res, data.getName() );
				final SourceInfo info = new SourceInfo( baseId + c, isMoving, data.getName(), () -> data.getSource() );
				info.setSerializable( first );
				if ( first )
					first = false;

				sourceInfoMap.put( source, info );
			}
		}
		else
		{
			final RandomAccessibleInterval<RealType<?>> img = hasZ ? data : Views.addDimension( data, 0, 0 );

			final RandomAccessibleIntervalSource source = new RandomAccessibleIntervalSource( img, data.getType(), res, data.getName() );
			final SourceInfo info = new SourceInfo( baseId, isMoving, data.getName(), () -> data.getSource() );
			info.setSerializable( true );
			sourceInfoMap.put( source, info );
		}

		return sourceInfoMap;
	}

	public static AffineTransform3D datasetResolution( Dataset data )
	{
		final AffineTransform3D affine = new AffineTransform3D();
		final CalibratedAxis[] axes = new CalibratedAxis[ data.numDimensions() ];
		data.axes( axes );

		for ( int d = 0; d < data.numDimensions(); d++ )
		{
			if ( axes[ d ].type().equals( Axes.X ) )
				affine.set( axes[ d ].calibratedValue( 1 ), 0, 0 );
			else if ( axes[ d ].type().equals( Axes.Y ) )
				affine.set( axes[ d ].calibratedValue( 1 ), 1, 1 );
			else if ( axes[ d ].type().equals( Axes.Z ) )
				affine.set( axes[ d ].calibratedValue( 1 ), 2, 2 );
		}
		return affine;
	}

	@SuppressWarnings( { "rawtypes" } )
	public static < T > LinkedHashMap< Source< T >, SourceInfo > createSources( BigWarpData bwdata, AbstractSpimData< ? > data, int baseId, final boolean isMoving )
	{
		final List< SourceAndConverter< ? > > tmpSources = new ArrayList<>();
		final List< ConverterSetup > tmpConverterSetups = new ArrayList<>();
		initSetups( data, tmpConverterSetups, tmpSources );

		final LinkedHashMap< Source< T >, SourceInfo > sourceInfoMap = new LinkedHashMap<>();
		int setupId = baseId;
		for ( final SourceAndConverter sac : tmpSources )
		{
			@SuppressWarnings("unchecked")
			final Source< T > source = sac.getSpimSource();
			sourceInfoMap.put( source, new SourceInfo( setupId++, isMoving, source.getName() ) );
		}

		return sourceInfoMap;
	}

	public static < T > LinkedHashMap< Source< T >, SourceInfo > createSources( BigWarpData<?> bwdata, Source< T > src, int baseId, final boolean isMoving )
	{
		final LinkedHashMap< Source< T >, SourceInfo > sourceInfoMap = new LinkedHashMap<>();
		sourceInfoMap.put( src, new SourceInfo( baseId, isMoving, src.getName() ) );
		return sourceInfoMap;
	}

	@SuppressWarnings("unchecked")
	private static <T extends NumericType<T> & NativeType<T>, V extends NumericType<V> & NativeType<V>> List<Pair<Source<T>, Source<V>>> createSource(
			final T type,
			final String srcName,
			final RandomAccessibleInterval<T>[] images,
			final AffineTransform3D[] transforms,
			final SharedQueue sharedQueue,
			final VoxelDimensions vd) {

		final long nChannels = images[0].dimension(2);

		final ArrayList<Pair<Source<T>, Source<V>>> sourcePairs = new ArrayList<>();
		for ( int c = 0; c < nChannels; ++c ) {

			final RandomAccessibleInterval<T>[] channels = new RandomAccessibleInterval[images.length];
			for (int level = 0; level < images.length; ++level)
				channels[level] = Views.hyperSlice(images[level], 2, c);

			final RandomAccessibleIntervalMipmapSource4D<T> source = new RandomAccessibleIntervalMipmapSource4D<>(
					channels, type, transforms, vd, srcName, true);

			// TODO fix generics
			final ValuePair<Source<T>, Source<V>> pair = new ValuePair(
					source,
					source.asVolatile(sharedQueue));
			sourcePairs.add(pair);
		}
		return sourcePairs;
	}


	private static String schemeSpecificPartWithoutQuery( URI uri )
	{
		return uri.getSchemeSpecificPart().replaceAll( "\\?" + uri.getQuery(), "" ).replaceAll( "//", "" );
	}

	public static GsonBuilder gsonBuilder() {

		return new GsonBuilder()
				.registerTypeHierarchyAdapter(CoordinateTransform.class, NameConfigAdapter.getJsonAdapter("type", CoordinateTransform.class))
				.registerTypeAdapter(OmeNgffV06MultiScaleMetadata.class, new OmeNgffV06MultiScaleMetadataAdapter());
	}

	public static < T > LinkedHashMap< Source< T >, SourceInfo > createSources( final BigWarpData< T > bwData, String uri, int setupId, boolean isMoving ) throws URISyntaxException, IOException, SpimDataException
	{
		final SharedQueue sharedQueue = BigWarpData.getSharedQueue();
		final URI encodedUri = N5URI.encodeAsUri( uri.trim() );
		final LinkedHashMap< Source< T >, SourceInfo > sourceStateMap = new LinkedHashMap<>();
		if ( encodedUri.isOpaque() )
		{
			final N5URI n5URL = new N5URI( encodedUri.getSchemeSpecificPart() );
			final String firstScheme = encodedUri.getScheme().toLowerCase();
			final N5Reader n5reader;
			switch ( firstScheme.toLowerCase() )
			{
			case "n5":
				n5reader = new N5Factory().gsonBuilder(gsonBuilder()).openReader( n5URL.getContainerPath() );
				break;
			case "zarr3":
				n5reader = new N5Factory().gsonBuilder(gsonBuilder()).openReader( n5URL.getContainerPath() );
				break;
			case "zarr":
				n5reader = new N5ZarrReader( n5URL.getContainerPath() );
				break;
			case "h5":
			case "hdf5":
			case "hdf":
				n5reader = new N5HDF5Reader( n5URL.getContainerPath() );
				break;
			default:
				throw new URISyntaxException( firstScheme, "Unsupported Top Level Protocol" );
			}

			LinkedHashMap<Source<T>, SourceInfo> newSources = loadN5SourceInfo(bwData, n5reader, n5URL.getGroupPath(), sharedQueue, setupId, isMoving );
			sourceStateMap.putAll(newSources);
		}
		else
		{
			final N5URI n5URL = new N5URI( encodedUri );
			try
			{
				final String containerWithoutN5Scheme = n5URL.getContainerPath().replaceFirst( "^n5://", "" );
				final N5Reader n5reader = new N5Factory().gsonBuilder(gsonBuilder()).openReader( containerWithoutN5Scheme );
				LinkedHashMap<Source<T>, SourceInfo> newSources = loadN5SourceInfo(bwData, n5reader, n5URL.getGroupPath(), sharedQueue, setupId, isMoving );
				sourceStateMap.putAll(newSources);
			}
			catch ( final Exception ignored )
			{}

			if ( sourceStateMap.isEmpty() )
			{
				final String containerPath = n5URL.getContainerPath();
				if ( containerPath.trim().toLowerCase().endsWith( ".xml" ) )
				{
					sourceStateMap.putAll( createSources( bwData, isMoving, setupId, containerPath, n5URL.getGroupPath() ) );
				}
				else
				{
					final ImagePlus ijp;
					if ( Objects.equals( encodedUri.getScheme(), "imagej" ) )
					{
						final String title = n5URL.getContainerPath().replaceAll( "^imagej:(///|//)", "" );
						IJ.selectWindow( title );
						ijp = IJ.getImage();
					}
					else if ( new File( uri ).isDirectory() )
					{
						ijp = FolderOpener.open( uri );
					}
					else
					{
						ijp = IJ.openImage( uri.trim() );
					}
					sourceStateMap.putAll( createSources( bwData, ijp, setupId, 0, isMoving ) );
				}
			}

		}

		/*
		 * override any already set urls with the uri we used to load this
		 * source.
		 */
		sourceStateMap.forEach( ( source, state ) -> {
			state.setUriSupplier( () -> uri );
		} );
		for ( final Map.Entry< Source< T >, SourceInfo > sourceSourceInfoEntry : sourceStateMap.entrySet() )
		{
			sourceSourceInfoEntry.getValue().setSerializable( true );
			/* Always break after the first */
			break;
		}
		return sourceStateMap;
	}

	/**
	 * Add a source to thet given {@link BigWarpData} instance using the N5 api
	 *
	 * @param <T> the type
	 * @param bwdata the BigWarpData instance
	 * @param isMoving true if this source is moving
	 * @param setupId the id to assign to this source
	 * @param rootPath the root path to the container
	 * @param dataset the path to the dataset relative to the container root
	 * @return a {@link SpimData} instance
	 * 
	 * 	 * @deprecated Use output from
	 *             {@code createSources(BigWarpData, boolean, int, String, String)} and add with
	 *             {@code add(BigWarpData, LinkedHashMap, RealTransform)} instead.
	 */
	@Deprecated
	public static < T > SpimData addToData( final BigWarpData bwdata, final boolean isMoving, final int setupId, final String rootPath, final String dataset )
	{
		final AtomicReference< SpimData > returnMovingSpimData = new AtomicReference<>();
		final LinkedHashMap< Source< T >, SourceInfo > sources = createSources( bwdata, isMoving, setupId, rootPath, dataset, returnMovingSpimData );
		add( bwdata, sources );
		return returnMovingSpimData.get();
	}

	public static < T > Map< Source< T >, SourceInfo > createSources( final BigWarpData bwdata, final boolean isMoving, final int setupId, final String rootPath, final String dataset )
	{
		return createSources( bwdata, isMoving, setupId, rootPath, dataset, null );
	}

	private static < T extends NumericType<T> & NativeType<T> > LinkedHashMap< Source< T >, SourceInfo > createSources( final BigWarpData< T > bwdata, final boolean isMoving, final int setupId, final String rootPath, final String dataset, final AtomicReference< SpimData > returnMovingSpimData )
	{
		final SharedQueue sharedQueue = new SharedQueue(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
		if ( rootPath.endsWith( "xml" ) )
		{
			SpimData spimData;
			try
			{
				spimData = new XmlIoSpimData().load( rootPath );
				if ( returnMovingSpimData != null && isMoving )
				{
					returnMovingSpimData.set( spimData );
				}
				final LinkedHashMap< Source< T >, SourceInfo > sources = createSources( bwdata, spimData, setupId, isMoving );

				sources.forEach( ( source, state ) -> {
					state.setUriSupplier( () -> {
						try
						{
							return spimData.getBasePath().getCanonicalPath();
						}
						catch ( final IOException e )
						{
							return null;
						}
					} );
				} );

				for ( final Map.Entry< Source< T >, SourceInfo > sourceSourceInfoEntry : sources.entrySet() )
				{
					sourceSourceInfoEntry.getValue().setSerializable( true );
					/* Always break after the first */
					break;
				}
				return sources;
			}
			catch ( final SpimDataException e )
			{
				e.printStackTrace();
			}
			return null;
		}
		else
		{
			return loadN5SourceInfo(bwdata, rootPath, dataset, sharedQueue, setupId, isMoving);
		}
	}

	public static < T extends NativeType<T> & NumericType<T> > LinkedHashMap< Source< T >, SourceInfo > loadN5SourceInfo( final BigWarpData<?> bwData, final String n5Root, final String n5Dataset, final SharedQueue queue,
			final int sourceId, final boolean moving )
	{
		final N5Reader n5;
		try
		{
			n5 = new N5Factory().gsonBuilder(gsonBuilder()).openReader( n5Root );
		}
		catch ( final RuntimeException e ) {
			e.printStackTrace();
			return null;
		}
		return loadN5SourceInfo( bwData, n5, n5Dataset, queue, sourceId, moving );
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static < T > LinkedHashMap< Source< T >, SourceInfo > loadN5SourceInfo( final BigWarpData bwData, final N5Reader n5, final String n5Dataset, final SharedQueue queue, 
			final int sourceId, final boolean moving )
	{

		N5Metadata meta = null;
		try
		{
			final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer( n5, N5DatasetDiscoverer.fromParsers( PARSERS ), N5DatasetDiscoverer.fromParsers( GROUP_PARSERS ) );
			final N5TreeNode node = discoverer.discoverAndParseRecursive("");
			meta = node.getDescendant(n5Dataset).map(N5TreeNode::getMetadata).orElse(null);
		}
		catch ( final IOException e )
		{}

		final List<SourceAndConverter<?>> sources;
		CoordinateSystem cs = null;
		if( meta instanceof OmeNgffV06MultiScaleMetadata ) {
			sources = openNgff06DevSourceAndConverter((BigWarpData)bwData, n5, (OmeNgffV06MultiScaleMetadata)meta, queue);
			cs = ((OmeNgffV06MultiScaleMetadata)meta).getCoordinateSystems()[0]; // TODO eventually don't use first one
		}
		else
			sources = openN5VSourceAndConverter(bwData, n5, meta, queue);

		final LinkedHashMap< Source<T>, SourceInfo > sourceStateMap = new LinkedHashMap<>();
		for( SourceAndConverter<?> sac : sources ) {
			if( bwData != null ) {
				bwData.sources.add((SourceAndConverter)sac);
				bwData.converterSetups.add( BigDataViewer.createConverterSetup(sac, sourceId));
			}

			final String uri = n5.getURI().toString() + "$" + n5Dataset;
			final SourceInfo info = new SourceInfo(sourceId, moving, sac.getSpimSource().getName(), () -> uri );
			info.setSourceAndConverter(sac);
			if (cs != null)
				info.setCoordinateSystem(cs);

			sourceStateMap.put((Source<T>)sac.getSpimSource(), info);
		}
		return sourceStateMap;
	}

	@Deprecated
	public static < T extends NumericType<T> & NativeType<T>> Source< T > loadN5Source( final String n5Root, final String n5Dataset, final SharedQueue queue )
	{
		final N5Reader n5;
		try
		{
			n5 = new N5Factory().gsonBuilder(gsonBuilder()).openReader( n5Root );
		}
		catch ( final RuntimeException e ) {
			e.printStackTrace();
			return null;
		}
		return loadN5Source( n5, n5Dataset, queue );
	}

	@SuppressWarnings("unchecked")
	@Deprecated
	public static < T extends NativeType<T>> Source< T > loadN5Source( final N5Reader n5, final String n5Dataset, final SharedQueue queue )
	{

		N5Metadata meta = null;
		try
		{
			final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer( n5, N5DatasetDiscoverer.fromParsers( PARSERS ), N5DatasetDiscoverer.fromParsers( GROUP_PARSERS ) );
			final N5TreeNode node = discoverer.discoverAndParseRecursive("");
			meta = node.getDescendant(n5Dataset).map(N5TreeNode::getMetadata).orElse(null);
		}
		catch ( final IOException e )
		{}

		if ( meta instanceof MultiscaleMetadata )
		{
			return (Source<T>)openN5V( n5, ( MultiscaleMetadata< ? > ) meta, queue);
		}
		else
		{
			return openAsSource( n5, meta, queue, true );
		}
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static < T extends NativeType<T>, M extends N5Metadata > Source< T > openAsSource( final N5Reader n5, final M meta, final SharedQueue sharedQueue, final boolean isVolatile )
	{
		final RandomAccessibleInterval imageRaw;
		final RandomAccessibleInterval image;
		if( meta == null )
			return null;

		try
		{
			if ( isVolatile )
			{
				final CachedCellImg<T, ?> rai = N5Utils.openVolatile( n5, meta.getPath() );
				imageRaw = to3d( rai );
			}
			else
				imageRaw = to3d( N5Utils.open( n5, meta.getPath() ) );

			if ( meta instanceof N5ImagePlusMetadata && ( ( N5ImagePlusMetadata ) meta ).getType() == ImagePlus.COLOR_RGB && imageRaw.getType() instanceof UnsignedIntType )
			{
				image = toColor( imageRaw );
			}
			else
				image = imageRaw;

			if ( meta instanceof SpatialMetadata )
			{
				final String unit = ( ( SpatialMetadata ) meta ).unit();
				final AffineTransform3D srcXfm = ( ( SpatialMetadata ) meta ).spatialTransform3d();
				final FinalVoxelDimensions voxelDims = new FinalVoxelDimensions( unit, new double[] { srcXfm.get( 0, 0 ), srcXfm.get( 1, 1 ), srcXfm.get( 2, 2 ) } );

				return new BwRandomAccessibleIntervalSource( image, ( NumericType ) image.getType(), srcXfm, meta.getPath(), voxelDims );
			}
			else
				return new BwRandomAccessibleIntervalSource( image, ( NumericType ) image.getType(), new AffineTransform3D(), meta.getPath() );
		}
		catch ( final RuntimeException e )
		{
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Use openN5VSourceAndConverter instead
	 * 
	 * @param <T> source type
	 * @param n5 the n5 reader
	 * @param multiMeta the multiscale metadata
	 * @param sharedQueue the shared queue
	 * @return a source
	 */
	@Deprecated
	public static <T extends NumericType<T> & NativeType<T>> List<SourceAndConverter<T>> openN5V( final N5Reader n5, final MultiscaleMetadata<?> multiMeta,
			final SharedQueue sharedQueue) {

		return openN5VSourceAndConverter(null, n5, multiMeta, sharedQueue);
	}

	public static <T extends NativeType<T> & NumericType<T> > List<SourceAndConverter<T>> openN5VSourceAndConverter(
			final BigWarpData<T> bwData,
			final N5Reader n5,
			final N5Metadata multiMeta,
			final SharedQueue sharedQueue) {

		final List<SourceAndConverter<T>> sources = new ArrayList<>();
		final List<ConverterSetup> converterSetups = new ArrayList<>();
		try {
			N5Viewer.buildN5Sources(n5, new DataSelection(n5, Collections.singletonList(multiMeta)), sharedQueue, converterSetups, sources,
					BdvOptions.options());

			return sources;
		} catch (final IOException e) {}

		return null;
	}

	public static <T extends NativeType<T> & NumericType<T>, V extends Volatile<T> & NumericType<V>> List<SourceAndConverter<T>> 
		openNgff06DevSourceAndConverter(
			final BigWarpData<T> bwData,
			final N5Reader n5,
			final OmeNgffV06MultiScaleMetadata metadata,
			final SharedQueue sharedQueue) {

		final String srcName = metadata.getName();
		final String[] datasetsToOpen = metadata.getPaths();
		final AffineTransform3D[] transforms = new AffineTransform3D[datasetsToOpen.length];
		final String unit = metadata.getCoordinateSystems()[0].getAxis(0).getUnit();

		if (datasetsToOpen == null || datasetsToOpen.length == 0)
			throw new N5Exception("No datasets");

		T type = null;
		boolean isApplied = false;

		@SuppressWarnings("rawtypes")
		final RandomAccessibleInterval[] images = new RandomAccessibleInterval[datasetsToOpen.length];
		for (int s = 0; s < images.length; ++s) {

			final String dsetPath = metadata.getPath() + "/" + datasetsToOpen[s];

			@SuppressWarnings("unchecked")
			final RandomAccessibleInterval<T> img = (RandomAccessibleInterval<T>)N5Utils.openVolatile(n5, dsetPath);
			type = type == null ? img.getType() : type;

			RandomAccessibleInterval<T> imagejImg = AxisUtils.permuteForImagePlus(img, metadata);

			images[s] = imagejImg;

			// only use first ct for now
			final CoordinateTransform<?>[] cts = metadata.getDatasets()[s].coordinateTransformations;

			final AffineTransform3D tform = TransformUtils.toAffine3D(n5, Collections.singleton(cts[0]));
			if (tform != null) {
				isApplied = true;
				transforms[s] = tform;
			}
			else
				transforms[s] = new AffineTransform3D();
		}

		// this could / should be generalized
		final double rx = transforms[0].get(0, 0);
		final double ry = transforms[0].get(1, 1);
		final double rz = transforms[0].get(2, 2);

		/* there still can be many channels */
		@SuppressWarnings("unchecked")
		final List<Pair<Source<T>, Source<V>>> sourcePairs = createSource(
				type,
				srcName,
				images,
				transforms,
				sharedQueue,
				new FinalVoxelDimensions(unit, rx, ry, rz));


		// assuming for now that only one source is opened
		if (!isApplied) {

			System.out.println("apply non-affine");
			final CoordinateSystem cs = metadata.coordinateSystems[0];
			final String to = cs.getName();

			OmeNgffDataset dset = metadata.getDatasets()[0];
			final CoordinateTransform<?>[] cts = dset.coordinateTransformations;

			final String from = dset.path;
			final RealTransform tform = getNonAffineTransformForSource(from, to, n5, cts[0]);

			if( tform != null ) {

				final Source<T> src = sourcePairs.get(0).getA();
				final Source<V> vsrc = sourcePairs.get(0).getB();

				final WarpedSource<T> wsrc = new WarpedSource<T>(src, "");
				wsrc.updateTransform(tform);
				wsrc.setIsTransformed(true);

				final WarpedSource<V> wvsrc = new WarpedSource<V>(vsrc, "");
				wvsrc.updateTransform(tform);
				wvsrc.setIsTransformed(true);

				sourcePairs.clear();
				sourcePairs.add(new ValuePair(wsrc,wvsrc));
			}
			else {
				System.err.println("Could not find appropriate transform.");
			}
		}

		final List<SourceAndConverter<T>> sources = new ArrayList<>();
		final List<ConverterSetup> converterSetups = new ArrayList<>();

		int setupId = 1;
		for (final Pair<Source<T>, Source<V>> sourcePair : sourcePairs) {
			addSourceToListsNumericType( sourcePair.getA(), sourcePair.getB(),
					setupId++, converterSetups, sources);
		}

		return sources;
	}

	protected static RealTransform getNonAffineTransformForSource(
			final String input,
			final String output,
			final N5Reader n5,
			CoordinateTransform<?> ct ) {

		if ( ct == null )
			return null;

		RealTransform tf;
		if (isInverse(input, output, ct)) {
			return ensure3D(ct.getTransform(n5));
		}
		else if (isForward(input, output, ct)) {

			RealTransform tmp = ct.getTransform(n5);
			if( ct instanceof InverseCoordinateTransform )
				return ensure3D(tmp);
			else if( tmp instanceof InvertibleRealTransform )
				return ensure3D(((InvertibleRealTransform) tmp).inverse());
			else {
				// this branch is terrible for performance, but should be "correct"
				RealTransform tform3d = ensure3D(tmp);
				RealTransformFiniteDerivatives tformDiff = new RealTransformFiniteDerivatives(tform3d);

				WrappedIterativeInvertibleRealTransform invTform = new WrappedIterativeInvertibleRealTransform<>(tformDiff);
				return invTform.inverse();
			}
		}

		return null;
	}

	protected static boolean isForward(final String input, final String output, CoordinateTransform<?> ct) {
		return ct.getInput().equals(input) && ct.getOutput().equals(output);
	}

	protected static boolean isInverse(final String input, final String output, CoordinateTransform<?> ct) {
		return ct.getOutput().equals(input) && ct.getInput().equals(output);
	}

	protected static RealTransform ensure3D(RealTransform tform) {

		if (tform.numSourceDimensions() == 3)
			return tform;
		else if (tform.numSourceDimensions() == 2)
			return new Wrapped2DTransformAs3D(tform);
		else if (tform.numSourceDimensions() == 1)
			return new StackedRealTransform(tform, new Scale(1, 1));
		else
			return null;
	}

	@SuppressWarnings("unchecked")
	public static < T extends NumericType<T> & NativeType<T> > Source< T > openAsSourceMulti( final N5Reader n5, final MultiscaleMetadata< ? > multiMeta, final SharedQueue sharedQueue, final boolean isVolatile )
	{
		final String[] paths = multiMeta.getPaths();
		final AffineTransform3D[] transforms = multiMeta.spatialTransforms3d();
		final String unit = multiMeta.units()[ 0 ];

		@SuppressWarnings( "rawtypes" )
		final RandomAccessibleInterval[] images = new RandomAccessibleInterval[ paths.length ];
		final double[][] mipmapScales = new double[ images.length ][ 3 ];
		final CacheHints cacheHints = new CacheHints(LoadingStrategy.VOLATILE, 0, true);
		for ( int s = 0; s < images.length; ++s )
		{
			try
			{
				if ( isVolatile )
				{
					final CachedCellImg<T, ?> rai = N5Utils.openVolatile( n5, paths[ s ] );
					images[ s ] = to3d( VolatileViews.wrapAsVolatile( rai, sharedQueue, cacheHints) );
				}
				else
					images[ s ] = to3d( N5Utils.open( n5, paths[ s ] ) );
			}
			catch ( final RuntimeException e )
			{
				e.printStackTrace();
			}

			mipmapScales[ s ][ 0 ] = transforms[ s ].get( 0, 0 );
			mipmapScales[ s ][ 1 ] = transforms[ s ].get( 1, 1 );
			mipmapScales[ s ][ 2 ] = transforms[ s ].get( 2, 2 );
		}

		return new RandomAccessibleIntervalMipmapSource<T>( images, (T)images[ 0 ].getType(), mipmapScales, new mpicbg.spim.data.sequence.FinalVoxelDimensions( unit, mipmapScales[ 0 ] ), new AffineTransform3D(), multiMeta.getPaths()[ 0 ] + "_group" );
	}

	private static RandomAccessibleInterval< ? > to3d( RandomAccessibleInterval< ? > img )
	{
		if ( img.numDimensions() == 2 )
			return Views.addDimension( img, 0, 0 );
		else
			return img;
	}

	private static RandomAccessibleInterval< ARGBType > toColor( RandomAccessibleInterval< UnsignedIntType > img )
	{
		return Converters.convertRAI( img, new Converter< UnsignedIntType, ARGBType >()
		{
			@Override
			public void convert( UnsignedIntType input, ARGBType output )
			{
				output.set( input.getInt() );
			}
		}, new ARGBType() );
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <T> BigWarpData<T> initData() {

		return new BigWarpData();
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
	private static < T > void addSourceToListsGenericType( final Source< T > source, final int setupId, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< T > > sources )
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
	private static < T extends NumericType< T > > void addSourceToListsNumericType( final Source< T > source, final int setupId, final List< ConverterSetup > converterSetups, final List< SourceAndConverter< T > > sources )
	{
		final T type = source.getType();
		final SourceAndConverter< T > soc = BigDataViewer.wrapWithTransformedSource( new SourceAndConverter<>( source, BigDataViewer.createConverterToARGB( type ) ) );
		converterSetups.add( BigDataViewer.createConverterSetup( soc, setupId ) );
		sources.add( soc );
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
	 *            id of the new source for use in {@code SetupAssignments}.
	 * @param converterSetups
	 *            list of {@link ConverterSetup}s to which the source should be
	 *            added.
	 * @param sources
	 *            list of {@link SourceAndConverter}s to which the source should
	 *            be added.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static <T, V extends Volatile<T>> void addSourceToListsGenericType(
			final Source<T> source,
			final Source<V> volatileSource,
			final int setupId,
			final List<ConverterSetup> converterSetups,
			final List<SourceAndConverter<T>> sources) {

		final T type = source.getType();
		if (type instanceof RealType || type instanceof ARGBType || type instanceof VolatileARGBType)
			addSourceToListsNumericType(
					(Source)source,
					(Source)volatileSource,
					setupId,
					converterSetups,
					(List)sources);
		else
			throw new IllegalArgumentException("Unknown source type. Expected RealType, ARGBType, or VolatileARGBType");
	}

	/**
	 * Add the given {@code source} to the lists of {@code converterSetups}
	 * (using specified {@code setupId}) and {@code sources}. For this, the
	 * {@code source} is wrapped with an appropriate {@link Converter} to
	 * {@link ARGBType} and into a {@link TransformedSource}.
	 *
	 * @param source
	 *            source to add.
	 * @param volatileSource
	 *            corresponding volatile source.
	 * @param setupId
	 *            id of the new source for use in {@code SetupAssignments}.
	 * @param converterSetups
	 *            list of {@link ConverterSetup}s to which the source should be
	 *            added.
	 * @param sources
	 *            list of {@link SourceAndConverter}s to which the source should
	 *            be added.
	 */
	private static <T extends NumericType<T>, V extends Volatile<T> & NumericType<V>> void addSourceToListsNumericType(
			final Source<T> source,
			final Source<V> volatileSource,
			final int setupId,
			final List<ConverterSetup> converterSetups,
			final List<SourceAndConverter<T>> sources) {

		final SourceAndConverter<V> vsoc = (volatileSource == null)
				? null
				: new SourceAndConverter<>(volatileSource, createConverterToARGB(volatileSource.getType()));
		final SourceAndConverter<T> soc = new SourceAndConverter<>(
				source,
				createConverterToARGB(source.getType()),
				vsoc);
		final SourceAndConverter<T> tsoc = wrapWithTransformedSource(soc);

		converterSetups.add(BigDataViewer.createConverterSetup(tsoc, setupId));
		sources.add(tsoc);
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
	public static BigWarpData< ? > createBigWarpData( final AbstractSpimData< ? >[] spimDataPList, final AbstractSpimData< ? >[] spimDataQList, final String[] names )
	{
		final ArrayList< ConverterSetup > converterSetups = new ArrayList< ConverterSetup >();
		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();

		int numMovingSources = 0;
		for ( final AbstractSpimData< ? > spimDataP : spimDataPList )
		{
			numMovingSources += spimDataP.getSequenceDescription().getViewSetups().size();
			BigDataViewer.initSetups( spimDataP, converterSetups, sources );
		}

		int numTargetSources = 0;
		for ( final AbstractSpimData< ? > spimDataQ : spimDataQList )
		{
			numTargetSources += spimDataQ.getSequenceDescription().getViewSetups().size();
			BigDataViewer.initSetups( spimDataQ, converterSetups, sources );
		}

		final int[] movingSourceIndices = ImagePlusLoader.range( 0, numMovingSources );
		final int[] targetSourceIndices = ImagePlusLoader.range( numMovingSources, numTargetSources );

		if ( names != null && names.length == sources.size() )
		{
			return new BigWarpData( wrapSourcesAsRenamable( sources, names ), converterSetups, null, movingSourceIndices, targetSourceIndices );
		}
		else
		{
			return new BigWarpData( sources, converterSetups, null, movingSourceIndices, targetSourceIndices );
		}
	}

	public static ArrayList<SourceAndConverter<?>> wrapSourcesAsRenamable(final List<SourceAndConverter<?>> sources,
			final String[] names) {

		final ArrayList<SourceAndConverter<?>> wrappedSource = new ArrayList<SourceAndConverter<?>>();

		int i = 0;
		for (final SourceAndConverter<?> sac : sources) {
			final SourceAndConverter<?> renamableSource = wrapSourceAsRenamable(sac);
			if (names != null) {
				((RenamableSource<?>)renamableSource.getSpimSource()).setName(names[i]);
			}
			wrappedSource.add(renamableSource);
			i++;
		}
		return wrappedSource;
	}

	private static <T> SourceAndConverter<T> wrapSourceAsRenamable(final SourceAndConverter<T> src) {

		if (src.asVolatile() == null) {
			return new SourceAndConverter<T>(new RenamableSource<T>(src.getSpimSource()), src.getConverter(), null);
		} else {
			return new SourceAndConverter<T>(new RenamableSource<T>(src.getSpimSource()), src.getConverter(),
					src.asVolatile());
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends NumericType<T> & NativeType<T>, V extends Volatile<T> & NumericType<V>> SourceAndConverter<T> cacheTransformedSource(
			final BigWarpData data,
			final int index,
			final SourceAndConverter<T> sac,
			final InvertibleRealTransform transform,
			SharedQueue sharedQueue) {


		final SourceInfo info = data.getSourceInfo(sac);
		final Source<T> src = sac.getSpimSource();
		final int setupId = info.getId();

		final int nd = src.getSource(0, 0).numDimensions();
		final int N = src.getNumMipmapLevels();

		final T type = src.getType();
		final V vtype = (V)VolatileTypeMatcher.getVolatileTypeForType(type);

		final int[] defaultCellDimensions = new int[nd];
		Arrays.fill(defaultCellDimensions, 64);

		// TODO handle time?
		final RandomAccessibleInterval<T>[] mipmaps = new RandomAccessibleInterval[N];
		final RandomAccessibleInterval<Volatile<T>>[] vmipmaps = new RandomAccessibleInterval[N];
		final AffineTransform3D[] sourceTransforms = new AffineTransform3D[N];
		for (int i = 0; i < src.getNumMipmapLevels(); i++) {

			final RandomAccessibleInterval<T> img = src.getSource(0, i);

			final AffineTransform3D origScaleTform = new AffineTransform3D();
			src.getSourceTransform(0, i, origScaleTform);

			// build the inverse transform: from transformed pixel coordinates
			// to original pixel coordinates "through" physical coordinates
			InvertibleRealTransformSequence tformToPhysicalSpace = new InvertibleRealTransformSequence();
			tformToPhysicalSpace.add(origScaleTform);
			tformToPhysicalSpace.add(transform.copy());
			tformToPhysicalSpace.add(origScaleTform.inverse());

			// compute the bounding box in physical coordinates
			final BoundingBoxEstimation bbox = new BoundingBoxEstimation();
			final RealInterval targetInterval = bbox.estimateInterval(tformToPhysicalSpace.inverse(), img);
			final Interval pixelInterval = BoundingBoxEstimation.containingInterval(targetInterval);

			// update the source transform to take into account any change in bounding box 
			final AffineTransform3D newSourceTform = origScaleTform.copy();
			final Translation tlation = new Translation(targetInterval.minAsDoubleArray());
			newSourceTform.concatenate(tlation);
			sourceTransforms[i] = newSourceTform;

			InvertibleRealTransformSequence tformToPixelSpace = new InvertibleRealTransformSequence();
			tformToPixelSpace.add(newSourceTform);
			tformToPixelSpace.add(transform.copy());
			tformToPixelSpace.add(origScaleTform.inverse());

			final RandomAccessibleInterval<T> raiTform = transform(img, pixelInterval, tformToPixelSpace);
			final ReadOnlyCachedCellImgFactory cacheFactory = new ReadOnlyCachedCellImgFactory(
					new ReadOnlyCachedCellImgOptions()
							.volatileAccesses(true)
							.cellDimensions(getCellDimensionsOrDefault(img, defaultCellDimensions)));

			final CellLoader<T> copier = new CellLoader<T>() {
				@Override
				public void load(SingleCellArrayImg<T, ?> cell) throws Exception {

					Views.flatIterable(Views.interval(Views.pair(raiTform, cell), cell)).forEach(
							pair -> pair.getB().set(pair.getA()));
				}
			};

			final RandomAccessibleInterval<T> cachedTransformedMipmap = cacheFactory.create(
					raiTform.dimensionsAsLongArray(),
					type.copy(),
					copier);
			mipmaps[i] = cachedTransformedMipmap;

			final int priority = N - 1 - i;
			final CacheHints cacheHints = new CacheHints(LoadingStrategy.BUDGETED, priority, false);
			vmipmaps[i] = VolatileViews.wrapAsVolatile(cachedTransformedMipmap, sharedQueue, cacheHints);
		}

		final RandomAccessibleIntervalMipmapSource<T> cachedTransformedSource =
				new RandomAccessibleIntervalMipmapSource<T>(
						mipmaps,
						type,
						sourceTransforms,
						src.getVoxelDimensions(),
						src.getName() + " (cached transform)",
						src.doBoundingBoxCulling());


		final Source<V> vsrc = new VolatileSource<T, V>(cachedTransformedSource, vtype, sharedQueue);
		final SourceAndConverter<V> vsac = new SourceAndConverter<V>(vsrc, BigDataViewer.createConverterToARGB(vtype));

		final SourceAndConverter<T> tsac = new SourceAndConverter<T>(cachedTransformedSource, BigDataViewer.createConverterToARGB(type), vsac);
		ConverterSetup converterSetup = BigDataViewer.createConverterSetup(tsac, setupId);
		data.converterSetups.set(index, converterSetup);
		return tsac;
	}

	@SuppressWarnings("rawtypes")
	private static int[] getCellDimensionsOrDefault(final RandomAccessibleInterval<?> img,
			final int[] defaultDimensions) {

		// TODO need to unwrap views
		if (img instanceof AbstractCellImg) {
			return ((AbstractCellImg)img).getCellGrid().getCellDimensions();
		} else {
			return defaultDimensions;
		}
	}

	private static <T extends NumericType<T> & NativeType<T>> RandomAccessibleInterval<T> transform(
			final RandomAccessibleInterval<T> img,
			final Interval targetInterval,
			final RealTransform transform) {

		final T background = img.getType().copy();
		background.setZero();

		return Views.interval(
				new RealTransformRandomAccessible<>(
						Views.interpolate(
								new ExtendedRandomAccessibleInterval<>(img,
										new OutOfBoundsConstantValueFactory<>(background)),
								new ClampingNLinearInterpolatorFactory<>()),
						transform),
				targetInterval);
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

		final int numMovingSources = seqP.getViewSetups().size();
		final int numTargetSources = seqQ.getViewSetups().size();

		final int[] movingSourceIndices = ImagePlusLoader.range( 0, numMovingSources );
		final int[] targetSourceIndices = ImagePlusLoader.range( numMovingSources, numTargetSources );

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
		final int numMovingChannels = loaderP.numChannels();

		/* Load the second source, giving each channel a different setupId */
		final AbstractSpimData< ? >[] spimDataQ = loaderQ.loadAll( numMovingChannels );

		final BigWarpData< ? > data = createBigWarpData( spimDataP, spimDataQ, names );

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
		if ( loaderP instanceof ImagePlusLoader )
			spimDataP = loaderP.load();
		else
			spimDataP = loaderP.load();

		/* Load the fixed sources */
		final AbstractSpimData< ? >[] spimDataQ;
		if ( loaderQ instanceof ImagePlusLoader )
			spimDataQ = ( ( ImagePlusLoader ) loaderQ ).loadAll( spimDataP.length );
		else
			spimDataQ = loaderQ.load();

		final int N = loaderP.numSources() + loaderQ.numSources();

		String[] names;
		if ( namesIn == null || namesIn.length != N )
		{
			names = new String[ N ];
			int j = 0;
			for ( int i = 0; i < loaderP.numSources(); i++ )
				names[ j++ ] = loaderP.name( i );

			for ( int i = 0; i < loaderQ.numSources(); i++ )
				names[ j++ ] = loaderQ.name( i );
		}
		else
			names = namesIn;

		final BigWarpData< ? > data = createBigWarpData( spimDataP, spimDataQ, names );

		if ( loaderP instanceof ImagePlusLoader )
			( ( ImagePlusLoader ) loaderP ).update( data );

		if ( loaderQ instanceof ImagePlusLoader )
			( ( ImagePlusLoader ) loaderQ ).update( data );

		return data;
	}

	/**
	 * Create {@link BigWarpData} from two XML files.
	 * 
	 * @param <T> the type
	 * @param xmlFilenameP
	 *            moving source XML
	 * @param xmlFilenameQ
	 *            fixed source XML
	 * @return a BigWarpData instance
	 */
	public static < T extends NativeType<T> > BigWarpData< T > createBigWarpDataFromXML( final String xmlFilenameP, final String xmlFilenameQ )
	{
		final BigWarpData< T > bwdata = BigWarpInit.initData();
		try
		{
			int id = 0;
			LinkedHashMap< Source< T >, SourceInfo > mvgSrcs;
			mvgSrcs = BigWarpInit.createSources( bwdata, xmlFilenameP, id, true );
			id += mvgSrcs.size();
			BigWarpInit.add( bwdata, mvgSrcs );
			BigWarpInit.add( bwdata, BigWarpInit.createSources( bwdata, xmlFilenameQ, id, false ) );
		}
		catch ( final URISyntaxException e )
		{
			e.printStackTrace();
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
		catch ( final SpimDataException e )
		{
			e.printStackTrace();
		}

		return bwdata;
	}

	/**
	 * Create {@link BigWarpData} from two {@link ImagePlus ImagePluses}.
	 *
	 * @param <T> the type
	 * @param impP
	 *            moving source ImagePlus
	 * @param impQ
	 *            fixed source ImagePlus
	 * @return BigWarpData
	 */
	public static < T > BigWarpData< T > createBigWarpDataFromImages( final ImagePlus impP, final ImagePlus impQ )
	{
		int id = 0;
		final BigWarpData< T > bwdata = BigWarpInit.initData();
		final LinkedHashMap< Source< T >, SourceInfo > mvgSrcs = BigWarpInit.createSources( bwdata, impP, id, 0, true );
		id += mvgSrcs.size();
		BigWarpInit.add( bwdata, mvgSrcs );

		if (impQ != null)
			BigWarpInit.add(bwdata, BigWarpInit.createSources(bwdata, impQ, id, 0, false));

		return bwdata;
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
	 * @param <T> the type
	 * @param xmlFilenameP
	 *            movingSource XML
	 * @param impQ
	 *            fixed source ImagePlus
	 * @return BigWarpData
	 */
	public static < T extends NativeType<T> > BigWarpData< T > createBigWarpDataFromXMLImagePlus( final String xmlFilenameP, final ImagePlus impQ )
	{
		final BigWarpData< T > bwdata = BigWarpInit.initData();
		try
		{
			int id = 0;
			LinkedHashMap< Source< T >, SourceInfo > mvgSrcs;
			mvgSrcs = BigWarpInit.createSources( bwdata, xmlFilenameP, id, true );
			id += mvgSrcs.size();
			BigWarpInit.add( bwdata, mvgSrcs );
			BigWarpInit.add( bwdata, BigWarpInit.createSources( bwdata, impQ, id, 0, false ) );
		}
		catch ( final URISyntaxException e )
		{
			e.printStackTrace();
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
		catch ( final SpimDataException e )
		{
			e.printStackTrace();
		}

		return bwdata;
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
	 * @param <T> the type
	 * @param impP
	 *            moving source ImagePlus
	 * @param xmlFilenameQ
	 *            fixed source XML
	 * @return BigWarpData
	 */
	public static < T extends NativeType<T> > BigWarpData< T > createBigWarpDataFromImagePlusXML( final ImagePlus impP, final String xmlFilenameQ )
	{
		final BigWarpData< T > bwdata = BigWarpInit.initData();
		try
		{
			int id = 0;
			LinkedHashMap< Source< T >, SourceInfo > mvgSrcs;
			mvgSrcs = BigWarpInit.createSources( bwdata, impP, id, 0, true );
			id += mvgSrcs.size();
			BigWarpInit.add( bwdata, mvgSrcs );
			BigWarpInit.add( bwdata, BigWarpInit.createSources( bwdata, xmlFilenameQ, id, false ) );
		}
		catch ( final URISyntaxException e )
		{
			e.printStackTrace();
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
		catch ( final SpimDataException e )
		{
			e.printStackTrace();
		}

		return bwdata;
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
	 * {@link #namesFromImagePlus(ImagePlus)} with each.
	 *
	 * @param impP
	 *            first image to generate names from
	 * @param impQ
	 *            second image to generate names from
	 * @return String array of names from both images
	 */
	public static String[] namesFromImagePluses( final ImagePlus impP, final ImagePlus impQ )
	{
		final String[] names = new String[ impP.getNChannels() + impQ.getNChannels() ];

		final String[] impPnames = namesFromImagePlus( impP );
		final String[] impQnames = namesFromImagePlus( impQ );

		int i = 0;
		for ( final String name : impPnames )
			names[ i++ ] = name;
		for ( final String name : impQnames )
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
	public static String[] namesFromImagePlus( final ImagePlus imp )
	{
		if ( imp.getNChannels() == 1 )
			return new String[] { imp.getTitle() };
		final String[] names = new String[ imp.getNChannels() ];
		for ( int i = 0; i < names.length; ++i )
			names[ i ] = imp.getTitle() + "-" + i;
		return names;
	}

}
