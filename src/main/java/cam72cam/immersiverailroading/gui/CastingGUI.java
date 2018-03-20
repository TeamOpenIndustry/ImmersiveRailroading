package cam72cam.immersiverailroading.gui;

import java.io.IOException;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.items.nbt.ItemRawCast;
import cam72cam.immersiverailroading.library.CraftingType;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.multiblock.CastingMultiblock.CastingInstance;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.util.ItemCastingCost;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidRegistry;

public class CastingGUI extends GuiScreen {
    public static final ResourceLocation CASTING_GUI_TEXTURE = new ResourceLocation("immersiverailroading:gui/casting_gui.png");
    
	private GuiButton gaugeButton;
	private Gauge gauge;

	private GuiButton pickerButton;
	private CraftPicker picker;
	
	private GuiButton singleCastButton;
	private GuiButton repeatCastButton;
	
	private TileMultiblock tile;
	private ItemStack currentItem;
	private double fluidPercent;
	
	public CastingGUI(TileMultiblock te) {
		this.tile = te;
		currentItem = ((CastingInstance) te.getMultiblock()).getCraftItem();
		fluidPercent = ((CastingInstance) te.getMultiblock()).getSteelLevel();
		
		gauge = ItemGauge.get(currentItem);
		picker = new CraftPicker(null, CraftingType.CASTING, (ItemStack item) -> {
        	this.mc.displayGuiScreen(this);
        	
        	if (item != null) {
        		currentItem = item;
        		updatePickerButton();
        	}
        });
	}
	
	private void updatePickerButton() {
		if (currentItem.isEmpty()) {
			pickerButton.displayString = GuiText.SELECTOR_TYPE.toString("");
		} else {
			pickerButton.displayString = currentItem.getDisplayName();
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
	
	@Override
	public void initGui() {
		int buttonID = 0;

		gaugeButton = new GuiButton(buttonID++, this.width / 2, this.height / 4, 100, 20, GuiText.SELECTOR_GAUGE.toString(gauge));
		this.buttonList.add(gaugeButton);
		
		singleCastButton = new GuiButton(buttonID++, this.width / 2, this.height / 4 + 20, 100, 20, "Single Cast");
		this.buttonList.add(singleCastButton);
		
		repeatCastButton = new GuiButton(buttonID++, this.width / 2, this.height / 4 + 40, 100, 20, "Repeat Cast");
		this.buttonList.add(repeatCastButton);
		
		pickerButton = new GuiButton(buttonID++, this.width / 2 - 100, this.height / 4 + 105, GuiText.SELECTOR_TYPE.toString(""));
		updatePickerButton();
		this.buttonList.add(pickerButton);
		
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		
		float cost = ItemCastingCost.getCastCost(currentItem);
		
    	
    	this.mc.getTextureManager().bindTexture(CASTING_GUI_TEXTURE);

        GUIHelpers.texturedRect(this.width / 2 - 100, this.height / 4, 200, 100);

		GUIHelpers.drawTankBlock(this.width / 2 - 94.5, this.height / 4 + 3, 56.7, 60, FluidRegistry.LAVA, (float) fluidPercent, false, 0x99fb7e15);
		GUIHelpers.drawTankBlock(this.width / 2 - 28.5, this.height / 4 + 67, 125.2, 30, FluidRegistry.LAVA, this.tile.getCraftProgress()/cost, false, 0x998c1919);
	}
	
	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button == gaugeButton) {
			gauge = Gauge.values()[((gauge.ordinal() + 1) % (Gauge.values().length))];
			gaugeButton.displayString = GuiText.SELECTOR_GAUGE.toString(gauge);
		}
		if (button == pickerButton) {
			this.mc.displayGuiScreen(picker);
		}
		if (button == singleCastButton) {
			sendPacket();
			this.mc.displayGuiScreen(null);
			if (this.mc.currentScreen == null)
				this.mc.setIngameFocus();
		}
	}
	
	private void sendPacket() {
		ItemGauge.set(currentItem, gauge);
		currentItem.setCount(1);
		ItemRawCast.set(currentItem, true);
    	((CastingInstance) tile.getMultiblock()).setCraftItem(currentItem);
    }
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Enter or ESC
        if (keyCode == 1 || keyCode == 28 || keyCode == 156) {
			this.mc.displayGuiScreen(null);
			if (this.mc.currentScreen == null)
				this.mc.setIngameFocus();
        }
	}
}