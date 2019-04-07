package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.model.TrackModel;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrackDefinition {
    public final String trackID;
    public final String name;
    public final List<TrackModel> models;

    public TrackDefinition(String trackID, JsonObject object) throws Exception {
        this.trackID = trackID;
        this.name = object.get("name").getAsString();
        this.models = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("models").entrySet()) {
            models.add(new TrackModel(entry.getKey(), entry.getValue().getAsString()));
        }
    }
    public TrackModel getTrackForGauge(double gauge) {
        for (TrackModel model : models) {
            if (model.canRender(gauge)) {
                return model;
            }
        }
        ImmersiveRailroading.warn("Bad track gauge def for %s - %s", trackID, gauge);
        return models.get(0);
    }
}
