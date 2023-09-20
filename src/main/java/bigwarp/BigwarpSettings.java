package bigwarp;

import bdv.tools.bookmarks.Bookmarks;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.MinMaxGroup;
import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SynchronizedViewerState;
import bdv.viewer.ViewerPanel;
import bdv.viewer.state.SourceGroup;
import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;
import bdv.viewer.state.XmlIoViewerState;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.source.SourceInfo;
import bigwarp.transforms.BigWarpTransform;
import bigwarp.transforms.io.TransformWriterJson;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.listeners.Listeners;

import static bdv.viewer.Interpolation.NEARESTNEIGHBOR;

public class BigwarpSettings extends TypeAdapter< BigwarpSettings >
{

	public static Gson gson = new GsonBuilder()
			.registerTypeAdapter( AffineTransform3D.class, new AffineTransform3dAdapter() )
			.setPrettyPrinting()
			.create();

	transient private final BigWarp< ? > bigWarp;

	private final LandmarkTableModel landmarks;

	private final BigWarpTransform transform;

	private final Map< Integer, SourceInfo> sourceInfos;

	BigWarpViewerPanel viewerP;

	BigWarpViewerPanel viewerQ;

	SetupAssignments setupAssignments;

	Bookmarks bookmarks;

	BigWarpAutoSaver autoSaver;

	boolean overwriteSources = false;

	public BigwarpSettings(
			final BigWarp<?> bigWarp,
			final BigWarpViewerPanel viewerP,
			final BigWarpViewerPanel viewerQ,
			final SetupAssignments setupAssignments,
			final Bookmarks bookmarks,
			final BigWarpAutoSaver autoSaver,
			final LandmarkTableModel landmarks,
			final BigWarpTransform transform,
			final Map< Integer, SourceInfo > sourceInfos
	)
	{

		this.bigWarp = bigWarp;
		this.viewerP = viewerP;
		this.viewerQ = viewerQ;
		this.setupAssignments = setupAssignments;
		this.bookmarks = bookmarks;
		this.autoSaver = autoSaver;
		this.landmarks = landmarks;
		this.transform = transform;
		this.sourceInfos = sourceInfos;
	}

	public void setOverwriteSources( final boolean overwriteSources )
	{
		this.overwriteSources = overwriteSources;
	}

	public void serialize( String jsonFilename ) throws IOException
	{
		try ( final FileWriter fileWriter = new FileWriter( jsonFilename ) )
		{
			write( new JsonWriter( fileWriter ), this );
		}
	}

	@Override
	public void write( final JsonWriter out, final BigwarpSettings value ) throws IOException
	{

		out.beginObject();
		out.name( "Sources" );
		new BigWarpSourcesAdapter( bigWarp, overwriteSources ).write( out, sourceInfos );
		out.name( "ViewerP" );
		new BigWarpViewerPanelAdapter( viewerP ).write( out, viewerP );
		out.name( "ViewerQ" );
		new BigWarpViewerPanelAdapter( viewerQ ).write( out, viewerQ );
		out.name( "SetupAssignments" );
		new SetupAssignmentsAdapter( setupAssignments ).write( out, setupAssignments );
		out.name( "Bookmarks" );
		gson.toJson( bookmarks, Bookmarks.class, out );
		out.name( "Autosave" );
		gson.toJson( autoSaver, BigWarpAutoSaver.class, out );
		if ( landmarks != null )
		{
			out.name( "Transform" );
			out.jsonValue( TransformWriterJson.write( landmarks, transform ).toString() );
		}
		out.endObject();
	}

	@Override
	public BigwarpSettings read( final JsonReader in ) throws IOException
	{
		final JsonObject json = JsonParser.parseReader(in).getAsJsonObject();
		if( json.has("Sources"))
		{
			new BigWarpSourcesAdapter<>( bigWarp, overwriteSources ).fromJsonTree(json.get("Sources"));
			final boolean is2D = BigWarp.detectNumDims(bigWarp.getSources()) == 2;
			if (is2D != bigWarp.options.values.is2D()) {
				bigWarp.changeDimensionality( is2D );
			}
		}

		// need to parse transform first
		if( json.has("Transform"))
			TransformWriterJson.read( bigWarp, json.get("Transform").getAsJsonObject());

		if( json.has("ViewerP"))
			new BigWarpViewerPanelAdapter( viewerP ).fromJsonTree( json.get("ViewerP") );

		if( json.has("ViewerQ"))
			new BigWarpViewerPanelAdapter( viewerQ ).fromJsonTree( json.get("ViewerQ") );

		if( json.has("SetupAssignments"))
			new SetupAssignmentsAdapter(setupAssignments).fromJsonTree(json.get("SetupAssignments"));

		if( json.has("Bookmarks"))
			bigWarp.setBookmarks(gson.fromJson(json.get("Bookmarks"), Bookmarks.class));

		if( json.has("Autosave"))
			bigWarp.setAutoSaver( gson.fromJson( json.get("Autosave"), BigWarpAutoSaver.class ));

		return this;
	}

	public static class BigWarpSourcesAdapter< T > extends TypeAdapter< Map< Integer, SourceInfo > >
	{

		private BigWarp< T > bigwarp;

		private boolean overwriteExisting;

		public BigWarpSourcesAdapter( final BigWarp< T > bigwarp, boolean overwriteSources )
		{
			this.bigwarp = bigwarp;
			this.overwriteExisting = overwriteSources;
		}

		@Override
		public void write( final JsonWriter out, final Map< Integer, SourceInfo > value ) throws IOException
		{
			out.beginObject();

			/* We only want the lowest setupId with the same url*/

			for ( final Map.Entry< Integer, SourceInfo > entry : value.entrySet() )
			{
				if ( !entry.getValue().isSerializable() )
					continue;

				final SourceInfo sourceInfo = entry.getValue();
				final int id = sourceInfo.getId();
				final URI uriObj;
				String uri = sourceInfo.getUri();
				final String name = sourceInfo.getName();
				out.name( "" + id );
				out.beginObject();
				if ( uri == null && name != null && !name.trim().isEmpty() )
				{
					uri = "imagej:///" + name;
				}
				if ( uri != null )
				{
					out.name( "uri" ).value( uri );
				}
				if ( sourceInfo.getName() != null )
				{
					out.name( "name" ).value( sourceInfo.getName() );
				}
				out.name( "isMoving" ).value( sourceInfo.isMoving() );
				out.endObject();
			}
			out.endObject();
		}

		@Override
		public Map< Integer, SourceInfo > read( final JsonReader in ) throws IOException
		{
			in.beginObject();
			while ( in.hasNext() )
			{
				//TODO Caleb: What to do if `data` alrread has a source for this `id`?
				final int id = Integer.parseInt( in.nextName() );
				in.beginObject();
				String uri = null;
				String name = null;
				Boolean isMoving = null;
				while ( in.hasNext() )
				{
					final String key = in.nextName();
					switch ( key )
					{
					case "uri":
						uri = in.nextString();
						break;
					case "name":
						name = in.nextString();
						break;
					case "isMoving":
						isMoving = in.nextBoolean();
						break;
					}
				}
				/* Only add if we either are told to override (in which case remove previous) or don't have. */
				SourceInfo existingInfo = bigwarp.data.sourceInfos.get( id );
				int targetIdx = -1;
				if ( existingInfo != null && overwriteExisting )
				{
					targetIdx = bigwarp.data.remove( existingInfo );
					existingInfo = null;
				}
				if ( existingInfo == null && uri != null )
				{

					final LinkedHashMap< Source< T >, SourceInfo > sources;
					try
					{
						//TODO Transform
						sources = BigWarpInit.createSources( bigwarp.data, uri, id, isMoving );
					}
					catch ( URISyntaxException | SpimDataException e )
					{
						throw new RuntimeException( e );
					}
					BigWarpInit.add( bigwarp.data, sources );
					if ( targetIdx >= 0 )
					{
						/* move the source and converterSetup to the correct idx */
						final SourceAndConverter< T > sacToMove = bigwarp.data.sources.remove( bigwarp.data.sources.size() - 1 );
						bigwarp.data.sources.add( targetIdx, sacToMove );

						final ConverterSetup setupToMove = bigwarp.data.converterSetups.remove( bigwarp.data.converterSetups.size() - 1 );
						bigwarp.data.converterSetups.add( targetIdx, setupToMove );
					}
				}
				in.endObject();
			}
			in.endObject();
			bigwarp.initialize();
			return bigwarp.data.sourceInfos;
		}
	}

	public static class BigWarpViewerPanelAdapter extends TypeAdapter< BigWarpViewerPanel >
	{

		private final BigWarpViewerPanel panel;

		private final ViewerState state;

		public BigWarpViewerPanelAdapter( final BigWarpViewerPanel viewerP )
		{
			this.panel = viewerP;

			final Field deprecatedState;
			try
			{
				deprecatedState = ViewerPanel.class.getDeclaredField( "deprecatedState" );
				deprecatedState.setAccessible( true );
				this.state = ( ViewerState ) deprecatedState.get( panel );
			}
			catch ( NoSuchFieldException | IllegalAccessException e )
			{
				throw new RuntimeException( e );
			}
		}

		@Override
		public void write( final JsonWriter out, final BigWarpViewerPanel value ) throws IOException
		{
			out.beginObject();

			out.name( XmlIoViewerState.VIEWERSTATE_SOURCES_TAG );
			writeSources( out, value );
			out.name( XmlIoViewerState.VIEWERSTATE_GROUPS_TAG );
			writeGroups( out, value );
			out.name( XmlIoViewerState.VIEWERSTATE_DISPLAYMODE_TAG );
			writeDisplayMode( out, value );
			out.name( XmlIoViewerState.VIEWERSTATE_INTERPOLATION_TAG );
			writeInterpolationMode( out, value );
			out.name( XmlIoViewerState.VIEWERSTATE_CURRENTSOURCE_TAG ).value( value.getState().getCurrentSource() );
			out.name( XmlIoViewerState.VIEWERSTATE_CURRENTGROUP_TAG ).value( value.getState().getCurrentGroup() );
			out.name( XmlIoViewerState.VIEWERSTATE_CURRENTTIMEPOINT_TAG ).value( value.getState().getCurrentTimepoint() );

			out.endObject();
		}

		private void writeInterpolationMode( final JsonWriter out, final BigWarpViewerPanel value ) throws IOException
		{
			final Interpolation interpolation = value.getState().getInterpolation();
			switch ( interpolation )
			{
			case NLINEAR:
				out.value( XmlIoViewerState.VIEWERSTATE_INTERPOLATION_VALUE_NLINEAR );
				break;
			case NEARESTNEIGHBOR:
			default:
				out.value( XmlIoViewerState.VIEWERSTATE_INTERPOLATION_VALUE_NEARESTNEIGHBOR );
			}
		}

		private void readInterpolationMode( final JsonReader in ) throws IOException
		{
			switch ( in.nextString() )
			{
			case XmlIoViewerState.VIEWERSTATE_INTERPOLATION_VALUE_NLINEAR:
				state.setInterpolation( Interpolation.NLINEAR );
				break;
			case XmlIoViewerState.VIEWERSTATE_INTERPOLATION_VALUE_NEARESTNEIGHBOR:
			default:
				state.setInterpolation( NEARESTNEIGHBOR );
			}
		}

		private void writeDisplayMode( final JsonWriter out, final ViewerPanel value ) throws IOException
		{
			switch ( value.getState().getDisplayMode() )
			{
			case GROUP:
				out.value( XmlIoViewerState.VIEWERSTATE_DISPLAYMODE_VALUE_GROUP );
				break;
			case FUSED:
				out.value( XmlIoViewerState.VIEWERSTATE_DISPLAYMODE_VALUE_FUSED );
				break;
			case FUSEDGROUP:
				out.value( XmlIoViewerState.VIEWERSTATE_DISPLAYMODE_VALUE_FUSEDGROUP );
				break;
			case SINGLE:
			default:
				out.value( XmlIoViewerState.VIEWERSTATE_DISPLAYMODE_VALUE_SINGLE );
			}
		}

		private void readDisplayMode( final JsonReader in ) throws IOException
		{
			switch ( in.nextString() )
			{
			case XmlIoViewerState.VIEWERSTATE_DISPLAYMODE_VALUE_GROUP:
				state.setDisplayMode( DisplayMode.GROUP );
				break;
			case XmlIoViewerState.VIEWERSTATE_DISPLAYMODE_VALUE_FUSED:
				state.setDisplayMode( DisplayMode.FUSED );
				break;
			case XmlIoViewerState.VIEWERSTATE_DISPLAYMODE_VALUE_FUSEDGROUP:
				state.setDisplayMode( DisplayMode.FUSEDGROUP );
				break;
			case XmlIoViewerState.VIEWERSTATE_DISPLAYMODE_VALUE_SINGLE:
			default:
				state.setDisplayMode( DisplayMode.SINGLE );
			}
		}

		private void writeGroups( final JsonWriter out, final ViewerPanel value ) throws IOException
		{

			out.beginArray();
			final List< SourceGroup > sourceGroups = value.getState().getSourceGroups();
			for ( final SourceGroup sourceGroup : sourceGroups )
			{
				out.beginObject();
				out.name( XmlIoViewerState.VIEWERSTATE_GROUP_ACTIVE_TAG ).value( sourceGroup.isActive() );
				out.name( XmlIoViewerState.VIEWERSTATE_GROUP_NAME_TAG ).value( sourceGroup.getName() );
				out.name( XmlIoViewerState.VIEWERSTATE_GROUP_SOURCEID_TAG );
				out.beginArray();
				for ( final Integer sourceId : sourceGroup.getSourceIds() )
				{
					out.value( sourceId );
				}
				out.endArray();
				out.endObject();
			}
			out.endArray();
		}

		private void readGroups( final JsonReader in ) throws IOException
		{
			in.beginArray();
			final SynchronizedViewerState state = panel.state();
			state.setGroupsActive( state.getActiveGroups(), false );
			state.removeGroups( state.getGroups() );
			while ( in.hasNext() )
			{
				in.beginObject();
				final bdv.viewer.SourceGroup group = new bdv.viewer.SourceGroup();
				state.addGroup( group );
				while ( in.hasNext() )
				{

					switch ( in.nextName() )
					{
					case XmlIoViewerState.VIEWERSTATE_GROUP_ACTIVE_TAG:
						final boolean active = in.nextBoolean();
						state.setGroupActive( group, active );
						break;
					case XmlIoViewerState.VIEWERSTATE_GROUP_NAME_TAG:
						final String name = in.nextString();
						state.setGroupName( group, name );
						break;
					case XmlIoViewerState.VIEWERSTATE_GROUP_SOURCEID_TAG:
						state.removeSourcesFromGroup( new ArrayList<>( state.getSourcesInGroup( group ) ), group );
						in.beginArray();
						while ( in.hasNext() )
						{
							state.addSourceToGroup( state.getSources().get( in.nextInt() ), group );
						}
						in.endArray();
						break;
					}
				}
				in.endObject();
			}
			in.endArray();
		}

		private void writeSources( final JsonWriter out, final ViewerPanel value ) throws IOException
		{

			out.beginArray();
			final List< SourceState< ? > > sources = value.getState().getSources();
			for ( final SourceState< ? > source : sources )
			{
				out.value( source.isActive() );
			}
			out.endArray();
		}

		private void readSources( final JsonReader in ) throws IOException
		{
			final List< SourceState< ? > > sources = state.getSources();
			in.beginArray();
			int i = 0;
			while ( in.hasNext() )
			{
				final boolean isActive = in.nextBoolean();
				final SourceState< ? > source = sources.get( i++ );
				source.setActive( isActive );
			}
			in.endArray();
		}

		@Override
		public BigWarpViewerPanel read( final JsonReader in ) throws IOException
		{

			in.beginObject();
			while ( in.hasNext() )
			{
				final String nextName = in.nextName();
				switch ( nextName )
				{
				case XmlIoViewerState.VIEWERSTATE_SOURCES_TAG:
					readSources( in );
					break;
				case XmlIoViewerState.VIEWERSTATE_GROUPS_TAG:
					readGroups( in );
					break;
				case XmlIoViewerState.VIEWERSTATE_DISPLAYMODE_TAG:
					readDisplayMode( in );
					break;
				case XmlIoViewerState.VIEWERSTATE_INTERPOLATION_TAG:
					readInterpolationMode( in );
					break;
				case XmlIoViewerState.VIEWERSTATE_CURRENTSOURCE_TAG:
					state.setCurrentSource( in.nextInt() );
					break;
				case XmlIoViewerState.VIEWERSTATE_CURRENTGROUP_TAG:
					state.setCurrentGroup( in.nextInt() );
					break;
				case XmlIoViewerState.VIEWERSTATE_CURRENTTIMEPOINT_TAG:
					state.setCurrentTimepoint( in.nextInt() );
					break;
				}
			}
			in.endObject();
			return panel;
		}
	}

	public static class SetupAssignmentsAdapter extends TypeAdapter< SetupAssignments >
	{

		private final SetupAssignments setupAssignments;

		public SetupAssignmentsAdapter( final SetupAssignments setupAssignments )
		{
			this.setupAssignments = setupAssignments;
		}

		@Override
		public void write( final JsonWriter out, final SetupAssignments value ) throws IOException
		{
			out.beginObject();
			out.name( "ConverterSetups" );
			out.beginObject();
			final List< ConverterSetup > converterSetups = value.getConverterSetups();
			final ConverterSetupAdapter converterSetupAdapter = new ConverterSetupAdapter( value );
			for ( final ConverterSetup converterSetup : converterSetups )
			{
				out.name(Integer.toString( converterSetup.getSetupId() ));
				out.beginObject();
				converterSetupAdapter.setSetupId( converterSetup.getSetupId() );
				converterSetupAdapter.write( out, converterSetup );
				out.endObject();
			}
			out.endObject();
			final List< MinMaxGroup > minMaxGroups = value.getMinMaxGroups();
			out.name( "MinMaxGroups" );
			new MinMaxGroupsAdapter().write( out, minMaxGroups );
			out.endObject();
		}

		@Override
		public SetupAssignments read( final JsonReader in ) throws IOException
		{
			final List< ConverterSetupDTO > converters = new ArrayList<>();
			final ArrayList< MinMaxGroup > minMaxGroups;
			try
			{
				final Field minMaxGroupsField = setupAssignments.getClass().getDeclaredField( "minMaxGroups" );
				minMaxGroupsField.setAccessible( true );
				//noinspection unchecked
				minMaxGroups = ( ArrayList< MinMaxGroup > ) minMaxGroupsField.get( setupAssignments );
			}
			catch ( NoSuchFieldException | IllegalAccessException e )
			{
				throw new RuntimeException( e );
			}

			minMaxGroups.clear();

			in.beginObject();
			while ( in.hasNext() )
			{
				final String name = in.nextName();
				switch ( name )
				{
				case "ConverterSetups":
					final ConverterSetupAdapter converterSetupAdapter = new ConverterSetupAdapter( setupAssignments );
					in.beginObject();
					while ( in.hasNext() )
					{
						final int id = Integer.parseInt( in.nextName() );
						converterSetupAdapter.setSetupId( id );
						in.beginObject();
						final ConverterSetupDTO dto = ( ConverterSetupDTO ) converterSetupAdapter.read( in);
						converters.add( dto );

						in.endObject();
					}
					in.endObject();

					final List< ConverterSetup > converterSetups = setupAssignments.getConverterSetups();
					final List<Integer> originalSetupIdOrder = converterSetups.stream().map( ConverterSetup::getSetupId ).collect( Collectors.toList());
					for ( int idx = 0; idx < converters.size(); idx++ )
					{
						final ConverterSetupDTO converterSetupDTO = converters.get( idx );
						final int setupId = converterSetupDTO.getSetupId();

						final int idxOfConverterSetup = originalSetupIdOrder.indexOf( setupId );
						if (idxOfConverterSetup >= 0 && idx != idxOfConverterSetup) {
							converters.remove( idx );
							converters.add( idxOfConverterSetup, converterSetupDTO );
						}
					}
					break;
				case "MinMaxGroups":
					minMaxGroups.addAll( new MinMaxGroupsAdapter().read( in ) );
					break;
				default:
					throw new RuntimeException( "Unknown SetupAssignment Key: " + name );
				}
			}
			in.endObject();

			for ( final ConverterSetupDTO setupDto : converters )
			{
				final ConverterSetup setup = setupAssignments.getConverterSetups().stream().filter( it -> it.getSetupId() == setupDto.getSetupId() ).findFirst().get();
				setup.setDisplayRange( setupDto.getDisplayRangeMin(), setupDto.getDisplayRangeMax() );
				setup.setColor( setupDto.getColor() );
				final MinMaxGroup group = minMaxGroups.get( setupDto.getGroupId() );
				setupAssignments.moveSetupToGroup( setup, group );

			}
			return setupAssignments;
		}
	}

	public static class MinMaxGroupsAdapter extends TypeAdapter< List< MinMaxGroup > >
	{

		@Override
		public void write( final JsonWriter out, final List< MinMaxGroup > value ) throws IOException
		{
			out.beginObject();
			for ( int i = 0; i < value.size(); i++ )
			{
				out.name( Integer.toString( i ));
				out.beginObject();
				out.name( "fullRangeMin" ).value( value.get( i ).getFullRangeMin() );
				out.name( "fullRangeMax" ).value( value.get( i ).getFullRangeMax() );
				out.name( "rangeMin" ).value( value.get( i ).getRangeMin() );
				out.name( "rangeMax" ).value( value.get( i ).getRangeMax() );
				out.name( "currentMin" ).value( value.get( i ).getMinBoundedValue().getCurrentValue() );
				out.name( "currentMax" ).value( value.get( i ).getMaxBoundedValue().getCurrentValue() );
				out.endObject();
			}
			out.endObject();
		}

		@Override
		public List< MinMaxGroup > read( final JsonReader in ) throws IOException
		{
			final HashMap< Integer, MinMaxGroup > groupMap = new HashMap<>();
			final ArrayList< MinMaxGroup > groups = new ArrayList<>();
			in.beginObject();
			while ( in.hasNext() )
			{
				final int id = Integer.parseInt( in.nextName() );
				double fullRangeMin = 0;
				double fullRangeMax = 0;
				double rangeMin = 0;
				double rangeMax = 0;
				double currentMin = 0;
				double currentMax = 0;
				in.beginObject();
				while ( in.hasNext() )
				{
					switch ( in.nextName() )
					{
					case "fullRangeMin":
						fullRangeMin = in.nextDouble();
						break;
					case "fullRangeMax":
						fullRangeMax = in.nextDouble();
						break;
					case "rangeMin":
						rangeMin = in.nextDouble();
						break;
					case "rangeMax":
						rangeMax = in.nextDouble();
						break;
					case "currentMin":
						currentMin = in.nextDouble();
						break;
					case "currentMax":
						currentMax = in.nextDouble();
						break;
					}
				}

				/* Note: Currently, this MinMaxGroup constructor always passes in a private static final `0` for minIntervalSize */
				final double minIntervalSize = 0;
				final MinMaxGroup group = new MinMaxGroup( fullRangeMin,
						fullRangeMax,
						rangeMin,
						rangeMax,
						currentMin,
						currentMax,
						minIntervalSize );
				groupMap.put( id, group );
				in.endObject();
			}
			in.endObject();
			for ( int i = 0; i < groupMap.size(); i++ )
			{
				/* We require that the `id` of the deserialized group matches the index of the returned list. */
				groups.add( groupMap.get( i ) );
			}
			return groups;
		}
	}

	public static class ConverterSetupAdapter extends TypeAdapter< ConverterSetup >
	{
		private final SetupAssignments setupAssignments;

		private int setupId = -1;

		public ConverterSetupAdapter( final SetupAssignments setupAssignments )
		{
			this.setupAssignments = setupAssignments;
		}

		public void setSetupId( final int setupId )
		{
			this.setupId = setupId;
		}

		@Override
		public void write( final JsonWriter out, final ConverterSetup value ) throws IOException
		{
			final List< MinMaxGroup > minMaxGroups = setupAssignments.getMinMaxGroups();
			out.name( "min" ).value( value.getDisplayRangeMin() );
			out.name( "max" ).value( value.getDisplayRangeMax() );
			out.name( "color" ).value( value.getColor().get() );
			out.name( "groupId" ).value( minMaxGroups.indexOf( setupAssignments.getMinMaxGroup( value ) ) );
		}

		@Override
		public ConverterSetup read( final JsonReader in ) throws IOException
		{
			double tmpmin = 0;
			double tmpmax = 0;
			int tmpcolor = 0;
			int tmpgroupId = 0;
			while ( in.hasNext() )
			{
				switch ( in.nextName() )
				{
				case "min":
					tmpmin = in.nextDouble();
					break;
				case "max":
					tmpmax = in.nextDouble();
					break;
				case "color":
					tmpcolor = in.nextInt();
					break;
				case "groupId":
					tmpgroupId = in.nextInt();
					break;
				}
			}

			final double min = tmpmin;
			final double max = tmpmax;
			final int color = tmpcolor;
			final int groupId = tmpgroupId;

			final ConverterSetup converterSetupDTO = new ConverterSetupDTO()
			{

				private final int id = setupId;

				@Override
				public int getGroupId()
				{
					return groupId;
				}

				@Override
				public Listeners< SetupChangeListener > setupChangeListeners()
				{
					return null;
				}

				@Override
				public int getSetupId()
				{
					return id;
				}

				@Override
				public void setDisplayRange( final double min, final double max )
				{
				}

				@Override
				public void setColor( final ARGBType color )
				{
				}

				@Override
				public boolean supportsColor()
				{
					return false;
				}

				@Override
				public double getDisplayRangeMin()
				{
					return min;
				}

				@Override
				public double getDisplayRangeMax()
				{
					return max;
				}

				@Override
				public ARGBType getColor()
				{
					return new ARGBType( color );
				}
			};
			return converterSetupDTO;
		}
	}

	public static class FileAdapter extends TypeAdapter< File >
	{
		@Override
		public void write( final JsonWriter out, final File value ) throws IOException
		{
			out.value( value.getCanonicalPath() );
		}

		@Override
		public File read( final JsonReader in ) throws IOException
		{
			return new File( in.nextString() );
		}
	}

	public static class AffineTransform3dAdapter extends TypeAdapter< AffineTransform3D >
	{

		@Override
		public void write( final JsonWriter out, final AffineTransform3D value ) throws IOException
		{
			assert value.numDimensions() == 3;

			final double[] values = value.getRowPackedCopy();
			out.beginArray();
			for ( final double v : values )
			{
				out.value( v );
			}
			out.endArray();
		}

		@Override
		public AffineTransform3D read( final JsonReader in ) throws IOException
		{
			final AffineTransform3D affine = new AffineTransform3D();

			final double[] values = new double[ 12 ];
			in.beginArray();
			for ( int i = 0; i < values.length; i++ )
			{
				values[ i ] = in.nextDouble();
			}
			in.endArray();

			affine.set( values );
			return affine;
		}
	}

	private interface ConverterSetupDTO extends ConverterSetup
	{

		int getGroupId();

	}
}

