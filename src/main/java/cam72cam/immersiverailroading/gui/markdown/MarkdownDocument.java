package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import util.Matrix4;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static cam72cam.immersiverailroading.gui.markdown.Colors.*;

/**
 * Storage class to store Markdown file's content
 */
@SuppressWarnings("unused")
public class MarkdownDocument {
    //All cached document
    //TODO Maybe we should to use ExpireableMap?
    private static final HashMap<Identifier, MarkdownDocument> DOCUMENTS = new HashMap<>();

    public final Identifier page;
    protected final List<MarkdownLine> originalLines;
    protected final List<MarkdownLine> brokenLines;
    private Rectangle2D scrollRegion;
    private double scrollSpeed;
    private double verticalOffset;
    private int pageWidth;
    private int pageHeight;
    private MarkdownClickableElement hoveredElement;

    /**
     * Internal constructor class
     * @param page This page's content location
     */
    private MarkdownDocument(Identifier page) {
        this.page = page;
        this.originalLines = new LinkedList<>();
        this.brokenLines = new LinkedList<>();
    }

    /**
     * Try to get a cached page
     * @param id The page's content location
     * @return The cached page or a new page if not present
     */
    public static synchronized MarkdownDocument getOrComputePageByID(Identifier id){
        return DOCUMENTS.computeIfAbsent(id, MarkdownDocument::new);
    }

    /**
     * Provide an API for dynamic generated content
     * @param id The cached page need to be cleared
     */
    public static synchronized void refreshByID(Identifier id){
        Optional.ofNullable(DOCUMENTS.get(id)).ifPresent(document -> {
            document.clearCache();
            try {
                MarkdownBuilder.build(id, document.getPageWidth());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Render this page and return the height
     * @param matrix4 Transform matrix
     * @return Height of the page
     */
    public int render(@Nonnull Matrix4 matrix4){
        matrix4.translate(0, -verticalOffset, 0);
        int height = 0;
        boolean inTips = false;
        Vec3d offset;
        hoveredElement = null;
        //We need the iterator so here we use while instead of for each
        Iterator<MarkdownLine> lineIterator = brokenLines.iterator();
        while (lineIterator.hasNext()){
            MarkdownLine line = lineIterator.next();
            int currWidth = 0;
            //Stores current matrix result
            offset = matrix4.apply(Vec3d.ZERO);
            if(line.codeBlockStart){
                //Let proxy class do it
                height += MarkdownCodeBlock.render(matrix4, lineIterator, this, line);
                continue;
            }

            //Tips block have a green bar
            if(line.tipStart){
                inTips = true;
                continue;
            } else if(line.tipEnd){
                inTips = false;
                continue;
            }
            if(inTips){
                GUIHelpers.drawRect((int) offset.x , (int) offset.y ,
                        MarkdownLine.LIST_PREFIX_WIDTH / 4, 10, TIPS_BAR_COLOR);
            }

            //Should we translate the matrix to next line manually?
            boolean shouldStartANewLine = false;

            for(MarkdownElement element : line.elements){
                //Show current matrix result
                offset = matrix4.apply(Vec3d.ZERO);
                height += element.render(matrix4, pageWidth);

                String str = element.apply();

                //These two element could be used multiply times in a line so they can't auto start new line, need manual translate
                if(element instanceof MarkdownStyledText || element instanceof MarkdownUrl){
                    shouldStartANewLine = true;
                }

                if(element instanceof MarkdownStyledText){
                    currWidth += GUIHelpers.getTextWidth(str) + (((MarkdownStyledText) element).hasCode() ? 2 : 0);
                }

                //Dynamically update clickable elements' pos(for now only url is included)
                if(element instanceof MarkdownClickableElement){
                    currWidth += GUIHelpers.getTextWidth(str);
                    ((MarkdownClickableElement) element).section = new Rectangle((int) offset.x, (int) offset.y,
                            GUIHelpers.getTextWidth(str), 10);

                    if(((MarkdownClickableElement) element).section.contains(ManualHoverRenderer.mouseX, ManualHoverRenderer.mouseY)){
                        hoveredElement = (MarkdownClickableElement) element;
                    }
                }
            }
            matrix4.translate(-currWidth, 0, 0);
            if(shouldStartANewLine){
                matrix4.translate(0, 10, 0);
                height += 10;
            }
        }
        this.pageHeight = height - 100;
        return height;
    }

    /**
     * addLine and overloads and addLines to simplify external use
     * @param lines Given lines
     * @return This
     */
    public MarkdownDocument addLines(List<MarkdownLine> lines) {
        lines.forEach(this::addLine);
        return this;
    }

    //Overloads
    public MarkdownDocument addLine(MarkdownElement line){
        return this.addLine(Collections.singletonList(line));
    }

    public MarkdownDocument addLine(List<MarkdownElement> line){
        return this.addLine(new MarkdownLine(line));
    }

    public MarkdownDocument addLine(MarkdownLine line){
        this.originalLines.add(line);
        return this;
    }

    //Method used by rendering manual's footer
    public int getLineCount(){
        return brokenLines.size();
    }

    public boolean isEmpty(){
        return this.originalLines.isEmpty();
    }

    private void clearCache(){
        this.originalLines.clear();
        this.brokenLines.clear();
    }

    public int getPageWidth() {
        return pageWidth;
    }

    public void setPageWidth(int pageWidth) {
        this.pageWidth = pageWidth;
    }

    public MarkdownClickableElement getHoveredElement() {
        return hoveredElement;
    }

    public List<MarkdownLine> getBrokenLines() {
        return brokenLines;
    }

    public List<MarkdownLine> getOriginalLines() {
        return originalLines;
    }

    public void setScrollRegion(Rectangle2D scrollRegion) {
        this.scrollRegion = scrollRegion;
    }

    public double getVerticalOffset() {
        return verticalOffset;
    }

    public void setVerticalOffset(double verticalOffset) {
        this.verticalOffset = verticalOffset;
    }

    public Rectangle2D getScrollRegion() {
        return scrollRegion;
    }

    /**
     * Change scroll speed based on input
     * @param scrollEvent mouse scroll input
     */
    public void onScroll(ClientEvents.MouseGuiEvent scrollEvent){
        //Check validate
        if(scrollRegion != null && scrollRegion.contains(scrollEvent.x, scrollEvent.y)){
            this.scrollSpeed =  Math.min(50, Math.max(-50, this.scrollSpeed - (10 * scrollEvent.scroll)));
        }
    }

    /**
     * Try to find child elements that can be invoked by this click
     * @param releaseEvent We only consider a mouse release as a click
     */
    public void onMouseRelease(ClientEvents.MouseGuiEvent releaseEvent){
        if(this.scrollRegion.contains(releaseEvent.x, releaseEvent.y)){
            this.brokenLines.forEach(line -> line.elements.stream().filter(e -> e instanceof MarkdownClickableElement)
                    .forEach(element -> {
                if(((MarkdownClickableElement) element).section.contains(releaseEvent.x, releaseEvent.y)){
                    ((MarkdownClickableElement) element).click();
                }
            }));
        }
    }

    /**
     * Reduce scroll speed on client ticks
     */
    public void handleScrollOnTicks(){
        this.verticalOffset += (int) scrollSpeed;

        verticalOffset = Math.max(0, Math.min(pageHeight, verticalOffset));

        scrollSpeed += scrollSpeed > 0 ? -Math.min(scrollSpeed, 3) :
                       scrollSpeed < 0 ? -Math.max(scrollSpeed, -3) :
                       0;
    }

    /**
     * Storage class to store documents' single line and interline status
     */
    public static class MarkdownLine{
        //For those need to indent by 2 * x spaces
        public static final int LIST_PREFIX_WIDTH = GUIHelpers.getTextWidth("  ");
        private final List<MarkdownElement> elements;
        //Store interline state to control rendering
        public boolean unorderedList = false;
        public boolean codeBlockStart = false;
        public boolean codeBlockEnd = false;
        public boolean tipStart = false;
        public boolean tipEnd = false;

        private MarkdownLine(List<MarkdownElement> elements){
            this.elements = elements;
        }

        public static MarkdownLine create(MarkdownElement element){
            return create(Collections.singletonList(element));
        }

        public static MarkdownLine create(List<MarkdownElement> line) {
            return new MarkdownLine(line);
        }

        //Allow chained call to optimize object creation
        public MarkdownLine isUnorderedList(boolean isUnorderedList){
            this.unorderedList = isUnorderedList;
            return this;
        }

        public MarkdownLine isCodeBlockStart(boolean isCodeBlockStart){
            this.codeBlockStart = isCodeBlockStart;
            return this;
        }

        public MarkdownLine isCodeBlockEnd(boolean isCodeBlockEnd){
            this.codeBlockEnd = isCodeBlockEnd;
            return this;
        }

        public MarkdownLine isTipStart(boolean tipStart) {
            this.tipStart = tipStart;
            return this;
        }

        public MarkdownLine isTipEnd(boolean tipEnd) {
            this.tipEnd = tipEnd;
            return this;
        }

        //Get the line's content for rendering
        public List<MarkdownElement> getElements() {
            return elements;
        }
    }
}
