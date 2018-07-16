package cam72cam.immersiverailroading.net;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.physics.TickPos;
import cam72cam.immersiverailroading.proxy.CommonProxy;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/*
 * Movable rolling stock sync packet
 */
public class MRSSyncPacket implements IMessage {
	
	private int dimension;
	private int entityID;
	private List<TickPos> positions;
	private double serverTPS;

	public MRSSyncPacket() {
		// Reflect constructor
	}

	public MRSSyncPacket(EntityMoveableRollingStock mrs, List<TickPos> positions) {
		this.dimension = mrs.dimension;
		this.entityID = mrs.getEntityId();
		this.positions = positions;
		this.serverTPS = ConfigDebug.serverTickCompensation ? 20 : CommonProxy.getServerTPS(mrs.getEntityWorld(), positions.size());
	}

	public void applyTo(EntityMoveableRollingStock mrs) {
		mrs.handleTickPosPacket(this.positions, this.serverTPS);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(dimension);
		buf.writeInt(entityID);
		buf.writeDouble(serverTPS);
		buf.writeInt(positions.size());
		for (TickPos pos : positions ) {
			pos.write(buf);
		}
	}


	@Override
	public void fromBytes(ByteBuf buf) {
		dimension = buf.readInt();
		entityID = buf.readInt();
		serverTPS = buf.readDouble();
		
		positions = new ArrayList<TickPos>();
		
		for (int numPositions = buf.readInt(); numPositions > 0; numPositions --) {
			TickPos pos = new TickPos();
			pos.read(buf);
			positions.add(pos);
		}
	}
	public static class Handler implements IMessageHandler<MRSSyncPacket, IMessage> {
		@Override
		public IMessage onMessage(MRSSyncPacket message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(MRSSyncPacket message, MessageContext ctx) {
			EntityMoveableRollingStock entity = (EntityMoveableRollingStock) ImmersiveRailroading.proxy.getWorld(message.dimension).getEntityByID(message.entityID);
			if (entity == null) {
				return;
			}

			message.applyTo(entity);
		}
	}
}
