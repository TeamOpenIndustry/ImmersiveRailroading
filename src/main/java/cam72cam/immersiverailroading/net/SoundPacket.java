package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.sound.ISound;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.resource.Identifier;

public class SoundPacket extends Packet {
	@TagField
	private String file;
	@TagField
	private Vec3d pos;
	@TagField
	private Vec3d motion;
	@TagField
	private float volume;
	@TagField
	private float pitch;
	@TagField
	private int distance;
	@TagField
	private double gauge;

	public SoundPacket() { }
	public SoundPacket(String soundfile, Vec3d pos, Vec3d motion, float volume, float pitch, int distance, Gauge gauge) {
		this.file = soundfile;
		this.pos = pos;
		this.motion = motion;
		this.volume = volume;
		this.pitch = pitch;
		this.distance = distance;
		this.gauge = gauge.value();
	}

	@Override
	public void handle() {
		Gauge gauge = Gauge.from(this.gauge);
		ISound snd = ImmersiveRailroading.newSound(new Identifier(file), false, distance, gauge);
		snd.setVelocity(motion);
		snd.setVolume(volume);
		snd.setPitch(pitch);
		snd.disposable();
		snd.play(pos);
	}
}
