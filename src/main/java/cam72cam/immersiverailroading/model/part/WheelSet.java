package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;

import java.util.List;
import java.util.stream.Collectors;

public class WheelSet {
    protected final List<Wheel> wheels;
    private final float angleOffset;

    public static WheelSet get(ComponentProvider provider, ModelComponentType type, float angleOffset) {
        return get(provider, type, null, angleOffset);
    }

    public static WheelSet get(ComponentProvider provider, ModelComponentType type, String pos, float angleOffset) {
        List<Wheel> wheels = (pos == null ?
                provider.parseAll(type) :
                provider.parseAll(type, pos)
        ).stream().map(Wheel::new).collect(Collectors.toList());

        return wheels.isEmpty() ? null : new WheelSet(wheels, angleOffset);
    }

    public WheelSet(List<Wheel> wheels, float angleOffset) {
        this.wheels = wheels;
        this.angleOffset = angleOffset;
    }

    public float angle(double distance) {
        return wheels.get(0).angle(distance) + angleOffset;
    }

    public void render(double distance, ComponentRenderer draw) {
        float angle = angle(distance);
        for (Wheel wheel : wheels) {
            wheel.render(angle, draw);
        }
    }
}
