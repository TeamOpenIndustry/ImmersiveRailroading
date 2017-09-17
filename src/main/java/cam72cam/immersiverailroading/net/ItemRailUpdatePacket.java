package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.items.ItemRail;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.library.TrackPositionType;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemRailUpdatePacket implements IMessage {
	private int slot;
	private int length;
	private int quarters;
	private TrackItems type;
	private TrackPositionType posType;
	private ItemStack bedStack;
	private boolean railBedFill;
	private boolean isPreview;
	
	public ItemRailUpdatePacket() {
		// For Reflection
	}
	
	@SideOnly(Side.CLIENT)
	public ItemRailUpdatePacket(int slot, int length, int quarters, TrackItems type, TrackPositionType posType, ItemStack bedStack, boolean railBedFill, boolean isPreview) {
		this.slot = slot;
		this.length = length;
		this.quarters = quarters;
		this.type = type;
		this.posType = posType;
		this.bedStack = bedStack;
		this.railBedFill = railBedFill;
		this.isPreview = isPreview;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.length = buf.readInt();
		this.slot = buf.readInt();
		this.quarters = buf.readInt();
		this.type = TrackItems.fromMeta(buf.readInt());
		this.posType = TrackPositionType.values()[buf.readInt()];
		this.bedStack = ByteBufUtils.readItemStack(buf);
		this.railBedFill = buf.readBoolean();
		this.isPreview = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(length);
		buf.writeInt(slot);
		buf.writeInt(quarters);
		buf.writeInt(type.getMeta());
		buf.writeInt(posType.ordinal());
		ByteBufUtils.writeItemStack(buf, bedStack);
		buf.writeBoolean(railBedFill);
		buf.writeBoolean(isPreview);
	}
	
	public static class Handler implements IMessageHandler<ItemRailUpdatePacket, IMessage> {
		@Override
		public IMessage onMessage(ItemRailUpdatePacket message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(ItemRailUpdatePacket message, MessageContext ctx) {
			ItemStack stack = ctx.getServerHandler().player.inventory.getStackInSlot(message.slot);
			ItemRail.setLength(stack, message.length);
			ItemRail.setQuarters(stack, message.quarters);
			ItemRail.setPosType(stack, message.posType);
			ItemRail.setBed(stack, message.bedStack);
			ItemRail.setBedFill(stack, message.railBedFill);
			ItemRail.setPreview(stack, message.isPreview);
			stack.setItemDamage(message.type.getMeta());
			ctx.getServerHandler().player.inventory.setInventorySlotContents(message.slot, stack);
		}
	}
}
