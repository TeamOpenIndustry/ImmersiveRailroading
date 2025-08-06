package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.gui.helpers.GUIHelpers;

import java.util.*;

import static cam72cam.immersiverailroading.gui.markdown.MarkdownDocument.MarkdownLine.LIST_PREFIX_WIDTH;

/**
 * Helper class to break a MarkdownDocument's originalLines into its brokenLines
 * @see MarkdownDocument
 */
public class MarkdownLineBreaker {
    /**
     * API method to break document lines into screen-width appropriate segments
     * @param document Document needed to be re-broken
     * @param screenWidth Target rendering width
     * @return The document itself, with refreshed brokenLines
     */
    protected static MarkdownDocument breakDocument(MarkdownDocument document, int screenWidth){
        document.brokenLines.clear();
        boolean codeBlockFlag = false;
        boolean tipsFlag = false;
        for (MarkdownDocument.MarkdownLine markdownLine : document.originalLines) {
            if(markdownLine.codeBlockStart){
                //No need to split
                document.brokenLines.add(markdownLine);
                codeBlockFlag = true;
                continue;
            }
            if(markdownLine.codeBlockEnd){
                document.brokenLines.add(markdownLine);
                codeBlockFlag = false;
                continue;
            }
            if(markdownLine.tipStart){
                document.brokenLines.add(markdownLine);
                tipsFlag = true;
                continue;
            }
            if(markdownLine.tipEnd){
                document.brokenLines.add(markdownLine);
                tipsFlag = false;
                continue;
            }

            List<List<MarkdownElement>> lines;
            if(markdownLine.unorderedList){
                lines = breakLine(markdownLine, screenWidth - LIST_PREFIX_WIDTH, true);
                lines.get(0).add(0, new MarkdownStyledText("• "));
                for(int i = 1; i < lines.size(); i++){
                    lines.get(i).add(0, new MarkdownStyledText("  "));
                }
            } else if(tipsFlag){
                lines = breakLine(markdownLine, screenWidth - LIST_PREFIX_WIDTH, true);
                lines.forEach(line -> line.add(0, new MarkdownStyledText("  ")));
            } else if(codeBlockFlag){
                lines = breakLine(markdownLine, screenWidth, false);
            } else {
                lines = breakLine(markdownLine, screenWidth, true);
            }
            lines.forEach(l -> document.brokenLines.add(MarkdownDocument.MarkdownLine.create(l)));
        }

        return document;
    }

    /**
     * Internal method to break a single line into multiple screen-width appropriate segments
     * @param line Original line for breaking
     * @param screenWidth Target rendering width
     * @param preferSpace Whether the line prefer to break on spaces first or break letters
     * @return The broken raw lines representing in list
     */
    private static List<List<MarkdownElement>> breakLine(MarkdownDocument.MarkdownLine line, int screenWidth, boolean preferSpace) {
        List<List<MarkdownElement>> lines = new LinkedList<>();
        Deque<MarkdownElement> processingDeque = new LinkedList<>(line.getElements());
        List<MarkdownElement> currentLine = new LinkedList<>();
        int currentLineWidth = 0;

        while (!processingDeque.isEmpty()) {
            MarkdownElement element = processingDeque.poll();
            //Bold text is wider; Titles are wider too
            double multiplier = (element instanceof MarkdownStyledText && ((MarkdownStyledText) element).hasBold()) ? 1.4 :
                                (element instanceof MarkdownTitle && ((MarkdownTitle) element).level == 1) ? 1.35 :
                                (element instanceof MarkdownTitle && ((MarkdownTitle) element).level == 2) ? 1.15 :
                                1;
            int elementWidth = (int) (GUIHelpers.getTextWidth(element.text) * multiplier);

            // If the element is oversize...
            if (elementWidth > screenWidth && currentLine.isEmpty()) {
                handleOversizeElement(element, screenWidth, processingDeque, lines, multiplier, preferSpace);
                continue;
            }

            // Or
            if (currentLineWidth + elementWidth <= screenWidth) {
                currentLine.add(element);
                currentLineWidth += elementWidth;
            } else {
                processLineBreak(element, processingDeque, currentLine, lines, currentLineWidth, screenWidth, multiplier, preferSpace);
                currentLine = new LinkedList<>();
                currentLineWidth = 0;
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine);
        }

        return lines;
    }

    /**
     * Internal breaker method for an element which is too large to fit on a single line
     * @param element The oversize element
     * @param screenWidth Target rendering width
     * @param queue MarkdownElements waiting for being added to broken lines
     * @param lines Already broken lines
     * @param multiplier Element's width multiplier, e.g. bold text is wider than ordinary text
     * @param preferSpace Whether the line prefer to break on spaces first or break letters
     */
    private static void handleOversizeElement(MarkdownElement element, int screenWidth,
                                              Deque<MarkdownElement> queue, List<List<MarkdownElement>> lines,
                                              double multiplier, boolean preferSpace) {
        int splitPos = findOptimalPosOrSpace(element.text, screenWidth, multiplier);
        if(!preferSpace || splitPos == -1){
            //Very long string without spacing or don't want, use default method
            splitPos = findOptimalPos(element.text, screenWidth, multiplier);
        }
        MarkdownElement[] splitElements = element.split(splitPos);

        List<MarkdownElement> newLine = new LinkedList<>();
        newLine.add(splitElements[0]);
        lines.add(newLine);

        queue.addFirst(splitElements[1]);
    }

    /**
     * Internal breaker method for an element which is not that big
     * @param element The element
     * @param queue MarkdownElements waiting for being added to broken lines
     * @param currentLine Current line
     * @param lines Already broken lines
     * @param currentWidth The line's current width taken by existing elements
     * @param screenWidth Target rendering width
     * @param multiplier Element's width multiplier, e.g. bold text is wider than ordinary text
     * @param preferSpace Whether the line prefer to break on spaces first or break letters
     */
    private static void processLineBreak(MarkdownElement element, Deque<MarkdownElement> queue,
                                         List<MarkdownElement> currentLine, List<List<MarkdownElement>> lines,
                                         int currentWidth, int screenWidth, double multiplier, boolean preferSpace) {
        int splitPos;
        if(preferSpace){
           splitPos = findOptimalPosOrSpace(element.text, screenWidth - currentWidth, multiplier);
        } else {
            splitPos = findOptimalPos(element.text, screenWidth - currentWidth, multiplier);
        }
        if (splitPos != -1) {
            //We should break it
            MarkdownElement[] splitElements = element.split(splitPos);

            currentLine.add(splitElements[0]);
            lines.add(currentLine);

            queue.addFirst(splitElements[1]);
        } else {
            //It should be fully moved to next line
            lines.add(currentLine);
            queue.addFirst(element);
        }
    }

    /**
     * Internal method to find optimal breaking point using binary search, and return the nearest space's position before it
     * @param text Text needed to be broken
     * @param maxWidth Rest available space of current line
     * @param widthMultiplier Element's width multiplier, e.g. bold text is wider than ordinary text
     * @return The found breaking point, -1 if no space before are found
     */
    private static int findOptimalPosOrSpace(String text, int maxWidth, double widthMultiplier) {
        int result = findOptimalPos(text, maxWidth, widthMultiplier);

        //Try to find a space to break down
        for(int i = result; i >= 1; i--){
            if(Character.isWhitespace(text.charAt(i))){
                return i;
            }
        }
        //Can't find space, we'd better move it fully to the next line
        return -1;
    }

    /**
     * Internal method to find optimal breaking point using binary search and return it
     * @param text Text needed to be broken
     * @param maxWidth Rest available space of current line
     * @param widthMultiplier Element's width multiplier, e.g. bold text is wider than ordinary text
     * @return The optimal breaking point
     */
    private static int findOptimalPos(String text, int maxWidth, double widthMultiplier){
        int low = 0;
        int high = text.length();
        int bestPos = 0;

        while (low <= high) {
            int mid = (low + high) / 2;
            int currentWidth = (int) (GUIHelpers.getTextWidth(text.substring(0, mid)) * widthMultiplier);

            if (currentWidth <= maxWidth) {
                bestPos = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        //To avoid StackOverflowError
        return Math.max(1, Math.min(bestPos, text.length() - 1));
    }
}
