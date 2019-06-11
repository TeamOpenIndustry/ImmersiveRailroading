package cam72cam.immersiverailroading.gui;

import java.io.IOException;
import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemTrackExchanger;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.net.ItemTrackExchangerUpdatePacket;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

public class TrackExchangerGui extends GuiScreen {
	private GuiButton trackSelector;
	
	private String track;
	private int slot;
	
	public TrackExchangerGui () {
		EntityPlayer player = Minecraft.getMinecraft().player;
		this.slot = player.inventory.currentItem;
		
		String track = ItemTrackExchanger.get(player.getHeldItemMainhand());
		if (track != null) this.track = track;
		else this.track = DefinitionManager.getTrack("").name;
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

	@Override
	public void initGui() {
		trackSelector = new GuiButton(0, this.width / 2 - 100, this.height / 2 - 10, GuiText.SELECTOR_TRACK.toString(DefinitionManager.getTrack(this.track).name));
		this.buttonList.add(trackSelector);
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button == trackSelector) {
			List<String> defs = DefinitionManager.getTrackIDs();
			int idx = defs.indexOf(this.track);
			idx = (idx + 1) % defs.size();
			this.track = defs.get(idx);
			trackSelector.displayString = GuiText.SELECTOR_TRACK.toString(DefinitionManager.getTrack(this.track).name);
		}
	}
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Enter or ESC
        if (keyCode == 1 || keyCode == 28 || keyCode == 156) {
        	ImmersiveRailroading.net.sendToServer(new ItemTrackExchangerUpdatePacket(this.slot, this.track));
        	
			this.mc.displayGuiScreen(null);
			if (this.mc.currentScreen == null) this.mc.setIngameFocus();
        }
	}
}
