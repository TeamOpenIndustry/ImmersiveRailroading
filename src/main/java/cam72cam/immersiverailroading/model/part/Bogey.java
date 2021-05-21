package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.math.Vec3d;

import java.util.List;
import java.util.stream.Collectors;

public class Bogey {
    private final ModelComponent bogey;
    private final List<Wheel> wheels;

    public static Bogey get(ComponentProvider provider, boolean unified, String pos) {
        List<Wheel> wheels = (unified ?
                provider.parseAll(ModelComponentType.BOGEY_POS_WHEEL_X, pos) :
                provider.parseAll(pos.equals("FRONT") ? ModelComponentType.BOGEY_FRONT_WHEEL_X : ModelComponentType.BOGEY_REAR_WHEEL_X)
        ).stream().map(Wheel::new).collect(Collectors.toList());

        ModelComponent bogey = unified ?
                provider.parse(ModelComponentType.BOGEY_POS, pos) :
                provider.parse(pos.equals("FRONT") ? ModelComponentType.BOGEY_FRONT : ModelComponentType.BOGEY_REAR);

        return bogey == null ? null : new Bogey(bogey, wheels);
    }

    public Bogey(ModelComponent bogey, List<Wheel> wheels) {
        this.bogey = bogey;
        this.wheels = wheels;
    }

    public Vec3d center() {
        return bogey.center;
    }

    public void render(double distance, ComponentRenderer draw) {
        draw.render(bogey);
        for (Wheel wheel : wheels) {
            wheel.render(wheel.angle(distance), draw);
        }
    }
}
