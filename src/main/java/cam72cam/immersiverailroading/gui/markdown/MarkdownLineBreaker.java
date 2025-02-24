package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.gui.helpers.GUIHelpers;

import java.util.*;

public class MarkdownLineBreaker {
    public static List<List<MarkdownElement>> breakLine(List<MarkdownElement> elements, int screenWidth) {
        List<List<MarkdownElement>> lines = new LinkedList<>();
        Deque<MarkdownElement> processingDeque = new LinkedList<>(elements);
        List<MarkdownElement> currentLine = new LinkedList<>();
        int currentLineWidth = 0;

        while (!processingDeque.isEmpty()) {
            MarkdownElement element = processingDeque.poll();
            int elementWidth = GUIHelpers.getTextWidth(element.text);

            // If the element is oversize...
            if (elementWidth > screenWidth && currentLine.isEmpty()) {
                handleOversizeElement(element, screenWidth, processingDeque, lines);
                continue;
            }

            // Or
            if (currentLineWidth + elementWidth <= screenWidth) {
                currentLine.add(element);
                currentLineWidth += elementWidth;
            } else {
                processLineBreak(element, processingDeque, currentLine, lines, currentLineWidth, screenWidth);
                currentLine = new LinkedList<>();
                currentLineWidth = 0;
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine);
        }

        return lines;
    }

    private static void handleOversizeElement(MarkdownElement element, int screenWidth,
                                              Deque<MarkdownElement> queue, List<List<MarkdownElement>> lines) {
        int splitPos = findOptimalSplitPosition(element.text, screenWidth);
        MarkdownElement[] splitElements = element.split(splitPos);

        List<MarkdownElement> newLine = new LinkedList<>();
        newLine.add(splitElements[0]);
        lines.add(newLine);

        queue.addFirst(splitElements[1]);
    }

    private static void processLineBreak(MarkdownElement element, Deque<MarkdownElement> queue,
                                         List<MarkdownElement> currentLine, List<List<MarkdownElement>> lines,
                                         int currentWidth, int screenWidth) {
        int splitPos = findOptimalSplitPosition(element.text, screenWidth - currentWidth);
        if (splitPos != -1) {
            MarkdownElement[] splitElements = element.split(splitPos);

            currentLine.add(splitElements[0]);
            lines.add(currentLine);

            queue.addFirst(splitElements[1]);
        } else {//It should be fully moved to next line
            lines.add(currentLine);
            queue.addFirst(element);
        }
    }

    private static int findOptimalSplitPosition(String text, int maxWidth) {
        int low = 0;
        int high = text.length();
        int bestPos = 0;

        while (low <= high) {
            int mid = (low + high) / 2;
            int currentWidth = GUIHelpers.getTextWidth(text.substring(0, mid));

            if (currentWidth <= maxWidth) {
                bestPos = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        int result = Math.max(1, Math.min(bestPos, text.length() - 1));

        //Try to find a space to break down
        for(int i = result; i >= 1; i--){
            if(text.charAt(i) ==' '){
                return i;
            }
        }
        //Can't break, move to next line
        return -1;
    }
}
