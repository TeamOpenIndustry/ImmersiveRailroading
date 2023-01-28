package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;

public class Bell extends PartSound {
    private final ModelComponent component;

    public static Bell get(ComponentProvider provider, ModelState state, EntityRollingStockDefinition.SoundDefinition soundFile) {
        ModelComponent component = provider.parse(ModelComponentType.BELL);
        state.include(component);
        return new Bell(component, soundFile);
    }

    public Bell(ModelComponent component, EntityRollingStockDefinition.SoundDefinition soundFile) {
        super(soundFile, true, 150);
        this.component = component;
    }
}
