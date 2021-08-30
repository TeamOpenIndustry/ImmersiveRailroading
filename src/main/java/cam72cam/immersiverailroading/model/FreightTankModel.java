package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.FreightTank;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.part.Readout;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.FreightDefinition;

import java.util.List;

public class FreightTankModel<T extends FreightTank> extends FreightModel<T> {
    private List<Readout> fluidGauges;

    public FreightTankModel(FreightDefinition def) throws Exception {
        super(def);
    }

    @Override
    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        super.parseComponents(provider, def);
        fluidGauges = Readout.getReadouts(provider, ModelComponentType.GAUGE_LIQUID_X);
    }

    @Override
    protected void effects(T stock) {
        super.effects(stock);
        fluidGauges.forEach(g -> g.setValue(stock, stock.getPercentLiquidFull() / 100f));
    }

    @Override
    public List<Readout> getReadouts() {
        List<Readout> readouts = super.getReadouts();
        readouts.addAll(fluidGauges);
        return readouts;
    }
}
