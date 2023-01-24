package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;

import java.util.List;
import java.util.stream.Collectors;

public class WheelSet {
    protected final List<Wheel> wheels;
    private final float angleOffset;

    public static WheelSet get(ComponentProvider provider, ModelState state, ModelComponentType type, float angleOffset) {
        return get(provider, state, type, null, angleOffset);
    }

    public static WheelSet get(ComponentProvider provider, ModelState state, ModelComponentType type, ModelPosition pos, float angleOffset) {
        List<ModelComponent> wheels = (pos == null ?
                provider.parseAll(type) :
                provider.parseAll(type, pos)
        );

        return wheels.isEmpty() ? null : new WheelSet(state, wheels, angleOffset);
    }

    public WheelSet(ModelState state, List<ModelComponent> wheels, float angleOffset) {
        this.wheels = wheels.stream().map(wheel ->
                new Wheel(wheel, state, stock -> angle(stock.distanceTraveled))).collect(Collectors.toList());
        this.angleOffset = angleOffset;
    }

    public float angle(double distance) {
        return wheels.get(0).angle(distance) + angleOffset;
    }
}
