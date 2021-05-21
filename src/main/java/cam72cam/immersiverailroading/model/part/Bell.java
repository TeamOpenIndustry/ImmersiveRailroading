package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.resource.Identifier;

public class Bell extends PartSound {
    private final ModelComponent component;

    public static Bell get(ComponentProvider provider, Identifier soundFile) {
        return new Bell(provider.parse(ModelComponentType.BELL), soundFile);
    }

    public Bell(ModelComponent component, Identifier soundFile) {
        super(soundFile == null ? null :
                stock -> ImmersiveRailroading.newSound(soundFile, true, 150, stock.soundGauge())
        );
        this.component = component;
    }


    public void render(ComponentRenderer draw) {
        draw.render(component);
    }
}
