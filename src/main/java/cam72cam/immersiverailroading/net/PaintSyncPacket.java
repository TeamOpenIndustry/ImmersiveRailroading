package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/*
 * Movable rolling stock sync packet
 */
public class PaintSyncPacket implements IMessage {
	
	private int dimension;
	private int entityID;
	private String texture; 

	public PaintSyncPacket() {
		// Reflect constructor
	}

	public PaintSyncPacket(EntityRollingStock mrs) {
		this.dimension = mrs.dimension;
		this.entityID = mrs.getEntityId();
		this.texture = mrs.texture;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(dimension);
		buf.writeInt(entityID);
		buf.writeBoolean(texture != null);
		if (texture != null) {
			BufferUtil.writeString(buf, texture);
		}
	}


	@Override
	public void fromBytes(ByteBuf buf) {
		dimension = buf.readInt();
		entityID = buf.readInt();
		if (buf.readBoolean()) {
			texture = BufferUtil.readString(buf);
		} else {
			texture = null;
		}
	}
	public static class Handler implements IMessageHandler<PaintSyncPacket, IMessage> {
		@Override
		public IMessage onMessage(PaintSyncPacket message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(PaintSyncPacket message, MessageContext ctx) {
			EntityRollingStock entity = (EntityRollingStock) ImmersiveRailroading.proxy.getWorld(message.dimension).getEntityByID(message.entityID);
			if (entity == null) {
				return;
			}

			entity.texture = message.texture;
		}
	}
}
