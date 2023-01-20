package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.library.ValveGearConfig;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;

public class DrivingAssembly {
    private final WheelSet wheels;
    private final ValveGear right;
    private final ValveGear inner_right;
    private final ValveGear center;
    private final ValveGear inner_left;
    private final ValveGear left;
    private final ModelComponent steamChest;

    public static DrivingAssembly get(ValveGearConfig type, ComponentProvider provider, ModelState state, float angleOffset) {
        return get(type, provider, state, null, angleOffset);
    }

    public static DrivingAssembly get(ValveGearConfig type, ComponentProvider provider, ModelState state, ModelPosition pos, float angleOffset) {
        WheelSet wheels = WheelSet.get(provider, state, pos == null ? ModelComponentType.WHEEL_DRIVER_X : ModelComponentType.WHEEL_DRIVER_POS_X, pos, angleOffset);
        if (wheels == null) {
            return null;
        }

        ValveGear left = ValveGear.get(wheels, type, provider, state, ModelPosition.LEFT.and(pos), 0);
        ValveGear inner_left = ValveGear.get(wheels, type, provider, state, ModelPosition.INNER_LEFT.and(pos), 180);
        ValveGear center = ValveGear.get(wheels, type, provider, state, ModelPosition.CENTER.and(pos), -120);
        ValveGear inner_right = ValveGear.get(wheels, type, provider, state, ModelPosition.INNER_RIGHT.and(pos), 90);
        ValveGear right = ValveGear.get(wheels, type, provider, state, ModelPosition.RIGHT.and(pos), center == null ? -90 : -240);

        ModelComponent steamChest = pos == null ?
                provider.parse(ModelComponentType.STEAM_CHEST) :
                provider.parse(ModelComponentType.STEAM_CHEST_POS, pos);

        // TODO this should rock
        state.include(steamChest);

        return new DrivingAssembly(wheels, right, inner_right, center, inner_left, left, steamChest);
    }
    public DrivingAssembly(WheelSet wheels, ValveGear right, ValveGear inner_right, ValveGear center, ValveGear inner_left, ValveGear left, ModelComponent steamChest) {
        this.wheels = wheels;
        this.right = right;
        this.inner_right = inner_right;
        this.center = center;
        this.inner_left = inner_left;
        this.left = left;
        this.steamChest = steamChest;
    }

    public boolean isEndStroke(EntityMoveableRollingStock stock) {
        boolean isEndStroke = right != null && right.isEndStroke(stock);
        isEndStroke |= inner_right != null && inner_right.isEndStroke(stock);
        isEndStroke |= center != null && center.isEndStroke(stock);
        isEndStroke |= inner_left != null && inner_left.isEndStroke(stock);
        isEndStroke |= inner_left != null && left.isEndStroke(stock);
        return isEndStroke;
    }

    public void effects(EntityMoveableRollingStock stock) {
        if (right != null) {
            right.effects(stock);
        }
        if (inner_right != null) {
            inner_right.effects(stock);
        }
        if (center != null) {
            center.effects(stock);
        }
        if (inner_left != null) {
            inner_left.effects(stock);
        }
        if (left != null) {
            left.effects(stock);
        }
    }
}
