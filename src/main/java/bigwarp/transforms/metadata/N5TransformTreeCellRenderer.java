package bigwarp.transforms.metadata;

import java.awt.Component;

import javax.swing.JTree;

import org.janelia.saalfeldlab.n5.ui.N5DatasetTreeCellRenderer;
import org.janelia.saalfeldlab.n5.ui.N5SwingTreeNode;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;

public class N5TransformTreeCellRenderer extends N5DatasetTreeCellRenderer {

	private static final long serialVersionUID = 9198333318002035941L;

	public N5TransformTreeCellRenderer(boolean showConversionWarning) {

		super(showConversionWarning);
	}

	@Override
	public String getParameterString(final N5TreeNode node) {

		final N5Metadata meta = node.getMetadata();
		if (meta == null || !(meta instanceof N5TransformMetadata))
			return "";

		final CoordinateTransform<?>[] tforms = ((N5TransformMetadata)node.getMetadata()).getTransforms();

		if (tforms != null) {
			final String suffix = tforms.length > 1 ? " ..." : "";
			final String first = tforms.length > 0 ? String.format("\"%s\" (<i>%s</i>)", tforms[0].getName(), tforms[0].getType()) : "";

			return String.format(" (%d) [%s%s]", tforms.length, first, suffix);
		} else
			return "";

	}

	@Override
	public Component getTreeCellRendererComponent( final JTree tree, final Object value,
			final boolean sel, final boolean exp, final boolean leaf, final int row, final boolean hasFocus )
	{

		super.getTreeCellRendererComponent( tree, value, sel, exp, leaf, row, hasFocus );

		N5SwingTreeNode node;
		if ( value instanceof N5SwingTreeNode )
		{
			node = ( ( N5SwingTreeNode ) value );
			if ( node.getMetadata() != null )
			{

			    final String memStr = memString( node );
			    final String memSizeString = memStr.isEmpty() ? "" : " (" + memStr + ")";
			    final String name = node.getParent() == null ? rootName : node.getNodeName();

				setText( String.join( "", new String[]{
						"<html>",
						String.format(nameFormat, name),
						getParameterString( node ),
						memSizeString,
						"</html>"
				}));
			}
			else
			{
				setText(node.getParent() == null ? rootName : node.getNodeName());
			}
		}
		return this;
    }

}

