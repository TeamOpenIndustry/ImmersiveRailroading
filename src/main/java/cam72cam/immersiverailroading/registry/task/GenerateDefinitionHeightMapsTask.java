package cam72cam.immersiverailroading.registry.task;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;

import java.util.List;

public class GenerateDefinitionHeightMapsTask implements Runnable {

    private final List<EntityRollingStockDefinition> stockDefinitions;

    public GenerateDefinitionHeightMapsTask(List<EntityRollingStockDefinition> stockDefinitions) {
        this.stockDefinitions = stockDefinitions;
    }

    @Override
    public void run() {
        for (EntityRollingStockDefinition stockDefinition : this.stockDefinitions) {
            try {
                stockDefinition.initHeightMap();
            } catch (Exception e) {
                ImmersiveRailroading.catching(e);
            }
        }
    }

}
