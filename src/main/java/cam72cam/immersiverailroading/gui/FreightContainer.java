package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.CarFreight;
import net.minecraft.inventory.IInventory;
import invtweaks.api.container.ChestContainer;

@ChestContainer
public class FreightContainer extends ContainerBase {
	
	private CarFreight freight;
	protected int numRows;
	protected int numSlots;

	public FreightContainer(IInventory playerInventory, CarFreight stock) {
		this.freight = stock;
        int horizSlots = stock.getInventoryWidth();
		this.numRows = (int) Math.ceil((double)stock.getInventorySize() / horizSlots);
		this.numSlots = stock.getInventorySize(); 

		int width = 0;
		int currY = 0;
		currY = offsetTopBar(0, currY, horizSlots);
		currY = addSlotBlock(freight.cargoItems, stock.getInventorySize(), 0, currY, horizSlots);
    	currY = offsetPlayerInventoryConnector(0, currY, width, horizSlots);
    	currY = addPlayerInventory(playerInventory, currY, horizSlots);
	}

	@Override
	public int numSlots() {
		return freight.getInventorySize();
	}
}
