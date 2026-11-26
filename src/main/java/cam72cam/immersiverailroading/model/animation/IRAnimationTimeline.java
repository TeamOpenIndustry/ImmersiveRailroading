package cam72cam.immersiverailroading.model.animation;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition.AnimationDefinition.AnimationMode;

import java.util.Map;
import java.util.WeakHashMap;

public final class IRAnimationTimeline {
    private final Map<EntityRollingStock, State> states = new WeakHashMap<>();
    private static final class State {
        boolean active;
        int startTick;
        int lastTick;
        float start;
        float target;
        State(boolean active, int tick, float value) {
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
    public static boolean isPlay(AnimationMode mode) {
        return mode == AnimationMode.PLAY_FORWARD || mode == AnimationMode.PLAY_REVERSE || mode == AnimationMode.PLAY_BOTH;
    }
    public synchronized float sample(EntityRollingStock stock, AnimationMode mode, float value, int tick, float partial, int frames, float rate) {
        // Preserve original non-triggered animation modes.
        float duration = frames / rate;
        float time = tick + partial;
        if (!isPlay(mode)) {
            switch (mode) {
                case VALUE: return value;
                case LOOP: if (value < 0.95) return 0; break;
                case LOOP_SPEED: if (value == 0) return 0; duration /= value; break;
                default: break;
            }
            return (time % duration) / duration;
        }
        boolean active = value >= 0.95;
        boolean both = mode == AnimationMode.PLAY_BOTH;
        boolean reverse = mode == AnimationMode.PLAY_REVERSE;
        State s = states.get(stock);
        if (s == null || tick < s.lastTick) {
            // A newly tracked wagon may already be open. Do not replay a historical transition.
            float initial = reverse || active ? 1 : 0;
            s = new State(active, tick, initial);
            states.put(stock, s);
        } else if (s.active != active) {
            float previous = s.at(tick, duration);
            s.active = active;
            s.startTick = tick;
            if (both) {
                s.start = previous;
                s.target = active ? 1 : 0;
            } else if (reverse ? !active : active) {
                s.start = 0;
                s.target = 1;
            } else {
                s.start = s.target = reverse ? 1 : 0;
            }
        }
        s.lastTick = tick;
        return s.at((double) tick + Math.max(0, Math.min(1, partial)), duration);
    }
    public synchronized void removed(EntityRollingStock stock) { states.remove(stock); }
}
