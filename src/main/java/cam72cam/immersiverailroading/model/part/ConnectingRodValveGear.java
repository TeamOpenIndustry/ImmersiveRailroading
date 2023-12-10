package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import util.Matrix4;

import java.util.stream.Collectors;

public class ConnectingRodValveGear extends ValveGear {
    protected Vec3d centerOfWheels;
    protected final ModelComponent connectingRod;

    public static ConnectingRodValveGear get(WheelSet wheels, ComponentProvider provider, ModelState state, ModelPosition pos, float angleOffset) {
        ModelComponent connectingRod = provider.parse(ModelComponentType.SIDE_ROD_SIDE, pos);
        return connectingRod != null ? new ConnectingRodValveGear(wheels, state, connectingRod, angleOffset) : null;
    }

    public ConnectingRodValveGear(WheelSet wheels, ModelState state, ModelComponent connectingRod, float angleOffset) {
        super(wheels, state, angleOffset);

        this.connectingRod = connectingRod;
        this.centerOfWheels = ModelComponent.center(wheels.wheels.stream().map(x -> x.wheel).collect(Collectors.toList()));

        state.push(settings -> settings.add((ModelState.Animator) (stock, partialTicks) -> {
            Vec3d connRodMovment = connRodMovement(stock);
            return new Matrix4().translate(-connRodRadius(), 0, 0).translate(connRodMovment.x, connRodMovment.z, 0);
        })).include(connectingRod);
    }

    /** Find new connecting rod pos based on the connecting rod radius */
    public Vec3d connRodMovement(EntityMoveableRollingStock stock) {
        return VecUtil.fromWrongYaw(connRodRadius(), angle(stock.distanceTraveled));
    }

    public double connRodRadius() {
        // Center of the connecting rod, may not line up with a wheel directly
        Vec3d connRodPos = connectingRod.center;
        // Wheel Center is the center of all wheels, may not line up with a wheel directly
        // The difference between these centers is the radius of the connecting rod movement

        return connRodPos.x - centerOfWheels.x;
    }
}
