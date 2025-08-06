package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.RenderState;

import static cam72cam.immersiverailroading.gui.markdown.Colors.*;

/**
 * Element class representing a split line
 * @see MarkdownElement
 */
public class MarkdownSplitLine extends MarkdownElement {
    private static final char[] ALLOWED_CHARS_FOR_SPLIT = new char[]{'*', '-', '_'};

    public static boolean validate(String str){
        if(str.length() < 3){
            return false;
        }
        for(char c : ALLOWED_CHARS_FOR_SPLIT){
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
    public int render(RenderState state, int pageWidth) {
        state.translate(0, 10,0);
        Vec3d offset = state.model_view().apply(Vec3d.ZERO);
        GUIHelpers.drawRect((int) offset.x, (int) offset.y, pageWidth, 2,  SPLIT_LINE_COLOR);
        state.translate(0, 2,0);
        state.translate(0, 10,0);
        return 22;
    }
}
