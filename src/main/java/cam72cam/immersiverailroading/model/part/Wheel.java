package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.math.Vec3d;
import org.lwjgl.opengl.GL11;

public class Wheel {
    protected final ModelComponent wheel;

    protected Wheel(ModelComponent wheel) {
        this.wheel = wheel;
    }

    public double diameter() {
        return wheel.height();
    }
    public float angle(double distance) {
        double circumference = diameter() * (float) Math.PI;
        double relDist = distance % circumference;
        return (float) (360 * relDist / circumference);
    }

    public void render(float angle, ComponentRenderer draw) {
        Vec3d wheelPos = wheel.center;
        try (ComponentRenderer matrix = draw.push()) {
            GL11.glTranslated(wheelPos.x, wheelPos.y, wheelPos.z);
            GL11.glRotated(angle, 0, 0, 1);
            GL11.glTranslated(-wheelPos.x, -wheelPos.y, -wheelPos.z);
            matrix.render(wheel);
        }
    }
}
