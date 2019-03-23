package cam72cam.immersiverailroading.net;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.CarArtillery;
import cam72cam.immersiverailroading.entity.EntityRidableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/*
 * Movable rolling stock sync packet
 */
public class CarArtilleryUpdatePacket implements IMessage {
	
	private int dimension;
	private int entityID;
	private int order = 0;
	private BlockPos targetPos = BlockPos.ORIGIN;

	public CarArtilleryUpdatePacket() {
		// Reflect constructor
	}

	/** Message artillery to aim at target **/
	@SideOnly(Side.CLIENT)
	public CarArtilleryUpdatePacket(CarArtillery stock, BlockPos target) {
		this.dimension = stock.dimension;
		this.entityID = stock.getEntityId();
		this.order = 2;
		this.targetPos = target;
	}
	
	/** Message artillery to fire at current target**/
	@SideOnly(Side.CLIENT)
	public CarArtilleryUpdatePacket(CarArtillery stock) {
		this.dimension = stock.dimension;
		this.entityID = stock.getEntityId();
		this.order = 1;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(dimension);
		buf.writeInt(entityID);
		buf.writeInt(order);
		buf.writeLong(targetPos.toLong());
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		dimension = buf.readInt();
		entityID = buf.readInt();
		order = buf.readInt();
		targetPos = BlockPos.fromLong(buf.readLong());
	}
	
	public static class Handler implements IMessageHandler<CarArtilleryUpdatePacket, IMessage> {
		@Override
		public IMessage onMessage(CarArtilleryUpdatePacket message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(CarArtilleryUpdatePacket message, MessageContext ctx) {
			List<CarArtillery> matches = ctx.getServerHandler().player.getServerWorld().getEntities(CarArtillery.class, new Predicate<CarArtillery>()
		    {
		        @Override
				public boolean apply(@Nullable CarArtillery entity)
		        {
		            return entity != null && entity.getEntityId() == message.entityID && entity instanceof CarArtillery;
		        }
		    });

			if (matches.isEmpty()) {
				return;
			}

			if (message.order == 1) {
				matches.get(0).attemptFire();
			} else if (message.order == 2) {
				matches.get(0).aim(message.targetPos);
			}
		}
	}
}
