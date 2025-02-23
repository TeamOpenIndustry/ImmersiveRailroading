package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.gui.markdown.*;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;
import util.Matrix4;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public class ManualGui implements IScreen {
    //TODO Does it works fine on server?
    public static boolean isOpen;
    public static ManualGui currentOpeningManual;

    private int width;
    private int height;
    //实现滚动
    private double sidebarOffset = 0;
    private double mainOffset = 0;
    private int sidebarHeight;
    private int mainHeight;
    //      全文  行    元素
    private List<List<MarkdownElement>> sidebar;
    private MarkdownElement footer;
    private Identifier lastPage;
    private int lastWidth;
    private Identifier currentPage = new Identifier(ImmersiveRailroading.MODID, "wiki/en_us/getting_started.md");
    private List<List<MarkdownElement>> content;

    @Override
    public void init(IScreenBuilder screen) {
        isOpen = true;
        currentOpeningManual = this;
        try {
            sidebar = MarkdownBuilder.build(new Identifier(ImmersiveRailroading.MODID, "wiki/en_us/_sidebar.md").getResourceStream(), screen.getWidth());
            footer = MarkdownBuilder.build(new Identifier(ImmersiveRailroading.MODID, "wiki/en_us/_footer.md").getResourceStream(), screen.getWidth())
                    .get(0).get(0);
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

        if(content == null || currentPage != lastPage || lastWidth != width){
            //Update main line break
            try {
                content = MarkdownBuilder.build(currentPage.getResourceStream(), width - 240);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            lastPage = currentPage;
            lastWidth = width;
            mainOffset = 0;
        }

        //Background
        GUIHelpers.drawRect(0, 0, width , height, 0xCC000000);
        GUIHelpers.drawRect(50, 0, width - 100, height, 0xFFFFFFFF);
        GUIHelpers.drawRect(50, 20, 120, height - 30, 0xFFDDDDDD);

        Matrix4 side = state.model_view().copy();
        side.translate(57, 27 - sidebarOffset, 0);

        //Dynamically update urls' pos
        for(List<MarkdownElement> line : sidebar){
            int currWidth = 0;
            for(MarkdownElement element : line){
                String str = element.apply();
                GUIHelpers.drawString(str, (int) side.apply(Vec3d.ZERO).x, (int) side.apply(Vec3d.ZERO).y, 0xFF000000);

                if(element instanceof MarkdownUrl){
                    ((MarkdownUrl) element).section = new Rectangle((int) side.apply(Vec3d.ZERO).x, (int) side.apply(Vec3d.ZERO).y,
                            GUIHelpers.getTextWidth(str), 10);
                    ((MarkdownUrl) element).inMain = true;
                }

                currWidth += GUIHelpers.getTextWidth(str);
                side.translate(GUIHelpers.getTextWidth(str), 0, 0);
            }
            side.translate(-currWidth, 10, 0);
        }
        sidebarHeight = (int) side.apply(Vec3d.ZERO).y;

        Matrix4 main = state.model_view().copy();
        main.translate(180, 30 - mainOffset, 0);
        for(List<MarkdownElement> line : content){
            int currWidth = 0;
            for(MarkdownElement element : line){
                if(element instanceof MarkdownPicture){
                    RenderState s = new RenderState();
                    s.translate((int) main.apply(Vec3d.ZERO).x, (int) main.apply(Vec3d.ZERO).y, 10)
                            .scale(width - 240, width - 240, 1)
                            .texture(Texture.wrap(((MarkdownPicture) element).picture));
                    ((MarkdownPicture) element).directDraw.draw(state.clone().scale(2,2,2).texture(Texture.wrap(((MarkdownPicture) element).picture)));
                    main.translate(0, (width - 240) * ((MarkdownPicture) element).ratio, 0);
                    continue;
                }

                String str = element.apply();
                GUIHelpers.drawString(str, (int) main.apply(Vec3d.ZERO).x, (int) main.apply(Vec3d.ZERO).y, 0xFF000000);

                if(element instanceof MarkdownUrl){
                    ((MarkdownUrl) element).section = new Rectangle((int) main.apply(Vec3d.ZERO).x, (int) main.apply(Vec3d.ZERO).y,
                            GUIHelpers.getTextWidth(str), 10);
                    ((MarkdownUrl) element).inMain = true;
                }

                currWidth += GUIHelpers.getTextWidth(str);
                main.translate(GUIHelpers.getTextWidth(str), 0, 0);
            }
            main.translate(-currWidth, 10, 0);
        }
        mainHeight = (int) main.apply(Vec3d.ZERO).y + 40;

        //Foreground(to hide text)
        //Header rect
        GUIHelpers.drawRect(50, 0, builder.getWidth() - 100, 20, 0xFF888888);
        //Footer rect
        GUIHelpers.drawRect(50, builder.getHeight() - 10, builder.getWidth() - 100, 10, 0xFFEEEEEE);

        //Footer text
        GUIHelpers.drawCenteredString(footer.apply(), width / 2, builder.getHeight() - 10, 0xFF000000);
    }

    public void changeContent(Identifier identifier){
        this.currentPage = identifier;
    }

    public void onClick(ClientEvents.MouseGuiEvent event){
        int region = detectRegion(event.x, event.y);
        if(event.scroll != 0) {
            switch (region) {
                case 1:
                    sidebarOffset = Math.max(0, Math.min(sidebarHeight, sidebarOffset - (event.scroll * 5)));
                    return;
                case 2:
                    mainOffset = Math.max(0, Math.min(mainHeight, mainOffset - (event.scroll * 5)));
                    return;
            }
        }

        if(event.action == ClientEvents.MouseAction.RELEASE){
            switch (region) {
                case 1:
                    sidebar.forEach(line -> {
                        line.stream().filter(e -> e instanceof MarkdownHeader).forEach(element -> {
                            if(((MarkdownHeader) element).url.section.contains(event.x, event.y)){
                                ((MarkdownHeader) element).url.click();
                            }
                        });
                        line.stream().filter(e -> e instanceof MarkdownUrl).forEach(element -> {
                            if(((MarkdownUrl) element).section.contains(event.x, event.y)){
                                ((MarkdownUrl) element).click();
                            }
                        });
                    });
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
