package cam72cam.mod.sound;

import cam72cam.mod.math.Vec3d;

public interface ISound {
	void play(Vec3d pos);
	void stop();
	void update();
	void terminate();
	void setPosition(Vec3d pos);
	void setPitch(float f);
	void setVelocity(Vec3d vel);
	void setVolume(float f);
	boolean isPlaying();
	void updateBaseSoundLevel(float baseSoundMultiplier);
	void reload();
	void disposable();
	boolean isDisposable();
}
