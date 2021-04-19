package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import org.lwjgl.opengl.GL11;

public class StephensonValveGear implements ValveGear {
    protected final DrivingWheels wheels;
    protected final ModelComponent drivingRod;
    protected final ModelComponent connectingRod;
    protected final ModelComponent pistonRod;
    protected final ModelComponent cylinder;
    protected final float angleOffset;
    protected final boolean reverse;

    public static StephensonValveGear get(DrivingWheels wheels, ComponentProvider provider, String pos, float angleOffset) {
        ModelComponent drivingRod = provider.parse(ModelComponentType.MAIN_ROD_SIDE, pos);
        ModelComponent connectingRod = provider.parse(ModelComponentType.SIDE_ROD_SIDE, pos);
        ModelComponent pistonRod = provider.parse(ModelComponentType.PISTON_ROD_SIDE, pos);
        ModelComponent cylinder = provider.parse(ModelComponentType.CYLINDER_SIDE, pos);
        return drivingRod != null && connectingRod != null && pistonRod != null ?
                new StephensonValveGear(wheels, drivingRod, connectingRod, pistonRod, cylinder, angleOffset) : null;
    }
    public StephensonValveGear(DrivingWheels wheels, ModelComponent drivingRod, ModelComponent connectingRod, ModelComponent pistonRod, ModelComponent cylinder, float angleOffset) {
        this.wheels = wheels;
        this.drivingRod = drivingRod;
        this.connectingRod = connectingRod;
        this.pistonRod = pistonRod;
        this.cylinder = cylinder;
        this.angleOffset = angleOffset;
        this.reverse = false; // TODO detect reverse condition (Garrat)
    }

    public float angle(double distance) {
        return wheels.angle(distance) + angleOffset;
    }

    public void render(double distance, float throttle, ComponentRenderer draw) {
        float wheelAngle = angle(distance);
        if (reverse) {
            wheelAngle -= 90;
        }

        // Center of the connecting rod, may not line up with a wheel directly
        Vec3d connRodPos = connectingRod.center;
        // Wheel Center is the center of all wheels, may not line up with a wheel directly
        // The difference between these centers is the radius of the connecting rod movement
        double connRodRadius = connRodPos.x - wheels.center().x;
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

        // X: rear driving rod X - driving rod height/2 (hack assuming diameter == height)
        // Y: Center of the rod
        // Z: does not really matter due to rotation axis
        Vec3d drivingRodRotPoint = new Vec3d(drivingRod.max.x - drivingRod.height()/2, drivingRod.center.y, drivingRod.max.z);
        // Angle for movement height vs driving rod length (adjusted for assumed diameter == height, both sides == 2r)
        float drivingRodAngle = (float) Math.toDegrees(Math.atan2(connRodMovment.z, drivingRod.length() - drivingRod.height()));

        // Draw driving rod
        try (ComponentRenderer matrix = draw.push()) {
            // Move to conn rod center
            GL11.glTranslated(-connRodRadius, 0, 0);
            // Apply conn rod movement
            GL11.glTranslated(connRodMovment.x, connRodMovment.z, 0);

            // Move to rot point center
            GL11.glTranslated(drivingRodRotPoint.x, drivingRodRotPoint.y, drivingRodRotPoint.z);
            // Rotate rod angle
            GL11.glRotated(drivingRodAngle, 0, 0, 1);
            // Move back from rot point center
            GL11.glTranslated(-drivingRodRotPoint.x, -drivingRodRotPoint.y, -drivingRodRotPoint.z);

            matrix.render(drivingRod);
        }

        // Piston movement is rod movement offset by the rotation radius
        // Not 100% accurate, missing the offset due to angled driving rod
        double pistonDelta = connRodMovment.x - connRodRadius;

        // Draw piston rod and cross head
        try (ComponentRenderer matrix = draw.push()) {
            GL11.glTranslated(pistonDelta, 0, 0);
            matrix.render(pistonRod);
        }

    }

}
