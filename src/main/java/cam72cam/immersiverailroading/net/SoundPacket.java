package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.sound.Audio;
import cam72cam.mod.sound.ISound;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.sound.SoundCategory;

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

	@TagField
	private PacketSoundCategory category;

	public SoundPacket() { }
	public SoundPacket(Identifier soundfile, Vec3d pos, Vec3d motion, float volume, float pitch, int distance, float scale, PacketSoundCategory category) {
		this.file = soundfile.toString();
		this.pos = pos;
		this.motion = motion;
		this.volume = volume;
		this.pitch = pitch;
		this.distance = distance;
		this.scale = scale;
		this.category = category;
	}

	@Override
	public void handle() {
		ISound snd = Audio.newSound(new Identifier(file), SoundCategory.MASTER, false, (float) (distance * ConfigSound.soundDistanceScale), scale);
		snd.setVelocity(motion);
		switch (category) {
			case COUPLE:
				volume *= ConfigSound.SoundCategories.RollingStock.couple();
				break;
			case COLLISION:
				volume *= ConfigSound.SoundCategories.RollingStock.collision();
				break;
			case WHISTLE:
				volume *= ConfigSound.SoundCategories.passenger_whistle();
				break;
		}
		snd.setVolume(volume);
		snd.setPitch(pitch);
		snd.play(pos);
	}

	public enum PacketSoundCategory {
		COUPLE,
		COLLISION,
		WHISTLE,
	}
}
