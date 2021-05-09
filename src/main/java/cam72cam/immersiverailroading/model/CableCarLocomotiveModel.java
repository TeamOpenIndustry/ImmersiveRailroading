package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.CableCar;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.part.Horn;
import cam72cam.immersiverailroading.registry.CableCarDefinition;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;

public class CableCarLocomotiveModel extends LocomotiveModel<CableCar> {
    private Horn horn;

    public CableCarLocomotiveModel(CableCarDefinition def) throws Exception {
        super(def);
    }

    @Override
    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        super.parseComponents(provider, def);
        horn = Horn.get(provider, ((CableCarDefinition)def).horn, ((CableCarDefinition)def).getHornSus());
    }

    @Override
    protected void effects(CableCar stock) {
        super.effects(stock);
        horn.effects(stock,
                stock.getHornTime() > 0
                        ? stock.getDefinition().getHornSus() ? stock.getHornTime() / 10f : 1
                        : 0);
    }

    @Override
    protected void removed(CableCar stock) {
        super.removed(stock);
        horn.removed(stock);
    }

    @Override
    protected void render(CableCar stock, ComponentRenderer draw, double distanceTraveled) {
        super.render(stock, draw, distanceTraveled);
        horn.render(draw);
    }
}
