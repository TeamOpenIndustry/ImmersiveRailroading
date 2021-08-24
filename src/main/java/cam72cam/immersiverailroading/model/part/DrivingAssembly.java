package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ValveGearType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;

public class DrivingAssembly {
    private final WheelSet wheels;
    private final ValveGear right;
    private final ValveGear center;
    private final ValveGear left;
    private final ModelComponent steamChest;

    public static DrivingAssembly get(ValveGearType type, ComponentProvider provider, float angleOffset) {
        return get(type, provider, null, angleOffset);
    }

    public static DrivingAssembly get(ValveGearType type, ComponentProvider provider, String pos, float angleOffset) {
        WheelSet wheels = WheelSet.get(provider, pos == null ? ModelComponentType.WHEEL_DRIVER_X : ModelComponentType.WHEEL_DRIVER_POS_X, pos, angleOffset);
        if (wheels == null) {
            return null;
        }

        ValveGear left = ValveGear.get(wheels, type, provider, "LEFT" + (pos == null ? "" : ("_" + pos)), 0);
        ValveGear center = ValveGear.get(wheels, type, provider, "CENTER" + (pos == null ? "" : ("_" + pos)), -120);
        ValveGear right = ValveGear.get(wheels, type, provider, "RIGHT" + (pos == null ? "" : ("_" + pos)), center == null ? -90 : -240);

        ModelComponent steamChest = pos == null ?
                provider.parse(ModelComponentType.STEAM_CHEST) :
                provider.parse(ModelComponentType.STEAM_CHEST_POS, pos);

        return new DrivingAssembly(wheels, right, center, left, steamChest);
    }
    public DrivingAssembly(WheelSet wheels, ValveGear right, ValveGear center, ValveGear left, ModelComponent steamChest) {
        this.wheels = wheels;
        this.right = right;
        this.center = center;
        this.left = left;
        this.steamChest = steamChest;
    }

    public boolean isEndStroke(EntityMoveableRollingStock stock, float throttle) {
        boolean isEndStroke = false;
        if (right != null) {
            isEndStroke |= right.isEndStroke(stock, throttle);
        }
        if (left != null) {
            isEndStroke |= left.isEndStroke(stock, throttle);
        }
        if (center != null) {
            isEndStroke |= center.isEndStroke(stock, throttle);
        }
        return isEndStroke;
    }

    public void effects(EntityMoveableRollingStock stock, float throttle) {
        if (right != null) {
            right.effects(stock, throttle);
        }
        if (left != null) {
            left.effects(stock, throttle);
        }
        if (center != null) {
            center.effects(stock, throttle);
        }
    }

    public void render(double distance, float throttle, ComponentRenderer draw) {
        wheels.render(distance, draw);
        if (right != null) {
            right.render(distance, throttle, draw);
        }
        if (center != null) {
            center.render(distance, throttle, draw);
        }
        if (left != null) {
            left.render(distance, throttle, draw);
        }
        draw.render(steamChest);
    }

}
