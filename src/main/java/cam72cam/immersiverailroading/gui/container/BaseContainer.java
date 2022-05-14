package cam72cam.immersiverailroading.gui.container;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.mod.gui.container.IContainer;
import cam72cam.mod.gui.container.IContainerBuilder;

public abstract class BaseContainer implements IContainer {
    protected void drawName(IContainerBuilder container, EntityRollingStock stock) {
        String name = stock.getDefinition().name();
        if (stock.tag != null && !stock.tag.isEmpty()) {
            name = stock.tag;
        }
        container.drawCenteredString(name, 0, 7);
    }
}
