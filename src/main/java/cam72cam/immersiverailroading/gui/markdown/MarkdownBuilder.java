package cam72cam.immersiverailroading.gui.markdown;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import static cam72cam.immersiverailroading.gui.markdown.MarkdownStyledText.*;

/*
 * Beginning of md
 */
public class MarkdownBuilder {
    public static List<List<MarkdownElement>> build(InputStream stream, int screenWidth) throws IOException {
        //Detect line by line
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        List<List<MarkdownElement>> builtString = new ArrayList<>();
        String str;
        while ((str = reader.readLine()) != null){
            str = str.trim();
            //TODO fix header with url
            if(str.startsWith("#")){
                //Title
                builtString.add(parse(str.replaceAll("#", " ")));
            } else if(str.startsWith("!")){
                //Picture
                MarkdownUrl url = MarkdownUrl.compileSingle(str.substring(1));
                if(url != null) {
                    builtString.add(Collections.singletonList(new MarkdownPicture(url.url)));
                }
            } else {
                if(!str.isEmpty()){
                    List<List<MarkdownElement>> elements = MarkdownLineBreaker.breakLine(parse(str), screenWidth);
                    builtString.addAll(elements);
                }else {
                    builtString.add(Collections.singletonList(new MarkdownStyledText("", Collections.emptySet())));
                }
            }
        }
        return builtString;
    }

    public static List<MarkdownElement> parse(String input){
        //Deal with escapes
        input = serializeEscape(input);
        List<Set<MarkdownStyledText.MarkdownTextStyle>> stateMap = new ArrayList<>(input.length());
        Deque<Set<MarkdownStyledText.MarkdownTextStyle>> styleStack = new ArrayDeque<>();
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
                Set<MarkdownStyledText.MarkdownTextStyle> currentStyles = mergeStackStyles(styleStack);
                stateMap.add(currentStyles);
                i++;
            }
        }

        // 阶段3：合并相邻相同状态区间
        StringBuilder builder = new StringBuilder();
        for(char c : input.toCharArray()){
            if(c != '_' && c != '+' && c != '~' && c != '*'){
                builder.append(c);
            }
        }

        //remove temp escape chars
        String result = deserializeEscape(builder.toString());

        return mergeElements(result, stateMap);
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

    private static Set<MarkdownStyledText.MarkdownTextStyle> mergeStackStyles(Deque<Set<MarkdownStyledText.MarkdownTextStyle>> stack) {
        Set<MarkdownStyledText.MarkdownTextStyle> styles = new HashSet<>();
        for (Set<MarkdownStyledText.MarkdownTextStyle> s : stack) styles.addAll(s);
        return styles;
    }

    // 处理标记符号入栈/出栈
    private static void handleMarker(String marker, Deque<Set<MarkdownStyledText.MarkdownTextStyle>> stack) {
        Set<MarkdownStyledText.MarkdownTextStyle> styles = markerStyles.get(marker);
        if (stack.peek() != null && stack.peek().equals(styles)) {
            stack.pop(); // 闭合标记
        } else {
            stack.push(styles); // 开始标记
        }
    }

    private static List<MarkdownElement> mergeElements(String input, List<Set<MarkdownStyledText.MarkdownTextStyle>> stateMap) {
        List<MarkdownElement> elements = new ArrayList<>();
        if (stateMap.isEmpty()) return elements;

        Set<MarkdownStyledText.MarkdownTextStyle> currentStyles = new HashSet<>(stateMap.get(0));
        int start = 0;

        for (int i = 1; i < stateMap.size(); i++) {
            if (!stateMap.get(i).equals(currentStyles)) {
                elements.addAll(createElement(input, start, i, currentStyles));

                currentStyles = new HashSet<>(stateMap.get(i));
                start = i;
            }
        }
        elements.addAll(createElement(input, start, stateMap.size(), currentStyles));

        return elements;
    }

    private static List<MarkdownElement> createElement(String input, int start, int end, Set<MarkdownStyledText.MarkdownTextStyle> styles) {
        return MarkdownUrl.splitByUrl(new MarkdownStyledText(input.substring(start, end), Collections.unmodifiableSet(styles)));
    }
}
