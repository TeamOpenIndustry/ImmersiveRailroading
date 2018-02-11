package cam72cam.immersiverailroading.sound;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.client.audio.ISound.AttenuationType;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import paulscode.sound.SoundSystem;

public class IRSoundManager {
	private SoundManager manager;
	private Function<ResourceLocation, URL> getURLForSoundResource;
	private Supplier<SoundSystem> soundSystem;
	private List<ISound> sounds = new ArrayList<ISound>();

	public IRSoundManager(SoundManager manager) {
		this.manager = manager;
		
		initSoundSystem(Pair.of("field_148620_e", "func_148612_a"), Pair.of("sndSystem", "getURLForSoundResource"));
	}
	
	@SafeVarargs
	private final void initSoundSystem(Pair<String, String> ... fieldNames) {
		for (Pair<String, String> fieldName : fieldNames ) {
			try {
				Method getURLField = SoundManager.class.getDeclaredMethod(fieldName.getRight(), ResourceLocation.class);
				getURLField.setAccessible(true);
				this.getURLForSoundResource = (ResourceLocation loc) -> {
					try {
						return (URL) getURLField.invoke(null, loc);
					} catch (Exception e) {
						ImmersiveRailroading.catching(e);
					}
					return null;
				};
			} catch (Exception e) {
				ImmersiveRailroading.catching(e);
				continue;
			}
			
			try {
				Field sndSystemField = SoundManager.class.getDeclaredField(fieldName.getLeft());
		        sndSystemField.setAccessible(true);
		        this.soundSystem = () -> {
		        	try {
						return (paulscode.sound.SoundSystem) sndSystemField.get(manager);
					} catch (Exception e) {
						ImmersiveRailroading.catching(e);
						return null;
					}
		        };
		        return;
			} catch (Exception e) {
				ImmersiveRailroading.catching(e);
				continue;
			}
		}
	}
	
	public ISound createSound(ResourceLocation oggLocation, AttenuationType aType, Vec3d pos) {
        String identifier = MathHelper.getRandomUUID(ThreadLocalRandom.current()).toString();
        boolean repeats = false;
        float f = 1;
        
        SoundSystem sndSystem = this.soundSystem.get();
		if (sndSystem == null) {
			return null;
		}

		sndSystem.newSource(false, identifier, getURLForSoundResource.apply(oggLocation), oggLocation.toString(), repeats, (float)pos.x, (float)pos.y, (float)pos.z, aType.getTypeInt(), f);
		
		ClientSound snd = new ClientSound(identifier, sndSystem);
		this.sounds.add(snd);
        
        return snd;
	}
	
	public void tick() {
		for (ISound snd : sounds) {
			snd.tick();
		}
	}
}
