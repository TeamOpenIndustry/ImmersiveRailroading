package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.library.ValveGearType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;

public interface ValveGear {
    void render(double distance, float throttle, boolean reverse, ComponentRenderer draw);

    static ValveGear get(DrivingWheels wheels, ValveGearType type, ComponentProvider provider, String pos) {
        switch (type) {
            case WALSCHAERTS:
            case TRI_WALSCHAERTS:
            case MALLET_WALSCHAERTS:
            case GARRAT:
                return WalschaertsValveGear.get(wheels, provider, pos);
            case STEPHENSON:
            case T1:
                return StephensonValveGear.get(wheels, provider, pos);
            case SHAY:
            case CLIMAX:
            case HIDDEN:
            default:
                return null;
        }
    }
}
