package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.FreightTank;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.registry.FreightDefinition;

public class FreightTankModel<ENTITY extends FreightTank, DEFINITION extends FreightDefinition> extends FreightModel<ENTITY, DEFINITION> {
    public FreightTankModel(DEFINITION def) throws Exception {
        super(def);
    }

    @Override
    protected void parseComponents(ComponentProvider provider, DEFINITION def) {
        super.parseComponents(provider, def);
        addGauge(provider, ModelComponentType.GAUGE_LIQUID_X, Readouts.LIQUID);
    }
}
