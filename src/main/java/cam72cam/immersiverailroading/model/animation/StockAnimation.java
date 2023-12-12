package cam72cam.immersiverailroading.model.animation;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.model.part.PartSound;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition.AnimationDefinition;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition.AnimationDefinition.AnimationMode;
import cam72cam.immersiverailroading.render.ExpireableMap;
import util.Matrix4;

import java.io.IOException;
import java.util.UUID;

public class StockAnimation {
    private final AnimationDefinition def;
    private final Animatrix animatrix;
    private final ExpireableMap<UUID, Boolean> active;
    private final ExpireableMap<UUID, Float> tickStart;
    private final ExpireableMap<UUID, Float> tickStop;
    private final boolean looping;
    private final PartSound sound;

    public StockAnimation(AnimationDefinition def, double internal_model_scale) throws IOException {
        this.def = def;
        this.animatrix = new Animatrix(def.animatrix.getResourceStream(), internal_model_scale);
        tickStart = new ExpireableMap<>();
        tickStop = new ExpireableMap<>();
        active = new ExpireableMap<>();
        switch (def.mode) {
            case VALUE:
            case PLAY_FORWARD:
            case PLAY_REVERSE:
            case PLAY_BOTH:
                looping = false;
                break;
            case LOOP:
            case LOOP_SPEED:
            default:
                looping = true;
        }
        this.sound = def.sound != null ? new PartSound(def.sound, true, 20, ConfigSound.SoundCategories::animations) : null;
    }

    public float getValue(EntityRollingStock stock) {
        float value = def.control_group != null ? stock.getControlPosition(def.control_group) : def.readout.getValue(stock);
        value += def.offset;
        if (def.invert) {
            value = 1-value;
        }
        return value;
    }

    public float getPercent(EntityRollingStock stock, float partialTicks) {
        float value = getValue(stock);

        float total_ticks_per_loop = animatrix.frameCount() / def.frames_per_tick;
        if (def.mode == AnimationMode.LOOP_SPEED) {
            total_ticks_per_loop /= value;
        }

        float tickCount = stock.getTickCount() + partialTicks;

        switch (def.mode) {
            case VALUE:
                return value;
            case PLAY_FORWARD:
            case PLAY_REVERSE:
            case PLAY_BOTH:
                UUID key = stock.getUUID();
                float tickDelta;
                if (value >= 0.95) {
                    // FORWARD
                    if (tickStart.get(key) == null) {
                        if (active.get(key) == null) {
                            active.put(key, true);
                            tickStart.put(key, tickCount - total_ticks_per_loop - 1);
                        } else {
                            tickStart.put(key, tickCount);
                        }
                        tickStop.remove(key);
                    }
                    if (def.mode == AnimationMode.PLAY_REVERSE) {
                        return 1;
                    }
                    // 0 -> 1+
                    tickDelta = tickCount - tickStart.get(key);
                } else {
                    // REVERSE
                    if (tickStop.get(key) == null) {
                        if (active.get(key) == null) {
                            active.put(key, true);
                            tickStop.put(key, tickCount - total_ticks_per_loop - 1);
                        } else {
                            tickStop.put(key, tickCount);
                        }
                        tickStart.remove(key);
                    }
                    if (def.mode == AnimationMode.PLAY_FORWARD) {
                        return 0;
                    }
                    // 0 -> 1+
                    tickDelta = tickCount - tickStop.get(key);
                    if (def.mode == AnimationMode.PLAY_BOTH) {
                        // 1 -> 0-
                        tickDelta = total_ticks_per_loop - tickDelta;
                    }
                }
                // Clipped in getMatrix
                return tickDelta / total_ticks_per_loop;
            case LOOP:
                if (value < 0.95) {
                    return 0;
                }
                break;
            case LOOP_SPEED:
                if (value == 0) {
                    return 0;
                }
                break;
        }

        return (tickCount % total_ticks_per_loop) / total_ticks_per_loop;
    }

    public Matrix4 getMatrix(EntityRollingStock stock, String group, float partialTicks) {
        return animatrix.groups().contains(group) ? animatrix.getMatrix(group, getPercent(stock, partialTicks), looping) : null;
    }

    public <ENTITY extends EntityMoveableRollingStock> void effects(ENTITY stock) {
        if (sound != null) {
            float volume = 0;
            float pitch = 1;
            switch (def.mode) {
                case VALUE:
                    volume = getValue(stock);
                    break;
                case PLAY_FORWARD:
                case PLAY_REVERSE:
                case PLAY_BOTH:
                    volume = getPercent(stock, 0) > 0 && getPercent(stock, 0) < 1 ? 1 : 0;
                    break;
                case LOOP:
                    volume = getValue(stock) > 0.95 ? 1 : 0;
                    break;
                case LOOP_SPEED:
                    volume = getValue(stock) > 0 ? 1 : 0;
                    pitch = getValue(stock);
                    break;
            }
            sound.effects(stock, volume, pitch);
        }
    }

    public <ENTITY extends EntityMoveableRollingStock> void removed(ENTITY stock) {
        if (sound != null) {
            sound.removed(stock);
        }
    }
}
