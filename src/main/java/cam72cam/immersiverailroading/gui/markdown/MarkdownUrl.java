package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.immersiverailroading.gui.ManualGui;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.text.TextColor;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownUrl extends MarkdownElement implements IClickableElement {
    public final Identifier url;
    public Rectangle2D section;
    public boolean inMain = false;

    //text may be empty; url mustn't be empty
    private static final Pattern URL_PATTERN = Pattern.compile("\\[(?<text>.*?)]\\((?<url>\\S+?)\\)");

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
            return new MarkdownUrl(matcher.group("text"), matcher.group("url"));
        } else {
            return null;
        }
    }

    public static List<MarkdownElement> splitLineByUrl(MarkdownStyledText input) {
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
        ModCore.info(this.url.toString());
        if(this.url.canLoad()){
            ManualGui.currentOpeningManual.pushContent(this.url);
        } else if(this.url.getDomain().equals("https")){
            MinecraftClient.getPlayer().sendMessage(PlayerMessage.url(this.url.toString()));
        } else {
            //What should we do?
        }
    }

    @Override
    public void renderTooltip(int bottomBound) {
        if(this.url.canLoad()){
            String[] path = this.url.getPath().split("/");
            ManualTooltipRenderer.renderTooltip("Open page: " + path[path.length-1], bottomBound);
        } else if(this.url.getDomain().equals("https")){
            ManualTooltipRenderer.renderTooltip("Click to send this website to your dialog!", bottomBound);
        } else {
            //What should we do?
        }
    }
}
