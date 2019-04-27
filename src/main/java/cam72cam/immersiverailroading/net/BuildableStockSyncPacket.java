package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.entity.EntityBuildableRollingStock;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.mod.net.Packet;

/*
 * Movable rolling stock sync packet
 */
public class BuildableStockSyncPacket extends Packet {
	public BuildableStockSyncPacket(EntityBuildableRollingStock stock) {
		this.data.setEntity("entity", stock.self);
		this.data.setEnumList("items", stock.getItemComponents());
	}

	@Override
	public void handle() {
		EntityBuildableRollingStock stock = data.getEntity("entity", EntityBuildableRollingStock.class);
		if (stock != null) {
			stock.setComponents(data.getEnumList("items", ItemComponentType.class));
		}
	}
}
