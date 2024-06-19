package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.multiblock.CustomMultiblock;
import cam72cam.immersiverailroading.registry.MultiblockDefinition;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.gui.container.IContainer;
import cam72cam.mod.gui.container.IContainerBuilder;

public class CustomMBTransporterGUI implements IContainer {
    private final TileMultiblock tile;
    private final MultiblockDefinition def;

    public CustomMBTransporterGUI(TileMultiblock tile) {
        this.tile = tile;
        this.def = tile.getMultiblock() instanceof CustomMultiblock.CustomMultiblockInstance ?
                ((CustomMultiblock.CustomMultiblockInstance) tile.getMultiblock()).def :
                null;
    }

    public void draw(IContainerBuilder container){
        int currY = 0;
        currY = container.drawTopBar(0, currY, getSlotsX());
        currY = container.drawSlotBlock(tile.getContainer(), 0, getSlotsX(), 0, currY);
        currY = container.drawPlayerInventoryConnector(0, currY, getSlotsX());
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
