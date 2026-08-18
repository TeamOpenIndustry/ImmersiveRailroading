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
        for (Modifier modifier : modifiers) {
            modifier.apply(stock, sound);
        }
    }

    public record Modifier(ModifierType type, String controller, ControllerType controllerType, Curve curve) {

        public void apply(EntityRollingStock stock, ISound sound) {
            float state = switch (controllerType) {
                case CONTROL_GROUP -> stock.getControlPosition(controller);
                case READOUT -> Readouts.valueOf(controller.toUpperCase()).getValue(stock);
            };
            float value = (float) curve.interpolate(state);

            switch (type) {
                case VOLUME -> sound.setVolume(value);
                case PITCH -> sound.setPitch(value);
            }
        }
    }

    public enum ModifierType {
        VOLUME,
        PITCH
    }
}
