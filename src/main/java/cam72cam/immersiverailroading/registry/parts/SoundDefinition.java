package cam72cam.immersiverailroading.registry.parts;

import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.resource.Identifier;

public class SoundDefinition {
    public final Identifier start;
    public final Identifier main;
    public final boolean looping;
    public final Identifier stop;
    public final Float distance;
    public final float volume;

    public SoundDefinition(Identifier fallback) {
        // Simple
        start = null;
        main = fallback;
        looping = true;
        stop = null;
        distance = null;
        volume = 1;
    }

    public SoundDefinition(DataBlock obj) {
        start = obj.getValue("start").asIdentifier();
        main = obj.getValue("main").asIdentifier();
        looping = obj.getValue("looping").asBoolean(true);
        stop = obj.getValue("stop").asIdentifier();
        distance = obj.getValue("distance").asFloat();
        volume = obj.getValue("volume").asFloat(1.0f);
    }

    public static SoundDefinition getOrDefault(DataBlock block, String key) {
        DataBlock found = block.getBlock(key);
        if (found != null) {
            return new SoundDefinition(found);
        }
        Identifier ident = block.getValue(key).asIdentifier();
        if (ident != null && ident.canLoad()) {
            return new SoundDefinition(ident);
        }
        return null;
    }
}
