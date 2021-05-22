package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.mod.math.Vec3d;

import java.util.List;
import java.util.stream.Collectors;

public class DrivingWheels {
    protected final List<Wheel> wheels;
    private final float angleOffset;

    public static DrivingWheels get(ComponentProvider provider, String pos, float angleOffset) {
        List<Wheel> wheels = (pos == null ?
                provider.parseAll(ModelComponentType.WHEEL_DRIVER_X) :
                provider.parseAll(ModelComponentType.WHEEL_DRIVER_POS_X, pos)
        ).stream().map(Wheel::new).collect(Collectors.toList());

        return wheels.isEmpty() ? null : new DrivingWheels(wheels, angleOffset);
    }

    public DrivingWheels(List<Wheel> wheels, float angleOffset) {
        this.wheels = wheels;
        this.angleOffset = angleOffset;
    }

    public void render(double distance, ComponentRenderer draw) {
        float angle = wheels.get(0).angle(distance) + angleOffset;
        for (Wheel wheel : wheels) {
            wheel.render(angle, draw);
        }
    }
}
