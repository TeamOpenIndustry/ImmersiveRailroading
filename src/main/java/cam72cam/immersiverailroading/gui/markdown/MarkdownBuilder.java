package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.resource.Identifier;

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
        boolean lastLineIsSplit = false;
        boolean isCodeBlock = false;
        while ((str = reader.readLine()) != null){
            str = str.trim();
            //Deal with escapes
            str = serializeEscape(str);

            if(isCodeBlock && !str.startsWith("```")){
                //TODO make it don't ignore starting spaces
                builtString.addAll(MarkdownLineBreaker.breakLine(
                        Collections.singletonList(new MarkdownStyledText(str, EnumSet.of(MarkdownTextStyle.CODE))), screenWidth));
                continue;
            }

            if(lastLineIsSplit && str.isEmpty()){
                builtString.add(Collections.singletonList(new MarkdownSplitLine()));
                lastLineIsSplit = false;
                continue;
            }

            if(str.startsWith("#")){
                //Title
                //CANNOT CONTAIN URL
                builtString.add(Collections.singletonList(new MarkdownHeader(str)));
            } else if(str.startsWith("!")){
                //Picture
                MarkdownUrl url = MarkdownUrl.compileSingle(str.substring(1));
                if(url != null) {
                    builtString.add(Collections.singletonList(new MarkdownPicture(new Identifier(deserializeEscape(url.url.toString())))));
                }
            } else if(str.startsWith("```")){
                //Code block
                isCodeBlock = ! isCodeBlock;
            } else if(str.startsWith("* ") || str.startsWith("- ")){
                //Unsorted list
                List<List<MarkdownElement>> elements = MarkdownLineBreaker.breakLine(parse(str.substring(2)), screenWidth - GUIHelpers.getTextWidth("* "));
                elements.get(0).add(0, new MarkdownStyledText("• ", Collections.emptySet()));
                for(int i = 1; i < elements.size(); i++){
                    elements.get(i).add(0, new MarkdownStyledText("  ", Collections.emptySet()));
                }
                builtString.addAll(elements);
            } else if(MarkdownSplitLine.validate(str)){
                //Check split line
                lastLineIsSplit = true;
            } else {
                if(!str.isEmpty()){
                    //Basic String
                    List<List<MarkdownElement>> elements = MarkdownLineBreaker.breakLine(parse(str), screenWidth);
                    for (List<MarkdownElement> line : elements) {
                        //If a line's first element starts with spacing...
                        if(!line.isEmpty() && !line.get(0).text.isEmpty() && line.get(0).text.charAt(0) == ' '){
                            line.get(0).text = line.get(0).text.substring(1);
                        }
                    }
                    builtString.addAll(elements);
                } else {
                    builtString.add(Collections.singletonList(new MarkdownStyledText("", Collections.emptySet())));
                }
            }
        }
        return builtString;
    }

    public static List<MarkdownElement> parse(String input){
        List<Set<MarkdownStyledText.MarkdownTextStyle>> stateMap = new ArrayList<>(input.length());
        Deque<Set<MarkdownStyledText.MarkdownTextStyle>> styleStack = new ArrayDeque<>();
        int i = 0;

        while (i < input.length()) {
            boolean markerMatched = false;

            // Try to match longest possible marker
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
                Set<MarkdownStyledText.MarkdownTextStyle> currentStyles = mergeStackStyles(styleStack);
                stateMap.add(currentStyles);
                i++;
            }
        }

        StringBuilder builder = new StringBuilder();
        for(char c : input.toCharArray()){
            if(c != '+' && c != '~' && c != '*' && c != '`'){
                builder.append(c);
            }
        }

        //remove temp escape chars
        String result = deserializeEscape(builder.toString());

        return mergeElements(result, stateMap);
    }

    public static String serializeEscape(String str){
        str = str.replaceAll("\\\\\\+", "ā");
        str = str.replaceAll("\\\\`", "á");
        str = str.replaceAll("\\\\\\*", "ǎ");
        str = str.replaceAll("\\\\~", "à");
//        str = str.replaceAll("\\\\\\[", "ō");
//        str = str.replaceAll("\\\\\\]", "ó");
//        str = str.replaceAll("\\\\\\(", "ǒ");
//        str = str.replaceAll("\\\\\\)", "ò");
        return str;
    }

    public static String deserializeEscape(String str){
        str = str.replaceAll("ā", "+");
        str = str.replaceAll("á", "`");
        str = str.replaceAll("ǎ", "*");
        str = str.replaceAll("à", "~");
//        str = str.replaceAll("ō", "[");
//        str = str.replaceAll("ó", "]");
//        str = str.replaceAll("ǒ", "(");
//        str = str.replaceAll("ò", ")");
        return str;
    }

    private static Set<MarkdownStyledText.MarkdownTextStyle> mergeStackStyles(Deque<Set<MarkdownStyledText.MarkdownTextStyle>> stack) {
        Set<MarkdownStyledText.MarkdownTextStyle> styles = new HashSet<>();
        for (Set<MarkdownStyledText.MarkdownTextStyle> s : stack) styles.addAll(s);
        return styles;
    }

    private static void handleMarker(String marker, Deque<Set<MarkdownStyledText.MarkdownTextStyle>> stack) {
        Set<MarkdownStyledText.MarkdownTextStyle> styles = markerStyles.get(marker);
        if (stack.peek() != null && stack.peek().equals(styles)) {
            stack.pop();
        } else {
            stack.push(styles);
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
        return MarkdownUrl.splitLineByUrl(new MarkdownStyledText(input.substring(start, end), Collections.unmodifiableSet(styles)));
    }
}
