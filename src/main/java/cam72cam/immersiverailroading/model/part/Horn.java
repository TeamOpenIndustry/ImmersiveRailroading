package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.resource.Identifier;

public class Horn extends PartSound {

    public static Horn get(ComponentProvider provider, ModelState state, Identifier soundFile, boolean repeats) {
        return new Horn(provider.parse(ModelComponentType.HORN), state, soundFile, repeats);
    }

    public Horn(ModelComponent component, ModelState state, Identifier soundFile, boolean repeats) {
        super(stock -> stock.createSound(soundFile, repeats, 100));
        state.include(component);
    }
}
