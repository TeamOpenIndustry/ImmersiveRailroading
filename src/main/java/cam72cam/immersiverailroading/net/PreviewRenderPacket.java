package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.render.tile.MultiPreviewRender;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.umc.api.math.Vec3i;
import cam72cam.umc.api.net.Packet;
import cam72cam.umc.api.serialization.TagField;
import cam72cam.umc.api.world.World;

public class PreviewRenderPacket extends Packet {
	@TagField
	private TileRailPreview preview;
	@TagField
	private World world;
	@TagField
	private Vec3i removed;

	public PreviewRenderPacket() { }
	public PreviewRenderPacket(TileRailPreview preview) {
		this.preview = preview;
	}
	public PreviewRenderPacket(World world, Vec3i removed) {
		this.world = world;
		this.removed = removed;
	}

	@Override
	public void handle() {
		if (removed != null) {
			MultiPreviewRender.remove(world, removed);
			return;
		}

		if (preview == null || preview.getWorld() != getPlayer().getWorld()) {
			return;
		}

		MultiPreviewRender.add(preview);
	}
}
