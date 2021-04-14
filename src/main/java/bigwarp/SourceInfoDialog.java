package bigwarp;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;

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
		for ( SourceAndConverter<?> src : bwData.sources )
		{
			final String name =  src.getSpimSource().getName();
			if( name.equals( "WarpMagnitudeSource" ) ||
				name.equals( "JacobianDeterminantSource" ) ||
				name.equals( "GridSource" ) )
			{
				continue;
			}

			final String unit = src.getSpimSource().getVoxelDimensions().unit();
			final AffineTransform3D xfm = new AffineTransform3D();
			src.getSpimSource().getSourceTransform( 0, 0, xfm );

			infoString.append( name + "\n" );
			infoString.append( "  scale  : " + xfm.get( 0,0 ) + " " + xfm.get(1,1) + " " + xfm.get(2,2) +"\n" );
			infoString.append( "  offset : " + xfm.get( 0,3 ) + " " + xfm.get(1,3) + " " + xfm.get(2,3) +"\n" );
			infoString.append( "  unit   : " + unit );
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
}
