package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.mod.sound.ISound;

import java.util.UUID;
import java.util.function.Function;

public class PartSound {
    private final Function<EntityMoveableRollingStock, ISound> create;

    public PartSound(Function<EntityMoveableRollingStock, ISound> create) {
        this.create = create;
    }

    private final ExpireableMap<UUID, ISound> sounds = new ExpireableMap<UUID, ISound>() {
        @Override
        public void onRemove(UUID key, ISound value) {
            value.terminate();
        }
    };

    public void effects(EntityMoveableRollingStock stock, boolean enabled) {
        effects(stock, enabled ? 1 : 0, 1);
    }

    public void effects(EntityMoveableRollingStock stock, float volume) {
        effects(stock, volume, 1);
    }

    public void effects(EntityMoveableRollingStock stock, float volume, float pitch) {
        if (ConfigSound.soundEnabled && create != null) {
            ISound sound = sounds.get(stock.getUUID());
            if (sound == null) {
                sound = create.apply(stock);
                sounds.put(stock.getUUID(), sound);
            }

            if (volume > 0) {
                sound.setPosition(stock.getPosition());
                sound.setVelocity(stock.getVelocity());
                sound.setVolume(volume);
                sound.setPitch(pitch);

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

}
