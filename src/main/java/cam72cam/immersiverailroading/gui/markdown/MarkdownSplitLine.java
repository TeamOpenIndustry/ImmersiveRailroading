package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.math.Vec3d;
import util.Matrix4;

public class MarkdownSplitLine extends MarkdownElement {

    private static final char[] allowed = new char[]{'*', '-', '_'};
    private static final int SPLIT_LINE_COLOR = 0xFF888888;

    public static boolean validate(String str){
        if(str.length() < 3){
            return false;
        }
        for(char c : allowed){
            label: {
                for (int i = 0; i < str.length(); i++) {
                    if (str.charAt(i) != c) {
                        break label;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public String apply() {
        return "";
    }

    @Override
    public MarkdownElement[] split(int splitPos) {
        return new MarkdownElement[0];
    }

    @Override
    public int render(Matrix4 transform, int pageWidth) {
        Vec3d offset = transform.apply(Vec3d.ZERO);
        GUIHelpers.drawRect((int) offset.x, (int) offset.y, pageWidth, 2,  SPLIT_LINE_COLOR);
        transform.translate(0, 2,0);
        return 2;
    }
}
