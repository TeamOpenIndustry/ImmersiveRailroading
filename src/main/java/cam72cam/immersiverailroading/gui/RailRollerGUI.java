package cam72cam.immersiverailroading.gui;

import java.io.IOException;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.multiblock.RailRollerMultiblock.RailRollerInstance;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

public class RailRollerGUI extends GuiScreen {
	private GuiButton gaugeButton;
	private Gauge gauge;
	private TileMultiblock te;
	
	public RailRollerGUI(TileMultiblock te) {
		this.te = te;
		if (te != null) {
			gauge = ItemGauge.get(((RailRollerInstance) te.getMultiblock()).getCraftItem());
		} else {
			gauge = Gauge.STANDARD;
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
	}
	
	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button == gaugeButton) {
			gauge = Gauge.values()[((gauge.ordinal() + 1) % (Gauge.values().length))];
			gaugeButton.displayString = GuiText.SELECTOR_GAUGE.toString(gauge);
		}
	}
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Enter or ESC
        if (keyCode == 1 || keyCode == 28 || keyCode == 156) {
        	ItemStack stack = new ItemStack(ImmersiveRailroading.ITEM_RAIL, (int) (16 / gauge.scale()));
        	ItemGauge.set(stack, gauge);
        	((RailRollerInstance) te.getMultiblock()).setCraftItem(stack);

			this.mc.displayGuiScreen(null);
			if (this.mc.currentScreen == null)
				this.mc.setIngameFocus();
        }
	}
}
