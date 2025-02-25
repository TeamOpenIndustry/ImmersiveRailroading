package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import util.Matrix4;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

//Storage content
public class MarkdownDocument {
    public static HashMap<Identifier, MarkdownDocument> documents = new HashMap<>();

    public final Identifier page;
    public MarkdownClickableElement over;

    protected List<MarkdownLine> original;
    protected int pageWidth;
    public List<MarkdownLine> brokenLines;

    private static final int CODE_COLOR = 0xFFDDDDDD;
    private static final int TIPS_BAR_COLOR = 0xFF00DD00;

    private static final int BLACK = 0xFF000000;

    private Rectangle2D scrollRegion;
    private double scrollSpeed;
    private double offset;
    private int pageHeight;

    public static MarkdownDocument getPageByID(Identifier id){
        if(!documents.containsKey(id)){
            documents.put(id, new MarkdownDocument(id));
        }
        return documents.get(id);
    }

    private MarkdownDocument(Identifier page) {
        this.page = page;
        this.original = new LinkedList<>();
        this.brokenLines = new LinkedList<>();
    }

    //For chained call
    public MarkdownDocument addLines(List<MarkdownLine> lines) {
        lines.forEach(this::addLine);
        return this;
    }

    public MarkdownDocument addLine(MarkdownElement line){
        return this.addLine(Collections.singletonList(line));
    }

    public MarkdownDocument addLine(List<MarkdownElement> line){
        return this.addLine(new MarkdownLine(line));
    }

    public MarkdownDocument addLine(MarkdownLine line){
        this.original.add(line);
        return this;
    }

    public int countLine(){
        return brokenLines.size();
    }

    public boolean isEmpty(){
        return this.original.isEmpty();
    }

    public int getPageWidth() {
        return pageWidth;
    }

    public void setPageWidth(int pageWidth) {
        this.pageWidth = pageWidth;
    }

    public void setScrollRegion(Rectangle2D scrollRegion) {
        this.scrollRegion = scrollRegion;
    }

    public double getOffset() {
        return offset;
    }

    public void setOffset(double offset) {
        this.offset = offset;
    }

    public Rectangle2D getScrollRegion() {
        return scrollRegion;
    }

    //Render a page and return its height
    public int render(Matrix4 matrix4){
        matrix4.translate(0, -offset, 0);
        int height = 0;
        boolean inCodeBlock = false;
        boolean inTips = false;
        Vec3d offset;
        over = null;
        for(MarkdownLine line : brokenLines){
            int currWidth = 0;
            offset = matrix4.apply(Vec3d.ZERO);
            if(line.codeBlockStart){
                inCodeBlock = true;
                //Draw code block
                //In code block there is at most one text element
                if(!line.line.get(0).apply().isEmpty()){ //Contains language specification
                    GUIHelpers.drawRect((int) offset.x, (int) offset.y,
                            pageWidth, 10, CODE_COLOR);
                    int delta = pageWidth - GUIHelpers.getTextWidth(line.line.get(0).apply());
                    matrix4.translate(delta, 0, 0);
                    GUIHelpers.drawString(line.line.get(0).apply(), 0, 0, BLACK, matrix4);
                    matrix4.translate(-delta, 10, 0);
                    height += 10;
                } else {
                    GUIHelpers.drawRect((int) offset.x, (int) offset.y,
                            pageWidth, 5, CODE_COLOR);
                    matrix4.translate(0, 5, 0);
                    height += 5;

                }
                continue;
            } else if(line.codeBlockEnd) {
                inCodeBlock = false;
                //Draw code block ending
                GUIHelpers.drawRect((int) offset.x , (int) offset.y ,
                        pageWidth, 5, CODE_COLOR);
                matrix4.translate(0, 5, 0);
                height += 5;
                continue;
            }
            if(inCodeBlock){
                //In code block there is at most one text element
                GUIHelpers.drawRect((int) offset.x , (int) offset.y ,
                        pageWidth, 10, CODE_COLOR);
                GUIHelpers.drawString(line.line.get(0).apply(), 0, 0, BLACK, matrix4);
                matrix4.translate(0, 10, 0);
                height += 10;
                continue;
            }

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

            for(MarkdownElement element : line.line){
                //Show current matrix result
                offset = matrix4.apply(Vec3d.ZERO);
                height += element.render(matrix4, pageWidth);

                String str = element.apply();

                if(element instanceof MarkdownStyledText){
                    currWidth += GUIHelpers.getTextWidth(str) + (((MarkdownStyledText) element).hasCode() ? 2 : 0);
                }

                //Dynamically update urls' pos
                if(element instanceof MarkdownClickableElement){
                    currWidth += GUIHelpers.getTextWidth(str);
                    ((MarkdownClickableElement) element).section = new Rectangle((int) offset.x, (int) offset.y,
                            GUIHelpers.getTextWidth(str), 10);

                    if(((MarkdownClickableElement) element).section.contains(ManualHoverRenderer.mouseX, ManualHoverRenderer.mouseY)){
                        over = (MarkdownClickableElement) element;
                    }
                }
            }
            matrix4.translate(-currWidth, 10, 0);
            height += 10;
        }
        this.pageHeight = height - 100;
        return height;
    }

    //Transform page in Y axis
    public void onScroll(ClientEvents.MouseGuiEvent event){
        if(scrollRegion != null && scrollRegion.contains(event.x, event.y)){
            this.scrollSpeed =  Math.min(50, Math.max(-50, this.scrollSpeed - (10 * event.scroll)));
        }
    }

    //Handle click
    public void onMouseRelease(ClientEvents.MouseGuiEvent event){
        if(this.scrollRegion.contains(event.x, event.y)){
            this.brokenLines.forEach(line -> line.line.stream().filter(e -> e instanceof MarkdownUrl).forEach(element -> {
                if(((MarkdownUrl) element).section.contains(event.x, event.y)){
                    ((MarkdownUrl) element).click();
                }
            }));
        }
    }

    //Reduce scroll speed
    public void onClientTick(){
        this.offset += (int) scrollSpeed;

        offset = Math.max(0, Math.min(pageHeight, offset));

        scrollSpeed += scrollSpeed > 0 ? -Math.min(scrollSpeed, 3) :
                scrollSpeed < 0 ? -Math.max(scrollSpeed, -3) :
                        0;
    }

    public static class MarkdownLine{
        public List<MarkdownElement> line;
        //Store interline state to control rendering
        public boolean unorderedList = false;
        public boolean codeBlockStart = false;
        public boolean codeBlockEnd = false;
        public boolean tipStart = false;
        public boolean tipEnd = false;
        public static final int LIST_PREFIX_WIDTH = GUIHelpers.getTextWidth("  ");

        public MarkdownLine(MarkdownElement element){
            this(Collections.singletonList(element));
        }

        public MarkdownLine(List<MarkdownElement> line) {
            this.line = line;
        }

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
    }
}
