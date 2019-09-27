package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.resource.Identifier;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Quilling {
    public List<Chime> chimes = new ArrayList<>();
    public double maxPull;

    Quilling(JsonArray jsonElement) {
        for (JsonElement quill : jsonElement) {
            Chime chime = new Chime(quill.getAsJsonObject());
            chimes.add(chime);
            maxPull = Math.max(maxPull, chime.pull_end);
        }
    }

    Quilling(Identifier sample) {
        double pitchUp = 0.14;
        chimes.add(new Chime(0.15, 0.45, 0.75 + pitchUp, 0.85 + pitchUp, sample));
        chimes.add(new Chime(0.4, 0.55, 0.95 + pitchUp, 1 + pitchUp, sample));
        maxPull = 0.55;
    }

    public class Chime {
        public final double pull_start;
        public final double pull_end;
        public final double pitch_start;
        public final double pitch_end;
        public final Identifier sample;

        Chime(JsonObject data) {
            pull_start = data.get("pull_start").getAsDouble();
            pull_end = data.get("pull_end").getAsDouble();
            pitch_start = data.get("pitch_start").getAsDouble();
            pitch_end = data.get("pitch_end").getAsDouble();
            sample = new Identifier(ImmersiveRailroading.MODID, data.get("sample").getAsString());
        }

        Chime(double pull_start, double pull_end, double pitch_start, double pitch_end, Identifier sample) {
            this.pull_start = pull_start;
            this.pull_end = pull_end;
            this.pitch_start = pitch_start;
            this.pitch_end = pitch_end;
            this.sample = sample;
        }
    }

}
