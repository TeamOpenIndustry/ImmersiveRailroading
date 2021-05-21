package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.resource.Identifier;

public class Horn extends PartSound {
    private final ModelComponent component;

    public static Horn get(ComponentProvider provider, Identifier soundFile, boolean repeats) {
        return new Horn(provider.parse(ModelComponentType.HORN), soundFile, repeats);
    }

    public Horn(ModelComponent component, Identifier soundFile, boolean repeats) {
        super(stock -> ImmersiveRailroading.newSound(soundFile, repeats, 100, stock.soundGauge()));
        this.component = component;
    }

    public void render(ComponentRenderer draw) {
        draw.render(component);
    }
}
