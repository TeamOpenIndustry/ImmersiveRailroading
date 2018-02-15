package cam72cam.immersiverailroading.sound;

import net.minecraft.util.math.Vec3d;

public interface ISound {
	public void play(float pitch, float vol, Vec3d pos);
	public void stop();
	public void setPosition(Vec3d pos);
	public boolean isPlaying();
	public void terminate();
}
