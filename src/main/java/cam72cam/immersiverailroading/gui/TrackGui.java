package cam72cam.immersiverailroading.gui;

import java.io.IOException;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemRail;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.net.ItemRailUpdatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.config.GuiSlider;
import net.minecraft.item.ItemStack;

public class TrackGui extends GuiScreen {
	private GuiButton typeButton;
	private GuiSlider lengthSlider;
	private GuiSlider quartersSlider;
	private GuiButton doneButton;
	
	private int slot;
	private int length;
	private int quarters;
	private TrackItems type;
	
	public TrackGui() {
		slot = Minecraft.getMinecraft().player.inventory.currentItem;
		ItemStack stack = Minecraft.getMinecraft().player.getHeldItemMainhand();
		length = ItemRail.getLength(stack);
		quarters = ItemRail.getQuarters(stack);
		type = TrackItems.fromMeta(stack.getMetadata());
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
	    this.drawDefaultBackground();
	    super.drawScreen(mouseX, mouseY, partialTicks);
	}
	@Override
	public boolean doesGuiPauseGame() {
	    return false;
	}
	public void initGui() {
		int buttonID = 0;
		
		typeButton = new GuiButton(buttonID++, this.width / 2 - 100, this.height / 2 - 24 + buttonID*30, "Type: " + type.getName());
		this.buttonList.add(typeButton);
		
		this.lengthSlider = new GuiSlider(buttonID++, this.width / 2 - 75, this.height / 2 - 24 + buttonID*30, "Length: ", 1, 100, length, null);
		lengthSlider.showDecimal = false;
		
		this.quartersSlider = new GuiSlider(buttonID++, this.width / 2 - 75, this.height / 2 - 24 + buttonID*30, "Quarters: ", 1, 4, quarters, null);
		quartersSlider.showDecimal = false;
		
	    this.buttonList.add(lengthSlider);
	    this.buttonList.add(quartersSlider);

		quartersSlider.visible = type == TrackItems.SWITCH || type == TrackItems.TURN;
	    
	    doneButton = new GuiButton(buttonID++, this.width / 2 - 100, this.height / 2 - 24 + buttonID*30, "Done");
	    this.buttonList.add(doneButton);
	}
	
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button == typeButton) {
			type = TrackItems.fromMeta((type.ordinal() + 1)%(TrackItems.values().length));
			typeButton.displayString = "Type: " + type.getName();
			quartersSlider.visible = type == TrackItems.SWITCH || type == TrackItems.TURN;
		}
		if (button == doneButton) {
			ImmersiveRailroading.net.sendToServer(new ItemRailUpdatePacket(slot, lengthSlider.getValueInt(), quartersSlider.getValueInt(), type));
			this.mc.displayGuiScreen(null);
	        if (this.mc.currentScreen == null)
	            this.mc.setIngameFocus();
		}
	}
}
