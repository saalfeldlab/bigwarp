/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
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
package bdv.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.text.Collator;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.saalfeldlab.n5.CachedGsonKeyValueN5Reader;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.CompressionAdapter;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.janelia.saalfeldlab.n5.ui.N5DatasetTreeCellRenderer;
import org.janelia.saalfeldlab.n5.ui.N5MetadataTranslationPanel;
import org.janelia.saalfeldlab.n5.ui.N5SpatialKeySpecDialog;
import org.janelia.saalfeldlab.n5.ui.N5SwingTreeNode;
import org.janelia.saalfeldlab.n5.ui.TranslationResultPanel;
import org.janelia.saalfeldlab.n5.universe.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5Factory.StorageFormat;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5GenericSingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.translation.TranslatedN5Reader;

import com.formdev.flatlaf.util.UIScale;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import bigwarp.transforms.metadata.N5TransformMetadata;
import ij.IJ;
import ij.ImageJ;
import ij.gui.ProgressBar;
import loci.poi.util.SystemOutLogger;
import net.imglib2.util.Pair;
import se.sawano.java.text.AlphanumericComparator;

/**
 * Essentially a copy of {@link DatasetSelectorDialog} to work for 
 * transformation selection.
 *
 * Aim to deprecate this class in the future once DatasetSelectorDialog can
 * be extended to cover this use case.
 */
public class TransformSelectorDialog {

	/**
	 * The dataset/group discoverer that takes a list of metadata parsers.
	 * <p>
	 * Currently, there is only one parser for N5 Viewer-style metadata (that
	 * comes from the previous version of this plugin).
	 * <p>
	 * To add more parsers, add a new class that implements
	 * {@link N5MetadataParser} and pass an instance of it to the
	 * {@link N5DatasetDiscoverer} constructor here.
	 */
	private N5DatasetDiscoverer datasetDiscoverer;

	private Consumer<DataSelection> okCallback;

	private JFrame dialog;

	private org.janelia.saalfeldlab.n5.ui.ImprovedFormattedTextField containerPathText;

	private JCheckBox virtualBox;

	private JCheckBox cropBox;

	private JTree containerTree;

	private JButton browseBtn;

	private JButton detectBtn;

	private JLabel messageLabel;

	private JButton okBtn;

	private JButton cancelBtn;

	private DefaultTreeModel treeModel;

	private String lastBrowsePath;

	private Function<String, N5Reader> n5Fun;

	private final Function<String, String> pathFun;

	private N5Reader n5;

	private boolean virtualOption = false;

	private boolean cropOption = false;

	private Thread loaderThread;

	private ExecutorService loaderExecutor;

	private Future<N5TreeNode> parserFuture;

	private final String initialContainerPath;

	private Consumer<String> containerPathUpdateCallback;

	private Consumer<Void> cancelCallback;

	private Predicate<N5TreeNode> n5NodeFilter;

	private Predicate<N5Metadata> selectionFilter;

	private TreeCellRenderer treeRenderer;

	private final N5MetadataParser<?>[] parsers;

	private N5SwingTreeNode rootNode;

	private N5SpatialKeySpecDialog spatialMetaSpec;

	private N5MetadataTranslationPanel translationPanel;

	private TranslationResultPanel translationResultPanel;

	private ExecutorService parseExec;

	private ProgressBar ijProgressBar;

	private boolean detectCalled;

	private final AlphanumericComparator comp = new AlphanumericComparator(Collator.getInstance());

	public TransformSelectorDialog(
			final Function<String, N5Reader> n5Fun,
			final Function<String, String> pathFun,
			final String initialContainerPath,
			final N5MetadataParser<?>... parsers) {

		this.n5Fun = n5Fun;
		this.pathFun = pathFun;
		this.initialContainerPath = initialContainerPath;

		this.parsers = parsers;

		spatialMetaSpec = new N5SpatialKeySpecDialog();
		translationPanel = new N5MetadataTranslationPanel();
		translationResultPanel = new TranslationResultPanel();

		final ImageJ ij = IJ.getInstance();
		if (ij != null)
			ijProgressBar = ij.getProgressBar();

		detectCalled = false;
	}

	public TransformSelectorDialog(
			final Function<String, N5Reader> n5Fun,
			final Function<String, String> pathFun,
			final N5MetadataParser<?>... parsers) {

		this(n5Fun, pathFun, "", parsers);
	}

	public TransformSelectorDialog(
			final Function<String, N5Reader> n5Fun,
			final N5MetadataParser<?>... parsers) {

		this(n5Fun, x -> "", parsers);
	}

	public TransformSelectorDialog(
			final N5Reader n5,
			final N5MetadataParser<?>... parsers) {

		this.n5 = n5;
		this.pathFun = x -> "";
		this.initialContainerPath = "";

		this.parsers = parsers;

		final ImageJ ij = IJ.getInstance();
		if (ij != null)
			ijProgressBar = ij.getProgressBar();

		detectCalled = false;
	}

	public N5MetadataTranslationPanel getTranslationPanel() {

		return translationPanel;
	}

	public TranslationResultPanel getTranslationResultPanel() {

		return translationResultPanel;
	}

	public void setLoaderExecutor(final ExecutorService loaderExecutor) {

		this.loaderExecutor = loaderExecutor;
	}

	public void setTreeRenderer(final TreeCellRenderer treeRenderer) {

		this.treeRenderer = treeRenderer;
	}

	public void setRecursiveFilterCallback(final Predicate<N5TreeNode> n5NodeFilter) {

		this.n5NodeFilter = n5NodeFilter;
	}

	/**
	 * Sets a selection filter. A {@link N5TreeNode} will not be selectable if the 
	 * selection filter returns false for its metadata.
	 * 
	 * @param selectionFilter the predicate
	 */
	public void setSelectionFilter(final Predicate<N5Metadata> selectionFilter) {

		this.selectionFilter = selectionFilter;
	}

	public void setCancelCallback(final Consumer<Void> cancelCallback) {

		this.cancelCallback = cancelCallback;
	}

	public void setContainerPathUpdateCallback(final Consumer<String> containerPathUpdateCallback) {

		this.containerPathUpdateCallback = containerPathUpdateCallback;
	}

	public void setMessage(final String message) {

		messageLabel.setText(message);
	}

	public void setVirtualOption(final boolean arg) {

		virtualOption = arg;
	}

	public void setCropOption(final boolean arg) {

		this.cropOption = arg;
	}

	public boolean getCropOption() {

		return cropOption;
	}

	public boolean isCropSelected() {

		return cropOption && cropBox.isSelected();
	}

	public boolean isVirtual() {

		return (virtualBox != null) && virtualBox.isSelected();
	}

	public String getN5RootPath() {

		return containerPathText.getText().trim();
	}

	public void setLoaderThread(final Thread loaderThread) {

		this.loaderThread = loaderThread;
	}

	public void run(final Consumer<DataSelection> okCallback) {

		this.okCallback = okCallback;
		dialog = buildDialog();

		if (n5 == null) {
			browseBtn.addActionListener(e -> openContainer(n5Fun, this::openBrowseDialog));
			detectBtn.addActionListener(e -> openContainer(n5Fun, () -> getN5RootPath(), pathFun));
		}

		// ok and cancel buttons
		okBtn.addActionListener(e -> ok());
		cancelBtn.addActionListener(e -> cancel());
		dialog.setVisible(true);
	}

	private static final int DEFAULT_OUTER_PAD = 8;
	private static final int DEFAULT_BUTTON_PAD = 3;
	private static final int DEFAULT_MID_PAD = 5;

	private JFrame buildDialog() {

		final int OUTER_PAD = DEFAULT_OUTER_PAD;
		final int BUTTON_PAD = DEFAULT_BUTTON_PAD;
		final int MID_PAD = DEFAULT_MID_PAD;

		final int frameSizeX = UIScale.scale(600);
		final int frameSizeY = UIScale.scale(400);

		dialog = new JFrame("Open N5");
		dialog.setPreferredSize(new Dimension(frameSizeX, frameSizeY));
		dialog.setMinimumSize(dialog.getPreferredSize());

		final Container pane = dialog.getContentPane();
		final JTabbedPane tabs = new JTabbedPane();
		pane.add(tabs);

		final JPanel panel = new JPanel(false);
		panel.setLayout(new GridBagLayout());
		tabs.addTab("Main", panel);
		tabs.addTab("Spatial Metadata", spatialMetaSpec.buildPanel());
		tabs.addTab("Metadata Translation", translationPanel.buildPanel());
		tabs.addTab("Translation Result", translationResultPanel.buildPanel());

		containerPathText = new org.janelia.saalfeldlab.n5.ui.ImprovedFormattedTextField(
				new UriValidator(), initialContainerPath);
		containerPathText.setPreferredSize(new Dimension(frameSizeX / 3, containerPathText.getPreferredSize().height));
		containerPathText.addActionListener(e -> openContainer(n5Fun, () -> getN5RootPath(), pathFun));

		final GridBagConstraints ctxt = new GridBagConstraints();
		ctxt.gridx = 0;
		ctxt.gridy = 0;
		ctxt.gridwidth = 3;
		ctxt.gridheight = 1;
		ctxt.weightx = 1.0;
		ctxt.weighty = 0.0;
		ctxt.fill = GridBagConstraints.HORIZONTAL;
		ctxt.insets = new Insets(OUTER_PAD, OUTER_PAD, MID_PAD, BUTTON_PAD);
		panel.add(containerPathText, ctxt);

		browseBtn = new JButton("Browse");
		final GridBagConstraints cbrowse = new GridBagConstraints();
		cbrowse.gridx = 3;
		cbrowse.gridy = 0;
		cbrowse.gridwidth = 1;
		cbrowse.gridheight = 1;
		cbrowse.weightx = 0.0;
		cbrowse.weighty = 0.0;
		cbrowse.fill = GridBagConstraints.HORIZONTAL;
		cbrowse.insets = new Insets(OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD);
		panel.add(browseBtn, cbrowse);

		detectBtn = new JButton("Detect datasets");
		final GridBagConstraints cdetect = new GridBagConstraints();
		cdetect.gridx = 4;
		cdetect.gridy = 0;
		cdetect.gridwidth = 2;
		cdetect.gridheight = 1;
		cdetect.weightx = 0.0;
		cdetect.weighty = 0.0;
		cdetect.fill = GridBagConstraints.HORIZONTAL;
		cdetect.insets = new Insets(OUTER_PAD, BUTTON_PAD, MID_PAD, OUTER_PAD);
		panel.add(detectBtn, cdetect);

		final GridBagConstraints ctree = new GridBagConstraints();
		ctree.gridx = 0;
		ctree.gridy = 1;
		ctree.gridwidth = 6;
		ctree.gridheight = 3;
		ctree.weightx = 1.0;
		ctree.weighty = 1.0;
		ctree.ipadx = 0;
		ctree.ipady = 0;
		ctree.insets = new Insets(0, OUTER_PAD, 0, OUTER_PAD);
		ctree.fill = GridBagConstraints.BOTH;

		treeModel = new DefaultTreeModel(null);
		containerTree = new JTree(treeModel);
		containerTree.setMinimumSize(new Dimension(550, 230));

		containerTree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

		// disable selection of nodes that are not open-able
		final N5IjTreeSelectionListener treeSelectionListener = new N5IjTreeSelectionListener(containerTree.getSelectionModel());
		treeSelectionListener.setSelectionFilter(selectionFilter);
		containerTree.addTreeSelectionListener(treeSelectionListener);

		// By default leaf nodes (datasets) are displayed as files. This changes
		// the default behavior to display them as folders
		// final DefaultTreeCellRenderer treeCellRenderer =
		// (DefaultTreeCellRenderer) containerTree.getCellRenderer();
		if (treeRenderer != null)
			containerTree.setCellRenderer(treeRenderer);

		final JScrollPane treeScroller = new JScrollPane(containerTree);
		treeScroller.setViewportView(containerTree);
		treeScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		panel.add(treeScroller, ctree);

		// bottom button
		final GridBagConstraints cbot = new GridBagConstraints();
		cbot.gridx = 0;
		cbot.gridy = 4;
		cbot.gridwidth = 1;
		cbot.gridheight = 1;
		cbot.weightx = 0.0;
		cbot.weighty = 0.0;
		cbot.insets = new Insets(OUTER_PAD, OUTER_PAD, OUTER_PAD, OUTER_PAD);
		cbot.anchor = GridBagConstraints.CENTER;

		if (virtualOption) {
			final JPanel virtPanel = new JPanel();
			virtualBox = new JCheckBox();
			final JLabel virtLabel = new JLabel("Open as virtual");
			virtPanel.add(virtualBox);
			virtPanel.add(virtLabel);
			panel.add(virtPanel, cbot);
		}

		if (cropOption) {
			final JPanel cropPanel = new JPanel();
			cropBox = new JCheckBox();
			final JLabel cropLabel = new JLabel("Crop");
			cbot.gridx = 1;
			cbot.anchor = GridBagConstraints.WEST;
			cropPanel.add(cropBox);
			cropPanel.add(cropLabel);
			panel.add(cropPanel, cbot);
		}

		messageLabel = new JLabel("");
		messageLabel.setVisible(false);
		cbot.gridx = 2;
		cbot.anchor = GridBagConstraints.CENTER;
		panel.add(messageLabel, cbot);

		okBtn = new JButton("OK");
		cbot.gridx = 4;
		cbot.ipadx = 20;
		cbot.anchor = GridBagConstraints.EAST;
		cbot.fill = GridBagConstraints.HORIZONTAL;
		cbot.insets = new Insets(MID_PAD, OUTER_PAD, OUTER_PAD, BUTTON_PAD);
		panel.add(okBtn, cbot);

		cancelBtn = new JButton("Cancel");
		cbot.gridx = 5;
		cbot.ipadx = 0;
		cbot.anchor = GridBagConstraints.EAST;
		cbot.fill = GridBagConstraints.HORIZONTAL;
		cbot.insets = new Insets(MID_PAD, BUTTON_PAD, OUTER_PAD, OUTER_PAD);
		panel.add(cancelBtn, cbot);

		dialog.pack();
		return dialog;
	}

	public JTree getJTree() {

		return containerTree;
	}

	private String openBrowseDialog() {

		final JFileChooser fileChooser = new JFileChooser();
		/*
		 * Need to allow files so h5 containers can be opened, and directories
		 * so that filesystem n5's and zarrs can be opened.
		 */
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

		if (lastBrowsePath != null && !lastBrowsePath.isEmpty())
			fileChooser.setCurrentDirectory(new File(lastBrowsePath));
		else if (initialContainerPath != null && !initialContainerPath.isEmpty())
			fileChooser.setCurrentDirectory(new File(initialContainerPath));
		else if (IJ.getInstance() != null) {
			File f = null;

			final String currDir = IJ.getDirectory("current");
			final String homeDir = IJ.getDirectory("home");
			if (currDir != null)
				f = new File(currDir);
			else if (homeDir != null)
				f = new File(homeDir);

			fileChooser.setCurrentDirectory(f);
		}

		final int ret = fileChooser.showOpenDialog(dialog);
		if (ret != JFileChooser.APPROVE_OPTION)
			return null;

		final String path = fileChooser.getSelectedFile().getAbsolutePath();
		containerPathText.setText(path);
		lastBrowsePath = path;

		// callback after browse as well
		containerPathUpdateCallback.accept(path);

		return path;
	}

	private N5DatasetDiscoverer makeDiscoverer() {

		// copy list
		final ArrayList<N5MetadataParser<?>> parserList = new ArrayList<>();

		// add custom metadata parser into the first position in the list if it
		// exists
		final Optional<N5GenericSingleScaleMetadataParser> parserOptional = spatialMetaSpec.getParserOptional();
		if (parserOptional.isPresent()) {
			parserList.add(parserOptional.get());
			parserList.addAll(Arrays.asList(parsers));
		} else
			parserList.addAll(Arrays.asList(parsers));

		datasetDiscoverer = new N5DatasetDiscoverer(n5, loaderExecutor, n5NodeFilter,
				parserList, null);

		return datasetDiscoverer;
	}

	private N5Reader makeN5(final String n5Path) {

		n5 = n5Fun.apply(n5Path);
		if (n5 == null) {
			messageLabel.setVisible(false);
			dialog.repaint();
			return null;
		}

		final Gson gson;
		if (n5 instanceof CachedGsonKeyValueN5Reader)
			gson = ((CachedGsonKeyValueN5Reader)n5).getGson();
		else {
			final GsonBuilder gsonBuilder = new GsonBuilder();
			gsonBuilder.registerTypeAdapter(DataType.class, new DataType.JsonAdapter());
			gsonBuilder.registerTypeHierarchyAdapter(Compression.class, CompressionAdapter.getJsonAdapter());
			gsonBuilder.disableHtmlEscaping();
			gson = gsonBuilder.create();
		}

		final Optional<TranslatedN5Reader> translatedN5 = translationPanel.getTranslatedN5Optional(n5, gson);
		if (translatedN5.isPresent()) {
			n5 = translatedN5.get();
		}

		return n5;
	}

	private void openContainer(final Function<String, N5Reader> n5Fun, final Supplier<String> opener) {

		openContainer(n5Fun, opener, pathFun);
	}

	private void openContainer(final Function<String, N5Reader> n5Fun, final Supplier<String> opener,
			final Function<String, String> pathToRoot) {

		if (ijProgressBar != null)
			ijProgressBar.show(0.2);

		SwingUtilities.invokeLater(() -> {
			messageLabel.setText("Building reader...");
			messageLabel.setVisible(true);
			messageLabel.repaint();
		});

		// validate and update input
		containerPathText.validateAndUpdate();

		final String n5Path = opener.get();
		containerPathUpdateCallback.accept(n5Path);

		if (n5Path == null) {
			messageLabel.setVisible(false);
			dialog.repaint();
			return;
		}

		if (loaderExecutor == null) {
			loaderExecutor = Executors.newCachedThreadPool();
		}

		n5 = n5Fun.apply(n5Path);
		final String rootPath = pathToRoot.apply(n5Path).replaceFirst("^/", "");

		if (n5 == null) {
			messageLabel.setVisible(false);
			dialog.repaint();
			return;
		}

		final Gson gson;
		if (n5 instanceof CachedGsonKeyValueN5Reader)
			gson = ((CachedGsonKeyValueN5Reader)n5).getGson();
		else {
			final GsonBuilder gsonBuilder = new GsonBuilder();
			gsonBuilder.registerTypeAdapter(DataType.class, new DataType.JsonAdapter());
			gsonBuilder.registerTypeHierarchyAdapter(Compression.class, CompressionAdapter.getJsonAdapter());
			gsonBuilder.disableHtmlEscaping();
			gson = gsonBuilder.create();
		}

		boolean isTranslated = false;
		final Optional<TranslatedN5Reader> translatedN5 = translationPanel.getTranslatedN5Optional(n5, gson);
		if (translatedN5.isPresent()) {
			n5 = translatedN5.get();
			isTranslated = true;
		}

		datasetDiscoverer = makeDiscoverer();
		final String[] pathParts = n5.getURI().toString().split(n5.getGroupSeparator());
		final String suffix = (rootPath == null || rootPath.isEmpty()) ? "" : n5.getGroupSeparator() + rootPath;
		final String treeRootName = pathParts[pathParts.length - 1] + suffix;
		if (treeRenderer != null && treeRenderer instanceof N5DatasetTreeCellRenderer)
			((N5DatasetTreeCellRenderer)treeRenderer).setRootName(treeRootName);

		final N5TreeNode tmpRootNode = new N5TreeNode(rootPath);
		rootNode = new N5SwingTreeNode(rootPath, treeModel);
		treeModel.setRoot(rootNode);

		containerTree.setEnabled(true);
		containerTree.repaint();

		if (ijProgressBar != null)
			ijProgressBar.show(0.3);

		final Consumer<N5TreeNode> callback = (x) -> {
			SwingUtilities.invokeLater(() -> {
				if (x.getMetadata() != null) {
					// get the node at the requested path, or add it if not present
					final N5SwingTreeNode node = (N5SwingTreeNode)rootNode.getDescendants(y -> pathsEqual(y.getPath(), x.getPath())).findFirst()
							.orElseGet( () -> {
								if (rootNode.getPath().equals(x.getPath()))
									return null;
								else
									return rootNode.addPath(x.getPath());
							});

					// update the node's metadata
					if (node != null) {
						// set metadata, update ui
						node.setMetadata(x.getMetadata());
						addTransformsToNode(node);

						// sort children, update ui
						final N5SwingTreeNode parent = (N5SwingTreeNode)node.getParent();
						sortRecursive(parent);

						treeModel.nodeChanged(node);
						expandTree();
					}
				} else {
					final Optional<N5TreeNode> desc = rootNode.getDescendant(x.getNodeName());
					if (desc.isPresent()) {
						final N5SwingTreeNode node = (N5SwingTreeNode)desc.get();
						if (node.getParent() != null && node.getChildCount() == 0) {
							treeModel.removeNodeFromParent(node);
						}

					}
				}
			});
		};

		parseExec = Executors.newSingleThreadExecutor();
		parseExec.submit(() -> {
			try {
				String[] datasetPaths;
				try {

					if (ijProgressBar != null)
						ijProgressBar.show(0.3);


					SwingUtilities.invokeLater(() -> {
						messageLabel.setText("Listing...");
						messageLabel.repaint();
					});

					// build a temporary tree
					datasetPaths = n5.deepList(rootPath, loaderExecutor);
					N5SwingTreeNode.fromFlatList(tmpRootNode, datasetPaths, "/");
					for (final String p : datasetPaths)
						rootNode.addPath(rootPath + "/" + p);


					sortRecursive(rootNode);
					containerTree.expandRow(0);

					if (ijProgressBar != null)
						ijProgressBar.show(0.5);

					SwingUtilities.invokeLater(() -> {
						messageLabel.setText("Parsing...");
						messageLabel.repaint();
					});

					// callback copies values from temporary tree into the ui
					// when metadata is parsed
					datasetDiscoverer.parseMetadataRecursive(tmpRootNode, callback);

					if (ijProgressBar != null)
						ijProgressBar.show(0.8);

					SwingUtilities.invokeLater(() -> {
						messageLabel.setText("Done");
						messageLabel.repaint();
					});

					if (ijProgressBar != null)
						ijProgressBar.show(1.1);

					Thread.sleep(1000);
					SwingUtilities.invokeLater(() -> {
						messageLabel.setText("");
						messageLabel.setVisible(false);
						messageLabel.repaint();
					});
				} catch (final InterruptedException e) {
					// can ignore
				} catch (final ExecutionException e) {
					// can ignore
				}
			} catch (final N5Exception e) {
				e.printStackTrace();
			}
		});

		if (isTranslated) {
			final TranslatedN5Reader xlatedN5 = (TranslatedN5Reader)n5;
			translationResultPanel.set(
					xlatedN5.getGson(),
					xlatedN5.getTranslation().getOrig(),
					xlatedN5.getTranslation().getTranslated());
		}

		detectCalled = true;
	}

	private void expandTree() {

		for (int i = 0; i < containerTree.getRowCount(); i++)
			containerTree.expandRow(i);
	}

	private void addTransformsToNode(final N5SwingTreeNode node) {

		N5Metadata metadata = node.getMetadata();
		if( metadata == null)
			return;

		if( !(metadata instanceof N5TransformMetadata ))
			return;

		int i = 0;
		final N5TransformMetadata tformMetadata = (N5TransformMetadata)metadata;
		for (CoordinateTransform<?> tform : tformMetadata.getTransforms()) {

			final String path = node.getPath() + "/" +
					String.format("coordinateTransformations[%d]", i);

			final N5SwingTreeNode child = new N5SwingTreeNode(path, node);
			child.setMetadata(new N5TransformMetadata(path, new CoordinateTransform[]{tform}));
			node.add(child);

			i++;
		}
		node.setMetadata(null);
	}

	public void ok() {

		// stop parsing things
		if (parseExec != null)
			parseExec.shutdownNow();

		// validate and update input
		containerPathText.validateAndUpdate();

		final ArrayList<N5Metadata> selectedMetadata = new ArrayList<>();

		// check if we can skip explicit dataset detection
		final String n5Path = getN5RootPath();
		if (!detectCalled && !n5Path.isEmpty()) {

			containerPathUpdateCallback.accept(getN5RootPath());

			if( n5== null )
				n5 = makeN5(n5Path);

			if( datasetDiscoverer == null )
				datasetDiscoverer = makeDiscoverer();

			final String dataset = pathFun.apply(n5Path);
			try {

				final N5TreeNode root = datasetDiscoverer.discoverAndParseRecursive("");
				final Predicate<N5Metadata> filter = selectionFilter != null ? selectionFilter : x -> { return x != null; };
				root.getDescendant(dataset)
					.filter(x -> {
						return x.getMetadata() != null && filter.test(x.getMetadata());
					})
					.ifPresent(x -> {
						selectedMetadata.add(x.getMetadata());
					});
			} catch (final Exception e) {
				throw new N5Exception( "Failure to parse or find data at " + dataset, e);
			}

			if (selectedMetadata.isEmpty()) {
				JOptionPane.showMessageDialog(null, "Could not find a dataset / metadata at the provided path.");
				return;
			}
		} else {
			if( containerTree.getSelectionPath() == null ) {
				JOptionPane.showMessageDialog(null, "No dataset selected.");
				return;
			}

			// datasets were selected by the user
			for (final TreePath path : containerTree.getSelectionPaths())
				selectedMetadata.add(((N5SwingTreeNode)path.getLastPathComponent()).getMetadata());
		}
		okCallback.accept(new DataSelection(n5, selectedMetadata));
		dialog.setVisible(false);
		dialog.dispose();
	}

	public void cancel() {

		// stop parsing things
		if (parseExec != null)
			parseExec.shutdownNow();

		dialog.setVisible(false);
		dialog.dispose();

		if (loaderThread != null)
			loaderThread.interrupt();

		if (parserFuture != null) {
			parserFuture.cancel(true);
		}

		if (cancelCallback != null)
			cancelCallback.accept(null);
	}

	public void detectDatasets() {

		openContainer(n5Fun, () -> getN5RootPath(), pathFun);
	}

	/**
	 * Removes selected nodes that do not have metadata, and are therefore not
	 * openable.
	 */
	public static class N5IjTreeSelectionListener implements TreeSelectionListener {

		private final TreeSelectionModel selectionModel;

		private Predicate<N5Metadata> selectionFilter;

		public N5IjTreeSelectionListener(final TreeSelectionModel selectionModel) {

			this.selectionModel = selectionModel;
		}

		public void setSelectionFilter(final Predicate<N5Metadata> selectionFilter) {

			this.selectionFilter = selectionFilter;
		}

		@Override
		public void valueChanged(final TreeSelectionEvent sel) {

			int i = 0;
			for (final TreePath path : sel.getPaths()) {
				if (!sel.isAddedPath(i))
					continue;

				final Object last = path.getLastPathComponent();
				if (last instanceof N5SwingTreeNode) {
					final N5SwingTreeNode node = ((N5SwingTreeNode)last);
					if (node.getMetadata() == null) {
						selectionModel.removeSelectionPath(path);
					}
					else if( selectionFilter != null && !selectionFilter.test(node.getMetadata()) ) {
						selectionModel.removeSelectionPath(path);
					}
				}
				i++;
			}
		}
	}

	private void sortRecursive(final N5SwingTreeNode node) {

		if (node != null) {
			final List<N5TreeNode> children = node.childrenList();
			if (!children.isEmpty()) {
				children.sort(Comparator.comparing(N5TreeNode::toString, comp));
			}
			treeModel.nodeStructureChanged(node);
			for (final N5TreeNode child : children)
				sortRecursive((N5SwingTreeNode)child);
		}
	}

	private static String normalDatasetName(final String fullPath, final String groupSeparator) {

		return fullPath.replaceAll("(^" + groupSeparator + "*)|(" + groupSeparator + "*$)", "");
	}

	private static boolean pathsEqual(final String a, final String b) {

		return normalDatasetName(a, "/").equals(normalDatasetName(b, "/"));
	}

	public static class UriValidator extends AbstractFormatter {

		private static final long serialVersionUID = 6765664180035018335L;

		@Override
		public Object stringToValue(String inputArg) throws ParseException {

			if (inputArg == null || inputArg.isEmpty())
				return null;

			// Check if the input starts with a format string 
			// one of "h5:", "hdf5:", "n5:", "zarr:", etc
			String fmt = null;
			String input = inputArg;
			final Pair<StorageFormat, String> fmtAndUri = N5Factory.StorageFormat.getStorageFromNestedScheme(input);
			final StorageFormat format = fmtAndUri.getA();
			if (format != null)
				fmt = format.toString().toLowerCase() + "://";

			input = fmtAndUri.getB();

			N5URI n5uri = null;
			try {
				final URI uri = new URI(input.trim());
				if (uri.isAbsolute())
					return addTypeScheme(fmt, uri.normalize());
				else {
					n5uri = new N5URI(uri);
				}
			} catch (final Throwable ignore) {}

			try {
				if (n5uri != null) {
					final N5URI pathOnly = new N5URI(Paths.get(n5uri.getContainerPath()).normalize().toUri());
					final URI uri = n5uri.getURI();
					final N5URI queryFragmentOnly = N5URI.from("",
						uri.getQuery() == null ? null : n5uri.getGroupPath(),
						uri.getFragment() == null ? null : n5uri.getAttributePath());

					return addTypeScheme(fmt, pathOnly.resolve(queryFragmentOnly).getURI().normalize());
				} else
					return addTypeScheme(fmt, Paths.get(input.trim()).normalize().toUri());

			} catch (final Throwable ignore) {}

			throw new ParseException("input " + input + " not a valid URI", 0);
		}

		private URI addTypeScheme(String typeScheme, URI uri) {

			if (typeScheme == null)
				return uri;
			else
				return URI.create(typeScheme + uri.toString());

		}

		@Override
		public String valueToString(Object arg) throws ParseException {

			if( arg instanceof URI )
				return ((URI)arg).toString();
			else
				throw new ParseException("input " + arg + " not a valid URI", 0);
		}

	}

}
