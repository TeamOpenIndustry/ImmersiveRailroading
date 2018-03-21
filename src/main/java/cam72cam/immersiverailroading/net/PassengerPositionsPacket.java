package cam72cam.immersiverailroading.net;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRidableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRidableRollingStock.StaticPassenger;
import cam72cam.immersiverailroading.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PassengerPositionsPacket implements IMessage {
	private int dimension;
	private UUID stockID;
	private Map<UUID, Vec3d> passengerPositions;
	private List<StaticPassenger> staticPassengers;
	
	public PassengerPositionsPacket() {
		//Reflection
	}
	public PassengerPositionsPacket(EntityRidableRollingStock stock) {
		this.dimension = stock.getEntityWorld().provider.getDimension();
		this.stockID = stock.getPersistentID();
		this.passengerPositions = stock.passengerPositions;
		this.staticPassengers = stock.staticPassengers;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		dimension = buf.readInt();
		stockID = BufferUtil.readUUID(buf);
		passengerPositions = BufferUtil.readPlayerPositions(buf);
		staticPassengers = BufferUtil.readStaticPassengers(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(dimension);
		BufferUtil.writeUUID(buf, stockID);
		BufferUtil.writePlayerPositions(buf, passengerPositions);
		BufferUtil.writeStaticPassengers(buf, staticPassengers);
	}
	
	public static class Handler implements IMessageHandler<PassengerPositionsPacket, IMessage> {
		@Override
		public IMessage onMessage(PassengerPositionsPacket message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(PassengerPositionsPacket message, MessageContext ctx) {
			List<EntityRidableRollingStock> matches = ImmersiveRailroading.proxy.getWorld(message.dimension).getEntities(EntityRidableRollingStock.class, new Predicate<EntityRidableRollingStock>()
		    {
		        @Override
				public boolean apply(@Nullable EntityRidableRollingStock entity)
		        {
		            return entity != null && entity.getPersistentID().equals(message.stockID);
		        }
		    });
			
			if (matches.size() != 1) {
				return;
			}
			
			EntityRidableRollingStock entity = matches.get(0);
			entity.handlePassengerPositions(message.passengerPositions);
			entity.staticPassengers = message.staticPassengers;
		}
	}
}
