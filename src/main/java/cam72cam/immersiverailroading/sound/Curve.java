package cam72cam.immersiverailroading.sound;

import cam72cam.immersiverailroading.util.DataBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Curve {
    public final List<Point> points;

    public Curve(DataBlock json) {
        this.points = new ArrayList<>();
        json.getBlocks("points").forEach(p -> points.add(new Point(p.getValue("x").asFloat(), p.getValue("y").asFloat())));
        points.sort(Comparator.comparing(p -> p.x));
    }

    public double interpolate(float x) {
        if (x <= points.getFirst().x()) {
            return points.getFirst().y();
        } else if (x >= points.getLast().x()) {
            return points.getLast().y();
        }

        for (int i = 0; i < points.size() - 1; i++) {
            Point a = points.get(i);
            Point b = points.get(i + 1);

            if (x >= a.x() && x <= b.x()) {
                double localT = (x - a.x()) / (b.x() - a.x());
                return a.y() + localT * (b.y() - a.y());
            }
        }

        return points.getLast().y();
    }

    public record Point(float x, float y) {}
}
