package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;

import java.util.List;

public class Cargo {
    private final List<ModelComponent> cargoLoads;

    public static Cargo get(ComponentProvider provider) {
        List<ModelComponent> cargoLoads = provider.parseAll(RenderComponentType.CARGO_FILL_X);
        return cargoLoads.isEmpty() ? null : new Cargo(cargoLoads);
    }

    public Cargo(List<ModelComponent> cargoLoads) {
        this.cargoLoads = cargoLoads;
    }

    public void render(int percentFull, boolean showCurrentLoadOnly, ComponentRenderer draw) {
        for (ModelComponent cargoLoad : cargoLoads) {
            if (cargoLoad.id <= percentFull) {
                draw.render(cargoLoad);
                //if the stock should only render the current cargo load it'll stop at the highest matching load
                if (showCurrentLoadOnly) {
                    break;
                }
            }
        }
    }
}
