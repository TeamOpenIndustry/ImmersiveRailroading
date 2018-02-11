package cam72cam.immersiverailroading.sound;

import paulscode.sound.SoundSystem;

public class ClientSound implements ISound {
	private final String id;
	private SoundSystem sndSystem;
	
	/*
	 * TODO figure out snd system reload!
	 */

	public ClientSound(String identifier, SoundSystem sndSystem) {
		this.id = identifier;
		this.sndSystem = sndSystem;
	}
	
	public void play(float pitch, float vol, double x, double y, double z) {
		sndSystem.stop(id);
		sndSystem.setPosition(id, (float)x, (float)y, (float)z);
		sndSystem.setPitch(id, pitch);
		sndSystem.setVolume(id, vol);
        sndSystem.play(id);
    }

	@Override
	public void tick() {
	}
}
