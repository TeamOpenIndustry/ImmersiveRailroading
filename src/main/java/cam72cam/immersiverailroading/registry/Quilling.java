package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.resource.Identifier;

import java.util.ArrayList;
import java.util.List;

public class Quilling {
    public List<Chime> chimes = new ArrayList<>();
    public double maxPull;

    Quilling(List<DataBlock> quilling) {
        for (DataBlock quill : quilling) {
            Chime chime = new Chime(quill);
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

    public boolean canLoad() {
        for(Chime chime : chimes) {
            if(!chime.sample.canLoad()) return false;
        }
        return true;
    }

    public static class Chime {
        public final double pull_start;
        public final double pull_end;
        public final double pitch_start;
        public final double pitch_end;
        public final Identifier sample;

        Chime(DataBlock data) {
            pull_start = data.getValue("pull_start").asDouble();
            pull_end = data.getValue("pull_end").asDouble();
            pitch_start = data.getValue("pitch_start").asDouble();
            pitch_end = data.getValue("pitch_end").asDouble();
            sample = data.getValue("sample").asIdentifier();
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
