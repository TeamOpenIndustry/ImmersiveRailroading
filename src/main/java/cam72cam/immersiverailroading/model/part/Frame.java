package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ValveGearType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;

import java.util.List;
import java.util.stream.Collectors;

public class Frame {
    private final ModelComponent frame;
    private final List<Wheel> wheels;
    private final ValveGear valveGearRight;
    private final ValveGear valveGearLeft;

    public Frame(ComponentProvider provider, String blame, ValveGearType type) {
        this.wheels = provider.parseAll(ModelComponentType.FRAME_WHEEL_X)
                .stream().map(Wheel::new).collect(Collectors.toList());

        this.frame = provider.parse(ModelComponentType.FRAME);
        if (frame == null) {
            ImmersiveRailroading.warn("Invalid model: Missing FRAME for %s!  (this will fail in future versions of IR)", blame);
        }
        valveGearRight = ValveGear.get(wheels, type, provider, "RIGHT", -90);
        valveGearLeft = ValveGear.get(wheels, type, provider, "LEFT", 0);
    }

    public void render(double distance, ComponentRenderer draw) {
        draw.render(frame);
        for (Wheel wheel : wheels) {
            wheel.render(valveGearRight != null ?
                    valveGearRight.angle(distance) + 90 :
                    valveGearLeft != null ?
                            valveGearLeft.angle(distance) + 0 :
                            wheel.angle(distance),
                    draw);
        }
        if (valveGearRight != null) {
            valveGearRight.render(distance, 0, draw);
        }
        if (valveGearLeft != null) {
            valveGearLeft.render(distance, 0, draw);
        }
    }
}
