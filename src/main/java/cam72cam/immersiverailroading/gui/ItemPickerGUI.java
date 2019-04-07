package cam72cam.immersiverailroading.gui;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.Lists;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.Vec3i;

public class ItemPickerGUI extends GuiScreen {
	private NonNullList<ItemStack> items;
	private List<Vec3i> buttonCoordList = Lists.<Vec3i>newArrayList();
	public ItemStack choosenItem;
	private Consumer<ItemStack> onExit;
	private GuiScrollBar scrollBar; 

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
			if(button instanceof GuiScrollBar) continue;
			if (scrollBar != null) {
				button.y = buttonCoordList.get(button.id).getY() - (int)Math.floor(scrollBar.getValue()*32);
			}
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
		int startX = this.width / 16;
		int startY = this.height / 8;
		
		int stacksX = this.width * 7/8 / 32;
		int stacksY = this.height * 7/8 / 32;
		
		this.buttonList.clear();
		this.buttonCoordList.clear();
		startX += Math.max(0, (stacksX - items.size())/2) * 32;
		int i;
		for (i = 0; i < items.size(); i++) {
			int col = i % stacksX;
			int row = i / stacksX;
			this.buttonList.add(new ItemButton(i, items.get(i), startX + col * 32, startY + row * 32));
			this.buttonCoordList.add(new Vec3i(startX + col * 32, startY + row * 32, 0));
		}
		int rows = i/stacksX+2;
		if (stacksY < rows) {
			this.scrollBar = new GuiScrollBar(i++, this.width - 30 , 4, 20, this.height-8 , "", 0.0, rows - stacksY, 0.0, null);
			this.buttonList.add(this.scrollBar);
		}
	}
	
	@Override
	public void actionPerformed(GuiButton button) throws IOException {
		for (GuiButton itemButton: this.buttonList) {
			if (itemButton == button && !(button instanceof GuiScrollBar)) {
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
