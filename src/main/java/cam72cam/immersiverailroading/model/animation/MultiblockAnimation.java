package cam72cam.immersiverailroading.model.animation;

import cam72cam.immersiverailroading.library.AnimationMode;
import cam72cam.immersiverailroading.registry.MultiblockDefinition;
import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import util.Matrix4;

import java.io.IOException;

//Modified from StockAnimation
public class MultiblockAnimation {
    private final Animatrix animatrix;
    private final ExpireableMap<String, Boolean> active;
    private final ExpireableMap<String, Float> tickStart;
    private final ExpireableMap<String, Float> tickStop;
    private final boolean looping;

    public final MultiblockDefinition.MultiblockAnimationDefinition def;

    public MultiblockAnimation(MultiblockDefinition.MultiblockAnimationDefinition def) throws IOException {
        this.def = def;
        this.animatrix = new Animatrix(def.animatrix.getResourceStream(), 1);
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
    }

    public float getValue(TileMultiblock tile) {
        float value = tile.getControlPosition(def.control_group);
        value += def.offset;
        if (def.invert) {
            value = 1-value;
        }
        return value;
    }

    public float getPercent(TileMultiblock tile, float partialTicks) {
        float value = getValue(tile);

        float total_ticks_per_loop = animatrix.frameCount() / def.frames_per_tick;
        if (def.mode == AnimationMode.LOOP_SPEED) {
            total_ticks_per_loop /= value;
        }

        float tickCount = tile.getRenderTicks() + partialTicks;

        switch (def.mode) {
            case VALUE:
                return value;
            case PLAY_FORWARD:
            case PLAY_REVERSE:
            case PLAY_BOTH:
                String key = tile.getPos().toString() + tile.getName();
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

    public Matrix4 getMatrix(TileMultiblock tile, String group, float partialTicks) {
        return animatrix.groups().contains(group) ? animatrix.getMatrix(group, getPercent(tile, partialTicks), looping) : null;
    }
}
