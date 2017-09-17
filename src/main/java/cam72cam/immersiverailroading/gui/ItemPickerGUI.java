package cam72cam.immersiverailroading.gui;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public class ItemPickerGUI extends GuiScreen {
	private NonNullList<ItemStack> items;
	public ItemStack choosenItem;
	public boolean isActive = false;

	public ItemPickerGUI(NonNullList<ItemStack> items) {
		this.items = items;
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		super.drawScreen(mouseX, mouseY, partialTicks);

		for (GuiButton button: this.buttonList) {
			if (((ItemButton)button).isMouseOver(mouseX, mouseY)) {
				this.renderToolTip(((ItemButton)button).stack, mouseX, mouseY);
			}
		}
	}
	
	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
	
	@Override
	public void initGui() {
		if (width == 0 || height == 0) {
			return;
		}
		int startX = this.width / 4;
		int startY = this.height / 4;
		
		int stacksX = this.width/2 / 16;
		
		for (int i = 0; i < items.size(); i++) {
			int col = i % stacksX;
			int row = i / stacksX;
			this.buttonList.add(new ItemButton(i, items.get(i), startX + col * 16, startY + row * 16));
		}
	}
	
	public void actionPerformed(GuiButton button) throws IOException {
		for (GuiButton itemButton: this.buttonList) {
			if (itemButton == button) {
				this.choosenItem = ((ItemButton)button).stack;
				this.isActive = false;
				break;
			}
		}
	}
	
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}
}
