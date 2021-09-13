/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2021 Howard Hughes Medical Institute.
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.Optional;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;

import bdv.img.RenamableSource;
import bdv.img.WarpedSource;
import bdv.tools.transformation.TransformedSource;
import bdv.util.RandomAccessibleIntervalMipmapSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp.BigWarpData;
import net.imglib2.realtransform.AffineTransform3D;

public class SourceInfoDialog extends JDialog
{
	private static final long serialVersionUID = 8333668892181971701L;

	/**
	 * Instantiates and displays a JFrame that lists information on 
	 * the opened sources.
	 * 
	 * @param owner the parent frame
	 * @param the bigwarp data
	 */
	public SourceInfoDialog( final Frame owner, final BigWarpData< ? > bwData )
	{
		super( owner, "Source information", false );

		final StringBuffer infoString = new StringBuffer();
		infoString.append( "MOVING:\n" );
		for ( int i = 0; i < bwData.movingSourceIndices.length; i++ )
		{
			SourceAndConverter< ? > src = bwData.sources.get( bwData.movingSourceIndices[ i ]);
			final String name =  src.getSpimSource().getName();
			if( name.equals( "WarpMagnitudeSource" ) ||
				name.equals( "JacobianDeterminantSource" ) ||
				name.equals( "GridSource" ) )
			{
				continue;
			}
			appendSourceString( infoString, src );
			infoString.append( "\n" );
		}

		infoString.append( "\nTARGET:\n" );
		for ( int i = 0; i < bwData.targetSourceIndices.length; i++ )
		{
			SourceAndConverter< ? > src = bwData.sources.get( bwData.targetSourceIndices[ i ]);
			final String name =  src.getSpimSource().getName();
			if( name.equals( "WarpMagnitudeSource" ) ||
				name.equals( "JacobianDeterminantSource" ) ||
				name.equals( "GridSource" ) )
			{
				continue;
			}
			appendSourceString( infoString, src );
			infoString.append( "\n" );
		}

		final JTextArea textArea = new JTextArea( infoString.toString() );
		textArea.setEditable( false );
		final JScrollPane editorScrollPane = new JScrollPane( textArea );
		editorScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED );
		editorScrollPane.setPreferredSize( new Dimension( 800, 800 ) );

		getContentPane().add( editorScrollPane, BorderLayout.CENTER );
		pack();
		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
	}

	private void appendSourceString( final StringBuffer infoString, final SourceAndConverter<?> src  )
	{
		Source< ? > ssrc = src.getSpimSource();
		Optional< RandomAccessibleIntervalMipmapSource > mipmapSrcOpt = unwrapToMultiscale( ssrc );

		final String name =  src.getSpimSource().getName();
		final String unit = src.getSpimSource().getVoxelDimensions().unit();

		infoString.append( name + "\n" );
		infoString.append( "  unit   : " + unit + "\n");

		final AffineTransform3D xfm = new AffineTransform3D();
		if( mipmapSrcOpt.isPresent() )
		{
			RandomAccessibleIntervalMipmapSource<?> mmSrc = mipmapSrcOpt.get();
			for( int i = 0; i < mmSrc.getNumMipmapLevels(); i++ )
			{
				mmSrc.getSourceTransform( 0, i, xfm );
				infoString.append( "  level " + i + "\n" ); 
				infoString.append( "    scale  : " + xfm.get( 0,0 ) + " " + xfm.get(1,1) + " " + xfm.get(2,2) +"\n" );
				infoString.append( "    offset : " + xfm.get( 0,3 ) + " " + xfm.get(1,3) + " " + xfm.get(2,3) +"\n" );
			}
		}
		else
		{
			src.getSpimSource().getSourceTransform( 0, 0, xfm );
			infoString.append( "  scale  : " + xfm.get( 0,0 ) + " " + xfm.get(1,1) + " " + xfm.get(2,2) +"\n" );
			infoString.append( "  offset : " + xfm.get( 0,3 ) + " " + xfm.get(1,3) + " " + xfm.get(2,3) +"\n" );
		}
	}

	@SuppressWarnings( "rawtypes" )
	private Optional<RandomAccessibleIntervalMipmapSource> unwrapToMultiscale( Source< ? > src )
	{
		Source outSrc = src;
		boolean wrappedSource = true;

		while( wrappedSource )
		{
			if ( outSrc instanceof TransformedSource )
			{
				outSrc = ( ( TransformedSource ) outSrc ).getWrappedSource();
			}
			else if ( outSrc instanceof WarpedSource )
			{
				outSrc = ( ( WarpedSource ) outSrc ).getWrappedSource();
			}
			else if ( outSrc instanceof RenamableSource )
			{
				outSrc = ( ( RenamableSource ) outSrc ).getWrappedSource();
			}
			else
			{
				wrappedSource = false;
			}
		}

		if( outSrc instanceof RandomAccessibleIntervalMipmapSource )
			return Optional.of( (RandomAccessibleIntervalMipmapSource)outSrc );
		else
			return Optional.empty();
	}
}
