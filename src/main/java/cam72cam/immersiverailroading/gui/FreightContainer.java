package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.Freight;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class FreightContainer extends Container {
	
	private Freight freight;
	protected int numRows;

	public FreightContainer(IInventory playerInventory, Freight stock) {
		this.freight = stock;
		this.numRows = stock.getInventorySize() / 9;
        int i = (this.numRows - 4) * 18;
		
		IItemHandler itemHandler = this.freight.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
		
		for (int j = 0; j < this.numRows; ++j)
        {
            for (int k = 0; k < 9; ++k)
            {
                this.addSlotToContainer(new SlotItemHandler(itemHandler, k + j * 9, 8 + k * 18, 18 + j * 18));
            }
        }

        for (int l = 0; l < 3; ++l)
        {
            for (int j1 = 0; j1 < 9; ++j1)
            {
                this.addSlotToContainer(new Slot(playerInventory, j1 + l * 9 + 9, 8 + j1 * 18, 103 + l * 18 + i));
            }
        }

        for (int i1 = 0; i1 < 9; ++i1)
        {
            this.addSlotToContainer(new Slot(playerInventory, i1, 8 + i1 * 18, 161 + i));
        }
	}

	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		return true;
	}

}
