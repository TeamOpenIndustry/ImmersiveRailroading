package cam72cam.immersiverailroading.net;

import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityBuildableRollingStock;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/*
 * Movable rolling stock sync packet
 */
public class BuildableStockSyncPacket implements IMessage {
	
	private int dimension;
	private int entityID;
	private boolean isBuilt;
	private List<ItemComponentType> items;

	public BuildableStockSyncPacket() {
		// Reflect constructor
	}

	public BuildableStockSyncPacket(EntityBuildableRollingStock stock) {
		this.dimension = stock.dimension;
		this.entityID = stock.getEntityId();
		this.items = stock.getItemComponents();
		this.isBuilt = stock.isBuilt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(dimension);
		buf.writeInt(entityID);
		buf.writeBoolean(isBuilt);
		BufferUtil.writeItemComponentTypes(buf, items);
	}


	@Override
	public void fromBytes(ByteBuf buf) {
		dimension = buf.readInt();
		entityID = buf.readInt();
		isBuilt = buf.readBoolean();
		items = BufferUtil.readItemComponentTypes(buf);
	}
	public static class Handler implements IMessageHandler<BuildableStockSyncPacket, IMessage> {
		@Override
		public IMessage onMessage(BuildableStockSyncPacket message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(BuildableStockSyncPacket message, MessageContext ctx) {
			EntityBuildableRollingStock entity = (EntityBuildableRollingStock) ImmersiveRailroading.proxy.getWorld(message.dimension).getEntityByID(message.entityID);
			if (entity == null) {
				return;
			}

			entity.setComponents(message.isBuilt, message.items);
		}
	}
}
