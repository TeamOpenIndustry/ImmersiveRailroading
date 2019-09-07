package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;

/*
 * Movable rolling stock sync packet
 */
public class PreviewRenderPacket extends Packet {
	static {
		Packet.register(PreviewRenderPacket::new, PacketDirection.ServerToClient);
	}
	public PreviewRenderPacket() {
		// Forge Reflection
	}
	public PreviewRenderPacket(TileRailPreview preview) {
		preview.save(data);
		//TODO proper world dim sync
		data.setInteger("dimIDCustom", preview.world.internal.provider.getDimension());
	}

	@Override
	public void handle() {
		/* TODO HACKS
		int dimension = data.getInteger("dimIDCustom");
		TileRailPreview preview = new TileRailPreview();
		preview.load(data);
		ImmersiveRailroading.proxy.addPreview(dimension, preview);
		*/
	}
}
