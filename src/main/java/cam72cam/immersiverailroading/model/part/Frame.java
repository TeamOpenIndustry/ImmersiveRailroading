package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;

import java.util.List;
import java.util.stream.Collectors;

public class Frame {
    private final ModelComponent frame;
    private final List<Wheel> wheels;

    public Frame(ComponentProvider provider, String blame) {
        this.wheels = provider.parseAll(ModelComponentType.FRAME_WHEEL_X)
                .stream().map(Wheel::new).collect(Collectors.toList());

        this.frame = provider.parse(ModelComponentType.FRAME);
        if (frame == null) {
            ImmersiveRailroading.warn("Invalid model: Missing FRAME for %s!  (this will fail in future versions of IR)", blame);
        }
    }

    public void render(double distance, ComponentRenderer draw) {
        draw.render(frame);
        for (Wheel wheel : wheels) {
            wheel.render(wheel.angle(distance), draw);
        }
    }
}
