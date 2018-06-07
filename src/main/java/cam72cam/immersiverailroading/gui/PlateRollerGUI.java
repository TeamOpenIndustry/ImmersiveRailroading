package cam72cam.immersiverailroading.gui;

import java.io.IOException;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.items.nbt.ItemPlateType;
import cam72cam.immersiverailroading.library.CraftingType;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.PlateType;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

public class PlateRollerGUI extends GuiScreen {
	private GuiButton gaugeButton;
	private Gauge gauge;
	
	private GuiButton plateButton;
	private PlateType plate;

	private GuiButton pickerButton;
	private CraftPicker picker;
	
	private TileMultiblock tile;
	private ItemStack currentItem;
	
	public PlateRollerGUI(TileMultiblock te) {
		this.tile = te;
		currentItem = te.getCraftItem();
		if (currentItem == null || currentItem.isEmpty()) {
			currentItem = new ItemStack(IRItems.ITEM_PLATE, 1);
		}
		
		gauge = ItemGauge.get(currentItem);
		plate = ItemPlateType.get(currentItem);
		picker = new CraftPicker(null, CraftingType.PLATE_BOILER, (ItemStack item) -> {
        	this.mc.displayGuiScreen(this);
        	
        	if (item != null) {
        		String defID = ItemDefinition.getID(item);
        		ItemDefinition.setID(currentItem, defID);
        		updatePickerButton();
	        	sendPacket();
        	}
        });
	}
	
	private void updatePickerButton() {
		EntityRollingStockDefinition def = ItemDefinition.get(currentItem.copy());
		if (def != null) {
			pickerButton.displayString = GuiText.SELECTOR_PLATE_BOILER.toString(def.name());
		}
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
		
		plateButton = new GuiButton(buttonID++, this.width / 2 - 100, this.height / 4 - 24 + buttonID * 30, GuiText.SELECTOR_PLATE_TYPE.toString(plate));
		this.buttonList.add(plateButton);
		
		pickerButton = new GuiButton(buttonID++, this.width / 2 - 100, this.height / 4 - 24 + buttonID * 30, GuiText.SELECTOR_PLATE_BOILER.toString(""));
		pickerButton.visible = plate == PlateType.BOILER;
		updatePickerButton();
		this.buttonList.add(pickerButton);
	}
	
	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button == gaugeButton) {
			gauge = gauge.next();
			gaugeButton.displayString = GuiText.SELECTOR_GAUGE.toString(gauge);
			sendPacket();
		}
		if (button == plateButton) {
			plate = PlateType.values()[((plate.ordinal() + 1) % (PlateType.values().length))];
			plateButton.displayString = GuiText.SELECTOR_PLATE_TYPE.toString(plate);
			pickerButton.visible = plate == PlateType.BOILER;
			sendPacket();
		}
		if (button == pickerButton) {
			this.mc.displayGuiScreen(picker);
		}
	}
	
	private void sendPacket() {
		ItemGauge.set(currentItem, gauge);
		ItemPlateType.set(currentItem, plate);
    	switch (plate) {
		case BOILER:
			currentItem.setCount(1);
			break;
		case LARGE:
			currentItem.setCount(1);
			break;
		case MEDIUM:
			currentItem.setCount(4);
			break;
		case SMALL:
			currentItem.setCount(8);
			break;
		default:
			break;
    	}
		currentItem.setCount(Math.max(1, (int) Math.floor(currentItem.getCount()/gauge.scale())));
		tile.setCraftItem(currentItem);
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
