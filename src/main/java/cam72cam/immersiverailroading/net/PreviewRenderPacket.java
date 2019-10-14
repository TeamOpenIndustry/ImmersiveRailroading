package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.render.tile.MultiPreviewRender;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.world.World;

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
	public PreviewRenderPacket(World world, Vec3i removed) {
		data.setVec3i("removed", removed);
		data.setWorld("world", world);
	}

	@Override
	public void handle() {
		if (data.hasKey("removed")) {
			MultiPreviewRender.remove(data.getWorld("world", true), data.getVec3i("removed"));
		}


		TileRailPreview preview = data.getTile("preview", true);

		if (preview == null || preview.world != getPlayer().getWorld()) {
			return;
		}

		MultiPreviewRender.add(preview);
	}
}
