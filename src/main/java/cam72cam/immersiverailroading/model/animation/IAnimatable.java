package cam72cam.immersiverailroading.model.animation;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.part.Control;
import org.apache.commons.lang3.tuple.Pair;
import util.Matrix4;

//A hack to expand ModelState's usage
public interface IAnimatable {
    //Add the code below in the implemented classes
//    @TagSync
//    @TagField(value="controlPositions", mapper = ControlPositionMapper.class)
//    protected Map<String, Pair<Boolean, Float>> controlPositions = new HashMap<>();

    Matrix4 getModelMatrix();

    float defaultControlPosition(Control<?> control);

    Pair<Boolean, Float> getControlData(String control);

    Pair<Boolean, Float> getControlData(Control<?> control);

    float getControlPosition(Control<?> control);

    float getControlPosition(String control);

    void setControlPosition(Control<?> control, float val);

    void setControlPosition(String control, float val);

    void setControlPositions(ModelComponentType type, float val);

    default EntityMoveableRollingStock asStock(){
        return (EntityMoveableRollingStock) this;
    }
}
