package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;

import java.util.Comparator;
import java.util.List;

public class CargoFill {
    private final List<ModelComponent> cargoLoads;

    public static CargoFill get(ComponentProvider provider, ModelPosition pos) {
        List<ModelComponent> cargoLoads = pos == null ? provider.parseAll(ModelComponentType.CARGO_FILL_X) : provider.parseAll(ModelComponentType.CARGO_FILL_POS_X, pos);
        // Make sure that cargo loads are in order for showCurrentLoadOnly (if enabled)
        cargoLoads.sort(Comparator.comparingInt(a -> -a.id));
        return cargoLoads.isEmpty() ? null : new CargoFill(cargoLoads);
    }

    public CargoFill(List<ModelComponent> cargoLoads) {
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
