package cam72cam.immersiverailroading.track;

import java.util.List;

public interface IIterableTrack {
    public List<PosStep> getPath(double stepSize);

    public List<BuilderBase> getSubBuilders();

}
