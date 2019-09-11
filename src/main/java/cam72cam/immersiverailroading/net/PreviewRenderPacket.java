package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.render.tile.MultiPreviewRender;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.mod.net.Packet;

/*
 * Movable rolling stock sync packet
 */
public class PreviewRenderPacket extends Packet {

	public PreviewRenderPacket() {
		// Forge Reflection
	}
	public PreviewRenderPacket(TileRailPreview preview) {
		data.setTile("preview", preview);
	}

	@Override
	public void handle() {
		TileRailPreview preview = data.getTile("preview", true);

		if (preview.world != getPlayer().getWorld()) {
			return;
		}

		MultiPreviewRender.add(preview);
	}
}
