package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import org.lwjgl.opengl.GL11;

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

    public static WalschaertsValveGear get(List<Wheel> wheels, ComponentProvider provider, String pos, float angleOffset) {
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

        List<ModelComponent> todo = provider.parse(pos,
                ModelComponentType.VALVE_STEM_SIDE,
                ModelComponentType.REVERSING_ARM_SIDE,
                ModelComponentType.LIFTING_LINK_SIDE,
                ModelComponentType.REACH_ROD_SIDE
        );

        return drivingRod != null && connectingRod != null && pistonRod != null &&
                crossHead != null && combinationLever != null && returnCrank != null && returnCrankRod != null && slottedLink != null && radiusBar != null ?
                new WalschaertsValveGear(wheels, drivingRod, connectingRod, pistonRod, cylinder, angleOffset, crossHead, combinationLever, returnCrank, returnCrankRod, slottedLink, radiusBar, todo) : null;
    }

    public WalschaertsValveGear(List<Wheel> wheels,
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
                                List<ModelComponent> todo) {
        super(wheels, drivingRod, connectingRod, pistonRod, cylinder, angleOffset);
        this.crossHead = crossHead;
        this.combinationLever = combinationLever;
        this.returnCrank = returnCrank;
        this.returnCrankRod = returnCrankRod;
        this.slottedLink = slottedLink;
        this.radiusBar = radiusBar;
        this.todo = todo;

        crankWheel = wheels.stream().map(w -> w.wheel.center).min(Comparator.comparingDouble(w -> w.distanceTo(reverse ? returnCrank.min : returnCrank.max))).get();
    }

    public void render(double distance, float throttle, ComponentRenderer draw) {
        super.render(distance, throttle, draw);

        float wheelAngle = super.angle(distance);

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
        try (ComponentRenderer matrix = draw.push()) {
            GL11.glTranslated(pistonDelta, 0, 0);
            matrix.render(crossHead);
        }

        Vec3d returnCrankRotPoint = reverse ?
                returnCrank.min.add(returnCrank.height()/2, returnCrank.height()/2, 0) :
                returnCrank.max.add(-returnCrank.height()/2, -returnCrank.height()/2, 0);
        Vec3d wheelRotationOffset = reverse ?
                VecUtil.fromWrongYaw(returnCrankRotPoint.x - crankWheel.x, (float) wheelAngle) :
                VecUtil.fromWrongYaw(returnCrankRotPoint.x - crankWheel.x, (float) wheelAngle);

        Vec3d returnCrankOriginOffset = crankWheel.add(wheelRotationOffset.x, wheelRotationOffset.z, 0);
        double returnCrankAngle = wheelAngle + 90 + 30;
        try (ComponentRenderer matrix = draw.push()) {
            // Move to crank offset from origin
            GL11.glTranslated(returnCrankOriginOffset.x, returnCrankOriginOffset.y, 0);
            // Rotate crank
            GL11.glRotated(returnCrankAngle, 0, 0, 1);
            // Draw return crank at current position
            GL11.glTranslated(-returnCrankRotPoint.x, -returnCrankRotPoint.y, 0);
            matrix.render(returnCrank);
        }

        // We take the length of the crank and subtract the radius on either side.
        // We use rod radius and crank radius since it can be a funny shape
        double returnCrankLength = -(returnCrank.length() - returnCrank.height()/2 - returnCrankRod.height()/2);
        // Rotation offset around the return crank point
        Vec3d returnCrankRotationOffset = VecUtil.fromWrongYaw(returnCrankLength, (float) returnCrankAngle + (reverse ? 90 : -90));
        // Combine wheel->crankpoint offset and the crankpoint->crankrod offset
        Vec3d returnCrankRodOriginOffset = returnCrankOriginOffset.add(returnCrankRotationOffset.x, returnCrankRotationOffset.z, 0);
        // Point about which the return crank rotates
        Vec3d returnCrankRodRotPoint = reverse ?
                returnCrankRod.min.add(returnCrankRod.height()/2, returnCrankRod.height()/2, 0) :
                returnCrankRod.max.add(-returnCrankRod.height()/2, -returnCrankRod.height()/2, 0);
        // Length between return crank rod centers
        double returnCrankRodLength = returnCrankRod.length() - returnCrankRod.height()/2;
        // Height that the return crank rod should shoot for
        double slottedLinkLowest = slottedLink.min.y + slottedLink.width()/2;
        // Fudge
        double returnCrankRodFudge = reverse ?
                Math.abs(slottedLink.center.x - (returnCrankRodOriginOffset.x + returnCrankRodLength))/3 :
                Math.abs(slottedLink.center.x - (returnCrankRodOriginOffset.x - returnCrankRodLength))/3;
        float returnCrankRodRot = reverse ?
                -VecUtil.toWrongYaw(new Vec3d(slottedLinkLowest - returnCrankRodOriginOffset.y + returnCrankRodFudge, 0, returnCrankRodLength)) :
                VecUtil.toWrongYaw(new Vec3d(slottedLinkLowest - returnCrankRodOriginOffset.y + returnCrankRodFudge, 0, returnCrankRodLength));
        // Angle the return crank rod should be at to hit the slotted link
        try (ComponentRenderer matrix = draw.push()) {
            // Move to crank rod offset from origin
            GL11.glTranslated(returnCrankRodOriginOffset.x, returnCrankRodOriginOffset.y, 0);

            GL11.glRotated(returnCrankRodRot, 0, 0, 1);

            // Draw return crank rod at current position
            GL11.glTranslated(-returnCrankRodRotPoint.x, -returnCrankRodRotPoint.y, 0);
            matrix.render(returnCrankRod);
        }

        Vec3d returnCrankRodRotationOffset = VecUtil.fromWrongYaw(returnCrankRodLength, returnCrankRodRot+(reverse ? -90 : 90));
        Vec3d returnCrankRodFarPoint = returnCrankRodOriginOffset.add(returnCrankRodRotationOffset.x, returnCrankRodRotationOffset.z, 0);
        // Slotted link rotation point
        Vec3d slottedLinkRotPoint = slottedLink.center;
        double slottedLinkRot = Math.toDegrees(Math.atan2(-slottedLinkRotPoint.x + returnCrankRodFarPoint.x, slottedLinkRotPoint.y - returnCrankRodFarPoint.y));
        try (ComponentRenderer matrix = draw.push()) {
            // Move to origin
            GL11.glTranslated(slottedLinkRotPoint.x, slottedLinkRotPoint.y, 0);

            // Rotate around center point
            GL11.glRotated(slottedLinkRot, 0, 0, 1);

            // Draw slotted link at current position
            GL11.glTranslated(-slottedLinkRotPoint.x, -slottedLinkRotPoint.y, 0);
            matrix.render(slottedLink);
        }

        double forwardMax = (slottedLink.min.y - slottedLinkRotPoint.y) * 0.4;
        double forwardMin = (slottedLink.max.y - slottedLinkRotPoint.y) * 0.65;
        double throttleSlotPos = 0;
        if (throttle > 0) {
            throttleSlotPos = forwardMax * throttle;
        } else {
            throttleSlotPos = forwardMin * -throttle;
        }

        double radiusBarSliding = Math.sin(Math.toRadians(-slottedLinkRot)) * (throttleSlotPos);

        Vec3d radiusBarClose = reverse ? radiusBar.min : radiusBar.max;
        throttleSlotPos += slottedLinkRotPoint.y - radiusBar.max.y;

        float raidiusBarAngle = reverse ?
                -(VecUtil.toWrongYaw(new Vec3d(radiusBar.length(), 0, throttleSlotPos))+90) :
                VecUtil.toWrongYaw(new Vec3d(radiusBar.length(), 0, throttleSlotPos))+90;

        try (ComponentRenderer matrix = draw.push()) {
            GL11.glTranslated(0, throttleSlotPos, 0);

            GL11.glTranslated(radiusBarSliding, 0, 0);

            GL11.glTranslated(radiusBarClose.x, radiusBarClose.y, 0);
            GL11.glRotated(raidiusBarAngle, 0, 0, 1);
            GL11.glTranslated(-radiusBarClose.x, -radiusBarClose.y, 0);
            matrix.render(radiusBar);
        }

        Vec3d radiusBarFar = reverse ? radiusBar.max : radiusBar.min;
        //radiusBarSliding != correct TODO angle offset
        Vec3d radiusBarFarPoint = radiusBarFar.add(radiusBarSliding + combinationLever.width()/2, 0, 0);

        Vec3d combinationLeverRotPos = combinationLever.min.add(combinationLever.width()/2, combinationLever.width()/2, 0);

        Vec3d delta = radiusBarFarPoint.subtract(combinationLeverRotPos.add(pistonDelta, 0, 0));

        float combinationLeverAngle = VecUtil.toWrongYaw(new Vec3d(delta.x, 0, delta.y));

        try (ComponentRenderer matrix = draw.push()) {
            GL11.glTranslated(pistonDelta, 0, 0);
            GL11.glTranslated(combinationLeverRotPos.x, combinationLeverRotPos.y, 0);
            GL11.glRotated(combinationLeverAngle, 0, 0, 1);
            GL11.glTranslated(-combinationLeverRotPos.x, -combinationLeverRotPos.y, 0);
            matrix.render(combinationLever);
        }

        draw.render(todo);
    }
}
