package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SnowRenderUpdatePacket implements IMessage {
	
	private int dimension;
	private Vec3i pos;
	private int snowLayers;

	public SnowRenderUpdatePacket() {
		// Reflection
	}
	
	public SnowRenderUpdatePacket(int dimension, BlockPos pos, int snowLayers) {
		this.pos = pos;
		this.dimension = dimension;
		this.snowLayers = snowLayers;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.dimension = buf.readInt();
		this.pos = BufferUtil.readVec3i(buf);
		this.snowLayers = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(this.dimension);
		BufferUtil.writeVec3i(buf, this.pos);
		buf.writeInt(this.snowLayers);
	}
	
	public static class Handler implements IMessageHandler<SnowRenderUpdatePacket, IMessage> {
		@Override
		public IMessage onMessage(SnowRenderUpdatePacket message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(SnowRenderUpdatePacket message, MessageContext ctx) {
			TileEntity te = ImmersiveRailroading.proxy.getWorld(message.dimension).getTileEntity(new BlockPos(message.pos));
			if (te instanceof TileRailBase) {
				((TileRailBase)te).setSnowLayers(message.snowLayers);
			}
		}
	}
}
