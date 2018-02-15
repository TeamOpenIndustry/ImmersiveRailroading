package cam72cam.immersiverailroading.sound;

import net.minecraft.util.math.Vec3d;

public interface ISound {
	public void play();
	public void play(float pitch, float vol, Vec3d pos);
	public void stop();
	public void update(Vec3d pos, Vec3d vel);
	public void setPosition(Vec3d pos);
	public void setPitch(float f);
	public void setVelocity(Vec3d vel);
	public void setVolume(float f);
	public boolean isPlaying();
	public void terminate();
	void updateBaseSoundLevel(float baseSoundMultiplier);
}
