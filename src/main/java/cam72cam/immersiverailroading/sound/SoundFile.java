package cam72cam.immersiverailroading.sound;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.sound.ISound;

import java.util.*;

public class SoundFile {
    public final String name;
    public final float volume;
    public final float pitch;
    public final int distance;
    public final List<RandomDef> files;

    public SoundFile(DataBlock json) {
        this.name = json.getValue("name").asString();
        this.volume = json.getValue("volume").asFloat(1.0f);
        this.pitch = json.getValue("pitch").asFloat(1.0f);
        this.distance = json.getValue("distance").asInteger(10);
        Identifier file = json.getValue("file").asIdentifier();
        if (file != null) {
            this.files = Collections.singletonList(new RandomDef(1, file));
        } else {
            files = new ArrayList<>();
            json.getBlocks("files").forEach(f -> {
                float likelihood = f.getValue("likelihood").asFloat();
                Identifier randFile = f.getValue("file").asIdentifier();
                files.add(new RandomDef(likelihood, randFile));
            });
        }
    }

    public Map<ISound, Float> create(EntityRollingStock stock) {
        Map<ISound, Float> sounds = new HashMap<>();

        for (RandomDef file : files) {
            ISound sound =  stock.createSound(file.file(), true, distance, ConfigSound.SoundCategories.RollingStock::general);
            sound.setPitch(pitch);
            sound.setVolume(volume);
            sounds.put(sound, file.likelihood());
        }

        return sounds;
    }

    public record RandomDef(float likelihood, Identifier file) { }
}
