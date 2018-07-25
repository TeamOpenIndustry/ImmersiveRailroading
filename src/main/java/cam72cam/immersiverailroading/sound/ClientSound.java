package cam72cam.immersiverailroading.sound;

import java.net.URL;
import java.util.function.Supplier;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.proxy.ClientProxy;
import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound.AttenuationType;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import paulscode.sound.CommandObject;
import paulscode.sound.SoundSystem;

public class ClientSound implements ISound {
	private final static float dopplerScale = 0.05f;
	private String id;
	private Supplier<SoundSystem> sndSystem;
	private URL resource;
	private boolean repeats;
	private ResourceLocation oggLocation;
	private float attenuationDistance;
	private Vec3d currentPos;
	private Vec3d velocity;
	private float currentPitch = 1;
	private float currentVolume = 1;
	private float baseSoundMultiplier;
	private Gauge gauge;
	private boolean disposable = false;

	public ClientSound(Supplier<SoundSystem> soundSystem, ResourceLocation oggLocation, URL resource, float baseSoundMultiplier, boolean repeats, float attenuationDistance, Gauge gauge) {
		this.sndSystem = soundSystem;
		this.resource = resource;
		this.baseSoundMultiplier = baseSoundMultiplier;
		this.repeats = repeats;
		this.oggLocation = oggLocation;
		this.attenuationDistance = attenuationDistance * (float)gauge.scale() * (float)ConfigSound.soundDistanceScale;
		this.gauge = gauge;
	}
	
	public void init() {
        id = MathHelper.getRandomUUID(ThreadLocalRandom.current()).toString();
		sndSystem.get().newSource(false, id, resource, oggLocation.toString(), repeats, 0f, 0f, 0f, AttenuationType.LINEAR.getTypeInt(), attenuationDistance);
	}
	
	@Override
	public void play(Vec3d pos) {
		this.setPosition(pos);
		update();
		
		if (repeats || currentPos == null || Minecraft.getMinecraft().player == null) {
			sndSystem.get().play(id);
		} else if (Minecraft.getMinecraft().player.getPositionVector().distanceTo(currentPos) < this.attenuationDistance * 1.1) {
			sndSystem.get().play(id);
		}
	}

	@Override
	public void stop() {
		if (isPlaying()) {
			sndSystem.get().stop(id);
		}
	}

	@Override
	public void terminate() {
		if (id == null) {
			return;
		}
		sndSystem.get().removeSource(id);
	}
	
	@Override
	public void update() {
		if (id == null) {
			init();
		}
		
		Minecraft.getMinecraft().mcProfiler.startSection("irSound");
		
		SoundSystem snd = sndSystem.get();
		float vol = currentVolume * ClientProxy.getDampeningAmount() * baseSoundMultiplier * (float)Math.sqrt(Math.sqrt(gauge.scale()));
		snd.CommandQueue(new CommandObject(CommandObject.SET_VOLUME, id, vol));
			
		if (currentPos != null) {
			snd.CommandQueue(new CommandObject(CommandObject.SET_POSITION, id, (float)currentPos.x, (float)currentPos.y, (float)currentPos.z));
		}
		
		if (currentPos == null || velocity == null) {
			snd.CommandQueue(new CommandObject(CommandObject.SET_PITCH, id, currentPitch / (float)Math.sqrt(Math.sqrt(gauge.scale()))));
		} else {
			//Doppler shift
			
			EntityPlayerSP player = Minecraft.getMinecraft().player;
			Vec3d ppos = player.getPositionVector();
			Vec3d nextPpos = ppos.addVector(player.motionX, player.motionY, player.motionZ);
			
			Vec3d nextPos = this.currentPos.add(velocity);
			
			double origDist = ppos.subtract(currentPos).lengthVector();
			double newDist = nextPpos.subtract(nextPos).lengthVector();
			
			float appliedPitch = currentPitch;
			if (origDist > newDist) {
				appliedPitch *= 1 + (origDist-newDist) * dopplerScale;
			} else {
				appliedPitch *= 1 - (newDist-origDist) * dopplerScale;
			}
			
			sndSystem.get().setPitch(id, appliedPitch / (float)Math.sqrt(Math.sqrt(gauge.scale())));
			snd.CommandQueue(new CommandObject(CommandObject.SET_PITCH, id, appliedPitch / (float)Math.sqrt(Math.sqrt(gauge.scale()))));
		}

		Minecraft.getMinecraft().mcProfiler.endSection();
		
		snd.interruptCommandThread();
	}
	
	@Override
	public void setPosition(Vec3d pos) {
		this.currentPos = pos;
	}

	@Override
	public void setPitch(float f) {
		this.currentPitch  = f;
	}
	
	@Override
	public void setVelocity(Vec3d vel) {
		this.velocity = vel;
	}
	
	@Override
	public void setVolume(float f) {
		this.currentVolume  = f;
	}
	
	@Override
	public void updateBaseSoundLevel(float baseSoundMultiplier) {
		this.baseSoundMultiplier = baseSoundMultiplier;
	}
	
	@Override
	public boolean isPlaying() {
		if (id == null) {
			return false;
		}
		
		return sndSystem.get().playing(id);
	}

	@Override
	public void reload() {
		// Force re-create sound
		id = null;
	}

	@Override
	public void disposable() {
		disposable = true;
	}

	@Override
	public boolean isDisposable() {
		return disposable;
	}
}
