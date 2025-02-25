package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.resource.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Function;

import static cam72cam.immersiverailroading.gui.markdown.MarkdownStyledText.*;

/*
 * Beginning of markdown
 */
public class MarkdownBuilder {
    public static final HashMap<String, Function<String, List<MarkdownDocument.MarkdownLine>>> SPECIAL_MATCHER = new HashMap<>();

    public static MarkdownDocument build(Identifier id, int screenWidth) throws IOException {
        //Detect line by line
        BufferedReader reader = new BufferedReader(new InputStreamReader(id.getResourceStream()));
        MarkdownDocument document = MarkdownDocument.getPageByID(id);

        //If it's loaded...
        if(!document.isEmpty()){
            if(document.getPageWidth() == screenWidth){
                return document;
            } else {
                document.setPageWidth(screenWidth);
                return MarkdownLineBreaker.breakDocument(document, screenWidth);
            }
        }
        //Otherwise we need to parse it
        String str;
        //Interline state storage
        boolean lastLineIsSplit = false;
        boolean isCodeBlock = false;
        boolean isInTips = false;

        while ((str = reader.readLine()) != null){
            //Deal with custom logic first
            String finalStr = str;
            Optional<Map.Entry<String, Function<String, List<MarkdownDocument.MarkdownLine>>>> optionalFunc =
                    SPECIAL_MATCHER.entrySet().stream().filter(entry -> finalStr.startsWith(entry.getKey())).findFirst();
            if(optionalFunc.isPresent()){
                document.addLines(optionalFunc.get().getValue().apply(str));
                continue;
            }

            //In code block there's no need to consider text style
            if(isCodeBlock && !str.startsWith("```")){
                document.addLine(new MarkdownStyledText(str, EnumSet.of(MarkdownTextStyle.CODE)));
                continue;
            }

            if(isInTips){
                if(!str.startsWith(">")){
                    document.addLine(new MarkdownDocument.MarkdownLine(new MarkdownStyledText("")).isTipEnd(true));
                    isInTips = false;
                } else {
                    if(str.length() > 1){//More than just 1 '>'
                        document.addLine(new MarkdownDocument.MarkdownLine(parse(str.substring(Math.min(2, str.length() - 1)))));
                    } else {//Only one '>'
                        document.addLine(new MarkdownDocument.MarkdownLine(
                                new MarkdownStyledText("")));
                    }
                }
                continue;
            }

            //Deal with escapes
            str = serializeEscape(str);
            str = str.trim();

            if(lastLineIsSplit && str.isEmpty()){
                document.addLine(new MarkdownSplitLine());
                lastLineIsSplit = false;
                continue;
            }

            if(str.startsWith("#")){
                //Title
                //CANNOT CONTAIN URL
                document.addLine(Collections.singletonList(new MarkdownTitle(str)));
            } else if(str.startsWith("!")){
                //Picture
                MarkdownUrl url = MarkdownUrl.compileSingle(str.substring(1));
                if(url != null) {
                    document.addLine(new MarkdownPicture(new Identifier(deserializeEscape(url.url.toString()))));
                }
            } else if(str.startsWith("```")){
                //Code block
                if(isCodeBlock){
                    document.addLine(new MarkdownDocument.MarkdownLine(new MarkdownStyledText(""))
                            .isCodeBlockEnd(true));
                    isCodeBlock = false;
                }else {
                    if(str.length() > 3){
                        //Meaning it has language mark
                        document.addLine(new MarkdownDocument.MarkdownLine(new MarkdownStyledText(str.substring(3)))
                                .isCodeBlockStart(true));
                    } else {
                        //Language is empty
                        document.addLine(new MarkdownDocument.MarkdownLine(new MarkdownStyledText(""))
                                .isCodeBlockStart(true));
                    }
                    isCodeBlock = true;
                }
            } else if(str.startsWith("* ") || str.startsWith("- ")){
                //Unsorted list
                document.addLine(new MarkdownDocument.MarkdownLine(parse(str.substring(2))).isUnorderedList(true));
            } else if(str.startsWith("> ")){
                //Tips
                //Must be more than just 1 '>'
                document.addLine(new MarkdownDocument.MarkdownLine(new MarkdownStyledText("")).isTipStart(true));
                document.addLine(new MarkdownDocument.MarkdownLine(parse(str.substring(Math.min(2, str.length() - 1)))));
                isInTips = true;
            } else if(MarkdownSplitLine.validate(str)){
                //Check split line
                lastLineIsSplit = true;
            } else {
                if(!str.isEmpty()){
                    //Basic String
                    document.addLine(parse(str));
                } else {
                    document.addLine(new MarkdownStyledText(""));
                }
            }
        }
        //Finalize
        if(isInTips){
            document.addLine(new MarkdownDocument.MarkdownLine(new MarkdownStyledText("")).isTipEnd(true));
        }
        if(isCodeBlock){
            document.addLine(new MarkdownDocument.MarkdownLine(new MarkdownStyledText("")).isCodeBlockEnd(true));
        }
        document.setPageWidth(screenWidth);
        return MarkdownLineBreaker.breakDocument(document, screenWidth);
    }

    public static void register(String prefix, Function<String, List<MarkdownDocument.MarkdownLine>> func){
        SPECIAL_MATCHER.put(prefix, func);
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
                //Pure text, add to stateMap
                Set<MarkdownStyledText.MarkdownTextStyle> currentStyles = mergeStackStyles(styleStack);
                stateMap.add(currentStyles);
                i++;
            }
        }

        //Build a string that matches stateMap
        StringBuilder builder = new StringBuilder();
        for(char c : input.toCharArray()){
            if(c != '+' && c != '~' && c != '*' && c != '`'){
                builder.append(c);
            }
        }
        //remove temporary escape chars
        String result = deserializeEscape(builder.toString());

        return mergeElements(result, stateMap);
    }

    //Turn all styles in the queue into one set
    private static Set<MarkdownStyledText.MarkdownTextStyle> mergeStackStyles(Deque<Set<MarkdownStyledText.MarkdownTextStyle>> stack) {
        Set<MarkdownStyledText.MarkdownTextStyle> styles = new HashSet<>();
        for (Set<MarkdownStyledText.MarkdownTextStyle> s : stack) styles.addAll(s);
        return styles;
    }

    private static void handleMarker(String marker, Deque<Set<MarkdownStyledText.MarkdownTextStyle>> stack) {
        Set<MarkdownStyledText.MarkdownTextStyle> styles = markerStyles.get(marker);
        //If stack's peek value equals current means current is the end of corresponding style, pop it
        if (stack.peek() != null && stack.peek().equals(styles)) {
            stack.pop();
        } else {
            //Otherwise it's the beginning
            stack.push(styles);
        }
    }

    //Split the string by stateMap(merge all neighbour char elements with the same style into one element)
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
        elements.addAll(createElement(input, start, currentStyles));

        return elements;
    }


    private static List<MarkdownElement> createElement(String input, int start, int end, Set<MarkdownStyledText.MarkdownTextStyle> styles) {
        return MarkdownUrl.splitLineByUrl(new MarkdownStyledText(input.substring(start, end), Collections.unmodifiableSet(styles)));
    }

    //For some edge cases, in order not to throw an IndexOutOfBoundException
    private static List<MarkdownElement> createElement(String input, int start, Set<MarkdownStyledText.MarkdownTextStyle> styles) {
        return MarkdownUrl.splitLineByUrl(new MarkdownStyledText(input.substring(start), Collections.unmodifiableSet(styles)));
    }

    public static String serializeEscape(String str){
        str = str.replaceAll("\\\\\\+", "ā");
        str = str.replaceAll("\\\\`", "á");
        str = str.replaceAll("\\\\\\*", "ǎ");
        str = str.replaceAll("\\\\~", "à");
        return str;
    }

    public static String deserializeEscape(String str){
        str = str.replaceAll("ā", "+");
        str = str.replaceAll("á", "`");
        str = str.replaceAll("ǎ", "*");
        str = str.replaceAll("à", "~");
        return str;
    }
}
