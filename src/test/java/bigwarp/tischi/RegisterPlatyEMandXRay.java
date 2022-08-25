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
package bigwarp.tischi;

import bdv.ij.BigWarpBdvCommand;

import java.io.File;

public class RegisterPlatyEMandXRay
{
	public static void main( String[] args )
	{
		final BigWarpBdvCommand command = new BigWarpBdvCommand();
		command.fixedImageXml = new File( "/Volumes/cba/exchange/maxim/ver2/2sources/Platy-88_01_tomo-transformed.xml" );
		command.movingImageXml = new File( "/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data/rawdata/xray-6dpf-1-whole-raw.xml" );
		command.run() ;
	}
}
