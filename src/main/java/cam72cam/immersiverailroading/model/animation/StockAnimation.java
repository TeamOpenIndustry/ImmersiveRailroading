package cam72cam.immersiverailroading.model.animation;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.model.part.PartSound;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition.AnimationDefinition;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition.AnimationDefinition.AnimationMode;
import cam72cam.immersiverailroading.util.MathUtil;
import util.Matrix4;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

public class StockAnimation {
    private final AnimationDefinition def;
    private final Animatrix animatrix;
    private final Map<EntityRollingStock, AnimationState> states = new WeakHashMap<>();
    private final boolean looping;
    private final PartSound sound;

    private static final class AnimationState {
        boolean active;
        int startTick;
        int lastTick;
        float start;
        float target;

        AnimationState(boolean active, int tick, float value) {
            this.active = active;
            startTick = lastTick = tick;
            start = target = value;
        }

        float at(double tick, float duration) {
            if (start == target || !(duration > 0) || !Float.isFinite(duration)) return target;
            double distance = Math.max(0, tick - startTick) / duration;
            if (target > start) return (float) Math.min(target, start + distance);
            return (float) Math.max(target, start - distance);
        }
    }

    public StockAnimation(AnimationDefinition def, double internal_model_scale) throws IOException {
        this.def = def;
        this.animatrix = new Animatrix(def.animatrix.getResourceStream(), internal_model_scale);
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
        value = MathUtil.clamp((value - def.rangeMin) / (def.rangeMax - def.rangeMin), 0, 1);
        value += def.offset;
        return def.invert ? 1 - value : value;
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
                return getPlayPercent(stock, value, partialTicks, total_ticks_per_loop);
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

    private static boolean isPlay(AnimationMode mode) {
        return mode == AnimationMode.PLAY_FORWARD || mode == AnimationMode.PLAY_REVERSE || mode == AnimationMode.PLAY_BOTH;
    }

    private float getPlayPercent(EntityRollingStock stock, float value, float partialTicks, float duration) {
        synchronized (states) {
            int tick = stock.getTickCount();
            boolean active = value >= 0.95;
            boolean both = def.mode == AnimationMode.PLAY_BOTH;
            boolean reverse = def.mode == AnimationMode.PLAY_REVERSE;
            AnimationState state = states.get(stock);
            if (state == null || tick < state.lastTick) {
                // Start at the current endpoint instead of replaying an unseen transition.
                float initial = reverse || active ? 1 : 0;
                state = new AnimationState(active, tick, initial);
                states.put(stock, state);
            } else if (state.active != active) {
                float previous = state.at(tick, duration);
                state.active = active;
                state.startTick = tick;
                if (both) {
                    // Reverse from the current position without jumping to an endpoint.
                    state.start = previous;
                    state.target = active ? 1 : 0;
                } else if (reverse ? !active : active) {
                    state.start = 0;
                    state.target = 1;
                } else {
                    state.start = state.target = reverse ? 1 : 0;
                }
            }
            state.lastTick = tick;
            return state.at((double) tick + Math.max(0, Math.min(1, partialTicks)), duration);
        }
    }

    public Matrix4 getMatrix(EntityRollingStock stock, String group, float partialTicks) {
        return animatrix.groups().contains(group) ? animatrix.getMatrix(group, getPercent(stock, partialTicks), looping) : null;
    }

    public <ENTITY extends EntityMoveableRollingStock> void effects(ENTITY stock) {
        // Observe control changes even for animations without sound or visible groups.
        if (isPlay(def.mode)) {
            getPercent(stock, 0);
        }
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
        synchronized (states) {
            states.remove(stock);
        }
        if (sound != null) {
            sound.removed(stock);
        }
    }
}
