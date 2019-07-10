package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.tile.TileMultiblock;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class SteamHammerContainer extends ContainerBase {
	protected int numRows;
	protected TileMultiblock tile;

	public SteamHammerContainer(IInventory playerInventory, TileMultiblock tile) {
        int horizSlots = 10;
		this.numRows = 4;
		this.tile = tile;
		
		IItemHandler itemHandler = null; // TODO CAPABILITIES tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
		
		int width = 0;
		int currY = 0;
		currY = offsetTopBar(0, currY, horizSlots);
		currY = offsetSlotBlock(0, currY, horizSlots, numRows);
		
		this.addSlotToContainer(new SlotItemHandler(itemHandler, 0, 0 + paddingLeft + 5, currY - numRows * slotSize + (int)(slotSize * 1.5)));
		this.addSlotToContainer(new SlotItemHandler(itemHandler, 1, 0 + paddingLeft + slotSize * horizSlots - slotSize - 5, currY - numRows * slotSize + (int)(slotSize * 1.5)));
		
    	currY = offsetPlayerInventoryConnector(0, currY, width, horizSlots);
    	currY = addPlayerInventory(playerInventory, currY, horizSlots);
	}
	
	@Override
	public int numSlots() {
		return 2;
	}
}
