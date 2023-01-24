package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.resource.Identifier;

public class Bell extends PartSound {
    private final ModelComponent component;

    public static Bell get(ComponentProvider provider, ModelState state, Identifier soundFile) {
        ModelComponent component = provider.parse(ModelComponentType.BELL);
        state.include(component);
        return new Bell(component, soundFile);
    }

    public Bell(ModelComponent component, Identifier soundFile) {
        super(soundFile == null ? null :
                stock -> stock.createSound(soundFile, true, 150)
        );
        this.component = component;
    }
}
