package cam72cam.immersiverailroading.gui;

import java.util.List;

import cam72cam.immersiverailroading.items.nbt.ItemTrackExchangerType;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.net.ItemTrackExchangerUpdatePacket;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.Button;
import cam72cam.mod.gui.IScreen;
import cam72cam.mod.gui.IScreenBuilder;
import cam72cam.mod.util.Hand;

public class TrackExchangerGui implements IScreen {
	private Button trackSelector;
	
	private String track;
	
	public TrackExchangerGui () {
		Player player = MinecraftClient.getPlayer();
		
		String track = ItemTrackExchangerType.get(player.getHeldItem(Hand.PRIMARY));
		if (track != null) this.track = track;
		else this.track = DefinitionManager.getTrack("").name;
	}

	@Override
	public void init(IScreenBuilder screen) {
		trackSelector = new Button(screen, -100, -10, GuiText.SELECTOR_TRACK.toString(DefinitionManager.getTrack(this.track).name)) {
			@Override
			public void onClick(Hand hand) {
				List<String> defs = DefinitionManager.getTrackIDs();
				int idx = defs.indexOf(TrackExchangerGui.this.track);
				idx = (idx + 1) % defs.size();
				TrackExchangerGui.this.track = defs.get(idx);
				trackSelector.setText(GuiText.SELECTOR_TRACK.toString(DefinitionManager.getTrack(TrackExchangerGui.this.track).name));
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
