package bigwarp.source;

import bdv.viewer.SourceAndConverter;
import bigwarp.loader.ImagePlusLoader.ColorSettings;
import java.util.function.Supplier;
import net.imglib2.realtransform.RealTransform;

public class SourceInfo
{
	private SourceAndConverter<?> sourceAndConverter = null;
	private final int id;

	private String name;

	private Supplier<String> uriSupplier;

	private ColorSettings colorSettings = null;

	private final boolean moving;

	private RealTransform transform;

	boolean serializable = false;

	public SourceInfo( final int id, final boolean moving )
	{
		this( id, moving, null, null, null);
	}

	public SourceInfo( final int id, final boolean moving, final String name )
	{
		this(id, moving, name, () -> null, null);
	}

	public SourceInfo( final int id, final boolean moving, final String name, Supplier<String> uriSupplier )
	{
		this(id, moving, name, uriSupplier, null);
	}

	public SourceInfo( final int id, final boolean moving, final String name, final Supplier< String > uriSupplier, RealTransform transform )
	{
		this.id = id;
		this.moving = moving;
		this.name = name;
		this.uriSupplier = uriSupplier;
		this.transform = transform;
	}

	/**
	 * Some source origins (url, .xml file, ImagePlus) may lead to multiple Sources being created.
	 * We only want to setSerializable the initial one which caused the subsequent onces to be created.
	 * We do this by setting the {@link #serializable} flag to {@code true} for the first source in the sequence.
	 *
	 * @param serializable whether to setSerialize this source or not.
	 */
	public void setSerializable( final boolean serializable )
	{
		this.serializable = serializable;
	}

	public boolean isSerializable()
	{
		return serializable;
	}

	public String getUri()
	{
		return uriSupplier.get();
	}

	public void setUriSupplier( final Supplier< String > getUri )
	{
		this.uriSupplier = getUri;
	}

	public int getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public void setName( final String name )
	{
		this.name = name;
	}

	public void setColorSettings( final ColorSettings colorSettings )
	{
		this.colorSettings = colorSettings;
	}

	public ColorSettings getColorSettings()
	{
		return colorSettings;
	}

	public boolean isMoving()
	{
		return moving;
	}

	public void setTransform( final RealTransform transform )
	{
		this.transform = transform;
	}

	public RealTransform getTransform()
	{
		return transform;
	}

	public SourceAndConverter< ? > getSourceAndConverter()
	{
		return sourceAndConverter;
	}

	public void setSourceAndConverter( final SourceAndConverter< ? > sourceAndConverter )
	{
		this.sourceAndConverter = sourceAndConverter;
	}
}
