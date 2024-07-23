package cam72cam.immersiverailroading.registry.parts;

import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.resource.Identifier;

import java.util.Locale;

public class AnimationDefinition {
    public enum AnimationMode {
        VALUE,
        PLAY_FORWARD,
        PLAY_REVERSE,
        PLAY_BOTH,
        LOOP,
        LOOP_SPEED
    }

    public final String control_group;
    public final AnimationMode mode;
    public final Readouts readout;
    public final Identifier animatrix;
    public final float offset;
    public final boolean invert;
    public final float frames_per_tick;
    public final SoundDefinition sound;

    public AnimationDefinition(DataBlock obj) {
        control_group = obj.getValue("control_group").asString();
        String readout = obj.getValue("readout").asString();
        this.readout = readout != null ? Readouts.valueOf(readout.toUpperCase(Locale.ROOT)) : null;
        if (control_group == null && readout == null) {
            throw new IllegalArgumentException("Must specify either a control group or a readout for an animation");
        }
        animatrix = obj.getValue("animatrix").asIdentifier();
        mode = AnimationMode.valueOf(obj.getValue("mode").asString().toUpperCase(Locale.ROOT));
        offset = obj.getValue("offset").asFloat(0f);
        invert = obj.getValue("invert").asBoolean(false);
        frames_per_tick = obj.getValue("frames_per_tick").asFloat(1f);
        sound = SoundDefinition.getOrDefault(obj, "sound");
    }

    public boolean valid() {
        return animatrix != null && (control_group != null || readout != null);
    }
}
