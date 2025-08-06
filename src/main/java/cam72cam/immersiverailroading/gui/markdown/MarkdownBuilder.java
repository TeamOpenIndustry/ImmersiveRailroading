package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.resource.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Function;

import static cam72cam.immersiverailroading.gui.markdown.MarkdownStyledText.*;

/**
 * Beginning of markdown
 * <p>
 * To use it, call build and get your MarkdownDocument
 * <p>
 * If you are a Markdown writer, please see the document at immersiverailroading:wiki/en_us/__READ_ME_BEFORE_CONTRIBUTING.txt
 *
 * @see MarkdownDocument
 */
public class MarkdownBuilder {
    private static final HashMap<String, Function<String, List<MarkdownDocument.MarkdownLine>>> SPECIAL_MATCHER = new HashMap<>();

    /**
     * Builds a MarkdownDocument from the given resource identifier
     * @param id The identifier containing markdown content
     * @param screenWidth Target rendering width for line breaking
     * @return Built MarkdownDocument instance
     * @throws IOException If resource reading fails
     */
    public static MarkdownDocument build(Identifier id, int screenWidth) throws IOException {
        //We want to detect line by line
        BufferedReader reader = new BufferedReader(new InputStreamReader(id.getResourceStream()));

        MarkdownDocument document = MarkdownDocument.getOrComputePageByID(id);
        //If it's already loaded, just get from cache(and re-break the line) then return
        if(!document.isEmpty()){
            if(document.getPageWidth() == screenWidth){
                return document;
            } else {
                document.setPageWidth(screenWidth);
                return MarkdownLineBreaker.breakDocument(document, screenWidth);
            }
        }
        //Otherwise we need to parse it
        String currentLine;
        //Interline state storage
        boolean isInTips = false;

        while ((currentLine = reader.readLine()) != null){
            //Deal with custom parser logic first
            String finalStr = currentLine.trim();
            Optional<Map.Entry<String, Function<String, List<MarkdownDocument.MarkdownLine>>>> optionalFunc =
                    SPECIAL_MATCHER.entrySet().stream().filter(entry -> finalStr.startsWith(entry.getKey())).findFirst();
            if(optionalFunc.isPresent()){
                document.addLines(optionalFunc.get().getValue().apply(currentLine));
                continue;
            }

            if(isInTips){
                if(!currentLine.startsWith(">")){
                    document.addLine(MarkdownDocument.MarkdownLine.create(new MarkdownStyledText("")).isTipEnd(true));
                    isInTips = false;
                } else {
                    if(currentLine.length() > 1){//More than just 1 '>', we should read its content
                        document.addLine(MarkdownDocument.MarkdownLine.create(parse(currentLine.substring(Math.min(2, currentLine.length() - 1)))));
                    } else {//Only one '>', just marks tips block hasn't end
                        document.addLine(MarkdownDocument.MarkdownLine.create(new MarkdownStyledText("")));
                    }
                }
                continue;
            }

            //Deal with escapes
            currentLine = serializeEscape(currentLine);
            currentLine = currentLine.trim();

            if(currentLine.startsWith("#")){
                //Title
                //Cannot contain URL
                document.addLine(Collections.singletonList(new MarkdownTitle(currentLine)));
            } else if(currentLine.startsWith("!")){
                //Picture
                MarkdownUrl url = MarkdownUrl.compileSingle(currentLine.substring(1));
                if(url != null) {
                    document.addLine(new MarkdownPicture(new Identifier(deserializeEscape(url.destination.toString()))));
                }
            } else if(currentLine.startsWith("```")){
                //Code block, let specific proxy class parse it
                MarkdownCodeBlock.parse(reader, document, currentLine);
            } else if(currentLine.startsWith("* ") || currentLine.startsWith("- ")){
                //Unsorted list
                document.addLine(MarkdownDocument.MarkdownLine.create(parse(currentLine.substring(2))).isUnorderedList(true));
            } else if(currentLine.startsWith("> ")){
                //Tips
                //Must be more than just 1 '>'
                document.addLine(MarkdownDocument.MarkdownLine.create(new MarkdownStyledText("")).isTipStart(true));
                document.addLine(MarkdownDocument.MarkdownLine.create(parse(currentLine.substring(Math.min(2, currentLine.length() - 1)))));
                isInTips = true;
            } else if(MarkdownSplitLine.validate(currentLine)){
                //Split line
                document.addLine(new MarkdownSplitLine());
            } else {
                if(!currentLine.isEmpty()){
                    //Basic String
                    document.addLine(parse(currentLine));
                } else {
                    document.addLine(new MarkdownStyledText(""));
                }
            }
        }
        //If the file ends but code(proxy class has done it)/tips block doesn't end, finalize it
        if(isInTips){
            document.addLine(MarkdownDocument.MarkdownLine.create(new MarkdownStyledText("")).isTipEnd(true));
        }
        document.setPageWidth(screenWidth);
        //Deal wit line break
        return MarkdownLineBreaker.breakDocument(document, screenWidth);
    }

    /**
     * Registers a custom syntax handler
     * @param syntaxPrefix Syntax prefix to trigger this handler
     * @param handler Function to process matching lines
     */
    public static void register(String syntaxPrefix, Function<String, List<MarkdownDocument.MarkdownLine>> handler){
        SPECIAL_MATCHER.put(syntaxPrefix, handler);
    }

    /**
     * Internal method to parse a raw String markdown line into MarkdownStyledText and MarkdownUrl
     * @param input Raw markdown line
     * @return Parsed MarkdownElement line
     */
    protected static List<MarkdownElement> parse(String input){
        List<Set<MarkdownStyledText.MarkdownTextStyle>> stateMap = new ArrayList<>(input.length());
        Deque<Set<MarkdownStyledText.MarkdownTextStyle>> styleStack = new ArrayDeque<>();
        int currentPos = 0;

        while (currentPos < input.length()) {
            boolean markerMatched = false;

            // Try to match the longest possible marker
            for (String marker : MARKER_PARSE_PRIORITY) {
                int len = marker.length();
                if (currentPos + len > input.length()) continue;

                String substring = input.substring(currentPos, currentPos + len);
                if (MARKER_STYLES.containsKey(substring)) {
                    handleMarker(substring, styleStack);
                    currentPos += len;
                    markerMatched = true;
                    break;
                }
            }

            if (!markerMatched) {
                //Pure text, add to stateMap
                Set<MarkdownStyledText.MarkdownTextStyle> currentStyles = mergeStackStyles(styleStack);
                stateMap.add(currentStyles);
                currentPos++;
            }
        }

        //Build a String that matches stateMap
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

    /**
     * Internal method to merge more than one style sets into one
     * @param stack Style sets
     * @return Deduplicated style set
     */
    private static Set<MarkdownStyledText.MarkdownTextStyle> mergeStackStyles(Deque<Set<MarkdownStyledText.MarkdownTextStyle>> stack) {
        Set<MarkdownStyledText.MarkdownTextStyle> styles = new HashSet<>();
        for (Set<MarkdownStyledText.MarkdownTextStyle> s : stack) styles.addAll(s);
        return styles;
    }

    /**
     * Internal method to deal with style stack
     * @param marker Current matched marker
     * @param stack The style stack
     */
    private static void handleMarker(String marker, Deque<Set<MarkdownStyledText.MarkdownTextStyle>> stack) {
        Set<MarkdownStyledText.MarkdownTextStyle> styles = MARKER_STYLES.get(marker);
        //If stack's peek value equals current marker means current marker is the end of corresponding style, pop it
        if (stack.peek() != null && stack.peek().equals(styles)) {
            stack.pop();
        } else {
            //Otherwise it's the beginning, push it
            stack.push(styles);
        }
    }

    /**
     * Internal method to turn all neighbor chars with the same style into one MarkdownStyledText element
     * @param input Original String
     * @param stateMap List representing style of the chars
     * @return The split line
     */
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

    /**
     * Create a MarkdownStyledText element then detect urls inside it and split
     * @param input Original line
     * @param start Starting position of the element
     * @param end Ending position of the element
     * @param styles Styles that should be applied
     * @return The parsed text and url line
     */
    private static List<MarkdownElement> createElement(String input, int start, int end, Set<MarkdownStyledText.MarkdownTextStyle> styles) {
        return MarkdownUrl.splitLineByUrl(new MarkdownStyledText(input.substring(start, end), Collections.unmodifiableSet(styles)));
    }

    //For some weird edge cases I wrote this, in order not to throw an IndexOutOfBoundException
    private static List<MarkdownElement> createElement(String input, int start, Set<MarkdownStyledText.MarkdownTextStyle> styles) {
        return MarkdownUrl.splitLineByUrl(new MarkdownStyledText(input.substring(start), Collections.unmodifiableSet(styles)));
    }

    /**
     * Serialize the escaped marker chars into Chinese Pinyin(which I think no one would use them)
     * @param str Original String
     * @return Serialized String
     */
    public static String serializeEscape(String str){
        str = str.replaceAll("\\\\\\+", "ā");
        str = str.replaceAll("\\\\`", "á");
        str = str.replaceAll("\\\\\\*", "ǎ");
        str = str.replaceAll("\\\\~", "à");
        return str;
    }

    /**
     * Deserialize the serialized Pinyin into their base char after the line have been parsed
     * @param str Serialized String
     * @return Original String
     */
    public static String deserializeEscape(String str){
        str = str.replaceAll("ā", "+");
        str = str.replaceAll("á", "`");
        str = str.replaceAll("ǎ", "*");
        str = str.replaceAll("à", "~");
        return str;
    }
}
