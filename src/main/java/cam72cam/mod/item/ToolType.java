package cam72cam.mod.item;

public enum ToolType {
    SHOVEL("shovel"),
    ;
    private final String internal;

    ToolType(String internal) {
        this.internal = internal;
    }
    public String toString() {
        return this.internal;
    }
}
