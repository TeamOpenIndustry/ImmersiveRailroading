package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.CraftingMachineMode;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MultiblockSelectCraftPacket implements IMessage {
	private ItemStack selected;
	private BlockPos tilePreviewPos;
	private CraftingMachineMode mode;
	
	public MultiblockSelectCraftPacket() {
		// For Reflection
	}

	public MultiblockSelectCraftPacket(BlockPos tilePreviewPos, ItemStack selected, CraftingMachineMode mode) {
		this.tilePreviewPos = tilePreviewPos;
		this.selected = selected;
		this.mode = mode;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.tilePreviewPos = new BlockPos(BufferUtil.readVec3i(buf));
		this.selected = ByteBufUtils.readItemStack(buf);
		this.mode = CraftingMachineMode.values()[buf.readInt()];
	}

	@Override
	public void toBytes(ByteBuf buf) {
		BufferUtil.writeVec3i(buf, tilePreviewPos);
		ByteBufUtils.writeItemStack(buf, selected);
		buf.writeInt(mode.ordinal());
	}
	
	public static class Handler implements IMessageHandler<MultiblockSelectCraftPacket, IMessage> {
		@Override
		public IMessage onMessage(MultiblockSelectCraftPacket message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(MultiblockSelectCraftPacket message, MessageContext ctx) {
			TileMultiblock tile = TileMultiblock.get(ctx.getServerHandler().player.world, message.tilePreviewPos);
			if (tile == null) {
				ImmersiveRailroading.warn("Got invalid craft update packet at %s", message.tilePreviewPos);
				return;
			}
			tile.setCraftItem(message.selected);
			tile.setCraftMode(message.mode);
		}
	}
}
