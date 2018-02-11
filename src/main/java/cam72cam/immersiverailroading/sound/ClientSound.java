package cam72cam.immersiverailroading.sound;

import java.net.URL;

import net.minecraft.client.audio.ISound.AttenuationType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import paulscode.sound.SoundSystem;

public class ClientSound implements ISound {
	private final String id;
	private SoundSystem sndSystem;
	private URL resource;
	private boolean repeats;
	private ResourceLocation oggLocation;
	private float attenuationDistance;
	
	/*
	 * TODO figure out snd system reload!
	 */

	public ClientSound(String identifier, SoundSystem sndSystem, ResourceLocation oggLocation, URL resource, boolean repeats, float attenuationDistance) {
		this.id = identifier;
		this.sndSystem = sndSystem;
		this.resource = resource;
		this.repeats = repeats;
		this.oggLocation = oggLocation;
		this.attenuationDistance = attenuationDistance;
		
		this.init();
	}
	
	public void init() {
		sndSystem.newSource(false, id, resource, oggLocation.toString(), repeats, 0f, 0f, 0f, AttenuationType.LINEAR.getTypeInt(), attenuationDistance);
	}
	
	@Override
	public void play(float pitch, float vol, Vec3d pos) {
		stop();
		sndSystem.setPosition(id, (float)pos.x, (float)pos.y, (float)pos.z);
		sndSystem.setPitch(id, pitch);
		sndSystem.setVolume(id, vol);
        sndSystem.play(id);
    }
	
	@Override
	public void setPosition(Vec3d pos) {
		if (isPlaying()) {
			sndSystem.setPosition(id, (float)pos.x, (float)pos.y, (float)pos.z);
		}
	}
	
	@Override
	public boolean isPlaying() {
		return sndSystem.playing(id);
	}

	@Override
	public void stop() {
		if (isPlaying()) {
			sndSystem.stop(id);
		}
	}
}
