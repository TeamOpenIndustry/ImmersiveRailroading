package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.gui.markdown.MarkdownBuilder;
import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import org.apache.commons.lang3.tuple.Pair;
import util.Matrix4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ManualGui implements IScreen {
    //TODO Does it works fine on server?
    public static boolean isOpen;
    public static ManualGui current;

    private int width;
    private int height;
    private double navigationOffset = 0;
    private int sidebarHeight;
    private List<String> sidebar;
    private List<String> content;
    private Pair<Integer, List<String>> slashed;

    @Override
    public void init(IScreenBuilder screen) {
        isOpen = true;
        current = this;
        try {
            sidebar = MarkdownBuilder.build(new Identifier(ImmersiveRailroading.MODID, "wiki/en_us/sidebar.md").getResourceStream());
            content = MarkdownBuilder.build(new Identifier(ImmersiveRailroading.MODID, "wiki/en_us/getting_started.md").getResourceStream());
        } catch (IOException e) {
            throw new RuntimeException("eeeeeeeeeeeee");
        }
    }

    @Override
    public void onEnterKey(IScreenBuilder builder) {

    }

    @Override
    public void onClose() {
        isOpen = false;
        current = null;
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        IScreen.super.draw(builder, state);
        int y = 27;
        width = builder.getWidth();
        height = builder.getHeight();

        //Background
        GUIHelpers.drawRect(0, 0, width , height, 0xCC000000);
        GUIHelpers.drawRect(50, 0, width - 100, height, 0xFFFFFFFF);
        GUIHelpers.drawRect(50, 20, 120, height - 30, 0xFFDDDDDD);

        Matrix4 side = state.model_view().copy();

        side.translate(57, y - navigationOffset, 0);
        for (String str : sidebar){
            GUIHelpers.drawString(str, (int) side.apply(Vec3d.ZERO).x, (int) side.apply(Vec3d.ZERO).y, 0xFF000000);
            side.translate(0,12,0);
            y += 12;
        }
        sidebarHeight = (int) side.apply(Vec3d.ZERO).y;

        if(slashed == null || !slashed.getKey().equals(width - 220)){
            slashed = Pair.of(width - 220, new ArrayList<>());
            for(String str : content){
                int ptr = 0;
                while (ptr <= str.length() -1){
                    StringBuilder b = new StringBuilder();
                    while (GUIHelpers.getTextWidth(b.toString()) < width - 280) {
                        if(ptr > str.length() -1){
                            break;
                        }
                        b.append(str.charAt(ptr));
                        ptr++;
                    }
                    slashed.getValue().add(b.toString());
                }
            }
        }

        Matrix4 main = state.model_view().copy();
        main.translate(180, 30, 0);
        for(String str : content){
            GUIHelpers.drawString(str, (int) main.apply(Vec3d.ZERO).x, (int) main.apply(Vec3d.ZERO).y, 0xFF000000);
            main.translate(0,10,0);
        }
        //Foreground
        GUIHelpers.drawRect(50, 0, builder.getWidth() - 100, 20, 0xFF888888);
        GUIHelpers.drawRect(50, builder.getHeight() - 10, builder.getWidth() - 100, 10, 0xFFEEEEEE);
    }

    private void drawBackground(IScreenBuilder builder){
    }

    public void onClick(ClientEvents.MouseGuiEvent event){
        int region = detectRegion(event.x, event.y);
        ModCore.info(String.valueOf(region));

        if(region == 1 && event.scroll != 0){
            navigationOffset = Math.max(0, Math.min(sidebarHeight, navigationOffset - (event.scroll * 5)));
        }

        if(event.action == ClientEvents.MouseAction.RELEASE){

        }
    }

    public int detectRegion(int x, int y){
        if(x >= 50 && x < 170 && y >= 20 && y < height - 10){
            //Navigation panel
            return 1;
        } else if (x >= 170 && x < width - 50 && y >= 20 && y < height - 10) {
            //Main panel
            return 2;
        }
        return -1;
    }
}
