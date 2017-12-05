package cam72cam.immersiverailroading.gui;

import java.io.IOException;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.items.ItemRollingStockComponent;
import cam72cam.immersiverailroading.items.ItemTabs;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.net.SteamHammerSelectPacket;
import cam72cam.immersiverailroading.tile.TileSteamHammer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.FluidRegistry;

public class SteamHammerContainerGui extends ContainerGuiBase {
	
	private int inventoryRows;
	private int horizSlots;
	private Gauge gauge;
	private TileSteamHammer tile;
	private ItemPickerGUI stockSelector;
	private ItemPickerGUI itemSelector;
	private GuiButton gaugeButton;
	private NonNullList<ItemStack> items;

    public SteamHammerContainerGui(SteamHammerContainer container) {
        super(container);
        this.inventoryRows = container.numRows;
        this.tile = container.tile;
        this.horizSlots = 10;
        this.xSize = paddingRight + horizSlots * slotSize + paddingLeft;
        this.ySize = 114 + this.inventoryRows * slotSize; 
        
        NonNullList<ItemStack> items = NonNullList.create(); 
        
        ImmersiveRailroading.ITEM_ROLLING_STOCK_COMPONENT.getSubItems(ItemTabs.COMPONENT_TAB, items);
        
        NonNullList<ItemStack> stock = NonNullList.create();
        stock.add(new ItemStack(ImmersiveRailroading.ITEM_LARGE_WRENCH, 1));
        stock.add(new ItemStack(ImmersiveRailroading.ITEM_HOOK, 1));
        stock.add(new ItemStack(ImmersiveRailroading.ITEM_RAIL_BLOCK, 1));
        ImmersiveRailroading.ITEM_AUGMENT.getSubItems(ItemTabs.MAIN_TAB, stock);
        ImmersiveRailroading.ITEM_ROLLING_STOCK.getSubItems(ItemTabs.STOCK_TAB, stock);

		stockSelector = new ItemPickerGUI(stock);
		for (ItemStack itemStock : stock) {
			if (isPartOf(tile.getChoosenItem(), itemStock)) {				
				stockSelector.choosenItem = itemStock;
			}
		}
		
		itemSelector = new ItemPickerGUI(NonNullList.create());
		itemSelector.choosenItem = tile.getChoosenItem();
		this.items = items;
		
		this.gauge = ItemRollingStockComponent.getGauge(itemSelector.choosenItem);
    }
    
    public void initGui() {
    	super.initGui();
    	gaugeButton = new GuiButton(1, this.width / 2 - 100 + 3, (this.height - this.ySize) / 2, 194, 20, GuiText.SELECTOR_GAUGE.toString(gauge));
		this.buttonList.add(gaugeButton);
    }
    
    private boolean isPartOf(ItemStack stock, ItemStack item) {
    	if (stock.getItem() != ImmersiveRailroading.ITEM_ROLLING_STOCK) {
    		return false;
    	}
    	if (item.getItem() != ImmersiveRailroading.ITEM_ROLLING_STOCK_COMPONENT) {
    		return false;
    	}
    	return ItemRollingStockComponent.getDefinitionID(item).equals(ItemRollingStock.getDefinitionID(stock));
    }
    
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    	if (itemSelector.isActive) {
    		itemSelector.mouseClicked(mouseX, mouseY, mouseButton);
    		
    		if (!itemSelector.isActive) {
    			ImmersiveRailroading.net.sendToServer(new SteamHammerSelectPacket(tile.getPos(), this.itemSelector.choosenItem, this.gauge));
    		}
        	
        	return;
        }
    	if (stockSelector.isActive) {
    		stockSelector.mouseClicked(mouseX, mouseY, mouseButton);
    		
    		if (!stockSelector.isActive) {
    			NonNullList<ItemStack> filteredItems = NonNullList.create();
    			for (ItemStack item : items) {
    				if (isPartOf(stockSelector.choosenItem, item)) {
    					filteredItems.add(item);
    				}
    			}
    			if (filteredItems.size() == 0) {
    				itemSelector.choosenItem = this.stockSelector.choosenItem;
        			ImmersiveRailroading.net.sendToServer(new SteamHammerSelectPacket(tile.getPos(), this.stockSelector.choosenItem, this.gauge));
    			} else {
	    			itemSelector.setItems(filteredItems);
	    			itemSelector.isActive = true;
    			}
    		}
        	
        	return;
        }
    	
		if (gaugeButton.mousePressed(mc, mouseX, mouseY)) {
			gauge = Gauge.values()[((gauge.ordinal() + 1) % (Gauge.values().length))];
			gaugeButton.displayString = GuiText.SELECTOR_GAUGE.toString(gauge);
			ImmersiveRailroading.net.sendToServer(new SteamHammerSelectPacket(tile.getPos(), this.itemSelector.choosenItem, this.gauge));
			return;
		}
    	
    	int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
    	
    	if (mouseX > i + paddingLeft + 2*slotSize && mouseX < i + paddingLeft  + horizSlots * slotSize - 2*slotSize) {
    		if (mouseY > j + topOffset && mouseY < j + topOffset + inventoryRows * slotSize ) {
    			if (stockSelector.choosenItem != null) {
    				NonNullList<ItemStack> filteredItems = NonNullList.create();
        			for (ItemStack item : items) {
        				if (isPartOf(stockSelector.choosenItem, item)) {
        					filteredItems.add(item);
        				}
        			}
        			if (filteredItems.size() == 0) {
        				stockSelector.isActive = true;
        			} else {
	        			itemSelector.setItems(filteredItems);
	        			itemSelector.isActive = true;
        			}
    			} else {
    				stockSelector.isActive = true;
    			}
    		}
    	}
    	
    	super.mouseClicked(mouseX, mouseY, mouseButton);
    }
    
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
    	if (itemSelector.isActive) {
    		itemSelector.drawScreen(mouseX, mouseY, partialTicks);
    	} else if (stockSelector.isActive) {
    		stockSelector.drawScreen(mouseX, mouseY, partialTicks);
		} else {
	    	super.drawScreen(mouseX, mouseY, partialTicks);
		}
    }

    public void setWorldAndResolution(Minecraft mc, int width, int height) {
		super.setWorldAndResolution(mc, width, height);
		itemSelector.setWorldAndResolution(mc, width, height);
		stockSelector.setWorldAndResolution(mc, width, height);
	}
	
	public void setGuiSize(int w, int h) {
		this.setGuiSize(w, h);
		itemSelector.setGuiSize(w, h);
		stockSelector.setGuiSize(w, h);
	}
    
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
    	// Enter or ESC
        if (keyCode == 1) {
        	if (itemSelector.isActive) {
        		itemSelector.isActive = false;
    			stockSelector.isActive = true;
        		return;
        	}
        	if (stockSelector.isActive) {
        		stockSelector.isActive = false;
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