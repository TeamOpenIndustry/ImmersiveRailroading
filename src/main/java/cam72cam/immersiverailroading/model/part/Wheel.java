package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.math.Vec3d;
import util.Matrix4;

import java.util.function.Function;

public class Wheel {
    public final ModelComponent wheel;

    protected Wheel(ModelComponent wheel, ModelState state, Function<EntityMoveableRollingStock, Float> angle) {
        this.wheel = wheel;
        Vec3d wheelPos = wheel.center;

        state.push(settings -> settings.add((ModelState.Animator) stock ->
                new Matrix4()
                        .translate(wheelPos.x, wheelPos.y, wheelPos.z)
                        .rotate(Math.toRadians(angle != null ?
                                        angle.apply(stock) :
                                        this.angle(stock.distanceTraveled)),
                                0, 0, 1)
                        .translate(-wheelPos.x, -wheelPos.y, -wheelPos.z))
        ).include(wheel);
    }

    public double diameter() {
        return wheel.height();
    }
    public float angle(double distance) {
        double circumference = diameter() * (float) Math.PI;
        double relDist = distance % circumference;
        return (float) (360 * relDist / circumference);
    }
}
