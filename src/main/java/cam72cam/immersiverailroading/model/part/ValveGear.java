package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.library.ValveGearConfig;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ComponentProvider;

public interface ValveGear {
    void effects(EntityMoveableRollingStock stock, float throttle);

    static ValveGear get(WheelSet wheels, ValveGearConfig type, ComponentProvider provider, ModelState state, ModelPosition pos, float angleOffset) {
        if (type == null) {
            return null;
        }
        switch (type.type) {
            case WALSCHAERTS:
                return WalschaertsValveGear.get(wheels, provider, state, pos, angleOffset);
            case STEPHENSON:
                return StephensonValveGear.get(wheels, provider, state, pos, angleOffset);
            case CONNECTING:
                return ConnectingRodValveGear.get(wheels, provider, state, pos, angleOffset);
            case CUSTOM:
                return CustomValveGear.get(type.custom, wheels, provider, state, pos);
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
