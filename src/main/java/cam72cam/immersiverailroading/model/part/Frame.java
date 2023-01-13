package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.library.ValveGearConfig;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;

public class Frame {
    private final ModelComponent frame;
    private final WheelSet wheels;
    private final ValveGear valveGearRight;
    private final ValveGear valveGearLeft;

    public Frame(ComponentProvider provider, String blame, ValveGearConfig type) {
        this.wheels = WheelSet.get(provider, ModelComponentType.FRAME_WHEEL_X, 0);
        this.frame = provider.parse(ModelComponentType.FRAME);
        if (frame == null) {
            ImmersiveRailroading.warn("Invalid model: Missing FRAME for %s!  (this will fail in future versions of IR)", blame);
        }
        valveGearRight = wheels != null ? ValveGear.get(wheels, type, provider, ModelPosition.RIGHT, -90) : null;
        valveGearLeft = wheels != null ? ValveGear.get(wheels, type, provider, ModelPosition.LEFT, 0) : null;
    }

    public void render(double distance, ComponentRenderer draw) {
        draw.render(frame);
        if (wheels != null) {
            wheels.render(distance, draw);
        }
        if (valveGearRight != null) {
            valveGearRight.render(distance, 0, draw);
        }
        if (valveGearLeft != null) {
            valveGearLeft.render(distance, 0, draw);
        }
    }
}
