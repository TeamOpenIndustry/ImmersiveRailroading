package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;

import java.util.List;
import java.util.stream.Collectors;

public class Frame {
    private final ModelComponent frame;
    private final List<Wheel> wheels;

    public Frame(ComponentProvider provider) {
        this.wheels = provider.parseAll(RenderComponentType.FRAME_WHEEL_X)
                .stream().map(Wheel::new).collect(Collectors.toList());

        this.frame = provider.parse(RenderComponentType.FRAME);
        if (frame == null) {
            throw new RuntimeException("Invalid model: Missing FRAME!");
        }
    }

    public void render(double distance, ComponentRenderer draw) {
        draw.render(frame);
        for (Wheel wheel : wheels) {
            wheel.render(wheel.angle(distance), draw);
        }
    }
}
