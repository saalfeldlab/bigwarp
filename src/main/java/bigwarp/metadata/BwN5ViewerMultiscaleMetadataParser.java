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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5MultiScaleMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5SingleScaleMetadata;

import net.imglib2.realtransform.AffineTransform3D;
import se.sawano.java.text.AlphanumericComparator;

public class BwN5ViewerMultiscaleMetadataParser //implements N5GroupParser< N5MultiScaleMetadata >
{
//    private static final Predicate<String> scaleLevelPredicate = Pattern.compile("^s\\d+$").asPredicate();
//
//    private static final AlphanumericComparator COMPARATOR = new AlphanumericComparator(Collator.getInstance());
//
//    /**
//     * Called by the {@link org.janelia.saalfeldlab.n5.N5DatasetDiscoverer}
//     * while discovering the N5 tree and filling the metadata for datasets or groups.
//     *
//     * @param node the node
//     * @return the metadata
//     */
//	@Override
//	public N5MultiScaleMetadata parseMetadataGroup( final N5TreeNode node )
//	{
//		final Map< String, N5TreeNode > scaleLevelNodes = new HashMap<>();
//		String[] units = null;
//
//		final List< N5TreeNode > children = node.childrenList();
//		children.sort( Comparator.comparing( N5TreeNode::toString, COMPARATOR ) );
//
//		for ( final N5TreeNode childNode : children )
//		{
//			if ( scaleLevelPredicate.test( childNode.getNodeName() ) &&
//				 childNode.isDataset() &&
//				 ( childNode.getMetadata() instanceof N5SingleScaleMetadata ||
//				   childNode.getMetadata() instanceof BwN5SingleScaleLegacyMetadata ))
//			{
//				scaleLevelNodes.put( childNode.getNodeName(), childNode );
//				if( units == null )
//					units = ((PhysicalMetadata)childNode.getMetadata()).units();
//			}
//		}
//
//		if ( scaleLevelNodes.isEmpty() )
//			return null;
//
//		final List<AffineTransform3D> transforms = new ArrayList<>();
//		final List<String> paths = new ArrayList<>();
//
//        children.forEach( c -> {
////            System.out.println( c.getPath() );
//			if( scaleLevelNodes.containsKey( c.getNodeName() ))
//			{
//				paths.add( c .getPath());
//				transforms.add( ((N5CosemMetadata)c.getMetadata() ).getTransform().toAffineTransform3d() );
//			}
//		});
//
//		return new N5MultiScaleMetadata(
//				node.getPath(),
//				paths.toArray( new String[ 0 ] ),
//				transforms.toArray( new AffineTransform3D[ 0 ] ),
//				units );
//	}

}
