package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.items.ItemTrackExchanger;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.net.ItemTrackExchangerUpdatePacket;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.screen.Button;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.IScreenBuilder;

import static cam72cam.immersiverailroading.gui.ClickListHelper.next;

public class TrackExchangerGui implements IScreen {
	private Button trackSelector;
	
	private String track;
	
	public TrackExchangerGui () {
		Player player = MinecraftClient.getPlayer();
		
		this.track = new ItemTrackExchanger.Data(player.getHeldItem(Player.Hand.PRIMARY)).track;
	}

	@Override
	public void init(IScreenBuilder screen) {
		trackSelector = new Button(screen, -100, -10, GuiText.SELECTOR_TRACK.toString(DefinitionManager.getTrack(this.track).name)) {
			@Override
			public void onClick(Player.Hand hand) {
				track = next(DefinitionManager.getTrackIDs(), track, hand);
				trackSelector.setText(GuiText.SELECTOR_TRACK.toString(DefinitionManager.getTrack(track).name));
			}
		};
	}

	@Override
	public void onEnterKey(IScreenBuilder builder) {
		builder.close();
	}

	@Override
	public void onClose() {
		new ItemTrackExchangerUpdatePacket(this.track).sendToServer();
	}

	@Override
	public void draw(IScreenBuilder builder) {}
}
