package cam72cam.immersiverailroading.registry;


import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.resource.Identifier;

public class AnimationDefinition {
    public final Identifier animatrix;
    public final float offset;
    public final boolean invert;
    public final float frames_per_tick;

    public AnimationDefinition(DataBlock obj) {
        animatrix = obj.getValue("animatrix").asIdentifier();
        offset = obj.getValue("offset").asFloat(0f);
        invert = obj.getValue("invert").asBoolean(false);
        frames_per_tick = obj.getValue("frames_per_tick").asFloat(1f);
    }
}
