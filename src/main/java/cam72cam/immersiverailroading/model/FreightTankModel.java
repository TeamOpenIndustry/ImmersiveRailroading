package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.FreightTank;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.part.Readout;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.FreightDefinition;

import java.util.List;

public class FreightTankModel<T extends FreightTank> extends FreightModel<T> {
    private List<Readout<T>> gauges;

    public FreightTankModel(FreightDefinition def) throws Exception {
        super(def);
    }

    @Override
    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        super.parseComponents(provider, def);
        gauges = Readout.getReadouts(provider, ModelComponentType.GAUGE_LIQUID_X, stock -> stock.getPercentLiquidFull() / 100f);
    }

    @Override
    public List<Readout<T>> getReadouts() {
        List<Readout<T>> readouts = super.getReadouts();
        readouts.addAll(gauges);
        return readouts;
    }
}
