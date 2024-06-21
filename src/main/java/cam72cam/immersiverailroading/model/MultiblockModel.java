package cam72cam.immersiverailroading.model;

import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.resource.Identifier;

import java.util.HashMap;

public class MultiblockModel extends OBJModel {
    public final HashMap<String, Float> controlGroups;

    public MultiblockModel(Identifier modelLoc, float darken) throws Exception {
        super(modelLoc, darken);
        this.controlGroups = new HashMap<>();
    }
}
