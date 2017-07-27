package cam72cam.immersiverailroading.net;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityLinkableRollingStock;
import cam72cam.immersiverailroading.entity.EntityLinkableRollingStock.CouplerType;
import cam72cam.immersiverailroading.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class LinkStatusPacket implements IMessage {
	private UUID stockID;
	private UUID stockFrontID;
	private UUID stockBackID;
	
	public LinkStatusPacket() {
		//Reflection
	}
	public LinkStatusPacket(EntityLinkableRollingStock stock) {
		stockID = stock.getPersistentID(); 
		stockFrontID = stock.getCoupledUUID(CouplerType.FRONT);
		stockBackID = stock.getCoupledUUID(CouplerType.BACK);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		stockID = BufferUtil.readUUID(buf);
		stockFrontID = BufferUtil.readUUID(buf);
		stockBackID = BufferUtil.readUUID(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		BufferUtil.writeUUID(buf, stockID);
		BufferUtil.writeUUID(buf, stockFrontID);
		BufferUtil.writeUUID(buf, stockBackID);
	}
	
	public static class Handler implements IMessageHandler<LinkStatusPacket, IMessage> {
		@Override
		public IMessage onMessage(LinkStatusPacket message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(LinkStatusPacket message, MessageContext ctx) {
			List<EntityLinkableRollingStock> matches = Minecraft.getMinecraft().world.getEntities(EntityLinkableRollingStock.class, new Predicate<EntityLinkableRollingStock>()
		    {
		        public boolean apply(@Nullable EntityLinkableRollingStock entity)
		        {
		            return entity != null && entity.getPersistentID().equals(message.stockID);
		        }
		    });
			
			if (matches.size() != 1) {
				ImmersiveRailroading.logger.error("Bad packet for entityID " + message.stockID + " " + matches.size());
				return;
			}
			
			EntityLinkableRollingStock stock = matches.get(0);
			ImmersiveRailroading.logger.info("GOT LINK PACKET");
			stock.setCoupledUUID(CouplerType.FRONT, message.stockFrontID);
			stock.setCoupledUUID(CouplerType.BACK, message.stockBackID);
		}
	}
}
