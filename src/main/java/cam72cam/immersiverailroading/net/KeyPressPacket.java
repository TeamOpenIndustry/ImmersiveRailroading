package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.entity.EntityRidableRollingStock;
import cam72cam.immersiverailroading.library.KeyBindings;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class KeyPressPacket implements IMessage {
	private int keyBindingOrdinal;
	private int sourceEntityID;
	private int targetEntityID;
	
	public KeyPressPacket() {
		// For Reflection
	}
	
	@SideOnly(Side.CLIENT)
	public KeyPressPacket(KeyBindings binding, int sourceEntityID, int targetEntityID) {
		this.keyBindingOrdinal = binding.ordinal();
		this.sourceEntityID = sourceEntityID;
		this.targetEntityID = targetEntityID;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.keyBindingOrdinal = buf.readInt();
		this.sourceEntityID = buf.readInt();
		this.targetEntityID = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(keyBindingOrdinal);
		buf.writeInt(sourceEntityID);
		buf.writeInt(targetEntityID);
	}
	
	public static class Handler implements IMessageHandler<KeyPressPacket, IMessage> {
		@Override
		public IMessage onMessage(KeyPressPacket message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(KeyPressPacket message, MessageContext ctx) {
			Entity source = ctx.getServerHandler().player.getServerWorld().getEntityByID(message.sourceEntityID);
			EntityRidableRollingStock target = (EntityRidableRollingStock) ctx.getServerHandler().player.getServerWorld().getEntityByID(message.targetEntityID);
			if (target == null) {
				return;
			}
			
			target.handleKeyPress(source, KeyBindings.values()[message.keyBindingOrdinal]);
		}
	}
}
