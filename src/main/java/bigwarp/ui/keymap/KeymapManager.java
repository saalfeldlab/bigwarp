package bigwarp.ui.keymap;

import bdv.KeyConfigScopes;
import bdv.ui.keymap.AbstractKeymapManager;
import bdv.ui.keymap.Keymap;
import bigwarp.BigWarpActions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.scijava.Context;
import org.scijava.plugin.PluginService;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionsBuilder;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

/**
 * Manages a collection of {@link Keymap}.
 * <p>
 * Provides de/serialization of user-defined keymaps.
 *
 * @author Tobias Pietzsch
 * @author John Bogovic
 */
public class KeymapManager extends AbstractKeymapManager< KeymapManager >
{
	private static final String CONFIG_DIR_NAME = "keymap/";

	private final String configFile;

	private CommandDescriptions descriptions;

	public KeymapManager(final String configDir) {

		this(true, configDir);
		discoverCommandDescriptions();
	}

	public KeymapManager() {

		this(false, null);
	}

	private KeymapManager(final boolean loadStyles, final String configDir) {

		configFile = configDir == null ? null : configDir + "/" + CONFIG_DIR_NAME;
		if (loadStyles)
			loadStyles();
	}

	public synchronized void setCommandDescriptions(final CommandDescriptions descriptions) {

		this.descriptions = descriptions;
		final Consumer<Keymap> augmentInputTriggerConfig = k -> descriptions.augmentInputTriggerConfig(k.getConfig());
		builtinStyles.forEach(augmentInputTriggerConfig);
		userStyles.forEach(augmentInputTriggerConfig);
	}

	public CommandDescriptions getCommandDescriptions() {

		return descriptions;
	}

	/**
	 * Discover all {@code CommandDescriptionProvider}s with scope {@link KeyConfigScopes#BIGDATAVIEWER},
	 * build {@code CommandDescriptions}, and set it.
	 */
	public synchronized void discoverCommandDescriptions()
	{
		final CommandDescriptionsBuilder builder = new CommandDescriptionsBuilder();
		final Context context = new Context( PluginService.class );
		context.inject( builder );
		builder.discoverProviders( BigWarpActions.BIGWARP );
		context.dispose();
		setCommandDescriptions( builder.build() );
	}

	@Override
	protected List<Keymap> loadBuiltinStyles() {

		synchronized (KeymapManager.class) {
			// TODO figures this out
			if (loadedBuiltinStyles == null)
				try {
					loadedBuiltinStyles = Arrays.asList(
							loadBuiltinStyle("Default", "default.yaml"));
				} catch (final Exception e) {
					e.printStackTrace();
					loadedBuiltinStyles = Arrays.asList(createDefaultStyle("Default"));
				}
		}

		return loadedBuiltinStyles;
	}

	@Override
	protected void loadStyles(final File directory) throws IOException {

		final File file = new File(directory, "/keymaps.yaml");
		final boolean keymapFileExists = file.exists();
		super.loadStyles(directory);
		if (!keymapFileExists)
			saveStyles(directory);
	}

	private static List< Keymap > loadedBuiltinStyles;

	private static Keymap loadBuiltinStyle(final String name, final String filename) throws IOException {

		try (Reader reader = new InputStreamReader(KeymapManager.class.getResourceAsStream(filename))) {
			return new Keymap(name, new InputTriggerConfig(YamlConfigIO.read(reader)));
		}
	}

	private static Keymap createDefaultStyle(final String name) {

		final Context context = new Context(PluginService.class);
		final InputTriggerConfig defaultKeyconfig = DumpInputConfig.buildCommandDescriptions(context).createDefaultKeyconfig();
		return new Keymap(name, defaultKeyconfig);
	}

	public void loadStyles() {

		try {
			loadStyles(new File(configFile));
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void saveStyles() {

		try {
			saveStyles(new File(configFile));
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

}
