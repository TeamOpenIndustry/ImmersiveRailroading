package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.gui.markdown.*;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.text.TextColor;
import org.apache.commons.lang3.tuple.MutablePair;
import util.Matrix4;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.List;
import java.util.Stack;

public class ManualGui implements IScreen {
    //TODO Does it works fine on server?
    //Yes as the class is client-only
    public static boolean isOpen;
    public static ManualGui currentOpeningManual;
    //                                        page     page's mainOffset
    private static final Stack<MutablePair<Identifier, Double>> pageStack;

    private static final int HEADER_COLOR = 0xFF888888;
    private static final int SIDEBAR_COLOR = 0xFFCCCCCC;
    private static final int MAIN_COLOR = 0xFFFFFFFF;
    private static final int CODE_COLOR = 0xFFDDDDDD;
    private static final int SPLIT_LINE_COLOR = 0xFF888888;
    private static final int FOOTER_COLOR = 0xFFEEEEEE;
    private static final int BACKGROUND_COLOR = 0xCC000000;

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    private int width;
    private int height;
    //Imply scroll
    private double sidebarSpeed = 0;
    private double sidebarOffset = 0;
    private double mainSpeed = 0;
    private double mainOffset = 0;
    private int sidebarHeight;
    private int mainHeight;
    //     Whole Line   Element
    private List<List<MarkdownElement>> sidebar;
    private List<List<MarkdownElement>> footer;
    private Identifier lastPage;
    private List<List<MarkdownElement>> content;

    private Rectangle2D prevPageButton;

    static {
        pageStack = new Stack<>();
    }

    //Will be called every time the screen scale changes
    //So there's no need to update line break manually
    @Override
    public void init(IScreenBuilder screen) {
        isOpen = true;
        currentOpeningManual = this;
        try {
            sidebar = MarkdownBuilder.build(new Identifier(ImmersiveRailroading.MODID, "wiki/en_us/_sidebar.md").getResourceStream(), screen.getWidth());
            footer = MarkdownBuilder.build(new Identifier(ImmersiveRailroading.MODID, "wiki/en_us/_footer.md").getResourceStream(), screen.getWidth() - 120);
            pageStack.push(MutablePair.of(new Identifier("immersiverailroading:wiki/en_us/home.md"), 0d));
            content = MarkdownBuilder.build(pageStack.peek().getLeft().getResourceStream(), screen.getWidth() - 240);

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
        pageStack.clear();
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        IScreen.super.draw(builder, state);
        width = builder.getWidth();
        height = builder.getHeight();

        if(lastPage != pageStack.peek().getLeft()){
            try {
                content = MarkdownBuilder.build(pageStack.peek().getLeft().getResourceStream(), width - 240);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            mainOffset = pageStack.peek().getValue();
            lastPage = pageStack.peek().getLeft();
        }

        //Background
        GUIHelpers.drawRect(0, 0, width, height, BACKGROUND_COLOR);
        GUIHelpers.drawRect(50, 0, width - 100, height, MAIN_COLOR);
        GUIHelpers.drawRect(50, 0, 120, height, SIDEBAR_COLOR);

        Matrix4 side = state.model_view().copy();
        side.translate(57, 27 - sidebarOffset, 0);

        int cachedHeight = 0;

        for(List<MarkdownElement> line : sidebar){
            int currWidth = 0;
            for(MarkdownElement element : line){
                //Show current matrix result
                Vec3d offset = side.apply(Vec3d.ZERO);

                if(element instanceof MarkdownHeader && ((MarkdownHeader) element).render(side)){
                    //Successfully rendered as header, continue
                    continue;
                } //Otherwise render as normal text

                String str = element.apply();
                GUIHelpers.drawString(str, 0, 0, BLACK, side);

                //Dynamically update urls' pos
                if(element instanceof MarkdownUrl){
                    ((MarkdownUrl) element).section = new Rectangle((int) offset.x, (int) offset.y,
                            GUIHelpers.getTextWidth(str), 10);
                    ((MarkdownUrl) element).inMain = false;
                }

                currWidth += GUIHelpers.getTextWidth(str);
                side.translate(GUIHelpers.getTextWidth(str), 0, 0);
            }
            side.translate(-currWidth, 10, 0);
            cachedHeight += 10;
        }
        sidebarHeight = cachedHeight - 100;
        cachedHeight = 0;

        Matrix4 main = state.model_view().copy();
        main.translate(180, 30 - mainOffset, 0);
        for(List<MarkdownElement> line : content){
            int currWidth = 0;
            for(MarkdownElement element : line){
                //Show current matrix result
                Vec3d offset = main.apply(Vec3d.ZERO);

                if(element instanceof MarkdownHeader && ((MarkdownHeader) element).render(main)){
                    //Successfully rendered as header, continue
                    continue;
                } //Otherwise render as normal text

                if (element instanceof MarkdownPicture) {
                    MarkdownPicture mdPicture = (MarkdownPicture) element;
                    int picHeight = (int) ((width - 240) * mdPicture.ratio);
                    GUIHelpers.texturedRect(mdPicture.picture, (int) offset.x, (int) offset.y,
                            width - 240, picHeight);
                    main.translate(0, picHeight, 0);
                    cachedHeight += picHeight;
                    continue;
                }

                if(element instanceof MarkdownSplitLine){
                    GUIHelpers.drawRect((int) offset.x, (int) offset.y,width - 240, 2,  SPLIT_LINE_COLOR);
                    main.translate(0, 2,0);
                    cachedHeight += 2;
                    continue;
                }

                if(element instanceof MarkdownStyledText &&((MarkdownStyledText) element).isCode()){
                    //Draw code block
                    String str = element.apply();
                    GUIHelpers.drawRect((int) offset.x - 2, (int) offset.y - 1,
                            GUIHelpers.getTextWidth(str) + 4, 12,  CODE_COLOR);
                    GUIHelpers.drawString(str, (int) offset.x, (int) offset.y, BLACK);
                    currWidth += GUIHelpers.getTextWidth(str) +2;
                    main.translate(GUIHelpers.getTextWidth(str) + 2, 0, 0);
                    continue;
                }

                String str = element.apply();
                GUIHelpers.drawString(str, (int) offset.x, (int) offset.y, BLACK);

                //Dynamically update urls' pos
                if(element instanceof MarkdownUrl){
                    ((MarkdownUrl) element).section = new Rectangle((int) offset.x, (int) offset.y,
                            GUIHelpers.getTextWidth(str), 10);
                    ((MarkdownUrl) element).inMain = true;
                }

                currWidth += GUIHelpers.getTextWidth(str);
                main.translate(GUIHelpers.getTextWidth(str), 0, 0);
            }
            main.translate(-currWidth, 10, 0);
            cachedHeight += 10;
        }
        mainHeight = cachedHeight - 100;

        //Foreground(to hide text)
        //Footer rect
        int lineCount = footer.size();
        GUIHelpers.drawRect(50, height - (10 * lineCount),
                width - 100, 10 * lineCount, FOOTER_COLOR);

        //Footer text
        for(List<MarkdownElement> line : footer){
            GUIHelpers.drawCenteredString(line.get(0).apply(),
                    width / 2, builder.getHeight() - (10 * lineCount), BLACK);
            lineCount --;
        }

        //Header rect
        GUIHelpers.drawRect(50, 0, width - 100, 20, HEADER_COLOR);
        GUIHelpers.drawString(TextColor.BOLD.wrap("<-"), 60,5, WHITE);
    }

    public void changeContent(Identifier identifier){
        if(!pageStack.peek().getLeft().equals(identifier)){
            pageStack.push(MutablePair.of(identifier, 0d));
        }
    }

    public void onClick(ClientEvents.MouseGuiEvent event){
        int region = detectRegion(event.x, event.y);
        if(event.scroll != 0) {
            switch (region) {
                case 1:
                    sidebarSpeed =  Math.min(50, Math.max(-50, sidebarSpeed - (10 * event.scroll)));
                    return;
                case 2:
                    mainSpeed =  Math.min(50, Math.max(-50, mainSpeed - (10 * event.scroll)));
                    return;
            }
        }

        if(event.action == ClientEvents.MouseAction.RELEASE){
            if(prevPageButton.contains(event.x, event.y)){
                if(pageStack.size() > 1){
                    pageStack.pop();
                }
                return;
            }
            switch (region) {
                case 1:
                    sidebar.forEach(line -> line.stream().filter(e -> e instanceof MarkdownUrl).forEach(element -> {
                        if(((MarkdownUrl) element).section.contains(event.x, event.y)){
                            ((MarkdownUrl) element).click();
                        }
                    }));
                    break;
                case 2:
                    content.forEach(line -> line.stream().filter(e -> e instanceof MarkdownUrl).forEach(element -> {
                        if(((MarkdownUrl) element).section.contains(event.x, event.y)){
                            ((MarkdownUrl) element).click();
                        }
                    }));
                    break;
            }
        }
    }

    //For scroll
    public void onClientTick(){
        this.mainOffset += mainSpeed;
        this.sidebarOffset += sidebarSpeed;

        sidebarOffset = Math.max(0, Math.min(sidebarHeight, sidebarOffset));
        mainOffset = Math.max(0, Math.min(mainHeight, mainOffset));

        pageStack.peek().setValue(mainOffset);

        mainSpeed += mainSpeed > 0 ? -Math.min(mainSpeed, 3) :
                    mainSpeed < 0 ? -Math.max(mainSpeed, -3) :
                            0;
        sidebarSpeed += sidebarSpeed > 0 ? -Math.min(sidebarSpeed, 3) :
                sidebarSpeed < 0 ? -Math.max(sidebarSpeed, -3) :
                        0;
    }

    public int detectRegion(int x, int y){
        if(x >= 50 && x < 170 && y >= 20 && y < height - 10){
            //Sidebar
            return 1;
        } else if (x >= 170 && x < width - 50 && y >= 20 && y < height - 10) {
            //Main panel
            return 2;
        }
        return -1;
    }
}
