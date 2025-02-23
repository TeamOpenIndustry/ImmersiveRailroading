package cam72cam.immersiverailroading.gui.markdown;

public abstract class MarkdownElement {
    public String text;
    public static boolean splittable;

    public abstract String apply();

    public abstract MarkdownElement[] split(int splitPos);
}
