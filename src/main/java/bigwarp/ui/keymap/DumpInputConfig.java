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
package bigwarp.ui.keymap;

import bdv.TransformEventHandler2D;
import bdv.TransformEventHandler3D;
import bdv.tools.CloseWindowActions;
import bdv.viewer.NavigationActions;
import bigwarp.BigWarpActions;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.scijava.Context;
import org.scijava.plugin.PluginService;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.InputTriggerDescription;
import org.scijava.ui.behaviour.io.InputTriggerDescriptionsBuilder;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionsBuilder;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

class DumpInputConfig
{
	public static void writeToYaml( final String fileName, final InputTriggerConfig config ) throws IOException
	{
		mkdirs( fileName );
		final List< InputTriggerDescription > descriptions = new InputTriggerDescriptionsBuilder( config ).getDescriptions();
		YamlConfigIO.write( descriptions, fileName );
	}

	public static void writeDefaultConfigToYaml( final String fileName, final Context context ) throws IOException
	{
		mkdirs( fileName );
		final List< InputTriggerDescription > descriptions = new InputTriggerDescriptionsBuilder( buildCommandDescriptions( context ).createDefaultKeyconfig() ).getDescriptions();
		YamlConfigIO.write( descriptions, fileName );
	}

	private static boolean mkdirs( final String fileName )
	{
		final File dir = new File( fileName ).getParentFile();
		return dir != null && dir.mkdirs();
	}

	static CommandDescriptions buildCommandDescriptions( final Context context )
	{
		final CommandDescriptionsBuilder builder = new CommandDescriptionsBuilder();
		context.inject( builder );

		builder.addManually( new BigWarpActions.Descriptions(), BigWarpActions.BIGWARP_CTXT );
		builder.addManually( new NavigationActions.Descriptions(), BigWarpActions.BIGWARP_CTXT );
		builder.addManually( new CloseWindowActions.Descriptions(), BigWarpActions.BIGWARP_CTXT );
		builder.addManually( new TransformEventHandler3D.Descriptions(), BigWarpActions.BIGWARP_CTXT );
		builder.addManually( new TransformEventHandler2D.Descriptions(), BigWarpActions.BIGWARP_CTXT );

		builder.verifyManuallyAdded(); // TODO: It should be possible to filter by Scope here

		return builder.build();
	}

	public static void main( String[] args ) throws IOException
	{
		final String target = KeymapManager.class.getResource( "default.yaml" ).getFile();
		final File resource = new File( target.replaceAll( "target/classes", "src/main/resources" ) );
		System.out.println( "resource = " + resource );
		writeDefaultConfigToYaml( resource.getAbsolutePath(), new Context( PluginService.class ) );
	}
}
