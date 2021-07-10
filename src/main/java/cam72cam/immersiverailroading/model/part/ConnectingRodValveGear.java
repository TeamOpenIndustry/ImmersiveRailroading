package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.stream.Collectors;

public class ConnectingRodValveGear implements ValveGear {
    protected final List<Wheel> wheels;
    protected Vec3d centerOfWheels;
    protected final ModelComponent connectingRod;
    protected float angleOffset;

    public static ConnectingRodValveGear get(List<Wheel> wheels, ComponentProvider provider, String pos, float angleOffset) {
        ModelComponent connectingRod = provider.parse(ModelComponentType.SIDE_ROD_SIDE, pos);
        return connectingRod != null ? new ConnectingRodValveGear(wheels, connectingRod, angleOffset) : null;
    }

    public ConnectingRodValveGear(List<Wheel> wheels, ModelComponent connectingRod, float angleOffset) {
        this.wheels = wheels;
        this.connectingRod = connectingRod;
        this.angleOffset = angleOffset;
        this.centerOfWheels = ModelComponent.center(wheels.stream().map(x -> x.wheel).collect(Collectors.toList()));
    }

    public float angle(double distance) {
        return wheels.get(0).angle(distance) + angleOffset;
    }

    @Override
    public void render(double distance, float throttle, ComponentRenderer draw) {
        float wheelAngle = angle(distance);

        // Center of the connecting rod, may not line up with a wheel directly
        Vec3d connRodPos = connectingRod.center;
        // Wheel Center is the center of all wheels, may not line up with a wheel directly
        // The difference between these centers is the radius of the connecting rod movement
        double connRodRadius = connRodPos.x - centerOfWheels.x;
        // Find new connecting rod pos based on the connecting rod rod radius
        Vec3d connRodMovment = VecUtil.fromWrongYaw(connRodRadius, (float) wheelAngle);

        // Draw Connecting Rod
        try (ComponentRenderer matrix = draw.push()) {
            // Move to origin
            GL11.glTranslated(-connRodRadius, 0, 0);
            // Apply connection rod movement
            GL11.glTranslated(connRodMovment.x, connRodMovment.z, 0);

            matrix.render(connectingRod);
        }
    }

    @Override
    public void effects(EntityMoveableRollingStock stock, float throttle) {

    }

    @Override
    public boolean isEndStroke(EntityMoveableRollingStock stock, float throttle) {
        return false;
    }
}
