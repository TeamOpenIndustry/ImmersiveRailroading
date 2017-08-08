package cam72cam.immersiverailroading.net;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class CoupleStatusPacket implements IMessage {
	private int dimension;
	private UUID stockID;
	private UUID stockFrontID;
	private UUID stockBackID;
	
	public CoupleStatusPacket() {
		//Reflection
	}
	public CoupleStatusPacket(EntityCoupleableRollingStock stock) {
		dimension = stock.getEntityWorld().provider.getDimension();
		stockID = stock.getPersistentID(); 
		stockFrontID = stock.getCoupledUUID(CouplerType.FRONT);
		stockBackID = stock.getCoupledUUID(CouplerType.BACK);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		dimension = buf.readInt();
		stockID = BufferUtil.readUUID(buf);
		stockFrontID = BufferUtil.readUUID(buf);
		stockBackID = BufferUtil.readUUID(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(dimension);
		BufferUtil.writeUUID(buf, stockID);
		BufferUtil.writeUUID(buf, stockFrontID);
		BufferUtil.writeUUID(buf, stockBackID);
	}
	
	public static class Handler implements IMessageHandler<CoupleStatusPacket, IMessage> {
		@Override
		public IMessage onMessage(CoupleStatusPacket message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(CoupleStatusPacket message, MessageContext ctx) {
			List<EntityCoupleableRollingStock> matches = ImmersiveRailroading.proxy.getWorld(message.dimension).getEntities(EntityCoupleableRollingStock.class, new Predicate<EntityCoupleableRollingStock>()
		    {
		        public boolean apply(@Nullable EntityCoupleableRollingStock entity)
		        {
		            return entity != null && entity.getPersistentID().equals(message.stockID);
		        }
		    });
			
			if (matches.size() != 1) {
				ImmersiveRailroading.logger.error("Bad packet for entityID " + message.stockID + " " + matches.size());
				return;
			}
			
			EntityCoupleableRollingStock stock = matches.get(0);
			stock.setCoupledUUID(CouplerType.FRONT, message.stockFrontID);
			stock.setCoupledUUID(CouplerType.BACK, message.stockBackID);
		}
	}
}
