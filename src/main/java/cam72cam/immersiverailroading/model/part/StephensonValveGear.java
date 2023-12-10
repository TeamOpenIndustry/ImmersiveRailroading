package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.math.Vec3d;
import util.Matrix4;

import java.util.Comparator;
import java.util.stream.Collectors;

public class StephensonValveGear extends ConnectingRodValveGear {
    protected final ModelComponent drivingRod;
    protected final ModelComponent pistonRod;
    protected final ModelComponent cylinder;
    protected final boolean reverse;

    protected final Vec3d drivenWheel;

    public static StephensonValveGear get(WheelSet wheels, ComponentProvider provider, ModelState state, ModelPosition pos, float angleOffset) {
        ModelComponent drivingRod = provider.parse(ModelComponentType.MAIN_ROD_SIDE, pos);
        ModelComponent connectingRod = provider.parse(ModelComponentType.SIDE_ROD_SIDE, pos);
        ModelComponent pistonRod = provider.parse(ModelComponentType.PISTON_ROD_SIDE, pos);
        ModelComponent cylinder = provider.parse(ModelComponentType.CYLINDER_SIDE, pos);
        ModelComponent frontExhaust = provider.parse(ModelComponentType.CYLINDER_DRAIN_SIDE, pos.and(ModelPosition.A));
        ModelComponent rearExhaust = provider.parse(ModelComponentType.CYLINDER_DRAIN_SIDE, pos.and(ModelPosition.B));

        return drivingRod != null && connectingRod != null && pistonRod != null ?
                new StephensonValveGear(wheels, state, drivingRod, connectingRod, pistonRod, cylinder, angleOffset, frontExhaust, rearExhaust) : null;
    }
    public StephensonValveGear(WheelSet wheels, ModelState state, ModelComponent drivingRod, ModelComponent connectingRod, ModelComponent pistonRod, ModelComponent cylinder, float angleOffset, ModelComponent frontExhaust, ModelComponent rearExhaust) {
        super(wheels, state, connectingRod, angleOffset);
        this.drivingRod = drivingRod;
        this.pistonRod = pistonRod;
        this.cylinder = cylinder;
        Vec3d center = ModelComponent.center(wheels.wheels.stream().map(x -> x.wheel).collect(Collectors.toList()));
        this.reverse = pistonRod.center.x > center.x;
        // TODO this sucks, do better
        this.angleOffset = angleOffset + (reverse ? -90 : 0);


        //noinspection OptionalGetWithoutIsPresent
        drivenWheel = wheels.wheels.stream().map(w -> w.wheel.center).min(Comparator.comparingDouble(w -> w.distanceTo(reverse ? drivingRod.min : drivingRod.max))).get();
        centerOfWheels = drivingRod.pos.equals(ModelPosition.CENTER) ? drivenWheel : center; // Bad hack for old TRI_WALSCHERTS code

        state.include(cylinder);

        state.push(builder -> builder.add((ModelState.Animator) (stock, partialTicks) -> {
            Matrix4 matrix = new Matrix4();

            Vec3d connRodMovment = connRodMovement(stock);

            // X: rear driving rod X - driving rod height/2 (hack assuming diameter == height)
            // Y: Center of the rod
            // Z: does not really matter due to rotation axis
            Vec3d drivingRodRotPoint = new Vec3d((reverse ? drivingRod.min.x + drivingRod.height()/2 : drivingRod.max.x - drivingRod.height()/2), drivingRod.center.y, reverse ? drivingRod.min.z : drivingRod.max.z);
            // Angle for movement height vs driving rod length (adjusted for assumed diameter == height, both sides == 2r)
            float drivingRodAngle = (float) Math.toDegrees(Math.atan2((reverse ? -connRodMovment.z : connRodMovment.z), drivingRod.length() - drivingRod.height()));

            double connRodRadius = connRodRadius();

            // Move to conn rod center
            matrix.translate(-connRodRadius, 0, 0);
            // Apply conn rod movement
            matrix.translate(connRodMovment.x, connRodMovment.z, 0);

            // Move to rot point center
            matrix.translate(drivingRodRotPoint.x, drivingRodRotPoint.y, drivingRodRotPoint.z);
            // Rotate rod angle
            matrix.rotate(Math.toRadians(drivingRodAngle), 0, 0, 1);
            // Move back from rot point center
            matrix.translate(-drivingRodRotPoint.x, -drivingRodRotPoint.y, -drivingRodRotPoint.z);

            return matrix;
        })).include(drivingRod);

        state.push(builder -> builder.add((ModelState.Animator) (stock, partialTicks) -> {
            Matrix4 matrix = new Matrix4();

            Vec3d connRodMovment = connRodMovement(stock);

            // Piston movement is rod movement offset by the rotation radius
            // Not 100% accurate, missing the offset due to angled driving rod
            double pistonDelta = connRodMovment.x - connRodRadius();
            matrix.translate(pistonDelta, 0, 0);
            return matrix;
        })).include(pistonRod);


        this.frontExhaust = frontExhaust != null ?
                new Exhaust(frontExhaust, 90) :
                new Exhaust(pistonRod.min, pistonRod.pos, 90);
        this.rearExhaust = rearExhaust != null ?
                new Exhaust(rearExhaust, 270) :
                new Exhaust(pistonRod.min, pistonRod.pos, 270);
    }
}
