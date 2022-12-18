package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.mod.sound.ISound;

import java.util.UUID;
import java.util.function.Function;

public class PartSound {
    private final Function<EntityMoveableRollingStock, ISound> loopCreate;
    private final Function<EntityMoveableRollingStock, ISound> startCreate;
    private final Function<EntityMoveableRollingStock, ISound> endCreate;

    public PartSound(Function<EntityMoveableRollingStock, ISound> loopCreate)
    { this(null, loopCreate, null);}
    public PartSound(Function<EntityMoveableRollingStock, ISound> startCreate, Function<EntityMoveableRollingStock, ISound> loopCreate)
    { this(startCreate, loopCreate, null);}
    public PartSound(Function<EntityMoveableRollingStock, ISound> startCreate, Function<EntityMoveableRollingStock, ISound> loopCreate, Function<EntityMoveableRollingStock, ISound> endCreate)
    { this.startCreate = startCreate; this.loopCreate = loopCreate; this.endCreate = endCreate;}

    private boolean startIsPlaying;
    private boolean startHasPlayed;
    private boolean endIsPlaying; //Unused
    private boolean endHasPlayed;

    class Sounds {
        ISound start;
        ISound loop;
        ISound end;
    }

    private final ExpireableMap<UUID, Sounds> sounds = new ExpireableMap<UUID, Sounds>() {
        @Override
        public void onRemove(UUID key, Sounds value) {
            if(value.start != null){ value.start.terminate();}
            if(value.loop != null){ value.loop.terminate();}
            if(value.end != null){ value.end.terminate();}
        }
    };

    public void effects(EntityMoveableRollingStock stock, boolean enabled) {
        effects(stock, enabled ? 1 : 0, 1);
    }

    public void effects(EntityMoveableRollingStock stock, float volume) {
        effects(stock, volume, 1);
    }

    public void effects(EntityMoveableRollingStock stock, float volume, float pitch) {
        if (ConfigSound.soundEnabled && loopCreate != null) {

            Sounds soundTrio = sounds.get(stock.getUUID());

            if(soundTrio == null) {
                soundTrio = new Sounds();
                soundTrio.start = startCreate != null ? startCreate.apply(stock) : null;
                soundTrio.loop = loopCreate != null ? loopCreate.apply(stock) : null;
                soundTrio.end = endCreate != null ? endCreate.apply(stock) : null;
                sounds.put(stock.getUUID(), soundTrio);
            }
            ISound startSound = soundTrio.start;
            ISound loopSound = soundTrio.loop;
            ISound endSound = soundTrio.end; //Unused for now

            if (volume > 0) {
                if(startSound != null) {
                    startSound.setPosition(stock.getPosition());
                    startSound.setVelocity(stock.getVelocity());
                    startSound.setVolume(volume);
                    startSound.setPitch(pitch);

                    if (!startSound.isPlaying() && startIsPlaying) { //Start sound is done playing one take
                        startHasPlayed = true;
                        startIsPlaying = false;
                    } else if(!startSound.isPlaying() && !startHasPlayed) { //If nothing has started yet
                        startSound.play(stock.getPosition());
                        startIsPlaying = true;
                    } else {
                        startSound.update();
                    }
                }
                if(startHasPlayed || startSound == null) { //Once start has played, move on to loop
                    loopSound.setPosition(stock.getPosition());
                    loopSound.setVelocity(stock.getVelocity());
                    loopSound.setVolume(volume);
                    loopSound.setPitch(pitch);

                    if(!loopSound.isPlaying())
                    {
                        loopSound.play(stock.getPosition());
                    } else {
                        loopSound.update();
                    }
                }
            } else {
                if (startSound != null && startSound.isPlaying()) {
                    startSound.stop();
                }
                if (loopSound != null && loopSound.isPlaying()) {
                    loopSound.stop();
                }
                if (endSound != null && endSound.isPlaying()) {
                    endSound.stop();
                }
                startIsPlaying = false;
                startHasPlayed = false;
                endHasPlayed = false; //Probably needs to be somewhere else when endSound has usage
            }
        }
    }

    public void removed(EntityMoveableRollingStock stock) {
        sounds.remove(stock.getUUID());
    }

}
