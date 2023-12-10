package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import util.Matrix4;

import java.util.*;

public class WalschaertsValveGear extends StephensonValveGear {
    protected final ModelComponent crossHead;
    protected final ModelComponent combinationLever;
    protected final ModelComponent returnCrank;
    protected final ModelComponent returnCrankRod;
    protected final ModelComponent slottedLink;
    protected final ModelComponent radiusBar;
    protected final List<ModelComponent> todo;
    private final Vec3d crankWheel;

    public static WalschaertsValveGear get(WheelSet wheels, ComponentProvider provider, ModelState state, ModelPosition pos, float angleOffset) {
        ModelComponent drivingRod = provider.parse(ModelComponentType.MAIN_ROD_SIDE, pos);
        ModelComponent connectingRod = provider.parse(ModelComponentType.SIDE_ROD_SIDE, pos);
        ModelComponent pistonRod = provider.parse(ModelComponentType.PISTON_ROD_SIDE, pos);
        ModelComponent cylinder = provider.parse(ModelComponentType.CYLINDER_SIDE, pos);
        ModelComponent crossHead = provider.parse(ModelComponentType.UNION_LINK_SIDE, pos);
        ModelComponent combinationLever = provider.parse(ModelComponentType.COMBINATION_LEVER_SIDE, pos);
        ModelComponent returnCrank = provider.parse(ModelComponentType.ECCENTRIC_CRANK_SIDE, pos);
        ModelComponent returnCrankRod = provider.parse(ModelComponentType.ECCENTRIC_ROD_SIDE, pos);
        ModelComponent slottedLink = provider.parse(ModelComponentType.EXPANSION_LINK_SIDE, pos);
        ModelComponent radiusBar = provider.parse(ModelComponentType.RADIUS_BAR_SIDE, pos);
        ModelComponent frontExhaust = provider.parse(ModelComponentType.CYLINDER_DRAIN_SIDE, pos.and(ModelPosition.A));
        ModelComponent rearExhaust = provider.parse(ModelComponentType.CYLINDER_DRAIN_SIDE, pos.and(ModelPosition.B));

        List<ModelComponent> todo = provider.parse(pos,
                ModelComponentType.VALVE_STEM_SIDE,
                ModelComponentType.REVERSING_ARM_SIDE,
                ModelComponentType.LIFTING_LINK_SIDE,
                ModelComponentType.REACH_ROD_SIDE
        );

        return drivingRod != null && connectingRod != null && pistonRod != null &&
                crossHead != null && combinationLever != null && returnCrank != null && returnCrankRod != null && slottedLink != null && radiusBar != null ?
                new WalschaertsValveGear(wheels,
                        state,
                        drivingRod,
                        connectingRod,
                        pistonRod,
                        cylinder,
                        angleOffset,
                        crossHead,
                        combinationLever,
                        returnCrank,
                        returnCrankRod,
                        slottedLink,
                        radiusBar,
                        todo,
                        frontExhaust,
                        rearExhaust) : null;
    }

    public WalschaertsValveGear(WheelSet wheels, ModelState state,
                                ModelComponent drivingRod,
                                ModelComponent connectingRod,
                                ModelComponent pistonRod,
                                ModelComponent cylinder,
                                float angleOffset,
                                ModelComponent crossHead,
                                ModelComponent combinationLever,
                                ModelComponent returnCrank,
                                ModelComponent returnCrankRod,
                                ModelComponent slottedLink,
                                ModelComponent radiusBar,
                                List<ModelComponent> todo,
                                ModelComponent frontExhaust, ModelComponent rearExhaust) {
        super(wheels, state, drivingRod, connectingRod, pistonRod, cylinder, angleOffset, frontExhaust, rearExhaust);
        this.crossHead = crossHead;
        this.combinationLever = combinationLever;
        this.returnCrank = returnCrank;
        this.returnCrankRod = returnCrankRod;
        this.slottedLink = slottedLink;
        this.radiusBar = radiusBar;
        this.todo = todo;

        crankWheel = wheels.wheels.stream().map(w -> w.wheel.center).min(Comparator.comparingDouble(w -> w.distanceTo(reverse ? returnCrank.min : returnCrank.max))).get();

        state.include(todo);

        // This is pretty terrible
        state = state.push(builder -> builder.add((ModelState.GroupAnimator) (stock, group, partialTicks) -> {

            float wheelAngle = super.angle(stock.distanceTraveled);
            float reverser = stock instanceof Locomotive ? ((Locomotive) stock).getReverser() : 0;

            // Center of the connecting rod, may not line up with a wheel directly
            Vec3d connRodPos = super.connectingRod.center;
            // Wheel Center is the center of all wheels, may not line up with a wheel directly
            // The difference between these centers is the radius of the connecting rod movement
            double connRodRadius = connRodPos.x - centerOfWheels.x;
            // Find new connecting rod pos based on the connecting rod rod radius
            Vec3d connRodMovment = VecUtil.fromWrongYaw(connRodRadius, (float) wheelAngle);


            // Piston movement is rod movement offset by the rotation radius
            // Not 100% accurate, missing the offset due to angled driving rod
            double pistonDelta = connRodMovment.x - connRodRadius;

            // Draw piston rod and cross head
            if (crossHead.modelIDs.contains(group)) {
                return new Matrix4().translate(pistonDelta, 0, 0);
            }

            Vec3d returnCrankRotPoint = reverse ?
                    returnCrank.min.add(returnCrank.height() / 2, returnCrank.height() / 2, 0) :
                    returnCrank.max.add(-returnCrank.height() / 2, -returnCrank.height() / 2, 0);
            Vec3d wheelRotationOffset = reverse ?
                    VecUtil.fromWrongYaw(returnCrankRotPoint.x - crankWheel.x, (float) wheelAngle) :
                    VecUtil.fromWrongYaw(returnCrankRotPoint.x - crankWheel.x, (float) wheelAngle);

            Vec3d returnCrankOriginOffset = crankWheel.add(wheelRotationOffset.x, wheelRotationOffset.z, 0);
            double returnCrankAngle = wheelAngle + 90 + 30;
            if (returnCrank.modelIDs.contains(group)) {
                Matrix4 matrix = new Matrix4();
                // Move to crank offset from origin
                matrix.translate(returnCrankOriginOffset.x, returnCrankOriginOffset.y, 0);
                // Rotate crank
                matrix.rotate(Math.toRadians(returnCrankAngle), 0, 0, 1);
                // Draw return crank at current position
                matrix.translate(-returnCrankRotPoint.x, -returnCrankRotPoint.y, 0);
                return matrix;
            }

            // We take the length of the crank and subtract the radius on either side.
            // We use rod radius and crank radius since it can be a funny shape
            double returnCrankLength = -(returnCrank.length() - returnCrank.height() / 2 - returnCrankRod.height() / 2);
            // Rotation offset around the return crank point
            Vec3d returnCrankRotationOffset = VecUtil.fromWrongYaw(returnCrankLength, (float) returnCrankAngle + (reverse ? 90 : -90));
            // Combine wheel->crankpoint offset and the crankpoint->crankrod offset
            Vec3d returnCrankRodOriginOffset = returnCrankOriginOffset.add(returnCrankRotationOffset.x, returnCrankRotationOffset.z, 0);
            // Point about which the return crank rotates
            Vec3d returnCrankRodRotPoint = reverse ?
                    returnCrankRod.min.add(returnCrankRod.height() / 2, returnCrankRod.height() / 2, 0) :
                    returnCrankRod.max.add(-returnCrankRod.height() / 2, -returnCrankRod.height() / 2, 0);
            // Length between return crank rod centers
            double returnCrankRodLength = returnCrankRod.length() - returnCrankRod.height() / 2;
            // Height that the return crank rod should shoot for
            double slottedLinkLowest = slottedLink.min.y + slottedLink.width() / 2;
            // Fudge
            double returnCrankRodFudge = reverse ?
                    Math.abs(slottedLink.center.x - (returnCrankRodOriginOffset.x + returnCrankRodLength)) / 3 :
                    Math.abs(slottedLink.center.x - (returnCrankRodOriginOffset.x - returnCrankRodLength)) / 3;
            float returnCrankRodRot = reverse ?
                    -VecUtil.toWrongYaw(new Vec3d(slottedLinkLowest - returnCrankRodOriginOffset.y + returnCrankRodFudge, 0, returnCrankRodLength)) :
                    VecUtil.toWrongYaw(new Vec3d(slottedLinkLowest - returnCrankRodOriginOffset.y + returnCrankRodFudge, 0, returnCrankRodLength));
            // Angle the return crank rod should be at to hit the slotted link
            if (returnCrankRod.modelIDs.contains(group)) {
                Matrix4 matrix = new Matrix4();
                // Move to crank rod offset from origin
                matrix.translate(returnCrankRodOriginOffset.x, returnCrankRodOriginOffset.y, 0);

                matrix.rotate(Math.toRadians(returnCrankRodRot), 0, 0, 1);

                // Draw return crank rod at current position
                matrix.translate(-returnCrankRodRotPoint.x, -returnCrankRodRotPoint.y, 0);
                return matrix;
            }

            Vec3d returnCrankRodRotationOffset = VecUtil.fromWrongYaw(returnCrankRodLength, returnCrankRodRot + (reverse ? -90 : 90));
            Vec3d returnCrankRodFarPoint = returnCrankRodOriginOffset.add(returnCrankRodRotationOffset.x, returnCrankRodRotationOffset.z, 0);
            // Slotted link rotation point
            Vec3d slottedLinkRotPoint = slottedLink.center;
            double slottedLinkRot = Math.toDegrees(Math.atan2(-slottedLinkRotPoint.x + returnCrankRodFarPoint.x, slottedLinkRotPoint.y - returnCrankRodFarPoint.y));
            if (slottedLink.modelIDs.contains(group)) {
                Matrix4 matrix = new Matrix4();
                // Move to origin
                matrix.translate(slottedLinkRotPoint.x, slottedLinkRotPoint.y, 0);

                // Rotate around center point
                matrix.rotate(Math.toRadians(slottedLinkRot), 0, 0, 1);

                // Draw slotted link at current position
                matrix.translate(-slottedLinkRotPoint.x, -slottedLinkRotPoint.y, 0);
                return matrix;
            }

            double forwardMax = (slottedLink.min.y - slottedLinkRotPoint.y) * 0.4;
            double forwardMin = (slottedLink.max.y - slottedLinkRotPoint.y) * 0.65;
            double throttleSlotPos = 0;
            if (reverser > 0) {
                throttleSlotPos = forwardMax * reverser;
            } else {
                throttleSlotPos = forwardMin * -reverser;
            }

            double radiusBarSliding = Math.sin(Math.toRadians(-slottedLinkRot)) * (throttleSlotPos);

            Vec3d radiusBarClose = reverse ? radiusBar.min : radiusBar.max;
            throttleSlotPos += slottedLinkRotPoint.y - radiusBar.max.y;

            float raidiusBarAngle = reverse ?
                    -(VecUtil.toWrongYaw(new Vec3d(radiusBar.length(), 0, throttleSlotPos)) + 90) :
                    VecUtil.toWrongYaw(new Vec3d(radiusBar.length(), 0, throttleSlotPos)) + 90;

            if (radiusBar.modelIDs.contains(group)) {
                Matrix4 matrix = new Matrix4();
                matrix.translate(0, throttleSlotPos, 0);

                matrix.translate(radiusBarSliding, 0, 0);

                matrix.translate(radiusBarClose.x, radiusBarClose.y, 0);
                matrix.rotate(Math.toRadians(raidiusBarAngle), 0, 0, 1);
                matrix.translate(-radiusBarClose.x, -radiusBarClose.y, 0);
                return matrix;
            }

            Vec3d radiusBarFar = reverse ? radiusBar.max : radiusBar.min;
            //radiusBarSliding != correct TODO angle offset
            Vec3d radiusBarFarPoint = radiusBarFar.add(radiusBarSliding + combinationLever.width() / 2, 0, 0);

            Vec3d combinationLeverRotPos = combinationLever.min.add(combinationLever.width() / 2, combinationLever.width() / 2, 0);

            Vec3d delta = radiusBarFarPoint.subtract(combinationLeverRotPos.add(pistonDelta, 0, 0));

            float combinationLeverAngle = VecUtil.toWrongYaw(new Vec3d(delta.x, 0, delta.y));

            if (combinationLever.modelIDs.contains(group)) {
                Matrix4 matrix = new Matrix4();
                matrix.translate(pistonDelta, 0, 0);
                matrix.translate(combinationLeverRotPos.x, combinationLeverRotPos.y, 0);
                matrix.rotate(Math.toRadians(combinationLeverAngle), 0, 0, 1);
                matrix.translate(-combinationLeverRotPos.x, -combinationLeverRotPos.y, 0);
                return matrix;
            }
            return null;
        }));

        state.include(crossHead);
        state.include(combinationLever);
        state.include(returnCrank);
        state.include(returnCrankRod);
        state.include(slottedLink);
        state.include(radiusBar);
    }
}
