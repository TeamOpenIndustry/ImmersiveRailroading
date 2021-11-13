package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.FreightTank;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.part.Readout;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.FreightDefinition;

public class FreightTankModel<T extends FreightTank> extends FreightModel<T> {
    public FreightTankModel(FreightDefinition def) throws Exception {
        super(def);
    }

    @Override
    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        super.parseComponents(provider, def);
        super.gauges.addAll(Readout.getReadouts(provider, ModelComponentType.GAUGE_LIQUID_X, Readouts.LIQUID));
    }
}
