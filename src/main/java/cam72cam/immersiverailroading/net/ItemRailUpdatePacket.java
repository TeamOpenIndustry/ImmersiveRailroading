package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.mod.World;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.TagCompound;
import io.netty.buffer.ByteBuf;
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
		this.settings = new RailSettings(new TagCompound(ByteBufUtils.readTag(buf)));
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(tilePreviewPos == null);
		if (tilePreviewPos == null) {
			buf.writeInt(slot);
		} else {
			buf.writeLong(tilePreviewPos.toLong());
		}
		ByteBufUtils.writeTag(buf, settings.toNBT().internal);
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
				stack = new ItemStack(ctx.getServerHandler().player.inventory.getStackInSlot(message.slot));
			} else {
				te = new World(ctx.getServerHandler().player.world).getTileEntity(new Vec3i(message.tilePreviewPos), TileRailPreview.class);
				if (te == null) {
					ImmersiveRailroading.warn("Got invalid item rail update packet at %s", message.tilePreviewPos);
					return;
				}
				stack = te.getItem();
			}
			ItemTrackBlueprint.settings(stack, message.settings);
			if (message.tilePreviewPos == null) {
				ctx.getServerHandler().player.inventory.setInventorySlotContents(message.slot, stack.internal);
			} else {
				te.setItem(stack);
			}
		}
	}
}
