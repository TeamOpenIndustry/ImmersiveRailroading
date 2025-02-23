package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.immersiverailroading.gui.ManualGui;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.text.TextColor;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownUrl extends MarkdownElement implements IClickable{
    public final Identifier url;
    public Rectangle2D section;
    public boolean inMain = false;

    private static final Pattern URL_PATTERN = Pattern.compile("\\[(?<text>[^]]+?)]\\((?<url>\\S+?)\\)");

    static {
        splittable = true;
    }

    public MarkdownUrl(String text, String url) {
        this(text, new Identifier(url));
    }

    public MarkdownUrl(String text, Identifier url) {
        this.text = text;
        this.url = url;
    }

    public static MarkdownUrl compileSingle(String input){
        Matcher matcher = URL_PATTERN.matcher(input);
        if(matcher.find()) {
            return new MarkdownUrl(matcher.group("text"),
                    matcher.group("url"));
        } else {
            return null;
        }
    }

    public static List<MarkdownElement> splitByUrl(MarkdownStyledText input) {
        List<MarkdownElement> urls = new ArrayList<>();

        Matcher matcher = URL_PATTERN.matcher(input.text);
        int prev = 0;
        while (matcher.find()) {
            urls.add(new MarkdownStyledText(input.text.substring(prev, matcher.start("text") - 1), input.styles));
            urls.add(new MarkdownUrl(matcher.group("text"),
                    matcher.group("url")));
            prev = matcher.end("url") + 1;
        }
        if(prev != input.text.length() -1){
            urls.add(new MarkdownStyledText(input.text.substring(prev), input.styles));
        }
        return urls;
    }

    @Override
    public String apply() {
        return TextColor.BLUE.wrap(TextColor.UNDERLINE.wrap(text));
    }

    @Override
    public MarkdownElement[] split(int splitPos) {
        int i = splitPos;
        while (this.text.charAt(i) == ' '){
            i++;
            if(i == this.text.length()){//rest are all space
                return new MarkdownElement[]{
                        new MarkdownUrl(this.text.substring(0, splitPos), this.url),
                        new MarkdownUrl("", this.url)};
            }
        }
        return new MarkdownElement[]{
                new MarkdownUrl(this.text.substring(0, splitPos), this.url),
                new MarkdownUrl(this.text.substring(i), this.url)};
    }

    @Override
    public void click() {
        //TODO work with browser
        if(!Objects.equals(this.url.getDomain(), "https")){
            ManualGui.currentOpeningManual.changeContent(this.url);
        } else {
            //We should open it in browser
        }
    }
}
