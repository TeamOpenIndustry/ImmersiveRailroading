package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.text.TextColor;
import util.Matrix4;

import java.util.*;

public class MarkdownStyledText extends MarkdownElement {
    public final Set<MarkdownTextStyle> styles;

    public static final Map<String, Set<MarkdownTextStyle>> markerStyles;
    public static final List<String> MARKER_PRIORITY = Arrays.asList("***", "++", "**", "~~", "*", "`");

    private static final int CODE_COLOR = 0xFFDDDDDD;
    private static final int BLACK = 0xFF000000;

    static {
        markerStyles = new HashMap<>();
        markerStyles.put("***", EnumSet.of(MarkdownTextStyle.BOLD, MarkdownTextStyle.ITALIC));
        markerStyles.put("++", EnumSet.of(MarkdownTextStyle.UNDERLINE));
        markerStyles.put("**", EnumSet.of(MarkdownTextStyle.BOLD));
        markerStyles.put("~~", EnumSet.of(MarkdownTextStyle.STRIKETHROUGH));
        markerStyles.put("*", EnumSet.of(MarkdownTextStyle.ITALIC));
        markerStyles.put("`", EnumSet.of(MarkdownTextStyle.CODE));
    }

    public MarkdownStyledText(String text) {
        this(text, Collections.emptySet());
    }

    public MarkdownStyledText(String text, Set<MarkdownTextStyle> styles) {
        this.text = text;
        this.styles = styles;
    }

    public boolean hasBold(){
        return this.styles.contains(MarkdownTextStyle.BOLD);
    }

    public boolean hasCode(){
        return this.styles.contains(MarkdownTextStyle.CODE);
    }

    @Override
    public String apply() {
        String str = text;
        for(MarkdownTextStyle style : styles){
            str = style.wrapper.wrap(str);
        }
        return str;
    }

    @Override
    public MarkdownElement[] split(int splitPos) {
        int i = splitPos;
        while (this.text.charAt(i) == ' '){
            i++;
            if(i == this.text.length()){//rest are all space
                return new MarkdownElement[]{
                        new MarkdownStyledText(this.text.substring(0, splitPos), this.styles),
                        new MarkdownStyledText("", this.styles)};
            }
        }
        return new MarkdownElement[]{
                new MarkdownStyledText(this.text.substring(0, splitPos), this.styles),
                new MarkdownStyledText(this.text.substring(i), this.styles)};
    }

    @Override
    public int render(Matrix4 transform, int pageWidth) {
        String str = this.apply();
        if(this.hasCode()){
            Vec3d offset = transform.apply(Vec3d.ZERO);
            GUIHelpers.drawRect((int) offset.x - 2, (int) offset.y - 1,
                    GUIHelpers.getTextWidth(str) + 4, 12, CODE_COLOR);
            GUIHelpers.drawString(str, 0, 0, BLACK, transform);
            transform.translate(GUIHelpers.getTextWidth(str) + 2, 0, 0);
        } else {
            GUIHelpers.drawString(str, 0, 0, BLACK, transform);
            transform.translate(GUIHelpers.getTextWidth(str), 0, 0);
        }
        return 0;
    }

    public enum MarkdownTextStyle {
        BOLD(TextColor.BOLD),
        ITALIC(TextColor.ITALIC),
        STRIKETHROUGH(TextColor.STRIKETHROUGH),
        UNDERLINE(TextColor.UNDERLINE),
        CODE(TextColor.BLACK);

        public final TextColor wrapper;

        MarkdownTextStyle(TextColor wrapper) {
            this.wrapper = wrapper;
        }
    }
}
