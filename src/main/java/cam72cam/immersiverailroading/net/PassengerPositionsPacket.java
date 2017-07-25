package cam72cam.immersiverailroading.net;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PassengerPositionsPacket implements IMessage {
	private UUID stockID;
	private Map<UUID, Vec3d> passengerOffsets;
	
	public PassengerPositionsPacket() {
		//Reflection
	}
	public PassengerPositionsPacket(EntityRollingStock stock) {
		this.stockID = stock.getPersistentID();
		this.passengerOffsets = stock.passengerOffsets;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		stockID = BufferUtil.readUUID(buf);
		
		passengerOffsets = new HashMap<UUID, Vec3d>();
		
		for (int itemCount = buf.readInt(); itemCount > 0; itemCount--) {
			UUID id = BufferUtil.readUUID(buf);
			Vec3d pos = BufferUtil.readVec3d(buf);
			passengerOffsets.put(id, pos);
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		BufferUtil.writeUUID(buf, stockID);
		
		buf.writeInt(passengerOffsets.size());
		for(UUID entry : passengerOffsets.keySet()) {
			BufferUtil.writeUUID(buf, entry);
			BufferUtil.writeVec3d(buf, passengerOffsets.get(entry));
		}
	}
	
	public static class Handler implements IMessageHandler<PassengerPositionsPacket, IMessage> {
		@Override
		public IMessage onMessage(PassengerPositionsPacket message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(PassengerPositionsPacket message, MessageContext ctx) {
			List<EntityRollingStock> matches = Minecraft.getMinecraft().world.getEntities(EntityRollingStock.class, new Predicate<EntityRollingStock>()
		    {
		        public boolean apply(@Nullable EntityRollingStock entity)
		        {
		            return entity != null && entity.getPersistentID().equals(message.stockID);
		        }
		    });
			
			if (matches.size() != 1) {
				ImmersiveRailroading.logger.error("Bad packet for entityID " + message.stockID + " " + matches.size());
				return;
			}
			
			EntityRollingStock entity = (EntityRollingStock) matches.get(0);
			
			entity.passengerOffsets = message.passengerOffsets;
		}
	}
}
