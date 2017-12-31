package cam72cam.immersiverailroading.gui;

import java.io.IOException;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.library.CraftingType;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.net.MultiblockSelectCraftPacket;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;

public class SteamHammerContainerGui extends ContainerGuiBase {
	
	private int inventoryRows;
	private int horizSlots;
	private Gauge gauge;
	private TileMultiblock tile;
	private CraftPicker picker;
	private GuiButton gaugeButton;
	
	private ItemStack currentItem;

    public SteamHammerContainerGui(SteamHammerContainer container) {
        super(container);
        this.inventoryRows = container.numRows;
        this.tile = container.tile;
        this.horizSlots = 10;
        this.xSize = paddingRight + horizSlots * slotSize + paddingLeft;
        this.ySize = 114 + this.inventoryRows * slotSize;
        
        picker = new CraftPicker(tile.getCraftItem(), CraftingType.CASTING_HAMMER, (ItemStack item) -> {
        	this.mc.displayGuiScreen(this);
        	
        	if (item != null) {
	        	currentItem = item;
	        	sendPacket(currentItem);
        	}
        });
        
		this.gauge = ItemGauge.get(tile.getCraftItem());
		
		currentItem = tile.getCraftItem();
    }
    
    public void initGui() {
    	super.initGui();
    	gaugeButton = new GuiButton(1, this.width / 2 - 100 + 3, (this.height - this.ySize) / 2, 194, 20, GuiText.SELECTOR_GAUGE.toString(gauge));
		this.buttonList.add(gaugeButton);
    }
    
    private void sendPacket(ItemStack selected) {
		if (selected.getItem() == ImmersiveRailroading.ITEM_ROLLING_STOCK_COMPONENT) {
			ItemGauge.set(selected, gauge);
		}
		ImmersiveRailroading.net.sendToServer(new MultiblockSelectCraftPacket(tile.getPos(), selected));
    }
    
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		if (gaugeButton.mousePressed(mc, mouseX, mouseY)) {
			gauge = Gauge.values()[((gauge.ordinal() + 1) % (Gauge.values().length))];
			gaugeButton.displayString = GuiText.SELECTOR_GAUGE.toString(gauge);
			sendPacket(currentItem);
			return;
		}
    	
    	int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
    	
    	if (mouseX > i + paddingLeft + 2*slotSize && mouseX < i + paddingLeft  + horizSlots * slotSize - 2*slotSize) {
    		if (mouseY > j + topOffset && mouseY < j + topOffset + inventoryRows * slotSize ) {
    			this.mc.displayGuiScreen(picker);
    		}
    	}
    	
    	super.mouseClicked(mouseX, mouseY, mouseButton);
    }
    
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
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
    	this.itemRender.renderItemIntoGUI(currentItem, (this.width/2-32) / scale, (int)(currY - inventoryRows * slotSize) / scale);
    	GL11.glPopMatrix();
    	
    	this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
    	
    	drawTankBlock(i + paddingLeft, currY - inventoryRows * slotSize, horizSlots, inventoryRows, FluidRegistry.LAVA, this.tile.getCraftProgress()/100f);
    	
    	drawSlot(i + paddingLeft+5, currY - inventoryRows * slotSize + (int)(slotSize * 1.5));
    	drawSlot(i + paddingLeft + slotSize * horizSlots - slotSize-5, currY - inventoryRows * slotSize + (int)(slotSize * 1.5));
    	
    	currY = drawPlayerInventoryConnector(i, currY, width, horizSlots);
    	currY = drawPlayerInventory((width - playerXSize) / 2, currY);
    }
}