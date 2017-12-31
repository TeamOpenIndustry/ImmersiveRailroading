package cam72cam.immersiverailroading.gui;

import java.io.IOException;

import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.library.CraftingType;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.multiblock.CastingMultiblock.CastingInstance;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

public class CastingGUI extends GuiScreen {
	private GuiButton gaugeButton;
	private Gauge gauge;

	private GuiButton pickerButton;
	private CraftPicker picker;
	
	private TileMultiblock tile;
	private ItemStack currentItem;
	
	public CastingGUI(TileMultiblock te) {
		this.tile = te;
		currentItem = ((CastingInstance) te.getMultiblock()).getCraftItem();
		
		gauge = ItemGauge.get(currentItem);
		picker = new CraftPicker(null, CraftingType.CASTING, (ItemStack item) -> {
        	this.mc.displayGuiScreen(this);
        	
        	if (item != null) {
        		currentItem = item;
        		updatePickerButton();
	        	sendPacket();
        	}
        });
	}
	
	private void updatePickerButton() {
		pickerButton.displayString = GuiText.SELECTOR_TYPE.toString(currentItem.getDisplayName());
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
	
	@Override
	public void initGui() {
		int buttonID = 0;

		gaugeButton = new GuiButton(buttonID++, this.width / 2 - 100, this.height / 4 - 24 + buttonID * 30, GuiText.SELECTOR_GAUGE.toString(gauge));
		this.buttonList.add(gaugeButton);
		
		pickerButton = new GuiButton(buttonID++, this.width / 2 - 100, this.height / 4 - 24 + buttonID * 30, GuiText.SELECTOR_TYPE.toString(""));
		updatePickerButton();
		this.buttonList.add(pickerButton);
	}
	
	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button == gaugeButton) {
			gauge = Gauge.values()[((gauge.ordinal() + 1) % (Gauge.values().length))];
			gaugeButton.displayString = GuiText.SELECTOR_GAUGE.toString(gauge);
			sendPacket();
		}
		if (button == pickerButton) {
			this.mc.displayGuiScreen(picker);
		}
	}
	
	private void sendPacket() {
		ItemGauge.set(currentItem, gauge);
		currentItem.setCount(1);
    	((CastingInstance) tile.getMultiblock()).setCraftItem(currentItem);
    }
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Enter or ESC
        if (keyCode == 1 || keyCode == 28 || keyCode == 156) {
        	sendPacket();

			this.mc.displayGuiScreen(null);
			if (this.mc.currentScreen == null)
				this.mc.setIngameFocus();
        }
	}
}