/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package bigwarp.metadata;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.metadata.AbstractN5Metadata;
import org.janelia.saalfeldlab.n5.metadata.ImageplusMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.metadata.PhysicalMetadata;

import ij.ImagePlus;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;

public class BwN5SingleScaleLegacyMetadata extends AbstractN5Metadata<BwN5SingleScaleLegacyMetadata> 
implements ImageplusMetadata< BwN5SingleScaleLegacyMetadata >, PhysicalMetadata 
{
    public static final String DOWNSAMPLING_FACTORS_KEY = "downsamplingFactors";
    public static final String PIXEL_RESOLUTION_KEY = "pixelResolution";
    public static final String AFFINE_TRANSFORM_KEY = "affineTransform";

	private final HashMap< String, Class< ? > > keysToTypes;
	
    public final AffineTransform3D transform;
	
	public final double[] resolution;

    public BwN5SingleScaleLegacyMetadata( final String path, final AffineTransform3D transform,
    		final DatasetAttributes attributes )
    {
		super( path, attributes );

		Objects.requireNonNull( path );
		Objects.requireNonNull( transform );

		this.transform = transform;
		this.resolution = new double[] {
				transform.get( 0, 0 ),
				transform.get( 1, 1 ),
				transform.get( 2, 2 ) };

    	keysToTypes = new HashMap<>();
		keysToTypes.put( DOWNSAMPLING_FACTORS_KEY, long[].class );
		keysToTypes.put( PIXEL_RESOLUTION_KEY, double[].class );
		keysToTypes.put( AFFINE_TRANSFORM_KEY, AffineTransform3D.class );

		AbstractN5Metadata.addDatasetAttributeKeys( keysToTypes );
    }

    public BwN5SingleScaleLegacyMetadata( final String path, final AffineTransform3D transform )
    {
    	this( path, transform, null );
    }

    public BwN5SingleScaleLegacyMetadata( final String path )
    {
    	this( path, new AffineTransform3D(), null );
    }

    public BwN5SingleScaleLegacyMetadata()
    {
    	this( "", new AffineTransform3D(), null );
    }
    
    public BwN5SingleScaleLegacyMetadata( final String path, final double[] resolution,
    		final DatasetAttributes attributes )
    {
		super( path, attributes );

		Objects.requireNonNull( path );
		Objects.requireNonNull( resolution );

		this.resolution = resolution;
		this.transform = resToAffine( resolution );

    	keysToTypes = new HashMap<>();
		keysToTypes.put( DOWNSAMPLING_FACTORS_KEY, long[].class );
		keysToTypes.put( PIXEL_RESOLUTION_KEY, double[].class );
		keysToTypes.put( AFFINE_TRANSFORM_KEY, AffineTransform3D.class );

		AbstractN5Metadata.addDatasetAttributeKeys( keysToTypes );
    }
    
    private static AffineTransform3D resToAffine( double[] res )
    {
    	AffineTransform3D affine = new AffineTransform3D();
    	affine.set( res[0], 0, 0, 0, 
    			0, res[1], 0, 0,
    			0, 0, res[2], 0 );

    	return affine;
    }

	@Override
	public HashMap<String,Class<?>> keysToTypes()
	{
		return keysToTypes;
	}

	@Override
	public boolean check( final Map< String, Object > metaMap )
	{
		final Map< String, Class< ? > > requiredKeys = AbstractN5Metadata.datasetAtttributeKeys();
		for( final String k : requiredKeys.keySet() )
		{
			if ( !metaMap.containsKey( k ) )
				return false;
			else if( metaMap.get( k ) == null )
				return false;
		}

		// needs to contain one of pixelResolution key
		if ( !metaMap.containsKey( PIXEL_RESOLUTION_KEY ) || metaMap.get( PIXEL_RESOLUTION_KEY ) == null ) 
			return false;

		return true;
	}

    @Override
	public BwN5SingleScaleLegacyMetadata parseMetadata( final Map< String, Object > metaMap ) throws Exception
	{
		if ( !check( metaMap ) )
			return null;

		final String dataset = ( String ) metaMap.get( "dataset" );

		final DatasetAttributes attributes = N5MetadataParser.parseAttributes( metaMap );
		if( attributes == null )
			return null;

		final long[] downsamplingFactors = ( long[] ) metaMap.get( DOWNSAMPLING_FACTORS_KEY );
		final double[] pixelResolution = ( double[] ) metaMap.get( PIXEL_RESOLUTION_KEY ); 

		final AffineTransform3D extraTransform = ( AffineTransform3D ) metaMap.get( AFFINE_TRANSFORM_KEY );
		final AffineTransform3D transform = N5SingleScaleMetadata.buildTransform( downsamplingFactors, pixelResolution, extraTransform );

		return new BwN5SingleScaleLegacyMetadata( dataset, transform, attributes );
	}

	@Override
	public void writeMetadata( final BwN5SingleScaleLegacyMetadata t, final N5Writer n5, final String dataset ) throws Exception
	{
		n5.setAttribute( dataset, PIXEL_RESOLUTION_KEY, t.resolution );
	}

	@Override
	public void writeMetadata( final BwN5SingleScaleLegacyMetadata t, final ImagePlus ip ) throws IOException
	{
		ip.setTitle( t.getPath() );
		ip.getCalibration().pixelWidth = t.resolution[ 0 ];
		ip.getCalibration().pixelHeight = t.resolution[ 1 ];
		ip.getCalibration().pixelDepth = t.resolution[ 2 ];
		ip.getCalibration().setUnit( "um" );
		ip.setDimensions( 1, ip.getStackSize(), 1 );
	}

	@Override
	public BwN5SingleScaleLegacyMetadata readMetadata( final ImagePlus ip ) throws IOException
	{
		final double sx = ip.getCalibration().pixelWidth;
		final double sy = ip.getCalibration().pixelHeight;
		final double sz = ip.getCalibration().pixelDepth;
		final double[] res = new double[] { sx, sy, sz };

		return new BwN5SingleScaleLegacyMetadata( "", res, null );
	}

	@Override
	public AffineGet physicalTransform()
	{
		return transform;
	}

	@Override
	public String[] units()
	{
		return Stream.generate( () -> "um" ).limit( 3 ).toArray( String[]::new );
	}

}
