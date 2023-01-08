package cam72cam.immersiverailroading.gui.components;

public class GuiUtils {
    public static String fitString(String s, int len) {
        if (s.length() < len) {
            return s;
        }
        len -= 3;
        return s.substring(0, len/2) + "..." + s.substring(s.length() - len/2);
    }
}
