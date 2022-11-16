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
package bdv.ij;

import bdv.ij.util.ProgressWriterIJ;
import bigwarp.BigWarp;
import bigwarp.BigWarpData;
import bigwarp.BigWarpInit;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import org.janelia.utility.ui.RepeatingReleasedEventsFixer;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

/**
 * ImageJ command to register two BDV/XML images.
 *
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 * @author Christian Tischer&lt;christian.tischer@embl.de&gt;
 */
@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>Big Warp XML/HDF5" )
public class BigWarpBdvCommand implements Command
{
	@Parameter ( label = "Fixed image file [ xml/hdf5 ]" )
	public File fixedImageXml;

	@Parameter ( label = "Moving image file [ xml/hdf5 ]" )
	public File movingImageXml;

	public BigWarp bw;

	@Override
	public void run()
	{
		try
        {
			final SpimData fixedSpimData = new XmlIoSpimData().load( fixedImageXml.getAbsolutePath() );
			final SpimData movingSpimData = new XmlIoSpimData().load( movingImageXml.getAbsolutePath() );
			new RepeatingReleasedEventsFixer().install();
			final BigWarpData< ? > bigWarpData = BigWarpInit.createBigWarpData( movingSpimData, fixedSpimData );
			bw = new BigWarp( bigWarpData,  new ProgressWriterIJ() );
			bw.getViewerFrameP().getViewerPanel().requestRepaint();
			bw.getViewerFrameQ().getViewerPanel().requestRepaint();
			bw.getLandmarkFrame().repaint();
			bw.setMovingSpimData( movingSpimData, movingImageXml );
		}
        catch (final SpimDataException e)
        {
			e.printStackTrace();
			return;
		}
	}

}
