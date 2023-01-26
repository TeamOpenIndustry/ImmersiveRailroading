package cam72cam.immersiverailroading.model.animation;

import cam72cam.mod.resource.Identifier;
import util.Matrix4;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AnimatrixSet {
    private final float min;
    private final float max;
    Map<Float, Animatrix> steps;
    public AnimatrixSet(Map<Float, Identifier> input, double internal_model_scale) throws IOException {
        steps = new HashMap<>();
        for (Map.Entry<Float, Identifier> entry : input.entrySet()) {
            steps.put(entry.getKey(), new Animatrix(entry.getValue().getResourceStream(), internal_model_scale));
        }

        if (steps.isEmpty()) {
            throw new RuntimeException("Invalid Animatrix Configuration (empty)");
        }

        min = (float)steps.keySet().stream().mapToDouble(x -> x).min().getAsDouble();
        max = (float)steps.keySet().stream().mapToDouble(x -> x).max().getAsDouble();
    }


    public Matrix4 getMatrix(String group, float index, float percent, boolean looping) {
        float min = this.min;
        float max = this.max;

        for (Float step : steps.keySet()) {
            if (step < index && step > min) {
                min = step;
            }
            if (step > index && step < max) {
                max = step;
            }
        }

        if (min == max) {
            return steps.get(min).getMatrix(group, percent, looping);
        }

        Matrix4 ms = steps.get(min).getMatrix(group, percent, looping);
        Matrix4 me = steps.get(max).getMatrix(group, percent, looping);
        float lerp = (index - min) / (max - min);
        return ms.slerp(me, lerp);
    }
}
