/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2025 Howard Hughes Medical Institute.
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

	private Supplier<String> transformUriSupplier;

	/*
	 * When using an imported fixed transform, we'll want to apply all the
	 * transforms in sequence, interpolating only once. This source should be
	 * used for export, in contrast to the source stored in BigWarpData, which
	 * should be used for visualization.
	 */
	private SourceAndConverter<?> sourceForExport;

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
	public void setSerializable(final boolean serializable) {

		this.serializable = serializable;
	}

	public boolean isSerializable() {

		return serializable;
	}

	public String getUri() {

		return uriSupplier.get();
	}

	public void setUriSupplier(final Supplier<String> getUri) {

		this.uriSupplier = getUri;
	}

	public void setSourceForExport(SourceAndConverter<?> sourceForExport) {

		this.sourceForExport = sourceForExport;
	}

	public SourceAndConverter<?> sourceForExport() {

		return sourceForExport;
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

	public void setTransform( final RealTransform transform, Supplier<String> transformUriSupplier )
	{
		this.transform = transform;
		this.transformUriSupplier = transformUriSupplier;
	}

	public RealTransform getTransform()
	{
		return transform;
	}

	public String getTransformUri()
	{
		if( transformUriSupplier != null )
			return transformUriSupplier.get();
		else
			return null;
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
