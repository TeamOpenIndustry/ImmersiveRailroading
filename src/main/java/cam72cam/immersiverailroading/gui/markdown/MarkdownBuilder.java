package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.text.TextColor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Currently supports following regex
 * Don't support inline url(WIP), you should make a separate line
 */
public class MarkdownBuilder {
    private static Map<String, Set<MarkdownStyle>> markerStyles;
    private static final List<String> MARKER_PRIORITY = Arrays.asList(
            "***", "~~~", "++", "**", "__", "~~", "*", "_", "+"
    );

    static {
        markerStyles = new HashMap<>();
        markerStyles.put("***", EnumSet.of(MarkdownStyle.BOLD, MarkdownStyle.ITALIC));
        markerStyles.put("~~~", EnumSet.of(MarkdownStyle.STRIKETHROUGH));
        markerStyles.put("++", EnumSet.of(MarkdownStyle.UNDERLINE));
        markerStyles.put("**", EnumSet.of(MarkdownStyle.BOLD));
        markerStyles.put("__", EnumSet.of(MarkdownStyle.BOLD));
        markerStyles.put("~~", EnumSet.of(MarkdownStyle.STRIKETHROUGH));
        markerStyles.put("*", EnumSet.of(MarkdownStyle.ITALIC));
        markerStyles.put("_", EnumSet.of(MarkdownStyle.ITALIC));
        markerStyles.put("+", EnumSet.of(MarkdownStyle.UNDERLINE));
    }

    public static List<String> build(InputStream stream) throws IOException {
        //Detect line by line
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        List<String> builtString = new ArrayList<>();
        String str;
        while ((str = reader.readLine()) != null){
            if(str.startsWith("#")){
                //Title
                int count = 1;
                while (str.charAt(count - 1) == '#'){
                    count++;
                }
                String input = str.substring(1).replaceAll("#", "  ");
                List<MarkdownUrl> url = MarkdownUrl.detectUrls(input);
                if(count <= 2){
                    if(!url.isEmpty()){
                        builtString.add(TextColor.BOLD.wrap(input.substring(0, url.get(0).start) +
                                TextColor.BLUE.wrap(TextColor.UNDERLINE.wrap(url.get(0).text))
                                + input.substring(url.get(0).end)));
                        continue;
                    }

                    builtString.add(TextColor.BOLD.wrap(parse(input)));
                } else {
                    if(!url.isEmpty()){
                        builtString.add(input.substring(0, url.get(0).start) +
                                TextColor.BLUE.wrap(TextColor.UNDERLINE.wrap(url.get(0).text))
                                + input.substring(url.get(0).end));
                        continue;
                    }
                    builtString.add(parse(input));
                }
            }
        }
        return builtString;
    }

    public static String parseWithUrl(String input){
        List<MarkdownUrl> urls = MarkdownUrl.detectUrls(input);
        StringBuilder builder = new StringBuilder();
        int lastEnd = 0;
        for(MarkdownUrl url : urls){
            builder.append(parse(input.substring(lastEnd, url.start)));
            lastEnd = url.end;
        }
        return builder.append(parse(input.substring(lastEnd))).toString();
    }

    public static String parse(String input){
        //Deal with escapes
        input = serializeEscape(input);
        List<Set<MarkdownStyle>> stateMap = new ArrayList<>(input.length());
        Deque<Set<MarkdownStyle>> styleStack = new ArrayDeque<>();
        int i = 0;

        while (i < input.length()) {
            boolean markerMatched = false;

            // 尝试匹配最长可能的标记（3 → 2 → 1字符）
            for (String marker : MARKER_PRIORITY) {
                int len = marker.length();
                if (i + len > input.length()) continue;

                String substr = input.substring(i, i + len);
                if (markerStyles.containsKey(substr)) {
                    handleMarker(substr, styleStack);
                    i += len;
                    markerMatched = true;
                    break;
                }
            }

            if (!markerMatched) {
                // 处理普通字符：合并栈中所有样式
                Set<MarkdownStyle> currentStyles = mergeStackStyles(styleStack);
                stateMap.add(currentStyles);
                i++;
            }
        }

        // 阶段2：处理URL
//        detectUrls(input, stateMap, urls);

        // 阶段3：合并相邻相同状态区间
        StringBuilder builder = new StringBuilder();
        for(char c : input.toCharArray()){
            if(c != '_' && c != '+' && c != '~' && c != '*'){
                builder.append(c);
            }
        }
        String result = deserializeEscape(builder.toString());

        List<MarkdownElement> elements = mergeElements(result, stateMap);
        builder = new StringBuilder();
        elements.forEach(builder::append);
        return builder.toString();
    }

    public static String serializeEscape(String str){
        str = str.replaceAll("\\\\\\+", "ā");
        str = str.replaceAll("\\\\\\_", "á");
        str = str.replaceAll("\\\\\\*", "ǎ");
        str = str.replaceAll("\\\\\\~", "à");
        str = str.replaceAll("\\\\\\[", "ō");
        str = str.replaceAll("\\\\\\]", "ó");
        str = str.replaceAll("\\\\\\(", "ǒ");
        str = str.replaceAll("\\\\\\)", "ò");
        return str;
    }

    public static String deserializeEscape(String str){
        str = str.replaceAll("ā", "+");
        str = str.replaceAll("á", "_");
        str = str.replaceAll("ǎ", "*");
        str = str.replaceAll("à", "~");
        str = str.replaceAll("ō", "[");
        str = str.replaceAll("ó", "]");
        str = str.replaceAll("ǒ", "(");
        str = str.replaceAll("ò", ")");
        return str;
    }

    private static Set<MarkdownStyle> mergeStackStyles(Deque<Set<MarkdownStyle>> stack) {
        Set<MarkdownStyle> styles = new HashSet<>();
        for (Set<MarkdownStyle> s : stack) styles.addAll(s);
        return styles;
    }

    // 处理标记符号入栈/出栈
    private static void handleMarker(String marker, Deque<Set<MarkdownStyle>> stack) {
        Set<MarkdownStyle> styles = markerStyles.get(marker);
        if (stack.peek() != null && stack.peek().equals(styles)) {
            stack.pop(); // 闭合标记
        } else {
            stack.push(styles); // 开始标记
        }
    }

    private static List<MarkdownElement> mergeElements(String input, List<Set<MarkdownStyle>> stateMap) {
        List<MarkdownElement> elements = new ArrayList<>();
        if (stateMap.isEmpty()) return elements;

        Set<MarkdownStyle> currentStyles = new HashSet<>(stateMap.get(0));
        int start = 0;

        for (int i = 1; i < stateMap.size(); i++) {
            if (!stateMap.get(i).equals(currentStyles)) {
                elements.add(createElement(input, start, i, currentStyles));

                currentStyles = new HashSet<>(stateMap.get(i));
                start = i;
            }
        }
        elements.add(createElement(input, start, stateMap.size(), currentStyles));

        return elements;
    }

    private static MarkdownElement createElement(String input, int start, int end, Set<MarkdownStyle> styles) {
        return new MarkdownElement(input.substring(start, end), Collections.unmodifiableSet(styles));
    }
}
