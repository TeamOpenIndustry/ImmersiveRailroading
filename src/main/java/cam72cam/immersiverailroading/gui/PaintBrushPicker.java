package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.gui.components.ListSelector;
import cam72cam.immersiverailroading.items.ItemPaintBrush;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.PaintBrushMode;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.*;
import cam72cam.mod.render.opengl.RenderState;
import util.Matrix4;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class PaintBrushPicker implements IScreen {
    private EntityMoveableRollingStock stock;
    private String variant;

    private double zoom = 1;
    private long frame;

    @Override
    public void init(IScreenBuilder screen) {
        frame = 0;

        Entity ent = MinecraftClient.getEntityMouseOver();
        if (ent == null) {
            screen.close();
            return;
        }
        this.stock = ent.as(EntityMoveableRollingStock.class);
        if (stock == null) {
            screen.close();
        }
        this.variant = stock.getTexture();

        int xtop = -GUIHelpers.getScreenWidth() / 2;
        int ytop = -GUIHelpers.getScreenHeight()/4;
        int width = 200;
        int height = 20;

        new ListSelector<String>(screen, 0, width, height, variant,
                stock.getDefinition().textureNames.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getValue, Map.Entry::getKey,
                                (u, v) -> u, LinkedHashMap::new))
        ) {
            @Override
            public void onClick(String option) {
                variant = option;
            }
        }.setVisible(true);

        Slider zoom_slider = new Slider(screen, xtop + width, (int) (GUIHelpers.getScreenHeight()*0.75 - height), "Zoom: ", 0.1, 2, 1, true) {
            @Override
            public void onSlider() {
                zoom = this.getValue();
            }
        };

        width = 80;
        Button random = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, ytop, width, height, "Random") {
            @Override
            public void onClick(Player.Hand hand) {
                variant = ItemPaintBrush.nextRandomTexture(stock, variant);
            }
        };

        Button apply = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, (int) (GUIHelpers.getScreenHeight()*0.75 - height*2), width, height, "Apply to Stock") {
            @Override
            public void onClick(Player.Hand hand) {
                new ItemPaintBrush.PaintBrushPacket(stock, PaintBrushMode.GUI, variant, false).sendToServer();
                screen.close();
            }
        };
        Button apply_connected = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, (int) (GUIHelpers.getScreenHeight()*0.75 - height), width, height, "Apply to Train") {
            @Override
            public void onClick(Player.Hand hand) {
                new ItemPaintBrush.PaintBrushPacket(stock, PaintBrushMode.GUI, variant, true).sendToServer();
                screen.close();
            }
        };
    }

    @Override
    public void onEnterKey(IScreenBuilder builder) {
        new ItemPaintBrush.PaintBrushPacket(stock, PaintBrushMode.GUI, variant, false).sendToServer();
        builder.close();
    }

    @Override
    public void onClose() {

    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        frame++;

        double textScale = 1.5;

        GUIHelpers.drawRect(200, 0, GUIHelpers.getScreenWidth()-200, GUIHelpers.getScreenHeight(), 0xCC000000);
        GUIHelpers.drawRect(0, 0, 200, GUIHelpers.getScreenHeight(), 0xEE000000);

        String current = variant == null ? null : variant + "";
        if (current == null || current.isEmpty()) {
            current = stock.getDefinition().textureNames.keySet().stream().findFirst().orElse(null);
        }
        if (current == null) {
            current = ChatText.BRUSH_NO_VARIANTS.toString();
        } else {
            current = stock.getDefinition().textureNames.getOrDefault(current, "Unknown");
        }
        current = GuiText.TEXTURE_TOOLTIP.toString(current);

        GUIHelpers.drawCenteredString(stock.getDefinition().name(), (int) ((200 + (GUIHelpers.getScreenWidth()-200) / 2) / textScale), (int) (40 / textScale), 0xFFFFFF, new Matrix4().scale(textScale, textScale, textScale));
        GUIHelpers.drawCenteredString(current, (int) ((200 + (GUIHelpers.getScreenWidth()-200) / 2) / textScale), (int) ((GUIHelpers.getScreenHeight() - 60) / textScale), 0xFFFFFF, new Matrix4().scale(textScale, textScale, textScale));

        StockModel<?, ?> model = stock.getDefinition().getModel();

        //int scale = 8;
        int scale = (int) (GUIHelpers.getScreenWidth() / 40 * zoom);
        float speed = 0.75f;
        state.translate(200 + (GUIHelpers.getScreenWidth()-200) / 2, builder.getHeight() / 2 + 10, 400);
        state.rotate((stock.getTickCount()*speed) % 360, 0, 1, 0);
        state.scale(-scale, -scale, -scale);
        state.lightmap(1, 1);

        double prevDist = stock.distanceTraveled;
        String prevTex = stock.getTexture();
        Gauge prevGauge = stock.gauge;

        stock.setTexture(variant);
        stock.distanceTraveled += frame * 0.02;
        stock.gauge = Gauge.standard();

        model.renderEntity(stock, state, 0);
        model.postRenderEntity(stock, state, 0);

        stock.setTexture(prevTex);
        stock.distanceTraveled = prevDist;
        stock.gauge = prevGauge;
    }
}
