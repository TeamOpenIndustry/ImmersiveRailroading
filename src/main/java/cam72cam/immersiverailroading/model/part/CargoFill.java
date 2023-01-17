package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.Freight;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;

import java.util.Comparator;
import java.util.List;

public class CargoFill {
    public static CargoFill get(ComponentProvider provider, ModelState state, boolean showCurrentLoadOnly, ModelPosition pos) {
        List<ModelComponent> cargoLoads = pos == null ? provider.parseAll(ModelComponentType.CARGO_FILL_X) : provider.parseAll(ModelComponentType.CARGO_FILL_POS_X, pos);
        // Make sure that cargo loads are in order for showCurrentLoadOnly (if enabled)
        cargoLoads.sort(Comparator.comparingInt(a -> -a.id));
        return cargoLoads.isEmpty() ? null : new CargoFill(cargoLoads, state, showCurrentLoadOnly);
    }

    public CargoFill(List<ModelComponent> cargoLoads, ModelState state, boolean showCurrentLoadOnly) {
        state.push(settings -> settings.add((ModelState.GroupVisibility) (stock, group) -> {
            int percentFull = stock instanceof Freight ? ((Freight) stock).getPercentCargoFull() : 100;
            for (ModelComponent cargoLoad : cargoLoads) {
                if (cargoLoad.id <= percentFull) {
                    if (cargoLoad.modelIDs.contains(group)) {
                        return true;
                    }
                    //if the stock should only render the current cargo load it'll stop at the highest matching load
                    if (showCurrentLoadOnly) {
                        break;
                    }
                }
            }
            return false;
        })).include(cargoLoads);
    }

}
