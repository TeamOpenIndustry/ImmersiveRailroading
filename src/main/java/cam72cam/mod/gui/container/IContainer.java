package cam72cam.mod.gui.container;

import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.ItemStackHandler;

public interface IContainer {
    int drawTopBar(int x, int y, int slots);
    int drawBottomBar(int x, int y, int slots);
    int drawPlayerInventoryConnector(int x, int y, int horizSlots);
    int drawPlayerTopBar(int x, int y);
    int drawPlayerInventory(int currY, int horizSlots);
    int drawPlayerMidBar(int x, int y);

    void drawSlotOverlay(ItemStack stack, int x, int y);

    void drawSlot(ItemStackHandler handler, int slotID, int x, int y);
    int drawSlotRow(ItemStackHandler handler, int start, int cols, int x, int y);
    int drawSlotBlock(ItemStackHandler handler, int start, int cols, int x, int y);

    void drawTankBlock(int x, int y, int horizSlots, int inventoryRows, Fluid fluid, float percentFull);

    void drawCenteredString(String quantityStr, int x, int y);
}
