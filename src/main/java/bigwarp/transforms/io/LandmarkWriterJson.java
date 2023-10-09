package bigwarp.transforms.io;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import bigwarp.landmarks.LandmarkTableModel;

public class LandmarkWriterJson {

	private final Gson gson;

	public LandmarkWriterJson() {
		gson = new Gson();
	}

	public JsonElement toJson(final LandmarkTableModel ltm) {

		return ltm.toJson();
	}

}
