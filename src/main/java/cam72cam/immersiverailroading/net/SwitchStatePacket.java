package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SwitchStatePacket implements IMessage {
	
	private int dimension;
	private Vec3i pos;
	private SwitchState state;

	public SwitchStatePacket() {
		// Reflection
	}
	
	public SwitchStatePacket(int dimension, BlockPos pos, SwitchState state) {
		this.pos = pos;
		this.dimension = dimension;
		this.state = state;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.dimension = buf.readInt();
		this.pos = BufferUtil.readVec3i(buf);
		this.state = SwitchState.values()[buf.readInt()];
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(this.dimension);
		BufferUtil.writeVec3i(buf, this.pos);
		buf.writeInt(this.state.ordinal());
	}
	
	public static class Handler implements IMessageHandler<SwitchStatePacket, IMessage> {
		@Override
		public IMessage onMessage(SwitchStatePacket message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(SwitchStatePacket message, MessageContext ctx) {
			TileEntity te = ImmersiveRailroading.proxy.getWorld(message.dimension).getTileEntity(new BlockPos(message.pos));
			if (te instanceof TileRail) {
				((TileRail)te).setSwitchState(message.state);
			}
		}
	}
}
