package cam72cam.immersiverailroading.gui.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownUrl {
    public String text;
    public String url;
    public int start;
    public int end;

    public MarkdownUrl(String text, String url, int start, int end) {
        this.text = text;
        this.url = url;
        this.start = start;
        this.end = end;
    }

    // 阶段2：URL检测（独立于标记处理）
    public static List<MarkdownUrl> detectUrls(String input) {
        List<MarkdownUrl> urls = new ArrayList<>();
        Pattern urlPattern = Pattern.compile(
                "\\[(?<text>[^]]+?)]\\((?<url>\\S+?)\\)"
        );

        Matcher matcher = urlPattern.matcher(input);
        while (matcher.find()) {
            urls.add(new MarkdownUrl(matcher.group("text"),
                    matcher.group("url"),
                    matcher.start("text") - 1,
                    matcher.end("url") + 1));
        }
        return urls;
    }
}
