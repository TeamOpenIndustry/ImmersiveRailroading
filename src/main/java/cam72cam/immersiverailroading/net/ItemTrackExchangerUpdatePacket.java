package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.items.nbt.ItemTrackExchanger;
import cam72cam.immersiverailroading.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemTrackExchangerUpdatePacket implements IMessage {
	private int slot;
	private String track;
	
	public ItemTrackExchangerUpdatePacket() {}
	
	@SideOnly(Side.CLIENT)
	public ItemTrackExchangerUpdatePacket(int slot, String track) {
		this.slot = slot;
		this.track = track;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.slot = buf.readInt();
		this.track = BufferUtil.readString(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(slot);
		BufferUtil.writeString(buf, this.track);
	}
	
	public static class Handler implements IMessageHandler<ItemTrackExchangerUpdatePacket, IMessage> {
		@Override
		public IMessage onMessage(ItemTrackExchangerUpdatePacket message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
				ItemStack stack = ctx.getServerHandler().player.inventory.getStackInSlot(message.slot);
				ItemTrackExchanger.set(stack, message.track);
				ctx.getServerHandler().player.inventory.setInventorySlotContents(message.slot, stack);
			});
			return null;
		}
	}
}
