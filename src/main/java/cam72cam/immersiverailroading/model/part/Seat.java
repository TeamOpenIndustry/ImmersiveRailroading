package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;

import java.util.List;
import java.util.stream.Collectors;

public class Seat<T extends EntityMoveableRollingStock> extends Interactable<T> {

    public static <T extends EntityMoveableRollingStock> List<Seat<T>> get(ComponentProvider provider, ModelState state) {
        return provider.parseAll(ModelComponentType.SEAT_X).stream().map(s -> new Seat<T>(s, state)).collect(Collectors.toList());
    }

    public Seat(ModelComponent part, ModelState state) {
        super(part);
        state.include(part);
    }
}
