package cam72cam.immersiverailroading.library;

public enum TrackModelPart {
    RAIL_LEFT,
    RAIL_RIGHT,
    RAIL_BASE,
    TABLE;

    public boolean is(String str) {
        if(this != RAIL_BASE) {
            return str.contains(this.name());
        }
        return !(RAIL_LEFT.is(str) || RAIL_RIGHT.is(str) || TABLE.is(str));
    }
}
