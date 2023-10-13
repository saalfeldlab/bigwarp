package bdv.ui.appearance;

import java.io.File;

public class BwAppearanceManager extends AppearanceManager {

	public BwAppearanceManager() {

		super();
	}

	public BwAppearanceManager(final String configDir) {

		super(configDir);
	}

	@Override
	void load(final String filename) {

		final File appearanceFile = new File(filename);
		final boolean appearanceFileExists = appearanceFile.exists();
		super.load(filename);
		if (!appearanceFileExists)
			save(filename);
	}

}
