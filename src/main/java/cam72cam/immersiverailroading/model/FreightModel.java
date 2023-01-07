package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.part.CargoFill;
import cam72cam.immersiverailroading.entity.Freight;
import cam72cam.immersiverailroading.model.part.CargoItems;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.FreightDefinition;
import cam72cam.mod.render.opengl.RenderState;

public class FreightModel<T extends Freight> extends StockModel<T> {
    private final FreightDefinition def;

    private CargoFill cargoFill;
    private CargoItems cargoItems;

    public FreightModel(FreightDefinition def) throws Exception {
        super(def);
        this.def = def;
    }

    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        super.parseComponents(provider, def);
        this.cargoFill = CargoFill.get(provider, null);
        this.cargoItems = CargoItems.get(provider);
    }

    @Override
    protected void render(T stock, ComponentRenderer draw, double distanceTraveled) {
        super.render(stock, draw, distanceTraveled);
        if (cargoFill != null) {
            cargoFill.render(stock.getPercentCargoFull(), def.shouldShowCurrentLoadOnly(), draw);
        }
    }

    @Override
    protected void postRender(T stock, RenderState state) {
        super.postRender(stock, state);

        if (cargoItems != null) {
            cargoItems.postRender(stock, state);
        }
    }
}
