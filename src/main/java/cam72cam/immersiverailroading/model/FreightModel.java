package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.Freight;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.part.CargoFill;
import cam72cam.immersiverailroading.model.part.CargoItems;
import cam72cam.immersiverailroading.model.part.CargoUnload;
import cam72cam.immersiverailroading.registry.FreightDefinition;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.opengl.RenderState;

import java.util.LinkedList;
import java.util.List;

public class FreightModel<ENTITY extends Freight, DEFINITION extends FreightDefinition> extends StockModel<ENTITY, DEFINITION> {
    private CargoFill cargoFill;
    private CargoItems cargoItems;
    private List<CargoUnload> cargoUnload;

    public FreightModel(DEFINITION def) throws Exception {
        super(def);
    }

    protected void parseComponents(ComponentProvider provider, DEFINITION def) {
        super.parseComponents(provider, def);
        this.cargoFill = CargoFill.get(provider, rocking, def.shouldShowCurrentLoadOnly(), null);
        this.cargoItems = CargoItems.get(provider);
        this.cargoUnload = CargoUnload.get(provider);
    }

    public List<ItemStack> getCargoNearbyItems(EntityRollingStock stock) {
        List<ItemStack> list = new LinkedList<>();
        if(cargoItems != null){
            list.addAll(cargoItems.getDroppedItem(stock.getWorld(), stock));
        }
        if(cargoFill != null){
            list.addAll(cargoFill.getDroppedItem(stock.getWorld(), stock));
        }
        return list;
    }

    public List<CargoUnload> getUnloadingPoints() {
        return cargoUnload;
    }

    @Override
    protected void postRender(ENTITY stock, RenderState state, float partialTicks) {
        super.postRender(stock, state, partialTicks);

        if (cargoItems != null) {
            cargoItems.postRender(stock, state);
        }
    }
}
