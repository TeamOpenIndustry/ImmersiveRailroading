package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
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
	private float rotationYaw;
	private float frontYaw;
	private float rearYaw;
    private float rotationPitch;
    private float prevRotationYaw;
    private float prevRotationPitch;
	private double posX;
	private double posY;
	private double posZ;
	private double prevPosX;
	private double prevPosY;
	private double prevPosZ;
	private double lastTickPosX;
	private double lastTickPosY;
	private double lastTickPosZ;
	private double motionX;
	private double motionY;
	private double motionZ;

	public MRSSyncPacket() {
		// Reflect constructor
	}

	public MRSSyncPacket(EntityMoveableRollingStock mrs) {
		this.dimension = mrs.getEntityWorld().provider.getDimension();
		this.entityID = mrs.getEntityId();
		this.rotationYaw = mrs.rotationYaw;
		this.frontYaw = mrs.frontYaw;
		this.rearYaw = mrs.rearYaw;
		this.rotationPitch = mrs.rotationPitch;
		this.prevRotationYaw = mrs.prevRotationYaw;
		this.prevRotationPitch = mrs.prevRotationPitch;
		this.posX = mrs.posX;
		this.posY = mrs.posY;
		this.posZ = mrs.posZ;
		this.prevPosX = mrs.prevPosX;
		this.prevPosY = mrs.prevPosY;
		this.prevPosZ = mrs.prevPosZ;
		this.lastTickPosX = mrs.lastTickPosX;
		this.lastTickPosY = mrs.lastTickPosY;
		this.lastTickPosZ = mrs.lastTickPosZ;
		this.motionX = mrs.motionX;
		this.motionY = mrs.motionY;
		this.motionZ = mrs.motionZ;
	}

	public void applyTo(EntityMoveableRollingStock mrs) {
		mrs.rotationYaw = this.rotationYaw;
		mrs.frontYaw = this.frontYaw;
		mrs.rearYaw = this.rearYaw;
		mrs.rotationPitch = this.rotationPitch;
		mrs.prevRotationYaw = this.prevRotationYaw;
		mrs.prevRotationPitch = this.prevRotationPitch;
		mrs.posX = this.posX;
		mrs.posY = this.posY;
		mrs.posZ = this.posZ;
		mrs.prevPosX = this.prevPosX;
		mrs.prevPosY = this.prevPosY;
		mrs.prevPosZ = this.prevPosZ;
		mrs.lastTickPosX = this.lastTickPosX;
		mrs.lastTickPosY = this.lastTickPosY;
		mrs.lastTickPosZ = this.lastTickPosZ;
		mrs.motionX = this.motionX;
		mrs.motionY = this.motionY;
		mrs.motionZ = this.motionZ;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		dimension = buf.readInt();
		entityID = buf.readInt();
		rotationYaw = buf.readFloat();
		frontYaw = buf.readFloat();
		rearYaw = buf.readFloat();
		rotationPitch = buf.readFloat();
		prevRotationYaw = buf.readFloat();
		prevRotationPitch = buf.readFloat();
		posX = buf.readDouble();
		posY = buf.readDouble();
		posZ = buf.readDouble();
		prevPosX = buf.readDouble();
		prevPosY = buf.readDouble();
		prevPosZ = buf.readDouble();
		lastTickPosX = buf.readDouble();
		lastTickPosY = buf.readDouble();
		lastTickPosZ = buf.readDouble();
		motionX = buf.readDouble();
		motionY = buf.readDouble();
		motionZ = buf.readDouble();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(dimension);
		buf.writeInt(entityID);
		buf.writeFloat(rotationYaw);
		buf.writeFloat(frontYaw);
		buf.writeFloat(rearYaw);
		buf.writeFloat(rotationPitch);
		buf.writeFloat(prevRotationYaw);
		buf.writeFloat(prevRotationPitch);
		buf.writeDouble(posX);
		buf.writeDouble(posY);
		buf.writeDouble(posZ);
		buf.writeDouble(prevPosX);
		buf.writeDouble(prevPosY);
		buf.writeDouble(prevPosZ);
		buf.writeDouble(lastTickPosX);
		buf.writeDouble(lastTickPosY);
		buf.writeDouble(lastTickPosZ);
		buf.writeDouble(motionX);
		buf.writeDouble(motionY);
		buf.writeDouble(motionZ);
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
