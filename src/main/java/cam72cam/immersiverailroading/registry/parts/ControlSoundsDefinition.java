package cam72cam.immersiverailroading.registry.parts;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.sound.ISound;

import java.util.*;

public class ControlSoundsDefinition {
    public final Identifier engage;
    public final Identifier move;
    public final Float movePercent;
    public final Identifier disengage;

    private final Map<UUID, List<ISound>> sounds = new HashMap<>();
    private final Map<UUID, Float> lastMoveSoundValue = new HashMap<>();
    private final Map<UUID, Boolean> wasSoundPressed = new HashMap<>();
    private static final List<ISound> toStop = new ArrayList<>();

    public ControlSoundsDefinition(Identifier engage, Identifier move, Float movePercent, Identifier disengage) {
        this.engage = engage;
        this.move = move;
        this.movePercent = movePercent;
        this.disengage = disengage;
    }

    public ControlSoundsDefinition(DataBlock data) {
        engage = data.getValue("engage").asIdentifier();
        move = data.getValue("move").asIdentifier();
        movePercent = data.getValue("movePercent").asFloat();
        disengage = data.getValue("disengage").asIdentifier();
    }

    public static void cleanupStoppedSounds() {
        if (toStop.isEmpty()) {
            return;
        }
        for (ISound sound : toStop) {
            sound.stop();
        }
        toStop.clear();
    }

    private void createSound(EntityRollingStock stock, Identifier sound, Vec3d pos, boolean repeats) {
        if (sound == null) {
            return;
        }
        ISound snd = stock.createSound(sound, repeats, 10, ConfigSound.SoundCategories::controls);
        snd.setVelocity(stock.getVelocity());
        snd.setVolume(1);
        snd.setPitch(1f);
        snd.play(pos);
        sounds.computeIfAbsent(stock.getUUID(), k -> new ArrayList<>()).add(snd);
    }

    public void effects(EntityRollingStock stock, boolean isPressed, float value, Vec3d pos) {
        if (this.sounds.containsKey(stock.getUUID())) {
            for (ISound snd : new ArrayList<>(this.sounds.get(stock.getUUID()))) {
                if (snd.isPlaying()) {
                    snd.setVelocity(stock.getVelocity());
                    snd.setPosition(pos);
                }
            }
        }

        Boolean wasPressed = wasSoundPressed.getOrDefault(stock.getUUID(), false);
        wasSoundPressed.put(stock.getUUID(), isPressed);

        float lastValue = lastMoveSoundValue.computeIfAbsent(stock.getUUID(), k -> value);

        if (!wasPressed && isPressed) {
            // Start
            createSound(stock, engage, pos, false);
            if (move != null && movePercent == null) {
                // Start move repeat
                createSound(stock, move, pos, true);
            }
        } else if (wasPressed && !isPressed) {
            // Release
            if (this.sounds.containsKey(stock.getUUID())) {
                // Start and Stop may have happend between ticks, we want to wait till the next tick to stop the sound
                toStop.addAll(this.sounds.remove(stock.getUUID()));
            }
            createSound(stock, disengage, pos, false);
        } else if (move != null && movePercent != null) {
            // Move
            if (Math.abs(lastValue - value) > movePercent) {
                createSound(stock, move, pos, false);
                lastMoveSoundValue.put(stock.getUUID(), value);
            }
        }
    }

    public <T extends EntityMoveableRollingStock> void removed(T stock) {
        List<ISound> removed = this.sounds.remove(stock.getUUID());
        if (removed != null) {
            for (ISound sound : removed) {
                sound.stop();
            }
        }
    }
}
