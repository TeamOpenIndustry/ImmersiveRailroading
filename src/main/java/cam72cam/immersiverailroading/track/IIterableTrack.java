package cam72cam.immersiverailroading.track;

import java.util.List;

public interface IIterableTrack {
    public abstract List<PosStep> getPath(double stepSize);

}
