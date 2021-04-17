package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.mod.math.Vec3d;

import java.util.List;
import java.util.stream.Collectors;

public class DrivingWheels {
    protected final List<Wheel> wheels;
    private final Vec3d center;

    public static DrivingWheels get(ComponentProvider provider, String pos) {
        List<Wheel> wheels = (pos == null ?
                provider.parseAll(ModelComponentType.WHEEL_DRIVER_X) :
                provider.parseAll(ModelComponentType.WHEEL_DRIVER_POS_X, pos)
        ).stream().map(Wheel::new).collect(Collectors.toList());

        return wheels.isEmpty() ? null : new DrivingWheels(wheels);
    }

    public DrivingWheels(List<Wheel> wheels) {
        this.wheels = wheels;

        double minX = wheels.get(0).wheel.min.x;
        double minY = wheels.get(0).wheel.min.y;
        double minZ = wheels.get(0).wheel.min.z;
        double maxX = wheels.get(0).wheel.max.x;
        double maxY = wheels.get(0).wheel.max.y;
        double maxZ = wheels.get(0).wheel.max.z;

        for (Wheel rc : wheels) {
            minX = Math.min(minX, rc.wheel.min.x);
            minY = Math.min(minY, rc.wheel.min.y);
            minZ = Math.min(minZ, rc.wheel.min.z);
            maxX = Math.max(maxX, rc.wheel.max.x);
            maxY = Math.max(maxY, rc.wheel.max.y);
            maxZ = Math.max(maxZ, rc.wheel.max.z);
        }
        Vec3d min = new Vec3d(minX, minY, minZ);
        Vec3d max = new Vec3d(maxX, maxY, maxZ);
        center = min.add(max).scale(.5);
    }

    public double diameter() {
        return wheels.get(wheels.size() / 2).diameter();
    }
    public float angle(double distance) {
        return wheels.get(wheels.size() / 2).angle(distance);
    }

    public Vec3d center() {
        return center;
    }

    public void render(double distance, ComponentRenderer draw) {
        float angle = angle(distance);
        for (Wheel wheel : wheels) {
            wheel.render(angle, draw);
        }
    }
}
