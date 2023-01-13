package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.library.ValveGearConfig;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;

public interface ValveGear {
    void render(double distance, float reverser, ComponentRenderer draw);

    void effects(EntityMoveableRollingStock stock, float throttle);

    static ValveGear get(WheelSet wheels, ValveGearConfig type, ComponentProvider provider, ModelPosition pos, float angleOffset) {
        if (type == null) {
            return null;
        }
        switch (type.type) {
            case WALSCHAERTS:
                return WalschaertsValveGear.get(wheels, provider, pos, angleOffset);
            case STEPHENSON:
                return StephensonValveGear.get(wheels, provider, pos, angleOffset);
            case CONNECTING:
                return ConnectingRodValveGear.get(wheels, provider, pos, angleOffset);
            case CUSTOM:
                return CustomValveGear.get(type.custom, wheels, provider, pos, angleOffset);
            case SHAY:
            case CLIMAX:
            case HIDDEN:
            default:
                return null;
        }
    }

    boolean isEndStroke(EntityMoveableRollingStock stock, float throttle);

    float angle(double distance);
}
