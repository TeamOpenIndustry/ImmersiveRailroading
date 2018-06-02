package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.sound.ISound;
import cam72cam.immersiverailroading.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SoundPacket implements IMessage {
	private String soundfile;
	private Vec3d pos;
	private Vec3d motion;
	private float volume;
	private float pitch;
	private int distance;
	private Gauge gauge;
	
	public SoundPacket() {
		//Reflection
	}
	public SoundPacket(String soundfile, Vec3d pos, Vec3d motion, float volume, float pitch, int distance, Gauge gauge) {
		this.soundfile = soundfile;
		this.pos = pos;
		this.motion = motion;
		this.volume = volume;
		this.pitch = pitch;
		this.distance = distance;
		this.gauge = gauge;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		soundfile = BufferUtil.readString(buf);
		pos = BufferUtil.readVec3d(buf);
		motion = BufferUtil.readVec3d(buf);
		volume = buf.readFloat();
		pitch = buf.readFloat();
		distance = buf.readInt();
		gauge = Gauge.values()[buf.readInt()];
	}

	@Override
	public void toBytes(ByteBuf buf) {
		BufferUtil.writeString(buf, soundfile);
		BufferUtil.writeVec3d(buf, pos);
		BufferUtil.writeVec3d(buf, motion);
		buf.writeFloat(volume);
		buf.writeFloat(pitch);
		buf.writeInt(distance);
		buf.writeInt(gauge.ordinal());
	}
	
	public static class Handler implements IMessageHandler<SoundPacket, IMessage> {
		@Override
		public IMessage onMessage(SoundPacket message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(SoundPacket message, MessageContext ctx) {
			ISound snd = ImmersiveRailroading.proxy.newSound(new ResourceLocation(message.soundfile), false, message.distance, message.gauge);
			snd.setVelocity(message.motion);
			snd.setVolume(message.volume);
			snd.setPitch(message.pitch);
			snd.disposable();
			snd.play(message.pos);
		}
	}
}
