package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.part.CargoFill;
import cam72cam.immersiverailroading.entity.Freight;
import cam72cam.immersiverailroading.model.part.CargoItems;
import cam72cam.immersiverailroading.registry.FreightDefinition;
import cam72cam.mod.render.opengl.RenderState;

public class FreightModel<ENTITY extends Freight, DEFINITION extends FreightDefinition> extends StockModel<ENTITY, DEFINITION> {
    private CargoFill cargoFill;
    private CargoItems cargoItems;

    public FreightModel(DEFINITION def) throws Exception {
        super(def);
    }

    protected void parseComponents(ComponentProvider provider, DEFINITION def) {
        super.parseComponents(provider, def);
        this.cargoFill = CargoFill.get(provider, rocking, def.shouldShowCurrentLoadOnly(), null);
        this.cargoItems = CargoItems.get(provider);
    }

    @Override
    protected void postRender(ENTITY stock, RenderState state) {
        super.postRender(stock, state);

        if (cargoItems != null) {
            cargoItems.postRender(stock, state);
        }
    }
}
