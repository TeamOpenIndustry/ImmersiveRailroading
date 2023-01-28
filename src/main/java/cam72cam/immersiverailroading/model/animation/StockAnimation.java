package cam72cam.immersiverailroading.model.animation;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition.AnimationDefinition;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition.AnimationDefinition.AnimationMode;
import util.Matrix4;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StockAnimation {
    private final AnimationDefinition def;
    private final Animatrix animatrix;

    private final Map<UUID, Integer> tickStart;
    private final Map<UUID, Integer> tickStop;
    private final boolean looping;

    public StockAnimation(AnimationDefinition def, double internal_model_scale) throws IOException {
        this.def = def;
        this.animatrix = new Animatrix(def.animatrix.getResourceStream(), internal_model_scale);
        tickStart = new HashMap<>();
        tickStop = new HashMap<>();
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
    }


    public float getPercent(EntityRollingStock stock) {
        float value = def.control_group != null ? stock.getControlPosition(def.control_group) : def.readout.getValue(stock);
        value += def.offset;
        if (def.invert) {
            value = 1-value;
        }

        float total_ticks_per_loop = animatrix.frameCount() / def.frames_per_tick;
        if (def.mode == AnimationMode.LOOP_SPEED) {
            total_ticks_per_loop /= value;
        }

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
                    if (!tickStart.containsKey(key)) {
                        tickStart.put(key, stock.getTickCount());
                        tickStop.remove(key);
                    }
                    if (def.mode == AnimationMode.PLAY_REVERSE) {
                        return 1;
                    }
                    // 0 -> 1+
                    tickDelta = stock.getTickCount() - tickStart.get(key);
                } else {
                    // REVERSE
                    if (!tickStop.containsKey(key)) {
                        tickStop.put(key, stock.getTickCount());
                        tickStart.remove(key);
                    }
                    if (def.mode == AnimationMode.PLAY_FORWARD) {
                        return 0;
                    }
                    // 0 -> 1+
                    tickDelta = stock.getTickCount() - tickStop.get(key);
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

        return (stock.getTickCount() % total_ticks_per_loop) / total_ticks_per_loop;
    }

    public Matrix4 getMatrix(EntityRollingStock stock, String group) {
        return animatrix.groups().contains(group) ? animatrix.getMatrix(group, getPercent(stock), looping) : null;
    }
}
