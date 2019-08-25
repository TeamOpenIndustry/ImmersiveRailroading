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

    void drawSlot(int x, int y);
    void drawSlotOverlay(ItemStack stack, int i, int j);
    int drawSlotRow(int x, int y, int slots, int numSlots);
    int drawSlotBlock(ItemStackHandler handler, int slots, int x, int y, int slotX);

    void drawTankBlock(int x, int y, int horizSlots, int inventoryRows, Fluid fluid, float percentFull);
}
