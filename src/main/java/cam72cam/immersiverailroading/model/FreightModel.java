package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.part.CargoFill;
import cam72cam.immersiverailroading.entity.Freight;
import cam72cam.immersiverailroading.model.part.CargoItems;
import cam72cam.immersiverailroading.registry.FreightDefinition;
import cam72cam.mod.entity.ItemEntity;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.RenderState;
import util.Matrix4;

import java.util.*;
import java.util.stream.Collectors;

public class FreightModel<ENTITY extends Freight, DEFINITION extends FreightDefinition> extends StockModel<ENTITY, DEFINITION> {
    private CargoFill cargoFill;
    private CargoItems cargoItems;
    private boolean hasCargo;

    public FreightModel(DEFINITION def) throws Exception {
        super(def);
    }

    protected void parseComponents(ComponentProvider provider, DEFINITION def) {
        super.parseComponents(provider, def);
        this.cargoFill = CargoFill.get(provider, rocking, def.shouldShowCurrentLoadOnly(), null);
        this.cargoItems = CargoItems.get(provider);
        this.hasCargo = this.cargoFill != null || this.cargoItems != null;
    }

    @Override
    protected void postRender(ENTITY stock, RenderState state, float partialTicks) {
        super.postRender(stock, state, partialTicks);

        if (cargoItems != null) {
            cargoItems.postRender(stock, state);
        }
    }

    public Set<ItemEntity> filterItems(EntityMoveableRollingStock stock, List<ItemEntity> entities) {
        if (hasCargo) {
            Matrix4 inverted = stock.getModelMatrix();
            inverted.invert();
            return entities.stream().filter(entity -> {
                final Vec3d point1 = inverted.apply(entity.getPosition());
                boolean flag = false;
                if (this.cargoFill != null) {
                    flag |= this.cargoFill.boxes.stream().anyMatch(box -> box.contains(point1));
                }
                if (this.cargoItems != null) {
                    flag |= this.cargoItems.boxes.stream().anyMatch(box -> box.contains(point1));
                }
                return flag;
            }).collect(Collectors.toSet());
        }
        return new HashSet<>();
    }
}
