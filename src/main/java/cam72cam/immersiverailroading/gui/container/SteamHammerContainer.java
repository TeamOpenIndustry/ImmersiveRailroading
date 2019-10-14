package cam72cam.immersiverailroading.gui.container;

import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.gui.container.IContainer;
import cam72cam.mod.gui.container.IContainerBuilder;

public class SteamHammerContainer implements IContainer {
    private final TileMultiblock tile;

    public SteamHammerContainer(TileMultiblock tile) {
        this.tile = tile;
    }

    public void draw(IContainerBuilder container){
        int currY = 0;
        int horizSlots = 10;
        int inventoryRows = 4;

        currY = container.drawTopBar(0, currY, horizSlots);
        int tankY = currY;
        int slotY = 0;

        for (int i = 0; i < inventoryRows; i++) {
            currY = container.drawSlotRow(null, 0, horizSlots, 0, currY);
            if (i == 0) {
                slotY = currY;
            }
        }

        container.drawTankBlock(0, tankY, horizSlots, inventoryRows, Fluid.LAVA, this.tile.getCraftProgress()/100f);

        container.drawSlot(tile.getContainer(), 0, 0, slotY);
        container.drawSlot(tile.getContainer(),1, 16 * horizSlots, slotY);

        currY = container.drawPlayerInventoryConnector(0, currY, horizSlots);
        currY = container.drawPlayerInventory(currY, horizSlots);
    }

    @Override
    public int getSlotsX() {
        return 10;
    }

    @Override
    public int getSlotsY() {
        return 4;
    }
}
