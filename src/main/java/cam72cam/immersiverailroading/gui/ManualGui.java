package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.gui.markdown.*;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.text.TextColor;
import org.apache.commons.lang3.tuple.MutablePair;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import static cam72cam.immersiverailroading.gui.markdown.Colors.*;

public class ManualGui implements IScreen {
    private static ManualGui currentOpeningManual;
    //                                        page     page's mainOffset
    private static final Stack<MutablePair<Identifier, Double>> historyPageStack = new Stack<>();
    private static final Stack<MutablePair<Identifier, Double>> futurePageStack = new Stack<>();
    private static final HashMap<Identifier, String> sidebarNameMap = new HashMap<>();
    private static final Rectangle2D prevPageButton;
    private static final Rectangle2D nextPageButton;

    private int width;
    private int height;
    private MarkdownDocument sidebar;
    private MarkdownDocument footer;
    private MarkdownDocument content;
    private Identifier lastPage;

    static {
        historyPageStack.push(MutablePair.of(new Identifier("immersiverailroading:wiki/en_us/home.md"), 0d));
        prevPageButton = new Rectangle(60,5,10,10);
        nextPageButton = new Rectangle(80,5,10,10);
    }

    //Will be called every time the screen scale changes
    //So there's no need to update line break manually
    @Override
    public void init(IScreenBuilder screen) {
        currentOpeningManual = this;
        try {
            footer = MarkdownBuilder.build(new Identifier(ImmersiveRailroading.MODID, "wiki/en_us/_footer.md"), screen.getWidth() - 120);
            int footerHeight = footer.getLineCount() * 10;
            sidebar = MarkdownBuilder.build(new Identifier(ImmersiveRailroading.MODID, "wiki/en_us/_sidebar.md"), screen.getWidth());
            sidebar.setScrollRegion(new Rectangle(50, 20, 120, screen.getHeight() - 20 - footerHeight));
            //Maintain a map to replace Markdown file name in url with pre-defined name
            sidebar.getOriginalLines().forEach(line ->
                    line.getElements().stream()
                            .filter(element -> element instanceof MarkdownUrl)
                            .forEach(element -> sidebarNameMap.put(((MarkdownUrl) element).destination, element.text)));
            content = MarkdownBuilder.build(historyPageStack.peek().getLeft(), screen.getWidth() - 240);
            content.setScrollRegion(new Rectangle(170,20,width - 220,height - 20 - footerHeight));
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public void onEnterKey(IScreenBuilder builder) {

    }

    @Override
    public void onClose() {
        currentOpeningManual = null;
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        IScreen.super.draw(builder, state);
        width = builder.getWidth();
        height = builder.getHeight();

        if(lastPage != historyPageStack.peek().getLeft()){
            //Meaning that we should refresh it
            try {
                content = MarkdownBuilder.build(historyPageStack.peek().getLeft(), width - 240);
                content.setScrollRegion(new Rectangle(170,20,width-220,height-30));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            content.setVerticalOffset(historyPageStack.peek().getValue());
            lastPage = historyPageStack.peek().getLeft();
        }

        //Background
        GUIHelpers.drawRect(0, 0, width, height, BACKGROUND_COLOR);
        GUIHelpers.drawRect(50, 0, width - 100, height, MAIN_COLOR);
        GUIHelpers.drawRect(50, 0, 120, height, SIDEBAR_COLOR);

        //Markdown
        sidebar.render(state.clone().translate(57, 27, 0));
        content.render(state.clone().translate(180, 30, 0));

        //Tooltip
        //Currently only MarkdownUrl inherits MarkdownClickableElement, need change when more types are added
        for(MarkdownDocument screen : new MarkdownDocument[]{sidebar, content}){
            if(screen.getHoveredElement() != null){
                MarkdownUrl clickable = (MarkdownUrl) screen.getHoveredElement();
                if (sidebarNameMap.containsKey(clickable.destination)) {
                    clickable.renderTooltip("Open page: "
                                    + sidebarNameMap.getOrDefault(clickable.destination, "UNDEFINED"),
                            (int) screen.getScrollRegion().getMaxY());
                } else {
                    clickable.renderTooltip((int) screen.getScrollRegion().getMaxY());
                }
                break;
            }
        }

        //Footer
        int lineCount = footer.getLineCount();
        GUIHelpers.drawRect(50, height - (10 * lineCount), width - 100, 10 * lineCount, FOOTER_COLOR);
        for(MarkdownDocument.MarkdownLine l : footer.getBrokenLines()){
            List<MarkdownElement> line = l.getElements();
            GUIHelpers.drawCenteredString(line.get(0).apply(),
                    width / 2, builder.getHeight() - (10 * lineCount), DEFAULT_TEXT_COLOR);
            lineCount --;
        }

        //Header
        GUIHelpers.drawRect(50, 0, width - 100, 20, HEADER_COLOR);
        GUIHelpers.drawString(TextColor.BOLD.wrap("<-"), 60, 5,
                historyPageStack.size() != 1 ? BUTTON_COLOR : BUTTON_DISABLED_COLOR);
        GUIHelpers.drawString(TextColor.BOLD.wrap("->"), 80, 5,
                !futurePageStack.isEmpty() ? BUTTON_COLOR : BUTTON_DISABLED_COLOR);
    }

    public static void pushContent(Identifier identifier){
        pushContent(identifier, 0d);
    }

    public static void pushContent(Identifier identifier, double offset){
        if(currentOpeningManual == null){
            return;
        }

        if(!historyPageStack.peek().getLeft().equals(identifier)){
            historyPageStack.push(MutablePair.of(identifier, offset));
            if(!futurePageStack.isEmpty() && identifier != futurePageStack.peek().getLeft()){
                futurePageStack.clear();
            }
        }
    }

    //Return true if the event is handled
    public static boolean onClick(ClientEvents.MouseGuiEvent event){
        if(currentOpeningManual == null){
            return false;
        }

        if(event.scroll != 0) {
            currentOpeningManual.sidebar.onScroll(event);
            currentOpeningManual.content.onScroll(event);
        }

        if(event.action == ClientEvents.MouseAction.RELEASE){
            if(prevPageButton.contains(event.x, event.y)){
                if(historyPageStack.size() > 1){
                    futurePageStack.push(historyPageStack.pop());
                }
                //Terminates unnecessary call of sidebar&content's onMouseRelease as the mouse's pos cannot inside them
                return true;
            } else if(nextPageButton.contains(event.x, event.y)){
                if(!futurePageStack.isEmpty()){
                    pushContent(futurePageStack.peek().getLeft(), futurePageStack.peek().getRight());
                    futurePageStack.pop();
                }
                return true;
            }
            currentOpeningManual.sidebar.onMouseRelease(event);
            currentOpeningManual.content.onMouseRelease(event);
        }
        return true;
    }

    //For scroll
    public static void onClientTick(){
        if(currentOpeningManual == null){
            return;
        }

        currentOpeningManual.sidebar.handleScrollOnTicks();
        currentOpeningManual.content.handleScrollOnTicks();
        historyPageStack.peek().setValue(currentOpeningManual.content.getVerticalOffset());
    }
}
