package bdv.gui.sourceList;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.imglib2.N5DisplacementField;
import org.janelia.saalfeldlab.n5.universe.N5Factory;

import bigwarp.transforms.NgffTransformations;
import net.imglib2.realtransform.RealTransform;

public class BigWarpSourceTableModel extends AbstractTableModel
{

	private static final long serialVersionUID = 5923947651732788341L;

	public static enum SourceType { IMAGEPLUS, DATASET, URL };
	protected final static String[] colNames = new String[] { "Name", "Moving", "Transform", " " };

	protected final String[] columnNames;
	protected final ArrayList<SourceRow> sources;
	protected final ArrayList<RemoveRowButton> rmRowButtons;

	protected static int imageColIdx = 0;
	protected static int movingColIdx = 1;
	protected static int transformColIdx = 2;
	protected static int removeColIdx = 3;

	protected Function<String,String> transformChangedCallback;

	private Component container;

	public BigWarpSourceTableModel() {

		this(null);
	}

	public BigWarpSourceTableModel(final Function<String, String> transformChangedCallback) {

		super();
		columnNames = colNames;
		sources = new ArrayList<>();
		rmRowButtons = new ArrayList<>();
		this.transformChangedCallback = transformChangedCallback;
	}

	/**
	 * Set the {@link Component} to repaint when a row is removed.
	 *
	 * @param container
	 *            the component containing this table
	 */
	public void setContainer(Component container) {

		this.container = container;
	}

	public SourceRow get(int i) {

		return sources.get(i);
	}

	@Override
	public String getColumnName(int col) {

		return columnNames[col];
	}

	@Override
	public int getColumnCount() {

		return columnNames.length;
	}

	@Override
	public int getRowCount() {

		return sources.size();
	}

	@Override
	public Object getValueAt(int r, int c) {

		if (c == 3)
			return rmRowButtons.get(r);
		else
			return sources.get(r).get(c);
	}

	@Override
	public Class<?> getColumnClass(int col) {

		if (col == 1)
			return Boolean.class;
		else if (col == 3)
			return JButton.class;
		else
			return String.class;
	}

	@Override
	public boolean isCellEditable(int row, int col) {

		return true;
	}

	@Override
	public void setValueAt(Object value, int row, int col) {

		if (col == movingColIdx)
			sources.get(row).moving = (Boolean)value;
		else if (col == imageColIdx)
			sources.get(row).srcName = (String)value;
		else if (col == transformColIdx)
			setTransform(row, (String)value);
	}

	public void setTransform(final int row, final String value) {

		if (transformChangedCallback != null) {
			final String res = transformChangedCallback.apply(value);
			if (res != null)
				sources.get(row).transformUrl = res;
			else
				sources.get(row).transformUrl = value;
		} else
			sources.get(row).transformUrl = value;
	}

	public void add(String srcName, boolean moving, SourceType type) {

		final RemoveRowButton rmButton = new RemoveRowButton(sources.size());
		rmRowButtons.add(rmButton);
		sources.add(new SourceRow(srcName, moving, "", type));
	}

	public void add(String srcName, boolean moving) {

		add(srcName, moving, SourceType.URL);
	}

	public void add(String srcName) {

		add(srcName, false);
	}

	public void addImagePlus(String srcName) {

		addImagePlus(srcName, false);
	}

	public void addImagePlus(String srcName, boolean isMoving) {

		add(srcName, isMoving, SourceType.IMAGEPLUS);
	}

	public void addDataset(String srcName) {

		addImagePlus(srcName, false);
	}

	public void addDataset(String srcName, boolean isMoving) {

		add(srcName, isMoving, SourceType.DATASET);
	}

	public boolean remove(int i) {

		if (i >= sources.size())
			return false;

		sources.remove(i);
		rmRowButtons.remove(i);
		updateRmButtonIndexes();

		if (container != null)
			container.repaint();

		return true;
	}

	private void updateRmButtonIndexes() {

		for (int i = 0; i < rmRowButtons.size(); i++)
			rmRowButtons.get(i).setRow(i);
	}

	public static class SourceRow {

		public String srcName;
		public boolean moving;
		public String transformUrl;

		public SourceType type;

		public SourceRow(String srcName, boolean moving, String transformUrl, SourceType type) {

			this.srcName = srcName;
			this.moving = moving;
			this.transformUrl = transformUrl;
			this.type = type;
		}

		public SourceRow(String srcName, boolean moving, String transformName) {

			this(srcName, moving, transformName, SourceType.URL);
		}

		public Object get(int c) {

			if (c == 0)
				return srcName;
			else if (c == 1)
				return moving;
			else if (c == 2)
				return transformUrl;
			else
				return null;
		}

		public RealTransform getTransform() {


			if (transformUrl != null && !transformUrl.isEmpty()) {
				final String trimUrl = transformUrl.trim();
				try {
					final N5URI n5Uri = new N5URI(trimUrl);
					final URI uri = n5Uri.getURI();
					if (uri.getFragment() == null) {
						return N5DisplacementField.open(
								new N5Factory().openReader(n5Uri.getContainerPath()),
								uri.getQuery() == null ? N5DisplacementField.FORWARD_ATTR : n5Uri.getGroupPath(),
								false);
					}
				} catch (final URISyntaxException e) {}

				return NgffTransformations.open(trimUrl);
			}

			return null;
		}

		public Supplier<String> getTransformUri() {

			if (transformUrl != null && !transformUrl.isEmpty())
				return () -> transformUrl;

			return null;
		}
	}

	protected static class RemoveRowButton extends JButton {

		private int row;

		public RemoveRowButton(int row) {

			super("remove");
			setRow(row);
		}

		public int getRow() {

			return row;
		}

		public void setRow(int row) {

			this.row = row;
		}
	}

	/**
	 * From
	 * http://www.java2s.com/Code/Java/Swing-Components/ButtonTableExample.htm
	 */
	protected static class ButtonRenderer extends JButton implements TableCellRenderer {

		public ButtonRenderer() {

			setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

			if (isSelected) {
				setForeground(table.getSelectionForeground());
				setBackground(table.getSelectionBackground());
			} else {
				setForeground(table.getForeground());
				setBackground(UIManager.getColor("Button.background"));
			}
			setText((value == null) ? "" : ((RemoveRowButton)value).getText());
			return this;
		}
	}

	/**
	 * From
	 * http://www.java2s.com/Code/Java/Swing-Components/ButtonTableExample.htm
	 */
	protected static class ButtonEditor extends DefaultCellEditor {

		protected JButton button;

		private String label;

		private RemoveRowButton thisButton;

		private BigWarpSourceTableModel model;

		private boolean isPushed;

		public ButtonEditor(JCheckBox checkBox, BigWarpSourceTableModel model) {

			super(checkBox);
			checkBox.setText("-");
			this.model = model;

			button = new JButton();
			button.setOpaque(true);
			button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					fireEditingStopped();
				}
			});
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {

			if (isSelected) {
				button.setForeground(table.getSelectionForeground());
				button.setBackground(table.getSelectionBackground());
			} else {
				button.setForeground(table.getForeground());
				button.setBackground(table.getBackground());
			}
			thisButton = ((RemoveRowButton)value);
			label = (value == null) ? "" : thisButton.getText();
			button.setText(label);
			isPushed = true;
			return button;
		}

		@Override
		public Object getCellEditorValue() {

			if (isPushed) {
				model.remove(thisButton.getRow());
			}
			isPushed = false;
			return new String(label);
		}

		@Override
		public boolean stopCellEditing() {

			isPushed = false;
			return super.stopCellEditing();
		}

		@Override
		protected void fireEditingStopped() {

			super.fireEditingStopped();
		}
	}

}
