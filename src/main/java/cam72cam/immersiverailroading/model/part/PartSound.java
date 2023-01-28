package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.mod.sound.ISound;

import java.util.UUID;
import java.util.function.Function;

public class PartSound {
    private final Function<EntityMoveableRollingStock, ISound> startCreate;
    private final Function<EntityMoveableRollingStock, ISound> loopCreate;
    private final Function<EntityMoveableRollingStock, ISound> endCreate;

    public PartSound(Function<EntityMoveableRollingStock, ISound> loopCreate) {
        this(null, loopCreate, null);
    }

    public PartSound(Function<EntityMoveableRollingStock, ISound> startCreate, Function<EntityMoveableRollingStock, ISound> loopCreate) {
        this(startCreate, loopCreate, null);
    }

    public PartSound(Function<EntityMoveableRollingStock, ISound> startCreate, Function<EntityMoveableRollingStock, ISound> loopCreate, Function<EntityMoveableRollingStock, ISound> endCreate) {
        this.startCreate = startCreate;
        this.loopCreate = loopCreate;
        this.endCreate = endCreate;
    }

    enum SoundState {
        STARTING,
        PLAYING,
        STOPPING,
        STOPPED,
    }

    private class Sounds {
        SoundState state;
        final ISound start;
        final ISound loop;
        final ISound end;

        public Sounds(EntityMoveableRollingStock stock) {
            state = SoundState.STOPPED;
            start = startCreate != null ? startCreate.apply(stock) : null;
            loop = loopCreate != null ? loopCreate.apply(stock) : null;
            end = endCreate != null ? endCreate.apply(stock) : null;
        }

        public void terminate() {
            if (start != null) {
                start.terminate();
            }
            if (loop != null) {
                loop.terminate();
            }
            if (end != null) {
                end.terminate();
            }
        }
    }

    private final ExpireableMap<UUID, Sounds> entitySounds = new ExpireableMap<UUID, Sounds>() {
        @Override
        public void onRemove(UUID key, Sounds value) {
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
        if (!ConfigSound.soundEnabled) {
            return;
        }


        Sounds sounds = entitySounds.get(stock.getUUID());
        if(sounds == null) {
            sounds = new Sounds(stock);
            entitySounds.put(stock.getUUID(), sounds);
        }

        ISound toUpdate = null;
        if (volume > 0) {
            // Playing
            switch (sounds.state) {
                case STOPPING:
                    if (sounds.end != null) {
                        sounds.end.stop();
                    }
                case STOPPED:
                    // Start from the beginning
                    sounds.state = SoundState.STARTING;
                    if (sounds.start != null) {
                        toUpdate = sounds.start;
                        toUpdate.play(stock.getPosition());
                        break;
                    }
                case STARTING:
                    // Keep starting until start has finished
                    if (sounds.start != null && sounds.start.isPlaying()) {
                        toUpdate = sounds.start;
                        break;
                    }
                    // Start has finished
                    sounds.state = SoundState.PLAYING;
                    if (sounds.loop != null) {
                        toUpdate = sounds.loop;
                        toUpdate.play(stock.getPosition());
                        break;
                    }
                case PLAYING:
                    // Keep looping until loop is stopped
                    if (sounds.loop != null && sounds.loop.isPlaying()) {
                        toUpdate = sounds.loop;
                        break;
                    }
                    // Loop Finished, wait for shutoff to play outro
            }

            // Update all sounds to current volume
            // Does not actually change until update is called below on the sound that is playing
            if (sounds.start != null) {
                sounds.start.setVolume(volume);
                sounds.start.setPitch(pitch);
            }
            if (sounds.start != null) {
                sounds.start.setVolume(volume);
                sounds.start.setPitch(pitch);
            }
            if (sounds.loop != null) {
                sounds.loop.setVolume(volume);
                sounds.loop.setPitch(pitch);
            }
            if (sounds.end != null) {
                sounds.end.setVolume(volume);
                sounds.end.setPitch(pitch);
            }
        } else {
            // Stopping
            switch (sounds.state) {
                case STARTING:
                case PLAYING:
                    if (sounds.start != null) {
                        sounds.start.stop();
                    }
                    if (sounds.loop != null) {
                        sounds.loop.stop();
                    }
                    // Play the outro
                    sounds.state = SoundState.STOPPING;
                    if (sounds.end != null) {
                        toUpdate = sounds.end;
                        toUpdate.play(stock.getPosition());
                        break;
                    }
                case STOPPING:
                    if (sounds.end != null && sounds.end.isPlaying()) {
                        toUpdate = sounds.end;
                        break;
                    }
                    sounds.state = SoundState.STOPPED;
                case STOPPED:
                    // Nothing to do here
                    break;
            }
        }

        if (toUpdate != null) {
            toUpdate.setPosition(stock.getPosition());
            toUpdate.setVelocity(stock.getVelocity());
            toUpdate.update();
        }
    }

    public void removed(EntityMoveableRollingStock stock) {
        entitySounds.remove(stock.getUUID());
    }

}
