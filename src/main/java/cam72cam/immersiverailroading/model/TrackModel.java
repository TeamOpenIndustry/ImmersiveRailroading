package cam72cam.immersiverailroading.model;

import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.resource.Identifier;

import java.util.ArrayList;

public class TrackModel extends OBJModel {
    private final String compare;
    private final double size;
    private final double height;

    public TrackModel(String condition, Identifier resource) throws Exception {
        super(resource, 0, null);
        this.compare = condition.substring(0, 1);
        this.size = Double.parseDouble(condition.substring(1));
        ArrayList<String> groups = new ArrayList<>();
        for (String group : groups()) {
            if (group.contains("RAIL_LEFT") || group.contains("RAIL_RIGHT")) {
                groups.add(group);
            }
        }
        height = maxOfGroup(groups).y;
    }

    public boolean canRender(double gauge) {
        switch (compare) {
            case ">":
                return gauge > size;
            case "<":
                return gauge < size;
            case "=":
                return gauge == size;
            default:
                return true;
        }
    }

    public double getHeight() {
        return height;
    }
}
