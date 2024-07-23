package cam72cam.immersiverailroading.model.animation;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.part.Control;
import org.apache.commons.lang3.tuple.Pair;

public interface IAnimatable {
    float defaultControlPosition(Control<?> control);

    Pair<Boolean, Float> getControlData(String control);

    Pair<Boolean, Float> getControlData(Control<?> control);

    boolean getControlPressed(Control<?> control);

    void setControlPressed(Control<?> control, boolean pressed);

    float getControlPosition(Control<?> control);

    float getControlPosition(String control);

    void setControlPosition(Control<?> control, float val);

    void setControlPosition(String control, float val);

    void setControlPositions(ModelComponentType type, float val);

    default EntityMoveableRollingStock asStock(){
        return (EntityMoveableRollingStock) this;
    }
}
