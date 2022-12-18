package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.Button;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.gui.screen.TextField;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;

import java.util.*;
import java.util.stream.Collectors;

public class PaintBrushPicker implements IScreen {
    private static final Random r = new Random();

    private EntityRollingStock stock;
    private String variant;

    private List<Button> options;

    private int page;
    private int pageSize;
    private Button pagination;

    @Override
    public void init(IScreenBuilder screen) {
        Entity ent = MinecraftClient.getEntityMouseOver();
        if (ent == null) {
            screen.close();
            return;
        }
        this.stock = ent.as(EntityRollingStock.class);
        if (stock == null) {
            screen.close();
        }
        this.variant = stock.getTexture();

        int xtop = -GUIHelpers.getScreenWidth() / 2;
        int ytop = -GUIHelpers.getScreenHeight()/4;
        int width = 200;
        int height = 20;
        page = 0;
        pageSize = Math.max(1, GUIHelpers.getScreenHeight() / height - 2);

        TextField search = new TextField(screen, xtop + 1, ytop + 1, width - 2, height - 2);

        pagination = new Button(screen, xtop-1, ytop + height, width + 1, height, "Page") {
            @Override
            public void onClick(Player.Hand hand) {
                page += hand == Player.Hand.PRIMARY ? 1 : -1;
                updateVariants(search.getText());
            }
        };

        options = new ArrayList<>();
        for (int i = 0; i < pageSize; i++) {
            options.add(new Button(screen, xtop-1, ytop + height*2 + i * height, width+1, height, "") {
                @Override
                public void onClick(Player.Hand hand) {
                    variant = stock.getDefinition().textureNames.entrySet().stream()
                            .filter(x -> x.getValue().equals(this.getText()))
                            .map(Map.Entry::getKey)
                            .findFirst().orElse(null);
                }
            });
        }

        search.setValidator(s -> {
            page = 0;
            this.updateVariants(s);
            return true;
        });
        this.updateVariants("");

        width = 60;
        Button random = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, ytop, width, height, "Random") {
            @Override
            public void onClick(Player.Hand hand) {
                List<String> choices = new ArrayList<>(stock.getDefinition().textureNames.keySet());
                variant = choices.get(r.nextInt(choices.size()));
            }
        };

        Button apply = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, (int) (GUIHelpers.getScreenHeight()*0.75 - height), width, height, "Apply") {
            @Override
            public void onClick(Player.Hand hand) {
                stock.setTexture(variant);
                screen.close();
            }
        };
    }

    private void updateVariants(String search) {

        Collection<String> names = stock.getDefinition().textureNames.values();
        if (!search.isEmpty()) {
            names = names.stream()
                    .filter(v -> v.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        int nPages = pageSize > 0 ? (int)Math.ceil(names.size() / (float)pageSize) : 0;
        if (page >= nPages) {
            page = 0;
        }
        if (page < 0) {
            page = nPages - 1;
        }

        pagination.setText(String.format("Page %s of %s", page+1, Math.max(1, nPages)));

        options.forEach(b -> {
            b.setVisible(false);
            b.setEnabled(false);
        });

        int bid = 0;
        for (String name : names.stream().skip((long) page * pageSize).limit(pageSize).collect(Collectors.toList())) {
            Button button = options.get(bid);
            button.setEnabled(true);
            button.setVisible(true);
            button.setText(name);

            bid++;
        }
    }

    @Override
    public void onEnterKey(IScreenBuilder builder) {
        stock.setTexture(variant);
        builder.close();
    }

    @Override
    public void onClose() {

    }

    @Override
    public void draw(IScreenBuilder builder) {
        //GUIHelpers.drawRect(0, 0, GUIHelpers.getScreenWidth(), GUIHelpers.getScreenHeight(), 0x44000000);
        GUIHelpers.drawCenteredString(stock.getDefinition().name(), 200 + (GUIHelpers.getScreenWidth()-200) / 2, 40, 0xFFFFFF);
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
        GUIHelpers.drawCenteredString(current, 200 + (GUIHelpers.getScreenWidth()-200) / 2, GUIHelpers.getScreenHeight() - 60, 0xFFFFFF);


        RenderState state = new RenderState();
        // TODO add state to draw params

        StockModel<?> model = stock.getDefinition().getModel();

        //int scale = 8;
        int scale = GUIHelpers.getScreenWidth() / 40;
        float speed = 1.5f;
        state.translate(200 + (GUIHelpers.getScreenWidth()-200) / 2, builder.getHeight() / 2, 400);
        state.rotate((stock.getTickCount()*speed) % 360, 0, 1, 0);
        state.scale(-scale, -scale, -scale);

        try (OBJRender.Binding binding = model.binder().texture(variant).bind(state)) {
            binding.draw(stock.getDefinition().itemGroups);
        }
    }
}
