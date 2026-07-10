package cam72cam.immersiverailroading.gui.util;

public record Color(double r, double g, double b, double a) {

    public Color(int r, int g, int b, int a) {
        this(r / 255.0, g / 255.0, b / 255.0, a / 255.0);
    }

    public Color(int rgb) {
        this((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, (rgb >> 24) & 0xFF);
    }

    public static final Color RED = new Color(255, 0, 0, 255);
    public static final Color LIME = new Color(0, 255, 0, 255);
    public static final Color CHARTREUSE = new Color(127, 255, 0, 255);
    public static final Color BLUE = new Color(0, 0, 255, 255);
    public static final Color YELLOW = new Color(255, 255, 0, 255);
    public static final Color GRAY = new Color(128, 128, 128, 255);
    public static final Color WHITE = new Color(255, 255, 255, 255);
    public static final Color MAGENTA = new Color(255, 0, 255, 255);
    public static final Color DARKGREEN = new Color(0, 100, 0, 255);

}