package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.gui.helpers.GUIHelpers;

import java.util.*;

//AI generated, need optimize
public class MarkdownLineBreaker {
    /**
     * 核心换行方法
     * @param elements 输入元素列表
     * @param screenWidth 屏幕宽度
     * @return 分行后的元素列表
     */
    public static List<List<MarkdownElement>> breakLine(List<MarkdownElement> elements, int screenWidth) {
        List<List<MarkdownElement>> lines = new ArrayList<>();
        Deque<MarkdownElement> processingDeque = new LinkedList<>(elements);
        List<MarkdownElement> currentLine = new ArrayList<>();
        int currentLineWidth = 0;

        while (!processingDeque.isEmpty()) {
            MarkdownElement element = processingDeque.poll();
            int elementWidth = GUIHelpers.getTextWidth(element.text);

            // 处理当前元素宽度超过屏幕的特殊情况
            if (elementWidth > screenWidth && currentLine.isEmpty()) {
                handleOversizeElement(element, screenWidth, processingDeque, lines);
                continue;
            }

            // 正常换行逻辑
            if (currentLineWidth + elementWidth <= screenWidth) {
                currentLine.add(element);
                currentLineWidth += elementWidth;
            } else {
                processLineBreak(element, processingDeque, currentLine, lines, currentLineWidth, screenWidth);
                currentLine = new ArrayList<>();
                currentLineWidth = 0;
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine);
        }

        return lines;
    }

    /**
     * 处理超长元素（元素宽度 > 屏幕宽度且当前行空）
     */
    private static void handleOversizeElement(MarkdownElement element, int screenWidth,
                                              Deque<MarkdownElement> queue, List<List<MarkdownElement>> lines) {
        int splitPos = findOptimalSplitPosition(element.text, screenWidth);
        MarkdownElement[] splitElements = element.split(splitPos);

        List<MarkdownElement> newLine = new ArrayList<>();
        newLine.add(splitElements[0]);
        lines.add(newLine);

        queue.addFirst(splitElements[1]); // 将剩余部分重新加入队列头部
    }

    /**
     * 处理常规换行情况
     */
    private static void processLineBreak(MarkdownElement element, Deque<MarkdownElement> queue,
                                         List<MarkdownElement> currentLine, List<List<MarkdownElement>> lines,
                                         int currentWidth, int screenWidth) {
        int splitPos = findOptimalSplitPosition(element.text, screenWidth - currentWidth);
        MarkdownElement[] splitElements = element.split(splitPos);

        currentLine.add(splitElements[0]);
        lines.add(currentLine);

        queue.addFirst(splitElements[1]); // 将剩余部分重新加入队列头部
    }

    /**
     * 寻找最佳分割位置（二分查找优化版）
     */
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

        // 保证至少分割1个字符（防止死循环）
        return Math.max(1, Math.min(bestPos, text.length() - 1));
    }
}
