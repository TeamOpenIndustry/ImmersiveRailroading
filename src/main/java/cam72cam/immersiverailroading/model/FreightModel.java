package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.part.Cargo;
import cam72cam.immersiverailroading.entity.Freight;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.FreightDefinition;

public class FreightModel<T extends Freight> extends StockModel<T> {
    private final FreightDefinition def;

    private Cargo cargo;

    public FreightModel(FreightDefinition def) throws Exception {
        super(def);
        this.def = def;
    }

    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        super.parseComponents(provider, def);
        this.cargo = Cargo.get(provider, null);
    }

    @Override
    protected void render(T stock, ComponentRenderer draw, double distanceTraveled) {
        super.render(stock, draw, distanceTraveled);
        if (cargo != null) {
            cargo.render(stock.getPercentCargoFull(), def.shouldShowCurrentLoadOnly(), draw);
        }
    }
}
