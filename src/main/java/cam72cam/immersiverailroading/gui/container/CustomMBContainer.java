package cam72cam.immersiverailroading.gui.container;

import cam72cam.immersiverailroading.multiblock.CustomMultiblock;
import cam72cam.immersiverailroading.registry.MultiblockDefinition;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.gui.container.IContainer;
import cam72cam.mod.gui.container.IContainerBuilder;

public class CustomMBContainer implements IContainer {
    private final TileMultiblock tile;
    private final MultiblockDefinition def;

    public CustomMBContainer(TileMultiblock tile) {
        this.tile = tile;
        this.def = tile.getMultiblock() instanceof CustomMultiblock.CustomMultiblockInstance ?
                ((CustomMultiblock.CustomMultiblockInstance) tile.getMultiblock()).def :
                null;
    }

    public void draw(IContainerBuilder container) {
        int currY = 0;
        if (def.tankCapability != 0) {
            currY = container.drawTopBar(0, currY, getSlotsX());
            int slotHeight = (int) Math.ceil(def.tankCapability / 500d / getSlotsX());

            int tankY = currY;
            int slotY = 1;
            for (int i = 0; i < slotHeight; i++) {
                currY = container.drawSlotRow(null, 0, getSlotsX(), 0, currY);
                if(i == (slotHeight - 1) / 2)
                    slotY = currY;
            }

            container.drawTankBlock(0, tankY, getSlotsX(), 4,
                    tile.getFluidContainer().getContents().getFluid(),
                    tile.getFluidContainer().getContents().getAmount() / (float) def.tankCapability);

            String quantityStr = String.format("%s/%s", tile.getFluidContainer().getContents().getAmount(), def.tankCapability);
            container.drawCenteredString(quantityStr, 0, slotY);
        }else{
            currY = container.drawTopBar(0, currY, getSlotsX());
        }
        if (tile.getContainer().getSlotCount() != 0) {
            currY = container.drawSlotBlock(tile.getContainer(), 0, getSlotsX(), 0, currY);
            currY = container.drawPlayerInventoryConnector(0, currY, getSlotsX());
        }else{
            currY = container.drawPlayerInventoryConnector(0, currY, getSlotsX());
        }
        currY = container.drawPlayerInventory(currY, getSlotsX());
    }

    @Override
    public int getSlotsX() {
        return this.def.inventoryWidth;
    }

    @Override
    public int getSlotsY() {
        return this.def.inventoryHeight;
    }
}
