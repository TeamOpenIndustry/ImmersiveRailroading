package cam72cam.immersiverailroading.sound;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.ControllerType;
import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.sound.ISound;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class OneShotSounds implements ISoundDefinition {
    private final ExpireableMap<UUID, Map<ISound, Float>> entitySounds = new ExpireableMap<>((key, value) -> value.keySet().forEach(ISound::stop));

    private final SoundFile soundFile;

    private final ControllerType controllerType;
    private final String name;
    private final String controller;
    private final TriggerCondition condition;
    private final float trigger;
    private final float timer;

    private float oldVal = -1;
    private long lastPlayed = -1;

    private final Random random = new Random();

    public OneShotSounds(DataBlock json, SoundFile soundFile) {
        this.soundFile = soundFile;

        this.name = json.getValue("name").asString();

        String readout = json.getValue("readout").asString();
        controllerType = readout != null ? ControllerType.READOUT : ControllerType.CONTROL_GROUP;
        controller = readout != null ? readout : json.getValue("control_group").asString();

        this.condition = TriggerCondition.valueOf(json.getValue("trigger_type").asString().toUpperCase());
        this.trigger = json.getValue("trigger_value").asFloat();
        this.timer = json.getValue("timer").asFloat();
    }

    @Override
    public void play(EntityMoveableRollingStock stock) {
        Map<ISound, Float> sounds = entitySounds.get(stock.getUUID());
        if (sounds == null) {
            sounds = soundFile.create(stock);
            entitySounds.put(stock.getUUID(), sounds);
        }

        for (ISound sound : sounds.keySet()) {
            if (sound.isPlaying()) {
                sound.setPosition(stock.getPosition());
                sound.setVelocity(stock.getVelocity());
            }
        }

        float newVal = switch (controllerType) {
            case READOUT -> Readouts.valueOf(controller.toUpperCase()).getValue(stock);
            case CONTROL_GROUP -> stock.getControlPosition(controller);
        };

        if (condition.check(trigger, oldVal, newVal)) {
            oldVal = newVal;

            long now = System.nanoTime();
            float deltaSeconds = (now - lastPlayed) / 1_000_000_000f;
            if (lastPlayed != -1 && deltaSeconds <= timer) {
                return;
            }

            ISound toBePlayed = null;

            float totalWeight = 0f;
            for (float weight : sounds.values()) {
                totalWeight += weight;
            }

            if (totalWeight <= 0f) {
                throw new RuntimeException();
            }

            float r = random.nextFloat() * totalWeight;
            float cumulative = 0f;

            for (Map.Entry<ISound, Float> entry : sounds.entrySet()) {
                cumulative += entry.getValue();
                if (r < cumulative) {
                    toBePlayed = entry.getKey();
                }
            }

            if (toBePlayed == null) {
                return;
            }

            if (!toBePlayed.isPlaying()) {
                toBePlayed.play(stock.getPosition());
                lastPlayed = System.nanoTime();
            }
        } else {
            oldVal = newVal;
        }
    }

    @Override
    public void removed(EntityMoveableRollingStock stock) {
        Map<ISound, Float> sounds = entitySounds.get(stock.getUUID());

        if (sounds == null) {
            return;
        }

        for (ISound sound : sounds.keySet()) {
            sound.stop();
        }
        entitySounds.remove(stock.getUUID());
    }

    private enum TriggerCondition {
        VALUE_INCREASE_PAST,
        VALUE_DECREASE_PAST,
        VALUE_MOVES_PAST,
        VALUE_IS_CHANGING_UP,
        VALUE_IS_CHANGING_DOWN,
        VALUE_IS_CHANGING;

        public boolean check(float trigger, float oldVal, float newVal) {
            return switch (this) {
                case VALUE_INCREASE_PAST -> newVal > trigger && oldVal != newVal;
                case VALUE_DECREASE_PAST -> newVal < trigger && oldVal != newVal;
                case VALUE_MOVES_PAST -> (oldVal <= trigger && trigger <= newVal) || (oldVal >= trigger && trigger >= newVal);
                case VALUE_IS_CHANGING_UP -> (newVal - oldVal) >= trigger;
                case VALUE_IS_CHANGING_DOWN -> (oldVal - newVal) >= trigger;
                case VALUE_IS_CHANGING -> (newVal - oldVal) >= trigger || (oldVal - newVal) >= trigger;
            };
        }
    }
}
