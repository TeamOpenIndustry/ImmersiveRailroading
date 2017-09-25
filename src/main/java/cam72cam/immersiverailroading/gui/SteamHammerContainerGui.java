package cam72cam.immersiverailroading.gui;

import java.io.IOException;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.net.SteamHammerSelectPacket;
import cam72cam.immersiverailroading.tile.TileSteamHammer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.FluidRegistry;

public class SteamHammerContainerGui extends ContainerGuiBase {
	
	private int inventoryRows;
	private int horizSlots;
	private TileSteamHammer tile;
	private ItemPickerGUI itemSelector;

    public SteamHammerContainerGui(SteamHammerContainer container) {
        super(container);
        this.inventoryRows = container.numRows;
        this.tile = container.tile;
        this.horizSlots = 10;
        this.xSize = paddingRight + horizSlots * slotSize + paddingLeft;
        this.ySize = 114 + this.inventoryRows * slotSize;
        
        NonNullList<ItemStack> items = NonNullList.create(); 
        
        ImmersiveRailroading.ITEM_ROLLING_STOCK_COMPONENT.getSubItems(CreativeTabs.TRANSPORTATION, items);

		itemSelector = new ItemPickerGUI(items);
		itemSelector.choosenItem = tile.getChoosenItem();
    }
    
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    	if (itemSelector.isActive) {
    		itemSelector.mouseClicked(mouseX, mouseY, mouseButton);
    		
    		if (!itemSelector.isActive) {
    			ImmersiveRailroading.net.sendToServer(new SteamHammerSelectPacket(tile.getPos(), this.itemSelector.choosenItem));
    		}
        	
        	return;
        }
    	
    	int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
    	
    	if (mouseX > i + paddingLeft + 2*slotSize && mouseX < i + paddingLeft  + horizSlots * slotSize - 2*slotSize) {
    		if (mouseY > j + topOffset && mouseY < j + topOffset + inventoryRows * slotSize ) {
    			itemSelector.isActive = true;
    		}
    	}
    	
    	super.mouseClicked(mouseX, mouseY, mouseButton);
    }
    
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
    	if (itemSelector.isActive) {
    		itemSelector.drawScreen(mouseX, mouseY, partialTicks);
		} else {
	    	super.drawScreen(mouseX, mouseY, partialTicks);
		}
    }

    public void setWorldAndResolution(Minecraft mc, int width, int height) {
		super.setWorldAndResolution(mc, width, height);
		itemSelector.setWorldAndResolution(mc, width, height);
	}
	
	public void setGuiSize(int w, int h) {
		this.setGuiSize(w, h);
		itemSelector.setGuiSize(w, h);
	}
    
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
    	// Enter or ESC
        if (keyCode == 1 || keyCode == 28 || keyCode == 156) {
        	if (itemSelector.isActive) {
        		itemSelector.isActive = false;
        		return;
        	}
			this.mc.displayGuiScreen(null);
			if (this.mc.currentScreen == null)
				this.mc.setIngameFocus();
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
    	int currY = j;
    	
        this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
        
        currY = drawTopBar(i, currY, horizSlots);
    	currY = drawSlotBlock(i, currY, horizSlots, inventoryRows);
    	
    	GL11.glPushMatrix();
    	int scale = 4;
    	GL11.glScaled(scale, scale, scale);
    	this.itemRender.renderItemIntoGUI(itemSelector.choosenItem, (this.width/2-32) / scale, (int)(currY - inventoryRows * slotSize) / scale);
    	GL11.glPopMatrix();
    	
    	this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
    	
    	drawTankBlock(i + paddingLeft, currY - inventoryRows * slotSize, horizSlots, inventoryRows, FluidRegistry.LAVA, this.tile.getCraftProgress()/100f);
    	
    	drawSlot(i + paddingLeft+5, currY - inventoryRows * slotSize + (int)(slotSize * 1.5));
    	drawSlot(i + paddingLeft + slotSize * horizSlots - slotSize-5, currY - inventoryRows * slotSize + (int)(slotSize * 1.5));
    	
    	currY = drawPlayerInventoryConnector(i, currY, width, horizSlots);
    	currY = drawPlayerInventory((width - playerXSize) / 2, currY);
    }
}