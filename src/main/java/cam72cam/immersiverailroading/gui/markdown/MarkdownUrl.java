package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.immersiverailroading.gui.ManualGui;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.text.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cam72cam.immersiverailroading.gui.markdown.Colors.*;

/**
 * Element class representing a url, which is clickable
 * <p>
 * Also parses Markdown format url
 * @see MarkdownClickableElement
 * @see MarkdownElement
 */
public class MarkdownUrl extends MarkdownClickableElement {
    //text may be empty, while url mustn't be empty
    public static final Pattern MARKDOWN_URL_PATTERN = Pattern.compile("\\[(?<text>.*?)]\\((?<url>\\S+?)\\)");

    public final Identifier destination;

    public MarkdownUrl(String text, String destination) {
        this(text, new Identifier(destination));
    }

    public MarkdownUrl(String text, Identifier destination) {
        this.text = text;
        this.destination = destination;
    }

    /**
     * Helper method to parse a String into MarkdownUrl element
     * @param input Raw String needed to be parsed
     * @return The parsed element, or null if it can't be parsed
     */
    public static MarkdownUrl compileSingle(String input){
        Matcher matcher = MARKDOWN_URL_PATTERN.matcher(input);
        if(matcher.find()) {
            return new MarkdownUrl(matcher.group("text"), matcher.group("url"));
        } else {
            return null;
        }
    }

    /**
     * Helper method to split a text element into a list by urls
     * @param input Raw String needed to be parsed
     * @return The parsed element, or null if it can't be parsed
     */
    public static List<MarkdownElement> splitLineByUrl(MarkdownStyledText input) {
        List<MarkdownElement> urls = new ArrayList<>();

        Matcher matcher = MARKDOWN_URL_PATTERN.matcher(input.text);
        int prev = 0;
        while (matcher.find()) {
            urls.add(new MarkdownStyledText(input.text.substring(prev, matcher.start("text") - 1), input.styles));
            urls.add(new MarkdownUrl(matcher.group("text"),
                    matcher.group("url")));
            prev = matcher.end("url") + 1;
        }
        //Last element is not a url, finalize as ordinary text
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
                        new MarkdownUrl(this.text.substring(0, splitPos), this.destination),
                        new MarkdownUrl("", this.destination)};
            }
        }
        return new MarkdownElement[]{
                new MarkdownUrl(this.text.substring(0, splitPos), this.destination),
                new MarkdownUrl(this.text.substring(i), this.destination)};
    }

    @Override
    public int render(RenderState state, int pageWidth) {
        String str = this.apply();
        GUIHelpers.drawString(str, 0, 0, DEFAULT_TEXT_COLOR, state.model_view());
        state.translate(GUIHelpers.getTextWidth(str), 0, 0);
        return 0;
    }

    @Override
    public void click() {
        if(this.destination.canLoad() && this.destination.getPath().endsWith(".md")){
            ManualGui.pushContent(this.destination);
        } else if(this.destination.getDomain().equals("https")){
            MinecraftClient.getPlayer().sendMessage(PlayerMessage.url(this.destination.toString()));
        } else {
            //What should we do?
        }
    }

    //TODO Translation file
    @Override
    public void renderTooltip(int bottomBound) {
        if(this.destination.getDomain().equals("https")){
            renderTooltip("Click to send this website to your dialog!", bottomBound);
        } else {
            //What should we do?
        }
    }
}
