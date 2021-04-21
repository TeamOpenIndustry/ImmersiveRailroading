package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.render.ExpireableList;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.sound.ISound;

import java.util.UUID;

public class Bell {
    private final ModelComponent component;
    private final Identifier soundFile;

    public static Bell get(ComponentProvider provider, Identifier soundFile) {
        return new Bell(provider.parse(ModelComponentType.BELL), soundFile);
    }
    public Bell(ModelComponent component, Identifier soundFile) {
        this.component = component;
        this.soundFile = soundFile;
    }

    ExpireableList<UUID, ISound> sounds = new ExpireableList<UUID, ISound>() {
        @Override
        public void onRemove(UUID key, ISound value) {
            value.terminate();
        }
    };

    public void effects(EntityMoveableRollingStock stock, boolean enabled) {
        if (ConfigSound.soundEnabled && soundFile != null) {
            ISound sound = sounds.get(stock.getUUID());
            if (sound == null) {
                sound = ImmersiveRailroading.newSound(soundFile, true, 150, stock.soundGauge());
                sound.setVolume(0.8f);
                sounds.put(stock.getUUID(), sound);
            }

            if (enabled) {
                sound.setPosition(stock.getPosition());
                sound.setVelocity(stock.getVelocity());

                if (!sound.isPlaying()) {
                    sound.play(stock.getPosition());
                } else {
                    sound.update();
                }
            } else {
                if (sound.isPlaying()) {
                    sound.stop();
                }
            }
        }
    }

    public void removed(EntityMoveableRollingStock stock) {
        ISound sound = sounds.get(stock.getUUID());
        if (sound != null) {
            sound.terminate();
        }
    }

    public void render(ComponentRenderer draw) {
        draw.render(component);
    }
}
