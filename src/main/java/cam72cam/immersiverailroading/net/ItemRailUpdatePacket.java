package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.library.TrackPositionType;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemRailUpdatePacket implements IMessage {
	private int slot;
	private BlockPos tilePreviewPos;
	private RailSettings settings;
	
	public ItemRailUpdatePacket() {
		// For Reflection
	}
	
	@SideOnly(Side.CLIENT)
	public ItemRailUpdatePacket(int slot, RailSettings settings) {
		this.slot = slot;
		this.settings = settings;
	}

	public ItemRailUpdatePacket(BlockPos tilePreviewPos, RailSettings settings) {
		this.tilePreviewPos = tilePreviewPos;
		this.settings = settings;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		if (buf.readBoolean()) {
			this.slot = buf.readInt();
		} else {
			this.tilePreviewPos = BlockPos.fromLong(buf.readLong());
		}
		this.settings = new RailSettings(ByteBufUtils.readTag(buf));
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(tilePreviewPos == null);
		if (tilePreviewPos == null) {
			buf.writeInt(slot);
		} else {
			buf.writeLong(tilePreviewPos.toLong());
		}
		ByteBufUtils.writeTag(buf, settings.toNBT());
	}
	
	public static class Handler implements IMessageHandler<ItemRailUpdatePacket, IMessage> {
		@Override
		public IMessage onMessage(ItemRailUpdatePacket message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(ItemRailUpdatePacket message, MessageContext ctx) {
			ItemStack stack;
			TileRailPreview te = null;
			if (message.tilePreviewPos == null) {
				stack = ctx.getServerHandler().player.inventory.getStackInSlot(message.slot);
			} else {
				te = TileRailPreview.get(ctx.getServerHandler().player.world, message.tilePreviewPos);
				if (te == null) {
					ImmersiveRailroading.warn("Got invalid item rail update packet at %s", message.tilePreviewPos);
					return;
				}
				stack = te.getItem();
			}
			ItemTrackBlueprint.settings(stack, message.settings);
			if (message.tilePreviewPos == null) {
				ctx.getServerHandler().player.inventory.setInventorySlotContents(message.slot, stack);
			} else {
				te.setItem(stack);
			}
		}
	}
}
