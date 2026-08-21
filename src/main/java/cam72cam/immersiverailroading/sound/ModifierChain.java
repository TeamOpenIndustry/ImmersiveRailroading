package cam72cam.immersiverailroading.sound;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.ControllerType;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.sound.ISound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ModifierChain {
    public final List<Modifier> modifiers;

    public ModifierChain(DataBlock json, Map<String, Curve> curves) {
        this.modifiers = new ArrayList<>();

        json.getBlocks("modifier").forEach(b -> {
            ModifierType type = ModifierType.valueOf(b.getValue("type").asString().toUpperCase());

            String readout = b.getValue("readout").asString();
            ControllerType controllerType = readout != null ? ControllerType.READOUT : ControllerType.CONTROL_GROUP;
            String controller = readout != null ? readout : b.getValue("control_group").asString();

            Curve curve = curves.get(b.getValue("curve").asString());

            modifiers.add(new Modifier(type, controller, controllerType, curve));
        });
    }

    public void apply(EntityRollingStock stock, ISound sound) {
        float newVolume = -1;
        float newPitch = -1;

        for (Modifier modifier : modifiers) {
            float newVal = modifier.get(stock);
            switch (modifier.type) {
                case VOLUME -> newVolume = newVolume != -1 ? newVolume * newVal : newVal;
                case PITCH -> newPitch = newPitch != -1? newPitch * newVal : newVal;
            }
        }

        if (newVolume != -1) {
            sound.setVolume(newVolume);
        }

        if (newPitch != -1) {
            sound.setPitch(newPitch);
        }
    }

    public record Modifier(ModifierType type, String controller, ControllerType controllerType, Curve curve) {

        public float get(EntityRollingStock stock) {
            float state = switch (controllerType) {
                case CONTROL_GROUP -> stock.getControlPosition(controller);
                case READOUT -> Readouts.valueOf(controller.toUpperCase()).getValue(stock);
            };
            return (float) curve.interpolate(state);
        }
    }

    public enum ModifierType {
        VOLUME,
        PITCH
    }
}
