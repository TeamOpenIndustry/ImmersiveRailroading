package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.text.TextColor;

import java.util.*;

public class MarkdownStyledText extends MarkdownElement {
    public final Set<MarkdownTextStyle> styles;

    public static final Map<String, Set<MarkdownTextStyle>> markerStyles;
    public static final List<String> MARKER_PRIORITY = Arrays.asList("***", "++", "**", "~~", "*", "`");

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
