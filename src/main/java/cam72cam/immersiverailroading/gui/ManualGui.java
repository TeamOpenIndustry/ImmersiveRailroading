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
import java.util.List;
import java.util.Stack;

public class ManualGui implements IScreen {
    public static boolean isOpen;
    public static ManualGui currentOpeningManual;
    //                                        page     page's mainOffset
    private static final Stack<MutablePair<Identifier, Double>> pageStack = new Stack<>();

    private static final int HEADER_COLOR = 0xFF888888;
    private static final int SIDEBAR_COLOR = 0xFFCCCCCC;
    private static final int MAIN_COLOR = 0xFFFFFFFF;
    private static final int FOOTER_COLOR = 0xFFEEEEEE;
    private static final int BACKGROUND_COLOR = 0xCC000000;

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    private int width;
    private int height;
    private MarkdownDocument sidebar;
    private MarkdownDocument footer;
    private MarkdownDocument content;
    private Identifier lastPage;

    private Rectangle2D prevPageButton;

    static {
        pageStack.push(MutablePair.of(new Identifier("immersiverailroading:wiki/en_us/home.md"), 0d));
    }

    //Will be called every time the screen scale changes
    //So there's no need to update line break manually
    @Override
    public void init(IScreenBuilder screen) {
        isOpen = true;
        currentOpeningManual = this;
        try {
            footer = MarkdownBuilder.build(new Identifier(ImmersiveRailroading.MODID, "wiki/en_us/_footer.md"), screen.getWidth() - 120);
            int footerHeight = footer.countLine() * 10;
            sidebar = MarkdownBuilder.build(new Identifier(ImmersiveRailroading.MODID, "wiki/en_us/_sidebar.md"), screen.getWidth());
            sidebar.setScrollRegion(new Rectangle(50, 20, 120, screen.getHeight() - 20 - footerHeight));
            content = MarkdownBuilder.build(pageStack.peek().getLeft(), screen.getWidth() - 240);
            content.setScrollRegion(new Rectangle(170,20,width - 220,height - 20 - footerHeight));

            prevPageButton = new Rectangle(60,5,10,10);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public void onEnterKey(IScreenBuilder builder) {

    }

    @Override
    public void onClose() {
        isOpen = false;
        currentOpeningManual = null;
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        IScreen.super.draw(builder, state);
        width = builder.getWidth();
        height = builder.getHeight();

        if(lastPage != pageStack.peek().getLeft()){
            try {
                content = MarkdownBuilder.build(pageStack.peek().getLeft(), width - 240);
                content.setScrollRegion(new Rectangle(170,20,width-220,height-30));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            content.setOffset(pageStack.peek().getValue());
            lastPage = pageStack.peek().getLeft();
        }

        //Background
        GUIHelpers.drawRect(0, 0, width, height, BACKGROUND_COLOR);
        GUIHelpers.drawRect(50, 0, width - 100, height, MAIN_COLOR);
        GUIHelpers.drawRect(50, 0, 120, height, SIDEBAR_COLOR);

        //Markdown
        sidebar.render(state.model_view().copy().translate(57, 27, 0));
        content.render(state.model_view().copy().translate(180, 30, 0));

        //Tooltip
        if(sidebar.over != null){
            sidebar.over.renderTooltip((int) sidebar.getScrollRegion().getMaxY());
        } else if(content.over != null){
            content.over.renderTooltip((int) content.getScrollRegion().getMaxY());
        }

        //Footer
        int lineCount = footer.countLine();
        GUIHelpers.drawRect(50, height - (10 * lineCount), width - 100, 10 * lineCount, FOOTER_COLOR);
        for(MarkdownDocument.MarkdownLine l : footer.brokenLines){
            List<MarkdownElement> line = l.line;
            GUIHelpers.drawCenteredString(line.get(0).apply(),
                    width / 2, builder.getHeight() - (10 * lineCount), BLACK);
            lineCount --;
        }

        //Header
        GUIHelpers.drawRect(50, 0, width - 100, 20, HEADER_COLOR);
        GUIHelpers.drawString(TextColor.BOLD.wrap("<-"), 60,5, WHITE);
    }

    public void pushContent(Identifier identifier){
        if(!pageStack.peek().getLeft().equals(identifier)){
            pageStack.push(MutablePair.of(identifier, 0d));
        }
    }

    public void onClick(ClientEvents.MouseGuiEvent event){
        if(event.scroll != 0) {
            sidebar.onScroll(event);
            content.onScroll(event);
        }

        if(event.action == ClientEvents.MouseAction.RELEASE){
            if(prevPageButton.contains(event.x, event.y)){
                if(pageStack.size() > 1){
                    pageStack.pop();
                }
                return;
            }
            sidebar.onMouseRelease(event);
            content.onMouseRelease(event);
        }
    }

    //For scroll
    public void onClientTick(){
        sidebar.onClientTick();
        content.onClientTick();
        pageStack.peek().setValue(content.getOffset());
    }
}
