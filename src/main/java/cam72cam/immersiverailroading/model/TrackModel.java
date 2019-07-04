package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.model.obj.OBJModel;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;

public class TrackModel extends OBJModel {
    private final String compare;
    private final double size;
    private final double height;

    public TrackModel(String condition, String resource) throws Exception {
        super(new ResourceLocation(resource), 0);
        this.compare = condition.substring(0, 1);
        this.size = Double.parseDouble(condition.substring(1));
        ArrayList<String> groups = new ArrayList<>();
        for (String group : groups()) {
            if (group.contains("RAIL")) {
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
