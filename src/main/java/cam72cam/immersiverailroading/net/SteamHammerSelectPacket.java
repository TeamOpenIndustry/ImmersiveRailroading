package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.tile.TileSteamHammer;
import cam72cam.immersiverailroading.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SteamHammerSelectPacket implements IMessage {
	private ItemStack selected;
	private BlockPos tilePreviewPos;
	
	public SteamHammerSelectPacket() {
		// For Reflection
	}

	public SteamHammerSelectPacket(BlockPos tilePreviewPos, ItemStack selected) {
		this.tilePreviewPos = tilePreviewPos;
		this.selected = selected;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.tilePreviewPos = new BlockPos(BufferUtil.readVec3i(buf));
		this.selected = ByteBufUtils.readItemStack(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		BufferUtil.writeVec3i(buf, tilePreviewPos);
		ByteBufUtils.writeItemStack(buf, selected);
	}
	
	public static class Handler implements IMessageHandler<SteamHammerSelectPacket, IMessage> {
		@Override
		public IMessage onMessage(SteamHammerSelectPacket message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(SteamHammerSelectPacket message, MessageContext ctx) {
			TileSteamHammer tile = ((TileSteamHammer)ctx.getServerHandler().player.world.getTileEntity(message.tilePreviewPos));
			tile.setChoosenItem(message.selected);
		}
	}
}
