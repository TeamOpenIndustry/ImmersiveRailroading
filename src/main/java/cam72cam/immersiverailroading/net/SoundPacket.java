package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.sound.ISound;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.util.Identifier;

public class SoundPacket extends Packet {
	public SoundPacket(String soundfile, Vec3d pos, Vec3d motion, float volume, float pitch, int distance, Gauge gauge) {
		data.setString("file", soundfile);
		data.setVec3d("pos", pos);
		data.setVec3d("motion", motion);
		data.setFloat("volume", volume);
		data.setFloat("pitch", pitch);
		data.setInteger("distance", distance);
		data.setDouble("gauge", gauge.value());
	}

	@Override
	public void handle() {
		String soundfile = data.getString("file");
		Vec3d pos = data.getVec3d("pos");
		Vec3d motion = data.getVec3d("motion");
		float volume = data.getFloat("volume");
		float pitch = data.getFloat("pitch");
		int distance = data.getInteger("distance");
		Gauge gauge = Gauge.from(data.getDouble("gauge"));
		ISound snd = ImmersiveRailroading.proxy.newSound(new Identifier(soundfile), false, distance, gauge);
		snd.setVelocity(motion);
		snd.setVolume(volume);
		snd.setPitch(pitch);
		snd.disposable();
		snd.play(pos);
	}
}
