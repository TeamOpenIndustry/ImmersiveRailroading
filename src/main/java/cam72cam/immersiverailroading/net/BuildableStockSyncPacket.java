package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.entity.EntityBuildableRollingStock;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;

import java.util.List;

/*
 * Movable rolling stock sync packet
 */
public class BuildableStockSyncPacket extends Packet {
	@TagField
	private EntityBuildableRollingStock stock;
	@TagField(typeHint = ItemComponentType.class)
	private List<ItemComponentType> items;

	public BuildableStockSyncPacket() { }

	public BuildableStockSyncPacket(EntityBuildableRollingStock stock) {
		this.stock = stock;
		this.items = stock.getItemComponents();
	}

	@Override
	public void handle() {
		if (stock != null) {
			stock.setComponents(items);
		}
	}
}
