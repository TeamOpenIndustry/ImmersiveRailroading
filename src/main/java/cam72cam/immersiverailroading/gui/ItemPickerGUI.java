package cam72cam.immersiverailroading.gui;

import java.io.IOException;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public class ItemPickerGUI extends GuiScreen {
	private NonNullList<ItemStack> items;
	public ItemStack choosenItem;
	private Consumer<ItemStack> onExit;
	
	public ItemPickerGUI(NonNullList<ItemStack> items, Consumer<ItemStack> onExit) {
		this.items = items;
		this.onExit = onExit;
	}
	
	public void setItems(NonNullList<ItemStack> items ) {
		this.items = items;
		this.initGui();
	}
	
	public boolean hasOptions() {
		return this.items.size() != 0;
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
		
		int stacksX = this.width/2 / 32;
		
		this.buttonList.clear();
		
		for (int i = 0; i < items.size(); i++) {
			int col = i % stacksX;
			int row = i / stacksX;
			this.buttonList.add(new ItemButton(i, items.get(i), startX + col * 32, startY + row * 32));
		}
	}
	
	@Override
	public void actionPerformed(GuiButton button) throws IOException {
		for (GuiButton itemButton: this.buttonList) {
			if (itemButton == button) {
				this.choosenItem = ((ItemButton)button).stack;
				onExit.accept(this.choosenItem);
				break;
			}
		}
	}
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
			onExit.accept(null);
        }
	}
	
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}
}
