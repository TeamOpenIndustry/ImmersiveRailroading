package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.sound.Audio;
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
	private float scale;

	public SoundPacket() { }
	public SoundPacket(String soundfile, Vec3d pos, Vec3d motion, float volume, float pitch, int distance, float scale) {
		this.file = soundfile;
		this.pos = pos;
		this.motion = motion;
		this.volume = volume;
		this.pitch = pitch;
		this.distance = distance;
		this.scale = scale;
	}

	@Override
	public void handle() {
		ISound snd = Audio.newSound(new Identifier(file), Identifier::getResourceStream, false, (float) (distance * ConfigSound.soundDistanceScale), scale);
		snd.setVelocity(motion);
		snd.setVolume(volume);
		snd.setPitch(pitch);
		snd.disposable();
		snd.play(pos);
	}
}
