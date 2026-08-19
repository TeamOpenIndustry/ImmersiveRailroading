package cam72cam.immersiverailroading.sound;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.ControllerType;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.sound.ISound;

import java.util.*;

public class LoopedSound implements ISoundDefinition {
    private final ExpireableMap<UUID, Map<ISound, Float>> entitySounds = new ExpireableMap<>((key, value) -> value.keySet().forEach(ISound::stop));

    private final SoundFile soundFile;
    private final ModifierChain modifierChain;

    private final ControllerType controllerType;
    private final String name;
    private final String controller;
    private final LoopCondition condition;
    private final float rangeStart;
    private final float rangeEnd;
    private final PlayState playState;
    private final String emitter;
    private final boolean playWhenOn;

    private Vec3d emitterPos;

    private float oldVal = -1;

    private final Random random = new Random();


    public LoopedSound(DataBlock json, SoundFile soundFile, ModifierChain modifierChain) {
        this.soundFile = soundFile;
        this.modifierChain = modifierChain;

        this.name = json.getValue("name").asString();

        String readout = json.getValue("readout").asString();
        controllerType = readout != null ? ControllerType.READOUT : ControllerType.CONTROL_GROUP;
        controller = readout != null ? readout : json.getValue("control_group").asString();

        this.condition = LoopCondition.valueOf(json.getValue("loop_condition").asString().toUpperCase());

        this.rangeStart = json.getValue("active_range_start").asFloat();
        this.rangeEnd = json.getValue("active_range_end").asFloat();

        this.playState = PlayState.valueOf(json.getValue("play_state").asString("BOTH").toUpperCase());

        this.emitter = json.getValue("emitter").asString();
        this.playWhenOn = json.getValue("only_play_when_on").asBoolean(false);
    }

    @Override
    public void play(EntityMoveableRollingStock stock) {
        if (emitterPos == null) {
            if (emitter != null) {
                StockModel<?, ?> model = stock.getDefinition().getModel();
                Optional<String> name = model.groups().stream().filter(f -> f.contains(emitter)).findFirst();
                name.ifPresent(n -> emitterPos = model.centerOfGroups(Collections.singletonList(n)).rotateYaw(90));
            } else {
                emitterPos = new Vec3d(0, 0, 0);
            }
        }


        Map<ISound, Float> sounds = entitySounds.get(stock.getUUID());
        if (sounds == null) {
            sounds = soundFile.create(stock);
            entitySounds.put(stock.getUUID(), sounds);
        }

        Player player = MinecraftClient.getPlayer();
        switch (playState) {
            case INSIDE -> {
                if (!stock.isPassenger(player)) {
                    for (ISound sound : sounds.keySet()) {
                        sound.stop();
                    }
                    return;
                }
            }
            case OUTSIDE -> {
                if (stock.isPassenger(player)) {
                    for (ISound sound : sounds.keySet()) {
                        sound.stop();
                    }
                    return;
                }
            }
            case BOTH -> {
                // Nothing happens
            }
        }

        if (stock instanceof LocomotiveDiesel loc) {
            if (!loc.isRunning() && playWhenOn) {
                return;
            }
        }

        float newVal = switch (controllerType) {
            case READOUT -> Readouts.valueOf(controller.toUpperCase()).getValue(stock);
            case CONTROL_GROUP -> stock.getControlPosition(controller);
        };

        Vec3d orientedEmitter = emitterPos.rotateYaw(stock.getRotationYaw());

        boolean isAnyPlaying = false;
        for (ISound sound : sounds.keySet()) {
            if (sound.isPlaying()) {
                isAnyPlaying = true;
                sound.setPosition(stock.getPosition().add(orientedEmitter));
                sound.setVelocity(stock.getVelocity());
            }

            if (modifierChain != null) {
                modifierChain.apply(stock, sound);
            }
        }

        if (condition.check(oldVal, newVal, rangeStart, rangeEnd)) {
            oldVal = newVal;

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
                    break;
                }
            }

            if (toBePlayed == null) {
                return;
            }

            if (!isAnyPlaying) {
                toBePlayed.play(stock.getPosition().add(orientedEmitter));
            }
        } else {
            oldVal = newVal;

            for (ISound sound : sounds.keySet()) {
                if (sound.isPlaying()) {
                    sound.stop();
                }
            }
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

    private enum LoopCondition {
        VALUE_IN_RANGE,
        VALUE_IS_CHANGING_UP,
        VALUE_IS_CHANGING_DOWN,
        VALUE_IS_CHANGING;

        public boolean check(float oldVal, float newVal, float rangeStart, float rangeEnd) {
            return switch (this) {
                case VALUE_IN_RANGE -> isInRange(newVal, rangeStart, rangeEnd);
                case VALUE_IS_CHANGING_UP -> oldVal < newVal && isInRange(newVal, rangeStart, rangeEnd);
                case VALUE_IS_CHANGING_DOWN -> oldVal > newVal && isInRange(newVal, rangeStart, rangeEnd);
                case VALUE_IS_CHANGING -> oldVal != newVal && isInRange(newVal, rangeStart, rangeEnd);
            };
        }

        private boolean isInRange(float val, float rangeStart, float rangeEnd) {
            return rangeStart <= val && rangeEnd >= val;
        }
    }
}
